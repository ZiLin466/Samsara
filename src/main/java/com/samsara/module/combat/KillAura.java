package com.samsara.module.combat;

import com.samsara.util.RotationUtil;
import com.mojang.blaze3d.platform.InputConstants;
import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.event.impl.EventPacketSend;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.module.FeatureManager;
import com.samsara.setting.BooleanSetting;
import com.samsara.setting.ModeSetting;
import com.samsara.setting.NumberSetting;
import com.samsara.util.InventoryUtil;
import com.samsara.util.PacketBlinkQueue;
import com.samsara.util.TargetFinder;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.Fireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.sdl.SDLMouse;

public class KillAura extends Feature {
   private static final String WATCHDOG_LABEL = "Watchdog";
   private static final String AUTO_BLOCK_RMB_LABEL = "AutoBlock RMB";
   private static final String ATTACK_TEAMMATES_LABEL = "Attack Teammates";
   private static final String WATCHDOG2_LABEL = "Watchdog2";
   private static final String ROTATIONS_LABEL = "Rotations";
   private static final String CYCLE_LABEL = "Cycle";
   private static final String KILL_AURA_LABEL = "KillAura";
   private static final String ATTACK_DELAY_LABEL = "Attack Delay";
   private static final String NONE_LABEL = "None";
   private static final String ATTACK_COOLDOWN_LABEL = "Attack Cooldown";
   private static final String ATTACK_DELAY2_LABEL = "Attack Delay2";
   private static final String ROTATION_RANGE_LABEL = "Rotation Range";
   private static final String VANILLA_LABEL = "Vanilla";
   private static final String REQUIRE_SWORD_LABEL = "Require Sword";
   private static final String HEAD_LABEL = "Head";
   private static final String SWAP_LABEL = "Swap";
   private static final String OPTIMAL_LABEL = "Optimal";
   private static final String IGNORE_SHIELD_LABEL = "Ignore Shield";
   private static final String AUTO_BLOCK_LABEL = "AutoBlock";
   private static final String SWAP2_LABEL = "Swap2";
   private static final String ATTACK_FIREBALLS_LABEL = "Attack Fireballs";

   private final ModeSetting rotations = new ModeSetting(ROTATIONS_LABEL, this, HEAD_LABEL, new String[]{HEAD_LABEL, OPTIMAL_LABEL});
   private final ModeSetting autoBlock;
   private final BooleanSetting predict;
   private final NumberSetting attackDelay;
   private final NumberSetting attackDelay2;
   private final NumberSetting rotationRange;
   private final NumberSetting wallRange;
   private final BooleanSetting attackCooldown;
   private final BooleanSetting ignoreShield;
   private final BooleanSetting requireSword;
   private final BooleanSetting autoBlockRmb;
   private final BooleanSetting attackTeammates;
   private final BooleanSetting attackFireballs;

   private final AutoBlockController blocking = new AutoBlockController();
   private final BlockPrediction prediction = new BlockPrediction();

   private float targetPitch;
   private float targetYaw;
   private int attackTicks;
   private boolean alternateAttackDelay;
   private Entity target;

   private void attackTarget(float yaw, float pitch, boolean interactAfterAttack) {
      if (!FeatureManager.bedAura.rotatingToBed) {
         double hitDistance = this.raycastDistance(this.target, yaw, pitch, 10.0);
         if (hitDistance <= 3.0 && hitDistance >= 0.0) {
            mc.gameMode.attack(mc.player, this.target);
            mc.player.swing(InteractionHand.MAIN_HAND, mc.player.getMainHandItem().getAttackAnimation(), false);
            // 26.3 的 swing 只播放本地动画；原版 startAttack 在之后单独发送 Punch。
            mc.getConnection().send(ServerboundPunchPacket.INSTANCE);
            if (interactAfterAttack) {
               mc.getConnection().send(new ServerboundInteractPacket(this.target.getId(), InteractionHand.MAIN_HAND, Vec3.ZERO, false));
            }
         }
      }
   }

   @Override
   public int getPriority(Event event) {
      return event == Events.ROTATION ? -2 : 0;
   }

   private void resetCombat() {
      this.target = null;
      resetBlockPrediction();
      this.blocking.stop();
   }

