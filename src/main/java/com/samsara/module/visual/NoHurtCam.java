package com.samsara.module.visual;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import java.nio.charset.StandardCharsets;

public class NoHurtCam extends Feature {
   private static final String f649 = "NoHurtCam";

   public NoHurtCam() {
      super(f649, Category.VISUAL);
   }

   @Override
   public void onEvent(Event var1) {
      if (var1 == Events.f6) {
         var1.setCancelled(true);
      }
   }
}
