package com.samsara.setting;

import com.samsara.module.Feature;

public class NumberSetting extends Setting {
   private double value;
   private final double minimum;
   private double defaultValue;
   private final double step;
   private final double maximum;
   private final double decimalMultiplier;
   private static final String DECIMAL_SEPARATOR_PATTERN = "\\.";

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
}
