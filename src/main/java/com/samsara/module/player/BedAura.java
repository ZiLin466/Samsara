package com.samsara.module.player;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.module.FeatureManager;
import com.samsara.setting.BooleanSetting;
import com.samsara.setting.NumberSetting;
import mixins.MultiPlayerGameModeAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Plane;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;

public class BedAura extends Feature {
   private static final String RANGE_LABEL = "Range";
   private static final String BREAK_DELAY_LABEL = "Break Delay";
   private static final String BED_AURA_LABEL = "BedAura";
   private static final String SPEED_LABEL = "Speed";
   private static final String WATCHDOG_MODE_LABEL = "Watchdog Mode";
   private static final String ONLY_SSROTATE_LABEL = "Only S/S Rotate";
   private static final String SURROUNDING_LABEL = "Surrounding";
   private static final String ALLOW_KILL_AURA_LABEL = "Allow KillAura";

   private final NumberSetting range = new NumberSetting(RANGE_LABEL, this, 5.0, 1.0, 8.0, 0.5);
   private final NumberSetting speed;
   private final NumberSetting breakDelay;
   private final BooleanSetting surrounding;
   private final BooleanSetting allowKillAura;
   private final BooleanSetting onlySSRotate;
   private final BooleanSetting watchdogMode;

   private int previousSlot;
   private float diggingProgress;
   private float previousDiggingProgress;
   private int breakDelayTicks;
   private BlockPos diggingPosition;
   public int searchTicks;
   private BlockPos targetBedPosition;
   public boolean rotatingToBed;
   private ClientLevel diggingLevel;
   private BlockState diggingState;
   private float diggingGoal = 1;

   /** The actual block being mined, which can be a defense block above the bed. */
   public record DiggingTarget(BlockPos position, BlockState state, float progress) { }

   public DiggingTarget diggingTarget(float partialTick) {
      if (!isEnabled() || mc.player == null || mc.level == null || mc.level != this.diggingLevel
          || this.diggingPosition == null || this.diggingState == null || mc.level.getBlockState(this.diggingPosition).isAir()) {
         return null;
      }
      float progress = this.previousDiggingProgress + (this.diggingProgress - this.previousDiggingProgress) * Math.clamp(partialTick, 0, 1);
      return new DiggingTarget(this.diggingPosition, this.diggingState, Math.clamp(progress / this.diggingGoal, 0, 1));
   }

