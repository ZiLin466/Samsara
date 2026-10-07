package com.samsara.module.movement;

import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.NumberSetting;

public class KeepSprint extends Feature {
   private static final String KEEP_SPRINT_LABEL = "KeepSprint";
   private static final String SPEED_LABEL = "Speed";

   private NumberSetting speed;

   public KeepSprint() {
      super(KEEP_SPRINT_LABEL, Category.MOVEMENT);
      this.speed = new NumberSetting(SPEED_LABEL, this, 0.6, 0.6, 1.0, 0.05);
   }

   public double getSprintMotionMultiplier() {
      return !this.isEnabled() ? 0.6 : this.speed.getValue();
   }
}
