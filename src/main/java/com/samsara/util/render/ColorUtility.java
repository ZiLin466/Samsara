package com.samsara.util.render;

import java.awt.Color;

public final class ColorUtility {

   private ColorUtility() {
   }

   public static int getShadowColor(int color) {
      return (color & 0xFCFCFC) >> 2 | color & 0xFF000000;
   }

   public static int[] hexToRGBA(int hex) {
      int red = hex >> 16 & 0xFF;
      int green = hex >> 8 & 0xFF;
      int blue = hex & 0xFF;
      int alpha = hex >> 24 & 0xFF;
      return new int[]{red, green, blue, alpha};
   }

   public static int rgbaToHex(int red, int green, int blue, int alpha) {
      return alpha << 24 | red << 16 | green << 8 | blue;
   }

   public static int applyOpacity(int color, float opacityFactor) {
      opacityFactor = Math.min(1, Math.max(0, opacityFactor));
      return (int)(opacityFactor * 255F) << 24 | color & 0xFFFFFF;
   }

   /** Scales the existing alpha, preserving the material's opacity. */
   public static int multiplyOpacity(int color, float opacityFactor) {
      return (int)((color >>> 24) * Math.clamp(opacityFactor, 0, 1)) << 24 | color & 0xFFFFFF;
   }

   public static int multiplyOpacityRounded(int color, float opacityFactor) {
      return Math.round((color >>> 24) * Math.clamp(opacityFactor, 0, 1)) << 24 | color & 0xFFFFFF;
   }

   /** UI transitions use float arithmetic and truncate each channel. */
   public static int mix(int from, int to, float fraction) {
      fraction = Math.clamp(fraction, 0, 1);
      int result = 0;
      for (int shift = 0; shift <= 24; shift += 8) {
         int start = from >>> shift & 0xFF;
         int end = to >>> shift & 0xFF;
         result |= (int)(start + (end - start) * fraction) << shift;
      }
      return result;
   }

   public static int interpolateColors(int color1, int color2, float amount) {
      amount = Math.min(1, Math.max(0, amount));

      int result = 0;
      for (int shift = 0; shift <= 24; shift += 8) {
         int start = color1 >>> shift & 0xFF;
         int end = color2 >>> shift & 0xFF;
         result |= (int)interpolate(start, end, amount) << shift;
      }
      return result;
   }

   public static int rainbow(int speed, int index, float saturation, float brightness) {
      int angle = (int)((System.currentTimeMillis() / speed + index) % 360);
      float hue = angle / 360f;
      return Color.HSBtoRGB(hue, saturation, brightness);
   }

   public static int interpolateColorsBackAndForth(int speed, int index, int startColor, int endColor) {
      int angle = (int)((System.currentTimeMillis() / speed - index) % 360);
      angle = (angle >= 180 ? 360 - angle : angle) * 2;
      return interpolateColors(startColor, endColor, angle / 360f);
   }

   private static double interpolate(double from, double to, double fraction) {
      return from + (to - from) * fraction;
   }
}
