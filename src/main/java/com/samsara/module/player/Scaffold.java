package com.samsara.module.player;

import com.samsara.util.RotationUtil;
import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.BooleanSetting;
import com.samsara.setting.ModeSetting;
import com.samsara.setting.NumberSetting;
import com.samsara.ui.dynamicIsland.DynamicIslandManager;
import com.samsara.util.Wrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class Scaffold extends Feature {
   private static final String KEEP_Y_LABEL = "Keep Y";
   private static final String ROTATION_SPEED_LABEL = "Rotation Speed";
   private static final String SCAFFOLD_LABEL = "Scaffold";
   private static final String TELLY_RMB_LABEL = "Telly RMB";
   private static final String BACKWARD_LABEL = "Backward";
   private static final String RAYCAST2_LABEL = "Raycast2";
   private static final String TELLY_LABEL = "Telly";
   private static final String ROTATIONS_LABEL = "Rotations";
   private static final String WATCHDOG_TOWER_LABEL = "Watchdog Tower";
   private static final String SNEAK_LABEL = "Sneak";
   private static final String HIT_VEC_LABEL = "HitVec";
   private static final String OFFSET_LABEL = "Offset";
   private static final String SNEAK_DELAY_LABEL = "Sneak Delay";
   private static final String DISABLED_LABEL = "Disabled";
   private static final String WATCHDOG3_LABEL = "Watchdog3";
   private static final String FAST_MODE_LABEL = "Fast Mode";
   private static final String RAYCAST_LABEL = "Raycast";
   private static final String WATCHDOG2_LABEL = "Watchdog2";
   private static final String WATCHDOG_LABEL = "Watchdog";
   private static final int FINE_SEARCH_LIMIT_DEGREES = 20;
   private static final int COARSE_SEARCH_STEP_DEGREES = 2;

   private final ModeSetting rotations;
   private final NumberSetting rotationSpeed;
   private final ModeSetting fastMode;
   private final BooleanSetting tellyRmb;
   private final BooleanSetting keepY;
   private final BooleanSetting watchdogTower;
   private final BooleanSetting sneak;
   private final NumberSetting sneakDelay;

   private final float[] yawOffsets;
   private final float[] pitchOffsets;

   private float rotationPitch;
   private int placedBlocks;
   private int previousSlot;
   private boolean jumpStartedOnGround;
   private RotationUpdate rotationUpdate = RotationUpdate.PENDING;
   private float rotationYaw;
   private int bridgeY;
   private float targetPitch;
   private int ticksSincePlacement;
   private float targetYaw;
   private boolean pendingWatchdogJump;
   private boolean rightMousePressed;

   private PlacementRotation placementRotation(BlockHitResult blockHit) {
      Vec3 eyePosition = mc.player.getEyePosition();
      Vec3 position = blockHit.getLocation();
      double deltaX = position.x - eyePosition.x;
      double deltaY = position.y - eyePosition.y;
      double deltaZ = position.z - eyePosition.z;
      double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
      float pitch = (float)(-Math.toDegrees(Math.atan2(deltaY, horizontalDistance)));
      return switch (this.rotations.getValue()) {
         case BACKWARD_LABEL -> new PlacementRotation(MoveDirectionUtil.movementYaw() - 180.0F, pitch);
         case OFFSET_LABEL -> new PlacementRotation(this.backwardOffsetYaw(), 83.0F);
         case RAYCAST_LABEL -> this.findRaycastRotation(blockHit, MoveDirectionUtil.movementYaw() - 180.0F);
         case RAYCAST2_LABEL -> this.findRaycastRotation(blockHit, this.targetYaw);
         default -> new PlacementRotation((float)Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0F, pitch);
      };
   }

   private int findBlockSlot() {
      int bestSlot = -1;
      int largestStack = 0;

      for (int slot = 0; slot < 9; slot++) {
         ItemStack stack = mc.player.getInventory().getItem(slot);
         if (stack.getItem() instanceof BlockItem && stack.getCount() > largestStack) {
            largestStack = stack.getCount();
            bestSlot = slot;
         }
      }

      return bestSlot;
   }

   private void updateRotation(float yaw, float pitch) {
      if (this.rotationUpdate == RotationUpdate.PENDING) {
         float rotationStep = (float)(this.rotationSpeed.getValue() * 20.0 - Math.random());
         if (this.jumpStartedOnGround && this.fastMode.is(WATCHDOG_LABEL)) {
            rotationStep = (float)(89.0 - Math.random());
         }

         if (this.pendingWatchdogJump) {
            rotationStep = (float)(129.0 - Math.random());
            this.pendingWatchdogJump = false;
         } else if (this.fastMode.is(WATCHDOG2_LABEL)) {
            rotationStep = 34.0F;
         }

         if (this.jumpStartedOnGround && this.fastMode.is(WATCHDOG2_LABEL)) {
            this.pendingWatchdogJump = true;
         }

         float yawDelta = Mth.wrapDegrees(yaw - this.rotationYaw);
         float pitchDelta = Mth.wrapDegrees(pitch - this.rotationPitch);
         yawDelta = Math.clamp(yawDelta, -rotationStep, rotationStep);
         pitchDelta = Math.clamp(pitchDelta, -rotationStep, rotationStep);
         this.rotationYaw += yawDelta;
         float playerYaw = mc.player.getYRot();
         this.rotationYaw = playerYaw + Mth.wrapDegrees(this.rotationYaw - playerYaw);
         this.rotationPitch += pitchDelta;
         if (!((double)Math.abs(Mth.wrapDegrees(this.rotationYaw - yaw)) > 0.1) && !((double)Math.abs(Mth.wrapDegrees(this.rotationPitch - pitch)) > 0.1)) {
            this.rotationUpdate = RotationUpdate.ALIGNED;
         } else {
            this.rotationUpdate = RotationUpdate.MISALIGNED;
         }
      }
   }

   private PlacementRotation findRaycastRotation(BlockHitResult targetHit, float baseYaw) {
      if (this.fastMode.is(WATCHDOG3_LABEL)) {
         baseYaw = MoveDirectionUtil.movementYaw() + 90.0F;
      }

      float basePitch = 0.0F;

      for (float yawOffset : this.yawOffsets) {
         for (float pitchOffset : this.pitchOffsets) {
            float yaw = baseYaw + yawOffset;
            float pitch = Mth.clamp(basePitch + pitchOffset, -90.0F, 90.0F);
            BlockHitResult candidateHit = this.raycastBlock(yaw, pitch, 4.5F);
            if (candidateHit.getBlockPos().equals(targetHit.getBlockPos()) && candidateHit.getDirection() == targetHit.getDirection()) {
               return new PlacementRotation(yaw, pitch);
            }
         }
      }

      this.rotationUpdate = RotationUpdate.MISALIGNED;
      return new PlacementRotation(this.targetYaw, this.targetPitch);
   }

   private BlockHitResult raycastBlock(float yaw, float pitch, float range) {
      Vec3 eyePosition = mc.getCameraEntity().getEyePosition(1.0F);
      Vec3 lookDirection = RotationUtil.lookVector(yaw, pitch);
      Vec3 rayEnd = eyePosition.add(lookDirection.x * (double)range, lookDirection.y * (double)range, lookDirection.z * (double)range);
      return mc.level.clip(new ClipContext(eyePosition, rayEnd, Block.COLLIDER, Fluid.NONE, mc.getCameraEntity()));
   }

   public Scaffold() {
      super(SCAFFOLD_LABEL, Category.PLAYER);
      this.rotations = new ModeSetting(ROTATIONS_LABEL, this, HIT_VEC_LABEL, new String[]{HIT_VEC_LABEL, BACKWARD_LABEL, OFFSET_LABEL, RAYCAST_LABEL, RAYCAST2_LABEL});
      this.rotationSpeed = new NumberSetting(ROTATION_SPEED_LABEL, this, 2.0, 0.0, 10.0, 0.5);
      this.fastMode = new ModeSetting(FAST_MODE_LABEL, this, DISABLED_LABEL, new String[]{DISABLED_LABEL, TELLY_LABEL, WATCHDOG_LABEL, WATCHDOG2_LABEL, WATCHDOG3_LABEL});
      this.tellyRmb = new BooleanSetting(TELLY_RMB_LABEL, this, false);
      this.keepY = new BooleanSetting(KEEP_Y_LABEL, this, false);
      this.watchdogTower = new BooleanSetting(WATCHDOG_TOWER_LABEL, this, false);
      this.sneak = new BooleanSetting(SNEAK_LABEL, this, false);
      this.sneakDelay = new NumberSetting(SNEAK_DELAY_LABEL, this, 0.0, 0.0, 25.0, 1.0);
      this.previousSlot = -1;
      this.yawOffsets = searchOffsets(180);
      this.pitchOffsets = searchOffsets(90);
   }


   private static float[] searchOffsets(int maximumDegrees) {
      int pairCount = FINE_SEARCH_LIMIT_DEGREES
         + (maximumDegrees - FINE_SEARCH_LIMIT_DEGREES) / COARSE_SEARCH_STEP_DEGREES;
      float[] offsets = new float[1 + pairCount * 2];
      int index = 1;
      for (int angle = 1; angle <= maximumDegrees;) {
         offsets[index++] = -angle;
         offsets[index++] = angle;
         angle += angle < FINE_SEARCH_LIMIT_DEGREES ? 1 : COARSE_SEARCH_STEP_DEGREES;
      }
      return offsets;
   }

   private float backwardOffsetYaw() {
      float movementYaw = MoveDirectionUtil.movementYaw();
      float offsetYaw;
      if (movementYaw % 90.0F <= 45.0F) {
         offsetYaw = movementYaw + 45.0F;
      } else {
         offsetYaw = movementYaw - 45.0F;
      }

      return offsetYaw - 180.0F;
   }

   private PlacementTarget getPlaceData(BlockPos targetPosition) {
      if (!mc.level.getBlockState(targetPosition).canBeReplaced()) return null;
      double nearestDistance = Double.MAX_VALUE;
      BlockPos nearestSupport = null;
      Direction nearestFace = null;

      for (int offsetX = -3; offsetX <= 3; offsetX++) {
         for (int offsetY = -2; offsetY <= 0; offsetY++) {
            for (int offsetZ = -3; offsetZ <= 3; offsetZ++) {
               BlockPos candidatePosition = targetPosition.offset(offsetX, offsetY, offsetZ);

               if (!mc.level.isEmptyBlock(candidatePosition)) {
                  Direction face = this.placementFace(candidatePosition, targetPosition);
                  if (face != null) {
                     double distanceSquared = mc.player.distanceToSqr((double)candidatePosition.getX() + 0.5, (double)candidatePosition.getY() + 0.5, (double)candidatePosition.getZ() + 0.5);
                     if (distanceSquared < nearestDistance && face != Direction.DOWN) {
                        nearestDistance = distanceSquared;
                        nearestSupport = candidatePosition;
                        nearestFace = face;
                     }
                  }
               }
            }
         }
      }

      return nearestSupport == null ? null : new PlacementTarget(nearestSupport, nearestFace);
   }

   @Override
   public void onEvent(Event event) {
      if (event == Events.POST_MOTION) DynamicIslandManager.sampleScaffoldMovement();
      else if (event == Events.MOUSE_BUTTON) this.handleMouseButton(event);
      else if (event == Events.ROTATION) this.updatePlacementRotation();
      else if (event == Events.PRE_MOTION) this.updateBodyRotation();
      else if (event == Events.TICK) this.selectBlockSlot();
      else if (event == Events.POST_MOVE_INPUT) this.updateBridgeInput();
   }

   private void handleMouseButton(Event event) {
      if (mc.gui.screen() == null) {
         event.setCancelled(true);
      }

      if (Events.MOUSE_BUTTON.getButton() == 1) {
         this.rightMousePressed = Events.MOUSE_BUTTON.isPressed();
      }
   }

   private void updatePlacementRotation() {
      if (this.keepY.getValue() && mc.options.keyJump.isDown()) {
         this.bridgeY = (int)mc.player.getY() - 1;
      }

      if (this.ticksSincePlacement > 0) {
         this.ticksSincePlacement--;
      }

      if ((this.fastMode.is(WATCHDOG_LABEL) || this.fastMode.is(WATCHDOG2_LABEL) || this.fastMode.is(TELLY_LABEL)) && mc.player.onGround() && (!this.tellyRmb.getValue() || this.rightMousePressed)) {
         float initialMovementYaw = MoveDirectionUtil.movementYaw();
         Events.ROTATION.setYaw(initialMovementYaw);
         this.rotationYaw = initialMovementYaw;
         this.jumpStartedOnGround = mc.player.onGround();
         return;
      }

      this.rotationUpdate = RotationUpdate.PENDING;
      BlockPos blockPosition = mc.player.blockPosition().below();
      if (this.keepY.getValue()) {
         blockPosition = blockPosition.atY(this.bridgeY);
      }

      this.placeBlockIfNeeded(blockPosition);

      if (this.fastMode.is(WATCHDOG3_LABEL)) {
         float movementYaw = MoveDirectionUtil.movementYaw();
         float diagonalYaw = movementYaw + 45.0F;
         float yawDifference = Math.abs(Mth.wrapDegrees(diagonalYaw - this.rotationYaw));
         if (this.ticksSincePlacement == (yawDifference > 60.0F ? 8 : 9) && !mc.options.keyJump.isDown() && this.jumpStartedOnGround) {
            this.rotationYaw = movementYaw + 45.0F;
         }

         if (mc.player.onGround()) {
            this.bridgeY = (int)(mc.player.getY() - 1.0);
         }
      } else {
         this.updateRotation(this.targetYaw, this.targetPitch);
      }

      if (this.watchdogTower.getValue() && mc.player.onGround() && mc.options.keyJump.isDown()) {
         this.rotationYaw = MoveDirectionUtil.movementYaw();
      }

      Events.ROTATION.setYaw(this.rotationYaw);
      Events.ROTATION.setPitch(this.rotationPitch);
      if (!this.fastMode.is(WATCHDOG3_LABEL)) {
         this.jumpStartedOnGround = mc.player.onGround();
      }
   }

   private void placeBlockIfNeeded(BlockPos blockPosition) {
      if (!mc.level.isEmptyBlock(blockPosition)) return;
      int blockSlot = this.findBlockSlot();
      if (blockSlot == -1) return;
      PlacementTarget placement = this.getPlaceData(blockPosition);
      if (placement == null) return;

      Vec3 position = Vec3.atCenterOf(placement.position());
      BlockHitResult blockHit = new BlockHitResult(position, placement.face(), placement.position(), false);
      PlacementRotation angles = this.placementRotation(blockHit);
      this.updateRotation(angles.yaw(), angles.pitch());
      this.targetYaw = angles.yaw();
      this.targetPitch = angles.pitch();
      if (this.watchdogTower.getValue() && mc.options.keyJump.isDown() && mc.player.onGround()) {
         this.rotationUpdate = RotationUpdate.MISALIGNED;
      }

      if (this.rotationUpdate == RotationUpdate.ALIGNED) {
         mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, blockHit);
         mc.player.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, false);
         this.placedBlocks++;
         this.ticksSincePlacement = 10;
      }
   }

   private void updateBodyRotation() {
      if (this.fastMode.is(WATCHDOG3_LABEL) || this.watchdogTower.getValue() && mc.options.keyJump.isDown() && !mc.player.onGround()) {
         float backwardYaw = MoveDirectionUtil.movementYaw() - 180.0F;
         Minecraft.getInstance().player.yHeadRot = backwardYaw;
         Minecraft.getInstance().player.yBodyRot = backwardYaw;
      }
   }

   private void selectBlockSlot() {
      int placementSlot = this.findBlockSlot();
      if (placementSlot != -1 && mc.player.getInventory().getSelectedSlot() != placementSlot) {
         mc.player.getInventory().setSelectedSlot(placementSlot);
      }
   }

   private void updateBridgeInput() {
      if (this.fastMode.is(WATCHDOG3_LABEL) && !this.jumpStartedOnGround) {
         mc.player.input.makeJump();
         this.jumpStartedOnGround = true;
      }

      if ((this.fastMode.is(TELLY_LABEL) || this.fastMode.is(WATCHDOG_LABEL) || this.fastMode.is(WATCHDOG2_LABEL))
         && (mc.player.input.getMoveVector().x != 0.0F || mc.player.input.getMoveVector().y != 0.0F)
         && (!this.tellyRmb.getValue() || this.rightMousePressed)) {
         mc.player.input.makeJump();
      }

      if (this.sneak.getValue()) {
         Input input = mc.player.input.keyPresses;
         if (mc.player.tickCount % (int)(this.sneakDelay.getValue() + 1.0) == 0) {
            mc.player.input.keyPresses = new Input(input.forward(), input.backward(), input.left(), input.right(), input.jump(), true, input.sprint());
         }
      }
   }

   @Override
   public void onEnable() {
      this.bridgeY = (int)(mc.player.getY() - 1.0);
      this.previousSlot = mc.player.getInventory().getSelectedSlot();
      this.rotationYaw = mc.player.getYRot();
      this.rotationPitch = mc.player.getXRot();
      this.targetYaw = MoveDirectionUtil.movementYaw() - 180.0F;
      this.targetPitch = 83.0F;
      this.rotationPitch = 83.0F;
      this.jumpStartedOnGround = false;
      this.rotationUpdate = RotationUpdate.PENDING;
      this.pendingWatchdogJump = this.rightMousePressed = false;
      this.ticksSincePlacement = this.placedBlocks = 0;
   }

   @Override
   public void onDisable() {
      if (mc.player != null) {
         if (this.previousSlot >= 0 && this.previousSlot < 9) mc.player.getInventory().setSelectedSlot(this.previousSlot);
         mc.player.yHeadRot = mc.player.yBodyRot = mc.player.getYRot();
         mc.player.yHeadRotO = mc.player.yBodyRotO = mc.player.getYRot();
         mc.player.yRotO = mc.player.getYRot();
         mc.player.xRotO = mc.player.getXRot();
      }
      this.previousSlot = -1;
      this.rotationUpdate = RotationUpdate.PENDING;
      this.pendingWatchdogJump = this.rightMousePressed = false;
      this.ticksSincePlacement = this.placedBlocks = 0;
   }

   private Direction placementFace(BlockPos supportPosition, BlockPos targetPosition) {
      int deltaX = targetPosition.getX() - supportPosition.getX();
      int deltaY = targetPosition.getY() - supportPosition.getY();
      int deltaZ = targetPosition.getZ() - supportPosition.getZ();
      if (Math.abs(deltaX) >= Math.abs(deltaY) && Math.abs(deltaX) >= Math.abs(deltaZ)) {
         return deltaX > 0 ? Direction.EAST : Direction.WEST;
      } else if (Math.abs(deltaY) >= Math.abs(deltaX) && Math.abs(deltaY) >= Math.abs(deltaZ)) {
         return deltaY > 0 ? Direction.UP : Direction.DOWN;
      } else {
         return deltaZ > 0 ? Direction.SOUTH : Direction.NORTH;
      }
   }

   private enum RotationUpdate { PENDING, ALIGNED, MISALIGNED }

   private record PlacementRotation(float yaw, float pitch) { }

   private record PlacementTarget(BlockPos position, Direction face) { }

   public static class MoveDirectionUtil implements Wrapper {
      public static float movementYaw() {
         float yaw = Wrapper.mc.player.getYRot();
         boolean forward = Wrapper.mc.options.keyUp.isDown();
         boolean backward = Wrapper.mc.options.keyDown.isDown();
         boolean left = Wrapper.mc.options.keyLeft.isDown();
         boolean right = Wrapper.mc.options.keyRight.isDown();
         if (forward && !backward) {
            if (left && !right) {
               yaw -= 45.0F;
            } else if (right && !left) {
               yaw += 45.0F;
            }
         } else if (backward && !forward) {
            yaw += 180.0F;
            if (left && !right) {
               yaw += 45.0F;
            } else if (right && !left) {
               yaw -= 45.0F;
            }
         } else if (left && !right) {
            yaw -= 90.0F;
         } else if (right && !left) {
            yaw += 90.0F;
         }

         yaw %= 360.0F;
         if (yaw < 0.0F) {
            yaw += 360.0F;
         }

         return yaw;
      }

      public static boolean isDiagonalMovement() {
         float quadrantYaw = movementYaw() % 90.0F;
         return quadrantYaw > 20.0F && quadrantYaw < 70.0F;
      }
   }

   /** Horizontal movement BPS, sampled after motion rather than during rendering. */
   public static final class ScaffoldBpsTracker {
      private Object player;
      private Object world;
      private long lastTick;
      private double lastX;
      private double lastZ;
      private double blocksPerSecond;

      public void sample(Object player, Object world, long tick, double x, double z, double timerMultiplier) {
         if (player == null || world == null || !Double.isFinite(x) || !Double.isFinite(z)
            || !Double.isFinite(timerMultiplier) || timerMultiplier < 0) {
            reset();
            return;
         }
         boolean sameSession = this.player == player && this.world == world;
         if (sameSession && tick == this.lastTick) {
            return;
         }
         // BPS = horizontal distance per tick * 20 TPS * timer multiplier.
         // Prime after enable, respawn, dimension changes or missing ticks; never measure from the origin.
         this.blocksPerSecond = sameSession && tick - this.lastTick == 1
            ? Math.hypot(x - this.lastX, z - this.lastZ) * 20 * timerMultiplier : 0;
         this.player = player;
         this.world = world;
         this.lastTick = tick;
         this.lastX = x;
         this.lastZ = z;
      }

      /** Reads do not decay the value: rendering at 30 or 240 FPS yields the same tick sample. */
      public double blocksPerSecond(Object player, Object world) {
         return this.player == player && this.world == world ? this.blocksPerSecond : 0;
      }

      public void reset() {
         this.player = null;
         this.world = null;
         this.blocksPerSecond = 0;
      }
   }
}
