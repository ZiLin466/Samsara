package com.samsara.ui.dynamicIsland;

import com.samsara.ui.clickgui.opai.OpaiStyle;
import com.samsara.ui.clickgui.opai.OpaiStyle.Palette;
import com.samsara.util.render.HudGlassStyle;
import static com.samsara.ui.dynamicIsland.DynamicIslandState.*;

public final class DynamicIslandPainter {
   public static final int BACKGROUND = HudGlassStyle.BODY;
   public static final int TEXT = 0xFFFFFFFF;
   public static final int DETAIL = 0xFFFFFFFF;
   public static final int ENABLED = 0xFF55FF55;
   public static final int DISABLED = 0xFFFF5555;
   public static final float SHADOW_EXTENT = 8;
   public static final float SHADOW_STEP = .25f;

   public interface Surface extends TextWidth {
      void rounded(float x, float y, float width, float height, float radius, int color);
      void line(float x1, float y1, float x2, float y2, float stroke, int color);
      void text(String text, float x, float centerY, float size, int color);
      default void breakingText(String text, float x, float centerY, float size, int color) {
         text(text, x, centerY, size, color);
      }
      void shadow(float x, float y, float width, float height, float radius);
      void clip(float x, float y, float width, float height, Runnable content);
      void symbol(DynamicIslandStatus.Symbol symbol, float x, float y, float size, int color);
   }

   private DynamicIslandPainter() { }

   public static void paint(Surface surface, Frame frame, float viewportWidth) {
      paint(surface, frame, viewportWidth, OpaiStyle.LAVENDER);
   }

   public static void paint(Surface surface, Frame frame, float viewportWidth, Palette palette) {
      paintShell(surface, frame, viewportWidth);
      paintContent(surface, frame, viewportWidth, palette);
   }

   public static void paintShell(Surface surface, Frame frame, float viewportWidth) {
      float x = (viewportWidth - frame.width()) / 2;
      float radius = DynamicIslandState.radius(frame);
      float top = DynamicIslandState.top(frame);
      surface.shadow(x, top, frame.width(), frame.height(), radius);
      surface.rounded(x, top, frame.width(), frame.height(), radius, BACKGROUND);
   }

   public static void paintContent(Surface surface, Frame frame, float viewportWidth, Palette palette) {
      float x = (viewportWidth - frame.width()) / 2;
      float top = DynamicIslandState.top(frame);
      surface.clip(x + 2, top + 1, Math.max(0, frame.width() - 4), frame.height() - 2, () -> {
         if (frame.idleOpacity() > 0) {
            drawIdle(surface, frame, x, palette);
         }
         for (Row row : frame.rows()) {
            if (row.opacity() <= 0 || row.height() <= 0) {
               continue;
            }
            float y = top + row.y();
            surface.clip(x + 3, y, Math.max(0, frame.width() - 6), row.height(),
               () -> drawRow(surface, row, x, y, frame.width(), Math.max(frame.width(), frame.contentWidth()), palette));
         }
      });
   }

   private static void drawIdle(Surface surface, Frame frame, float x, Palette palette) {
      float centerY = DynamicIslandState.top(frame) + IDLE_HEIGHT / 2;
      // The layout is based on final content width, so FPS changes only animate the clip.
      DynamicIslandStatus.Symbol previousIcon = null;
      for (var part : frame.idle().parts()) {
         int color = alpha(part.icon() == DynamicIslandStatus.Symbol.CHROME || previousIcon == DynamicIslandStatus.Symbol.CHROME
            ? palette.accent() : part.color(), frame.idleOpacity());
         previousIcon = part.icon();
         if (part.icon() == null) {
            surface.text(part.text(), x + part.x(), centerY, DynamicIslandStatus.FONT_SIZE, color);
         } else {
            surface.symbol(part.icon(), x + part.x(), centerY - part.width() / 2, part.width(), color);
         }
      }
   }

   private static void drawRow(Surface surface, Row row, float x, float y, float width, float contentWidth, Palette palette) {
      float opacity = row.opacity();
      if (row.icon() == Icon.SCAFFOLD || row.icon() == Icon.BREAKING) {
         drawProgressPanel(surface, row, x, y, width, contentWidth, opacity, palette);
         return;
      }
      drawIcon(surface, row, x, y, palette);
      float textX = x + NOTICE_TEXT_X;
      // Reveal a stable line through the animated clip; never re-ellipsize "has been" while growing.
      float textWidth = Math.max(0, contentWidth - NOTICE_TEXT_X - NOTICE_RIGHT_PADDING);
      surface.text(fit(row.title(), TITLE_FONT, textWidth, surface), textX, y + 13.5f,
         TITLE_FONT, alpha(TEXT, opacity));
      float punctuationWidth = row.status().isEmpty() ? 0 : surface.measure("!", DETAIL_FONT);
      DetailLine line = detailLine(row.detail(), row.status(), Math.max(0, textWidth - punctuationWidth), surface);
      surface.text(line.prefix(), textX, y + 24.75f, DETAIL_FONT, alpha(DETAIL, opacity));
      // Drawing returns no width: always measure the prefix once, independent of screen X.
      surface.text(line.status(), textX + line.statusOffset(), y + 24.75f, DETAIL_FONT,
         alpha(row.enabled() ? ENABLED : DISABLED, opacity));
      if (!line.status().isEmpty()) {
         surface.text("!", textX + line.statusOffset() + surface.measure(line.status(), DETAIL_FONT),
            y + 24.75f, DETAIL_FONT, alpha(TEXT, opacity));
      }
   }

