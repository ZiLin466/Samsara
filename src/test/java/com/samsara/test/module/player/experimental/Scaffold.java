package com.samsara.test.module.player.experimental;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.event.impl.EventMoveInput;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.BooleanSetting;
import com.samsara.setting.ModeSetting;
import com.samsara.setting.NumberSetting;
import com.samsara.ui.dynamicIsland.DynamicIslandManager;
import com.samsara.util.Wrapper;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.nio.charset.StandardCharsets;
import mixins.ClientInputAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class Scaffold extends Feature {
   private final ModeSetting mode;
   private final NumberSetting closetYawSpeed;
   private final NumberSetting closetPitchSpeed;
   private final NumberSetting closetAcceleration;
   private final NumberSetting closetEdgeMargin;
   private final NumberSetting closetLookAhead;
   private final ClosetBridge closet = new ClosetBridge();
   private LocalPlayer sessionPlayer;
   private ClientLevel sessionWorld;
   private String activeMode;
   private final NumberSetting rotationSpeed;
   private static final String KEEP_Y_LABEL = "Keep Y";
   private static final String ROTATION_SPEED_LABEL = "Rotation Speed";
   private static final String SCAFFOLD_LABEL = "Scaffold";
   private float rotationPitch;
   private int placedBlocks;
   private final ModeSetting rotations;
   private final float[] yawOffsets;
   private int previousSlot;
   private boolean jumpStartedOnGround;
   private final BooleanSetting watchdogTower;
   private boolean rotationUpdated;
   private float rotationYaw;
   private static final String TELLY_RMB_LABEL = "Telly RMB";
   private final PlacementTarget placementTarget;
   private int bridgeY;
   private static final String BACKWARD_LABEL = "Backward";
   private static final String RAYCAST2_LABEL = "Raycast2";
   private float targetPitch;
   private final BooleanSetting tellyRmb;
   private final NumberSetting sneakDelay;
   private int ticksSincePlacement;
   private final BooleanSetting keepY;
   private static final String TELLY_LABEL = "Telly";
   private static final String ROTATIONS_LABEL = "Rotations";
   private static final String WATCHDOG_TOWER_LABEL = "Watchdog Tower";
   private static final String SNEAK_LABEL = "Sneak";
   private static final String HIT_VEC_LABEL = "HitVec";
   private float targetYaw;
   private static final String OFFSET_LABEL = "Offset";
   private static final String SNEAK_DELAY_LABEL = "Sneak Delay";
   private boolean pendingWatchdogJump;
   private static final String DISABLED_LABEL = "Disabled";
   private static final String WATCHDOG3_LABEL = "Watchdog3";
   private static final String FAST_MODE_LABEL = "Fast Mode";
   private static final String RAYCAST_LABEL = "Raycast";
   private boolean rightMousePressed;
   private final ModeSetting fastMode;
   private final BooleanSetting sneak;
   private static final String WATCHDOG2_LABEL = "Watchdog2";
   private static final String WATCHDOG_LABEL = "Watchdog";
   private final float[] pitchOffsets;
   private boolean rotationAligned;

   private float[] placementRotation(BlockHitResult blockHit) {
      Vec3 eyePosition = mc.player.getEyePosition();
      Vec3 position = blockHit.getLocation();
      double deltaX = position.x - eyePosition.x;
      double deltaY = position.y - eyePosition.y;
      double deltaZ = position.z - eyePosition.z;
      double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
      float pitch = (float)(-Math.toDegrees(Math.atan2(deltaY, horizontalDistance)));
      String rotationsName = this.rotations.getValue();
      byte modeIndex = -1;
      int modeHash = rotationsName.hashCode();
      if (modeHash == -2133157087) {
         if (rotationsName.equals(HIT_VEC_LABEL)) {
            modeIndex = 4;
         }
      } else if (modeHash == -2108346365) {
         if (rotationsName.equals(BACKWARD_LABEL)) {
            modeIndex = 0;
         }
      } else if (modeHash == -1935912781) {
         if (rotationsName.equals(OFFSET_LABEL)) {
            modeIndex = 1;
         }
      } else if (modeHash == -1642289591) {
         if (rotationsName.equals(RAYCAST_LABEL)) {
            modeIndex = 2;
         }
      } else if (modeHash == 628630281) {
         if (rotationsName.equals(RAYCAST2_LABEL)) {
            modeIndex = 3;
         }
      }

      switch (modeIndex) {
         case 0:
            return new float[]{MoveDirectionUtil.movementYaw() - 180.0F, pitch};
         case 1:
            return new float[]{this.backwardOffsetYaw(), 83.0F};
         case 2:
            return this.findRaycastRotation(blockHit, MoveDirectionUtil.movementYaw() - 180.0F);
         case 3:
            return this.findRaycastRotation(blockHit, this.targetYaw);
         case 4:
         default:
            float yaw = (float)Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0F;
            return new float[]{yaw, pitch};
      }
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
      if (!this.rotationUpdated) {
         this.rotationUpdated = true;
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
            this.rotationAligned = true;
         } else {
            this.rotationAligned = false;
         }
      }
   }

   private float[] findRaycastRotation(BlockHitResult targetHit, float baseYaw) {
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
               return new float[]{yaw, pitch};
            }
         }
      }

      this.rotationAligned = false;
      this.rotationUpdated = true;
      return new float[]{this.targetYaw, this.targetPitch};
   }

   private BlockHitResult raycastBlock(float yaw, float pitch, float range) {
      Vec3 eyePosition = mc.getCameraEntity().getEyePosition(1.0F);
      Vec3 lookDirection = this.lookVector(yaw, pitch);
      Vec3 rayEnd = eyePosition.add(lookDirection.x * (double)range, lookDirection.y * (double)range, lookDirection.z * (double)range);
      return mc.level.clip(new ClipContext(eyePosition, rayEnd, Block.COLLIDER, Fluid.NONE, mc.getCameraEntity()));
   }

   public Scaffold() {
      super(SCAFFOLD_LABEL, Category.PLAYER);
      this.mode = new ModeSetting("Mode", this, "Blatant", new String[]{"Blatant", "Closet"}) {
         @Override public void setValue(String value) {
            super.setValue(value);
            if (Scaffold.this.isEnabled()) Events.refreshListeners();
         }
      };
      this.rotations = new ModeSetting(ROTATIONS_LABEL, this, HIT_VEC_LABEL, new String[]{HIT_VEC_LABEL, BACKWARD_LABEL, OFFSET_LABEL, RAYCAST_LABEL, RAYCAST2_LABEL});
      this.rotationSpeed = new NumberSetting(ROTATION_SPEED_LABEL, this, 2.0, 0.0, 10.0, 0.5);
      this.fastMode = new ModeSetting(FAST_MODE_LABEL, this, DISABLED_LABEL, new String[]{DISABLED_LABEL, TELLY_LABEL, WATCHDOG_LABEL, WATCHDOG2_LABEL, WATCHDOG3_LABEL});
      this.tellyRmb = new BooleanSetting(TELLY_RMB_LABEL, this, false);
      this.keepY = new BooleanSetting(KEEP_Y_LABEL, this, false);
      this.watchdogTower = new BooleanSetting(WATCHDOG_TOWER_LABEL, this, false);
      this.sneak = new BooleanSetting(SNEAK_LABEL, this, false);
      this.sneakDelay = new NumberSetting(SNEAK_DELAY_LABEL, this, 0.0, 0.0, 25.0, 1.0);
      for (var setting : this.settings) {
         if (setting != this.mode) setting.setVisible(() -> this.mode.is("Blatant"));
      }
      this.closetYawSpeed = new NumberSetting("Closet Yaw Speed", this, 22, 5, 35, 1);
      this.closetPitchSpeed = new NumberSetting("Closet Pitch Speed", this, 14, 5, 25, 1);
      this.closetAcceleration = new NumberSetting("Closet Acceleration", this, 5, 1, 8, 0.5);
      this.closetEdgeMargin = new NumberSetting("Closet Edge Margin", this, 0.04, 0.01, 0.15, 0.01);
      this.closetLookAhead = new NumberSetting("Closet Look Ahead", this, 0.65, 0.3, 1, 0.05);
      this.closetYawSpeed.setVisible(() -> this.mode.is("Closet"));
      this.closetPitchSpeed.setVisible(() -> this.mode.is("Closet"));
      this.closetAcceleration.setVisible(() -> this.mode.is("Closet"));
      this.closetEdgeMargin.setVisible(() -> this.mode.is("Closet"));
      this.closetLookAhead.setVisible(() -> this.mode.is("Closet"));
      this.placementTarget = new PlacementTarget();
      this.previousSlot = -1;
      this.yawOffsets = new float[]{
         0.0F,
         -1.0F,
         1.0F,
         -2.0F,
         2.0F,
         -3.0F,
         3.0F,
         -4.0F,
         4.0F,
         -5.0F,
         5.0F,
         -6.0F,
         6.0F,
         -7.0F,
         7.0F,
         -8.0F,
         8.0F,
         -9.0F,
         9.0F,
         -10.0F,
         10.0F,
         -11.0F,
         11.0F,
         -12.0F,
         12.0F,
         -13.0F,
         13.0F,
         -14.0F,
         14.0F,
         -15.0F,
         15.0F,
         -16.0F,
         16.0F,
         -17.0F,
         17.0F,
         -18.0F,
         18.0F,
         -19.0F,
         19.0F,
         -20.0F,
         20.0F,
         -22.0F,
         22.0F,
         -24.0F,
         24.0F,
         -26.0F,
         26.0F,
         -28.0F,
         28.0F,
         -30.0F,
         30.0F,
         -32.0F,
         32.0F,
         -34.0F,
         34.0F,
         -36.0F,
         36.0F,
         -38.0F,
         38.0F,
         -40.0F,
         40.0F,
         -42.0F,
         42.0F,
         -44.0F,
         44.0F,
         -46.0F,
         46.0F,
         -48.0F,
         48.0F,
         -50.0F,
         50.0F,
         -52.0F,
         52.0F,
         -54.0F,
         54.0F,
         -56.0F,
         56.0F,
         -58.0F,
         58.0F,
         -60.0F,
         60.0F,
         -62.0F,
         62.0F,
         -64.0F,
         64.0F,
         -66.0F,
         66.0F,
         -68.0F,
         68.0F,
         -70.0F,
         70.0F,
         -72.0F,
         72.0F,
         -74.0F,
         74.0F,
         -76.0F,
         76.0F,
         -78.0F,
         78.0F,
         -80.0F,
         80.0F,
         -82.0F,
         82.0F,
         -84.0F,
         84.0F,
         -86.0F,
         86.0F,
         -88.0F,
         88.0F,
         -90.0F,
         90.0F,
         -92.0F,
         92.0F,
         -94.0F,
         94.0F,
         -96.0F,
         96.0F,
         -98.0F,
         98.0F,
         -100.0F,
         100.0F,
         -102.0F,
         102.0F,
         -104.0F,
         104.0F,
         -106.0F,
         106.0F,
         -108.0F,
         108.0F,
         -110.0F,
         110.0F,
         -112.0F,
         112.0F,
         -114.0F,
         114.0F,
         -116.0F,
         116.0F,
         -118.0F,
         118.0F,
         -120.0F,
         120.0F,
         -122.0F,
         122.0F,
         -124.0F,
         124.0F,
         -126.0F,
         126.0F,
         -128.0F,
         128.0F,
         -130.0F,
         130.0F,
         -132.0F,
         132.0F,
         -134.0F,
         134.0F,
         -136.0F,
         136.0F,
         -138.0F,
         138.0F,
         -140.0F,
         140.0F,
         -142.0F,
         142.0F,
         -144.0F,
         144.0F,
         -146.0F,
         146.0F,
         -148.0F,
         148.0F,
         -150.0F,
         150.0F,
         -152.0F,
         152.0F,
         -154.0F,
         154.0F,
         -156.0F,
         156.0F,
         -158.0F,
         158.0F,
         -160.0F,
         160.0F,
         -162.0F,
         162.0F,
         -164.0F,
         164.0F,
         -166.0F,
         166.0F,
         -168.0F,
         168.0F,
         -170.0F,
         170.0F,
         -172.0F,
         172.0F,
         -174.0F,
         174.0F,
         -176.0F,
         176.0F,
         -178.0F,
         178.0F,
         -180.0F,
         180.0F
      };
      this.pitchOffsets = new float[]{
         0.0F,
         -1.0F,
         1.0F,
         -2.0F,
         2.0F,
         -3.0F,
         3.0F,
         -4.0F,
         4.0F,
         -5.0F,
         5.0F,
         -6.0F,
         6.0F,
         -7.0F,
         7.0F,
         -8.0F,
         8.0F,
         -9.0F,
         9.0F,
         -10.0F,
         10.0F,
         -11.0F,
         11.0F,
         -12.0F,
         12.0F,
         -13.0F,
         13.0F,
         -14.0F,
         14.0F,
         -15.0F,
         15.0F,
         -16.0F,
         16.0F,
         -17.0F,
         17.0F,
         -18.0F,
         18.0F,
         -19.0F,
         19.0F,
         -20.0F,
         20.0F,
         -22.0F,
         22.0F,
         -24.0F,
         24.0F,
         -26.0F,
         26.0F,
         -28.0F,
         28.0F,
         -30.0F,
         30.0F,
         -32.0F,
         32.0F,
         -34.0F,
         34.0F,
         -36.0F,
         36.0F,
         -38.0F,
         38.0F,
         -40.0F,
         40.0F,
         -42.0F,
         42.0F,
         -44.0F,
         44.0F,
         -46.0F,
         46.0F,
         -48.0F,
         48.0F,
         -50.0F,
         50.0F,
         -52.0F,
         52.0F,
         -54.0F,
         54.0F,
         -56.0F,
         56.0F,
         -58.0F,
         58.0F,
         -60.0F,
         60.0F,
         -62.0F,
         62.0F,
         -64.0F,
         64.0F,
         -66.0F,
         66.0F,
         -68.0F,
         68.0F,
         -70.0F,
         70.0F,
         -72.0F,
         72.0F,
         -74.0F,
         74.0F,
         -76.0F,
         76.0F,
         -78.0F,
         78.0F,
         -80.0F,
         80.0F,
         -82.0F,
         82.0F,
         -84.0F,
         84.0F,
         -86.0F,
         86.0F,
         -88.0F,
         88.0F,
         -90.0F,
         90.0F
      };
   }

   private Vec3 lookVector(float yaw, float pitch) {
      float yawCos = Mth.cos((double)(-yaw * (float) (Math.PI / 180.0) - (float) Math.PI));
      float yawSin = Mth.sin((double)(-yaw * (float) (Math.PI / 180.0) - (float) Math.PI));
      float pitchCos = -Mth.cos((double)(-pitch * (float) (Math.PI / 180.0)));
      float pitchSin = Mth.sin((double)(-pitch * (float) (Math.PI / 180.0)));
      return new Vec3((double)(yawSin * pitchCos), (double)pitchSin, (double)(yawCos * pitchCos));
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
      double nearestDistance = Double.MAX_VALUE;
      PlacementTarget nearestPlacement = null;

      for (int offsetX = -3; offsetX <= 3; offsetX++) {
         for (int offsetY = -2; offsetY <= 0; offsetY++) {
            for (int offsetZ = -3; offsetZ <= 3; offsetZ++) {
               BlockPos candidatePosition = targetPosition.offset(offsetX, offsetY, offsetZ);
               if (!mc.level.getBlockState(targetPosition).canBeReplaced()) {
                  return null;
               }

               if (!mc.level.isEmptyBlock(candidatePosition)) {
                  Direction face = this.placementFace(candidatePosition, targetPosition);
                  if (face != null) {
                     double distanceSquared = mc.player.distanceToSqr((double)candidatePosition.getX() + 0.5, (double)candidatePosition.getY() + 0.5, (double)candidatePosition.getZ() + 0.5);
                     if (distanceSquared < nearestDistance && face != Direction.DOWN) {
                        nearestDistance = distanceSquared;
                        this.placementTarget.set(candidatePosition, face);
                        nearestPlacement = this.placementTarget;
                     }
                  }
               }
            }
         }
      }

      return nearestPlacement;
   }

   @Override
   public void onEvent(Event event) {
      if (!ensureSession()) return;
      if (this.mode.is("Closet")) {
         this.closet.onEvent(event);
         return;
      }
      if (event == Events.POST_MOTION) {
         DynamicIslandManager.sampleScaffoldMovement();
      }

      if (event == Events.MOUSE_BUTTON) {
         if (mc.gui.screen() == null) {
            event.setCancelled(true);
         }

         if (Events.MOUSE_BUTTON.getButton() == 1) {
            this.rightMousePressed = Events.MOUSE_BUTTON.isPressed();
         }
      }

      if (event == Events.ROTATION) {
         boolean shouldSneak = true;
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

         this.rotationUpdated = false;
         BlockPos blockPosition = mc.player.blockPosition().below();
         if (this.keepY.getValue()) {
            blockPosition = blockPosition.atY(this.bridgeY);
         }

         if (mc.level.isEmptyBlock(blockPosition) && shouldSneak) {
            int blockSlot = this.findBlockSlot();
            if (blockSlot != -1) {
               PlacementTarget placement = this.getPlaceData(blockPosition);
               if (placement != null) {
                  Vec3 position = Vec3.atCenterOf(placement.position);
                  BlockHitResult blockHit = new BlockHitResult(position, placement.face, placement.position, false);
                  float[] angles = this.placementRotation(blockHit);
                  this.updateRotation(angles[0], angles[1]);
                  this.targetYaw = angles[0];
                  this.targetPitch = angles[1];
                  if (this.watchdogTower.getValue() && mc.options.keyJump.isDown() && mc.player.onGround()) {
                     this.rotationAligned = false;
                  }

                  if (this.rotationAligned) {
                     mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, blockHit);
                     mc.player.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, false);
                     this.placedBlocks++;
                     this.ticksSincePlacement = 10;
                  }
               }
            }
         }

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

      if (event == Events.PRE_MOTION && (this.fastMode.is(WATCHDOG3_LABEL) || this.watchdogTower.getValue() && mc.options.keyJump.isDown() && !mc.player.onGround())) {
         float backwardYaw = MoveDirectionUtil.movementYaw() - 180.0F;
         Minecraft.getInstance().player.yHeadRot = backwardYaw;
         Minecraft.getInstance().player.yBodyRot = backwardYaw;
      }

      if (event == Events.TICK) {
         int placementSlot = this.findBlockSlot();
         if (placementSlot != -1 && mc.player.getInventory().getSelectedSlot() != placementSlot) {
            mc.player.getInventory().setSelectedSlot(placementSlot);
         }
      }

      if (event == Events.POST_MOVE_INPUT) {
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
   }

   @Override
   public void onEnable() {
      this.sessionPlayer = null;
      this.sessionWorld = null;
      this.activeMode = null;
      this.previousSlot = -1;
      this.closet.reset();
      ensureSession();
   }

   private void initializeBlatant() {
      this.bridgeY = (int)(mc.player.getY() - 1.0);
      this.rotationYaw = mc.player.getYRot();
      this.rotationPitch = mc.player.getXRot();
      this.targetYaw = MoveDirectionUtil.movementYaw() - 180.0F;
      this.targetPitch = 83.0F;
      this.rotationPitch = 83.0F;
      this.jumpStartedOnGround = false;
      this.rotationUpdated = this.rotationAligned = this.pendingWatchdogJump = this.rightMousePressed = false;
      this.ticksSincePlacement = this.placedBlocks = 0;
   }

   @Override
   public void onDisable() {
      if (mc.player != null && mc.player == this.sessionPlayer) {
         if (this.previousSlot >= 0 && this.previousSlot < 9) mc.player.getInventory().setSelectedSlot(this.previousSlot);
         mc.player.yHeadRot = mc.player.yBodyRot = mc.player.getYRot();
         mc.player.yHeadRotO = mc.player.yBodyRotO = mc.player.getYRot();
         mc.player.yRotO = mc.player.getYRot();
         mc.player.xRotO = mc.player.getXRot();
      }
      this.previousSlot = -1;
      this.rotationUpdated = this.rotationAligned = this.pendingWatchdogJump = this.rightMousePressed = false;
      this.ticksSincePlacement = this.placedBlocks = 0;
      this.closet.reset();
      this.sessionPlayer = null;
      this.sessionWorld = null;
      this.activeMode = null;
   }

   @Override
   public int getPriority(Event event) {
      // Run after input corrections so the bridge follows the player's actual controls.
      return this.mode.is("Closet")
         && (event == Events.ROTATION || event == Events.MOVE_INPUT || event == Events.POST_MOVE_INPUT || event == Events.SPRINT) ? 100 : 0;
   }

   private boolean ensureSession() {
      if (mc.player == null || mc.level == null || mc.gameMode == null) {
         this.closet.reset();
         this.sessionPlayer = null;
         this.sessionWorld = null;
         this.activeMode = null;
         this.previousSlot = -1;
         return false;
      }
      boolean newSession = mc.player != this.sessionPlayer || mc.level != this.sessionWorld;
      if (newSession) {
         this.sessionPlayer = mc.player;
         this.sessionWorld = mc.level;
         this.previousSlot = mc.player.getInventory().getSelectedSlot();
      }
      if (newSession || !this.mode.getValue().equals(this.activeMode)) {
         this.activeMode = this.mode.getValue();
         this.rotationUpdated = this.rotationAligned = this.pendingWatchdogJump = this.rightMousePressed = false;
         this.ticksSincePlacement = this.placedBlocks = 0;
         this.closet.reset();
         if (this.mode.is("Closet")) this.closet.begin();
         else initializeBlatant();
         setSuffix(this.activeMode);
      }
      return true;
   }

   private final class ClosetBridge {
      private final TurnAxis yaw = new TurnAxis();
      private final TurnAxis pitch = new TurnAxis();
      private final BridgeRoute route = new BridgeRoute();
      private final BridgeStep step = new BridgeStep();
      private final Set<BlockPos> path = new LinkedHashSet<>();
      private ClosetAim target;
      private Input bridgeInput;
      private Vec3 travel = Vec3.ZERO;
      private Vec3 predictedMotion = Vec3.ZERO;
      private Vec3 lastAimOffset;
      private Direction lastAimFace;
      private boolean edgeSneaking;
      private boolean aiming;
      private boolean walking;
      private boolean hasBlocks;
      private boolean stepDeferred;
      private boolean waitingForJump;
      private int side = 1;
      private int rotationTick = Integer.MIN_VALUE;
      private int placementTick = Integer.MIN_VALUE;

      void begin() {
         this.yaw.reset(mc.player.getYRot());
         this.pitch.reset(mc.player.getXRot());
         this.route.reset();
         this.step.reset(mc.player.getY());
      }

      void reset() {
         this.target = null;
         this.bridgeInput = null;
         this.path.clear();
         this.predictedMotion = this.travel = Vec3.ZERO;
         this.lastAimOffset = null;
         this.lastAimFace = null;
         this.edgeSneaking = this.aiming = false;
         this.walking = this.waitingForJump = this.hasBlocks = this.stepDeferred = false;
         this.side = 1;
         this.rotationTick = this.placementTick = Integer.MIN_VALUE;
         this.yaw.reset(0);
         this.pitch.reset(0);
         this.route.reset();
      }

      private boolean available() {
         return mc.gui.screen() == null && mc.player.isAlive() && !mc.player.isSpectator()
            && !mc.player.getAbilities().flying && !mc.player.isPassenger()
            && !mc.player.isInWater() && !mc.player.isInLava() && !mc.player.isUsingItem();
      }

      void onEvent(Event event) {
         if (event == Events.POST_MOTION) DynamicIslandManager.sampleScaffoldMovement();
         if (!available()) {
            reset();
            begin();
            return;
         }
         if (event == Events.ROTATION) updateRotation();
         else if (event == Events.MOVE_INPUT) updateInput(Events.MOVE_INPUT);
         else if (event == Events.POST_MOVE_INPUT) restoreMovementInput();
         else if (event == Events.POST_MOTION) place();
         else if (event == Events.SPRINT && this.aiming) Events.SPRINT.setSprinting(false);
      }

      private void updateRotation() {
         if (this.rotationTick == mc.player.tickCount) return;
         this.rotationTick = mc.player.tickCount;
         EventMoveInput input = new EventMoveInput().reset(mc.options.keyUp.isDown(), mc.options.keyDown.isDown(),
            mc.options.keyLeft.isDown(), mc.options.keyRight.isDown(), mc.options.keyJump.isDown(),
            mc.options.keyShift.isDown(), false);
         int forward = (input.isForward() ? 1 : 0) - (input.isBackward() ? 1 : 0);
         int strafe = (input.isLeft() ? 1 : 0) - (input.isRight() ? 1 : 0);
         this.walking = forward != 0 || strafe != 0;
         boolean changed = this.route.update(mc.player.getYRot(), forward, strafe, mc.player.position());
         if (changed) {
            this.target = null;
            this.lastAimOffset = null;
            this.lastAimFace = null;
            if (strafe != 0) this.side = strafe > 0 ? -1 : 1;
         }
         this.stepDeferred = mc.player.onGround() && input.isJump() && (!centerSupported()
            || this.walking && !canRiseAt(mc.player.position(), this.route.direction()));
         this.step.update(mc.player.getY(), mc.player.onGround(), input.isJump() && !this.stepDeferred);
         this.travel = this.walking ? this.route.travel(mc.player.position(), mc.player.getDeltaMovement()) : Vec3.ZERO;
         this.predictedMotion = predictWalking(this.travel);
         planPath();
         int slot = blockSlot();
         this.hasBlocks = slot >= 0;
         this.target = slot < 0 ? null : findAim(slot);
         this.aiming = this.target != null || this.edgeSneaking || slot >= 0 && (this.step.rising || this.aiming && this.walking);
         if (!this.aiming) {
            // Keep rotation continuity while handing control back to the mouse.
            if (Math.abs(Mth.wrapDegrees(this.yaw.angle - mc.player.getYRot())) < 0.5
                && Math.abs(this.pitch.angle - mc.player.getXRot()) < 0.5) {
               begin();
               return;
            }
         } else {
            mc.player.setSprinting(false);
            if (slot >= 0) mc.player.getInventory().setSelectedSlot(slot);
         }
         float previousYaw = this.yaw.angle, previousPitch = this.pitch.angle;
         float sideYaw = this.route.aimYaw(this.side);
         float goalYaw = this.target != null ? this.target.yaw() : this.aiming ? sideYaw : mc.player.getYRot();
         float goalPitch = this.target != null ? this.target.pitch() : this.aiming ? this.pitch.angle : mc.player.getXRot();
         double sensitivity = mc.options.sensitivity().get() * 0.6 + 0.2;
         double step = sensitivity * sensitivity * sensitivity * 1.2;
         this.yaw.advance(goalYaw, (float)closetYawSpeed.getValue(), (float)closetAcceleration.getValue(), step, true);
         this.pitch.advance(goalPitch, (float)closetPitchSpeed.getValue(), (float)closetAcceleration.getValue(), step, false);
         Events.ROTATION.setPreviousYaw(previousYaw);
         Events.ROTATION.setPreviousPitch(previousPitch);
         Events.ROTATION.setYaw(this.yaw.angle);
         Events.ROTATION.setPitch(this.pitch.angle);
         Events.ROTATION.setMovementCorrection(this.aiming);
         Events.ROTATION.setUseClientRotation(true);
      }

      private void updateInput(EventMoveInput input) {
         this.travel = this.walking ? this.route.travel(mc.player.position(), mc.player.getDeltaMovement()) : Vec3.ZERO;
         boolean jumpRequested = input.isJump();
         boolean automaticSneak = this.step.allowsSneaking(jumpRequested);
         boolean jump = jumpRequested;
         if (this.hasBlocks && this.stepDeferred) jump = false;
         this.waitingForJump = this.hasBlocks && jump && mc.player.onGround() && this.step.rising && !readyToJump();
         if (this.waitingForJump) jump = false;
         input.setJump(jump);
         this.predictedMotion = predictWalking(this.travel);
         boolean landingUnsafe = this.hasBlocks && this.step.rising && !supportedLanding(this.predictedMotion.scale(2));
         boolean holdMovement = this.waitingForJump || this.hasBlocks && this.step.rising
            && (this.step.needsClearance(mc.player.getY()) || landingUnsafe)
            || this.hasBlocks && jumpRequested && this.stepDeferred && !supportedPath(this.predictedMotion, closetEdgeMargin.getValue());
         if (holdMovement) this.travel = brakingInput(mc.player.getDeltaMovement(), walkingAcceleration());
         this.predictedMotion = predictWalking(this.travel);
         boolean permitted = automaticSneak && mc.player.onGround()
            && (this.walking || this.predictedMotion.horizontalDistanceSqr() > 1.0E-5);
         double margin = closetEdgeMargin.getValue();
         boolean entering = !supportedPath(this.predictedMotion, margin);
         boolean remaining = !supportedPath(this.predictedMotion, margin + 0.03);
         this.edgeSneaking = edgeState(this.edgeSneaking, permitted, entering, remaining);
         boolean interactive = this.target != null
            && mc.level.getBlockState(this.target.support()).getMenuProvider(mc.level, this.target.support()) != null;
         input.setSneak(this.step.shouldSneak(input.isSneak() || mc.options.keyShift.isDown(), jumpRequested, this.edgeSneaking, interactive));
         if (this.aiming || this.edgeSneaking) {
            input.setSprint(false);
            mc.player.setSprinting(false);
         }
         this.bridgeInput = new Input(input.isForward(), input.isBackward(), input.isLeft(), input.isRight(), input.isJump(), input.isSneak(), input.isSprint());
      }

      private boolean readyToJump() {
         if (this.target == null) return false;
         BlockHitResult hit = ray(this.yaw.angle, this.pitch.angle, mc.player.blockInteractionRange());
         return validHit(hit, this.target.place(), mc.player.getInventory().getSelectedSlot(), true)
            && hit.getBlockPos().equals(this.target.support()) && hit.getDirection() == this.target.face();
      }

      private boolean centerSupported() {
         Vec3 position = mc.player.position();
         AABB center = new AABB(position.x - 0.0001, position.y - 0.08, position.z - 0.0001,
            position.x + 0.0001, position.y + 0.001, position.z + 0.0001);
         return !mc.level.noCollision(mc.player, center);
      }

      private void restoreMovementInput() {
         if (this.bridgeInput == null) return;
         if (this.aiming) {
            Vec2 move = relativeInput(this.travel, Events.ROTATION.getYaw());
            ((ClientInputAccessor)mc.player.input).setMoveVector(move);
            mc.player.input.keyPresses = new Input(move.y > 0.001, move.y < -0.001, move.x > 0.001, move.x < -0.001,
               this.bridgeInput.jump(), this.bridgeInput.shift(), false);
            return;
         }
         mc.player.input.keyPresses = this.bridgeInput;
         Vec2 move = new Vec2((this.bridgeInput.left() ? 1 : 0) - (this.bridgeInput.right() ? 1 : 0),
            (this.bridgeInput.forward() ? 1 : 0) - (this.bridgeInput.backward() ? 1 : 0));
         ((ClientInputAccessor)mc.player.input).setMoveVector(move.length() > 1 ? move.normalized() : move);
      }

      private double walkingAcceleration() {
         float friction = mc.level.getBlockState(mc.player.getBlockPosBelowThatAffectsMyMovement()).getBlock().getFriction();
         return mc.player.onGround() ? mc.player.getSpeed() * 0.21600002 / (friction * friction * friction) : 0.02;
      }

      private Vec3 predictWalking(Vec3 direction) {
         Vec2 relative = relativeInput(direction, this.aiming ? this.yaw.angle : mc.player.getYRot());
         double largest = Math.max(Math.abs(relative.x), Math.abs(relative.y));
         double length = direction.horizontalDistance();
         // Predict the unsneaked input after vanilla's square-movement conversion.
         Vec3 controls = direction.scale(walkingAcceleration() * (largest > 0 ? Math.min(0.98 * length / largest, 1 / length) : 0));
         Vec3 velocity = mc.player.getDeltaMovement();
         return new Vec3(Math.abs(velocity.x) < 0.003 ? 0 : velocity.x, 0, Math.abs(velocity.z) < 0.003 ? 0 : velocity.z)
            .add(controls);
      }

      private boolean supportedPath(Vec3 motion, double margin) {
         return hasSupport(mc.player.position(), motion, mc.player.getBbWidth(), margin,
            feet -> !mc.level.noCollision(mc.player, feet));
      }

      private boolean supportedLanding(Vec3 motion) {
         Vec3 position = mc.player.position();
         return hasSupport(new Vec3(position.x, this.step.placementY() + 1, position.z), motion,
            mc.player.getBbWidth(), closetEdgeMargin.getValue() + 0.03, feet -> !mc.level.noCollision(mc.player, feet));
      }

      private void planPath() {
         this.path.clear();
         Vec3 line = this.route.linePosition(mc.player.position());
         Vec3 position = new Vec3(line.x, this.step.placementY() + 1, line.z);
         this.path.addAll(walkingPath(position, this.walking ? this.route.direction() : Vec3.ZERO, closetLookAhead.getValue()));
      }

      private int blockSlot() {
         int selected = mc.player.getInventory().getSelectedSlot();
         if (usableBlock(mc.player.getInventory().getItem(selected))) return selected;
         int best = -1, count = 0;
         for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc.player.getInventory().getItem(slot);
            if (usableBlock(stack) && stack.getCount() > count) {
               best = slot;
               count = stack.getCount();
            }
         }
         return best;
      }

      private boolean usableBlock(ItemStack stack) {
         if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem item)) return false;
         var block = item.getBlock();
         return !(block instanceof FallingBlock) && block != Blocks.TNT
            && !block.defaultBlockState().hasBlockEntity()
            && block.defaultBlockState().isCollisionShapeFullBlock(mc.level, BlockPos.ZERO);
      }

      private ClosetAim findAim(int slot) {
         Vec3 eyes = mc.player.getEyePosition();
         ClosetAim immediate = searchAim(slot, eyes);
         if (immediate != null || !this.walking) return immediate;
         // Pre-aim before the side face becomes visible; placement still uses the real eye position.
         Vec3 futureEyes = eyes.add(this.route.direction().scale(closetLookAhead.getValue()));
         return searchAim(slot, futureEyes);
      }

      private ClosetAim searchAim(int slot, Vec3 eyes) {
         double reach = mc.player.blockInteractionRange();
         return searchFaces(eyes, this.pitch.angle, this.route.aimYaw(this.side),
            this.lastAimOffset, this.lastAimFace, this.target, this.path,
            support -> {
               var state = mc.level.getBlockState(support);
               return state.canBeReplaced() ? List.of() : state.getCollisionShape(mc.level, support).toAabbs();
            }, point -> {
               float[] rotation = angles(eyes, point);
               return rayFrom(eyes, rotation[0], rotation[1], reach);
            }, (hit, place) -> validHit(hit, place, slot, true));
      }

      private boolean validHit(BlockHitResult hit, BlockPos expected, int slot, boolean planning) {
         if (hit == null || hit.getType() != HitResult.Type.BLOCK || hit.getDirection() == Direction.DOWN || hit.isInside()) return false;
         BlockPos place = hit.getBlockPos().relative(hit.getDirection());
         if (expected != null && !expected.equals(place) || !this.path.contains(place)) return false;
         if (!mc.level.getWorldBorder().isWithinBounds(place) || !mc.level.getBlockState(place).canBeReplaced()) return false;
         ItemStack stack = mc.player.getInventory().getItem(slot);
         if (!usableBlock(stack)) return false;
         BlockPlaceContext context = new BlockPlaceContext(mc.player, InteractionHand.MAIN_HAND, stack, hit);
         if (!context.canPlace() || !context.getClickedPos().equals(place)) return false;
         var state = ((BlockItem)stack.getItem()).getBlock().getStateForPlacement(context);
         return state != null && state.canSurvive(mc.level, place) && (planning || mc.level.isUnobstructed(state, place,
            net.minecraft.world.phys.shapes.CollisionContext.of(mc.player)));
      }

      private BlockHitResult ray(float yaw, float pitch, double reach) {
         return rayFrom(mc.player.getEyePosition(), yaw, pitch, reach);
      }

      private BlockHitResult rayFrom(Vec3 eyes, float yaw, float pitch, double reach) {
         Vec3 direction = Vec3.directionFromRotation(pitch, yaw);
         return mc.level.clip(new ClipContext(eyes, eyes.add(direction.scale(reach)), Block.OUTLINE, Fluid.NONE, mc.player));
      }

      private void place() {
         if (this.target == null || this.placementTick == mc.player.tickCount || Events.ROTATION.isCancelled()) return;
         ClosetAim placing = this.target;
         int slot = mc.player.getInventory().getSelectedSlot();
         // Use the rotation already sent by sendPosition and recast after the player's movement.
         BlockHitResult hit = ray(Events.ROTATION.getYaw(), Events.ROTATION.getPitch(), mc.player.blockInteractionRange());
         if (!validHit(hit, placing.place(), slot, false) || !hit.getBlockPos().equals(placing.support())
             || hit.getDirection() != placing.face()) return;
         if (mc.level.getBlockState(hit.getBlockPos()).getMenuProvider(mc.level, hit.getBlockPos()) != null
             && !mc.player.isShiftKeyDown()) return;
         this.placementTick = mc.player.tickCount;
         var result = mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
         if (result.consumesAction() && !mc.level.getBlockState(placing.place()).canBeReplaced()) {
            mc.player.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, false);
            this.lastAimOffset = placing.point().subtract(Vec3.atLowerCornerOf(placing.support()));
            this.lastAimFace = placing.face();
            this.target = null;
         }
      }
   }

   private record ClosetAim(BlockPos place, BlockPos support, Direction face, float yaw, float pitch, Vec3 point) { }

   private static final class BridgeRoute {
      private Vec3 anchor;
      private float heading;
      private float cameraReference;
      private float cameraBasis;
      private int forward;
      private int strafe;

      void reset() { this.anchor = null; }

      boolean update(float cameraYaw, int forward, int strafe, Vec3 position) {
         boolean first = this.anchor == null;
         boolean turned = first || Math.abs(Mth.wrapDegrees(cameraYaw - this.cameraReference)) > 32;
         if (turned) {
            this.cameraReference = cameraYaw;
            this.cameraBasis = Math.round(cameraYaw / 45) * 45;
         }
         if (!first && !turned && (forward == 0 && strafe == 0 || forward == this.forward && strafe == this.strafe)) return false;
         float requested = this.cameraBasis + (forward == 0 && strafe == 0 ? 0 : (float)-Math.toDegrees(Math.atan2(strafe, forward)));
         boolean changed = first || Math.abs(Mth.wrapDegrees(requested - this.heading)) > 0.01;
         if (changed) {
            this.heading = requested;
            this.anchor = position;
         }
         if (forward != 0 || strafe != 0) {
            this.forward = forward;
            this.strafe = strafe;
         }
         return changed;
      }

      Vec3 direction() {
         double angle = Math.toRadians(this.heading);
         return new Vec3(-Math.sin(angle), 0, Math.cos(angle));
      }

      float aimYaw(int side) {
         Vec3 direction = direction();
         // A small fixed bias exposes the diagonal connector without switching between its two faces.
         boolean diagonal = Math.abs(direction.x) > 0.1 && Math.abs(direction.z) > 0.1;
         if (!diagonal && this.anchor != null) {
            double x = Math.floor(this.anchor.x) + 0.5 - this.anchor.x;
            double z = Math.floor(this.anchor.z) + 0.5 - this.anchor.z;
            double towardCenter = x * direction.z - z * direction.x;
            if (Math.abs(towardCenter) > 0.05) side = towardCenter > 0 ? -1 : 1;
         }
         if (diagonal && this.anchor != null) {
            double x = this.anchor.x - Math.floor(this.anchor.x), z = this.anchor.z - Math.floor(this.anchor.z);
            double toX = (direction.x > 0 ? 1 - x : x) / Math.abs(direction.x);
            double toZ = (direction.z > 0 ? 1 - z : z) / Math.abs(direction.z);
            if (Math.abs(toX - toZ) > 0.05) {
               double positiveYaw = Math.toRadians(this.heading + 150);
               boolean positiveFacesX = Math.abs(Math.sin(positiveYaw)) > Math.abs(Math.cos(positiveYaw));
               side = positiveFacesX == (toX < toZ) ? 1 : -1;
            }
         }
         return this.heading + side * (diagonal ? 150 : 135);
      }

      Vec3 linePosition(Vec3 position) {
         if (this.anchor == null) return position;
         Vec3 direction = direction();
         Vec3 delta = position.subtract(this.anchor);
         double along = delta.x * direction.x + delta.z * direction.z;
         return new Vec3(this.anchor.x + direction.x * along, position.y, this.anchor.z + direction.z * along);
      }

      Vec3 travel(Vec3 position, Vec3 velocity) {
         Vec3 direction = direction(), lateral = new Vec3(direction.z, 0, -direction.x);
         Vec3 offset = position.subtract(linePosition(position));
         double error = offset.x * lateral.x + offset.z * lateral.z;
         double drift = velocity.x * lateral.x + velocity.z * lateral.z;
         double correction = Mth.clamp(-error * 3 - drift * 2, -0.25, 0.25);
         return direction.add(lateral.scale(correction)).normalize();
      }
   }

   private static final class BridgeStep {
      private int floorY;
      private boolean rising;
      private boolean airborne;

      void reset(double feetY) {
         this.floorY = Mth.floor(feetY - 0.01);
         this.rising = this.airborne = false;
      }

      void update(double feetY, boolean grounded, boolean jump) {
         if (this.rising && !grounded) this.airborne = true;
         if (grounded && (this.airborne || !jump)) this.rising = this.airborne = false;
         if (!this.rising && grounded) {
            this.floorY = Mth.floor(feetY - 0.01);
            this.rising = jump;
         }
      }

      int placementY() { return this.floorY + (this.rising ? 1 : 0); }
      boolean needsClearance(double feetY) { return feetY < placementY() + 1.001; }
      boolean allowsSneaking(boolean jumpRequested) { return !jumpRequested && !this.rising; }
      boolean shouldSneak(boolean manual, boolean jumpRequested, boolean edge, boolean interactive) {
         return manual || allowsSneaking(jumpRequested) && (edge || interactive);
      }
   }

   private static Vec2 relativeInput(Vec3 direction, float facingYaw) {
      double angle = Math.toRadians(facingYaw);
      return new Vec2((float)(direction.x * Math.cos(angle) + direction.z * Math.sin(angle)),
         (float)(direction.z * Math.cos(angle) - direction.x * Math.sin(angle)));
   }

   private static Vec3 brakingInput(Vec3 velocity, double acceleration) {
      Vec3 horizontal = new Vec3(velocity.x, 0, velocity.z);
      double speed = horizontal.horizontalDistance();
      return speed < 0.003 ? Vec3.ZERO : horizontal.scale(-Math.min(1, speed / (acceleration * Math.sqrt(2))) / speed);
   }

   private static boolean canRiseAt(Vec3 position, Vec3 direction) {
      Vec3 next = position.add(direction.scale(0.35));
      return Mth.floor(position.x) == Mth.floor(next.x) && Mth.floor(position.z) == Mth.floor(next.z);
   }

   private static ClosetAim searchFaces(Vec3 eyes, float pitch, float sideYaw, Vec3 lastOffset, Direction lastFace,
                                       ClosetAim previous, Set<BlockPos> path, Function<BlockPos, List<AABB>> shapes,
                                       Function<Vec3, BlockHitResult> trace, BiPredicate<BlockHitResult, BlockPos> valid) {
      if (previous != null && path.contains(previous.place())) {
         BlockHitResult hit = trace.apply(previous.point());
         float[] rotation = angles(eyes, previous.point());
         if (Math.abs(Mth.wrapDegrees(rotation[0] - sideYaw)) < 0.01 && valid.test(hit, previous.place())
             && hit.getBlockPos().equals(previous.support()) && hit.getDirection() == previous.face()) {
            return new ClosetAim(previous.place(), previous.support(), previous.face(), rotation[0], rotation[1], previous.point());
         }
      }
      ClosetAim best = null;
      double bestScore = Double.MAX_VALUE;
      int order = 0;
      for (BlockPos place : path) {
         for (Direction towardSupport : Direction.values()) {
            Direction face = towardSupport.getOpposite();
            if (face == Direction.DOWN) continue;
            BlockPos support = place.relative(towardSupport);
            for (AABB shape : shapes.apply(support)) {
               AABB box = shape.move(support);
               Vec3 point = pointAtYaw(eyes, sideYaw, pitch, box, face);
               if (point == null) continue;
               boolean retained = previous != null && previous.place().equals(place) && previous.support().equals(support) && previous.face() == face;
               BlockHitResult hit = trace.apply(point);
               if (!valid.test(hit, place) || !hit.getBlockPos().equals(support) || hit.getDirection() != face) continue;
               float[] rotation = angles(eyes, point);
               double score = order * 12 + Math.abs(rotation[1] - pitch) * 0.2;
               if (retained) score -= 5;
               if (lastFace == face && lastOffset != null) score += point.distanceTo(Vec3.atLowerCornerOf(support).add(lastOffset)) * 2;
               if (score < bestScore) {
                  bestScore = score;
                  best = new ClosetAim(place, support, face, sideYaw, rotation[1], point);
               }
            }
         }
         order++;
      }
      return best;
   }

   private static Vec3 pointAtYaw(Vec3 eyes, float yaw, float pitch, AABB box, Direction face) {
      double angle = Math.toRadians(yaw), dx = -Math.sin(angle), dz = Math.cos(angle);
      double distance;
      if (face == Direction.UP) {
         if (eyes.y <= box.maxY) return null;
         double near = 0.0001, far = Double.POSITIVE_INFINITY;
         double insetX = Math.min(0.02, (box.maxX - box.minX) * 0.2), insetZ = Math.min(0.02, (box.maxZ - box.minZ) * 0.2);
         double[] origin = {eyes.x, eyes.z}, direction = {dx, dz};
         double[] minimum = {box.minX + insetX, box.minZ + insetZ}, maximum = {box.maxX - insetX, box.maxZ - insetZ};
         for (int axis = 0; axis < 2; axis++) {
            if (Math.abs(direction[axis]) < 1.0E-7) {
               if (origin[axis] < minimum[axis] || origin[axis] > maximum[axis]) return null;
            } else {
               double firstIntersection = (minimum[axis] - origin[axis]) / direction[axis];
               double secondIntersection = (maximum[axis] - origin[axis]) / direction[axis];
               near = Math.max(near, Math.min(firstIntersection, secondIntersection));
               far = Math.min(far, Math.max(firstIntersection, secondIntersection));
            }
         }
         if (far < near) return null;
         distance = Mth.clamp((eyes.y - box.maxY) / Math.tan(Math.toRadians(Mth.clamp(pitch, 70, 89))), near, far);
         return new Vec3(eyes.x + dx * distance, box.maxY, eyes.z + dz * distance);
      }
      boolean xFace = face.getAxis() == Direction.Axis.X;
      double component = xFace ? dx : dz;
      if (face == Direction.DOWN || Math.abs(component) < 1.0E-7) return null;
      double plane = xFace ? face == Direction.EAST ? box.maxX : box.minX : face == Direction.SOUTH ? box.maxZ : box.minZ;
      distance = (plane - (xFace ? eyes.x : eyes.z)) / component;
      if (distance <= 0) return null;
      double x = xFace ? plane : eyes.x + dx * distance;
      double z = xFace ? eyes.z + dz * distance : plane;
      double transverse = xFace ? z : x, minimum = xFace ? box.minZ : box.minX, maximum = xFace ? box.maxZ : box.maxX;
      double margin = Math.min(0.02, (maximum - minimum) * 0.2);
      if (transverse < minimum + margin || transverse > maximum - margin) return null;
      double y = eyes.y - distance * Math.tan(Math.toRadians(Mth.clamp(pitch, 45, 89)));
      double verticalMargin = (box.maxY - box.minY) * 0.2;
      // A near-vertical ray can miss a face edge after mouse-step quantization.
      if (y < box.minY + verticalMargin || y > box.maxY - verticalMargin) y = (box.minY + box.maxY) * 0.5;
      return new Vec3(x, y, z);
   }

   private static boolean edgeState(boolean active, boolean permitted, boolean entering, boolean remaining) {
      return permitted && (active ? remaining : entering);
   }

   private static boolean hasSupport(Vec3 position, Vec3 motion, float width, double margin, Predicate<AABB> collision) {
      int samples = Math.max(1, (int)Math.ceil(motion.horizontalDistance() / 0.04));
      double radius = Math.max(0.08, width * 0.5 - 0.05 - margin);
      for (int i = 0; i <= samples; i++) {
         Vec3 point = position.add(motion.scale((double)i / samples));
         AABB feet = new AABB(point.x - radius, point.y - 0.08, point.z - radius,
            point.x + radius, point.y + 0.001, point.z + radius);
         if (!collision.test(feet)) return false;
      }
      return true;
   }

   private static Set<BlockPos> walkingPath(Vec3 position, Vec3 motion, double lookAhead) {
      Set<BlockPos> path = new LinkedHashSet<>();
      BlockPos under = BlockPos.containing(position.x, position.y - 0.01, position.z);
      path.add(under);
      if (motion.horizontalDistanceSqr() < 1.0E-6) return path;
      Vec3 direction = new Vec3(motion.x, 0, motion.z).normalize();
      if (Math.abs(direction.x) > 0.1 && Math.abs(direction.z) > 0.1) {
         // The diagonal cell has no common face with the last support; retain its two connecting steps.
         path.add(under.offset(-(int)Math.signum(direction.x), 0, 0));
         path.add(under.offset(0, 0, -(int)Math.signum(direction.z)));
      }
      // Search only the imminent walking corridor, including both staircase choices on diagonals.
      int samples = (int)Math.ceil(lookAhead / 0.12);
      for (int i = 1; i <= samples; i++) {
         Vec3 point = position.add(direction.scale(lookAhead * i / samples));
         BlockPos next = BlockPos.containing(point.x, position.y - 0.01, point.z);
         path.add(next);
         if (next.getX() != under.getX() && next.getZ() != under.getZ()) {
            path.add(new BlockPos(next.getX(), under.getY(), under.getZ()));
            path.add(new BlockPos(under.getX(), under.getY(), next.getZ()));
         }
      }
      return path;
   }

   private static float[] angles(Vec3 eyes, Vec3 point) {
      Vec3 delta = point.subtract(eyes);
      return new float[]{(float)Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90,
         (float)-Math.toDegrees(Math.atan2(delta.y, delta.horizontalDistance()))};
   }

   private static final class TurnAxis {
      private float angle;
      private float velocity;

      void reset(float angle) {
         this.angle = angle;
         this.velocity = 0;
      }

      void advance(float target, float speed, float acceleration, double step, boolean wrap) {
         float error = wrap ? Mth.wrapDegrees(target - this.angle) : target - this.angle;
         float brakingSpeed = ((float)Math.sqrt(acceleration * acceleration + 8 * acceleration * Math.abs(error)) - acceleration) * 0.5F;
         float desired = Math.copySign(Math.min(speed, Math.min(Math.abs(error) * 0.65F,
            brakingSpeed)), error);
         this.velocity += Mth.clamp(desired - this.velocity, -acceleration, acceleration);
         long maxSteps = (long)Math.floor(speed / step);
         float delta = (float)(Math.clamp(Math.round(this.velocity / step), -maxSteps, maxSteps) * step);
         this.velocity = delta;
         this.angle += delta;
         if (!wrap) this.angle = Mth.clamp(this.angle, -90, 90);
      }
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

   public static class PlacementTarget {
      public Direction face;
      public BlockPos position;

      public void set(BlockPos position, Direction face) {
         this.position = position;
         this.face = face;
      }

      public PlacementTarget() {
      }
   }

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