   @Override
   public void onEvent(Event event) {
      if (mc.player == null || mc.level == null || mc.getConnection() == null) {
         this.resetCombat();
         return;
      }
      if ((event == Events.ROTATION || event == Events.POST_MOTION) && (mc.gui.screen() != null || !mc.isWindowActive())) {
         this.resetCombat();
         return;
      }
      if ((event == Events.ROTATION || event == Events.POST_MOTION) && FeatureManager.autoRod != null
          && FeatureManager.autoRod.isCombatHandReserved()) {
         this.prepareForAutoRod();
         return;
      }

      if (event == Events.MOUSE_BUTTON) this.handleMouseButton(event);
      else if (event == Events.ROTATION) this.updateCombatRotation();
      else if (event == Events.POST_MOTION) this.blocking.finishMotion();
      else if (this.predict.getValue() && (event == Events.MOVE_INPUT || event == Events.TICK || event == Events.RENDER_2D)) {
         refreshPredictAutoBlock();
      } else if (event instanceof EventPacketSend sending && sending.getPacket() instanceof ServerboundPlayerActionPacket && this.blocking.suppressesActionPackets()) {
         event.setCancelled(true);
      }
   }

   private void handleMouseButton(Event event) {
      if (this.autoBlockRmb.getValue()
          && Events.MOUSE_BUTTON.getButton() == InputConstants.MOUSE_BUTTON_RIGHT && Events.MOUSE_BUTTON.isReleased()
          && (!this.predict.getValue() || !this.prediction.hasThreat())) {
         this.blocking.stop();
      }
      if (this.predict.getValue() && Events.MOUSE_BUTTON.getButton() == InputConstants.MOUSE_BUTTON_RIGHT) {
         refreshPredictAutoBlock();
      }
      if (mc.gui.screen() == null && this.target != null) event.setCancelled(true);
   }

   private void updateCombatRotation() {
      updateBlockPrediction();
      this.blocking.stopInvalidAutoBlock();

      this.setSuffix(this.autoBlock.is(NONE_LABEL) ? null : this.autoBlock.getValue());
      this.blocking.beginRotation();
      if (!this.attackCooldown.getValue() || mc.player.getAttackStrengthScale(0.0F) >= 1.0F) {
         this.attackTicks++;
      }

      this.target = TargetFinder.nearest(this.rotationRange.getValue(), true, !this.attackTeammates.getValue(), entity -> {
         this.updateTargetRotation(entity);
         // Keep distant visible targets for rotations; reject occluded targets outside the wall range.
         return this.raycastDistance(entity, this.targetYaw, this.targetPitch, this.rotationRange.getValue()) >= 0;
      });
      if (this.target == null && this.prediction.hasThreat()) this.target = this.prediction.threatTarget();

      if (this.attackFireballIfPresent()) return;

      if (this.requireSword.getValue() && !InventoryUtil.isHoldingSword()) {
         this.target = null;
      }

      if (this.target == null) {
         this.resetCombat();
         return;
      }
      this.attackSelectedTarget();
   }

   private boolean attackFireballIfPresent() {
      if (!this.attackFireballs.getValue()) return false;
      Entity entity = this.findFireball(3.0);
      if (entity != null) {
         this.updateTargetRotation(entity);
         Events.ROTATION.setYaw(this.targetYaw);
         Events.ROTATION.setPitch(this.targetPitch);
         mc.gameMode.attack(mc.player, entity);
         mc.player.swing(InteractionHand.MAIN_HAND, mc.player.getMainHandItem().getAttackAnimation(), false);
         mc.getConnection().send(ServerboundPunchPacket.INSTANCE);
         this.resetCombat();
         return true;
      }

      return false;
   }

   private void attackSelectedTarget() {
      this.updateTargetRotation(this.target);
      Events.ROTATION.setYaw(this.targetYaw);
      Events.ROTATION.setPitch(this.targetPitch);
      double attackInterval = this.alternateAttackDelay ? this.attackDelay2.getValue() : this.attackDelay.getValue();
      if (!FeatureManager.velocity.blocksAttacks() && (double)this.attackTicks > attackInterval && (!this.attackCooldown.getValue() || mc.player.getAttackStrengthScale(0.0F) >= 1.0F)) {
         if (this.ignoreShield.getValue() && this.target instanceof Player player && player.isUsingItem() && player.getUseItem().is(Items.SHIELD)) {
            return;
         }

         if (!this.blocking.usesAutoBlockAttackCycle() || !this.canAutoBlock() && !this.isServerBlocking()) {
            this.attackTicks = 0;
            this.attackTarget(this.targetYaw, this.targetPitch, false);
         }

         this.alternateAttackDelay = !this.alternateAttackDelay;
      }

      if (!this.autoBlock.is(NONE_LABEL) && mc.player.getMainHandItem().is(ItemTags.SWORDS) && this.canAutoBlock()) {
         mc.options.keyUse.setDown(false);
         this.blocking.updateMode();
         if (this.predict.getValue()) this.blocking.synchronizePrediction();
      } else {
         this.blocking.stop();
      }
   }

