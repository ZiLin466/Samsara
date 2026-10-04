package com.samsara.module.visual;

import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.module.FeatureManager;
import com.samsara.setting.BooleanSetting;
import com.samsara.setting.ModeSetting;
import com.samsara.setting.MultiSelectSetting;
import com.samsara.setting.Setting;
import com.samsara.ui.clickgui.opai.OpaiStyle;
import com.samsara.ui.hud.HudLayouts;
import com.samsara.util.ClientColors;
import com.samsara.util.Wrapper;
import com.samsara.util.render.*;
import com.samsara.util.render.FontRepository;
import com.samsara.util.render.NVGRenderer;
import com.samsara.util.render.NVGTextRenderer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.ToDoubleFunction;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.system.MemoryStack;

import static com.samsara.module.visual.Hud.OpaiArraylistLayout.*;
import static org.lwjgl.nanovg.NanoVG.*;

public class Hud extends Feature {

   private final ModeSetting arraylistMode;
   private final ArraylistSettings defaultSettings;
   private final ArraylistSettings opaiSettings;

   private final List<ArraylistEntry> entries = new ArrayList<>();
   private final List<ArraylistEntry> visibleEntries = new ArrayList<>();

   private int widestWidth;
   private int totalHeight;
   private boolean initialized;
   private boolean lastOpai;
   private OpaiArraylistRenderer opaiRenderer;

   public Hud() {
      super("HUD", Category.VISUAL);
      this.arraylistMode = new ModeSetting("ArrayList mode", this, "Default", new String[]{"Default", "Opai"});
      this.defaultSettings = new ArraylistSettings(this, false, () -> !this.isOpai());
      this.opaiSettings = new ArraylistSettings(this, true, this::isOpai);
   }

   public void renderNano() {
      if (!NVGRenderer.isAvailable()) {
         return;
      }
      this.layout();

      final float scale = this.getScale();
      final ArraylistSettings settings = this.settings();

      if (this.isOpai()) {
         List<OpaiArraylistLayout.Row> rows = new ArrayList<>();
         for (int i = 0; i < this.visibleEntries.size(); i++) {
            float nextWidth = i + 1 < this.visibleEntries.size() ? this.visibleEntries.get(i + 1).getWidth() : 0;
            rows.add(this.visibleEntries.get(i).opaiRow(-OpaiArraylistLayout.EDGE_INSET, nextWidth));
         }
         long vg = NVGRenderer.getContext();
         nvgSave(vg);
         try {
            nvgTranslate(vg, Wrapper.mc.getWindow().getGuiScaledWidth(), 0);
            nvgScale(vg, scale, scale);
            this.opaiRenderer.draw(rows, settings.background.m215(), settings.shadow.m215(), settings.edge.m215(),
               ClickGui.currentOpaiPalette());
         } finally {
            nvgRestore(vg);
         }
      } else {
         final ArraylistEntry.BarMode mode = parseBarMode(settings.bar.m224());
         for (int i = 0; i < this.visibleEntries.size(); i++) {
            this.visibleEntries.get(i).renderNano(i, mode, settings.background.m215(), scale, 0, 0);
         }
      }
   }

