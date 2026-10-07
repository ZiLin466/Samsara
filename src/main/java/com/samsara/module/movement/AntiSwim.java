package com.samsara.module.movement;

import com.samsara.module.Category;
import com.samsara.module.Feature;

public class AntiSwim extends Feature {
   private static final String ANTI_SWIM_LABEL = "AntiSwim";

   public AntiSwim() {
      super(ANTI_SWIM_LABEL, Category.MOVEMENT);
   }
}