   public boolean hasAutoBlockAnimation() { return this.blocking.hasAnimation(); }

   public boolean hasAutoBlockMode() { return !this.autoBlock.is(NONE_LABEL); }

   public boolean isAutoBlocking() { return this.blocking.isBlocking(); }

   public boolean isServerBlocking() { return this.blocking.serverBlocking; }

   public Entity getTarget() { return this.target; }

   public boolean prepareForAutoRod() {
      if (!isEnabled()) return true;
      if (mc.player == null || mc.getConnection() == null) return false;
      resetBlockPrediction();
      this.blocking.stop();
      return true;
   }

   private void resetBlockPrediction() {
      this.prediction.reset();
      this.blocking.resetPrediction();
   }

   public void onPredictEntityUpdate(Entity entity, boolean rotationUpdated, boolean positionUpdated, boolean teleport) {
      if (!isEnabled() || !this.predict.getValue() || !hasAutoBlockMode() || mc.player == null || mc.level == null
          || !(entity instanceof Player)) return;
      this.prediction.recordEntityUpdate(entity, rotationUpdated, positionUpdated, teleport);
      refreshPredictAutoBlock();
   }

   public void onPredictHeadUpdate(Entity entity, float yaw) {
      if (!isEnabled() || !this.predict.getValue() || !hasAutoBlockMode() || mc.player == null || mc.level == null
          || !(entity instanceof Player)) return;
      this.prediction.recordHeadUpdate(entity, yaw);
      refreshPredictAutoBlock();
   }

   private void refreshPredictAutoBlock() {
      if (mc.player == null || mc.level == null || mc.getConnection() == null || !isAutoBlockInputAllowed()) {
         resetBlockPrediction();
         this.blocking.stop();
         return;
      }
      updateBlockPrediction();
      this.blocking.synchronizePrediction();
   }

   private void updateBlockPrediction() {
      if (!this.predict.getValue() || this.autoBlock.is(NONE_LABEL)) {
         this.resetBlockPrediction();
         return;
      }
      this.prediction.update();
      Player threat = this.prediction.threatTarget();
      if (threat != null && (this.target == null || this.target.isRemoved() || !this.target.isAlive())) this.target = threat;
   }

   private boolean canAutoBlock() {
      return isAutoBlockInputAllowed() && shouldAutoBlock(this.predict.getValue(), this.autoBlockRmb.getValue(),
         isManualAutoBlockRequested(), this.prediction.hasThreat());
   }

   static boolean shouldAutoBlock(boolean predict, boolean rmbEnabled, boolean rightMouseDown, boolean threat) {
      return predict ? threat || rmbEnabled && rightMouseDown : !rmbEnabled || rightMouseDown;
   }

   private boolean isManualAutoBlockRequested() {
      return this.autoBlockRmb.getValue() && (SDLMouse.SDL_GetMouseState((java.nio.FloatBuffer)null,
         (java.nio.FloatBuffer)null) & SDLMouse.SDL_BUTTON_RMASK) != 0;
   }

   public boolean isAutoBlockInputAllowed() {
      if (mc == null || mc.gui.screen() != null || !mc.isWindowActive()) return false;
      if (FeatureManager.autoRod != null && FeatureManager.autoRod.isCombatHandReserved()) return false;
      // MouseHandler retains its last gameplay button state while a Screen handles input.
      return this.predict.getValue() && hasAutoBlockMode() || !this.autoBlockRmb.getValue() || isManualAutoBlockRequested();
   }

   private Entity findFireball(double range) {
      AABB searchBounds = mc.player.getBoundingBox().inflate(range);
      Entity nearestFireball = null;
      double nearestDistance = Double.MAX_VALUE;

      for (Entity entity : mc.level.getEntities(mc.player, searchBounds)) {
         if (entity instanceof Fireball || entity instanceof SmallFireball || entity instanceof LargeFireball) {
            this.updateTargetRotation(entity);
            double hitDistance = this.raycastDistance(entity, this.targetYaw, this.targetPitch, 10.0);
            if (hitDistance >= 0.0 && hitDistance <= range && hitDistance < nearestDistance) {
               nearestFireball = entity;
               nearestDistance = hitDistance;
            }
         }
      }

      return nearestFireball;
   }

   @Override
   public void onDisable() {
      this.resetCombat();
   }

