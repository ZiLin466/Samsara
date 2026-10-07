package com.samsara.util;

public class TimerController {
   private static float multiplier = 1.0F;

   public static void reset() {
      multiplier = 1.0F;
   }

   public static void setMultiplier(float desiredMultiplier) {
      multiplier = desiredMultiplier;
   }

   public static boolean isDefault() {
      return multiplier == 1.0F;
   }

   public static float getMultiplier() {
      return multiplier;
   }
}
