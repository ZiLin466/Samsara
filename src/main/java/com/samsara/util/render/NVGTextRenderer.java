package com.samsara.util.render;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.nanovg.NanoVG.*;

public final class NVGTextRenderer {

   private static final int DEFAULT_ALIGNMENT = NVG_ALIGN_CENTER | NVG_ALIGN_LEFT;
   private static final char COLOR_INVOKER = '§';

   private final String name;
   private final ByteBuffer fontData;
   private final int fontId;

   public NVGTextRenderer(String name, InputStream inputStream) {
      this.name = name;
      this.fontData = IOUtility.ioResourceToByteBuffer(inputStream, 512 * 1024);
      this.fontId = this.fontData == null ? -1 : nvgCreateFontMem(NVGRenderer.getContext(), name, this.fontData, false);
   }

   public int getFontId() {
      return this.fontId;
   }

   public int getDataSize() {
      return this.fontData == null ? -1 : this.fontData.remaining();
   }

   public float drawStringWithShadow(String text, float x, float y, float size, int color) {
      drawString(text, x + 0.5F, y + 0.5F, size, color, true, DEFAULT_ALIGNMENT);
      return drawString(text, x, y, size, color, false, DEFAULT_ALIGNMENT);
   }

   public float drawString(String text, float x, float y, float size, int color) {
      return drawString(text, x, y, size, color, false, DEFAULT_ALIGNMENT);
   }

   public float drawString(String text, float x, float y, float size, int color, boolean shadow, int alignment) {
      long vg = NVGRenderer.getContext();
      nvgBeginPath(vg);
      nvgFontFaceId(vg, this.fontId);
      nvgFontSize(vg, size);
      nvgTextAlign(vg, alignment);

      NVGRenderer.applyColor(shadow ? ColorUtility.getShadowColor(color) : color, NVGRenderer.NVG_COLOR_1);
      nvgFillColor(vg, NVGRenderer.NVG_COLOR_1);

      StringBuilder segment = new StringBuilder();
      for (int i = 0; i < text.length(); i++) {
         char character = text.charAt(i);
         if (character == COLOR_INVOKER && i + 1 < text.length()) {
            if (segment.length() > 0) {
               drawSegment(segment.toString(), x, y);
               x += nvgTextBounds(vg, 0, 0, segment.toString(), (FloatBuffer)null);
               segment.setLength(0);
            }

            int colorIndex = colorIndex(Character.toLowerCase(text.charAt(++i)));
            if (colorIndex >= 0 && colorIndex < 16) {
               int codeColor = COLORS[colorIndex];
               if (shadow) {
                  codeColor = ColorUtility.getShadowColor(codeColor);
               }
               NVGRenderer.applyColor(ColorUtility.applyOpacity(codeColor, color >> 24 & 0xFF), NVGRenderer.NVG_COLOR_2);
               nvgFillColor(vg, NVGRenderer.NVG_COLOR_2);
            } else if (colorIndex == 21) {
               NVGRenderer.applyColor(shadow ? ColorUtility.getShadowColor(color) : color, NVGRenderer.NVG_COLOR_1);
               nvgFillColor(vg, NVGRenderer.NVG_COLOR_1);
            }
         } else {
            segment.append(character);
         }
      }

      if (segment.length() > 0) {
         drawSegment(segment.toString(), x, y);
      }

      nvgClosePath(vg);
      return x;
   }

   private void drawSegment(String text, float x, float y) {
      nvgText(NVGRenderer.getContext(), x, y, text);
   }

   public float getStringWidth(String text, float size) {
      long vg = NVGRenderer.getContext();
      nvgFontFaceId(vg, this.fontId);
      nvgFontSize(vg, size);

      if (text.indexOf(COLOR_INVOKER) < 0) {
         return nvgTextBounds(vg, 0, 0, text, (FloatBuffer)null);
      }
      StringBuilder segment = new StringBuilder();
      float width = 0F;
      for (int i = 0; i < text.length(); i++) {
         char character = text.charAt(i);
         if (character == COLOR_INVOKER && i + 1 < text.length()) {
            i++;
         } else {
            segment.append(character);
         }

         if ((character == COLOR_INVOKER && i < text.length() - 1) || i == text.length() - 1) {
            if (segment.length() > 0) {
               width += nvgTextBounds(vg, 0, 0, segment.toString(), (FloatBuffer)null);
               segment.setLength(0);
            }
         }
      }
      return width;
   }

   public float getStringHeight(String text, float size) {
      long vg = NVGRenderer.getContext();
      nvgFontFaceId(vg, this.fontId);
      nvgFontSize(vg, size);
      try (MemoryStack stack = MemoryStack.stackPush()) {
         FloatBuffer bounds = stack.mallocFloat(4);
         nvgTextBounds(vg, 0, 0, text, bounds);
         return bounds.get(3) - bounds.get(1);
      }
   }

   private static int colorIndex(char character) {
      return character < 128 ? CHAR_TO_INDEX[character] : -1;
   }

   private static final int[] COLORS = new int[32];
   private static final byte[] CHAR_TO_INDEX = new byte[128];

   private static void initColors() {
      for (int i = 0; i < 32; i++) {
         int amplifier = (i >> 3 & 1) * 85;
         int red = (i >> 2 & 1) * 170 + amplifier;
         int green = (i >> 1 & 1) * 170 + amplifier;
         int blue = (i & 1) * 170 + amplifier;
         if (i == 6) {
            red += 85;
         }
         if (i >= 16) {
            red /= 4;
            green /= 4;
            blue /= 4;
         }
         COLORS[i] = (red & 255) << 16 | (green & 255) << 8 | blue & 255;
      }

      String codes = "0123456789abcdefklmnor";
      for (int i = 0; i < 128; i++) {
         CHAR_TO_INDEX[i] = -1;
      }
      for (int i = 0; i < codes.length(); i++) {
         char character = codes.charAt(i);
         CHAR_TO_INDEX[character] = (byte)i;
         CHAR_TO_INDEX[Character.toLowerCase(character)] = (byte)i;
      }
   }

   static {
      initColors();
   }
}
