package com.samsara.module.combat;

import com.mojang.blaze3d.platform.InputConstants;
import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.module.FeatureManager;
import com.samsara.setting.BooleanSetting;
import com.samsara.setting.ModeSetting;
import com.samsara.setting.MultiSelectSetting;
import com.samsara.setting.NumberSetting;
import com.samsara.setting.Setting;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiPredicate;
import java.util.function.DoubleFunction;
import mixins.ClientInputAccessor;
import mixins.MultiPlayerGameModeAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Vector3f;

public final class AutoRod extends Feature {
   private static final List<String> DEFAULT_IGNORED_ITEMS = List.of(
      "minecraft:bow", "minecraft:crossbow", "minecraft:trident", "minecraft:fire_charge", "minecraft:ender_pearl");
   private final ModeSetting gravity = mode("Gravity Type", "Linear", "Linear", "Projectile");
   private final NumberSetting minRange = number("Min Range", 3.5, 2, 10, .1);
   private final NumberSetting maxRange = number("Max Range", 5, 2, 10, .1);
   private final NumberSetting scanMin = number("Scan Extra Range Min", 0, 0, 5, .1);
   private final NumberSetting scanMax = number("Scan Extra Range", 0, 0, 5, .1);
   private final NumberSetting maxEnemies = number("Max Enemies Nearby", 1, 0, 10, 1);
   private final NumberSetting minHealth = number("Min Health", 10, 1, 20, .1);
   private final NumberSetting minTargetHealth = number("Min Target Health", 4, 1, 20, .1);
   private final MultiSelectSetting requires = new MultiSelectSetting("Requires", this,
      new String[]{"Click", "Weapon", "Vanilla Name", "Not Breaking"}, List.of());
   private final MultiSelectSetting ignores = new MultiSelectSetting("Ignores", this,
      new String[]{"Open Inventory", "Using Item", "Holding Consumable"}, List.of());
   private final MultiSelectSetting ignoredItems;
   private final MultiSelectSetting priority = new MultiSelectSetting("Target Priority", this,
      new String[]{"Type", "Distance", "Health", "Direction", "Hurt Time", "Age"}, List.of("Type", "Distance"));
   private final NumberSetting fov = number("Target FOV", 180, 0, 180, 1);
   private final NumberSetting hurtTime = number("Target Hurt Time", 10, 0, 10, 1);
   private final NumberSetting aimThreshold = number("Aim Off Threshold", 5, 2, 10, .1);
   private final ModeSetting swingMode = mode("Swing Mode", "Do Not Hide", "Do Not Hide", "Hide For Both", "Hide For Client", "Hide For Server");
   private final NumberSetting hitTimeout = number("Hit Timeout", 30, 5, 200, 1);
   private final BooleanSetting pullOutOfRange = new BooleanSetting("Pull On Out Of Range", this, true);
   private final NumberSetting resetMin = number("Slot Reset Delay", 0, 0, 20, 1);
   private final NumberSetting resetMax = number("Slot Reset Delay Max", 0, 0, 20, 1);
   private final NumberSetting cooldownMin = number("Cooldown", 4, 1, 50, 1);
   private final NumberSetting cooldownMax = number("Cooldown Max", 8, 1, 50, 1);
   private final BooleanSetting targetRendering = new BooleanSetting("Target Rendering", this, true);
   private final Aim aiming = new Aim(this);
   private final Cycle<LivingEntity> cycle = new Cycle<>();
   private final Slot silentSlot = new Slot();
   private LivingEntity target;
   private FishingHook bobber;
   private int availableSlot = -2, castSlot = -2;
   private double scanExtra, previousScanMin = Double.NaN, previousScanMax = Double.NaN;
   private Aim.Rotation rotation, serverRotation, castRotation;
   private LocalPlayer owner;
   private ClientLevel world;
   private long lastClick = Long.MIN_VALUE;
   private boolean pendingRelease, aimingForCast;

   public AutoRod() {
      super("AutoRod", Category.COMBAT);
      this.scanMax.setDisplayName("Scan Extra Range Max");
      this.resetMin.setDisplayName("Slot Reset Delay Min");
      this.cooldownMin.setDisplayName("Cooldown Min");
      this.ignores.setLegacyBooleanPrefix("Ignore ");
      this.priority.setOrderSensitive(true);
      this.priority.setAllowEmpty(false);
      var itemOptions = new ArrayList<String>(DEFAULT_IGNORED_ITEMS);
      if (mc != null) for (var key : BuiltInRegistries.ITEM.keySet()) {
         if (!itemOptions.contains(key.toString())) itemOptions.add(key.toString());
      }
      this.ignoredItems = new MultiSelectSetting("Holding Items For Ignore", this, itemOptions.toArray(String[]::new), DEFAULT_IGNORED_ITEMS);
   }

   private NumberSetting number(String name, double value, double min, double max, double step) {
      return new NumberSetting(name, this, value, min, max, step);
   }
   private ModeSetting mode(String name, String value, String... options) { return new ModeSetting(name, this, value, options); }
   static double random(double from, double to) {
      double min = Math.min(from, to), max = Math.max(from, to);
      return min == max ? min : ThreadLocalRandom.current().nextDouble(min, max);
   }
   static int randomTicks(double from, double to) {
      int min = (int)Math.min(from, to), max = (int)Math.max(from, to);
      return min == max ? min : ThreadLocalRandom.current().nextInt(min, max + 1);
   }

   /** Called even while disabled so deferred release and slot restoration finish at a tick boundary. */
   public void clientTick() {
      if (mc.player != this.owner || mc.level != this.world || mc.player == null || !mc.player.isAlive()) {
         clear(); this.owner = mc.player; this.world = mc.level;
      }
      if (mc.player == null || mc.level == null || mc.gameMode == null || mc.getConnection() == null) return;
      this.aimingForCast = false;
      this.castRotation = null;
      if (this.pendingRelease) {
         mc.gameMode.releaseUsingItem(mc.player); this.pendingRelease = false;
      }
      if (this.silentSlot.tick()) syncSlot();
      if (!isEnabled() || !mc.player.isAlive()) return;
      if (!this.cycle.waiting()) this.cycle.tick(false, false, false, 0);
      if (mc.options.keyAttack.isDown()) this.lastClick = System.nanoTime();
      this.bobber = findBobber();
      if (!rodAt(this.availableSlot) || this.availableSlot >= 0 && this.availableSlot != mc.player.getInventory().getSelectedSlot()) this.availableSlot = findRod();
      if (this.scanMin.getValue() != this.previousScanMin || this.scanMax.getValue() != this.previousScanMax) {
         this.previousScanMin = this.scanMin.getValue(); this.previousScanMax = this.scanMax.getValue();
         this.scanExtra = random(this.previousScanMin, this.previousScanMax);
      }
   }

