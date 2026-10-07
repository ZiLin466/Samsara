package com.samsara.module.visual;

import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.ModeSetting;

public class Cape extends Feature {
   private static final String CRYPTIX_LABEL = "Cryptix";
   private static final String SKY_LABEL = "Sky";
   private static final String PUSHY_LABEL = "Pushy";
   private static final String CAPE_LABEL = "Cape";
   private static final String CAT_LABEL = "Cat";
   public final ModeSetting cape = new ModeSetting(CAPE_LABEL, this, CRYPTIX_LABEL, new String[]{CRYPTIX_LABEL, CAT_LABEL, PUSHY_LABEL, SKY_LABEL});

   public Cape() {
      super(CAPE_LABEL, Category.VISUAL);
   }
}
