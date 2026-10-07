package com.samsara.module.movement;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.NumberSetting;

public class Flight extends Feature {
   private static final String SPEED_LABEL = "Speed";
   private static final String FLIGHT_LABEL = "Flight";
   private NumberSetting speed = new NumberSetting(SPEED_LABEL, this, 1.0, 1.0, 10.0, 0.5);

   public Flight() {
      super(FLIGHT_LABEL, Category.MOVEMENT);
   }

   @Override
   public void onEvent(Event event) {
      if (event == Events.PRE_MOTION) {
         mc.player.getAbilities().flying = true;
         mc.player.getAbilities().setFlyingSpeed((float)this.speed.getValue() * 0.1F);
      }
   }
}
