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
   private static final String f453 = "Range";
   private int f469;
   private float f467;
   private final BooleanSetting f465;
   private final BooleanSetting f463;
   private static final String f455 = "Break Delay";
   private float f468;
   private final NumberSetting f462;
   private final NumberSetting f461;
   private int f472;
   private static final String f452 = "BedAura";
   private static final String f454 = "Speed";
   private BlockPos f470;
   public int f23;
   private BlockPos f471;
   public boolean f22;
   private static final String f459 = "Watchdog Mode";
   private static final String f458 = "Only S/S Rotate";
   private static final String f456 = "Surrounding";
   private static final String f457 = "Allow KillAura";
   private final BooleanSetting f464;
   private final NumberSetting f460 = new NumberSetting(f453, this, 5.0, 1.0, 8.0, 0.5);
   private final BooleanSetting f466;
   private ClientLevel diggingLevel;
   private BlockState diggingState;
   private float diggingGoal = 1;

   /** The actual block being mined, which can be a defense block above the bed. */
   public record DiggingTarget(BlockPos position, BlockState state, float progress) { }

   public DiggingTarget diggingTarget(float partialTick) {
      if (!isEnabled() || mc.player == null || mc.level == null || mc.level != this.diggingLevel
          || this.f470 == null || this.diggingState == null || mc.level.getBlockState(this.f470).isAir()) {
         return null;
      }
      float progress = this.f468 + (this.f467 - this.f468) * Math.clamp(partialTick, 0, 1);
      return new DiggingTarget(this.f470, this.diggingState, Math.clamp(progress / this.diggingGoal, 0, 1));
   }

   private void pm$84(BlockPos var1, BlockState var2) {
      if (this.f463.m215()) {
         BlockPos var3 = var1.above();
         BlockPos var4 = var1.relative((Direction)var2.getValue(BedBlock.FACING), var2.getValue(BedBlock.PART) == BedPart.HEAD ? -1 : 1);
         boolean var5 = false;

         for (Direction var7 : Plane.HORIZONTAL) {
            BlockPos var8 = var1.relative(var7);
            BlockPos var9 = var4.relative(var7);
            BlockState var10 = mc.level.getBlockState(var8);
            BlockState var11 = mc.level.getBlockState(var9);
            if (var10.getBlock() == Blocks.AIR) {
               var5 = true;
               break;
            }

            if (var11.getBlock() == Blocks.AIR) {
               var5 = true;
               break;
            }
         }

         if (!var5) {
            BlockState var25 = mc.level.getBlockState(var3);
            if (!var25.isAir()) {
               var1 = var3;
               var2 = var25;
               int var26 = -1;
               float var28 = 1.0F;

               for (int var29 = 0; var29 < 9; var29++) {
                  ItemStack var31 = mc.player.getInventory().getItem(var29);
                  float var32 = var31.getDestroySpeed(var2);
                  if (var32 > var28) {
                     var28 = var32;
                     var26 = var29;
                  }
               }

               if (var26 != -1 && mc.player.getInventory().getSelectedSlot() != var26) {
                  this.f469 = mc.player.getInventory().getSelectedSlot();
                  mc.player.getInventory().setSelectedSlot(var26);
               }
            }
         }
      }

      double var23 = (double)var1.getX() + 0.5;
      double var24 = (double)var1.getY() + 0.5;
      double var27 = (double)var1.getZ() + 0.5;
      double var30 = var23 - mc.player.getX();
      double var33 = var24 - mc.player.getEyeY();
      double var13 = var27 - mc.player.getZ();
      double var15 = Math.sqrt(var30 * var30 + var13 * var13);
      float var17 = (float)Math.toDegrees(Math.atan2(var13, var30)) - 90.0F;
      float var18 = (float)(-Math.toDegrees(Math.atan2(var33, var15)));
      if (!this.f465.m215() || this.f467 == 0.0F || this.f467 >= 1.0F) {
         Events.f3.m77(var17);
         Events.f3.m83(var18);
         this.f22 = true;
      }

      float var19 = (float)((double)var2.getDestroyProgress(mc.player, mc.player.level(), var1) * this.f461.m220());
      if (mc.player.isEyeInFluid(FluidTags.WATER) && this.f466.m215()) {
         var19 *= 5.0F;
      }

      float var20 = 1.0F;
      if (!mc.player.onGround() && this.f466.m215()) {
         var19 *= 5.0F;
         var20 = 5.0F;
      }

      if (!(var19 < 0.0F)) {
         if (mc.player.tickCount % 4 == 0) {
            SoundType var21 = var2.getSoundType();
            mc.getSoundManager()
               .play(
                  new SimpleSoundInstance(
                     var21.getHitSound(),
                     SoundSource.BLOCKS,
                     (var21.getVolume() + 1.0F) * 0.125F,
                     var21.getPitch() * 0.5F,
                     SoundInstance.createUnseededRandom(),
                     var1
                  )
               );
         }

         BlockPos var34 = var1;
         Direction var22 = this.pm$86(var17, var18);
         if (this.f467 == 0.0F) {
            ((MultiPlayerGameModeAccessor)mc.gameMode)
               .invokeStartPrediction(mc.level, var2x -> new ServerboundPlayerActionPacket(Action.START_DESTROY_BLOCK, var34, var22, var2x));
            // Publish on the START tick, rather than when a bed is merely found in range.
            this.f470 = var1.immutable();
            this.diggingLevel = mc.level;
            this.diggingState = var2;
         }
         this.diggingGoal = var20;

         if (this.f467 >= var20 + var19) {
            ((MultiPlayerGameModeAccessor)mc.gameMode)
               .invokeStartPrediction(mc.level, var2x -> new ServerboundPlayerActionPacket(Action.STOP_DESTROY_BLOCK, var34, var22, var2x));
            mc.player.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, false);
            this.f467 = 0.0F;
            mc.gameMode.destroyBlock(var1);
            mc.level.destroyBlockProgress(mc.player.getId(), var1, -1);
            this.pm$85();
            this.f472 = (int)this.f462.m220();
         } else {
            mc.level.destroyBlockProgress(mc.player.getId(), var1, Math.min(9, (int)(this.f467 * 10.0F / var20)));
            this.f468 = this.f467;
            this.f467 += var19;
            mc.player.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, false);
            this.f470 = var1.immutable();
            this.diggingState = var2;
         }
      }
   }

   @Override
   public void onEvent(Event var1) {
      if (var1 == Events.f15 && mc.gui.screen() == null && this.f471 != null) {
         var1.setCancelled(true);
      }

      if (var1 == Events.f3) {
         this.f22 = false;
         if (mc.player == null || mc.level == null || this.diggingLevel != null && this.diggingLevel != mc.level) {
            this.pm$85();
            return;
         }
         if (mc.gui.screen() != null) {
            this.pm$85();
            return;
         }

         if ((FeatureManager.f26.f17 != null || FeatureManager.f26.f16) && !this.f464.m215() || FeatureManager.f29.isEnabled()) {
            this.pm$85();
            return;
         }

         if (this.f472 < 0) {
            this.pm$85();
            return;
         }

         BlockPos var3 = this.pm$83();
         if (var3 != null && this.f471 != null) {
            BlockState var4 = mc.level.getBlockState(var3);
            BlockState var5 = mc.level.getBlockState(this.f471);
            if (var4 != var5) {
               this.pm$85();
               return;
            }
         }

         if (var3 != null) {
            if (mc.player.distanceToSqr((double)var3.getX() + 0.5, (double)var3.getY() + 0.5, (double)var3.getZ() + 0.5) > this.f460.m220() * this.f460.m220()) {
               this.pm$85();
               return;
            }

            BlockState var16 = mc.level.getBlockState(var3);
            this.pm$84(var3, var16);
         } else {
            this.pm$85();
         }

         this.f471 = var3;
      }
   }

   public BedAura() {
      super(f452, Category.PLAYER);
      this.f461 = new NumberSetting(f454, this, 1.0, 1.0, 2.0, 0.05);
      this.f462 = new NumberSetting(f455, this, 2.0, 1.0, 4.0, 1.0);
      this.f463 = new BooleanSetting(f456, this, false);
      this.f464 = new BooleanSetting(f457, this, false);
      this.f465 = new BooleanSetting(f458, this, false);
      this.f466 = new BooleanSetting(f459, this, false);
      this.f469 = -1;
   }

   private Direction pm$86(float var1, float var2) {
      Vec3 var3 = Vec3.directionFromRotation(var2, var1);
      double var4 = Math.abs(var3.x);
      double var6 = Math.abs(var3.y);
      double var8 = Math.abs(var3.z);
      if (var6 > var4 && var6 > var8) {
         return var3.y > 0.0 ? Direction.DOWN : Direction.UP;
      } else if (var4 > var8) {
         return var3.x > 0.0 ? Direction.WEST : Direction.EAST;
      } else {
         return var3.z > 0.0 ? Direction.NORTH : Direction.SOUTH;
      }
   }

   private void pm$85() {
      boolean sameWorld = mc.player != null && mc.level != null && this.diggingLevel == mc.level;
      if (sameWorld && mc.gameMode != null && this.f467 > 0.0F && this.f470 != null) {
         Direction var1 = this.pm$86(Events.f3.m76(), Events.f3.m82());
         ((MultiPlayerGameModeAccessor)mc.gameMode)
            .invokeStartPrediction(mc.level, var2 -> new ServerboundPlayerActionPacket(Action.ABORT_DESTROY_BLOCK, this.f470, var1, var2));
         mc.level.destroyBlockProgress(mc.player.getId(), this.f470, -1);
      }

      this.f470 = null;
      this.diggingState = null;
      this.diggingLevel = null;
      this.f468 = 0.0F;
      this.f467 = 0.0F;
      if (this.f469 != -1) {
         if (sameWorld) {
            mc.player.getInventory().setSelectedSlot(this.f469);
         }
         this.f469 = -1;
      }

      if (this.f472 < 0) {
         this.f472++;
      }

      this.f23 = 0;
      this.f471 = null;
   }

   @Override
   public void onDisable() {
      this.f22 = false;
      this.pm$85();
   }

   private BlockPos pm$83() {
      if (mc.player.isUsingItem()) {
         return null;
      } else {
         Vec3 var1 = FeatureManager.f30.f20;
         if (var1 != null && FeatureManager.f30.f19 && mc.player.distanceToSqr(var1) < 600.0) {
            return null;
         } else {
            BlockPos var2 = mc.player.blockPosition();
            int var3 = var2.getX();
            int var4 = var2.getY();
            int var5 = var2.getZ();
            int var6 = (int)this.f460.m220();
            MutableBlockPos var7 = new MutableBlockPos();

            for (int var8 = -var6; var8 <= var6; var8++) {
               int var9 = var3 + var8;

               for (int var10 = -var6; var10 <= var6; var10++) {
                  int var11 = var4 + var10;

                  for (int var12 = -var6; var12 <= var6; var12++) {
                     var7.set(var9, var11, var5 + var12);
                     BlockState var13 = mc.level.getBlockState(var7);
                     if (var13.getBlock() instanceof BedBlock) {
                        return new BlockPos(var7.getX(), var7.getY(), var7.getZ());
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
         var bedAura = FeatureManager.f33;
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
