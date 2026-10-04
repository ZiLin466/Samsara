package com.samsara.module.visual;

import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.ModeSetting;
import java.nio.charset.StandardCharsets;

public class Cape extends Feature {
   private static final String f612 = "Cryptix";
   private static final String f615 = "Sky";
   private static final String f614 = "Pushy";
   private static final String f611 = "Cape";
   private static final String f613 = "Cat";
   public final ModeSetting f31 = new ModeSetting(f611, this, f612, new String[]{f612, f613, f614, f615});

   public Cape() {
      super(f611, Category.VISUAL);
   }
}
