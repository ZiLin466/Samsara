package com.samsara.module.visual;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.module.FeatureManager;
import com.samsara.setting.BooleanSetting;
import com.samsara.util.ColorUtil;
import com.samsara.util.Vector3d;
import com.samsara.util.WorldToScreenProjector;
import java.nio.charset.StandardCharsets;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class PlayerEsp extends Feature {
   private static final String f652 = "Health bar";
   private static final String f650 = "PlayerESP";
   private final BooleanSetting f654;
   private final BooleanSetting f655;
   private final BooleanSetting f656;
   private final Vector3d f657;
   private static final String f651 = "2D";
   private static final String f653 = "Outline";

   public PlayerEsp() {
      super(f650, Category.VISUAL);
      this.f654 = new BooleanSetting(f651, this, false);
      this.f655 = new BooleanSetting(f652, this, false);
      this.f656 = new BooleanSetting(f653, this, false);
      this.f657 = new Vector3d();
   }

   private void draw2DBox(GuiGraphicsExtractor var1, LivingEntity var2, int var3, float var4, Vector3d var5) {
      double var6 = Double.MAX_VALUE;
      double var8 = Double.MAX_VALUE;
      double var10 = -Double.MAX_VALUE;
      double var12 = -Double.MAX_VALUE;
      double var14 = var2.xOld + (var2.getX() - var2.xOld) * (double)var4;
      double var16 = var2.yOld + (var2.getY() - var2.yOld) * (double)var4;
      double var18 = var2.zOld + (var2.getZ() - var2.zOld) * (double)var4;
      double var20 = (double)var2.getBbWidth() * 0.5;
      double var22 = (double)var2.getBbHeight();
      double var24 = var14 - var20;
      double var26 = var14 + var20;
      double var30 = var16 + var22 + 0.15;
      double var32 = var18 - var20;
      double var34 = var18 + var20;
      Vector3d var36 = WorldToScreenProjector.m43(var24, var16, var32, var5);
      if (var36 != null) {
         var6 = Math.min(var6, var36.x);
         var8 = Math.min(var8, var36.y);
         var10 = Math.max(var10, var36.x);
         var12 = Math.max(var12, var36.y);
         var36 = WorldToScreenProjector.m43(var24, var16, var34, var5);
         if (var36 != null) {
            var6 = Math.min(var6, var36.x);
            var8 = Math.min(var8, var36.y);
            var10 = Math.max(var10, var36.x);
            var12 = Math.max(var12, var36.y);
            var36 = WorldToScreenProjector.m43(var26, var16, var32, var5);
            if (var36 != null) {
               var6 = Math.min(var6, var36.x);
               var8 = Math.min(var8, var36.y);
               var10 = Math.max(var10, var36.x);
               var12 = Math.max(var12, var36.y);
               var36 = WorldToScreenProjector.m43(var26, var16, var34, var5);
               if (var36 != null) {
                  var6 = Math.min(var6, var36.x);
                  var8 = Math.min(var8, var36.y);
                  var10 = Math.max(var10, var36.x);
                  var12 = Math.max(var12, var36.y);
                  var36 = WorldToScreenProjector.m43(var24, var30, var32, var5);
                  if (var36 != null) {
                     var6 = Math.min(var6, var36.x);
                     var8 = Math.min(var8, var36.y);
                     var10 = Math.max(var10, var36.x);
                     var12 = Math.max(var12, var36.y);
                     var36 = WorldToScreenProjector.m43(var24, var30, var34, var5);
                     if (var36 != null) {
                        var6 = Math.min(var6, var36.x);
                        var8 = Math.min(var8, var36.y);
                        var10 = Math.max(var10, var36.x);
                        var12 = Math.max(var12, var36.y);
                        var36 = WorldToScreenProjector.m43(var26, var30, var32, var5);
                        if (var36 != null) {
                           var6 = Math.min(var6, var36.x);
                           var8 = Math.min(var8, var36.y);
                           var10 = Math.max(var10, var36.x);
                           var12 = Math.max(var12, var36.y);
                           var36 = WorldToScreenProjector.m43(var26, var30, var34, var5);
                           if (var36 != null) {
                              var6 = Math.min(var6, var36.x);
                              var8 = Math.min(var8, var36.y);
                              var10 = Math.max(var10, var36.x);
                              var12 = Math.max(var12, var36.y);
                              int var37 = (int)var6;
                              int var38 = (int)var8;
                              int var39 = (int)var10;
                              int var40 = (int)var12;
                              byte var41 = 1;
                              if (this.f654.m215()) {
                                 byte var42 = 1;
                                 var1.fill(var37 - var42, var38 - var42, var39 + var42, var38 + var42 + var41, -16777216);
                                 var1.fill(var37 - var42, var40 - var42 - var41, var39 + var42, var40 + var42, -16777216);
                                 var1.fill(var37 - var42, var38 - var42, var37 + var42 + var41, var40 + var42, -16777216);
                                 var1.fill(var39 - var42 - var41, var38 - var42, var39 + var42, var40 + var42, -16777216);
                                 var1.fill(var37, var38, var39, var38 + var41, var3);
                                 var1.fill(var37, var40 - var41, var39, var40, var3);
                                 var1.fill(var37, var38, var37 + var41, var40, var3);
                                 var1.fill(var39 - var41, var38, var39, var40, var3);
                              }

                              if (this.f655.m215()) {
                                 double var89 = Math.clamp((double)(var2.getHealth() / var2.getMaxHealth()), 0.0, 1.0);
                                 int var44 = ColorUtil.m26((float)var89);
                                 int var45 = Math.max(1, var40 - var38);
                                 int var46 = Math.max(var2.getHealth() > 0.0F ? 1 : 0, (int)Math.ceil((double)var45 * var89));
                                 byte var47 = 4;
                                 int var48 = var37 - var47;
                                 int var49 = var48 + 1;
                                 var1.fill(var48 - 1, var38 - 1, var49 + 1, var38 + var45 + 1, -16777216);
                                 var1.fill(var48, var38, var49, var38 + var45, -11513776);
                                 if (var46 > 0) {
                                    var1.fill(var48, var38 + var45 - var46, var49, var38 + var45, var44);
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   public void onEvent(Event var1) {
      if (var1 == Events.f5 && (this.f654.m215() || this.f655.m215())) {
         GuiGraphicsExtractor var2 = Events.f5.m89();
         float var3 = Events.f5.m91();
         int var4 = ColorUtil.m27();

         for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof LivingEntity var6 && FeatureManager.targets.shouldShow(var6)) {
               this.draw2DBox(var2, var6, var4, var3, this.f657);
            }
         }
      }

      if (var1 == Events.f14 && this.f656.m215()) {
         Entity var7 = Events.f14.m38();
         if (!FeatureManager.targets.shouldShow(var7)) {
            return;
         }

         Events.f14.m39(ColorUtil.m27());
      }
   }
}
