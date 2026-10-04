package com.samsara.module.visual;

import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.ModeSetting;
import com.samsara.setting.NumberSetting;
import java.nio.charset.StandardCharsets;

public class Theme extends Feature {
   private static final String f724 = "Sakura";
   private static final String f721 = "Nova";
   private static final String f720 = "Inferno";
   private static final String f723 = "Rainbow";
   private static final String f714 = "Theme";
   private static final String f722 = "Ocean";
   private static final String f716 = "Cherry";
   private static final String f715 = "Default";
   private static final String f718 = "Flower";
   public final ModeSetting f34;
   private static final String f725 = "Speed";
   private static final String f719 = "Gold";
   public final NumberSetting f35;
   private static final String f717 = "Emerald";

   public Theme() {
      super(f714, Category.VISUAL);
      this.f34 = new ModeSetting(f714, this, f715, new String[]{f715, f716, f717, f718, f719, f720, f721, f722, f723, f724});
      this.f35 = new NumberSetting(f725, this, 1.0, 0.5, 5.0, 0.25);
   }
}