   private void updateTargetRotation(Entity entity) {
      double targetX;
      double targetY;
      double targetZ;
      if (this.rotations.is(OPTIMAL_LABEL)) {
         AABB bounds = entity.getBoundingBox();
         targetX = Mth.clamp(mc.player.getX(), bounds.minX, bounds.maxX);
         targetY = Mth.clamp(mc.player.getEyeY(), bounds.minY, bounds.maxY);
         targetZ = Mth.clamp(mc.player.getZ(), bounds.minZ, bounds.maxZ);
      } else {
         targetX = entity.getX();
         targetY = entity.getEyeY();
         targetZ = entity.getZ();
      }

      double deltaX = targetX - mc.player.getX();
      double deltaY = targetY - mc.player.getEyeY();
      double deltaZ = targetZ - mc.player.getZ();
      double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
      float yaw = (float)Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0F;
      float pitch = (float)(-Math.toDegrees(Math.atan2(deltaY, horizontalDistance)));
      yaw = mc.player.getYRot() + Mth.wrapDegrees(yaw - mc.player.getYRot());
      float sensitivityStep = RotationUtil.mouseSensitivityStep(mc.options.sensitivity().get().floatValue());
      this.targetYaw = RotationUtil.quantize(yaw, mc.player.getYRot(), sensitivityStep);
      this.targetPitch = RotationUtil.quantize(pitch, mc.player.getXRot(), sensitivityStep);
   }

   public KillAura() {
      super(KILL_AURA_LABEL, Category.COMBAT);
      this.autoBlock = new ModeSetting(AUTO_BLOCK_LABEL, this, NONE_LABEL, new String[]{NONE_LABEL, VANILLA_LABEL, WATCHDOG_LABEL, WATCHDOG2_LABEL, SWAP_LABEL, SWAP2_LABEL, CYCLE_LABEL});
      this.predict = new BooleanSetting("Predict", this, false);
      this.predict.setVisible(() -> !this.autoBlock.is(NONE_LABEL));
      this.attackDelay = new NumberSetting(ATTACK_DELAY_LABEL, this, 3.0, 0.0, 10.0, 1.0);
      this.attackDelay2 = new NumberSetting(ATTACK_DELAY2_LABEL, this, 3.0, 0.0, 10.0, 1.0);
      this.rotationRange = new NumberSetting(ROTATION_RANGE_LABEL, this, 5.0, 3.0, 10.0, 0.5);
      this.wallRange = new NumberSetting("Wall Range", this, 0.0, 0.0, 3.0, 0.1);
      this.attackCooldown = new BooleanSetting(ATTACK_COOLDOWN_LABEL, this, true);
      this.ignoreShield = new BooleanSetting(IGNORE_SHIELD_LABEL, this, false);
      this.requireSword = new BooleanSetting(REQUIRE_SWORD_LABEL, this, false);
      this.autoBlockRmb = new BooleanSetting(AUTO_BLOCK_RMB_LABEL, this, false);
      this.attackTeammates = new BooleanSetting(ATTACK_TEAMMATES_LABEL, this, true);
      this.attackFireballs = new BooleanSetting(ATTACK_FIREBALLS_LABEL, this, false);
   }

   private double raycastDistance(Entity entity, float yaw, float pitch, double range) {
      Vec3 origin = mc.player.getEyePosition(1.0F);
      return raycastDistance(origin, RotationUtil.lookVector(yaw, pitch), entity.getBoundingBox(), range, this.wallRange.getValue(),
         hit -> mc.level.clip(new ClipContext(origin, hit, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player))
            .getType() != HitResult.Type.MISS);
   }

   static double raycastDistance(Vec3 origin, Vec3 direction, AABB target, double range, double wallRange, Predicate<Vec3> occluded) {
      var hit = target.contains(origin) ? java.util.Optional.of(origin) : target.clip(origin, origin.add(direction.scale(range)));
      if (hit.isEmpty()) return -0.1;
      double distance = origin.distanceTo(hit.get());
      return distance > wallRange && occluded.test(hit.get()) ? -0.1 : distance;
   }

   private final class AutoBlockController {
      private boolean blinkActive;
      private boolean usingAutoBlockItem;
      private boolean serverSlotChanged;
      private boolean pendingPostMotionBlock;
      private boolean suppressReleasePackets;
      private boolean serverBlocking;
      private boolean autoBlockActive;
      private boolean predictiveBlockActive;
      private boolean alternateSword;
      private int autoBlockTicks;
      private int swapBlockCycleCount;

      private void beginRotation() { this.autoBlockActive = false; }
      private void resetPrediction() { this.predictiveBlockActive = false; }
      private boolean isBlocking() { return this.serverBlocking || this.usingAutoBlockItem; }
      private boolean suppressesActionPackets() { return this.suppressReleasePackets; }

