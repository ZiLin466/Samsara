package com.samsara.module.visual;

import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.NumberSetting;
import java.nio.charset.StandardCharsets;

public class Scoreboard extends Feature {
   private static final String f659 = "X";
   public final NumberSetting f33;
   public final NumberSetting f32 = new NumberSetting(f659, this, 0.0, 0.0, 1000.0, 1.0);
   private static final String f658 = "Scoreboard";
   private static final String f660 = "Y";

   public Scoreboard() {
      super(f658, Category.VISUAL);
      this.f33 = new NumberSetting(f660, this, 0.0, -500.0, 500.0, 1.0);
   }
}
