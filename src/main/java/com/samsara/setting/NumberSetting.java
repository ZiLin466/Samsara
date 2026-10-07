package com.samsara.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.samsara.module.Feature;

public class NumberSetting extends Setting {
   private static final String DECIMAL_SEPARATOR_PATTERN = "\\.";

   private final double minimum;
   private final double step;
   private final double maximum;
   private final double decimalMultiplier;

   private double value;
   private double defaultValue;

   public double getValue() {
      return this.value;
   }

   public double getStep() {
      return this.step;
   }

   public double getMaximum() {
      return this.maximum;
   }

   public NumberSetting(String name, Feature feature, double defaultValue, double minimum, double maximum, double step) {
      super(name, feature);
      this.defaultValue = defaultValue;
      this.value = defaultValue;
      this.minimum = minimum;
      this.maximum = maximum;
      this.step = step;
      int decimalPlaces = String.valueOf(step).split(DECIMAL_SEPARATOR_PATTERN)[1].length();
      this.decimalMultiplier = Math.pow(10.0, decimalPlaces);
   }

   public double getMinimum() {
      return this.minimum;
   }

   public double getDefaultValue() {
      return this.defaultValue;
   }

   public void setValue(double value) {
      value = Math.max(this.minimum, Math.min(this.maximum, value));
      value = (double)Math.round(value / this.step) * this.step;
      value = (double)Math.round(value * this.decimalMultiplier) / this.decimalMultiplier;
      this.value = value;
   }

   @Override public JsonElement snapshot(boolean defaults) {
      return new JsonPrimitive(defaults ? this.defaultValue : this.value);
   }

   @Override protected Runnable prepareValue(JsonElement value) {
      double desired = value.getAsDouble();
      if (!Double.isFinite(desired)) throw new IllegalArgumentException("Non-finite setting");
      return () -> setValue(desired);
   }
}
