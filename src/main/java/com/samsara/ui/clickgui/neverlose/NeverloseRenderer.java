package com.samsara.ui.clickgui.neverlose;

import com.samsara.ui.clickgui.neverlose.NeverloseLayout.Rect;
import com.samsara.ui.clickgui.neverlose.NeverloseLayout.Viewport;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.Map;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.system.MemoryStack;

import static com.samsara.ui.clickgui.neverlose.NeverloseLayout.*;
import static org.lwjgl.nanovg.NanoVG.*;

public final class NeverloseRenderer {
   public static final int ACCENT = 0xFF3A85E2, TEXT = 0xFFBDC3C7, WHITE = 0xFFEDF3F5;
   public static final String REGULAR_FONT = "rockstar-regular", MEDIUM_FONT = "rockstar-medium", BOLD_FONT = "rockstar-bold";

   public enum Kind { ENABLED, BOOLEAN, NUMBER, CHOICE }
   public record Editor(String value, int cursor, int selection, boolean focused) { }
   public record Row(Kind kind, Rect bounds, String label, String value, float progress, float feedback, boolean hovered) { }
   public record Section(Rect bounds, String title, String binding, float reveal, List<Row> rows) { }
   public record Config(Rect bounds, String name, String detail, boolean active, boolean confirmingDelete) { }
   public record Option(String label, boolean selected, boolean highlighted) { }
   public record Dropdown(Rect bounds, float reveal, float scroll, List<Option> options) { }
   public record Frame(Viewport viewport, float opacity, float openingScale, int navigation, float selectionY,
      String username, String version, Editor search, String preset, List<Section> sections, List<Config> configs,
      Editor configName, Dropdown dropdown, float scroll, float maximumScroll, float pageReveal,
      String status, boolean error, float mouseX, float mouseY, Map<String, Float> buttonFeedback) { }

   private NeverloseRenderer() { }

   public static float textWidth(long vg, int font, String text, float size) {
      nvgFontFaceId(vg, font); nvgFontSize(vg, size); nvgTextLetterSpacing(vg, 0);
      return nvgTextBounds(vg, 0, 0, text, (FloatBuffer)null);
   }

   public static float editorOffset(long vg, int font, Editor editor, Rect box, float padding) {
      if (!editor.focused) return 0;
      return Math.max(0, textWidth(vg, font, editor.value.substring(0, editor.cursor), 12) - box.width() + padding + 18);
   }

