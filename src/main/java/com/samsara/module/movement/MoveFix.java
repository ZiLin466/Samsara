package com.samsara.module.movement;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.BooleanSetting;
import com.samsara.setting.ModeSetting;
import java.nio.charset.StandardCharsets;
import mixins.ClientInputAccessor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;

public class MoveFix extends Feature {
   private static final String f396 = "MoveFix";
   private final BooleanSetting f402;
   private static final String f400 = "Use PrevYaw";
   private static final String f398 = "Silent";
   private final ModeSetting f401;
   private static final String f397 = "Mode";
   private static final String f399 = "Strict";

   private float pm$81(float var1, float var2, float var3) {
      if (var2 == 0.0F && var3 == 0.0F) {
         return (float)Math.toRadians((double)var1);
      } else {
         double var4 = Math.toRadians((double)var1);
         double var6 = Math.sin(var4);
         double var8 = Math.cos(var4);
         double var10 = (double)var2 * var8 - (double)var3 * var6;
         double var12 = (double)var2 * var6 + (double)var3 * var8;
         return (float)Math.atan2(var12, var10);
      }
   }

   @Override
   public void onEvent(Event var1) {
      if (var1 == Events.f3) {
         this.setSuffix(this.f401.m224());
         Events.f3.m85(true);
         if (!this.f402.m215()) {
            Events.f3.m87(false);
         }
      }

      if (var1 == Events.f8 && this.f401.m228(f398)) {
         ClientInputAccessor var2 = (ClientInputAccessor)mc.player.input;
         float var3 = var2.getMoveVector().y;
         float var4 = var2.getMoveVector().x;
         if (var3 == 0.0F && var4 == 0.0F) {
            return;
         }

         double var5 = Mth.wrapDegrees(Math.toDegrees((double)this.pm$81(Events.f3.m76(), var3, var4)));
         float var7 = 0.0F;
         float var8 = 0.0F;
         float var9 = Float.MAX_VALUE;

         for (float var10 = -1.0F; var10 <= 1.0F; var10++) {
            for (float var11 = -1.0F; var11 <= 1.0F; var11++) {
               if (var11 != 0.0F || var10 != 0.0F) {
                  double var12 = Mth.wrapDegrees(Math.toDegrees((double)this.pm$81(mc.player.getYRot(), var10, var11)));
                  double var14 = Math.abs(var5 - var12);
                  if (var14 < (double)var9) {
                     var9 = (float)var14;
                     var7 = var10;
                     var8 = var11;
                  }
               }
            }
         }

         Vec2 var16 = new Vec2(var8, var7);
         if (var16.length() > 1.0F) {
            var16 = var16.normalized();
         }

         var2.setMoveVector(var16);
         mc.player.input.keyPresses = new Input(
            var7 > 0.0F,
            var7 < 0.0F,
            var8 > 0.0F,
            var8 < 0.0F,
            mc.player.input.keyPresses.jump(),
            mc.player.input.keyPresses.shift(),
            mc.player.input.keyPresses.sprint()
         );
      }
   }

   public MoveFix() {
      super(f396, Category.MOVEMENT);
      this.f401 = new ModeSetting(f397, this, f398, new String[]{f398, f399});
      this.f402 = new BooleanSetting(f400, this, true);
   }
}
