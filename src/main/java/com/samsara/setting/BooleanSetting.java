package com.samsara.setting;

import com.samsara.module.Feature;

public class BooleanSetting extends Setting {
   private boolean f728;
   private boolean f729;

   public BooleanSetting(String var1, Feature var2, boolean var3) {
      super(var1, var2);
      this.f729 = var3;
      this.f728 = var3;
   }

   public void m217(boolean var1) {
      this.f728 = var1;
   }

   public boolean m215() {
      return this.f728;
   }

   public boolean m216() {
      return this.f729;
   }
}