   public static void paint(long vg, int regular, int medium, int bold, Frame frame) {
      nvgSave(vg);
      try (MemoryStack stack = MemoryStack.stackPush()) {
         Surface surface = new Surface(vg, regular, medium, bold, NVGColor.malloc(stack), NVGColor.malloc(stack), NVGPaint.malloc(stack), frame.buttonFeedback);
         Viewport viewport = frame.viewport;
         nvgTranslate(vg, viewport.x() + WIDTH * viewport.scale() / 2, viewport.y() + HEIGHT * viewport.scale() / 2);
         nvgScale(vg, viewport.scale() * frame.openingScale, viewport.scale() * frame.openingScale);
         nvgTranslate(vg, -WIDTH / 2, -HEIGHT / 2);
         nvgGlobalAlpha(vg, frame.opacity);
         surface.shadow(WINDOW, 5);
         nvgSave(vg);
         try {
            nvgIntersectScissor(vg, 0, 0, SIDEBAR, HEIGHT);
            surface.gradient(WINDOW, RADIUS, 0xD2090A0A, 0xCC0D0E0C);
         } finally { nvgRestore(vg); }
         nvgSave(vg);
         try {
            nvgIntersectScissor(vg, SIDEBAR, 0, WIDTH - SIDEBAR, HEIGHT);
            surface.rounded(WINDOW, RADIUS, 0xFF080808);
         } finally { nvgRestore(vg); }
         surface.rounded(new Rect(SIDEBAR, 0, 1, HEIGHT), 0, 0xFF242525);
         surface.rounded(new Rect(SIDEBAR, HEADER, WIDTH - SIDEBAR, 1), 0, 0xFF1C1D1D);
         surface.brand();
         surface.navigation(frame);
         surface.footer(frame);
         surface.editor(SEARCH, frame.search, "Press CTRL+F to search within GUI", 30);
         surface.icon(8, SEARCH.x() + 14, SEARCH.y() + SEARCH.height() / 2, 11, frame.search.focused ? ACCENT : 0xFF677077);
         surface.button(SAVE, "Save", 0, "save", frame.mouseX, frame.mouseY);
         surface.field(PRESET, frame.preset, PRESET.contains(frame.mouseX, frame.mouseY), false);
         nvgSave(vg);
         try {
            nvgGlobalAlpha(vg, frame.opacity * frame.pageReveal);
            nvgIntersectScissor(vg, CONTENT.x(), CONTENT.y(), CONTENT.width(), CONTENT.height());
            if (frame.navigation == 7) {
               surface.editor(CONFIG_INPUT, frame.configName, "Configuration name", 10);
               surface.button(CREATE, "Create configuration", -1, "create", frame.mouseX, frame.mouseY);
               surface.text("Saved configurations", CONTENT.x(), 135, 12, medium, WHITE, CONTENT.width());
               surface.line(CONTENT.x(), 149, CONTENT.right(), 149, 0xFF202222);
               nvgSave(vg);
               try {
                  nvgIntersectScissor(vg, CONTENT.x(), 156, CONTENT.width(), CONTENT.bottom() - 156);
                  for (Config config : frame.configs) if (config.bounds.bottom() > 156 && config.bounds.y() < CONTENT.bottom())
                     surface.config(config, frame.mouseX, frame.mouseY);
               } finally { nvgRestore(vg); }
            } else {
               for (Section section : frame.sections) if (section.bounds.intersect(CONTENT).height() > 0)
                  surface.section(section, frame.opacity * frame.pageReveal, frame.mouseX, frame.mouseY);
               if (frame.sections.isEmpty()) {
                  surface.text("No modules found", CONTENT.x() + 12, CONTENT.y() + 28, 13, medium, TEXT, 300);
               }
            }
         } finally { nvgRestore(vg); }
         if (frame.maximumScroll > 0) {
            Rect track = frame.navigation == 7 ? new Rect(CONTENT.x(), 156, CONTENT.width(), CONTENT.bottom() - 156) : CONTENT;
            surface.rounded(new Rect(748, track.y(), 3, track.height()), 1.5f, 0xFF16191B);
            surface.rounded(thumb(frame.scroll, frame.maximumScroll, track), 1.5f, 0xFF424C54);
         }
         if (!frame.status.isEmpty()) surface.text(frame.status, CONTENT.x(), HEIGHT - 9, 10, regular,
            frame.error ? 0xFFE88989 : 0xFF849CAC, CONTENT.width());
         if (frame.dropdown == null && frame.navigation != 7) {
            for (Section section : frame.sections) for (Row row : section.rows) {
               if (!row.hovered || !row.bounds.intersect(section.bounds).intersect(CONTENT).contains(frame.mouseX, frame.mouseY)) continue;
               float width = row.kind == Kind.BOOLEAN || row.kind == Kind.ENABLED ? row.bounds.width() - 40 : row.bounds.width() - 138;
               if (frame.mouseX < control(row.bounds).x() && textWidth(vg, regular, row.label, 11.5f) > width)
                  surface.tooltip(row.label, frame.mouseX, frame.mouseY);
               else if (row.kind == Kind.CHOICE && frame.mouseX >= control(row.bounds).x()
                  && textWidth(vg, regular, row.value, 11.5f) > control(row.bounds).width() - 25)
                  surface.tooltip(row.value, frame.mouseX, frame.mouseY);
            }
         }
         if (frame.dropdown != null) surface.dropdown(frame.dropdown, frame.mouseX, frame.mouseY, frame.opacity);
      } finally { nvgRestore(vg); }
   }

   private static final class Surface {
      private final long vg;
      private final int regular, medium, bold;
      private final NVGColor color, other;
      private final NVGPaint paint;
      private final Map<String, Float> feedback;

      Surface(long vg, int regular, int medium, int bold, NVGColor color, NVGColor other, NVGPaint paint, Map<String, Float> feedback) {
         this.vg = vg; this.regular = regular; this.medium = medium; this.bold = bold;
         this.color = color; this.other = other; this.paint = paint;
         this.feedback = feedback;
      }

      private NVGColor rgba(int value, NVGColor target) {
         return nvgRGBA((byte)(value >> 16), (byte)(value >> 8), (byte)value, (byte)(value >>> 24), target);
      }