      private void sendBlockPacket() {
         if (KillAura.this.target == null || KillAura.this.autoBlock.is(NONE_LABEL) || !KillAura.this.canAutoBlock()) return;
         mc.getConnection().send(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 0, Events.ROTATION.getYaw(), Events.ROTATION.getPitch()));
         this.serverBlocking = true;
      }

      private void restoreSelectedSlot() {
         if (this.serverSlotChanged) {
            mc.getConnection().send(new ServerboundSetCarriedItemPacket(mc.player.getInventory().getSelectedSlot()));
            this.serverSlotChanged = false;
         }
      }

      private void releaseBlock() {
         this.suppressReleasePackets = false;
         mc.getConnection().send(new ServerboundPlayerActionPacket(Action.RELEASE_USE_ITEM, BlockPos.ZERO, Direction.DOWN));
         this.suppressReleasePackets = true;
         this.serverBlocking = false;
         stopAutoBlockItem();
      }

      private void startAutoBlockItem() {
         mc.player.startUsingItem(InteractionHand.MAIN_HAND);
         this.usingAutoBlockItem = true;
      }

      private void stopAutoBlockItem() {
         if (this.usingAutoBlockItem && mc.player != null) mc.player.stopUsingItem();
         this.usingAutoBlockItem = false;
      }

