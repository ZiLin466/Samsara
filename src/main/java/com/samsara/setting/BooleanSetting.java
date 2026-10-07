package com.samsara.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
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

   @Override public JsonElement snapshot(boolean defaults) {
      return new JsonPrimitive(defaults ? this.defaultValue : this.value);
   }

   @Override protected Runnable prepareValue(JsonElement value) {
      if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
         throw new IllegalArgumentException("Invalid boolean");
      }
      boolean desired = value.getAsBoolean();
      return () -> setValue(desired);
   }
}
