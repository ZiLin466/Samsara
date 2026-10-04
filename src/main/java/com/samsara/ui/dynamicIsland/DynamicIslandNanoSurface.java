package com.samsara.ui.dynamicIsland;

import java.nio.FloatBuffer;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGGlyphPosition;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.system.MemoryUtil.memAddress;

public final class DynamicIslandNanoSurface implements DynamicIslandPainter.Surface {
   public static final String FONT = "sourcesans3-semibold";
   public static final String IDLE_FONT = "googlesans-bold";
   private final long vg;
   private final int font;
   private final int idleFont;
   private final NVGColor ink = NVGColor.create();

   public DynamicIslandNanoSurface(long vg, int font, int idleFont) {
      this.vg = vg;
      this.font = font;
      this.idleFont = idleFont;
   }

   private void color(int argb) {
      this.ink.r(((argb >>> 16) & 255) / 255f).g(((argb >>> 8) & 255) / 255f)
         .b((argb & 255) / 255f).a((argb >>> 24) / 255f);
   }

   private void font(float size, boolean breaking) {
      nvgFontFaceId(this.vg, breaking || size == DynamicIslandStatus.FONT_SIZE ? this.idleFont : this.font);
      nvgFontSize(this.vg, size);
      nvgTextLetterSpacing(this.vg, 0);
      nvgTextAlign(this.vg, NVG_ALIGN_LEFT | NVG_ALIGN_BASELINE);
   }

   public static float letterSpacing(float size) {
      return size == DynamicIslandStatus.FONT_SIZE ? 0 : size == DynamicIslandState.DETAIL_FONT ? .125f : .14f;
   }