      void rounded(Rect bounds, float radius, int value) {
         if (bounds.height() <= 0 || bounds.width() <= 0) return;
         nvgBeginPath(vg); nvgRoundedRect(vg, bounds.x(), bounds.y(), bounds.width(), bounds.height(), radius);
         nvgFillColor(vg, rgba(value, color)); nvgFill(vg);
      }

      void border(Rect bounds, float radius, int value) {
         nvgBeginPath(vg); nvgRoundedRect(vg, bounds.x() + .5f, bounds.y() + .5f, bounds.width() - 1, bounds.height() - 1, radius);
         nvgStrokeWidth(vg, 1); nvgStrokeColor(vg, rgba(value, color)); nvgStroke(vg);
      }

      void gradient(Rect bounds, float radius, int top, int bottom) {
         nvgLinearGradient(vg, bounds.x(), bounds.y(), bounds.x(), bounds.bottom(), rgba(top, color), rgba(bottom, other), paint);
         nvgBeginPath(vg); nvgRoundedRect(vg, bounds.x(), bounds.y(), bounds.width(), bounds.height(), radius);
         nvgFillPaint(vg, paint); nvgFill(vg);
      }

      void shadow(Rect bounds, float radius) {
         nvgBoxGradient(vg, bounds.x(), bounds.y(), bounds.width(), bounds.height(), radius, 22, rgba(0x99000000, color), rgba(0, other), paint);
         nvgBeginPath(vg); nvgRect(vg, bounds.x() - 20, bounds.y() - 20, bounds.width() + 40, bounds.height() + 40);
         nvgRoundedRect(vg, bounds.x(), bounds.y(), bounds.width(), bounds.height(), radius); nvgPathWinding(vg, NVG_HOLE);
         nvgFillPaint(vg, paint); nvgFill(vg);
      }

      void line(float x, float y, float endX, float endY, int value) {
         nvgBeginPath(vg); nvgMoveTo(vg, x, y); nvgLineTo(vg, endX, endY);
         nvgStrokeWidth(vg, 1); nvgStrokeColor(vg, rgba(value, color)); nvgStroke(vg);
      }

      void circle(float x, float y, float radius, int value) {
         nvgBeginPath(vg); nvgCircle(vg, x, y, radius); nvgFillColor(vg, rgba(value, color)); nvgFill(vg);
      }

      void text(String value, float x, float y, float size, int font, int valueColor, float width) {
         nvgSave(vg);
         try {
            nvgIntersectScissor(vg, x, y - size, Math.max(0, width), size * 2);
            nvgFontFaceId(vg, font); nvgFontSize(vg, size); nvgTextLetterSpacing(vg, 0);
            nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
            nvgFillColor(vg, rgba(valueColor, color)); nvgText(vg, x, y, value);
         } finally { nvgRestore(vg); }
      }

      void brand() {
         nvgFontFaceId(vg, bold); nvgFontSize(vg, 24); nvgTextLetterSpacing(vg, .9f);
         nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
         nvgFillColor(vg, rgba(0x225CBADD, color)); nvgText(vg, 18.5f, 35.5f, "NEVERLOSE");
         nvgFillColor(vg, rgba(0xFFF1FAFD, color)); nvgText(vg, 18, 35, "NEVERLOSE");
         nvgTextLetterSpacing(vg, 0);
      }

      void navigation(Frame frame) {
         String[] groups = {"Combat", "Player", "Visuals", "Miscellaneous"};
         float[] groupY = {79, 183, 287, 355};
         for (int i = 0; i < groups.length; i++) text(groups[i], 20, groupY[i], 10.5f, medium, 0xFF646B6D, 145);
         rounded(new Rect(10, frame.selectionY, SIDEBAR - 20, 29), 3, 0xFF3D3D3B);
         for (int i = 0; i < NAV_NAMES.length; i++) {
            Rect bounds = NeverloseLayout.navigation(i);
            if (i != frame.navigation && bounds.contains(frame.mouseX, frame.mouseY)) rounded(bounds, 3, 0x452F363C);
            icon(i, 28, bounds.y() + bounds.height() / 2, 14, ACCENT);
            text(NAV_NAMES[i], 47, bounds.y() + bounds.height() / 2, 12.5f, medium, i == frame.navigation ? WHITE : TEXT, 115);
         }
      }

