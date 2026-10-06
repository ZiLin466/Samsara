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
      this.mode = new ModeSetting("Mode", this, "Blatant", new String[]{"Blatant", "Closet"}) {
         @Override public void m226(String value) {
            super.m226(value);
            if (Scaffold.this.isEnabled()) Events.m11();
         }
      };
      this.f559 = new ModeSetting(f540, this, f541, new String[]{f541, f542, f543, f544, f545});
      this.f560 = new NumberSetting(f546, this, 2.0, 0.0, 10.0, 0.5);
      this.f561 = new ModeSetting(f547, this, f548, new String[]{f548, f549, f550, f551, f552});
      this.f562 = new BooleanSetting(f553, this, false);
      this.f563 = new BooleanSetting(f554, this, false);
      this.f564 = new BooleanSetting(f555, this, false);
      this.f566 = new BooleanSetting(f557, this, false);
      this.f567 = new NumberSetting(f558, this, 0.0, 0.0, 25.0, 1.0);
      for (var setting : this.settings) {
         if (setting != this.mode) setting.setVisible(() -> this.mode.m228("Blatant"));
      }
      this.closetYawSpeed = new NumberSetting("Closet Yaw Speed", this, 22, 5, 35, 1);
      this.closetPitchSpeed = new NumberSetting("Closet Pitch Speed", this, 14, 5, 25, 1);
      this.closetAcceleration = new NumberSetting("Closet Acceleration", this, 5, 1, 8, 0.5);
      this.closetEdgeMargin = new NumberSetting("Closet Edge Margin", this, 0.04, 0.01, 0.15, 0.01);
      this.closetLookAhead = new NumberSetting("Closet Look Ahead", this, 0.65, 0.3, 1, 0.05);
      this.closetYawSpeed.setVisible(() -> this.mode.m228("Closet"));
      this.closetPitchSpeed.setVisible(() -> this.mode.m228("Closet"));
      this.closetAcceleration.setVisible(() -> this.mode.m228("Closet"));
      this.closetEdgeMargin.setVisible(() -> this.mode.m228("Closet"));
      this.closetLookAhead.setVisible(() -> this.mode.m228("Closet"));
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
      if (!ensureSession()) return;
      if (this.mode.m228("Closet")) {
         this.closet.onEvent(var1);
         return;
      }
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
      this.sessionPlayer = null;
      this.sessionWorld = null;
      this.activeMode = null;
      this.f578 = -1;
      this.closet.reset();
      ensureSession();
   }

   private void initializeBlatant() {
      this.f577 = (int)(mc.player.getY() - 1.0);
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
      if (mc.player != null && mc.player == this.sessionPlayer) {
         if (this.f578 >= 0 && this.f578 < 9) mc.player.getInventory().setSelectedSlot(this.f578);
         mc.player.yHeadRot = mc.player.yBodyRot = mc.player.getYRot();
         mc.player.yHeadRotO = mc.player.yBodyRotO = mc.player.getYRot();
         mc.player.yRotO = mc.player.getYRot();
         mc.player.xRotO = mc.player.getXRot();
      }
      this.f578 = -1;
      this.f574 = this.f573 = this.f583 = this.f576 = false;
      this.f579 = this.f580 = 0;
      this.closet.reset();
      this.sessionPlayer = null;
      this.sessionWorld = null;
      this.activeMode = null;
   }

   @Override
   public int getPriority(Event event) {
      // Run after input corrections so the bridge follows the player's actual controls.
      return this.mode.m228("Closet")
         && (event == Events.f3 || event == Events.f7 || event == Events.f8 || event == Events.f12) ? 100 : 0;
   }

   private boolean ensureSession() {
      if (mc.player == null || mc.level == null || mc.gameMode == null) {
         this.closet.reset();
         this.sessionPlayer = null;
         this.sessionWorld = null;
         this.activeMode = null;
         this.f578 = -1;
         return false;
      }
      boolean newSession = mc.player != this.sessionPlayer || mc.level != this.sessionWorld;
      if (newSession) {
         this.sessionPlayer = mc.player;
         this.sessionWorld = mc.level;
         this.f578 = mc.player.getInventory().getSelectedSlot();
      }
      if (newSession || !this.mode.m224().equals(this.activeMode)) {
         this.activeMode = this.mode.m224();
         this.f574 = this.f573 = this.f583 = this.f576 = false;
         this.f579 = this.f580 = 0;
         this.closet.reset();
         if (this.mode.m228("Closet")) this.closet.begin();
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
         if (event == Events.f2) DynamicIslandManager.sampleScaffoldMovement();
         if (!available()) {
            reset();
            begin();
            return;
         }
         if (event == Events.f3) updateRotation();
         else if (event == Events.f7) updateInput(Events.f7);
         else if (event == Events.f8) restoreMovementInput();
         else if (event == Events.f2) place();
         else if (event == Events.f12 && this.aiming) Events.f12.m99(false);
      }

      private void updateRotation() {
         if (this.rotationTick == mc.player.tickCount) return;
         this.rotationTick = mc.player.tickCount;
         EventMoveInput input = new EventMoveInput().m47(mc.options.keyUp.isDown(), mc.options.keyDown.isDown(),
            mc.options.keyLeft.isDown(), mc.options.keyRight.isDown(), mc.options.keyJump.isDown(),
            mc.options.keyShift.isDown(), false);
         int forward = (input.m48() ? 1 : 0) - (input.m49() ? 1 : 0);
         int strafe = (input.m50() ? 1 : 0) - (input.m51() ? 1 : 0);
         this.walking = forward != 0 || strafe != 0;
         boolean changed = this.route.update(mc.player.getYRot(), forward, strafe, mc.player.position());
         if (changed) {
            this.target = null;
            this.lastAimOffset = null;
            this.lastAimFace = null;
            if (strafe != 0) this.side = strafe > 0 ? -1 : 1;
         }
         this.stepDeferred = mc.player.onGround() && input.m52() && (!centerSupported()
            || this.walking && !canRiseAt(mc.player.position(), this.route.direction()));
         this.step.update(mc.player.getY(), mc.player.onGround(), input.m52() && !this.stepDeferred);
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
         this.yaw.advance(goalYaw, (float)closetYawSpeed.m220(), (float)closetAcceleration.m220(), step, true);
         this.pitch.advance(goalPitch, (float)closetPitchSpeed.m220(), (float)closetAcceleration.m220(), step, false);
         Events.f3.m81(previousYaw);
         Events.f3.m80(previousPitch);
         Events.f3.m77(this.yaw.angle);
         Events.f3.m83(this.pitch.angle);
         Events.f3.m85(this.aiming);
         Events.f3.m87(true);
      }

      private void updateInput(EventMoveInput input) {
         this.travel = this.walking ? this.route.travel(mc.player.position(), mc.player.getDeltaMovement()) : Vec3.ZERO;
         boolean jumpRequested = input.m52();
         boolean automaticSneak = this.step.allowsSneaking(jumpRequested);
         boolean jump = jumpRequested;
         if (this.hasBlocks && this.stepDeferred) jump = false;
         this.waitingForJump = this.hasBlocks && jump && mc.player.onGround() && this.step.rising && !readyToJump();
         if (this.waitingForJump) jump = false;
         input.m59(jump);
         this.predictedMotion = predictWalking(this.travel);
         boolean landingUnsafe = this.hasBlocks && this.step.rising && !supportedLanding(this.predictedMotion.scale(2));
         boolean holdMovement = this.waitingForJump || this.hasBlocks && this.step.rising
            && (this.step.needsClearance(mc.player.getY()) || landingUnsafe)
            || this.hasBlocks && jumpRequested && this.stepDeferred && !supportedPath(this.predictedMotion, closetEdgeMargin.m220());
         if (holdMovement) this.travel = brakingInput(mc.player.getDeltaMovement(), walkingAcceleration());
         this.predictedMotion = predictWalking(this.travel);
         boolean permitted = automaticSneak && mc.player.onGround()
            && (this.walking || this.predictedMotion.horizontalDistanceSqr() > 1.0E-5);
         double margin = closetEdgeMargin.m220();
         boolean entering = !supportedPath(this.predictedMotion, margin);
         boolean remaining = !supportedPath(this.predictedMotion, margin + 0.03);
         this.edgeSneaking = edgeState(this.edgeSneaking, permitted, entering, remaining);
         boolean interactive = this.target != null
            && mc.level.getBlockState(this.target.support()).getMenuProvider(mc.level, this.target.support()) != null;
         input.m60(this.step.shouldSneak(input.m53() || mc.options.keyShift.isDown(), jumpRequested, this.edgeSneaking, interactive));
         if (this.aiming || this.edgeSneaking) {
            input.m61(false);
            mc.player.setSprinting(false);
         }
         this.bridgeInput = new Input(input.m48(), input.m49(), input.m50(), input.m51(), input.m52(), input.m53(), input.m54());
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
            Vec2 move = relativeInput(this.travel, Events.f3.m76());
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
            mc.player.getBbWidth(), closetEdgeMargin.m220() + 0.03, feet -> !mc.level.noCollision(mc.player, feet));
      }

      private void planPath() {
         this.path.clear();
         Vec3 line = this.route.linePosition(mc.player.position());
         Vec3 position = new Vec3(line.x, this.step.placementY() + 1, line.z);
         this.path.addAll(walkingPath(position, this.walking ? this.route.direction() : Vec3.ZERO, closetLookAhead.m220()));
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
         Vec3 futureEyes = eyes.add(this.route.direction().scale(closetLookAhead.m220()));
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
         if (this.target == null || this.placementTick == mc.player.tickCount || Events.f3.isCancelled()) return;
         ClosetAim placing = this.target;
         int slot = mc.player.getInventory().getSelectedSlot();
         // Use the rotation already sent by sendPosition and recast after the player's movement.
         BlockHitResult hit = ray(Events.f3.m76(), Events.f3.m82(), mc.player.blockInteractionRange());
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
               double a = (minimum[axis] - origin[axis]) / direction[axis];
               double b = (maximum[axis] - origin[axis]) / direction[axis];
               near = Math.max(near, Math.min(a, b));
               far = Math.min(far, Math.max(a, b));
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
