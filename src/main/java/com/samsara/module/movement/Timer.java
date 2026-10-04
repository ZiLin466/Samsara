package com.samsara.module.movement;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.NumberSetting;
import com.samsara.util.TimerController;
import java.nio.charset.StandardCharsets;

public class Timer extends Feature {
   private final NumberSetting f433;
   private static final String f431 = "Timer";
   private static final String f432 = "Speed";

   @Override
   public void onDisable() {
      TimerController.setMultiplier(1.0F);
   }

   public Timer() {
      super(f431, Category.MOVEMENT);
      this.f433 = new NumberSetting(f432, this, 1.0, 0.1, 5.0, 0.1);
   }

   @Override
   public void onEvent(Event var1) {
      if (var1 == Events.f3) {
         TimerController.setMultiplier((float)this.f433.m220());
      }
   }
}
