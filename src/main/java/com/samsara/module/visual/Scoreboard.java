package com.samsara.module.visual;

import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.NumberSetting;

public class Scoreboard extends Feature {
   private static final String X_LABEL = "X";
   public final NumberSetting y;
   public final NumberSetting x = new NumberSetting(X_LABEL, this, 0.0, 0.0, 1000.0, 1.0);
   private static final String SCOREBOARD_LABEL = "Scoreboard";
   private static final String Y_LABEL = "Y";

   public Scoreboard() {
      super(SCOREBOARD_LABEL, Category.VISUAL);
      this.y = new NumberSetting(Y_LABEL, this, 0.0, -500.0, 500.0, 1.0);
   }
}