   public void layout() {
      if (!NVGRenderer.isAvailable()) {
         return;
      }

      this.ensureInitialized();

      final float scale = this.getScale();
      final ArraylistSettings settings = this.settings();
      final boolean opai = this.isOpai();
      if (opai && this.opaiRenderer == null) {
         this.opaiRenderer = new OpaiArraylistRenderer(NVGRenderer.getContext(), FontRepository.getFont("googlesans-bold").getFontId());
      }

      for (ArraylistEntry entry : this.entries) {
         if (this.lastOpai != opai) {
            entry.resetLayout();
         }
         if (opai) {
            entry.updateOpaiText(settings.suffix.m215(), settings.lowercase.m215(), this.opaiRenderer);
         } else {
            entry.updateText(settings.suffix.m215(), settings.lowercase.m215());
         }
      }
      this.lastOpai = opai;
      this.entries.sort(null);

      this.visibleEntries.clear();
      long now = System.nanoTime() / 1_000_000;
      float occupied = 0;
      for (ArraylistEntry entry : this.entries) {
         boolean visible = entry.isModuleVisible() && settings.shows(entry.getModule().getCategory());
         entry.visibility(visible, now);
         entry.position(occupied, now);
         if (entry.isVisible()) {
            this.visibleEntries.add(entry);
            occupied += entry.contribution() * ArraylistEntry.OFFSET;
         }
      }

      this.widestWidth = this.visibleEntries.isEmpty() ? 0 : (int)Math.ceil(this.visibleEntries.get(0).getWidth() * scale);
      this.totalHeight = (int)Math.ceil(occupied * scale);
      if (!this.visibleEntries.isEmpty()) HudLayouts.INSTANCE.drawn(HudLayouts.Element.ARRAYLIST,
         new HudLayouts.Box(Wrapper.mc.getWindow().getGuiScaledWidth() - this.widestWidth, 0,
            this.widestWidth, Math.max(12 * scale, this.totalHeight)));

      if (!logged && !this.visibleEntries.isEmpty()) {
         logged = true;
         org.slf4j.LoggerFactory.getLogger("samsara-nvg").info(
            "[samsara] nano render: entries={} widest={} firstWidth={}",
            this.visibleEntries.size(), this.widestWidth, this.visibleEntries.get(0).getWidth());
      }
   }

   private static boolean logged;

   public float getScale() {
      return HudLayouts.INSTANCE.get(HudLayouts.Element.ARRAYLIST).scale();
   }

   private boolean isOpai() {
      return this.arraylistMode.m228("Opai");
   }

   private ArraylistSettings settings() {
      return this.isOpai() ? this.opaiSettings : this.defaultSettings;
   }

   private static ArraylistEntry.BarMode parseBarMode(String value) {
      if ("Right".equals(value)) {
         return ArraylistEntry.BarMode.RIGHT;
      }
      if ("None".equals(value)) {
         return ArraylistEntry.BarMode.NONE;
      }
      return ArraylistEntry.BarMode.LEFT;
   }

   private void ensureInitialized() {
      if (this.initialized) {
         return;
      }
      this.initialized = true;
      for (Feature module : FeatureManager.getModules()) {
         this.entries.add(new ArraylistEntry(module));
      }
   }

   /** Each style owns its values; the empty prefix preserves existing Default config keys. */
   public static final class ArraylistSettings {
      public final ModeSetting bar;
      public final BooleanSetting lowercase;
      public final BooleanSetting suffix;
      public final BooleanSetting background;
      public final BooleanSetting shadow;
      public final BooleanSetting edge;
      public final MultiSelectSetting categories;

      public ArraylistSettings(Feature owner, boolean opai, BooleanSupplier visible) {
         String prefix = opai ? "Opai " : "";
         int start = owner.settings.size();
         this.bar = opai ? null : new ModeSetting("Bar mode", owner, "Left", new String[]{"Left", "Right", "None"});
         this.lowercase = new BooleanSetting(prefix + "Lowercase", owner, !opai);
         this.suffix = new BooleanSetting(prefix + "Show suffix", owner, true);
         this.background = new BooleanSetting(prefix + "Background", owner, true);
         this.shadow = opai ? new BooleanSetting(prefix + "Shadow", owner, true) : null;
         this.edge = opai ? new BooleanSetting(prefix + "Right line", owner, true) : null;
         String[] choices = {"Combat", "Movement", "Player", "Visual", "Misc"};
         this.categories = new MultiSelectSetting(prefix + "Categories", owner, choices, List.of(choices));
         this.categories.setLegacyBooleanPrefix(prefix);
         for (Setting setting : owner.settings.subList(start, owner.settings.size())) {
            setting.setDisplayName(setting.getName().substring(prefix.length()));
            setting.setVisible(visible);
         }
      }

      public boolean shows(Category category) {
         return this.categories.contains(switch (category) {
            case COMBAT -> "Combat";
            case MOVEMENT -> "Movement";
            case PLAYER -> "Player";
            case VISUAL -> "Visual";
            case MISC -> "Misc";
         });
      }
   }