   private static void drawProgressPanel(Surface surface, Row row, float x, float y, float width, float contentWidth,
                                    float opacity, Palette palette) {
      boolean breaking = row.icon() == Icon.BREAKING;
      int tile = alpha(0xC8141616, opacity);
      int ink = alpha(palette.accent(), opacity);
      float tileSize = breaking ? 27 : 28;
      surface.rounded(x + 4, y + 5, tileSize, tileSize, 7, tile);
      if (breaking) {
         surface.symbol(DynamicIslandStatus.Symbol.BED, x + 10, y + 13.5f, 16, ink);
      } else {
         drawCube(surface, x + 18, y + 19, 14, 1, ink);
      }
      float textWidth = Math.max(0, contentWidth - SCAFFOLD_TEXT_X - SCAFFOLD_RIGHT_PADDING);
      if (breaking) {
         TextWidth measure = surface::measureBreaking;
         surface.breakingText(fit(row.title(), BREAKING_TITLE_FONT, textWidth, measure), x + SCAFFOLD_TEXT_X,
            y + 12, BREAKING_TITLE_FONT, ink);
         surface.breakingText(fit(row.detail(), BREAKING_DETAIL_FONT, textWidth, measure), x + SCAFFOLD_TEXT_X,
            y + 24, BREAKING_DETAIL_FONT, alpha(TEXT, opacity));
      } else {
         surface.text(fit(row.title(), TITLE_FONT, textWidth, surface), x + SCAFFOLD_TEXT_X, y + 13,
            TITLE_FONT, ink);
         surface.text(fit(row.detail(), DETAIL_FONT, textWidth, surface), x + SCAFFOLD_TEXT_X, y + 25,
            DETAIL_FONT, alpha(TEXT, opacity));
      }
      float barWidth = Math.max(0, width - 8);
      float barY = y + (breaking ? 35 : 37);
      float barHeight = breaking ? 7.5f : 8;
      surface.rounded(x + 4, barY, barWidth, barHeight, barHeight / 2,
         alpha(breaking ? 0x7034343D : 0xFF343636, opacity));
      float filledWidth = barWidth * Math.clamp(row.progress(), 0, 1);
      if (filledWidth > 0) {
         surface.rounded(x + 4, barY, filledWidth, barHeight, Math.min(barHeight / 2, filledWidth / 2), alpha(palette.hudProgress(), opacity));
      }
   }

   private static void drawCube(Surface surface, float cx, float cy, float size, float stroke, int color) {
      float half = size * 0.5f;
      float top = cy - half;
      float bottom = cy + half;
      float left = cx - half * 0.9f;
      float right = cx + half * 0.9f;
      float shoulder = cy - half * 0.5f;
      float lower = cy + half * 0.5f;
      surface.line(cx, top, right, shoulder, stroke, color);
      surface.line(right, shoulder, cx, cy, stroke, color);
      surface.line(cx, cy, left, shoulder, stroke, color);
      surface.line(left, shoulder, cx, top, stroke, color);
      surface.line(left, shoulder, left, lower, stroke, color);
      surface.line(left, lower, cx, bottom, stroke, color);
      surface.line(cx, bottom, right, lower, stroke, color);
      surface.line(right, lower, right, shoulder, stroke, color);
      surface.line(cx, cy, cx, bottom, stroke, color);
   }

   private static void drawIcon(Surface surface, Row row, float x, float y, Palette palette) {
      float a = row.opacity();
      if (row.icon() == Icon.TOGGLE) {
         float t = row.toggle();
         surface.rounded(x + 5, y + 10, 26, 16, 8, alpha(mix(palette.toggleOutline(), palette.accent(), t), a));
         surface.rounded(x + 6, y + 11, 24, 14, 7, alpha(mix(0xFF36343B, palette.accent(), t), a));
         float knobSize = 8 + 3.5f * t;
         surface.rounded(x + 8.5f + 8.5f * t, y + 13.5f - 1.25f * t,
            knobSize, knobSize, knobSize / 2, alpha(mix(0xFF858488, palette.hudKnob(), t), a));
         return;
      }
      int tile = switch (row.icon()) {
         case SUCCESS -> 0xC85F8F50;
         case WARNING -> 0xC88F5050;
         default -> 0xC8307593;
      };
      surface.rounded(x + 7, y + 8, 24, 24, 7, alpha(tile, a));
      int ink = alpha(TEXT, a);
      if (row.icon() == Icon.SUCCESS) {
         surface.line(x + 13, y + 20, x + 18, y + 25, 2, ink);
         surface.line(x + 18, y + 25, x + 26, y + 14, 2, ink);
      } else if (row.icon() == Icon.WARNING) {
         surface.line(x + 19, y + 13, x + 19, y + 22, 2, ink);
         surface.rounded(x + 18, y + 25, 2, 2, 1, ink);
      } else {
         surface.line(x + 19, y + 16, x + 19, y + 26, 2, ink);
         surface.rounded(x + 18, y + 12, 2, 2, 1, ink);
      }
   }

   private static int alpha(int color, float opacity) {
      return (Math.round((color >>> 24) * Math.clamp(opacity, 0, 1)) << 24) | (color & 0xFFFFFF);
   }

   public static float shadowCoverage(float distance) {
      float normalized = Math.max(0, distance) / 2.8f;
      return .5f * (float)Math.exp(-.78f * normalized - .5f * normalized * normalized);
   }

   public static int shadowLayer(float spread) {
      float previous = shadowCoverage(spread + SHADOW_STEP);
      float desired = shadowCoverage(spread);
      return Math.round(255 * (desired - previous) / (1 - previous)) << 24;
   }

   private static int mix(int from, int to, float amount) {
      int result = 0;
      for (int shift = 0; shift <= 24; shift += 8) {
         int a = (from >>> shift) & 255;
         int b = (to >>> shift) & 255;
         result |= Math.round(a + (b - a) * amount) << shift;
      }
      return result;
   }
}
