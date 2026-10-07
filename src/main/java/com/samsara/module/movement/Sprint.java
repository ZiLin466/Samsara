package com.samsara.module.movement;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;

public class Sprint extends Feature {
   private static final String SPRINT_LABEL = "Sprint";

   public Sprint() {
      super(SPRINT_LABEL, Category.MOVEMENT);
   }

   @Override
   public void onDisable() {
      mc.options.keySprint.setDown(false);
   }

   @Override
   public void onEvent(Event event) {
      if (event == Events.ROTATION) {
         mc.options.keySprint.setDown(true);
      }
   }
}