   public static final class ArraylistEntry implements Comparable<ArraylistEntry> {
      public static final float OFFSET = 12;
      private final Feature module;
      private final ModeSetting suffixMode;
      private ArraylistMotion motion = new ArraylistMotion();
      private String text = "";
      private OpaiArraylistLayout.Metrics opaiText;
      private float width;
      private static NVGTextRenderer font() { return FontRepository.getFont("productsans-medium"); }
      public ArraylistEntry(Feature module) {
         this.module = module;
         List<ModeSetting> modes = module.settings.stream().filter(ModeSetting.class::isInstance).map(ModeSetting.class::cast).toList();
         int index = OpaiArraylistLayout.suffixSetting(module.getName(), modes.stream().map(ModeSetting::getName).toList());
         this.suffixMode = index < 0 ? null : modes.get(index);
      }
      public void updateText(boolean suffix, boolean lowercase) {
         String name = suffix ? module.getDisplayName() : module.getName();
         text = lowercase ? name.toLowerCase(Locale.ROOT) : name; width = font().getStringWidth(text, 8);
      }
      public void updateOpaiText(boolean suffix, boolean lowercase, OpaiArraylistRenderer renderer) {
         opaiText = OpaiArraylistLayout.measure(module.getName(), suffixMode == null ? "" : suffixMode.m224(), lowercase, suffix, renderer::measure);
         width = opaiText.width();
      }
      public void visibility(boolean visible, long now) { motion.visibility(visible, now); }
      public void position(float y, long now) { motion.position(y, now); }
      public float contribution() { return motion.contribution(); }
      private float slide() { return (width + 8) * (1 - motion.progress()); }
      public OpaiArraylistLayout.Row opaiRow(float right, float nextWidth) {
         return OpaiArraylistLayout.row(opaiText, right + slide(), OpaiArraylistLayout.TOP_INSET + motion.y(), nextWidth);
      }
      public void renderNano(int index, BarMode barMode, boolean background, float scale, int xOffset, int yOffset) {
         int screenWidth = NVGRenderer.getMinecraft().getWindow().getGuiScaledWidth();
         float x = screenWidth + xOffset - width + slide(), y = motion.y() + yOffset;
         int color = ClientColors.m52(index * 20);
         NVGRenderer.scale(scale, screenWidth + xOffset, 0, 0, 0, () -> {
            if (background) NVGRenderer.rect(x - 6.5f, y, width + 6.5f, OFFSET, 0x80090909);
            if (barMode != BarMode.NONE) {
               float bx = barMode == BarMode.LEFT ? x - 4.5f : x + width - 2.5f;
               NVGRenderer.roundedRect(bx + .5f, y + 2.5f, 1, 8, 1, ColorUtility.getShadowColor(color));
               NVGRenderer.roundedRect(bx, y + 2, 1, 8, 1, color);
            }
            float textOffset = barMode == BarMode.LEFT ? 2 : barMode == BarMode.NONE ? 3.5f : 4.25f;
            font().drawStringWithShadow(text, x - textOffset, y + 9, 8, color);
         });
      }
      public void resetLayout() { motion = new ArraylistMotion(); }
      public boolean isModuleVisible() { return module.isEnabled() && !module.isHidden(); }
      public boolean isVisible() { return motion.visible(); }
      public Feature getModule() { return module; }
      public float getWidth() { return width; }
      @Override public int compareTo(ArraylistEntry other) { return Float.compare(other.width, width); }
      public enum BarMode { LEFT, RIGHT, NONE }
   }

