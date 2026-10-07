package com.samsara.module.visual;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;

public class NoHurtCamera extends Feature {
   private static final String NO_HURT_CAM_LABEL = "NoHurtCam";

   public NoHurtCamera() {
      super(NO_HURT_CAM_LABEL, Category.VISUAL);
   }

   @Override
   public void onEvent(Event event) {
      if (event == Events.HURT_CAMERA) {
         event.setCancelled(true);
      }
   }
}
