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
         Surface s = new Surface(vg, regular, medium, bold, NVGColor.malloc(stack), NVGColor.malloc(stack), NVGPaint.malloc(stack), frame.buttonFeedback);
         Viewport v = frame.viewport;
         nvgTranslate(vg, v.x() + WIDTH * v.scale() / 2, v.y() + HEIGHT * v.scale() / 2);
         nvgScale(vg, v.scale() * frame.openingScale, v.scale() * frame.openingScale);
         nvgTranslate(vg, -WIDTH / 2, -HEIGHT / 2);
         nvgGlobalAlpha(vg, frame.opacity);
         s.shadow(WINDOW, 5);
         nvgSave(vg);
         try {
            nvgIntersectScissor(vg, 0, 0, SIDEBAR, HEIGHT);
            s.gradient(WINDOW, RADIUS, 0xD2090A0A, 0xCC0D0E0C);
         } finally { nvgRestore(vg); }
         nvgSave(vg);
         try {
            nvgIntersectScissor(vg, SIDEBAR, 0, WIDTH - SIDEBAR, HEIGHT);
            s.rounded(WINDOW, RADIUS, 0xFF080808);
         } finally { nvgRestore(vg); }
         s.rounded(new Rect(SIDEBAR, 0, 1, HEIGHT), 0, 0xFF242525);
         s.rounded(new Rect(SIDEBAR, HEADER, WIDTH - SIDEBAR, 1), 0, 0xFF1C1D1D);
         s.brand();
         s.navigation(frame);
         s.footer(frame);
         s.editor(SEARCH, frame.search, "Press CTRL+F to search within GUI", 30);
         s.icon(8, SEARCH.x() + 14, SEARCH.y() + SEARCH.height() / 2, 11, frame.search.focused ? ACCENT : 0xFF677077);
         s.button(SAVE, "Save", 0, "save", frame.mouseX, frame.mouseY);
         s.field(PRESET, frame.preset, PRESET.contains(frame.mouseX, frame.mouseY), false);
         nvgSave(vg);
         try {
            nvgGlobalAlpha(vg, frame.opacity * frame.pageReveal);
            nvgIntersectScissor(vg, CONTENT.x(), CONTENT.y(), CONTENT.width(), CONTENT.height());
            if (frame.navigation == 7) {
               s.editor(CONFIG_INPUT, frame.configName, "Configuration name", 10);
               s.button(CREATE, "Create configuration", -1, "create", frame.mouseX, frame.mouseY);
               s.text("Saved configurations", CONTENT.x(), 135, 12, medium, WHITE, CONTENT.width());
               s.line(CONTENT.x(), 149, CONTENT.right(), 149, 0xFF202222);
               nvgSave(vg);
               try {
                  nvgIntersectScissor(vg, CONTENT.x(), 156, CONTENT.width(), CONTENT.bottom() - 156);
                  for (Config config : frame.configs) if (config.bounds.bottom() > 156 && config.bounds.y() < CONTENT.bottom())
                     s.config(config, frame.mouseX, frame.mouseY);
               } finally { nvgRestore(vg); }
            } else {
               for (Section section : frame.sections) if (section.bounds.intersect(CONTENT).height() > 0)
                  s.section(section, frame.opacity * frame.pageReveal, frame.mouseX, frame.mouseY);
               if (frame.sections.isEmpty()) {
                  s.text("No modules found", CONTENT.x() + 12, CONTENT.y() + 28, 13, medium, TEXT, 300);
               }
            }
         } finally { nvgRestore(vg); }
         if (frame.maximumScroll > 0) {
            Rect track = frame.navigation == 7 ? new Rect(CONTENT.x(), 156, CONTENT.width(), CONTENT.bottom() - 156) : CONTENT;
            s.rounded(new Rect(748, track.y(), 3, track.height()), 1.5f, 0xFF16191B);
            s.rounded(thumb(frame.scroll, frame.maximumScroll, track), 1.5f, 0xFF424C54);
         }
         if (!frame.status.isEmpty()) s.text(frame.status, CONTENT.x(), HEIGHT - 9, 10, regular,
            frame.error ? 0xFFE88989 : 0xFF849CAC, CONTENT.width());
         if (frame.dropdown == null && frame.navigation != 7) {
            for (Section section : frame.sections) for (Row row : section.rows) {
               if (!row.hovered || !row.bounds.intersect(section.bounds).intersect(CONTENT).contains(frame.mouseX, frame.mouseY)) continue;
               float width = row.kind == Kind.BOOLEAN || row.kind == Kind.ENABLED ? row.bounds.width() - 40 : row.bounds.width() - 138;
               if (frame.mouseX < control(row.bounds).x() && textWidth(vg, regular, row.label, 11.5f) > width)
                  s.tooltip(row.label, frame.mouseX, frame.mouseY);
               else if (row.kind == Kind.CHOICE && frame.mouseX >= control(row.bounds).x()
                  && textWidth(vg, regular, row.value, 11.5f) > control(row.bounds).width() - 25)
                  s.tooltip(row.value, frame.mouseX, frame.mouseY);
            }
         }
         if (frame.dropdown != null) s.dropdown(frame.dropdown, frame.mouseX, frame.mouseY, frame.opacity);
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

      void rounded(Rect r, float radius, int value) {
         if (r.height() <= 0 || r.width() <= 0) return;
         nvgBeginPath(vg); nvgRoundedRect(vg, r.x(), r.y(), r.width(), r.height(), radius);
         nvgFillColor(vg, rgba(value, color)); nvgFill(vg);
      }

      void border(Rect r, float radius, int value) {
         nvgBeginPath(vg); nvgRoundedRect(vg, r.x() + .5f, r.y() + .5f, r.width() - 1, r.height() - 1, radius);
         nvgStrokeWidth(vg, 1); nvgStrokeColor(vg, rgba(value, color)); nvgStroke(vg);
      }

      void gradient(Rect r, float radius, int top, int bottom) {
         nvgLinearGradient(vg, r.x(), r.y(), r.x(), r.bottom(), rgba(top, color), rgba(bottom, other), paint);
         nvgBeginPath(vg); nvgRoundedRect(vg, r.x(), r.y(), r.width(), r.height(), radius);
         nvgFillPaint(vg, paint); nvgFill(vg);
      }

      void shadow(Rect r, float radius) {
         nvgBoxGradient(vg, r.x(), r.y(), r.width(), r.height(), radius, 22, rgba(0x99000000, color), rgba(0, other), paint);
         nvgBeginPath(vg); nvgRect(vg, r.x() - 20, r.y() - 20, r.width() + 40, r.height() + 40);
         nvgRoundedRect(vg, r.x(), r.y(), r.width(), r.height(), radius); nvgPathWinding(vg, NVG_HOLE);
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

      void navigation(Frame f) {
         String[] groups = {"Combat", "Player", "Visuals", "Miscellaneous"};
         float[] groupY = {79, 183, 287, 355};
         for (int i = 0; i < groups.length; i++) text(groups[i], 20, groupY[i], 10.5f, medium, 0xFF646B6D, 145);
         rounded(new Rect(10, f.selectionY, SIDEBAR - 20, 29), 3, 0xFF3D3D3B);
         for (int i = 0; i < NAV_NAMES.length; i++) {
            Rect r = NeverloseLayout.navigation(i);
            if (i != f.navigation && r.contains(f.mouseX, f.mouseY)) rounded(r, 3, 0x452F363C);
            icon(i, 28, r.y() + r.height() / 2, 14, ACCENT);
            text(NAV_NAMES[i], 47, r.y() + r.height() / 2, 12.5f, medium, i == f.navigation ? WHITE : TEXT, 115);
         }
      }

      void footer(Frame f) {
         line(0, HEIGHT - 62, SIDEBAR, HEIGHT - 62, 0xFF30322F);
         circle(30, HEIGHT - 31, 17, 0xFF151B1F);
         circle(30, HEIGHT - 31, 15.5f, 0xFF20323F);
         String initial = f.username.isEmpty() ? "S" : f.username.substring(0, f.username.offsetByCodePoints(0, 1)).toUpperCase(java.util.Locale.ROOT);
         float w = textWidth(vg, bold, initial, 15);
         text(initial, 30 - w / 2, HEIGHT - 31, 15, bold, WHITE, 30);
         text(f.username, 57, HEIGHT - 39, 12, medium, WHITE, SIDEBAR - 67);
         text("Samsara " + f.version, 57, HEIGHT - 23, 10.5f, regular, ACCENT, SIDEBAR - 67);
      }

      void editor(Rect r, Editor e, String hint, float padding) {
         rounded(r, 2, 0xFF0E1011); border(r, 2, e.focused ? 0xFF315374 : 0xFF202325);
         nvgSave(vg);
         try {
            float left = r.x() + padding, right = r.right() - 9;
            nvgIntersectScissor(vg, left, r.y() + 2, right - left, r.height() - 4);
            float offset = editorOffset(vg, regular, e, r, padding);
            float caret = textWidth(vg, regular, e.value.substring(0, e.cursor), 12);
            if (e.focused && e.selection != e.cursor) {
               float other = textWidth(vg, regular, e.value.substring(0, e.selection), 12);
               rounded(new Rect(left + Math.min(caret, other) - offset, r.y() + 6, Math.abs(caret - other), r.height() - 12), 1, 0x773A85E2);
            }
            text(e.value.isEmpty() ? hint : e.value, left - offset, r.y() + r.height() / 2,
               12, regular, e.value.isEmpty() ? 0xFF697277 : WHITE, r.width() + offset);
            if (e.focused && System.nanoTime() / 500_000_000 % 2 == 0)
               line(left + caret - offset, r.y() + 7, left + caret - offset, r.bottom() - 7, WHITE);
         } finally { nvgRestore(vg); }
      }

      void button(Rect r, String label, int icon, String key, float mouseX, float mouseY) {
         boolean hover = r.contains(mouseX, mouseY);
         float motion = feedback.getOrDefault(key, 0f);
         rounded(r, 2, blend(0xFF0D1012, 0xFF173044, motion)); border(r, 2, blend(0xFF242A2D, 0xFF315B7B, motion));
         float x = r.x() + (r.width() - textWidth(vg, medium, label, 12) - (icon >= 0 ? 18 : 0)) / 2;
         if (icon >= 0) { icon(9, x + 5, r.y() + r.height() / 2, 11, TEXT); x += 18; }
         text(label, x, r.y() + r.height() / 2, 12, medium, hover ? WHITE : TEXT, r.right() - x - 3);
      }

      void field(Rect r, String value, boolean hover, boolean multiple) {
         rounded(r, 1.5f, hover ? 0xFF11171D : 0xFF0B0C0D); border(r, 1.5f, hover ? 0xFF2B4B65 : 0xFF1E2225);
         text(elide(value, 11.5f, r.width() - 25, false), r.x() + 7, r.y() + r.height() / 2, 11.5f, regular, TEXT, r.width() - 25);
         chevron(r.right() - 10, r.y() + r.height() / 2, multiple ? ACCENT : 0xFF8B979F);
      }

      void chevron(float x, float y, int value) {
         nvgBeginPath(vg); nvgMoveTo(vg, x - 3, y - 1.5f); nvgLineTo(vg, x, y + 1.5f); nvgLineTo(vg, x + 3, y - 1.5f);
         nvgStrokeWidth(vg, 1.4f); nvgStrokeColor(vg, rgba(value, color)); nvgStroke(vg);
      }

      void section(Section section, float opacity, float mouseX, float mouseY) {
         Rect r = section.bounds;
         text(section.title, r.x() + 8, r.y() + 10, 13, medium, WHITE, r.width() - 84);
         Rect key = binding(r);
         boolean hoverKey = key.contains(mouseX, mouseY);
         rounded(key, 2, hoverKey ? 0xFF15202A : 0xFF101314);
         text(section.binding, key.x() + 5, key.y() + key.height() / 2, 9, regular,
            hoverKey ? ACCENT : 0xFF77858E, key.width() - 9);
         nvgSave(vg);
         try {
            nvgTranslate(vg, r.right() - 7, r.y() + 12);
            nvgRotate(vg, (float)((1 - section.reveal) * -Math.PI / 2));
            chevron(0, 0, 0xFF65717A);
         } finally { nvgRestore(vg); }
         line(r.x() + 6, r.y() + 24, r.right(), r.y() + 24, 0xFF222425);
         nvgSave(vg);
         try {
            nvgIntersectScissor(vg, r.x(), r.y() + SECTION_HEADER, r.width(), Math.max(0, r.height() - SECTION_HEADER));
            nvgGlobalAlpha(vg, opacity * section.reveal);
            for (Row row : section.rows) if (row.bounds.intersect(r).intersect(CONTENT).height() > 0) row(row);
         } finally { nvgRestore(vg); }
      }

      void row(Row row) {
         Rect r = row.bounds;
         if (row.hovered) rounded(new Rect(r.x(), r.y() + 1, r.width(), ROW - 2), 2, 0xFF0F1316);
         if (row.feedback > .001f) rounded(new Rect(r.x(), r.y() + 1, r.width(), ROW - 2), 2,
            ((int)(row.feedback * 35) << 24) | (ACCENT & 0xFFFFFF));
         float labelWidth = row.kind == Kind.BOOLEAN || row.kind == Kind.ENABLED ? r.width() - 40 : r.width() - 138;
         text(elide(row.label, 11.5f, labelWidth, true), r.x() + 8, r.y() + ROW / 2, 11.5f, regular, TEXT, labelWidth);
         switch (row.kind) {
            case BOOLEAN, ENABLED -> {
               float p = row.progress, x = r.right() - 29, y = r.y() + ROW / 2;
               rounded(new Rect(x, y - 6, 28, 12), 6, blend(0xFF111518, 0xFF0B2537, p));
               circle(x + 6 + p * 16, y, 8.5f, ((int)(p * 24) << 24) | (ACCENT & 0xFFFFFF));
               circle(x + 6 + p * 16, y, 5.5f, blend(0xFF586168, ACCENT, p));
            }
            case NUMBER -> {
               Rect track = slider(r);
               float y = r.y() + ROW / 2, knob = track.x() + track.width() * row.progress;
               rounded(new Rect(track.x(), y - 1, track.width(), 2), 1, 0xFF333A40);
               rounded(new Rect(track.x(), y - 1, Math.max(0, knob - track.x()), 2), 1, 0xFF286090);
               circle(knob, y, 8, ((int)(row.feedback * 28) << 24) | (ACCENT & 0xFFFFFF));
               circle(knob, y, 5.4f + row.feedback * .8f, ACCENT);
               Rect number = new Rect(track.right() + 7, r.y() + 5, 31, ROW - 10);
               rounded(number, 1, 0xFF0E1113);
               float width = textWidth(vg, regular, row.value, 10);
               text(row.value, number.x() + Math.max(3, (number.width() - width) / 2), y, 10, regular, TEXT, number.width() - 3);
            }
            case CHOICE -> field(control(r), row.value, row.hovered, false);
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
         Rect r = new Rect(Math.clamp(mouseX + 14, SIDEBAR + 8, WIDTH - width - 12),
            Math.clamp(mouseY + 15, HEADER + 8, HEIGHT - 38), width, 25);
         shadow(r, 3); rounded(r, 3, 0xFF1A2229); border(r, 3, 0xFF345168);
         text(label, r.x() + 9, r.y() + r.height() / 2, 11.5f, regular, WHITE, r.width() - 18);
      }

      void config(Config config, float mouseX, float mouseY) {
         Rect r = config.bounds;
         rounded(r, 3, config.active ? 0xFF101922 : 0xFF0E1113); border(r, 3, config.active ? 0xFF264C6B : 0xFF202426);
         icon(7, r.x() + 19, r.y() + 25, 15, ACCENT);
         text(config.name, r.x() + 39, r.y() + 18, 13, medium, WHITE, r.width() - 264);
         text(config.detail, r.x() + 39, r.y() + 36, 10.5f, regular, 0xFF73828C, r.width() - 264);
         button(new Rect(r.right() - 202, r.y() + 14, 58, 25), "Load", -1, "load:" + config.name, mouseX, mouseY);
         button(new Rect(r.right() - 136, r.y() + 14, 58, 25), "Save", -1, "store:" + config.name, mouseX, mouseY);
         button(new Rect(r.right() - 70, r.y() + 14, 58, 25), config.confirmingDelete ? "Confirm" : "Delete", -1, "delete:" + config.name, mouseX, mouseY);
      }

      void dropdown(Dropdown dropdown, float mouseX, float mouseY, float opacity) {
         Rect r = dropdown.bounds;
         shadow(r, 3);
         nvgSave(vg);
         try {
            nvgGlobalAlpha(vg, opacity * dropdown.reveal);
            nvgIntersectScissor(vg, r.x() - 1, r.y() - 1, r.width() + 2, (r.height() + 2) * dropdown.reveal);
            rounded(r, 3, 0xFF12171B); border(r, 3, 0xFF2C3C49);
            nvgIntersectScissor(vg, r.x() + 3, r.y() + 4, r.width() - 6, r.height() - 8);
            for (int i = 0; i < dropdown.options.size(); i++) {
               Option option = dropdown.options.get(i);
               Rect item = new Rect(r.x() + 4, r.y() + 4 + i * OPTION - dropdown.scroll, r.width() - 8, OPTION);
               if (item.bottom() <= r.y() || item.y() >= r.bottom()) continue;
               if (item.contains(mouseX, mouseY) || option.highlighted) rounded(item, 2, 0xFF203243);
               if (option.selected) { circle(item.right() - 9, item.y() + OPTION / 2, 2.2f, ACCENT); }
               text(option.label, item.x() + 5, item.y() + OPTION / 2, 11.5f, regular,
                  option.selected ? 0xFF73B2FA : TEXT, item.width() - 22);
            }
            float max = dropdown.options.size() * OPTION - r.height() + 8;
            if (max > 0) {
               float h = Math.max(15, (r.height() - 8) * (r.height() - 8) / (dropdown.options.size() * OPTION));
               rounded(new Rect(r.right() - 3, r.y() + 4 + dropdown.scroll / max * (r.height() - 8 - h), 2, h), 1, 0xFF587189);
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
                     double a = i * Math.PI / 2; float ax = (float)Math.cos(a), ay = (float)Math.sin(a);
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
                     double a = i * Math.PI / 4; float ax = (float)Math.cos(a), ay = (float)Math.sin(a);
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
      int b = Math.round((from & 255) + ((to & 255) - (from & 255)) * fraction);
      return 0xFF000000 | r << 16 | g << 8 | b;
   }
}
