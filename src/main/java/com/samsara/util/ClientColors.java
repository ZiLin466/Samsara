package com.samsara.util;

import com.samsara.module.FeatureManager;
import java.awt.Color;
import java.nio.charset.StandardCharsets;
import net.minecraft.util.Mth;

public class ClientColors {
   private static int f781;
   private static final String f772 = "Sakura";
   private static int f784;
   private static final String f771 = "Inferno";
   private static final String f770 = "Emerald";
   private static long f789;
   private static String f788;
   private static boolean f792;
   private static int f787;
   private static final float f775 = 0.0027777778F;
   private static final double f776 = 1.6666666666666666E-4;
   private static final double f777 = 0.001;
   private static float f791;
   private static final String f774 = "Flower";
   private static final String f773 = "Cherry";
   private static int f782;
   private static int f778 = -1;
   private static final String f767 = "Gold";
   private static final String f769 = "Nova";
   private static int f780;
   private static int f779 = -1;
   private static double f790;
   private static int f786;
   private static final String f766 = "Rainbow";
   private static int f783;
   private static final String f768 = "Ocean";
   private static int f785;

   public static void m51() {
      String var0 = FeatureManager.f24.f34.m224();
      double var1 = FeatureManager.f24.f35.m220();
      f789 = System.currentTimeMillis();
      f791 = (float)((double)f789 * var1 % 6000.0 * 1.6666666666666666E-4);
      f790 = (double)f789 * 0.001 * var1;
      f792 = f766.equals(var0);
      if (!var0.equals(f788)) {
         f788 = var0;
         byte var4 = -1;
         int var10000 = var0.hashCode();
         if (var10000 == -1825862521) {
            if (var0.equals(f772)) {
               var4 = 5;
            }
         } else if (var10000 == -685180305) {
            if (var0.equals(f771)) {
               var4 = 4;
            }
         } else if (var10000 == 2225280) {
            if (var0.equals(f767)) {
               var4 = 0;
            }
         } else if (var10000 == 2434124) {
            if (var0.equals(f769)) {
               var4 = 2;
            }
         } else if (var10000 == 30590468) {
            if (var0.equals(f770)) {
               var4 = 3;
            }
         } else if (var10000 == 76007646) {
            if (var0.equals(f768)) {
               var4 = 1;
            }
         } else if (var10000 == 2017321401) {
            if (var0.equals(f773)) {
               var4 = 6;
            }
         } else if (var10000 == 2107205243) {
            if (var0.equals(f774)) {
               var4 = 7;
            }
         }

         switch (var4) {
            case 0:
               f778 = -23296;
               f779 = -256;
               break;
            case 1:
               f778 = -16726273;
               f779 = -16747777;
               break;
            case 2:
               f778 = -39736;
               f779 = -15100161;
               break;
            case 3:
               f778 = -16718218;
               f779 = -15315669;
               break;
            case 4:
               f778 = -43230;
               f779 = -8768993;
               break;
            case 5:
               f778 = -32597;
               f779 = -2080517;
               break;
            case 6:
               f778 = -2278039;
               f779 = -2051145;
               break;
            case 7:
               f778 = -3630376;
               f779 = -5482055;
            default:
               f778 = -1;
               f779 = -1;
         }

         f780 = f778 >>> 24;
         f781 = f778 >> 16 & 0xFF;
         f782 = f778 >> 8 & 0xFF;
         f783 = f778 & 0xFF;
         f784 = f779 >>> 24;
         f785 = f779 >> 16 & 0xFF;
         f786 = f779 >> 8 & 0xFF;
         f787 = f779 & 0xFF;
      }
   }

   private static int pm$113(int var0) {
      float var1 = (f791 + (float)var0 * 0.0027777778F) % 1.0F;
      return Color.HSBtoRGB(var1, 1.0F, 1.0F) | 0xFF000000;
   }

   public static int m52(int var0) {
      if (f792) {
         return pm$113(var0);
      } else {
         double var1 = (double)(Mth.sin(f790 + (double)var0) + 1.0F) * 0.5;
         int var3 = (int)((double)f780 + (double)(f784 - f780) * var1);
         int var4 = (int)((double)f781 + (double)(f785 - f781) * var1);
         int var5 = (int)((double)f782 + (double)(f786 - f782) * var1);
         int var6 = (int)((double)f783 + (double)(f787 - f783) * var1);
         return var3 << 24 | var4 << 16 | var5 << 8 | var6;
      }
   }
}