      void footer(Frame frame) {
         line(0, HEIGHT - 62, SIDEBAR, HEIGHT - 62, 0xFF30322F);
         circle(30, HEIGHT - 31, 17, 0xFF151B1F);
         circle(30, HEIGHT - 31, 15.5f, 0xFF20323F);
         String initial = frame.username.isEmpty() ? "S" : frame.username.substring(0, frame.username.offsetByCodePoints(0, 1)).toUpperCase(java.util.Locale.ROOT);
         float w = textWidth(vg, bold, initial, 15);
         text(initial, 30 - w / 2, HEIGHT - 31, 15, bold, WHITE, 30);
         text(frame.username, 57, HEIGHT - 39, 12, medium, WHITE, SIDEBAR - 67);
         text("Samsara " + frame.version, 57, HEIGHT - 23, 10.5f, regular, ACCENT, SIDEBAR - 67);
      }

      void editor(Rect bounds, Editor editor, String hint, float padding) {
         rounded(bounds, 2, 0xFF0E1011); border(bounds, 2, editor.focused ? 0xFF315374 : 0xFF202325);
         nvgSave(vg);
         try {
            float left = bounds.x() + padding, right = bounds.right() - 9;
            nvgIntersectScissor(vg, left, bounds.y() + 2, right - left, bounds.height() - 4);
            float offset = editorOffset(vg, regular, editor, bounds, padding);
            float caret = textWidth(vg, regular, editor.value.substring(0, editor.cursor), 12);
            if (editor.focused && editor.selection != editor.cursor) {
               float other = textWidth(vg, regular, editor.value.substring(0, editor.selection), 12);
               rounded(new Rect(left + Math.min(caret, other) - offset, bounds.y() + 6, Math.abs(caret - other), bounds.height() - 12), 1, 0x773A85E2);
            }
            text(editor.value.isEmpty() ? hint : editor.value, left - offset, bounds.y() + bounds.height() / 2,
               12, regular, editor.value.isEmpty() ? 0xFF697277 : WHITE, bounds.width() + offset);
            if (editor.focused && System.nanoTime() / 500_000_000 % 2 == 0)
               line(left + caret - offset, bounds.y() + 7, left + caret - offset, bounds.bottom() - 7, WHITE);
         } finally { nvgRestore(vg); }
      }

      void button(Rect bounds, String label, int icon, String key, float mouseX, float mouseY) {
         boolean hover = bounds.contains(mouseX, mouseY);
         float motion = feedback.getOrDefault(key, 0f);
         rounded(bounds, 2, blend(0xFF0D1012, 0xFF173044, motion)); border(bounds, 2, blend(0xFF242A2D, 0xFF315B7B, motion));
         float x = bounds.x() + (bounds.width() - textWidth(vg, medium, label, 12) - (icon >= 0 ? 18 : 0)) / 2;
         if (icon >= 0) { icon(9, x + 5, bounds.y() + bounds.height() / 2, 11, TEXT); x += 18; }
         text(label, x, bounds.y() + bounds.height() / 2, 12, medium, hover ? WHITE : TEXT, bounds.right() - x - 3);
      }

      void field(Rect bounds, String value, boolean hover, boolean multiple) {
         rounded(bounds, 1.5f, hover ? 0xFF11171D : 0xFF0B0C0D); border(bounds, 1.5f, hover ? 0xFF2B4B65 : 0xFF1E2225);
         text(elide(value, 11.5f, bounds.width() - 25, false), bounds.x() + 7, bounds.y() + bounds.height() / 2, 11.5f, regular, TEXT, bounds.width() - 25);
         chevron(bounds.right() - 10, bounds.y() + bounds.height() / 2, multiple ? ACCENT : 0xFF8B979F);
      }

      void chevron(float x, float y, int value) {
         nvgBeginPath(vg); nvgMoveTo(vg, x - 3, y - 1.5f); nvgLineTo(vg, x, y + 1.5f); nvgLineTo(vg, x + 3, y - 1.5f);
         nvgStrokeWidth(vg, 1.4f); nvgStrokeColor(vg, rgba(value, color)); nvgStroke(vg);
      }

