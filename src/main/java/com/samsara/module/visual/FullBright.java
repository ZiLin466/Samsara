package com.samsara.module.visual;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import mixins.OptionInstanceAccessor;

public class FullBright extends Feature {
   private static final String FULL_BRIGHT_LABEL = "FullBright";

   private double previousGamma;

   public FullBright() {
      super(FULL_BRIGHT_LABEL, Category.VISUAL);
   }

   @Override
   public void onEnable() {
      this.previousGamma = (Double)mc.options.gamma().get();
      ((OptionInstanceAccessor)(Object)mc.options.gamma()).setValue(100.0);
   }

   @Override
   public void onEvent(Event event) {
      if (event == Events.ROTATION) {
         ((OptionInstanceAccessor)(Object)mc.options.gamma()).setValue(100.0);
      }
   }

   @Override
   public void onDisable() {
      mc.options.gamma().set(this.previousGamma);
   }
}