   @Override
   public void symbol(DynamicIslandStatus.Symbol symbol, float x, float y, float size, int argb) {
      nvgSave(this.vg);
      try {
         nvgTranslate(this.vg, x, y);
         nvgScale(this.vg, size / 24, size / 24);
         color(argb);
         nvgBeginPath(this.vg);
         switch (symbol) {
            case BED -> {
               // A single filled frame avoids the seams of overlapping rounded rectangles.
               nvgMoveTo(this.vg, 0, .75f);
               nvgBezierTo(this.vg, 0, .336f, .336f, 0, .75f, 0);
               nvgLineTo(this.vg, 2.25f, 0);
               nvgBezierTo(this.vg, 2.664f, 0, 3, .336f, 3, .75f);
               nvgLineTo(this.vg, 3, 9);
               nvgLineTo(this.vg, 24, 9);
               nvgLineTo(this.vg, 24, 15);
               nvgLineTo(this.vg, 21, 15);
               nvgLineTo(this.vg, 21, 12);
               nvgLineTo(this.vg, 3, 12);
               nvgLineTo(this.vg, 3, 15);
               nvgLineTo(this.vg, 0, 15);
               nvgClosePath(this.vg);
               nvgCircle(this.vg, 7.05f, 5.15f, 3.1f);
               nvgMoveTo(this.vg, 10.8f, 3);
               nvgLineTo(this.vg, 19.2f, 3);
               nvgBezierTo(this.vg, 21.85f, 3, 24, 4.6f, 24, 6.6f);
               nvgLineTo(this.vg, 24, 8.4f);
               nvgLineTo(this.vg, 10.8f, 8.4f);
               nvgClosePath(this.vg);
            }
            case USER -> {
               nvgCircle(this.vg, 12, 7.5f, 5);
               nvgMoveTo(this.vg, 3.5f, 21);
               nvgBezierTo(this.vg, 3.5f, 17, 6.5f, 14.5f, 10, 14.5f);
               nvgLineTo(this.vg, 14, 14.5f);
               nvgBezierTo(this.vg, 17.5f, 14.5f, 20.5f, 17, 20.5f, 21);
               nvgQuadTo(this.vg, 20.5f, 22, 19.5f, 22);
               nvgLineTo(this.vg, 4.5f, 22);
               nvgQuadTo(this.vg, 3.5f, 22, 3.5f, 21);
               nvgClosePath(this.vg);
            }
            case LINK -> {
               nvgMoveTo(this.vg, 8, 16.5f);
               nvgLineTo(this.vg, 6.5f, 16.5f);
               nvgBezierTo(this.vg, 3.8f, 16.5f, 2.25f, 15, 2.25f, 12);
               nvgBezierTo(this.vg, 2.25f, 9, 3.8f, 7.5f, 6.5f, 7.5f);
               nvgLineTo(this.vg, 10, 7.5f);
               nvgBezierTo(this.vg, 12.8f, 7.5f, 14, 9, 14, 12);
               nvgLineTo(this.vg, 14, 13.25f);
               nvgMoveTo(this.vg, 16, 7.5f);
               nvgLineTo(this.vg, 17.5f, 7.5f);
               nvgBezierTo(this.vg, 20.2f, 7.5f, 21.75f, 9, 21.75f, 12);
               nvgBezierTo(this.vg, 21.75f, 15, 20.2f, 16.5f, 17.5f, 16.5f);
               nvgLineTo(this.vg, 14, 16.5f);
               nvgBezierTo(this.vg, 11.2f, 16.5f, 10, 15, 10, 12);
               nvgLineTo(this.vg, 10, 10.75f);
               nvgStrokeColor(this.vg, this.ink);
               nvgStrokeWidth(this.vg, 2.5f);
               nvgLineCap(this.vg, NVG_ROUND);
               nvgLineJoin(this.vg, NVG_ROUND);
               nvgStroke(this.vg);
               return;
            }
            case REFRESH -> {
               for (int i = 0; i < 2; i++) {
                  nvgSave(this.vg);
                  if (i == 1) {
                     nvgTranslate(this.vg, 24, 24);
                     nvgRotate(this.vg, (float)Math.PI);
                  }
                  nvgBeginPath(this.vg);
                  nvgMoveTo(this.vg, 12.5f, 1);
                  nvgLineTo(this.vg, 4.5f, 6.5f);
                  nvgLineTo(this.vg, 12.5f, 12);
                  nvgQuadTo(this.vg, 13, 12.25f, 13, 11.5f);
                  nvgLineTo(this.vg, 13, 9);
                  nvgBezierTo(this.vg, 17.5f, 9, 20.5f, 11.5f, 21.5f, 15.5f);
                  nvgBezierTo(this.vg, 21.5f, 8.5f, 18.5f, 5.5f, 13, 5);
                  nvgLineTo(this.vg, 13, 1.5f);
                  nvgQuadTo(this.vg, 13, .75f, 12.5f, 1);
                  nvgClosePath(this.vg);
                  nvgFillColor(this.vg, this.ink);
                  nvgFill(this.vg);
                  nvgRestore(this.vg);
               }
               return;
            }
            case CHROME -> {
               // Keep the three lobes separate where parallel cuts meet the center clearance.
               float outerRadius = 12, innerRadius = 6, cutDistance = 5, gap = 1.75f;
               float upperCut = cutDistance + gap / 2, lowerCut = cutDistance - gap / 2;
               for (int i = 0; i < 3; i++) {
                  float rotation = (float)(i * Math.PI * 2 / 3);
                  float outerStart = rotation - (float)(Math.PI * 2 / 3) - (float)Math.asin(lowerCut / outerRadius);
                  float outerEnd = rotation - (float)Math.asin(upperCut / outerRadius);
                  float innerEnd = rotation - (float)Math.asin(upperCut / innerRadius);
                  float innerStart = rotation - (float)(Math.PI * 2 / 3) - (float)Math.asin(lowerCut / innerRadius);
                  nvgBeginPath(this.vg);
                  nvgArc(this.vg, 12, 12, outerRadius, outerStart, outerEnd, NVG_CW);
                  nvgLineTo(this.vg, 12 + innerRadius * (float)Math.cos(innerEnd), 12 + innerRadius * (float)Math.sin(innerEnd));
                  nvgArc(this.vg, 12, 12, innerRadius, innerEnd, innerStart, NVG_CCW);
                  nvgClosePath(this.vg);
                  nvgFillColor(this.vg, this.ink);
                  nvgFill(this.vg);
               }
               nvgBeginPath(this.vg);
               nvgCircle(this.vg, 12, 12, 4);
            }
         }
         nvgFillColor(this.vg, this.ink);
         nvgFill(this.vg);
      } finally {
         nvgRestore(this.vg);
      }
   }

