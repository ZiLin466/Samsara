package com.samsara.module.combat;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.NumberSetting;
import com.samsara.util.TargetFinder;
import java.nio.charset.StandardCharsets;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

public class AimAssist extends Feature {
   private static final String f231 = "Speed";
   private static final String f230 = "Range";
   private static final String f229 = "AimAssist";
   private NumberSetting f232 = new NumberSetting(f230, this, 6.0, 3.0, 8.0, 0.5);
   private NumberSetting f233 = new NumberSetting(f231, this, 5.0, 1.0, 10.0, 0.5);

   private float pm$54() {
      float var1 = ((Double)mc.options.sensitivity().get()).floatValue();
      float var2 = var1 * 0.6F + 0.2F;
      return var2 * var2 * var2 * 8.0F * 0.15F;
   }

   @Override
   public void onEvent(Event var1) {
      if (var1 == Events.f1) {
         LivingEntity var2 = TargetFinder.m47(this.f232.m220(), true);
         if (var2 != null) {
            AABB var3 = var2.getBoundingBox();
            double var4 = Mth.clamp(mc.player.getX(), var3.minX, var3.maxX) - mc.player.getX();
            double var6 = Mth.clamp(mc.player.getEyeY(), var3.minY, var3.maxY) - mc.player.getEyeY();
            double var8 = Mth.clamp(mc.player.getZ(), var3.minZ, var3.maxZ) - mc.player.getZ();
            double var10 = Math.sqrt(var4 * var4 + var8 * var8);
            float var12 = (float)(Math.toDegrees(Math.atan2(var8, var4)) - 90.0);
            float var13 = (float)(-Math.toDegrees(Math.atan2(var6, var10)));
            float var14 = Mth.wrapDegrees(var12 - mc.player.getYRot());
            float var15 = var13 - mc.player.getXRot();
            float var16 = (float)Math.min((double)Math.abs(var14), this.f233.m220());
            float var17 = (float)Math.min((double)Math.abs(var15), this.f233.m220());
            float var18 = this.pm$55(Math.copySign(var16, var14), mc.player.getYRot());
            float var19 = this.pm$55(Math.copySign(var17, var15), mc.player.getXRot());
            mc.player.setYRot(mc.player.getYRot() + var18);
            mc.player.setXRot(mc.player.getXRot() + var19);
         }
      }
   }

   private float pm$55(float var1, float var2) {
      float var3 = this.pm$54();
      float var4 = var1 - var2;
      var4 -= var4 % var3;
      return var2 + var4;
   }

   public AimAssist() {
      super(f229, Category.COMBAT);
   }
}
