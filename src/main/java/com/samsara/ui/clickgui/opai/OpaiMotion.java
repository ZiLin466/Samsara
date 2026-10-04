package com.samsara.ui.clickgui.opai;

final class OpaiMotion {
   static double now() {
      return System.nanoTime() / 1_000_000.0;
   }

   private final double rate;
   private double time = Double.NaN;
   private float value;
   private float destination;

   OpaiMotion(float initial, double rate) {
      this.value = initial;
      this.destination = initial;
      this.rate = rate;
   }

   float approach(float target, double now) {
      if (!Double.isNaN(this.time)) {
         double dt = Math.max(0, now - this.time) / 1000;
         this.value = this.destination + (this.value - this.destination) * (float)Math.exp(-this.rate * dt);
         if (Math.abs(this.value - this.destination) < .0005f) this.value = this.destination;
      }
      this.time = now;
      this.destination = target;
      return this.value;
   }

   float value() {
      return this.value;
   }

   void snap(float value, double now) {
      this.value = value;
      this.destination = value;
      this.time = now;
   }

   static float openingScale(double elapsedMs) {
      double seconds = Math.max(0, elapsedMs) / 1000;
      return seconds >= .32 ? 1 : 1 + (float)(.18 * Math.exp(-17 * seconds) * Math.cos(23 * seconds));
   }
}