      void section(Section section, float opacity, float mouseX, float mouseY) {
         Rect bounds = section.bounds;
         text(section.title, bounds.x() + 8, bounds.y() + 10, 13, medium, WHITE, bounds.width() - 84);
         Rect key = binding(bounds);
         boolean hoverKey = key.contains(mouseX, mouseY);
         rounded(key, 2, hoverKey ? 0xFF15202A : 0xFF101314);
         text(section.binding, key.x() + 5, key.y() + key.height() / 2, 9, regular,
            hoverKey ? ACCENT : 0xFF77858E, key.width() - 9);
         nvgSave(vg);
         try {
            nvgTranslate(vg, bounds.right() - 7, bounds.y() + 12);
            nvgRotate(vg, (float)((1 - section.reveal) * -Math.PI / 2));
            chevron(0, 0, 0xFF65717A);
         } finally { nvgRestore(vg); }
         line(bounds.x() + 6, bounds.y() + 24, bounds.right(), bounds.y() + 24, 0xFF222425);
         nvgSave(vg);
         try {
            nvgIntersectScissor(vg, bounds.x(), bounds.y() + SECTION_HEADER, bounds.width(), Math.max(0, bounds.height() - SECTION_HEADER));
            nvgGlobalAlpha(vg, opacity * section.reveal);
            for (Row row : section.rows) if (row.bounds.intersect(bounds).intersect(CONTENT).height() > 0) row(row);
         } finally { nvgRestore(vg); }
      }

      void row(Row row) {
         Rect bounds = row.bounds;
         if (row.hovered) rounded(new Rect(bounds.x(), bounds.y() + 1, bounds.width(), ROW - 2), 2, 0xFF0F1316);
         if (row.feedback > .001f) rounded(new Rect(bounds.x(), bounds.y() + 1, bounds.width(), ROW - 2), 2,
            ((int)(row.feedback * 35) << 24) | (ACCENT & 0xFFFFFF));
         float labelWidth = row.kind == Kind.BOOLEAN || row.kind == Kind.ENABLED ? bounds.width() - 40 : bounds.width() - 138;
         text(elide(row.label, 11.5f, labelWidth, true), bounds.x() + 8, bounds.y() + ROW / 2, 11.5f, regular, TEXT, labelWidth);
         switch (row.kind) {
            case BOOLEAN, ENABLED -> {
               float p = row.progress, x = bounds.right() - 29, y = bounds.y() + ROW / 2;
               rounded(new Rect(x, y - 6, 28, 12), 6, blend(0xFF111518, 0xFF0B2537, p));
               circle(x + 6 + p * 16, y, 8.5f, ((int)(p * 24) << 24) | (ACCENT & 0xFFFFFF));
               circle(x + 6 + p * 16, y, 5.5f, blend(0xFF586168, ACCENT, p));
            }
            case NUMBER -> {
               Rect track = slider(bounds);
               float y = bounds.y() + ROW / 2, knob = track.x() + track.width() * row.progress;
               rounded(new Rect(track.x(), y - 1, track.width(), 2), 1, 0xFF333A40);
               rounded(new Rect(track.x(), y - 1, Math.max(0, knob - track.x()), 2), 1, 0xFF286090);
               circle(knob, y, 8, ((int)(row.feedback * 28) << 24) | (ACCENT & 0xFFFFFF));
               circle(knob, y, 5.4f + row.feedback * .8f, ACCENT);
               Rect number = new Rect(track.right() + 7, bounds.y() + 5, 31, ROW - 10);
               rounded(number, 1, 0xFF0E1113);
               float width = textWidth(vg, regular, row.value, 10);
               text(row.value, number.x() + Math.max(3, (number.width() - width) / 2), y, 10, regular, TEXT, number.width() - 3);
            }
            case CHOICE -> field(control(bounds), row.value, row.hovered, false);
         }
      }

      private String elide(String value, float size, float width, boolean keepLastWord) {
         if (textWidth(vg, regular, value, size) <= width) return value;
         int end = value.length();
         String suffix = "";
         int lastSpace = value.lastIndexOf(' ');
         if (keepLastWord && lastSpace > 0) {
            suffix = value.substring(lastSpace);
            if (textWidth(vg, regular, suffix, size) < width * .45f) end = lastSpace;
            else suffix = "";
         }
         while (end > 0 && textWidth(vg, regular, value.substring(0, end) + "…" + suffix, size) > width)
            end = value.offsetByCodePoints(end, -1);
         return value.substring(0, end).stripTrailing() + "…" + suffix;
      }

