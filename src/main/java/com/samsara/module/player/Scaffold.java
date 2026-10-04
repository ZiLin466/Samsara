package com.samsara.module.player;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.BooleanSetting;
import com.samsara.setting.ModeSetting;
import com.samsara.setting.NumberSetting;
import com.samsara.ui.dynamicIsland.DynamicIslandManager;
import com.samsara.util.Wrapper;
import java.nio.charset.StandardCharsets;
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
   private final NumberSetting f560;
   private static final String f554 = "Keep Y";
   private static final String f546 = "Rotation Speed";
   private static final String f539 = "Scaffold";
   private float f570;
   private int f580;
   private final ModeSetting f559;
   private final float[] f581;
   private int f578;
   private boolean f575;
   private final BooleanSetting f564;
   private boolean f574;
   private float f569;
   private static final String f553 = "Telly RMB";
   private final PlacementTarget f568;
   private int f577;
   private static final String f542 = "Backward";
   private static final String f545 = "Raycast2";
   private float f572;
   private final BooleanSetting f562;
   private final NumberSetting f567;
   private int f579;
   private final BooleanSetting f563;
   private static final String f549 = "Telly";
   private static final String f540 = "Rotations";
   private static final String f555 = "Watchdog Tower";
   private static final String f557 = "Sneak";
   private static final String f541 = "HitVec";
   private float f571;
   private static final String f543 = "Offset";
   private static final String f558 = "Sneak Delay";
   private boolean f583;
   private static final String f548 = "Disabled";
   private static final String f552 = "Watchdog3";
   private static final String f547 = "Fast Mode";
   private static final String f544 = "Raycast";
   private boolean f576;
   private final ModeSetting f561;
   private final BooleanSetting f566;
   private static final String f551 = "Watchdog2";
   private static final String f550 = "Watchdog";
   private final float[] f582;
   private boolean f573;

   private float[] pm$102(BlockHitResult var1) {
      Vec3 var2 = mc.player.getEyePosition();
      Vec3 var3 = var1.getLocation();
      double var4 = var3.x - var2.x;
      double var6 = var3.y - var2.y;
      double var8 = var3.z - var2.z;
      double var10 = Math.sqrt(var4 * var4 + var8 * var8);
      float var12 = (float)(-Math.toDegrees(Math.atan2(var6, var10)));
      String var13 = this.f559.m224();
      byte var14 = -1;
      int var10000 = var13.hashCode();
      if (var10000 == -2133157087) {
         if (var13.equals(f541)) {
            var14 = 4;
         }
      } else if (var10000 == -2108346365) {
         if (var13.equals(f542)) {
            var14 = 0;
         }
      } else if (var10000 == -1935912781) {
         if (var13.equals(f543)) {
            var14 = 1;
         }
      } else if (var10000 == -1642289591) {
         if (var13.equals(f544)) {
            var14 = 2;
         }
      } else if (var10000 == 628630281) {
         if (var13.equals(f545)) {
            var14 = 3;
         }
      }

      switch (var14) {
         case 0:
            return new float[]{MoveDirectionUtil.m45() - 180.0F, var12};
         case 1:
            return new float[]{this.pm$106(), 83.0F};
         case 2:
            return this.pm$103(var1, MoveDirectionUtil.m45() - 180.0F);
         case 3:
            return this.pm$103(var1, this.f571);
         case 4:
         default:
            float var17 = (float)Math.toDegrees(Math.atan2(var8, var4)) - 90.0F;
            return new float[]{var17, var12};
      }
   }

   private int pm$100() {
      int var1 = -1;
      int var2 = 0;

      for (int var3 = 0; var3 < 9; var3++) {
         ItemStack var4 = mc.player.getInventory().getItem(var3);
         if (var4.getItem() instanceof BlockItem && var4.getCount() > var2) {
            var2 = var4.getCount();
            var1 = var3;
         }
      }

      return var1;
   }

   private void pm$101(float var1, float var2) {
      if (!this.f574) {
         this.f574 = true;
         float var3 = (float)(this.f560.m220() * 20.0 - Math.random());
         if (this.f575 && this.f561.m228(f550)) {
            var3 = (float)(89.0 - Math.random());
         }

         if (this.f583) {
            var3 = (float)(129.0 - Math.random());
            this.f583 = false;
         } else if (this.f561.m228(f551)) {
            var3 = 34.0F;
         }

         if (this.f575 && this.f561.m228(f551)) {
            this.f583 = true;
         }

         float var4 = Mth.wrapDegrees(var1 - this.f569);
         float var5 = Mth.wrapDegrees(var2 - this.f570);
         var4 = Math.clamp(var4, -var3, var3);
         var5 = Math.clamp(var5, -var3, var3);
         this.f569 += var4;
         float var6 = mc.player.getYRot();
         this.f569 = var6 + Mth.wrapDegrees(this.f569 - var6);
         this.f570 += var5;
         if (!((double)Math.abs(Mth.wrapDegrees(this.f569 - var1)) > 0.1) && !((double)Math.abs(Mth.wrapDegrees(this.f570 - var2)) > 0.1)) {
            this.f573 = true;
         } else {
            this.f573 = false;
         }
      }
   }

   private float[] pm$103(BlockHitResult var1, float var2) {
      if (this.f561.m228(f552)) {
         var2 = MoveDirectionUtil.m45() + 90.0F;
      }

      float var3 = 0.0F;

      for (float var7 : this.f581) {
         for (float var11 : this.f582) {
            float var12 = var2 + var7;
            float var13 = Mth.clamp(var3 + var11, -90.0F, 90.0F);
            BlockHitResult var14 = this.pm$104(var12, var13, 4.5F);
            if (var14.getBlockPos().equals(var1.getBlockPos()) && var14.getDirection() == var1.getDirection()) {
               return new float[]{var12, var13};
            }
         }
      }

      this.f573 = false;
      this.f574 = true;
      return new float[]{this.f571, this.f572};
   }

   private BlockHitResult pm$104(float var1, float var2, float var3) {
      Vec3 var4 = mc.getCameraEntity().getEyePosition(1.0F);
      Vec3 var5 = this.pm$105(var1, var2);
      Vec3 var6 = var4.add(var5.x * (double)var3, var5.y * (double)var3, var5.z * (double)var3);
      return mc.level.clip(new ClipContext(var4, var6, Block.COLLIDER, Fluid.NONE, mc.getCameraEntity()));
   }

   public Scaffold() {
      super(f539, Category.PLAYER);
      this.f559 = new ModeSetting(f540, this, f541, new String[]{f541, f542, f543, f544, f545});
      this.f560 = new NumberSetting(f546, this, 2.0, 0.0, 10.0, 0.5);
      this.f561 = new ModeSetting(f547, this, f548, new String[]{f548, f549, f550, f551, f552});
      this.f562 = new BooleanSetting(f553, this, false);
      this.f563 = new BooleanSetting(f554, this, false);
      this.f564 = new BooleanSetting(f555, this, false);
      this.f566 = new BooleanSetting(f557, this, false);
      this.f567 = new NumberSetting(f558, this, 0.0, 0.0, 25.0, 1.0);
      this.f568 = new PlacementTarget();
      this.f578 = -1;
      this.f581 = new float[]{
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
      this.f582 = new float[]{
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

   private Vec3 pm$105(float var1, float var2) {
      float var3 = Mth.cos((double)(-var1 * (float) (Math.PI / 180.0) - (float) Math.PI));
      float var4 = Mth.sin((double)(-var1 * (float) (Math.PI / 180.0) - (float) Math.PI));
      float var5 = -Mth.cos((double)(-var2 * (float) (Math.PI / 180.0)));
      float var6 = Mth.sin((double)(-var2 * (float) (Math.PI / 180.0)));
      return new Vec3((double)(var4 * var5), (double)var6, (double)(var3 * var5));
   }

   private float pm$106() {
      float var1 = MoveDirectionUtil.m45();
      float var2;
      if (var1 % 90.0F <= 45.0F) {
         var2 = var1 + 45.0F;
      } else {
         var2 = var1 - 45.0F;
      }

      return var2 - 180.0F;
   }

   private PlacementTarget getPlaceData(BlockPos var1) {
      double var2 = Double.MAX_VALUE;
      PlacementTarget var4 = null;

      for (int var5 = -3; var5 <= 3; var5++) {
         for (int var6 = -2; var6 <= 0; var6++) {
            for (int var7 = -3; var7 <= 3; var7++) {
               BlockPos var8 = var1.offset(var5, var6, var7);
               if (!mc.level.getBlockState(var1).canBeReplaced()) {
                  return null;
               }

               if (!mc.level.isEmptyBlock(var8)) {
                  Direction var9 = this.pm$108(var8, var1);
                  if (var9 != null) {
                     double var10 = mc.player.distanceToSqr((double)var8.getX() + 0.5, (double)var8.getY() + 0.5, (double)var8.getZ() + 0.5);
                     if (var10 < var2 && var9 != Direction.DOWN) {
                        var2 = var10;
                        this.f568.m185(var8, var9);
                        var4 = this.f568;
                     }
                  }
               }
            }
         }
      }

      return var4;
   }

   @Override
   public void onEvent(Event var1) {
      if (var1 == Events.f2) {
         DynamicIslandManager.sampleScaffoldMovement();
      }

      if (var1 == Events.f15) {
         if (mc.gui.screen() == null) {
            var1.setCancelled(true);
         }

         if (Events.f15.m13() == 1) {
            this.f576 = Events.f15.m16();
         }
      }

      if (var1 == Events.f3) {
         boolean var2 = true;
         if (this.f563.m215() && mc.options.keyJump.isDown()) {
            this.f577 = (int)mc.player.getY() - 1;
         }

         if (this.f579 > 0) {
            this.f579--;
         }

         if ((this.f561.m228(f550) || this.f561.m228(f551) || this.f561.m228(f549)) && mc.player.onGround() && (!this.f562.m215() || this.f576)) {
            float var14 = MoveDirectionUtil.m45();
            Events.f3.m77(var14);
            this.f569 = var14;
            this.f575 = mc.player.onGround();
            return;
         }

         this.f574 = false;
         BlockPos var3 = mc.player.blockPosition().below();
         if (this.f563.m215()) {
            var3 = var3.atY(this.f577);
         }

         if (mc.level.isEmptyBlock(var3) && var2) {
            int var4 = this.pm$100();
            if (var4 != -1) {
               PlacementTarget var5 = this.getPlaceData(var3);
               if (var5 != null) {
                  Vec3 var6 = Vec3.atCenterOf(var5.f24);
                  BlockHitResult var7 = new BlockHitResult(var6, var5.f25, var5.f24, false);
                  float[] var8 = this.pm$102(var7);
                  this.pm$101(var8[0], var8[1]);
                  this.f571 = var8[0];
                  this.f572 = var8[1];
                  if (this.f564.m215() && mc.options.keyJump.isDown() && mc.player.onGround()) {
                     this.f573 = false;
                  }

                  if (this.f573) {
                     mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, var7);
                     mc.player.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, false);
                     this.f580++;
                     this.f579 = 10;
                  }
               }
            }
         }

         if (this.f561.m228(f552)) {
            float var15 = MoveDirectionUtil.m45();
            float var17 = var15 + 45.0F;
            float var19 = Math.abs(Mth.wrapDegrees(var17 - this.f569));
            if (this.f579 == (var19 > 60.0F ? 8 : 9) && !mc.options.keyJump.isDown() && this.f575) {
               this.f569 = var15 + 45.0F;
            }

            if (mc.player.onGround()) {
               this.f577 = (int)(mc.player.getY() - 1.0);
            }
         } else {
            this.pm$101(this.f571, this.f572);
         }

         if (this.f564.m215() && mc.player.onGround() && mc.options.keyJump.isDown()) {
            this.f569 = MoveDirectionUtil.m45();
         }

         Events.f3.m77(this.f569);
         Events.f3.m83(this.f570);
         if (!this.f561.m228(f552)) {
            this.f575 = mc.player.onGround();
         }
      }

      if (var1 == Events.f1 && (this.f561.m228(f552) || this.f564.m215() && mc.options.keyJump.isDown() && !mc.player.onGround())) {
         float var9 = MoveDirectionUtil.m45() - 180.0F;
         Minecraft.getInstance().player.yHeadRot = var9;
         Minecraft.getInstance().player.yBodyRot = var9;
      }

      if (var1 == Events.f4) {
         int var10 = this.pm$100();
         if (var10 != -1 && mc.player.getInventory().getSelectedSlot() != var10) {
            mc.player.getInventory().setSelectedSlot(var10);
         }
      }

      if (var1 == Events.f8) {
         if (this.f561.m228(f552) && !this.f575) {
            mc.player.input.makeJump();
            this.f575 = true;
         }

         if ((this.f561.m228(f549) || this.f561.m228(f550) || this.f561.m228(f551))
            && (mc.player.input.getMoveVector().x != 0.0F || mc.player.input.getMoveVector().y != 0.0F)
            && (!this.f562.m215() || this.f576)) {
            mc.player.input.makeJump();
         }

         if (this.f566.m215()) {
            Input var11 = mc.player.input.keyPresses;
            if (mc.player.tickCount % (int)(this.f567.m220() + 1.0) == 0) {
               mc.player.input.keyPresses = new Input(var11.forward(), var11.backward(), var11.left(), var11.right(), var11.jump(), true, var11.sprint());
            }
         }
      }
   }

   @Override
   public void onEnable() {
      this.f577 = (int)(mc.player.getY() - 1.0);
      this.f578 = mc.player.getInventory().getSelectedSlot();
      this.f569 = mc.player.getYRot();
      this.f570 = mc.player.getXRot();
      this.f571 = MoveDirectionUtil.m45() - 180.0F;
      this.f572 = 83.0F;
      this.f570 = 83.0F;
      this.f575 = false;
      this.f574 = this.f573 = this.f583 = this.f576 = false;
      this.f579 = this.f580 = 0;
   }

   @Override
   public void onDisable() {
      if (mc.player != null) {
         if (this.f578 >= 0 && this.f578 < 9) mc.player.getInventory().setSelectedSlot(this.f578);
         mc.player.yHeadRot = mc.player.yBodyRot = mc.player.getYRot();
         mc.player.yHeadRotO = mc.player.yBodyRotO = mc.player.getYRot();
         mc.player.yRotO = mc.player.getYRot();
         mc.player.xRotO = mc.player.getXRot();
      }
      this.f578 = -1;
      this.f574 = this.f573 = this.f583 = this.f576 = false;
      this.f579 = this.f580 = 0;
   }

   private Direction pm$108(BlockPos var1, BlockPos var2) {
      int var3 = var2.getX() - var1.getX();
      int var4 = var2.getY() - var1.getY();
      int var5 = var2.getZ() - var1.getZ();
      if (Math.abs(var3) >= Math.abs(var4) && Math.abs(var3) >= Math.abs(var5)) {
         return var3 > 0 ? Direction.EAST : Direction.WEST;
      } else if (Math.abs(var4) >= Math.abs(var3) && Math.abs(var4) >= Math.abs(var5)) {
         return var4 > 0 ? Direction.UP : Direction.DOWN;
      } else {
         return var5 > 0 ? Direction.SOUTH : Direction.NORTH;
      }
   }

   public static class PlacementTarget {
      public Direction f25;
      public BlockPos f24;

      public void m185(BlockPos var1, Direction var2) {
         this.f24 = var1;
         this.f25 = var2;
      }

      public PlacementTarget() {
      }
   }

   public static class MoveDirectionUtil implements Wrapper {
      public static float m45() {
         float var0 = Wrapper.mc.player.getYRot();
         boolean var1 = Wrapper.mc.options.keyUp.isDown();
         boolean var2 = Wrapper.mc.options.keyDown.isDown();
         boolean var3 = Wrapper.mc.options.keyLeft.isDown();
         boolean var4 = Wrapper.mc.options.keyRight.isDown();
         if (var1 && !var2) {
            if (var3 && !var4) {
               var0 -= 45.0F;
            } else if (var4 && !var3) {
               var0 += 45.0F;
            }
         } else if (var2 && !var1) {
            var0 += 180.0F;
            if (var3 && !var4) {
               var0 += 45.0F;
            } else if (var4 && !var3) {
               var0 -= 45.0F;
            }
         } else if (var3 && !var4) {
            var0 -= 90.0F;
         } else if (var4 && !var3) {
            var0 += 90.0F;
         }

         var0 %= 360.0F;
         if (var0 < 0.0F) {
            var0 += 360.0F;
         }

         return var0;
      }

      public static boolean m46() {
         float var0 = m45() % 90.0F;
         return var0 > 20.0F && var0 < 70.0F;
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
