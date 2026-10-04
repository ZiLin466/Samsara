package com.samsara.setting;

import com.samsara.module.Feature;

public class ModeSetting extends Setting implements ChoiceSetting {
   private final String[] f736;
   private String f737;
   private String f738;

   public String m224() {
      return this.f737;
   }

   public String[] m227() {
      return this.f736;
   }

   public String m225() {
      return this.f738;
   }

   public boolean m228(String var1) {
      return this.f737.equals(var1);
   }

   public ModeSetting(String var1, Feature var2, String var3, String[] var4) {
      super(var1, var2);
      this.f736 = var4.clone();
      this.f737 = canonical(var3);
      this.f738 = this.f737;
   }

   /** Accept legacy capitalization, but only persist a real option. */
   public String canonical(String value) {
      for (String option : this.f736) if (option.equals(value)) return option;
      for (String option : this.f736) if (option.equalsIgnoreCase(value)) return option;
      throw new IllegalArgumentException("Unknown mode: " + value);
   }

   public void m226(String var1) { this.f737 = canonical(var1); }

   @Override public String[] options() { return m227(); }
   @Override public String selectionLabel() { return m224(); }
   @Override public boolean selected(int index) { return m228(this.f736[index]); }
   @Override public void select(int index) { m226(this.f736[index]); }
}
