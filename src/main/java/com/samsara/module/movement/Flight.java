package com.samsara.module.movement;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.NumberSetting;
import java.nio.charset.StandardCharsets;

public class Flight extends Feature {
   private static final String f371 = "Speed";
   private static final String f370 = "Flight";
   private NumberSetting f372 = new NumberSetting(f371, this, 1.0, 1.0, 10.0, 0.5);

   public Flight() {
      super(f370, Category.MOVEMENT);
   }

   @Override
   public void onEvent(Event var1) {
      if (var1 == Events.f1) {
         mc.player.getAbilities().flying = true;
         mc.player.getAbilities().setFlyingSpeed((float)this.f372.m220() * 0.1F);
      }
   }
}