   /** Analytic critically damped motion: real time and velocity survive rapid reversals. */
   public static final class ArraylistMotion {
      public static final class Spring {
         private double value, velocity, target;
         private long last = -1;
         private final double rate;
         private final boolean immediateDeparture;
         public Spring(double value, double rate) { this(value, rate, false); }
         public Spring(double value, double rate, boolean immediateDeparture) {
            this.value = this.target = value; this.rate = rate; this.immediateDeparture = immediateDeparture;
         }
         public double to(double target, long now) {
            if (last >= 0 && now > last) {
               double dt = (now - last) / 1000.0, offset = value - this.target;
               double linear = velocity + rate * offset, decay = Math.exp(-rate * dt);
               value = this.target + (offset + linear * dt) * decay;
               velocity = (velocity - rate * linear * dt) * decay;
            }
            if (target != this.target && immediateDeparture && Math.abs(value-this.target)<.002 && Math.abs(velocity)<.02)
               velocity = rate * (target-value);
            last = now; this.target = target; return value;
         }
         public void snap(double value, long now) { this.value = this.target = value; velocity = 0; last = now; }
         public double value() { return value; }
      }
      private final Spring horizontal = new Spring(0, 25), occupied = new Spring(0, 12.7, true);
      private boolean enabled;
      private float y;
      private long changedAt = Long.MIN_VALUE / 2;
      public void visibility(boolean enabled, long now) {
         if (this.enabled != enabled) { this.enabled = enabled; changedAt = now; }
         horizontal.to(enabled ? 1 : 0, now);
         // The outgoing row starts moving before the gap closes under it.
         occupied.to(enabled || now - changedAt < 65 ? 1 : 0, now);
      }
      public void position(float y, long now) {
         // Row spacing already animates; a second smoothing pass adds lag.
         this.y = y;
      }
      public float progress() { return (float)Math.clamp(horizontal.value(), 0, 1); }
      public float contribution() { return (float)Math.clamp(occupied.value(), 0, 1); }
      public float y() { return this.y; }
      public boolean visible() { return enabled || progress() > .001f || contribution() > .001f; }
   }

   public static final class OpaiArraylistLayout {
      public static final float FONT_SIZE = 9.08F;
      public static final float ROW_HEIGHT = 12F;
      public static final float LEFT_PAD = 3F;
      public static final float RIGHT_PAD = 2F;
      public static final float EDGE_WIDTH = 1F;
      public static final float EDGE_INSET = 0.5F;
      public static final float TOP_INSET = 0.25F;
      public static final float RADIUS = 5F;
      public static final float BASELINE = 8.5F;
      public static final int BACKGROUND = 0xCC101010;
      public static final int TEXT = 0xFFFFFFFF;

      private OpaiArraylistLayout() { }

      public static int suffixSetting(String moduleName, List<String> modeNames) {
         // UI configuration is not a gameplay mode. KillAura must show AutoBlock, not Rotations.
         return switch (moduleName) {
            case "HUD", "ClickGUI", "Theme" -> -1;
            case "KillAura" -> modeNames.indexOf("AutoBlock");
            default -> modeNames.isEmpty() ? -1 : 0;
         };
      }

      public static String displayName(String name, boolean lowercase) {
         String spaced = name.replaceAll("([A-Z]+)([A-Z][a-z])", "$1 $2")
            .replaceAll("([a-z0-9])([A-Z])", "$1 $2");
         return lowercase ? spaced.toLowerCase(Locale.ROOT) : spaced;
      }

      public static Metrics measure(String name, String suffix, boolean lowercase, boolean showSuffix,
                                    ToDoubleFunction<String> measure) {
         String label = displayName(name, lowercase);
         String tag = showSuffix && suffix != null ? suffix.strip() : "";
         if (lowercase) {
            tag = tag.toLowerCase(Locale.ROOT);
         }
         float nameWidth = (float)measure.applyAsDouble(label);
         float gap = tag.isEmpty() ? 0 : (float)measure.applyAsDouble(" ");
         float width = LEFT_PAD + nameWidth + gap + (float)measure.applyAsDouble(tag) + RIGHT_PAD + EDGE_WIDTH;
         return new Metrics(label, tag, nameWidth, gap, width);
      }

      public static Row row(Metrics text, float right, float y, float nextWidth) {
         float radius = Math.min(RADIUS, Math.max(0, text.width() - nextWidth));
         return new Row(text, right - text.width(), y, radius);
      }