   @Override public int getPriority(Event event) { return event == Events.ROTATION ? -3 : 0; }
   @Override public void onEvent(Event event) {
      if (event == Events.MOUSE_BUTTON && Events.MOUSE_BUTTON.getButton() == InputConstants.MOUSE_BUTTON_LEFT && Events.MOUSE_BUTTON.isPressed()) this.lastClick = System.nanoTime();
      if (mc.player == null || mc.level == null || mc.gameMode == null || !mc.player.isAlive()) return;
      if (event == Events.ROTATION) updateRotation();
      if (event == Events.POST_MOTION) updateCycle();
      if (event == Events.POST_MOVE_INPUT) this.aiming.correctMovement(this.rotation);
   }

   private void updateRotation() {
      this.rotation = null;
      this.castRotation = null;
      this.aimingForCast = false;
      if (!this.cycle.ready() || this.silentSlot.slot() != -1) {
         this.target = this.cycle.target();
         return;
      }
      if (!requirementsMet()) { this.target = null; restoreRotation(); return; }
      this.target = selectTarget();
      if (this.target == null) { restoreRotation(); return; }
      var desired = calculateRotation(this.target);
      if (desired == null) return;
      this.castRotation = desired;
      this.aimingForCast = true;
      this.rotation = this.aiming.turn(this.serverRotation == null ? cameraRotation() : this.serverRotation, desired);
      Events.ROTATION.setYaw(this.rotation.yaw()); Events.ROTATION.setPitch(this.rotation.pitch());
      this.aiming.applyMovementCorrection();
   }
   private void restoreRotation() {
      if (enabled(FeatureManager.killAura) && FeatureManager.killAura.target != null) {
         this.aiming.reset();
         return;
      }
      this.rotation = this.aiming.returnRotation(cameraRotation());
      if (this.rotation != null) {
         Events.ROTATION.setYaw(this.rotation.yaw()); Events.ROTATION.setPitch(this.rotation.pitch());
         this.aiming.applyMovementCorrection();
      }
   }

   private void updateCycle() {
      if (this.cycle.waiting()) {
         this.bobber = findBobber();
         LivingEntity initialTarget = this.cycle.target();
         boolean pull = this.cycle.tick(this.bobber != null && this.bobber.getHookedIn() != null,
            this.bobber != null && this.bobber.getKnownMovement().equals(Vec3.ZERO), this.pullOutOfRange.getValue(),
            initialTarget == null ? Double.POSITIVE_INFINITY : mc.player.distanceToSqr(initialTarget));
         if (pull && (this.bobber != null || this.cycle.timedOut())) {
            if (this.bobber != null) useRod(this.castSlot, randomTicks(this.resetMin.getValue(), this.resetMax.getValue()));
            else { this.silentSlot.clear(); syncSlot(); }
            this.cycle.pulled(randomTicks(this.cooldownMin.getValue(), this.cooldownMax.getValue()));
            this.castSlot = -2;
         }
         return;
      }
      if (!this.cycle.ready() || !requirementsMet() || this.target == null || this.rotation == null) return;
      var desired = this.castRotation;
      if (desired == null || (this.serverRotation == null ? cameraRotation() : this.serverRotation).angleTo(desired) > this.aimThreshold.getValue()) return;
      this.castSlot = this.availableSlot;
      if (this.bobber == null) {
         if (!useRod(this.castSlot, (int)this.hitTimeout.getValue() + (int)Math.max(this.resetMin.getValue(), this.resetMax.getValue()))) {
            this.castSlot = -2; return;
         }
         this.scanExtra = random(this.scanMin.getValue(), this.scanMax.getValue());
      }
      this.cycle.start(this.target, (int)this.hitTimeout.getValue(), Math.min(this.minRange.getValue(), this.maxRange.getValue()),
         Math.max(this.minRange.getValue(), this.maxRange.getValue()) + this.scanExtra);
   }

