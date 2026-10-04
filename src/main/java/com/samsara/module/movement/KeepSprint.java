package com.samsara.module.movement;

import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.NumberSetting;
import java.nio.charset.StandardCharsets;

public class KeepSprint extends Feature {
   private static final String f376 = "KeepSprint";
   private NumberSetting f378;
   private static final String f377 = "Speed";

   public KeepSprint() {
      super(f376, Category.MOVEMENT);
      this.f378 = new NumberSetting(f377, this, 0.6, 0.6, 1.0, 0.05);
   }

   public double m156() {
      return !this.isEnabled() ? 0.6 : this.f378.m220();
   }
}
