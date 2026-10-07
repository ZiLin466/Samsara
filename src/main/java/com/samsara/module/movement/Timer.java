package com.samsara.module.movement;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.NumberSetting;
import com.samsara.util.TimerController;

public class Timer extends Feature {
   private final NumberSetting speed;
   private static final String TIMER_LABEL = "Timer";
   private static final String SPEED_LABEL = "Speed";

   @Override
   public void onDisable() {
      TimerController.setMultiplier(1.0F);
   }

   public Timer() {
      super(TIMER_LABEL, Category.MOVEMENT);
      this.speed = new NumberSetting(SPEED_LABEL, this, 1.0, 0.1, 5.0, 0.1);
   }

   @Override
   public void onEvent(Event event) {
      if (event == Events.ROTATION) {
         TimerController.setMultiplier((float)this.speed.getValue());
      }
   }
}
