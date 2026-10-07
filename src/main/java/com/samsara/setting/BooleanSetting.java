package com.samsara.setting;

import com.samsara.module.Feature;

public class BooleanSetting extends Setting {
   private boolean value;
   private boolean defaultValue;

   public BooleanSetting(String name, Feature feature, boolean defaultValue) {
      super(name, feature);
      this.defaultValue = defaultValue;
      this.value = defaultValue;
   }

   public void setValue(boolean value) {
      this.value = value;
   }

   public boolean getValue() {
      return this.value;
   }

   public boolean getDefaultValue() {
      return this.defaultValue;
   }
}