      public record Metrics(String name, String suffix, float nameWidth, float gap, float width) { }
      public record Row(Metrics text, float x, float y, float radius) {
         public float right() { return this.x + this.text.width(); }
         public float textX() { return this.x + LEFT_PAD; }
         public float suffixX() { return textX() + this.text.nameWidth() + this.text.gap(); }
      }
   }

   public static final class OpaiArraylistRenderer {
      private final long vg;
      private final int fontId;

      public OpaiArraylistRenderer(long vg, int fontId) {
         this.vg = vg;
         this.fontId = fontId;
      }

      public float measure(String text) {
         nvgSave(this.vg);
         try {
            font();
            return text.isEmpty() ? 0 : nvgTextBounds(this.vg, 0, 0, text, (FloatBuffer)null);
         } finally {
            nvgRestore(this.vg);
         }
      }

      public void draw(List<Row> rows, boolean background, boolean shadow, boolean edge) {
         draw(rows, background, shadow, edge, OpaiStyle.LAVENDER);
      }

      public void draw(List<Row> rows, boolean background, boolean shadow, boolean edge, OpaiStyle.Palette palette) {
         nvgSave(this.vg);
         try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor ink = NVGColor.malloc(stack);
            if (shadow) {
               NVGColor transparent = NVGColor.malloc(stack);
               NVGPaint paint = NVGPaint.malloc(stack);
               color(0x65000000, ink);
               color(0x00000000, transparent);
               // All shadows go behind all fills, so adjacent rows do not darken text or the accent line.
               for (Row row : rows) {
                  nvgBoxGradient(this.vg, row.x(), row.y() + 1, row.text().width(), ROW_HEIGHT,
                     row.radius(), 6F, ink, transparent, paint);
                  nvgBeginPath(this.vg);
                  nvgRect(this.vg, row.x() - 9, row.y() - 8, row.text().width() + 18, ROW_HEIGHT + 18);
                  shape(row);
                  nvgPathWinding(this.vg, NVG_HOLE);
                  nvgFillPaint(this.vg, paint);
                  nvgFill(this.vg);
               }
            }
            if (background) {
               color(BACKGROUND, ink);
               nvgFillColor(this.vg, ink);
               // One fill also avoids double-alpha seams at the shared edges.
               nvgBeginPath(this.vg);
               for (Row row : rows) {
                  shape(row);
               }
               nvgFill(this.vg);
            }
            font();
            for (Row row : rows) {
               color(TEXT, ink);
               nvgFillColor(this.vg, ink);
               nvgText(this.vg, row.textX(), row.y() + BASELINE, row.text().name());
               color(palette.accent(), ink);
               nvgFillColor(this.vg, ink);
               if (!row.text().suffix().isEmpty()) {
                  nvgText(this.vg, row.suffixX(), row.y() + BASELINE, row.text().suffix());
               }
            }
            if (edge) {
               color(palette.accent(), ink);
               nvgFillColor(this.vg, ink);
               nvgBeginPath(this.vg);
               for (Row row : rows) {
                  nvgRect(this.vg, row.right() - EDGE_WIDTH, row.y(), EDGE_WIDTH, ROW_HEIGHT);
               }
               nvgFill(this.vg);
            }
         } finally {
            nvgRestore(this.vg);
         }
      }

      private void font() {
         nvgFontFaceId(this.vg, this.fontId);
         nvgFontSize(this.vg, FONT_SIZE);
         nvgTextLetterSpacing(this.vg, 0);
         nvgFontBlur(this.vg, 0);
         nvgTextAlign(this.vg, NVG_ALIGN_LEFT | NVG_ALIGN_BASELINE);
      }

      private void shape(Row row) {
         nvgRoundedRectVarying(this.vg, row.x(), row.y(), row.text().width(), ROW_HEIGHT, 0, 0, 0, row.radius());
      }

      private static void color(int argb, NVGColor output) {
         nvgRGBA((byte)(argb >> 16), (byte)(argb >> 8), (byte)argb, (byte)(argb >>> 24), output);
      }
   }
}
