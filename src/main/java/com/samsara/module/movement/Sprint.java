package com.samsara.module.movement;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import java.nio.charset.StandardCharsets;

public class Sprint extends Feature {
   private static final String f426 = "Sprint";

   public Sprint() {
      super(f426, Category.MOVEMENT);
   }

   @Override
   public void onDisable() {
      mc.options.keySprint.setDown(false);
   }

   @Override
   public void onEvent(Event var1) {
      if (var1 == Events.f3) {
         mc.options.keySprint.setDown(true);
      }
   }
}