      void tooltip(String label, float mouseX, float mouseY) {
         float width = Math.min(460, textWidth(vg, regular, label, 11.5f) + 18);
         Rect bounds = new Rect(Math.clamp(mouseX + 14, SIDEBAR + 8, WIDTH - width - 12),
            Math.clamp(mouseY + 15, HEADER + 8, HEIGHT - 38), width, 25);
         shadow(bounds, 3); rounded(bounds, 3, 0xFF1A2229); border(bounds, 3, 0xFF345168);
         text(label, bounds.x() + 9, bounds.y() + bounds.height() / 2, 11.5f, regular, WHITE, bounds.width() - 18);
      }

      void config(Config config, float mouseX, float mouseY) {
         Rect bounds = config.bounds;
         rounded(bounds, 3, config.active ? 0xFF101922 : 0xFF0E1113); border(bounds, 3, config.active ? 0xFF264C6B : 0xFF202426);
         icon(7, bounds.x() + 19, bounds.y() + 25, 15, ACCENT);
         text(config.name, bounds.x() + 39, bounds.y() + 18, 13, medium, WHITE, bounds.width() - 264);
         text(config.detail, bounds.x() + 39, bounds.y() + 36, 10.5f, regular, 0xFF73828C, bounds.width() - 264);
         button(new Rect(bounds.right() - 202, bounds.y() + 14, 58, 25), "Load", -1, "load:" + config.name, mouseX, mouseY);
         button(new Rect(bounds.right() - 136, bounds.y() + 14, 58, 25), "Save", -1, "store:" + config.name, mouseX, mouseY);
         button(new Rect(bounds.right() - 70, bounds.y() + 14, 58, 25), config.confirmingDelete ? "Confirm" : "Delete", -1, "delete:" + config.name, mouseX, mouseY);
      }

      void dropdown(Dropdown dropdown, float mouseX, float mouseY, float opacity) {
         Rect bounds = dropdown.bounds;
         shadow(bounds, 3);
         nvgSave(vg);
         try {
            nvgGlobalAlpha(vg, opacity * dropdown.reveal);
            nvgIntersectScissor(vg, bounds.x() - 1, bounds.y() - 1, bounds.width() + 2, (bounds.height() + 2) * dropdown.reveal);
            rounded(bounds, 3, 0xFF12171B); border(bounds, 3, 0xFF2C3C49);
            nvgIntersectScissor(vg, bounds.x() + 3, bounds.y() + 4, bounds.width() - 6, bounds.height() - 8);
            for (int i = 0; i < dropdown.options.size(); i++) {
               Option option = dropdown.options.get(i);
               Rect item = new Rect(bounds.x() + 4, bounds.y() + 4 + i * OPTION - dropdown.scroll, bounds.width() - 8, OPTION);
               if (item.bottom() <= bounds.y() || item.y() >= bounds.bottom()) continue;
               if (item.contains(mouseX, mouseY) || option.highlighted) rounded(item, 2, 0xFF203243);
               if (option.selected) { circle(item.right() - 9, item.y() + OPTION / 2, 2.2f, ACCENT); }
               text(option.label, item.x() + 5, item.y() + OPTION / 2, 11.5f, regular,
                  option.selected ? 0xFF73B2FA : TEXT, item.width() - 22);
            }
            float max = dropdown.options.size() * OPTION - bounds.height() + 8;
            if (max > 0) {
               float h = Math.max(15, (bounds.height() - 8) * (bounds.height() - 8) / (dropdown.options.size() * OPTION));
               rounded(new Rect(bounds.right() - 3, bounds.y() + 4 + dropdown.scroll / max * (bounds.height() - 8 - h), 2, h), 1, 0xFF587189);
            }
         } finally { nvgRestore(vg); }
      }