   private boolean requirementsMet() {
      if (!rodAt(this.availableSlot) || mc.player.getHealth() <= this.minHealth.getValue()
         || enabled(FeatureManager.scaffold) || enabled(FeatureManager.blink) || enabled(FeatureManager.stasis)) return false;
      ItemStack main = mc.player.getMainHandItem();
      if (this.ignoredItems.contains(BuiltInRegistries.ITEM.getKey(main.getItem()).toString())) return false;
      if (this.ignores.contains("Open Inventory") && mc.gui.screen() instanceof AbstractContainerScreen<?>
         || this.ignores.contains("Using Item") && mc.player.isUsingItem()
            && !(enabled(FeatureManager.killAura) && FeatureManager.killAura.isAutoBlocking())
         || this.ignores.contains("Holding Consumable") && (main.has(DataComponents.CONSUMABLE) || mc.player.getOffhandItem().has(DataComponents.CONSUMABLE))) return false;
      if (this.requires.contains("Click") && !mc.options.keyAttack.isDown()
         && (this.lastClick == Long.MIN_VALUE || System.nanoTime() - this.lastClick > 250_000_000L)
         || this.requires.contains("Weapon") && !isWeapon(main)
         || this.requires.contains("Vanilla Name") && main.has(DataComponents.CUSTOM_NAME)
         || this.requires.contains("Not Breaking") && mc.gameMode.isDestroying()) return false;
      double range = Math.max(this.minRange.getValue(), this.maxRange.getValue()) + this.scanExtra;
      return (int)this.maxEnemies.getValue() == 0 || targets().stream()
         .filter(entity -> mc.player.distanceToSqr(entity) <= range * range).count() <= (int)this.maxEnemies.getValue();
   }
   private static boolean enabled(Feature module) { return module != null && module.isEnabled(); }
   private boolean isWeapon(ItemStack stack) {
      if (stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES) || stack.getItem() instanceof MaceItem) return true;
      var enchantment = mc.level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(Enchantments.KNOCKBACK);
      return EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack) > 0;
   }
   private List<LivingEntity> targets() {
      var result = new ArrayList<LivingEntity>();
      for (Entity entity : mc.level.entitiesForRendering()) {
         if (entity instanceof LivingEntity living && FeatureManager.targets.shouldAttack(entity)
            && living.hurtTime <= this.hurtTime.getValue()
            && cameraRotation().angleTo(Aim.lookingAt(mc.player.getEyePosition(), living.getBoundingBox().getCenter())) <= this.fov.getValue()) result.add(living);
      }
      return result;
   }
   private LivingEntity selectTarget() {
      double minimum = Math.min(this.minRange.getValue(), this.maxRange.getValue());
      double maximum = Math.max(this.minRange.getValue(), this.maxRange.getValue()) + this.scanExtra;
      return targets().stream().filter(entity -> mc.player.distanceToSqr(entity) >= minimum * minimum
         && mc.player.distanceToSqr(entity) <= maximum * maximum && mc.player.hasLineOfSight(entity)
         && entity.getHealth() + entity.getAbsorptionAmount() > this.minTargetHealth.getValue())
         .min(targetComparator()).orElse(null);
   }
   private Comparator<LivingEntity> targetComparator() {
      Comparator<LivingEntity> result = (leftTarget, rightTarget) -> 0;
      for (String key : this.priority.selectedValues()) {
         Comparator<LivingEntity> next = switch (key) {
            case "Type" -> Comparator.comparingInt(this::typeWeight);
            case "Health" -> Comparator.comparingDouble(entity -> entity.getHealth() + entity.getAbsorptionAmount());
            case "Direction" -> Comparator.comparingDouble(entity -> cameraRotation().angleTo(Aim.lookingAt(mc.player.getEyePosition(), entity.getBoundingBox().getCenter())));
            case "Hurt Time" -> Comparator.comparingInt(entity -> entity.hurtTime);
            case "Age" -> Comparator.comparingInt((LivingEntity entity) -> entity.tickCount).reversed();
            default -> Comparator.comparingDouble(entity -> Aim.nearest(entity.getBoundingBox(), mc.player.getEyePosition()).distanceToSqr(mc.player.getEyePosition()));
         };
         result = result.thenComparing(next);
      }
      return result;
   }
   private int typeWeight(LivingEntity entity) {
      if (entity instanceof Player) return 0;
      if (entity instanceof Enemy) return 1;
      if (entity instanceof NeutralMob neutral && mc.player.getUUID().equals(neutral.getPersistentAngerTarget())) return 2;
      return Integer.MAX_VALUE;
   }
   private Aim.Rotation calculateRotation(LivingEntity entity) {
      return this.gravity.is("Linear") ? this.aiming.linear(entity) : this.aiming.projectile(entity);
   }
   private Aim.Rotation cameraRotation() { return new Aim.Rotation(mc.player.getYRot(), mc.player.getXRot()); }
   private FishingHook findBobber() {
      var fishing = mc.player.fishing;
      if (fishing != null && !fishing.isRemoved() && fishing.getPlayerOwner() == mc.player) return fishing;
      for (Entity entity : mc.level.entitiesForRendering()) {
         if (entity instanceof FishingHook hook && !hook.isRemoved() && hook.getPlayerOwner() == mc.player) return hook;
      }
      return null;
   }
   private int findRod() {
      if (rodAt(-1)) return -1;
      for (int slot = 0; slot < 9; slot++) if (rodAt(slot)) return slot;
      return -2;
   }
   private boolean rodAt(int slot) {
      return mc.player != null && (slot == -1 ? mc.player.getOffhandItem().is(Items.FISHING_ROD)
         : slot >= 0 && slot < 9 && mc.player.getInventory().getItem(slot).is(Items.FISHING_ROD));
   }
   public int serverSlot(int realSlot) {
      return mc.player == this.owner && mc.level == this.world ? this.silentSlot.selected(realSlot) : realSlot;
   }
   public LivingEntity renderedTarget() {
      return isEnabled() && this.targetRendering.getValue() && mc.level == this.world && this.target != null
         && !this.target.isRemoved() ? this.target : null;
   }
   private void syncSlot() { ((MultiPlayerGameModeAccessor)mc.gameMode).invokeEnsureHasSentCarriedItem(); }
   public boolean isCombatHandReserved() {
      return mc.player != null && mc.player == this.owner && mc.level == this.world
         && (this.silentSlot.slot() != -1 || isEnabled() && (this.aimingForCast || this.cycle.waiting() && this.castSlot >= 0));
   }
   private boolean useRod(int slot, int resetTicks) {
      if (!rodAt(slot)) { this.silentSlot.clear(); syncSlot(); return false; }
      if (FeatureManager.killAura != null && !FeatureManager.killAura.prepareForAutoRod()) return false;
      InteractionHand hand = slot == -1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
      if (slot >= 0) this.silentSlot.select(slot, resetTicks);
      float yaw = mc.player.getYRot(), pitch = mc.player.getXRot();
      try {
         // Prediction and the use packet must use the same server-facing rotation.
         mc.player.setYRot(Events.ROTATION.getYaw()); mc.player.setXRot(Events.ROTATION.getPitch());
         InteractionResult result = mc.gameMode.useItem(mc.player, hand);
         if (!result.consumesAction()) { this.silentSlot.clear(); syncSlot(); return false; }
         if (!this.swingMode.is("Hide For Both") && !this.swingMode.is("Hide For Client")) {
            mc.player.swing(hand, SwingAnimation.DEFAULT, false);
         }
         mc.player.itemUsed(hand);
         return true;
      } finally {
         mc.player.setYRot(yaw); mc.player.setXRot(pitch);
      }
   }
   public void observePacket(Packet<?> packet) {
      if (packet instanceof ServerboundMovePlayerPacket movement) {
         var previous = this.serverRotation;
         float yaw = previous == null && mc.player != null ? mc.player.getYRot() : previous == null ? 0 : previous.yaw();
         float pitch = previous == null && mc.player != null ? mc.player.getXRot() : previous == null ? 0 : previous.pitch();
         this.serverRotation = new Aim.Rotation(movement.getYRot(yaw), movement.getXRot(pitch));
      } else if (packet instanceof ServerboundUseItemPacket use) {
         this.serverRotation = new Aim.Rotation(use.yRot(), use.xRot());
      }
   }
   @Override public void onEnable() { clear(); this.owner = mc.player; this.world = mc.level; }
   @Override public void onDisable() {
      this.pendingRelease = mc.player != null && mc.level != null && mc.level == this.world && findBobber() != null;
      this.cycle.reset(); this.target = null; this.rotation = this.castRotation = null; this.bobber = null;
      this.aimingForCast = false;
      this.availableSlot = this.castSlot = -2; this.silentSlot.clear(); this.aiming.reset();
      if (mc.player != null && mc.gameMode != null && mc.getConnection() != null) syncSlot();
   }
   private void clear() {
      this.cycle.reset(); this.silentSlot.clear(); this.aiming.reset();
      this.target = null; this.bobber = null; this.rotation = this.serverRotation = this.castRotation = null;
      this.aimingForCast = false;
      this.availableSlot = this.castSlot = -2; this.pendingRelease = false;
      this.previousScanMin = this.previousScanMax = Double.NaN;
   }

   public static void extractTarget() {
      var mc = Minecraft.getInstance();
      var module = FeatureManager.autoRod;
      if (module == null || mc.player == null || mc.level == null) return;
      var target = module.renderedTarget();
      if (target == null) return;
      float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
      Vec3 center = target.getPosition(partial);
      try (var collection = mc.levelExtractor.collectPerFrameMainThreadGizmos()) {
         Gizmos.circle(center, .85f, GizmoStyle.stroke(0xFF007CFF, 1.5f)).setAlwaysOnTop();
         for (int band = 0; band < 8; band++) {
            double bottom = band * .3 / 8, top = (band + 1) * .3 / 8;
            int color = ((int)(90 * (1 - band / 8.0)) << 24) | 0x007CFF;
            for (int segment = 0; segment < 32; segment++) {
               double angle = segment * Math.PI * 2 / 32, next = (segment + 1) * Math.PI * 2 / 32;
               Vec3 left = center.add(Math.cos(angle) * .85, bottom, Math.sin(angle) * .85);
               Vec3 right = center.add(Math.cos(next) * .85, bottom, Math.sin(next) * .85);
               Gizmos.rect(left, right, right.add(0, top - bottom, 0), left.add(0, top - bottom, 0), GizmoStyle.fill(color)).setAlwaysOnTop();
            }
         }
      }
   }

   static final class Aim {
      private static final double HOOK_DRAG = .92;
      private static final double HOOK_SPEED = 1.5;
      private static final double HOOK_GRAVITY = .04;
      record Rotation(float yaw, float pitch) {
         Vec3 direction() { return Vec3.directionFromRotation(this.pitch, this.yaw); }
         double angleTo(Rotation other) {
            Vec3 direction = direction(), otherDirection = other.direction();
            return Math.toDegrees(Math.atan2(direction.cross(otherDirection).length(), direction.dot(otherDirection)));
         }
      }
      private static final Minecraft mc = Minecraft.getInstance();
      private final MultiSelectSetting exemptParts;
      private final BooleanSetting exemptNearest, delay, lazy, gaussian;
      private final NumberSetting exemptHorizontal, exemptVertical, delayMin, delayMax, lazyMin, lazyMax;
      private final NumberSetting gaussianYawMin, gaussianYawMax, gaussianPitchMin, gaussianPitchMax, gaussianChance, gaussianSpeedMin, gaussianSpeedMax, gaussianTolerance;
      private final ModeSetting smoothing, movementCorrection;
      private final NumberSetting yawMin, yawMax, pitchMin, pitchMax, steepness, midpoint;
      private final NumberSetting yawAccelMin, yawAccelMax, pitchAccelMin, pitchAccelMax, accelErrorYaw, accelErrorPitch, constantErrorYaw, constantErrorPitch, resetTicks, resetThreshold;
      private final BooleanSetting accelerationError, constantError, sigmoidDeceleration;
      private Rotation lastRotation;
      private float previousYawSpeed, previousPitchSpeed;
      private int remainingRotationTicks;
      private Vec3 delayedPoint, lazyPoint, gaussianOffset = Vec3.ZERO, gaussianTarget = Vec3.ZERO;
      private int pointDelay;
      private double lazyThreshold;

      Aim(Feature module) {
         this.exemptParts = new MultiSelectSetting("Aim Exempt Box Parts", module, new String[]{"Head", "Body", "Feet"}, List.of());
         this.exemptNearest = new BooleanSetting("Aim Exempt Best Hit Vector", module, false);
         this.exemptHorizontal = number(module, "Aim Exempt Horizontal", .1, 0, 1, .01, this.exemptNearest);
         this.exemptVertical = number(module, "Aim Exempt Vertical", .2, 0, 1, .01, this.exemptNearest);
         this.delay = new BooleanSetting("Aim Point Delay", module, false);
         this.delayMin = number(module, "Aim Point Delay Min", 2, 0, 5, 1, this.delay);
         this.delayMax = number(module, "Aim Point Delay Max", 4, 0, 5, 1, this.delay);
         this.lazy = new BooleanSetting("Aim Point Lazy", module, false);
         this.lazyMin = number(module, "Aim Point Threshold Min", .1, .01, .4, .01, this.lazy);
         this.lazyMax = number(module, "Aim Point Threshold Max", .2, .01, .4, .01, this.lazy);
         this.gaussian = new BooleanSetting("Aim Point Gaussian", module, false);
         this.gaussianYawMin = number(module, "Aim Gaussian Yaw Min", 0, 0, 1, .01, this.gaussian);
         this.gaussianYawMax = number(module, "Aim Gaussian Yaw Max", 0, 0, 1, .01, this.gaussian);
         this.gaussianPitchMin = number(module, "Aim Gaussian Pitch Min", 0, 0, 1, .01, this.gaussian);
         this.gaussianPitchMax = number(module, "Aim Gaussian Pitch Max", 0, 0, 1, .01, this.gaussian);
         this.gaussianChance = number(module, "Aim Gaussian Chance", 100, 0, 100, 1, this.gaussian);
         this.gaussianSpeedMin = number(module, "Aim Gaussian Speed Min", .1, .01, 1, .01, this.gaussian);
         this.gaussianSpeedMax = number(module, "Aim Gaussian Speed Max", .2, .01, 1, .01, this.gaussian);
         this.gaussianTolerance = number(module, "Aim Gaussian Tolerance", .05, .01, .1, .01, this.gaussian);
         this.smoothing = new ModeSetting("Rotation Smoothing", module, "Linear", new String[]{"Linear", "Sigmoid", "Acceleration"});
         this.yawMin = number(module, "Rotation Yaw Speed Min", 180, 0, 180, 1, null);
         this.yawMax = number(module, "Rotation Yaw Speed Max", 180, 0, 180, 1, null);
         this.pitchMin = number(module, "Rotation Pitch Speed Min", 180, 0, 180, 1, null);
         this.pitchMax = number(module, "Rotation Pitch Speed Max", 180, 0, 180, 1, null);
         this.steepness = number(module, "Rotation Sigmoid Steepness", 10, 0, 20, .1, null);
         this.midpoint = number(module, "Rotation Sigmoid Midpoint", .3, 0, 1, .01, null);
         this.steepness.setVisible(() -> this.smoothing.is("Sigmoid"));
         this.midpoint.setVisible(() -> this.smoothing.is("Sigmoid"));
         this.yawMin.setVisible(() -> !this.smoothing.is("Acceleration"));
         this.yawMax.setVisible(() -> !this.smoothing.is("Acceleration"));
         this.pitchMin.setVisible(() -> !this.smoothing.is("Acceleration"));
         this.pitchMax.setVisible(() -> !this.smoothing.is("Acceleration"));
         this.yawAccelMin = number(module, "Rotation Yaw Acceleration Min", 20, 1, 180, 1, null);
         this.yawAccelMax = number(module, "Rotation Yaw Acceleration Max", 25, 1, 180, 1, null);
         this.pitchAccelMin = number(module, "Rotation Pitch Acceleration Min", 20, 1, 180, 1, null);
         this.pitchAccelMax = number(module, "Rotation Pitch Acceleration Max", 25, 1, 180, 1, null);
         this.accelerationError = new BooleanSetting("Rotation Acceleration Error", module, true);
         this.constantError = new BooleanSetting("Rotation Constant Error", module, true);
         this.sigmoidDeceleration = new BooleanSetting("Rotation Sigmoid Deceleration", module, false);
         this.accelErrorYaw = number(module, "Rotation Yaw Accel Error", .1, .01, 1, .01, this.accelerationError);
         this.accelErrorPitch = number(module, "Rotation Pitch Accel Error", .1, .01, 1, .01, this.accelerationError);
         this.constantErrorYaw = number(module, "Rotation Yaw Constant Error", .1, .01, 1, .01, this.constantError);
         this.constantErrorPitch = number(module, "Rotation Pitch Constant Error", .1, .01, 1, .01, this.constantError);
         for (Setting setting : List.of(this.yawAccelMin, this.yawAccelMax, this.pitchAccelMin, this.pitchAccelMax, this.accelerationError, this.constantError, this.sigmoidDeceleration)) setting.setVisible(() -> this.smoothing.is("Acceleration"));
         this.accelErrorYaw.setVisible(() -> this.smoothing.is("Acceleration") && this.accelerationError.getValue());
         this.accelErrorPitch.setVisible(() -> this.smoothing.is("Acceleration") && this.accelerationError.getValue());
         this.constantErrorYaw.setVisible(() -> this.smoothing.is("Acceleration") && this.constantError.getValue());
         this.constantErrorPitch.setVisible(() -> this.smoothing.is("Acceleration") && this.constantError.getValue());
         this.steepness.setVisible(() -> this.smoothing.is("Sigmoid") || this.smoothing.is("Acceleration") && this.sigmoidDeceleration.getValue());
         this.midpoint.setVisible(() -> this.smoothing.is("Sigmoid") || this.smoothing.is("Acceleration") && this.sigmoidDeceleration.getValue());
         this.resetTicks = number(module, "Rotation Ticks Until Reset", 5, 1, 30, 1, null);
         this.resetThreshold = number(module, "Rotation Reset Threshold", 2, 1, 180, 1, null);
         this.movementCorrection = new ModeSetting("Rotation Movement Correction", module, "Silent", new String[]{"Off", "Silent", "Strict", "Change Look"});
      }
      private static NumberSetting number(Feature module, String name, double value, double min, double max, double step, BooleanSetting toggle) {
         var setting = new NumberSetting(name, module, value, min, max, step);
         if (toggle != null) setting.setVisible(toggle::getValue);
         return setting;
      }
      private static double sample(NumberSetting minimum, NumberSetting maximum) { return AutoRod.random(minimum.getValue(), maximum.getValue()); }

      Rotation linear(LivingEntity entity) {
         Vec3 eye = mc.player.getEyePosition();
         Vec3 predicted = prediction(entity).apply(1);
         AABB box = entity.getBoundingBox().move(predicted.subtract(entity.position())).inflate(entity.getPickRadius());
         var points = projectedPoints(eye, box);
         Vec3 closest = points.stream().min(Comparator.comparingDouble(eye::distanceToSqr)).orElse(nearest(box, eye));
         Vec3 point = points.stream().filter(p -> !exempt(box, closest, p))
            .min(Comparator.comparingDouble(eye::distanceToSqr)).orElse(closest);
         if (this.delay.getValue()) {
            if (this.delayedPoint == null) {
               this.delayedPoint = point; this.pointDelay = AutoRod.randomTicks(this.delayMin.getValue(), this.delayMax.getValue());
            } else if (!point.equals(this.delayedPoint)) {
               Vec3 old = this.delayedPoint;
               if (--this.pointDelay <= 0) {
                  this.delayedPoint = point; this.pointDelay = AutoRod.randomTicks(this.delayMin.getValue(), this.delayMax.getValue());
               }
               point = old;
            }
         }
         if (this.lazy.getValue()) {
            if (this.lazyPoint == null || point.distanceToSqr(this.lazyPoint) >= this.lazyThreshold * this.lazyThreshold) {
               this.lazyPoint = point; this.lazyThreshold = sample(this.lazyMin, this.lazyMax);
            }
            point = this.lazyPoint;
         }
         if (this.gaussian.getValue()) {
            double yaw = sample(this.gaussianYawMin, this.gaussianYawMax), pitch = sample(this.gaussianPitchMin, this.gaussianPitchMax);
            if (yaw > 0 && pitch > 0 && this.gaussianChance.getValue() > 0) {
               if (this.gaussianOffset.distanceToSqr(this.gaussianTarget) < Math.pow(this.gaussianTolerance.getValue(), 2)) {
                  var random = java.util.concurrent.ThreadLocalRandom.current();
                  if (random.nextDouble(100) < this.gaussianChance.getValue()) this.gaussianTarget = new Vec3(
                     random.nextGaussian(.00942273861037109, .23319837528201348) * yaw,
                     random.nextGaussian(-.30075078007595923, .3492437109081718) * pitch,
                     random.nextGaussian(.013282929419023442, .24453708645460387) * yaw);
               } else {
                  this.gaussianOffset = new Vec3(
                     Mth.lerp(sample(this.gaussianSpeedMin, this.gaussianSpeedMax), this.gaussianOffset.x, this.gaussianTarget.x),
                     Mth.lerp(sample(this.gaussianSpeedMin, this.gaussianSpeedMax), this.gaussianOffset.y, this.gaussianTarget.y),
                     Mth.lerp(sample(this.gaussianSpeedMin, this.gaussianSpeedMax), this.gaussianOffset.z, this.gaussianTarget.z));
               }
            }
            point = nearest(box, point.add(this.gaussianOffset));
         }
         return lookingAt(eye, point);
      }
      private boolean exempt(AABB box, Vec3 nearest, Vec3 point) {
         double third = box.getYsize() / 3;
         boolean parts = this.exemptParts.contains("Head") && point.y > box.maxY - third
            || this.exemptParts.contains("Body") && point.y >= box.minY + third && point.y <= box.maxY - third
            || this.exemptParts.contains("Feet") && point.y < box.minY + third;
         return parts || this.exemptNearest.getValue()
            && point.subtract(nearest).horizontalDistance() < this.exemptHorizontal.getValue()
            && Math.abs(point.y - nearest.y) < this.exemptVertical.getValue();
      }
      Rotation projectile(LivingEntity entity) {
         var dimensions = entity.getDimensions(entity.getPose());
         return projectile(mc.player.getEyePosition(), prediction(entity), dimensions.width(), dimensions.height(),
            (from, to) -> mc.level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity)).getType() == HitResult.Type.MISS);
      }

      static Rotation projectile(Vec3 eye, DoubleFunction<Vec3> position, double width, double height, BiPredicate<Vec3, Vec3> visible) {
         Vec3 base = position.apply(0);
         if (base.distanceToSqr(eye) < 25) {
            Vec3 difference = position.apply(base.distanceTo(eye) / HOOK_SPEED).subtract(eye);
            double distance = difference.horizontalDistance(), velocitySquared = 2.25, gravity = .03;
            double discriminant = velocitySquared * velocitySquared - gravity * (gravity * distance * distance + 2 * difference.y * velocitySquared);
            if (discriminant < 0 || distance < 1e-8) return null;
            return new Rotation(lookingAt(Vec3.ZERO, difference).yaw(),
               Mth.wrapDegrees((float)-Math.toDegrees(Math.atan((velocitySquared - Math.sqrt(discriminant)) / (gravity * distance)))));
         }
         Vec3 offset = new Vec3(width * .5, height * .5, width * .5);
         double lower = 0, upper = base.distanceTo(eye) / HOOK_SPEED * 1.75;
         while (upper - lower > 1e-4) {
            double middle = (lower + upper) * .5;
            double left = Math.abs(directionByTime(eye, position.apply((lower + middle) * .5).add(offset), (lower + middle) * .5).length() - 1);
            double right = Math.abs(directionByTime(eye, position.apply((middle + upper) * .5).add(offset), (middle + upper) * .5).length() - 1);
            if (left < right) upper = middle; else lower = middle;
         }
         double time = (lower + upper) * .5;
         Vec3 impact = position.apply(time);
         Vec3 direction = directionByTime(eye, impact.add(offset), time);
         if (Math.abs(direction.length() - 1) > .1) return null;
         double dragPower = Math.pow(HOOK_DRAG, time), dragLog = Math.log(HOOK_DRAG), resistance = HOOK_DRAG - 1;
         Vec3 incoming = new Vec3(direction.x * dragPower * dragLog * HOOK_SPEED / resistance,
            (direction.y * resistance * dragPower * dragLog * HOOK_SPEED - HOOK_GRAVITY * (dragPower * dragLog - HOOK_DRAG + 1)) / (resistance * resistance),
            direction.z * dragPower * dragLog * HOOK_SPEED / resistance).normalize();
         Vec3 virtualEye = eye.add(0, -incoming.y * eye.distanceTo(impact), 0);
         AABB box = new AABB(impact.x - width * .5, impact.y, impact.z - width * .5,
            impact.x + width * .5, impact.y + height, impact.z + width * .5).inflate(.25);
         var points = projectedPoints(virtualEye, box);
         points.sort(Comparator.comparingDouble(box.getCenter()::distanceToSqr));
         for (Vec3 point : points) {
            Vec3 vector = point.subtract(virtualEye);
            Vec3 start = point.subtract(vector.normalize().scale(5));
            if (visible.test(start, point)) return lookingAt(Vec3.ZERO, directionByTime(eye, point, Math.rint(time)));
         }
         return null;
      }
      static Vec3 directionByTime(Vec3 eye, Vec3 target, double time) {
         double power = Math.pow(HOOK_DRAG, time), denominator = HOOK_SPEED * (power - 1);
         Vec3 delta = target.subtract(eye);
         return new Vec3(delta.x * (HOOK_DRAG - 1) / denominator,
            delta.y * (HOOK_DRAG - 1) / denominator + HOOK_GRAVITY * (power - HOOK_DRAG * time + time - 1) / ((HOOK_DRAG - 1) * denominator),
            delta.z * (HOOK_DRAG - 1) / denominator);
      }
      private static DoubleFunction<Vec3> prediction(LivingEntity entity) {
         Vec3 base = entity.position(), velocity = base.subtract(new Vec3(entity.xo, entity.yo, entity.zo));
         if (!(entity instanceof Player)) return ticks -> base.add(velocity.scale(ticks));
         var snapshots = new ArrayList<Vec3>(); snapshots.add(base);
         return ticks -> {
            int requested = (int)Math.max(0, Math.rint(Math.min(ticks, 30)));
            while (snapshots.size() <= requested) {
               int step = snapshots.size();
               Vec3 position = snapshots.get(step - 1);
               double vertical = entity.onGround() || entity.isInWater() || entity.isFallFlying() ? velocity.y
                  : (velocity.y + 3.92) * Math.pow(.98, step - 1) - 3.92;
               Vec3 movement = new Vec3(velocity.x, vertical, velocity.z);
               AABB box = entity.getBoundingBox().move(position.subtract(base));
               Vec3 allowed = Entity.collideBoundingBox(entity, movement, box, entity.level(), List.of());
               snapshots.add(position.add(allowed));
            }
            return snapshots.get(requested);
         };
      }

      static Rotation lookingAt(Vec3 eye, Vec3 point) {
         Vec3 delta = point.subtract(eye);
         return new Rotation(Mth.wrapDegrees((float)(Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90)),
            Mth.wrapDegrees((float)-Math.toDegrees(Math.atan2(delta.y, delta.horizontalDistance()))));
      }
      static Vec3 nearest(AABB box, Vec3 point) {
         return new Vec3(Mth.clamp(point.x, box.minX, box.maxX), Mth.clamp(point.y, box.minY, box.maxY), Mth.clamp(point.z, box.minZ, box.maxZ));
      }
      static List<Vec3> projectedPoints(Vec3 eye, AABB box) {
         var result = new ArrayList<Vec3>();
         if (box.contains(eye)) return result;
         Vec3 normal = box.getCenter().subtract(eye).normalize();
         var vertices = new ArrayList<Vec3>();
         for (double x : new double[]{box.minX, box.maxX}) for (double y : new double[]{box.minY, box.maxY}) for (double z : new double[]{box.minZ, box.maxZ}) vertices.add(new Vec3(x, y, z));
         Vec3 origin = vertices.stream().map(point -> eye.add(normal.scale(point.subtract(eye).dot(normal))))
            .min(Comparator.comparingDouble(eye::distanceToSqr)).orElse(box.getCenter()).lerp(eye, .1);
         float yaw = (float)Math.atan2(normal.z, normal.x), pitch = (float)Math.atan2(normal.y, normal.horizontalDistance());
         var to = new Matrix3f().rotateY(-yaw).mul(new Matrix3f().rotateZ(pitch));
         var back = new Matrix3f().rotateZ(-pitch).mul(new Matrix3f().rotateY(yaw));
         float minY = 0, maxY = 0, minZ = 0, maxZ = 0;
         for (Vec3 point : vertices) {
            Vec3 ray = point.subtract(eye);
            Vec3 projection = eye.add(ray.scale(origin.subtract(eye).dot(normal) / ray.dot(normal))).subtract(origin);
            var transformed = new Vector3f((float)projection.x, (float)projection.y, (float)projection.z).mul(back);
            minY = Math.min(minY, transformed.y); maxY = Math.max(maxY, transformed.y);
            minZ = Math.min(minZ, transformed.z); maxZ = Math.max(maxZ, transformed.z);
         }
         Vec3 corner = vector(new Vector3f(0, minY, minZ).mul(to)).add(origin);
         Vec3 dy = vector(new Vector3f(0, maxY - minY, 0).mul(to));
         Vec3 dz = vector(new Vector3f(0, 0, maxZ - minZ).mul(to));
         double aspect = dz.length() / dy.length();
         double stepY = dy.lengthSqr() < 1e-12 ? 1 : dz.lengthSqr() < 1e-12 ? 2.0 / 128 : Math.sqrt(aspect / 128);
         double stepZ = dz.lengthSqr() < 1e-12 ? 1 : dy.lengthSqr() < 1e-12 ? 2.0 / 128 : Math.sqrt(1 / (aspect * 128));
         for (double y = 0; y <= 1; y += stepY) for (double z = 0; z <= 1; z += stepZ) {
            Vec3 planePoint = corner.add(dy.scale(y)).add(dz.scale(z));
            box.clip(eye, planePoint.lerp(eye, -100)).ifPresent(result::add);
         }
         return result;
      }
      private static Vec3 vector(Vector3f vector) { return new Vec3(vector.x, vector.y, vector.z); }

      Rotation turn(Rotation current, Rotation desired) {
         this.remainingRotationTicks = (int)this.resetTicks.getValue();
         return smooth(current, desired);
      }
      private Rotation smooth(Rotation current, Rotation desired) {
         double horizontal = sample(this.yawMin, this.yawMax), vertical = sample(this.pitchMin, this.pitchMax);
         if (this.smoothing.is("Sigmoid")) {
            double difference = Math.min(180, Math.hypot(Mth.wrapDegrees(desired.yaw - current.yaw), Mth.wrapDegrees(desired.pitch - current.pitch)));
            double sigmoid = 1 / (1 + Math.exp(-this.steepness.getValue() * (difference / 120 - this.midpoint.getValue())));
            horizontal *= sigmoid; vertical *= sigmoid;
         }
         Rotation turned = turnLinear(current, desired, horizontal, vertical);
         if (this.smoothing.is("Acceleration")) {
            float dy = Mth.wrapDegrees(desired.yaw - current.yaw), dp = Mth.wrapDegrees(desired.pitch - current.pitch);
            double factor = this.sigmoidDeceleration.getValue() ? 1 / (1 + Math.exp(-this.steepness.getValue() * (Math.hypot(dy, dp) / 120 - this.midpoint.getValue()))) : 1;
            double ay = Mth.clamp(Mth.wrapDegrees(dy - this.previousYawSpeed), -sample(this.yawAccelMin, this.yawAccelMax), sample(this.yawAccelMin, this.yawAccelMax)) * factor;
            double ap = Mth.clamp(Mth.wrapDegrees(dp - this.previousPitchSpeed), -sample(this.pitchAccelMin, this.pitchAccelMax), sample(this.pitchAccelMin, this.pitchAccelMax)) * factor;
            double y = this.previousYawSpeed + ay, p = this.previousPitchSpeed + ap;
            if (this.accelerationError.getValue()) { y += ay * AutoRod.random(-this.accelErrorYaw.getValue(), this.accelErrorYaw.getValue()); p += ap * AutoRod.random(-this.accelErrorPitch.getValue(), this.accelErrorPitch.getValue()); }
            if (this.constantError.getValue()) { y += AutoRod.random(-this.constantErrorYaw.getValue(), this.constantErrorYaw.getValue()); p += AutoRod.random(-this.constantErrorPitch.getValue(), this.constantErrorPitch.getValue()); }
            turned = new Rotation(current.yaw + (float)y, Mth.clamp(current.pitch + (float)p, -90, 90));
         }
         double sensitivity = mc.options.sensitivity().get() * .6 + .2;
         double step = sensitivity * sensitivity * sensitivity * 8 * .15;
         var quantized = new Rotation(current.yaw + (float)(Math.round((turned.yaw - current.yaw) / step) * step),
            Mth.clamp(current.pitch + (float)(Math.round((turned.pitch - current.pitch) / step) * step), -90, 90));
         this.previousYawSpeed = Mth.wrapDegrees(quantized.yaw - current.yaw);
         this.previousPitchSpeed = Mth.wrapDegrees(quantized.pitch - current.pitch);
         this.lastRotation = quantized;
         return quantized;
      }
      static Rotation turnLinear(Rotation current, Rotation desired, double horizontal, double vertical) {
         float yawDelta = Mth.wrapDegrees(desired.yaw - current.yaw), pitchDelta = Mth.wrapDegrees(desired.pitch - current.pitch);
         double length = Math.hypot(yawDelta, pitchDelta);
         if (length < 1e-6) return current;
         double yawLimit = Math.abs(yawDelta / length) * horizontal, pitchLimit = Math.abs(pitchDelta / length) * vertical;
         return new Rotation(current.yaw + (float)Mth.clamp(yawDelta, -yawLimit, yawLimit),
            Mth.clamp(current.pitch + (float)Mth.clamp(pitchDelta, -pitchLimit, pitchLimit), -90, 90));
      }
      void applyMovementCorrection() {
         if (this.movementCorrection.is("Change Look")) {
            mc.player.setYRot(Events.ROTATION.getYaw()); mc.player.setXRot(Events.ROTATION.getPitch());
         }
         if (!this.movementCorrection.is("Off")) { Events.ROTATION.setMovementCorrection(true); Events.ROTATION.setUseClientRotation(true); }
      }
      void correctMovement(Rotation rotation) {
         if (rotation == null || !this.movementCorrection.is("Silent")) return;
         var input = (ClientInputAccessor)mc.player.input;
         Vec2 original = input.getMoveVector();
         Vec2 corrected = correctMovement(original, mc.player.getYRot(), rotation.yaw);
         input.setMoveVector(corrected);
         Input keys = mc.player.input.keyPresses;
         mc.player.input.keyPresses = new Input(corrected.y > 0, corrected.y < 0, corrected.x > 0, corrected.x < 0, keys.jump(), keys.shift(), keys.sprint());
      }
      static Vec2 correctMovement(Vec2 input, float cameraYaw, float rotationYaw) {
         if (input.x == 0 && input.y == 0) return input;
         double difference = Math.toRadians(cameraYaw - rotationYaw), cos = Math.cos(difference), sin = Math.sin(difference);
         double forward = input.y * cos + input.x * sin, sideways = input.x * cos - input.y * sin;
         Vec2 best = Vec2.ZERO;
         double error = Double.POSITIVE_INFINITY;
         for (int forwardInput = -1; forwardInput <= 1; forwardInput++) for (int strafeInput = -1; strafeInput <= 1; strafeInput++) {
            if (forwardInput == 0 && strafeInput == 0) continue;
            Vec2 candidate = new Vec2(strafeInput, forwardInput).normalized();
            double next = Math.pow(candidate.y - forward, 2) + Math.pow(candidate.x - sideways, 2);
            if (next < error) { best = candidate; error = next; }
         }
         return best;
      }
      Rotation returnRotation(Rotation camera) {
         if (this.lastRotation == null) return null;
         if (this.remainingRotationTicks-- > 0) return this.lastRotation;
         if (Math.hypot(Mth.wrapDegrees(camera.yaw - this.lastRotation.yaw), Mth.wrapDegrees(camera.pitch - this.lastRotation.pitch)) <= this.resetThreshold.getValue()) {
            this.lastRotation = null; this.previousYawSpeed = this.previousPitchSpeed = 0; return null;
         }
         return smooth(this.lastRotation, camera);
      }
      void reset() {
         this.delayedPoint = this.lazyPoint = null;
         this.gaussianOffset = this.gaussianTarget = Vec3.ZERO;
         this.pointDelay = 0; this.lazyThreshold = 0;
         this.lastRotation = null; this.previousYawSpeed = this.previousPitchSpeed = 0; this.remainingRotationTicks = 0;
      }
   }

   static final class Cycle<T> {
      private T target;
      private int elapsed, timeout, cooldown;
      private double minRangeSquared, maxRangeSquared;
      private boolean waiting;
      boolean ready() { return !this.waiting && this.cooldown == 0; }
      boolean waiting() { return this.waiting; }
      boolean timedOut() { return this.waiting && this.elapsed >= this.timeout; }
      T target() { return this.target; }
      void start(T target, int timeout, double minimumRange, double maximumRange) {
         this.target = target; this.timeout = timeout;
         this.minRangeSquared = minimumRange * minimumRange;
         this.maxRangeSquared = maximumRange * maximumRange;
         this.elapsed = 0; this.waiting = true;
      }
      boolean tick(boolean hooked, boolean stopped, boolean pullOutOfRange, double distanceSquared) {
         if (!this.waiting) {
            if (this.cooldown > 0) --this.cooldown;
            return false;
         }
         return ++this.elapsed >= this.timeout || hooked || stopped
            || pullOutOfRange && (distanceSquared < this.minRangeSquared || distanceSquared > this.maxRangeSquared);
      }
      void pulled(int cooldown) { this.waiting = false; this.target = null; this.cooldown = cooldown; }
      void reset() { this.target = null; this.elapsed = this.timeout = this.cooldown = 0; this.waiting = false; }
   }

   static final class Slot {
      private int slot = -1, elapsed, duration;
      int selected(int realSlot) { return this.slot == -1 ? realSlot : this.slot; }
      int slot() { return this.slot; }
      void select(int slot, int duration) { this.slot = slot; this.duration = duration; this.elapsed = 0; }
      boolean tick() {
         if (this.slot == -1 || this.elapsed++ < this.duration) return false;
         clear(); return true;
      }
      void clear() { this.slot = -1; this.elapsed = this.duration = 0; }
   }
}
