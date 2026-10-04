package com.samsara.module.visual;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import java.nio.charset.StandardCharsets;
import mixins.OptionInstanceAccessor;

public class FullBright extends Feature {
   private double f619;
   private static final String f618 = "FullBright";

   public FullBright() {
      super(f618, Category.VISUAL);
   }

   @Override
   public void onEnable() {
      this.f619 = (Double)mc.options.gamma().get();
      ((OptionInstanceAccessor)(Object)mc.options.gamma()).setValue(100.0);
   }

   @Override
   public void onEvent(Event var1) {
      if (var1 == Events.f3) {
         ((OptionInstanceAccessor)(Object)mc.options.gamma()).setValue(100.0);
      }
   }

   @Override
   public void onDisable() {
      mc.options.gamma().set(this.f619);
   }
}