      void icon(int type, float x, float y, float size, int value) {
         nvgSave(vg);
         try {
            nvgTranslate(vg, x, y); nvgScale(vg, size / 16, size / 16);
            nvgBeginPath(vg); nvgStrokeWidth(vg, 1.7f); nvgStrokeColor(vg, rgba(value, color));
            nvgLineCap(vg, NVG_ROUND); nvgLineJoin(vg, NVG_ROUND);
            switch (type) {
               case 0, 1 -> {
                  nvgCircle(vg, 0, 0, 5.5f);
                  if (type == 0) {
                     nvgMoveTo(vg, -8, 0); nvgLineTo(vg, -3, 0); nvgMoveTo(vg, 3, 0); nvgLineTo(vg, 8, 0);
                     nvgMoveTo(vg, 0, -8); nvgLineTo(vg, 0, -3); nvgMoveTo(vg, 0, 3); nvgLineTo(vg, 0, 8);
                  } else { nvgMoveTo(vg, 0, 0); nvgLineTo(vg, 4, -4); }
               }
               case 2 -> {
                  nvgMoveTo(vg, -7, 0); nvgLineTo(vg, 7, 0); nvgMoveTo(vg, 0, -7); nvgLineTo(vg, 0, 7);
                  for (int i = 0; i < 4; i++) {
                     double angle = i * Math.PI / 2; float ax = (float)Math.cos(angle), ay = (float)Math.sin(angle);
                     nvgMoveTo(vg, ax * 4 - ay * 2, ay * 4 + ax * 2); nvgLineTo(vg, ax * 7, ay * 7);
                     nvgLineTo(vg, ax * 4 + ay * 2, ay * 4 - ax * 2);
                  }
               }
               case 3 -> {
                  nvgCircle(vg, 0, -4, 3); nvgMoveTo(vg, -6, 7); nvgLineTo(vg, -6, 4);
                  nvgBezierTo(vg, -6, -1, 6, -1, 6, 4); nvgLineTo(vg, 6, 7); nvgClosePath(vg);
               }
               case 4 -> {
                  nvgMoveTo(vg, -8, 0); nvgBezierTo(vg, -3, -7, 3, -7, 8, 0);
                  nvgBezierTo(vg, 3, 7, -3, 7, -8, 0); nvgCircle(vg, 0, 0, 2.5f);
               }
               case 5 -> {
                  nvgMoveTo(vg, -6, -6); nvgLineTo(vg, 6, 6); nvgMoveTo(vg, 6, -6); nvgLineTo(vg, -6, 6);
                  nvgMoveTo(vg, -7, -3); nvgLineTo(vg, -3, -7); nvgMoveTo(vg, 3, -7); nvgLineTo(vg, 7, -3);
                  nvgMoveTo(vg, -7, 3); nvgLineTo(vg, -3, 7); nvgMoveTo(vg, 3, 7); nvgLineTo(vg, 7, 3);
               }
               case 6 -> {
                  for (int i = -1; i <= 1; i++) { nvgMoveTo(vg, i * 5, -7); nvgLineTo(vg, i * 5, 7); }
                  nvgCircle(vg, -5, -2, 2); nvgCircle(vg, 0, 3, 2); nvgCircle(vg, 5, -4, 2);
               }
               case 7 -> {
                  for (int i = 0; i < 8; i++) {
                     double angle = i * Math.PI / 4; float ax = (float)Math.cos(angle), ay = (float)Math.sin(angle);
                     nvgMoveTo(vg, ax * 5, ay * 5); nvgLineTo(vg, ax * 7, ay * 7);
                  }
                  nvgCircle(vg, 0, 0, 5); nvgCircle(vg, 0, 0, 2);
               }
               case 8 -> { nvgCircle(vg, -1, -1, 5); nvgMoveTo(vg, 3, 3); nvgLineTo(vg, 7, 7); }
               case 9 -> { nvgRoundedRect(vg, -6, -6, 12, 12, 1); nvgRect(vg, -3, -6, 6, 4); nvgRect(vg, -3, 1, 6, 5); }
               default -> { }
            }
            nvgStroke(vg);
         } finally { nvgRestore(vg); }
      }
   }

   private static int blend(int from, int to, float fraction) {
      int r = Math.round((from >> 16 & 255) + ((to >> 16 & 255) - (from >> 16 & 255)) * fraction);
      int g = Math.round((from >> 8 & 255) + ((to >> 8 & 255) - (from >> 8 & 255)) * fraction);
      int blue = Math.round((from & 255) + ((to & 255) - (from & 255)) * fraction);
      return 0xFF000000 | r << 16 | g << 8 | blue;
   }
}
