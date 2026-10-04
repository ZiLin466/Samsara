package com.samsara.module.combat;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.BooleanSetting;
import com.samsara.setting.NumberSetting;
import com.mojang.blaze3d.platform.InputConstants.Key;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.phys.HitResult.Type;

public class AutoClicker extends Feature {
   private final BooleanSetting f244;
   private static final String f237 = "Min CPS";
   private static final String f238 = "Max CPS";
   private static final String f236 = "AutoClicker";
   private final NumberSetting f242;
   private final NumberSetting f241 = new NumberSetting(f237, this, 8.0, 1.0, 20.0, 1.0);
   private final BooleanSetting f243;
   private long f246;
   private final Random f245;
   private static final String f239 = "Randomize";
   private static final String f240 = "Break Blocks";

   @Override
   public void onEvent(Event var1) {
      if (var1 == Events.f3) {
         if (mc.gui.screen() != null) {
            return;
         }

         if (!mc.mouseHandler.isLeftPressed()) {
            return;
         }

         if (this.f244.m215() && mc.hitResult != null && mc.hitResult.getType() == Type.BLOCK) {
            Key var5 = com.mojang.blaze3d.platform.InputConstants.Type.MOUSE.getOrCreate(0);
            KeyMapping.set(var5, true);
            return;
         }

         long var2 = this.pm$56();
         if (System.currentTimeMillis() - this.f246 >= var2) {
            Key var4 = com.mojang.blaze3d.platform.InputConstants.Type.MOUSE.getOrCreate(0);
            KeyMapping.set(var4, true);
            KeyMapping.click(var4);
            KeyMapping.set(var4, false);
            this.f246 = System.currentTimeMillis();
         }
      }
   }

   @Override
   public void onEnable() {
      this.f246 = System.currentTimeMillis();
   }

   private long pm$56() {
      double var1;
      if (this.f243.m215()) {
         var1 = this.f241.m220() + (this.f242.m220() - this.f241.m220()) * this.f245.nextDouble();
      } else {
         var1 = this.f241.m220();
      }

      return (long)(1000.0 / var1);
   }

   public AutoClicker() {
      super(f236, Category.COMBAT);
      this.f242 = new NumberSetting(f238, this, 12.0, 1.0, 20.0, 1.0);
      this.f243 = new BooleanSetting(f239, this, true);
      this.f244 = new BooleanSetting(f240, this, false);
      this.f245 = new Random();
   }
}
