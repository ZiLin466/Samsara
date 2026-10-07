package com.samsara.util;

import com.samsara.module.FeatureManager;
import java.awt.Color;
import net.minecraft.util.Mth;

public class ClientColors {
   private static int firstRed;
   private static final String SAKURA_LABEL = "Sakura";
   private static int secondAlpha;
   private static final String INFERNO_LABEL = "Inferno";
   private static final String EMERALD_LABEL = "Emerald";
   private static long frameTimeMillis;
   private static String cachedTheme;
   private static boolean rainbow;
   private static int secondBlue;
   private static final float HUE_OFFSET_PER_DEGREE = 0.0027777778F;
   private static final double HUE_CYCLE_PER_MILLISECOND = 1.6666666666666666E-4;
   private static final double SECONDS_PER_MILLISECOND = 0.001;
   private static float rainbowHue;
   private static final String CHERRY_LABEL = "Cherry";
   private static int firstGreen;
   private static int firstColor = -1;
   private static final String GOLD_LABEL = "Gold";
   private static final String NOVA_LABEL = "Nova";
   private static int firstAlpha;
   private static int secondColor = -1;
   private static double gradientPhase;
   private static int secondGreen;
   private static final String RAINBOW_LABEL = "Rainbow";
   private static int firstBlue;
   private static final String OCEAN_LABEL = "Ocean";
   private static int secondRed;

   public static void updateTheme() {
      String themeName = FeatureManager.theme.theme.getValue();
      double speed = FeatureManager.theme.speed.getValue();
      frameTimeMillis = System.currentTimeMillis();
      rainbowHue = (float)((double)frameTimeMillis * speed % 6000.0 * HUE_CYCLE_PER_MILLISECOND);
      gradientPhase = (double)frameTimeMillis * SECONDS_PER_MILLISECOND * speed;
      rainbow = RAINBOW_LABEL.equals(themeName);
      if (!themeName.equals(cachedTheme)) {
         cachedTheme = themeName;
         switch (themeName) {
            case GOLD_LABEL -> { firstColor = 0xFFFFA500; secondColor = 0xFFFFFF00; }
            case OCEAN_LABEL -> { firstColor = 0xFF00C6FF; secondColor = 0xFF0072FF; }
            case NOVA_LABEL -> { firstColor = 0xFFFF64C8; secondColor = 0xFF1996FF; }
            case EMERALD_LABEL -> { firstColor = 0xFF00E676; secondColor = 0xFF164D2B; }
            case INFERNO_LABEL -> { firstColor = 0xFFFF5722; secondColor = 0xFF7A321F; }
            case SAKURA_LABEL -> { firstColor = 0xFFFF80AB; secondColor = 0xFFE040FB; }
            case CHERRY_LABEL -> { firstColor = 0xFFDD3D69; secondColor = 0xFFE0B3B7; }
            // Flower's previous fall-through produced white; retain that palette.
            default -> { firstColor = 0xFFFFFFFF; secondColor = 0xFFFFFFFF; }
         }

         firstAlpha = firstColor >>> 24;
         firstRed = firstColor >> 16 & 0xFF;
         firstGreen = firstColor >> 8 & 0xFF;
         firstBlue = firstColor & 0xFF;
         secondAlpha = secondColor >>> 24;
         secondRed = secondColor >> 16 & 0xFF;
         secondGreen = secondColor >> 8 & 0xFF;
         secondBlue = secondColor & 0xFF;
      }
   }

   private static int rainbowColor(int offset) {
      float hue = (rainbowHue + (float)offset * HUE_OFFSET_PER_DEGREE) % 1.0F;
      return Color.HSBtoRGB(hue, 1.0F, 1.0F) | 0xFF000000;
   }

   public static int healthColor(float healthRatio) {
      if ((double)healthRatio < 0.3) return 0xFFFF5555;
      if ((double)healthRatio < 0.5) return 0xFFFFAA00;
      if ((double)healthRatio < 0.7) return 0xFFFFFF55;
      return 0xFF55FF55;
   }

   public static int colorAtOffset(int offset) {
      if (rainbow) {
         return rainbowColor(offset);
      } else {
         double fraction = (double)(Mth.sin(gradientPhase + (double)offset) + 1.0F) * 0.5;
         int alpha = (int)((double)firstAlpha + (double)(secondAlpha - firstAlpha) * fraction);
         int red = (int)((double)firstRed + (double)(secondRed - firstRed) * fraction);
         int green = (int)((double)firstGreen + (double)(secondGreen - firstGreen) * fraction);
         int blue = (int)((double)firstBlue + (double)(secondBlue - firstBlue) * fraction);
         return alpha << 24 | red << 16 | green << 8 | blue;
      }
   }
}
