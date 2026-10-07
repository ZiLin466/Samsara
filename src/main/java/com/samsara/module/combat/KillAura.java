package com.samsara.module.combat;

import com.samsara.util.RotationUtil;
import com.mojang.blaze3d.platform.InputConstants;
import com.samsara.event.Event;
import com.samsara.event.Events;
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
   private static final double PREDICT_BLOCK_RANGE = 3.5;
   private static final double PREDICT_BLOCK_TOLERANCE = 0.3;
   private static final int PREDICT_BLOCK_STEPS = 16;
   private float targetPitch;
   private static final String WATCHDOG_LABEL = "Watchdog";
   private float targetYaw;
   private int attackTicks;
   private boolean blinkActive;
   private boolean usingAutoBlockItem;
   private static final String AUTO_BLOCK_RMB_LABEL = "AutoBlock RMB";
   private static final String ATTACK_TEAMMATES_LABEL = "Attack Teammates";
   private int firstSwordSlot;
   private static final String WATCHDOG2_LABEL = "Watchdog2";
   private final BooleanSetting requireSword;
   private static final String ROTATIONS_LABEL = "Rotations";
   private static final String CYCLE_LABEL = "Cycle";
   private boolean serverSlotChanged;
   private boolean swapBlockPhase;
   private boolean pendingPostMotionBlock;
   private boolean suppressReleasePackets;
   private static final String KILL_AURA_LABEL = "KillAura";
   private final NumberSetting rotationRange;
   private final NumberSetting wallRange;
   private int secondSwordSlot;
   private static final String ATTACK_DELAY_LABEL = "Attack Delay";
   private static final String NONE_LABEL = "None";
   private int autoBlockTicks;
   private static final String ATTACK_COOLDOWN_LABEL = "Attack Cooldown";
   private static final String ATTACK_DELAY2_LABEL = "Attack Delay2";
   private final BooleanSetting autoBlockRmb;
   private final BooleanSetting attackTeammates;
   private static final String ROTATION_RANGE_LABEL = "Rotation Range";
   private static final String VANILLA_LABEL = "Vanilla";
   private boolean alternateAttackDelay;
   public boolean serverBlocking;
   private static final String REQUIRE_SWORD_LABEL = "Require Sword";
   private final BooleanSetting attackFireballs;
   private static final String HEAD_LABEL = "Head";
   public boolean autoBlockActive;
   private final ModeSetting autoBlock;
   private final BooleanSetting predict;
   // Rotation samples use fractional ticks from a monotonic clock, independent of rendering interpolation.
   private final Map<Player, double[]> blockRotations = new IdentityHashMap<>();
   private final Map<Player, double[]> blockPositions = new IdentityHashMap<>();
   private boolean predictedBlockThreat;
   private Player blockThreatTarget;
   private boolean predictiveBlockActive;
   private final NumberSetting attackDelay2;
   private static final String SWAP_LABEL = "Swap";
   public Entity target;
   private final NumberSetting attackDelay;
   private boolean alternateSword;
   private final BooleanSetting ignoreShield;
   private static final String OPTIMAL_LABEL = "Optimal";
   private final ModeSetting rotations = new ModeSetting(ROTATIONS_LABEL, this, HEAD_LABEL, new String[]{HEAD_LABEL, OPTIMAL_LABEL});
   private static final String IGNORE_SHIELD_LABEL = "Ignore Shield";
   private static final String AUTO_BLOCK_LABEL = "AutoBlock";
   private final BooleanSetting attackCooldown;
   private static final String SWAP2_LABEL = "Swap2";
   private int swapCycleTicks;
   private static final String ATTACK_FIREBALLS_LABEL = "Attack Fireballs";

   private void sendBlockPacket() {
      if (this.target == null || this.autoBlock.is(NONE_LABEL) || !this.canAutoBlock()) return;
      mc.getConnection().send(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 0, Events.ROTATION.getYaw(), Events.ROTATION.getPitch()));
      this.serverBlocking = true;
   }

   private void restoreSelectedSlot() {
      if (this.serverSlotChanged) {
         mc.getConnection().send(new ServerboundSetCarriedItemPacket(mc.player.getInventory().getSelectedSlot()));
         this.serverSlotChanged = false;
      }
   }

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

   private void updateAutoBlock() {
      if (!this.canAutoBlock()) return;
      this.suppressReleasePackets = false;
      this.autoBlockActive = true;
      switch (this.autoBlock.getValue()) {
         case VANILLA_LABEL -> {
            mc.getConnection().send(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 0, mc.player.getYRot(), mc.player.getXRot()));
            this.serverBlocking = true;
         }
         case CYCLE_LABEL -> {
            this.suppressReleasePackets = true;
            this.autoBlockTicks++;
            if (this.autoBlockTicks < 3) {
               return;
            }

            if (this.serverSlotChanged) {
               this.sendSelectedSlot(mc.player.getInventory().getSelectedSlot());
            }

            this.attackTarget(this.targetYaw, this.targetPitch, true);
            this.sendBlockPacket();
            startAutoBlockItem();
            PacketBlinkQueue.disable();
            this.blinkActive = true;
            if (!this.serverSlotChanged) {
               this.sendSelectedSlot(mc.player.getInventory().getSelectedSlot() % 8 + 1);
            }

            PacketBlinkQueue.enable();
            this.autoBlockTicks = 0;
         }
         case WATCHDOG_LABEL -> {
            this.suppressReleasePackets = true;
            this.autoBlockTicks++;
            if (this.autoBlockTicks < 2) {
               if (this.serverBlocking) {
                  startAutoBlockItem();
               }

               return;
            }

            this.releaseBlock();
            this.attackTarget(this.targetYaw, this.targetPitch, true);
            this.sendBlockPacket();
            startAutoBlockItem();
            this.autoBlockTicks = 0;
         }
         case WATCHDOG2_LABEL -> {
            this.suppressReleasePackets = true;
            this.autoBlockTicks++;
            if (this.autoBlockTicks < 2) {
               if (this.serverBlocking) {
                  startAutoBlockItem();
               }

               return;
            }

            this.releaseBlock();
            boolean facingTarget = this.isTargetFacingPlayer(this.target, 45.0);
            if (this.predict.getValue() || mc.player.hurtTime <= 5 && facingTarget && !((double)mc.player.distanceTo(this.target) > 3.0)) {
               this.attackTarget(this.targetYaw, this.targetPitch, true);
               this.sendBlockPacket();
               startAutoBlockItem();
            } else {
               this.attackTarget(this.targetYaw, this.targetPitch, false);
            }

            this.autoBlockTicks = 0;
         }
         case SWAP_LABEL -> {
            this.suppressReleasePackets = true;
            this.autoBlockTicks++;
            if (this.autoBlockTicks < 3) {
               if (this.autoBlockTicks != 2 && this.serverBlocking) {
                  startAutoBlockItem();
               }

               return;
            }

            this.findSwordSlots();
            if (this.firstSwordSlot != -1 && this.secondSwordSlot != -1) {
               int swordSlot = this.alternateSword ? this.secondSwordSlot : this.firstSwordSlot;
               this.alternateSword = !this.alternateSword;
               this.sendSelectedSlot(swordSlot);
               if (mc.player.getInventory().getItem(swordSlot).is(ItemTags.SWORDS)) {
                  this.attackTarget(this.targetYaw, this.targetPitch, true);
                  this.sendBlockPacket();
                  startAutoBlockItem();
               }

               this.autoBlockTicks = 0;
            } else if (mc.player.getInventory().getItem(mc.player.getInventory().getSelectedSlot()).is(ItemTags.SWORDS)) {
               this.attackTarget(this.targetYaw, this.targetPitch, false);
            }
         }
         case SWAP2_LABEL -> {
            this.autoBlockTicks++;
            this.suppressReleasePackets = true;
            if (this.serverBlocking) {
               this.releaseBlock();
            }

            if (!this.serverSlotChanged && this.swapBlockPhase) {
               this.sendSelectedSlot(mc.player.getInventory().getSelectedSlot() % 8 + 1);
               return;
            }

            if (this.autoBlockTicks < 3) {
               return;
            }

            if (this.serverSlotChanged) {
               this.sendSelectedSlot(mc.player.getInventory().getSelectedSlot());
               this.attackTarget(this.targetYaw, this.targetPitch, false);
            }

            this.swapCycleTicks++;
            if (this.swapCycleTicks < 3) {
               this.swapBlockPhase = true;
               this.alternateSword = !this.alternateSword;
               this.attackTarget(this.targetYaw, this.targetPitch, true);
               this.sendBlockPacket();
            } else {
               this.swapBlockPhase = false;
               this.alternateSword = !this.alternateSword;
               if (this.alternateSword) {
                  this.attackTarget(this.targetYaw, this.targetPitch, true);
                  this.pendingPostMotionBlock = true;
               } else {
                  this.attackTarget(this.targetYaw, this.targetPitch, false);
               }

               this.swapCycleTicks = 0;
            }

            this.autoBlockTicks = 0;
         }
         default -> { }
      }
   }

   @Override
   public int getPriority(Event event) {
      return event == Events.ROTATION ? -2 : 0;
   }

   private void resetCombat() {
      this.target = null;
      resetBlockPrediction();
      stopAutoBlock();
   }

   private void stopAutoBlock() {
      this.predictiveBlockActive = false;
      this.autoBlockActive = false;
      this.suppressReleasePackets = false;
      this.pendingPostMotionBlock = this.swapBlockPhase = this.alternateSword = false;
      this.autoBlockTicks = this.swapCycleTicks = 0;
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
      if (event == Events.MOUSE_BUTTON && this.autoBlockRmb.getValue()
          && Events.MOUSE_BUTTON.getButton() == InputConstants.MOUSE_BUTTON_RIGHT && Events.MOUSE_BUTTON.isReleased()
          && (!this.predict.getValue() || !this.predictedBlockThreat)) {
         stopAutoBlock();
      }
      if (event == Events.ROTATION) {
         updateBlockPrediction();
      }
      if (this.predict.getValue() && (event == Events.POST_MOTION || event == Events.MOVE_INPUT || event == Events.TICK || event == Events.RENDER_2D
          || event == Events.MOUSE_BUTTON && Events.MOUSE_BUTTON.getButton() == InputConstants.MOUSE_BUTTON_RIGHT)) {
         refreshPredictAutoBlock();
      }
      if ((event == Events.POST_MOTION || event == Events.ROTATION)
          && (!this.canAutoBlock() || this.autoBlock.is(NONE_LABEL) || !mc.player.getMainHandItem().is(ItemTags.SWORDS))) {
         stopAutoBlock();
      }
      if (event == Events.POST_MOTION && this.pendingPostMotionBlock && this.target != null) {
         this.pendingPostMotionBlock = false;
         if (!this.autoBlock.is(WATCHDOG2_LABEL)) {
            this.sendBlockPacket();
         } else {
            this.releaseBlock();
         }
      }

      if (event == Events.MOUSE_BUTTON) {
         if (mc.gui.screen() == null && this.target != null) {
            event.setCancelled(true);
         }
      }

      if (event == Events.ROTATION) {
         this.setSuffix(this.autoBlock.is(NONE_LABEL) ? null : this.autoBlock.getValue());
         this.autoBlockActive = false;
         if (!this.attackCooldown.getValue() || mc.player.getAttackStrengthScale(0.0F) >= 1.0F) {
            this.attackTicks++;
         }

         this.target = TargetFinder.nearest(this.rotationRange.getValue(), true, !this.attackTeammates.getValue(), entity -> {
            this.updateTargetRotation(entity);
            // Keep distant visible targets for rotations; reject occluded targets outside the wall range.
            return this.raycastDistance(entity, this.targetYaw, this.targetPitch, this.rotationRange.getValue()) >= 0;
         });
         if (this.target == null && this.predictedBlockThreat) this.target = this.blockThreatTarget;

         if (this.attackFireballs.getValue()) {
            Entity entity = this.findFireball(3.0);
            if (entity != null) {
               this.updateTargetRotation(entity);
               Events.ROTATION.setYaw(this.targetYaw);
               Events.ROTATION.setPitch(this.targetPitch);
               mc.gameMode.attack(mc.player, entity);
               mc.player.swing(InteractionHand.MAIN_HAND, mc.player.getMainHandItem().getAttackAnimation(), false);
               mc.getConnection().send(ServerboundPunchPacket.INSTANCE);
               this.resetCombat();
               return;
            }
         }

         if (this.requireSword.getValue() && !InventoryUtil.isHoldingSword()) {
            this.target = null;
         }

         if (this.target != null) {
            this.updateTargetRotation(this.target);
            Events.ROTATION.setYaw(this.targetYaw);
            Events.ROTATION.setPitch(this.targetPitch);
            double attackDelay2Value = this.alternateAttackDelay ? this.attackDelay2.getValue() : this.attackDelay.getValue();
            if (!FeatureManager.velocity.blocksAttacks() && (double)this.attackTicks > attackDelay2Value && (!this.attackCooldown.getValue() || mc.player.getAttackStrengthScale(0.0F) >= 1.0F)) {
               if (this.ignoreShield.getValue() && this.target instanceof Player player && player.isUsingItem() && player.getUseItem().is(Items.SHIELD)) {
                  return;
               }

               if (!this.autoBlock.is(WATCHDOG_LABEL) && !this.autoBlock.is(WATCHDOG2_LABEL) && !this.autoBlock.is(SWAP_LABEL) && !this.autoBlock.is(SWAP2_LABEL) && !this.autoBlock.is(CYCLE_LABEL)
                  || !this.canAutoBlock() && !this.serverBlocking) {
                  this.attackTicks = 0;
                  this.attackTarget(this.targetYaw, this.targetPitch, false);
               }

               this.alternateAttackDelay = !this.alternateAttackDelay;
            }

            if (!this.autoBlock.is(NONE_LABEL) && mc.player.getMainHandItem().is(ItemTags.SWORDS)) {
               if (this.canAutoBlock()) {
                  mc.options.keyUse.setDown(false);
                  this.updateAutoBlock();
                  if (this.predict.getValue()) synchronizePredictAutoBlock();
               } else {
                  stopAutoBlock();
               }
            } else {
               stopAutoBlock();
            }
         } else {
            this.resetCombat();
         }
      }

      if (event == Events.PACKET_SEND && Events.PACKET_SEND.getPacket() instanceof ServerboundPlayerActionPacket && this.suppressReleasePackets) {
         event.setCancelled(true);
      }
   }

   public boolean hasAutoBlockAnimation() {
      // Cyclic modes release or swap before reblocking; keep their working phase visible.
      return hasAutoBlockMode() && isAutoBlockInputAllowed()
         && (this.autoBlockActive || isAutoBlocking() || this.predict.getValue() && !this.autoBlockRmb.getValue());
   }

   public boolean hasAutoBlockMode() { return !this.autoBlock.is(NONE_LABEL); }

   public boolean isAutoBlocking() { return this.serverBlocking || this.usingAutoBlockItem; }

   public boolean prepareForAutoRod() {
      if (!isEnabled()) return true;
      if (mc.player == null || mc.getConnection() == null) return false;
      resetBlockPrediction();
      stopAutoBlock();
      return true;
   }

   private void resetBlockPrediction() {
      this.predictedBlockThreat = false;
      this.blockThreatTarget = null;
      this.predictiveBlockActive = false;
      this.blockRotations.clear();
      this.blockPositions.clear();
   }

   public void onPredictEntityUpdate(Entity entity, boolean rotationUpdated, boolean positionUpdated, boolean teleport) {
      if (!isEnabled() || !this.predict.getValue() || !hasAutoBlockMode() || mc.player == null || mc.level == null
          || !(entity instanceof Player)) return;
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
      refreshPredictAutoBlock();
   }

   public void onPredictHeadUpdate(Entity entity, float yaw) {
      if (!isEnabled() || !this.predict.getValue() || !hasAutoBlockMode() || mc.player == null || mc.level == null
          || !(entity instanceof Player)) return;
      if (entity instanceof Player player && player != mc.player) {
         var previous = this.blockRotations.get(player);
         float pitch = previous == null ? player.getClientPositionAndRotation().xRot() : (float)previous[1];
         // Head packets interpolate separately; their received angle is already authoritative.
         this.blockRotations.put(player, sampleBlockRotation(previous, yaw, pitch, predictionTime()));
      }
      refreshPredictAutoBlock();
   }

   private void refreshPredictAutoBlock() {
      if (mc.player == null || mc.level == null || mc.getConnection() == null || !isAutoBlockInputAllowed()) {
         resetBlockPrediction();
         stopAutoBlock();
         return;
      }
      updateBlockPrediction();
      synchronizePredictAutoBlock();
   }

   private void synchronizePredictAutoBlock() {
      if (!hasAutoBlockMode() || !this.canAutoBlock() || this.target == null || this.target.isRemoved() || !this.target.isAlive()
          || !mc.player.getMainHandItem().is(ItemTags.SWORDS)) {
         stopAutoBlock();
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

   private static double predictionTime() { return System.nanoTime() / 50_000_000.0; }

   private void updateBlockPrediction() {
      if (!this.predict.getValue() || this.autoBlock.is(NONE_LABEL)) {
         resetBlockPrediction();
         return;
      }
      this.blockRotations.keySet().removeIf(player -> player.isRemoved() || player.level() != mc.level);
      this.blockPositions.keySet().removeIf(player -> player.isRemoved() || player.level() != mc.level);
      double now = predictionTime();
      var threat = TargetFinder.nearest(Double.POSITIVE_INFINITY, true, !this.attackTeammates.getValue(), entity -> {
         if (!(entity instanceof Player player) || !player.isAlive() || player.isSleeping() || player.isSpectator()) return false;
         var latest = player.getClientPositionAndRotation();
         Vec3 position = latest.position();
         double distanceSquared = mc.player.position().distanceToSqr(position);
         if (distanceSquared > PREDICT_BLOCK_RANGE * PREDICT_BLOCK_RANGE) return false;
         var rotation = this.blockRotations.computeIfAbsent(player,
            ignored -> sampleBlockRotation(null, latest.yRot(), latest.xRot(), now));
         var movement = this.blockPositions.computeIfAbsent(player, ignored -> sampleBlockMovement(null, position, now));
         Vec3 eyes = position.add(0, player.getEyeHeight(), 0);
         Vec3 velocity = now - movement[3] <= 3 ? new Vec3(movement[4], movement[5], movement[6]) : Vec3.ZERO;
         return predictsMovingBlockThreat(distanceSquared, eyes, rotation, now, velocity,
            mc.player.getDeltaMovement(), mc.player.getBoundingBox(), this.wallRange.getValue(), (origin, hit) -> mc.level.clip(
               new ClipContext(origin, hit, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() != HitResult.Type.MISS);
      });
      this.predictedBlockThreat = threat != null;
      this.blockThreatTarget = threat instanceof Player player ? player : null;
      if (threat != null && (this.target == null || this.target.isRemoved() || !this.target.isAlive())) this.target = threat;
   }

   private boolean isTargetFacingPlayer(Entity entity, double angleDegrees) {
      Vec3 targetEyePosition = entity.getEyePosition(1.0F);
      Vec3 targetLookDirection = entity.getViewVector(1.0F).normalize();
      Vec3 directionToPlayer = mc.player.getEyePosition(1.0F).subtract(targetEyePosition).normalize();
      double directionDot = targetLookDirection.dot(directionToPlayer);
      return directionDot >= Math.cos(Math.toRadians(angleDegrees));
   }

   private boolean canAutoBlock() {
      return isAutoBlockInputAllowed() && shouldAutoBlock(this.predict.getValue(), this.autoBlockRmb.getValue(),
         isManualAutoBlockRequested(), this.predictedBlockThreat);
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

   private void findSwordSlots() {
      this.firstSwordSlot = -1;
      this.secondSwordSlot = -1;

      for (int slot = 0; slot < 9; slot++) {
         if (mc.player.getInventory().getItem(slot).is(ItemTags.SWORDS)) {
            if (this.firstSwordSlot != -1) {
               this.secondSwordSlot = slot;
               break;
            }

            this.firstSwordSlot = slot;
         }
      }
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
      this.firstSwordSlot = -1;
      this.secondSwordSlot = -1;
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

   static boolean predictsBlockThreat(double distanceSquared, Vec3 eyes, double[] current, double[] previous,
                                     AABB target, double wallRange, Predicate<Vec3> occluded) {
      var sample = sampleBlockRotation(previous, (float)current[0], (float)current[1], current[2]);
      return predictsMovingBlockThreat(distanceSquared, eyes, sample, current[2], Vec3.ZERO, Vec3.ZERO,
         target, wallRange, (origin, hit) -> occluded.test(hit));
   }

   // Samples store yaw, pitch, time, yaw/tick and pitch/tick. Passive checks never replace them.
   static double[] sampleBlockRotation(double[] previous, float yaw, float pitch, double now) {
      double yawStep = 0, pitchStep = 0;
      double elapsed = previous == null ? 0 : now - previous[2];
      if (previous != null && previous.length >= 5 && elapsed >= 0 && elapsed < 0.5
          && Math.abs(Mth.wrapDegrees(yaw - (float)previous[0])) < 0.001 && Math.abs(pitch - previous[1]) < 0.001) {
         return previous;
      }
      if (elapsed > 0 && elapsed <= 2) {
         double ticks = Math.max(0.5, elapsed);
         yawStep = Mth.clamp(Mth.wrapDegrees(yaw - (float)previous[0]) / ticks, -60, 60);
         pitchStep = Mth.clamp((pitch - previous[1]) / ticks, -45, 45);
      }
      return new double[]{yaw, pitch, now, yawStep, pitchStep};
   }

   static double[] sampleBlockMovement(double[] previous, Vec3 position, double now) {
      Vec3 velocity = Vec3.ZERO;
      double elapsed = previous == null ? 0 : now - previous[3];
      if (elapsed > 0 && elapsed <= 3) {
         var change = position.subtract(previous[0], previous[1], previous[2]);
         // A teleport is a new observation, rather than movement to extrapolate.
         if (change.lengthSqr() <= 16) velocity = change.scale(1 / Math.max(0.5, elapsed));
      }
      return new double[]{position.x, position.y, position.z, now, velocity.x, velocity.y, velocity.z};
   }

   static boolean predictsMovingBlockThreat(double distanceSquared, Vec3 eyes, double[] rotation, double now,
                                           Vec3 attackerVelocity, Vec3 localVelocity, AABB target,
                                           double wallRange, BiPredicate<Vec3, Vec3> occluded) {
      if (distanceSquared > PREDICT_BLOCK_RANGE * PREDICT_BLOCK_RANGE) return false;
      double age = now - rotation[2];
      double yawStep = age >= 0 && age <= 2 ? rotation[3] : 0;
      double pitchStep = age >= 0 && age <= 2 ? rotation[4] : 0;
      // Sample the turn over the next two ticks so a fast sweep cannot skip the hitbox.
      for (int step = 0; step <= PREDICT_BLOCK_STEPS; step++) {
         double ticksAhead = step * 2.0 / PREDICT_BLOCK_STEPS;
         float yaw = (float)(rotation[0] + yawStep * ticksAhead);
         float pitch = (float)Mth.clamp(rotation[1] + pitchStep * ticksAhead, -90, 90);
         Vec3 origin = eyes.add(attackerVelocity.scale(ticksAhead));
         AABB predictedBox = target.move(localVelocity.scale(ticksAhead));
         Vec3 direction = Vec3.directionFromRotation(pitch, yaw);
         if (direction.dot(predictedBox.getCenter().subtract(origin)) <= 0) continue;
         if (raycastDistance(origin, direction, predictedBox.inflate(PREDICT_BLOCK_TOLERANCE),
             PREDICT_BLOCK_RANGE, wallRange, hit -> occluded.test(origin, hit)) >= 0) return true;
      }
      return false;
   }
}