   private void mineBed(BlockPos bedPosition, BlockState bedState) {
      if (this.surrounding.getValue()) {
         BlockPos aboveBed = bedPosition.above();
         BlockPos otherHalf = bedPosition.relative((Direction)bedState.getValue(BedBlock.FACING), bedState.getValue(BedBlock.PART) == BedPart.HEAD ? -1 : 1);
         boolean exposedSide = false;

         for (Direction direction : Plane.HORIZONTAL) {
            BlockPos adjacentBed = bedPosition.relative(direction);
            BlockPos adjacentOtherHalf = otherHalf.relative(direction);
            BlockState adjacentBedState = mc.level.getBlockState(adjacentBed);
            BlockState adjacentOtherState = mc.level.getBlockState(adjacentOtherHalf);
            if (adjacentBedState.getBlock() == Blocks.AIR) {
               exposedSide = true;
               break;
            }

            if (adjacentOtherState.getBlock() == Blocks.AIR) {
               exposedSide = true;
               break;
            }
         }

         if (!exposedSide) {
            BlockState defenseState = mc.level.getBlockState(aboveBed);
            if (!defenseState.isAir()) {
               bedPosition = aboveBed;
               bedState = defenseState;
               int bestToolSlot = -1;
               float bestDestroySpeed = 1.0F;

               for (int slot = 0; slot < 9; slot++) {
                  ItemStack stack = mc.player.getInventory().getItem(slot);
                  float destroySpeed = stack.getDestroySpeed(bedState);
                  if (destroySpeed > bestDestroySpeed) {
                     bestDestroySpeed = destroySpeed;
                     bestToolSlot = slot;
                  }
               }

               if (bestToolSlot != -1 && mc.player.getInventory().getSelectedSlot() != bestToolSlot) {
                  this.previousSlot = mc.player.getInventory().getSelectedSlot();
                  mc.player.getInventory().setSelectedSlot(bestToolSlot);
               }
            }
         }
      }

      double centerX = (double)bedPosition.getX() + 0.5;
      double centerY = (double)bedPosition.getY() + 0.5;
      double centerZ = (double)bedPosition.getZ() + 0.5;
      double deltaX = centerX - mc.player.getX();
      double deltaY = centerY - mc.player.getEyeY();
      double deltaZ = centerZ - mc.player.getZ();
      double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
      float yaw = (float)Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0F;
      float pitch = (float)(-Math.toDegrees(Math.atan2(deltaY, horizontalDistance)));
      if (!this.onlySSRotate.getValue() || this.diggingProgress == 0.0F || this.diggingProgress >= 1.0F) {
         Events.ROTATION.setYaw(yaw);
         Events.ROTATION.setPitch(pitch);
         this.rotatingToBed = true;
      }

      float destroyProgress = (float)((double)bedState.getDestroyProgress(mc.player, mc.player.level(), bedPosition) * this.speed.getValue());
      if (mc.player.isEyeInFluid(FluidTags.WATER) && this.watchdogMode.getValue()) {
         destroyProgress *= 5.0F;
      }

      float progressGoal = 1.0F;
      if (!mc.player.onGround() && this.watchdogMode.getValue()) {
         destroyProgress *= 5.0F;
         progressGoal = 5.0F;
      }

      if (!(destroyProgress < 0.0F)) {
         if (mc.player.tickCount % 4 == 0) {
            SoundType soundType = bedState.getSoundType();
            mc.getSoundManager()
               .play(
                  new SimpleSoundInstance(
                     soundType.getHitSound(),
                     SoundSource.BLOCKS,
                     (soundType.getVolume() + 1.0F) * 0.125F,
                     soundType.getPitch() * 0.5F,
                     SoundInstance.createUnseededRandom(),
                     bedPosition
                  )
               );
         }

         BlockPos miningPosition = bedPosition;
         Direction hitFace = this.hitFaceFromRotation(yaw, pitch);
         if (this.diggingProgress == 0.0F) {
            ((MultiPlayerGameModeAccessor)mc.gameMode)
               .invokeStartPrediction(mc.level, sequence -> new ServerboundPlayerActionPacket(Action.START_DESTROY_BLOCK, miningPosition, hitFace, sequence));
            // Publish on the START tick, rather than when a bed is merely found in range.
            this.diggingPosition = bedPosition.immutable();
            this.diggingLevel = mc.level;
            this.diggingState = bedState;
         }
         this.diggingGoal = progressGoal;

         if (this.diggingProgress >= progressGoal + destroyProgress) {
            ((MultiPlayerGameModeAccessor)mc.gameMode)
               .invokeStartPrediction(mc.level, sequence -> new ServerboundPlayerActionPacket(Action.STOP_DESTROY_BLOCK, miningPosition, hitFace, sequence));
            mc.player.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, false);
            this.diggingProgress = 0.0F;
            mc.gameMode.destroyBlock(bedPosition);
            mc.level.destroyBlockProgress(mc.player.getId(), bedPosition, -1);
            this.resetDigging();
            this.breakDelayTicks = (int)this.breakDelay.getValue();
         } else {
            mc.level.destroyBlockProgress(mc.player.getId(), bedPosition, Math.min(9, (int)(this.diggingProgress * 10.0F / progressGoal)));
            this.previousDiggingProgress = this.diggingProgress;
            this.diggingProgress += destroyProgress;
            mc.player.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, false);
            this.diggingPosition = bedPosition.immutable();
            this.diggingState = bedState;
         }
      }
   }

   @Override
   public void onEvent(Event event) {
      if (event == Events.MOUSE_BUTTON && mc.gui.screen() == null && this.targetBedPosition != null) {
         event.setCancelled(true);
      }

      if (event == Events.ROTATION) {
         this.rotatingToBed = false;
         if (mc.player == null || mc.level == null || this.diggingLevel != null && this.diggingLevel != mc.level) {
            this.resetDigging();
            return;
         }
         if (mc.gui.screen() != null) {
            this.resetDigging();
            return;
         }

         if ((FeatureManager.killAura.getTarget() != null || FeatureManager.killAura.isServerBlocking()) && !this.allowKillAura.getValue() || FeatureManager.scaffold.isEnabled()) {
            this.resetDigging();
            return;
         }

         if (this.breakDelayTicks < 0) {
            this.resetDigging();
            return;
         }

         BlockPos blockPosition = this.findBed();
         if (blockPosition != null && this.targetBedPosition != null) {
            BlockState currentBedState = mc.level.getBlockState(blockPosition);
            BlockState previousBedState = mc.level.getBlockState(this.targetBedPosition);
            if (currentBedState != previousBedState) {
               this.resetDigging();
               return;
            }
         }

         if (blockPosition != null) {
            if (mc.player.distanceToSqr((double)blockPosition.getX() + 0.5, (double)blockPosition.getY() + 0.5, (double)blockPosition.getZ() + 0.5) > this.range.getValue() * this.range.getValue()) {
               this.resetDigging();
               return;
            }

            BlockState bedState = mc.level.getBlockState(blockPosition);
            this.mineBed(blockPosition, bedState);
         } else {
            this.resetDigging();
         }

         this.targetBedPosition = blockPosition;
      }
   }

   public BedAura() {
      super(BED_AURA_LABEL, Category.PLAYER);
      this.speed = new NumberSetting(SPEED_LABEL, this, 1.0, 1.0, 2.0, 0.05);
      this.breakDelay = new NumberSetting(BREAK_DELAY_LABEL, this, 2.0, 1.0, 4.0, 1.0);
      this.surrounding = new BooleanSetting(SURROUNDING_LABEL, this, false);
      this.allowKillAura = new BooleanSetting(ALLOW_KILL_AURA_LABEL, this, false);
      this.onlySSRotate = new BooleanSetting(ONLY_SSROTATE_LABEL, this, false);
      this.watchdogMode = new BooleanSetting(WATCHDOG_MODE_LABEL, this, false);
      this.previousSlot = -1;
   }

   private Direction hitFaceFromRotation(float yaw, float pitch) {
      Vec3 direction = Vec3.directionFromRotation(pitch, yaw);
      double absoluteX = Math.abs(direction.x);
      double absoluteY = Math.abs(direction.y);
      double absoluteZ = Math.abs(direction.z);
      if (absoluteY > absoluteX && absoluteY > absoluteZ) {
         return direction.y > 0.0 ? Direction.DOWN : Direction.UP;
      } else if (absoluteX > absoluteZ) {
         return direction.x > 0.0 ? Direction.WEST : Direction.EAST;
      } else {
         return direction.z > 0.0 ? Direction.NORTH : Direction.SOUTH;
      }
   }

   private void resetDigging() {
      boolean sameWorld = mc.player != null && mc.level != null && this.diggingLevel == mc.level;
      if (sameWorld && mc.gameMode != null && this.diggingProgress > 0.0F && this.diggingPosition != null) {
         Direction hitFace = this.hitFaceFromRotation(Events.ROTATION.getYaw(), Events.ROTATION.getPitch());
         ((MultiPlayerGameModeAccessor)mc.gameMode)
            .invokeStartPrediction(mc.level, sequence -> new ServerboundPlayerActionPacket(Action.ABORT_DESTROY_BLOCK, this.diggingPosition, hitFace, sequence));
         mc.level.destroyBlockProgress(mc.player.getId(), this.diggingPosition, -1);
      }

      this.diggingPosition = null;
      this.diggingState = null;
      this.diggingLevel = null;
      this.previousDiggingProgress = 0.0F;
      this.diggingProgress = 0.0F;
      if (this.previousSlot != -1) {
         if (sameWorld) {
            mc.player.getInventory().setSelectedSlot(this.previousSlot);
         }
         this.previousSlot = -1;
      }

      if (this.breakDelayTicks < 0) {
         this.breakDelayTicks++;
      }

      this.searchTicks = 0;
      this.targetBedPosition = null;
   }

   @Override
   public void onDisable() {
      this.rotatingToBed = false;
      this.resetDigging();
   }

   private BlockPos findBed() {
      if (mc.player.isUsingItem()) {
         return null;
      } else {
         Vec3 ownBedPosition = FeatureManager.whitelist.bedSpawnPosition;
         if (ownBedPosition != null && FeatureManager.whitelist.bedSpawnKnown && mc.player.distanceToSqr(ownBedPosition) < 600.0) {
            return null;
         } else {
            BlockPos playerPosition = mc.player.blockPosition();
            int playerX = playerPosition.getX();
            int playerY = playerPosition.getY();
            int playerZ = playerPosition.getZ();
            int searchRange = (int)this.range.getValue();
            MutableBlockPos searchPosition = new MutableBlockPos();

            for (int offsetX = -searchRange; offsetX <= searchRange; offsetX++) {
               int searchX = playerX + offsetX;

               for (int offsetY = -searchRange; offsetY <= searchRange; offsetY++) {
                  int searchY = playerY + offsetY;

                  for (int offsetZ = -searchRange; offsetZ <= searchRange; offsetZ++) {
                     searchPosition.set(searchX, searchY, playerZ + offsetZ);
                     BlockState blockState = mc.level.getBlockState(searchPosition);
                     if (blockState.getBlock() instanceof BedBlock) {
                        return new BlockPos(searchPosition.getX(), searchPosition.getY(), searchPosition.getZ());
                     }
                  }
               }
            }

            return null;
         }
      }
   }

   public static final class BedAuraTargetRenderer {
      public static final int OUTLINE = 0xFFFF0000;
      public static final int FILL = 0x5AFF0000;
      private BedAuraTargetRenderer() { }

      public static void extract() {
         Minecraft mc = Minecraft.getInstance();
         var bedAura = FeatureManager.bedAura;
         if (bedAura == null || mc.player == null || mc.level == null) return;
         var target = bedAura.diggingTarget(mc.getDeltaTracker().getGameTimeDeltaPartialTick(false));
         if (target == null) return;
         var shape = target.state().getShape(mc.level, target.position());
         if (shape.isEmpty()) return;
         var box = shape.bounds().move(target.position()).inflate(.002);
         // The native collector owns depth, blending and camera transforms; no global render state changes.
         try (var collection = mc.levelExtractor.collectPerFrameMainThreadGizmos()) {
            Gizmos.cuboid(box, GizmoStyle.strokeAndFill(OUTLINE, 1.5f, FILL))
               .setAlwaysOnTop();
         }
      }
   }
}
