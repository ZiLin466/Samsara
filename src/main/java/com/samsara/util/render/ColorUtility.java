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
      int[] colorRGBA = hexToRGBA(color);
      return rgbaToHex(colorRGBA[0], colorRGBA[1], colorRGBA[2], (int)(opacityFactor * 255F));
   }

   public static int interpolateColors(int color1, int color2, float amount) {
      amount = Math.min(1, Math.max(0, amount));

      int[] color1RGBA = hexToRGBA(color1);
      int[] color2RGBA = hexToRGBA(color2);

      int r = (int)interpolate(color1RGBA[0], color2RGBA[0], amount);
      int g = (int)interpolate(color1RGBA[1], color2RGBA[1], amount);
      int b = (int)interpolate(color1RGBA[2], color2RGBA[2], amount);
      int a = (int)interpolate(color1RGBA[3], color2RGBA[3], amount);

      return rgbaToHex(r, g, b, a);
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

   private static double interpolate(double a, double b, double v) {
      return a + (b - a) * v;
   }
}