   @Override
   public float measure(String text, float size) {
      return measure(text, size, false);
   }

   @Override
   public float measureBreaking(String text, float size) {
      return measure(text, size, true);
   }

   private float measure(String text, float size, boolean breaking) {
      font(size, breaking);
      // Include the trailing tracking so separately colored segments retain the same spacing.
      return nvgTextBounds(this.vg, 0, 0, text, (FloatBuffer)null)
         + text.codePointCount(0, text.length()) * letterSpacing(size);
   }

   @Override
   public void rounded(float x, float y, float w, float h, float radius, int argb) {
      color(argb);
      nvgBeginPath(this.vg);
      nvgRoundedRect(this.vg, x, y, w, h, radius);
      nvgFillColor(this.vg, this.ink);
      nvgFill(this.vg);
   }

   @Override
   public void line(float x1, float y1, float x2, float y2, float stroke, int argb) {
      stroke(stroke, argb);
      nvgBeginPath(this.vg);
      nvgMoveTo(this.vg, x1, y1);
      nvgLineTo(this.vg, x2, y2);
      nvgStroke(this.vg);
   }

   private void stroke(float width, int argb) {
      color(argb);
      nvgStrokeColor(this.vg, this.ink);
      nvgStrokeWidth(this.vg, width);
      nvgLineCap(this.vg, NVG_ROUND);
   }

   @Override
   public void text(String text, float x, float centerY, float size, int argb) {
      text(text, x, centerY, size, argb, false);
   }

   @Override
   public void breakingText(String text, float x, float centerY, float size, int argb) {
      text(text, x, centerY, size, argb, true);
   }

   private void text(String text, float x, float centerY, float size, int argb, boolean breaking) {
      if (text.isEmpty()) return;
      font(size, breaking);
      color(argb);
      nvgFillColor(this.vg, this.ink);
      try (MemoryStack stack = MemoryStack.stackPush()) {
         FloatBuffer bounds = stack.mallocFloat(4);
         nvgTextBounds(this.vg, 0, 0, text, bounds);
         float baseline = centerY - (bounds.get(1) + bounds.get(3)) / 2;
         // Fontstash rounds native tracking; apply fractional tracking after kerning.
         var utf8 = stack.UTF8(text, false);
         var glyphs = NVGGlyphPosition.malloc(text.codePointCount(0, text.length()), stack);
         int count = nvgTextGlyphPositions(this.vg, 0, 0, utf8, glyphs);
         long start = memAddress(utf8);
         long end = start + utf8.remaining();
         for (int i = 0; i < count; i++) {
            long next = i + 1 < count ? glyphs.get(i + 1).str() : end;
            nnvgText(this.vg, x + glyphs.get(i).x() + i * letterSpacing(size), baseline,
               glyphs.get(i).str(), next);
         }
      }
   }

   @Override
   public void shadow(float x, float y, float w, float h, float radius) {
      for (float spread = DynamicIslandPainter.SHADOW_EXTENT; spread > 0; spread -= DynamicIslandPainter.SHADOW_STEP) {
         int layer = DynamicIslandPainter.shadowLayer(spread);
         if (layer == 0) continue;
         color(layer);
         nvgBeginPath(this.vg);
         nvgRoundedRect(this.vg, x - spread, y - spread, w + 2 * spread, h + 2 * spread, radius + spread);
         nvgRoundedRect(this.vg, x, y, w, h, radius);
         nvgPathWinding(this.vg, NVG_HOLE);
         nvgFillColor(this.vg, this.ink);
         nvgFill(this.vg);
      }
   }

   @Override
   public void clip(float x, float y, float w, float h, Runnable content) {
      if (w <= 0 || h <= 0) return;
      nvgSave(this.vg);
      try {
         nvgIntersectScissor(this.vg, x, y, w, h);
         content.run();
      } finally {
         nvgRestore(this.vg);
      }
   }
}