      private void updateMode() {
         if (!KillAura.this.canAutoBlock()) return;
         this.suppressReleasePackets = false;
         this.autoBlockActive = true;
         switch (KillAura.this.autoBlock.getValue()) {
            case VANILLA_LABEL -> {
               mc.getConnection().send(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 0, mc.player.getYRot(), mc.player.getXRot()));
               this.serverBlocking = true;
            }
            case CYCLE_LABEL -> this.updateCycleBlock();
            case WATCHDOG_LABEL -> this.updateWatchdogBlock(false);
            case WATCHDOG2_LABEL -> this.updateWatchdogBlock(true);
            case SWAP_LABEL -> this.updateSwapBlock();
            case SWAP2_LABEL -> this.updateSwap2Block();
            default -> { }
         }
      }

      private void updateCycleBlock() {
         this.suppressReleasePackets = true;
         this.autoBlockTicks++;
         if (this.autoBlockTicks < 3) {
            return;
         }

         if (this.serverSlotChanged) {
            this.sendSelectedSlot(mc.player.getInventory().getSelectedSlot());
         }

         this.attackAndBlock();
         PacketBlinkQueue.disable();
         this.blinkActive = true;
         if (!this.serverSlotChanged) {
            this.sendSelectedSlot(mc.player.getInventory().getSelectedSlot() % 8 + 1);
         }

         PacketBlinkQueue.enable();
         this.autoBlockTicks = 0;
      }

      private void updateWatchdogBlock(boolean conditionalBlock) {
         this.suppressReleasePackets = true;
         this.autoBlockTicks++;
         if (this.autoBlockTicks < 2) {
            if (this.serverBlocking) startAutoBlockItem();
            return;
         }

         this.releaseBlock();
         if (!conditionalBlock) {
            this.attackAndBlock();
         } else {
            boolean facingTarget = this.isTargetFacingPlayer(KillAura.this.target, 45.0);
            if (KillAura.this.predict.getValue() || mc.player.hurtTime <= 5 && facingTarget && !((double)mc.player.distanceTo(KillAura.this.target) > 3.0)) {
               this.attackAndBlock();
            } else {
               KillAura.this.attackTarget(KillAura.this.targetYaw, KillAura.this.targetPitch, false);
            }
         }
         this.autoBlockTicks = 0;
      }

      private void updateSwapBlock() {
         this.suppressReleasePackets = true;
         this.autoBlockTicks++;
         if (this.autoBlockTicks < 3) {
            if (this.autoBlockTicks != 2 && this.serverBlocking) {
               startAutoBlockItem();
            }

            return;
         }

         SwordSlots slots = this.findSwordSlots();
         if (slots.first() != -1 && slots.second() != -1) {
            int swordSlot = this.alternateSword ? slots.second() : slots.first();
            this.alternateSword = !this.alternateSword;
            this.sendSelectedSlot(swordSlot);
            if (mc.player.getInventory().getItem(swordSlot).is(ItemTags.SWORDS)) {
               this.attackAndBlock();
            }

            this.autoBlockTicks = 0;
         } else if (mc.player.getInventory().getItem(mc.player.getInventory().getSelectedSlot()).is(ItemTags.SWORDS)) {
            KillAura.this.attackTarget(KillAura.this.targetYaw, KillAura.this.targetPitch, false);
         }
      }

      private void updateSwap2Block() {
         this.autoBlockTicks++;
         this.suppressReleasePackets = true;
         if (this.serverBlocking) {
            this.releaseBlock();
         }

         if (!this.serverSlotChanged && this.swapBlockCycleCount > 0) {
            this.sendSelectedSlot(mc.player.getInventory().getSelectedSlot() % 8 + 1);
            return;
         }

         if (this.autoBlockTicks < 3) {
            return;
         }

         if (this.serverSlotChanged) {
            this.sendSelectedSlot(mc.player.getInventory().getSelectedSlot());
            KillAura.this.attackTarget(KillAura.this.targetYaw, KillAura.this.targetPitch, false);
         }

         this.swapBlockCycleCount++;
         if (this.swapBlockCycleCount < 3) {
            this.alternateSword = !this.alternateSword;
            KillAura.this.attackTarget(KillAura.this.targetYaw, KillAura.this.targetPitch, true);
            this.sendBlockPacket();
         } else {
            this.alternateSword = !this.alternateSword;
            if (this.alternateSword) {
               KillAura.this.attackTarget(KillAura.this.targetYaw, KillAura.this.targetPitch, true);
               this.pendingPostMotionBlock = true;
            } else {
               KillAura.this.attackTarget(KillAura.this.targetYaw, KillAura.this.targetPitch, false);
            }

            this.swapBlockCycleCount = 0;
         }

         this.autoBlockTicks = 0;
      }

      private void stop() {
         this.predictiveBlockActive = false;
         this.autoBlockActive = false;
         this.suppressReleasePackets = false;
         this.pendingPostMotionBlock = this.alternateSword = false;
         this.autoBlockTicks = this.swapBlockCycleCount = 0;
         if (mc.player == null || mc.getConnection() == null) {
            this.serverBlocking = this.serverSlotChanged = this.blinkActive = this.usingAutoBlockItem = false;
            return;
         }
         if (this.serverSlotChanged) {
            this.restoreSelectedSlot();
         }

         if (this.serverBlocking) {
            this.releaseBlock();
         }
         stopAutoBlockItem();

         if (this.blinkActive) {
            PacketBlinkQueue.disable();
            this.blinkActive = false;
         }

         this.suppressReleasePackets = false;
      }

      private void sendSelectedSlot(int slot) {
         mc.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
         this.serverSlotChanged = true;
         if (slot == mc.player.getInventory().getSelectedSlot()) {
            this.serverSlotChanged = false;
         }
      }

      private boolean usesAutoBlockAttackCycle() {
         return switch (KillAura.this.autoBlock.getValue()) {
            case WATCHDOG_LABEL, WATCHDOG2_LABEL, SWAP_LABEL, SWAP2_LABEL, CYCLE_LABEL -> true;
            default -> false;
         };
      }

      private void finishMotion() {
         if (KillAura.this.predict.getValue()) refreshPredictAutoBlock();
         stopInvalidAutoBlock();
         if (this.pendingPostMotionBlock && KillAura.this.target != null) {
            this.pendingPostMotionBlock = false;
            if (!KillAura.this.autoBlock.is(WATCHDOG2_LABEL)) this.sendBlockPacket();
            else this.releaseBlock();
         }
      }

      private void stopInvalidAutoBlock() {
         if (!KillAura.this.canAutoBlock() || KillAura.this.autoBlock.is(NONE_LABEL) || !mc.player.getMainHandItem().is(ItemTags.SWORDS)) {
            stop();
         }
      }

      private boolean hasAnimation() {
         // Cyclic modes release or swap before reblocking; keep their working phase visible.
         return hasAutoBlockMode() && isAutoBlockInputAllowed()
            && (this.autoBlockActive || isAutoBlocking() || KillAura.this.predict.getValue() && !KillAura.this.autoBlockRmb.getValue());
      }

      private void synchronizePrediction() {
         if (!hasAutoBlockMode() || !KillAura.this.canAutoBlock() || KillAura.this.target == null || KillAura.this.target.isRemoved() || !KillAura.this.target.isAlive()
             || !mc.player.getMainHandItem().is(ItemTags.SWORDS)) {
            stop();
            return;
         }
         this.autoBlockActive = true;
         if (!this.predictiveBlockActive) {
            // A brief threat must block immediately, before the normal mode cycle can advance.
            if (!this.serverBlocking) this.sendBlockPacket();
            if (!this.usingAutoBlockItem) startAutoBlockItem();
            this.predictiveBlockActive = true;
         }
      }

      private boolean isTargetFacingPlayer(Entity entity, double angleDegrees) {
         Vec3 targetEyePosition = entity.getEyePosition(1.0F);
         Vec3 targetLookDirection = entity.getViewVector(1.0F).normalize();
         Vec3 directionToPlayer = mc.player.getEyePosition(1.0F).subtract(targetEyePosition).normalize();
         double directionDot = targetLookDirection.dot(directionToPlayer);
         return directionDot >= Math.cos(Math.toRadians(angleDegrees));
      }

      private SwordSlots findSwordSlots() {
         int first = -1;
         int second = -1;
         for (int slot = 0; slot < 9; slot++) {
            if (mc.player.getInventory().getItem(slot).is(ItemTags.SWORDS)) {
               if (first != -1) {
                  second = slot;
                  break;
               }
               first = slot;
            }
         }
         return new SwordSlots(first, second);
      }

      private void attackAndBlock() {
         KillAura.this.attackTarget(KillAura.this.targetYaw, KillAura.this.targetPitch, true);
         this.sendBlockPacket();
         startAutoBlockItem();
      }

      private record SwordSlots(int first, int second) { }
   }

   private final class BlockPrediction {
      private static final double PREDICT_BLOCK_RANGE = 3.5;
      private static final double PREDICT_BLOCK_TOLERANCE = 0.3;
      private static final int PREDICT_BLOCK_STEPS = 16;
      private static final double PREDICT_BLOCK_HORIZON_TICKS = 2;
      private static final double MIN_SAMPLE_INTERVAL_TICKS = 0.5;
      private static final double MOVEMENT_HISTORY_TICKS = 3;
      private static final double MAX_SAMPLE_DISPLACEMENT_SQUARED = 16;
      private static final double MAX_YAW_PER_TICK = 60;
      private static final double MAX_PITCH_PER_TICK = 45;
      private static final double DUPLICATE_ROTATION_TOLERANCE = 0.001;
      private static final double NANOS_PER_TICK = 50_000_000.0;

      private final Map<Player, RotationSample> blockRotations = new IdentityHashMap<>();
      private final Map<Player, MovementSample> blockPositions = new IdentityHashMap<>();
      private Player blockThreatTarget;

      private boolean hasThreat() { return this.blockThreatTarget != null; }
      private Player threatTarget() { return this.blockThreatTarget; }

      private void reset() {
         this.blockThreatTarget = null;
         this.blockRotations.clear();
         this.blockPositions.clear();
      }

      private void update() {
         this.blockRotations.keySet().removeIf(player -> player.isRemoved() || player.level() != mc.level);
         this.blockPositions.keySet().removeIf(player -> player.isRemoved() || player.level() != mc.level);
         double now = predictionTime();
         var threat = TargetFinder.nearest(Double.POSITIVE_INFINITY, true, !KillAura.this.attackTeammates.getValue(), entity -> {
            if (!(entity instanceof Player player) || !player.isAlive() || player.isSleeping() || player.isSpectator()) return false;
            var latest = player.getClientPositionAndRotation();
            Vec3 position = latest.position();
            double distanceSquared = mc.player.position().distanceToSqr(position);
            if (distanceSquared > PREDICT_BLOCK_RANGE * PREDICT_BLOCK_RANGE) return false;
            var rotation = this.blockRotations.computeIfAbsent(player,
               ignored -> sampleBlockRotation(null, latest.yRot(), latest.xRot(), now));
            var movement = this.blockPositions.computeIfAbsent(player, ignored -> sampleBlockMovement(null, position, now));
            Vec3 eyes = position.add(0, player.getEyeHeight(), 0);
            Vec3 velocity = now - movement.tick() <= MOVEMENT_HISTORY_TICKS ? movement.velocity() : Vec3.ZERO;
            return predictsMovingBlockThreat(distanceSquared, eyes, rotation, now, velocity,
               mc.player.getDeltaMovement(), mc.player.getBoundingBox(), KillAura.this.wallRange.getValue(), (origin, hit) -> mc.level.clip(
                  new ClipContext(origin, hit, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() != HitResult.Type.MISS);
         });
         this.blockThreatTarget = threat instanceof Player player ? player : null;
      }

      private void recordEntityUpdate(Entity entity, boolean rotationUpdated, boolean positionUpdated, boolean teleport) {
         if (entity instanceof Player player && player != mc.player) {
            double now = predictionTime();
            var latest = player.getClientPositionAndRotation();
            if (teleport) {
               this.blockRotations.remove(player);
               this.blockPositions.remove(player);
            }
            if (rotationUpdated) this.blockRotations.put(player,
               sampleBlockRotation(this.blockRotations.get(player), latest.yRot(), latest.xRot(), now));
            if (positionUpdated) this.blockPositions.put(player,
               sampleBlockMovement(this.blockPositions.get(player), latest.position(), now));
         }
      }

      private void recordHeadUpdate(Entity entity, float yaw) {
         if (entity instanceof Player player && player != mc.player) {
            var previous = this.blockRotations.get(player);
            float pitch = previous == null ? player.getClientPositionAndRotation().xRot() : (float)previous.pitch();
            // Head packets interpolate separately; their received angle is already authoritative.
            this.blockRotations.put(player, sampleBlockRotation(previous, yaw, pitch, predictionTime()));
         }
      }

      private static double predictionTime() { return System.nanoTime() / NANOS_PER_TICK; }

      private static RotationSample sampleBlockRotation(RotationSample previous, float yaw, float pitch, double now) {
         double yawStep = 0, pitchStep = 0;
         double elapsed = previous == null ? 0 : now - previous.tick();
         if (previous != null && elapsed >= 0 && elapsed < MIN_SAMPLE_INTERVAL_TICKS
             && Math.abs(Mth.wrapDegrees(yaw - (float)previous.yaw())) < DUPLICATE_ROTATION_TOLERANCE
             && Math.abs(pitch - previous.pitch()) < DUPLICATE_ROTATION_TOLERANCE) {
            return previous;
         }
         if (elapsed > 0 && elapsed <= PREDICT_BLOCK_HORIZON_TICKS) {
            double ticks = Math.max(MIN_SAMPLE_INTERVAL_TICKS, elapsed);
            yawStep = Mth.clamp(Mth.wrapDegrees(yaw - (float)previous.yaw()) / ticks, -MAX_YAW_PER_TICK, MAX_YAW_PER_TICK);
            pitchStep = Mth.clamp((pitch - previous.pitch()) / ticks, -MAX_PITCH_PER_TICK, MAX_PITCH_PER_TICK);
         }
         return new RotationSample(yaw, pitch, now, yawStep, pitchStep);
      }

      private static MovementSample sampleBlockMovement(MovementSample previous, Vec3 position, double now) {
         Vec3 velocity = Vec3.ZERO;
         double elapsed = previous == null ? 0 : now - previous.tick();
         if (elapsed > 0 && elapsed <= MOVEMENT_HISTORY_TICKS) {
            var change = position.subtract(previous.position());
            // A teleport is a new observation, rather than movement to extrapolate.
            if (change.lengthSqr() <= MAX_SAMPLE_DISPLACEMENT_SQUARED) velocity = change.scale(1 / Math.max(MIN_SAMPLE_INTERVAL_TICKS, elapsed));
         }
         return new MovementSample(position, now, velocity);
      }

      private static boolean predictsMovingBlockThreat(double distanceSquared, Vec3 eyes, RotationSample rotation, double now,
                                              Vec3 attackerVelocity, Vec3 localVelocity, AABB target,
                                              double wallRange, BiPredicate<Vec3, Vec3> occluded) {
         if (distanceSquared > PREDICT_BLOCK_RANGE * PREDICT_BLOCK_RANGE) return false;
         double age = now - rotation.tick();
         double yawStep = age >= 0 && age <= PREDICT_BLOCK_HORIZON_TICKS ? rotation.yawPerTick() : 0;
         double pitchStep = age >= 0 && age <= PREDICT_BLOCK_HORIZON_TICKS ? rotation.pitchPerTick() : 0;
         // Sample the turn over the next two ticks so a fast sweep cannot skip the hitbox.
         for (int step = 0; step <= PREDICT_BLOCK_STEPS; step++) {
            double ticksAhead = step * PREDICT_BLOCK_HORIZON_TICKS / PREDICT_BLOCK_STEPS;
            float yaw = (float)(rotation.yaw() + yawStep * ticksAhead);
            float pitch = (float)Mth.clamp(rotation.pitch() + pitchStep * ticksAhead, -90, 90);
            Vec3 origin = eyes.add(attackerVelocity.scale(ticksAhead));
            AABB predictedBox = target.move(localVelocity.scale(ticksAhead));
            Vec3 direction = Vec3.directionFromRotation(pitch, yaw);
            if (direction.dot(predictedBox.getCenter().subtract(origin)) <= 0) continue;
            if (raycastDistance(origin, direction, predictedBox.inflate(PREDICT_BLOCK_TOLERANCE),
                PREDICT_BLOCK_RANGE, wallRange, hit -> occluded.test(origin, hit)) >= 0) return true;
         }
         return false;
      }

      // Passive checks never replace samples; time is measured in fractional ticks on a monotonic clock.
      private record RotationSample(double yaw, double pitch, double tick, double yawPerTick, double pitchPerTick) { }
      private record MovementSample(Vec3 position, double tick, Vec3 velocity) { }
   }
}
