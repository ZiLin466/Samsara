package com.samsara.setting;

import com.samsara.module.Feature;
import java.nio.charset.StandardCharsets;

public class NumberSetting extends Setting {
   private double f734;
   private final double f731;
   private double f735;
   private final double f733;
   private final double f732;
   private static final String f730 = "\\.";

   public double m220() {
      return this.f734;
   }

   public double m222() {
      return this.f733;
   }

   public double m219() {
      return this.f732;
   }

   public NumberSetting(String var1, Feature var2, double var3, double var5, double var7, double var9) {
      super(var1, var2);
      this.f735 = var3;
      this.f734 = var3;
      this.f731 = var5;
      this.f732 = var7;
      this.f733 = var9;
   }

   public double m218() {
      return this.f731;
   }

   public double m221() {
      return this.f735;
   }

   public void m223(double var1) {
      var1 = Math.max(this.f731, Math.min(this.f732, var1));
      var1 = (double)Math.round(var1 / this.f733) * this.f733;
      int var3 = String.valueOf(this.f733).split(f730)[1].length();
      var1 = (double)Math.round(var1 * Math.pow(10.0, (double)var3)) / Math.pow(10.0, (double)var3);
      this.f734 = var1;
   }
}
