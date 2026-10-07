package com.samsara.setting;

import com.samsara.module.Feature;

public class ModeSetting extends Setting implements ChoiceSetting {
   private final String[] options;
   private String value;
   private String defaultValue;

   public String getValue() {
      return this.value;
   }

   public String[] getOptions() {
      return this.options;
   }

   public String getDefaultValue() {
      return this.defaultValue;
   }

   public boolean is(String option) {
      return this.value.equals(option);
   }

   public ModeSetting(String name, Feature feature, String value, String[] options) {
      super(name, feature);
      this.options = options.clone();
      this.value = canonical(value);
      this.defaultValue = this.value;
   }

   /** Accept legacy capitalization, but only persist a real option. */
   public String canonical(String value) {
      for (String option : this.options) if (option.equals(value)) return option;
      for (String option : this.options) if (option.equalsIgnoreCase(value)) return option;
      throw new IllegalArgumentException("Unknown mode: " + value);
   }

   public void setValue(String value) { this.value = canonical(value); }

   @Override public String[] options() { return getOptions(); }
   @Override public String selectionLabel() { return getValue(); }
   @Override public boolean selected(int index) { return is(this.options[index]); }
   @Override public void select(int index) { setValue(this.options[index]); }
}
