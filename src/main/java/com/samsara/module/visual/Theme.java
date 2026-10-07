package com.samsara.module.visual;

import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.ModeSetting;
import com.samsara.setting.NumberSetting;

public class Theme extends Feature {
   private static final String SAKURA_LABEL = "Sakura";
   private static final String NOVA_LABEL = "Nova";
   private static final String INFERNO_LABEL = "Inferno";
   private static final String RAINBOW_LABEL = "Rainbow";
   private static final String THEME_LABEL = "Theme";
   private static final String OCEAN_LABEL = "Ocean";
   private static final String CHERRY_LABEL = "Cherry";
   private static final String DEFAULT_LABEL = "Default";
   private static final String FLOWER_LABEL = "Flower";
   public final ModeSetting theme;
   private static final String SPEED_LABEL = "Speed";
   private static final String GOLD_LABEL = "Gold";
   public final NumberSetting speed;
   private static final String EMERALD_LABEL = "Emerald";

   public Theme() {
      super(THEME_LABEL, Category.VISUAL);
      this.theme = new ModeSetting(THEME_LABEL, this, DEFAULT_LABEL, new String[]{DEFAULT_LABEL, CHERRY_LABEL, EMERALD_LABEL, FLOWER_LABEL, GOLD_LABEL, INFERNO_LABEL, NOVA_LABEL, OCEAN_LABEL, RAINBOW_LABEL, SAKURA_LABEL});
      this.speed = new NumberSetting(SPEED_LABEL, this, 1.0, 0.5, 5.0, 0.25);
   }
}
