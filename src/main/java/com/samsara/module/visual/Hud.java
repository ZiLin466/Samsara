package com.samsara.module.visual;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.event.impl.EventRender2D;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.module.FeatureManager;
import com.samsara.module.combat.KillAura;
import com.samsara.module.visual.Hud.TargetHud.OpaiTargetHudPainter.Surface;
import com.samsara.module.visual.Hud.TargetHud.OpaiTargetHudSurface;
import com.samsara.setting.BooleanSetting;
import com.samsara.setting.ModeSetting;
import com.samsara.setting.MultiSelectSetting;
import com.samsara.setting.Setting;
import com.samsara.ui.clickgui.opai.OpaiStyle;
import com.samsara.ui.hud.HudLayouts;
import com.samsara.ui.hud.editor.HudEditorScreen;
import com.samsara.util.ClientColors;
import com.samsara.util.ColorUtil;
import com.samsara.util.ModTextures;
import com.samsara.util.Wrapper;
import com.samsara.util.render.*;
import com.samsara.util.render.FontRepository;
import com.samsara.util.render.HudBackdrop;
import com.samsara.util.render.HudGlassStyle;
import com.samsara.util.render.NVGRenderer;
import com.samsara.util.render.NVGTextRenderer;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.ToDoubleFunction;
import java.util.stream.IntStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.system.MemoryStack;
import static com.samsara.module.visual.Hud.OpaiArraylistLayout.*;
import static org.lwjgl.nanovg.NanoVG.*;



public class Hud extends Feature {

   public final MultiSelectSetting widgets = new MultiSelectSetting("Widgets", this, Widget.labels(), List.of(Widget.labels()));
   private final InventoryHud inventoryHud = new InventoryHud();
   private final TargetHud targetHud;
   private final SessionHud sessionHud = new SessionHud();
   private final PotionStatus potionStatus = new PotionStatus();
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
      this.arraylistMode.setVisible(() -> selected(Widget.ARRAY_LIST));
      this.defaultSettings = new ArraylistSettings(this, false, () -> selected(Widget.ARRAY_LIST) && !this.isOpai());
      this.opaiSettings = new ArraylistSettings(this, true, () -> selected(Widget.ARRAY_LIST) && this.isOpai());
      this.targetHud = new TargetHud(this);
   }

   public boolean selected(Widget widget) { return this.widgets.contains(widget.label); }

   public boolean widgetEnabled(Widget widget) {
      return selected(widget) && (isEnabled() || mc != null && HudEditorScreen.active());
   }

   public static boolean enabled(Widget widget) {
      return FeatureManager.f25 != null && FeatureManager.f25.widgetEnabled(widget);
   }

   @Override public void onEvent(Event event) {
      if (selected(Widget.INVENTORY_HUD)) this.inventoryHud.onEvent(event);
      if (selected(Widget.TARGET_HUD)) this.targetHud.onEvent(event); else this.targetHud.onDisable();
      if (selected(Widget.SESSION_HUD)) this.sessionHud.onEvent(event);
      if (selected(Widget.POTION_STATUS)) this.potionStatus.onEvent(event); else this.potionStatus.onDisable();
   }

   @Override public void onDisable() {
      this.targetHud.onDisable();
      this.potionStatus.onDisable();
      this.visibleEntries.clear();
      this.entries.forEach(ArraylistEntry::resetLayout);
   }

   public void renderPreview(GuiGraphicsExtractor graphics, float partialTick) {
      if (selected(Widget.INVENTORY_HUD)) this.inventoryHud.renderInventory(graphics);
      if (selected(Widget.TARGET_HUD)) this.targetHud.m207(graphics, partialTick, mc.player);
      else this.targetHud.onDisable();
      if (selected(Widget.SESSION_HUD)) this.sessionHud.renderSession(graphics);
      if (selected(Widget.POTION_STATUS)) this.potionStatus.renderStatus(graphics);
      else this.potionStatus.onDisable();
   }

   public void renderNano() {
      if (!widgetEnabled(Widget.ARRAY_LIST)) {
         this.visibleEntries.clear();
         this.entries.forEach(ArraylistEntry::resetLayout);
         return;
      }
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

   public enum Widget {
      STATUS_BAR("Dynamic Island"), ARRAY_LIST("ArrayList"), POTION_STATUS("Potion Status"),
      INVENTORY_HUD("InventoryHUD"), TARGET_HUD("TargetHUD"), SESSION_HUD("SessionHUD");
      public final String label;
      Widget(String label) { this.label = label; }
      public static String[] labels() { return java.util.Arrays.stream(values()).map(widget -> widget.label).toArray(String[]::new); }

      /** Retired modules become widget selections; explicit new values take precedence. */
      public static JsonObject migrate(JsonObject source) {
         JsonObject root = source.deepCopy();
         if (!root.has("HUD") && !root.has("TargetHUD") && !root.has("InventoryHUD")
             && !root.has("SessionHUD") && !root.has("Potion Status")) return root;
         JsonObject hud = object(root, "HUD");
         if (hud == null) {
            if (root.has("HUD")) return root;
            hud = new JsonObject(); root.add("HUD", hud);
         }
         JsonObject settings = object(hud, "settings");
         if (settings == null) {
            if (hud.has("settings")) return root;
            settings = new JsonObject(); hud.add("settings", settings);
         }
         if (!settings.has("Widgets")) {
            JsonArray selected = new JsonArray();
            selected.add(STATUS_BAR.label);
            if (legacyEnabled(hud)) selected.add(ARRAY_LIST.label);
            if (legacyEnabled(object(root, "Potion Status"))) selected.add(POTION_STATUS.label);
            if (legacyEnabled(object(root, "InventoryHUD"))) selected.add(INVENTORY_HUD.label);
            if (legacyEnabled(object(root, "TargetHUD"))) selected.add(TARGET_HUD.label);
            if (legacyEnabled(object(root, "SessionHUD"))) selected.add(SESSION_HUD.label);
            settings.add("Widgets", selected);
            hud.addProperty("enabled", true);
         }
         JsonObject target = object(root, "TargetHUD");
         JsonObject targetSettings = target == null ? null : object(target, "settings");
         if (targetSettings != null) for (var entry : targetSettings.entrySet()) {
            String key = "Target " + entry.getKey();
            if (!settings.has(key)) settings.add(key, entry.getValue().deepCopy());
         }
         return root;
      }
      private static JsonObject object(JsonObject parent, String key) {
         var value = parent.get(key); return value != null && value.isJsonObject() ? value.getAsJsonObject() : null;
      }
      private static boolean legacyEnabled(JsonObject module) {
         if (module == null) return false;
         var value = module.get("enabled");
         return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean() && value.getAsBoolean();
      }
   }

   public static final class InventoryHud {

      public InventoryHud() {

      }

      public void onEvent(Event event) {
         if (event == Events.f5 && !HudEditorScreen.active()) {
            renderInventory(Events.f5.m89());
         }
      }

      public InventoryHudLayout.Bounds bounds(int viewportWidth, int viewportHeight) {
         var placement = HudLayouts.INSTANCE.get(HudLayouts.Element.INVENTORY);
         return InventoryHudLayout.bounds(viewportWidth, viewportHeight,
            placement.x(), placement.y(), placement.scale());
      }

      public void renderInventory(GuiGraphicsExtractor graphics) {
         if (mc.player == null || mc.level == null || mc.gui.overlay() != null) {
            return;
         }
         // Fabric Loader alone does not expose mod assets through Minecraft's resource packs.
         // Upload from the jar on first rendering, when the texture manager is ready.
         Identifier panel = ModTextures.register("textures/hud/inventory.png");
         InventoryHudLayout.Bounds bounds = bounds(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
         HudLayouts.INSTANCE.drawn(HudLayouts.Element.INVENTORY,
            new HudLayouts.Box(bounds.x(), bounds.y(), bounds.width(), bounds.height()));
         HudBackdrop.inventory(bounds);
         graphics.pose().pushMatrix();
         try {
            graphics.pose().translate(bounds.x(), bounds.y());
            graphics.pose().scale(bounds.scale(), bounds.scale());
            // Separate strata keep the panel behind native item models and their decorations.
            graphics.nextStratum();
            graphics.blit(RenderPipelines.GUI_TEXTURED, panel, -InventoryHudLayout.SHADOW_MARGIN, -InventoryHudLayout.SHADOW_MARGIN,
               0, 0, InventoryHudLayout.WIDTH + InventoryHudLayout.SHADOW_MARGIN * 2, InventoryHudLayout.HEIGHT + InventoryHudLayout.SHADOW_MARGIN * 2,
               InventoryHudLayout.TEXTURE_WIDTH, InventoryHudLayout.TEXTURE_HEIGHT, InventoryHudLayout.TEXTURE_WIDTH, InventoryHudLayout.TEXTURE_HEIGHT);
            graphics.nextStratum();
         for (InventoryHudLayout.Slot slot : InventoryHudLayout.SLOTS) {
               ItemStack stack = mc.player.getInventory().getItem(slot.inventoryIndex());
               if (!stack.isEmpty()) {
                  graphics.item(stack, slot.x(), slot.y());
                  graphics.itemDecorations(mc.font, stack, slot.x(), slot.y());
               }
            }
         } finally {
            graphics.pose().popMatrix();
         }
      }

      public static final class InventoryHudLayout {
         public static final int WIDTH = 182;
         public static final int HEIGHT = 80;
         public static final int HEADER_HEIGHT = 18;
         public static final int RADIUS = 7;
         public static final int SHADOW_MARGIN = 8;
         public static final int TEXTURE_SCALE = 4;
         public static final int TEXTURE_WIDTH = (WIDTH + SHADOW_MARGIN * 2) * TEXTURE_SCALE;
         public static final int TEXTURE_HEIGHT = (HEIGHT + SHADOW_MARGIN * 2) * TEXTURE_SCALE;
         public static final int DEFAULT_X = 10;
         public static final int DEFAULT_Y = 83;
         public static final int BODY_COLOR = HudGlassStyle.BODY;
         public static final int HEADER_COLOR = HudGlassStyle.HEADER;
         public static final String PANEL_RESOURCE = "assets/samsara/textures/hud/inventory.png";
         public static final List<Slot> SLOTS = IntStream.range(0, 27)
            .mapToObj(index -> new Slot(index + 9, 3 + index % 9 * 20, 20 + index / 9 * 20))
            .toList();

         public record Slot(int inventoryIndex, int x, int y) { }

         public record Bounds(float x, float y, float scale) {
            public float width() { return WIDTH * this.scale; }
            public float height() { return HEIGHT * this.scale; }
            public boolean contains(double mouseX, double mouseY) {
               return mouseX >= this.x && mouseX < this.x + width()
                  && mouseY >= this.y && mouseY < this.y + height();
            }
         }

         private InventoryHudLayout() { }

         /** Fits smaller windows without changing the saved position or chosen scale. */
         public static Bounds bounds(int viewportWidth, int viewportHeight, double x, double y, double scale) {
            float availableWidth = Math.max(1, viewportWidth - 4);
            float availableHeight = Math.max(1, viewportHeight - 4);
            float fittedScale = (float)Math.min(Math.clamp(scale, .5, 2),
               Math.min(availableWidth / WIDTH, availableHeight / HEIGHT));
            float left = (float)Math.clamp(x, 2, Math.max(2, viewportWidth - 2 - WIDTH * fittedScale));
            float top = (float)Math.clamp(y, 2, Math.max(2, viewportHeight - 2 - HEIGHT * fittedScale));
            return new Bounds(left, top, fittedScale);
         }
      }
   }

   public static final class TargetHud {
      public final ModeSetting mode;
      private final BooleanSetting showArmor;
      private final OpaiTargetHudHealth opaiHealth = new OpaiTargetHudHealth();
      private Player opaiTarget;
      private OpaiTargetHudPainter.Bounds opaiBounds;
      private static final String f676 = "HP: 4";
      private float f698;
      private String f706;
      private static final String f665 = "Show Win or Loss";
      private Player f703;
      private final BooleanSetting f693;
      private static final String f666 = "Health Animation";
      private static final String f688 = "HP: 16";
      private static final String f685 = "HP: 13";
      private float f700;
      private static final String f671 = "HP: 20";
      private static final String f691 = "HP: 19";
      private final BooleanSetting f696;
      private static final String f682 = "HP: 10";
      private Player f705;
      private final BooleanSetting f692;
      private int f701;
      private static final String f677 = "HP: 5";
      private static final String f674 = "HP: 2";
      private static final String f683 = "HP: 11";
      private static final String f681 = "HP: 9";
      private int f702;
      private float f697;
      private static final String f687 = "HP: 15";
      private final BooleanSetting f695;
      private static final String f690 = "HP: 18";
      private static final String f668 = "\u00a7aW";
      private static final String f663 = "Outline";
      private static final String f675 = "HP: 3";
      private static final String f689 = "HP: 17";
      private Identifier f704;
      private static final String[] f713 = new String[]{
         TargetHud.f672,
         TargetHud.f673,
         f674,
         f675,
         f676,
         f677,
         TargetHud.f678,
         TargetHud.f679,
         TargetHud.f680,
         f681,
         f682,
         f683,
         TargetHud.f684,
         f685,
         TargetHud.f686,
         f687,
         f688,
         f689,
         f690,
         f691,
         f671
      };
      private static final String f667 = new String(new byte[0], StandardCharsets.UTF_8);
      private static final String f662 = "Theme Color";
      private static final String f680 = "HP: 8";
      private static final String f672 = "HP: 0";
      private static final String f679 = "HP: 7";
      private static final String f684 = "HP: 12";
      private static final String f669 = "\u00a7cL";
      private static final String f686 = "HP: 14";
      private int f707;
      private static final String f678 = "HP: 6";
      private int f711;
      private static final String f673 = "HP: 1";
      private int f710;
      private float f699;
      private static final String f670 = "Player";
      private static final String f661 = "TargetHUD";

      public void onEvent(Event var1) {
         if (var1 == Events.f5) {
            if (HudEditorScreen.active()) {
               return;
            }

            this.render(Events.f5);
         }

         if (var1 == Events.f3) {
            if (this.mode.m228("Opai")) return;

            KillAura var2 = FeatureManager.f26;
            boolean var3 = var2 != null && var2.f17 instanceof Player;
            float var4 = var3 ? 255.0F : 0.0F;
            this.f702 = this.f701;
            this.f701 = (int)Mth.lerp(0.5F, (float)this.f701, var4);
            if (!var3) {
               return;
            }

            Player var5 = (Player)var2.f17;
            this.f699 = this.f698;
            this.f698 = this.f697;
            this.f697 = this.f700;
            this.f700 = var5.getHealth();
         }
      }

      public void m207(GuiGraphicsExtractor var1, float var2, Player var3) {
         if (this.mode.m228("Opai")) {
            renderOpai(var1, var3);
            return;
         }
         if (var3 == null) return;
         String var8 = var3.getName().getString();
         int var9 = mc.font.width(var8);
         int var10 = Math.max(80, 38 + var9);
         this.f710 = var10;
         this.f711 = 42;
         var box = classicBox(var10);
         var1.pose().pushMatrix();
         try {
            var1.pose().translate(box.x(), box.y()); var1.pose().scale(box.width()/var10, box.width()/var10);
            int alpha = (int)(255 * HudEditorScreen.previewOpacity()), white = alpha << 24 | 0xFFFFFF;
            var1.fill(0,0,var10,42,((int)(170*HudEditorScreen.previewOpacity()))<<24);
            var1.text(mc.font,var8,34,5,white,false);
            var1.text(mc.font,"HP: " + Math.round(var3.getHealth()),var10-35,22,white,false);
            var1.fill(4,34,var10-4,38,white);
            new OpaiTargetHudSurface(var1,var3,8,OpaiTargetHudPainter.PANEL_RESOURCE,122,40,6,HudEditorScreen.previewOpacity()).face(4,4,26,0);
         } finally { var1.pose().popMatrix(); }
      }

      private HudLayouts.Box classicBox(int width) {
         var box = HudLayouts.INSTANCE.fit(HudLayouts.Element.TARGET,width,42,
            mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight(),true);
         HudLayouts.INSTANCE.drawn(HudLayouts.Element.TARGET,box); return box;
      }

      private void render(EventRender2D var1) {
         if (mc.player == null || mc.level == null || mc.gui.overlay() != null) {
            clearTarget();
            return;
         }
         if (this.mode.m228("Opai")) {
            KillAura aura = FeatureManager.f26;
            Player target = aura != null && aura.isEnabled() && aura.f17 instanceof Player player ? player : null;
            renderOpai(var1.m89(), target);
            return;
         }
         this.opaiTarget = null;
         this.opaiBounds = null;
         KillAura var2 = FeatureManager.f26;
         if (var2 != null) {
            boolean var3 = var2.f17 instanceof Player || this.f703 != null && this.f701 > 10;
            if (var3) {
               Player var4 = var2.f17 instanceof Player ? (Player)var2.f17 : this.f703;
               if (this.f705 != var4) {
                  this.f705 = var4;
                  this.f704 = null;
                  this.f706 = var4.getDisplayName().getString();
                  this.f707 = mc.font.width(this.f706);
                  mc.getSkinManager().get(var4.getGameProfile()).thenAccept(var1x -> var1x.ifPresent(var1xx -> this.f704 = var1xx.body().texturePath()));
               }

               int var5 = mc.getWindow().getGuiScaledWidth();
               int var6 = mc.getWindow().getGuiScaledHeight();
               int var7 = 0, var8 = 0;
               int var9 = Math.max(80, 48 + this.f707);
               float var10 = Mth.lerp(var1.m91(), this.f699, this.f698);
               float var11 = Mth.lerp(var1.m91(), this.f697, this.f700);
               float var12 = var4.getMaxHealth();
               float var13 = var11 / var12;
               float var14 = var10 / var12;
               GuiGraphicsExtractor var15 = var1.m89();
               int var16 = ColorUtil.m27();
               int var17 = this.f692.m215() ? var16 : ColorUtil.m26(var13);
               int var18 = this.f692.m215() ? var16 : ColorUtil.m26(var13);
               var18 = var18 & 16777215 | 1677721600;
               int var19 = (int)((float)this.f702 + (float)(this.f701 - this.f702) * var1.m91());
               var17 = var19 << 24 | var17 & 16777215;
               int var20 = var19 * 170 / 255 << 24;
               int var21 = Math.round(var11);
               if (var21 < 0) {
                  var21 = 0;
               } else if (var21 > 20) {
                  var21 = 20;
               }

               String var22 = f713[var21];
               var box = classicBox(var9);
               var15.pose().pushMatrix();
               try {
               var15.pose().translate(box.x(),box.y()); var15.pose().scale(box.width()/var9,box.width()/var9);
               var15.fill(var7, var8, var7 + var9, var8 + 42, var20);
               if (this.f693.m215()) {
                  var15.fill(var7, var8, var7 + 1, var8 + 42, var19 << 24 | var16 & 16777215);
                  var15.fill(var7 + var9 - 1, var8, var7 + var9, var8 + 42, var19 << 24 | var16 & 16777215);
                  var15.fill(var7, var8, var7 + var9, var8 + 1, var19 << 24 | var16 & 16777215);
                  var15.fill(var7, var8 + 41, var7 + var9, var8 + 42, var19 << 24 | var16 & 16777215);
               }

               if (this.f696.m215() && this.f701 > 200) {
                  var15.fill(var7 + 4, var8 + 34, var7 + 8 + (int)((float)(var9 - 12) * var14), var8 + 38, var18);
               }

               var15.fill(var7 + 4, var8 + 34, var7 + 8 + (int)((float)(var9 - 12) * var13), var8 + 38, var17);
               this.f710 = var9;
               this.f711 = 42;
               if (this.f695.m215()) {
                  String var23 = var4.getHealth() <= mc.player.getHealth() ? f668 : f669;
                  var15.text(mc.font, var23, var7 + var9 - 10, var8 + 5, var19 << 24 | 16777215, false);
               }

               var15.text(mc.font, this.f706, var7 + 34, var8 + 5, var19 << 24 | 16777215, false);
               var15.text(mc.font, var22, var7 + var9 - 35, var8 + 22, var17, false);
               if (this.f704 != null) {
                  var15.blit(RenderPipelines.GUI_TEXTURED, this.f704, var7 + 4, var8 + 4, 8.0F, 8.0F, 26, 26, 8, 8, 64, 64, var19 << 24 | 16777215);
               }

               this.f703 = var4;
               } finally { var15.pose().popMatrix(); }
            }
         }
      }

      public TargetHud(Hud owner) {
         int start = owner.settings.size();
         this.mode = new ModeSetting("Target Mode", owner, "Classic", new String[]{"Classic", "Opai"});
         this.mode.setDisplayName("Target HUD mode");
         this.showArmor = new BooleanSetting("Target Show Armor", owner, true);
         this.f692 = new BooleanSetting("Target " + f662, owner, false);
         this.f693 = new BooleanSetting("Target " + f663, owner, false);
         this.f695 = new BooleanSetting("Target " + f665, owner, false);
         this.f696 = new BooleanSetting("Target " + f666, owner, false);
         this.f706 = f667;
         for (Setting setting : owner.settings.subList(start + 1, owner.settings.size()))
            setting.setDisplayName(setting.getName());
         this.mode.setVisible(() -> owner.selected(Widget.TARGET_HUD));
         this.showArmor.setVisible(() -> owner.selected(Widget.TARGET_HUD) && this.mode.m228("Opai"));
         for (BooleanSetting setting : new BooleanSetting[]{this.f692, this.f693, this.f695, this.f696})
            setting.setVisible(() -> owner.selected(Widget.TARGET_HUD) && !this.mode.m228("Opai"));
      }

      private void renderOpai(GuiGraphicsExtractor graphics, Player target) {
         this.f703 = null;
         this.f701 = this.f702 = 0;
         if (mc.player == null || mc.level == null || mc.gui.overlay() != null || target == null
             || target.level() != mc.level || target.isRemoved()) {
            this.opaiTarget = null;
            this.opaiBounds = null;
            return;
         }
         long now = System.nanoTime() / 1_000_000L;
         if (target != this.opaiTarget) {
            this.opaiTarget = target;
            this.opaiHealth.reset(target.getHealth(), target.getMaxHealth(), now);
         }
         var health = this.opaiHealth.update(target.getHealth(), target.getMaxHealth(), now);
         var surface = new OpaiTargetHudSurface(graphics, target, OpaiTargetHudPainter.TEXT_SIZE,
            OpaiTargetHudPainter.PANEL_RESOURCE, OpaiTargetHudPainter.WIDTH, OpaiTargetHudPainter.HEIGHT,
            OpaiTargetHudPainter.RADIUS, HudEditorScreen.previewOpacity());
         String name = target.getName().getString();
         int viewportWidth = mc.getWindow().getGuiScaledWidth(), viewportHeight = mc.getWindow().getGuiScaledHeight();
         var content = OpaiTargetHudPainter.bounds(surface, name, health.maximum(), viewportWidth, viewportHeight, 0, 0);
         var box = HudLayouts.INSTANCE.fit(HudLayouts.Element.TARGET, content.width(), content.height(), viewportWidth, viewportHeight, true);
         HudLayouts.INSTANCE.drawn(HudLayouts.Element.TARGET, box);
         float scale = box.width() / content.width();
         this.opaiBounds = new OpaiTargetHudPainter.Bounds(0, 0, content.width(), content.height());
         this.f710 = this.opaiBounds.width();
         this.f711 = this.opaiBounds.height();
         HudBackdrop.widget(box, OpaiTargetHudPainter.RADIUS * scale);
         graphics.pose().pushMatrix();
         try {
            graphics.pose().translate(box.x(), box.y()); graphics.pose().scale(scale, scale);
            OpaiTargetHudPainter.paint(surface, this.opaiBounds, name, health,
               this.showArmor.m215(), ClickGui.currentOpaiPalette());
         } finally { graphics.pose().popMatrix(); }
      }

      private void clearTarget() {
         this.opaiTarget = this.f703 = this.f705 = null;
         this.opaiBounds = null;
         this.f704 = null;
         this.f701 = this.f702 = 0;
      }

      public void onDisable() { clearTarget(); }

      /** Two wall-time health responses: a quick fill and a lighter, delayed damage trail. */
      public static final class OpaiTargetHudHealth {
         private float goal;
         private float maximum = 20;
         private float fillFrom;
         private float trailFrom;
         private long changedAt;
         private boolean damage;
         private boolean initialized;

         public record Sample(float health, float trail, float maximum) {
            public float fraction() { return health / maximum; }
            public float trailFraction() { return trail / maximum; }
            public String label() { return format(health); }
         }

         public void reset(float health, float maximum, long now) {
            this.maximum = maximum(maximum);
            this.goal = health(health, this.maximum);
            this.fillFrom = this.trailFrom = this.goal;
            this.changedAt = now;
            this.damage = false;
            this.initialized = true;
         }

         public Sample update(float health, float maximum, long now) {
            float max = maximum(maximum);
            float value = health(health, max);
            if (!this.initialized) reset(value, max, now);
            if (value != this.goal || max != this.maximum) {
               Sample current = sample(now);
               this.damage = value < current.health();
               this.fillFrom = current.health();
               this.trailFrom = Math.max(current.health(), current.trail());
               this.goal = value;
               this.maximum = max;
               this.changedAt = now;
            }
            return sample(now);
         }

         public Sample sample(long now) {
            double elapsed = Math.max(0, now - this.changedAt);
            float fill = response(this.fillFrom, this.goal, elapsed, 65);
            float trail = this.damage
               ? Math.max(fill, response(this.trailFrom, this.goal, Math.max(0, elapsed - 100), 220)) : fill;
            return new Sample(health(fill, this.maximum), health(trail, this.maximum), this.maximum);
         }

         private static float response(float from, float to, double elapsed, double decay) {
            float value = (float)(to + (from - to) * Math.exp(-elapsed / decay));
            return Math.abs(value - to) < .025f ? to : value;
         }

         private static float maximum(float value) {
            return Float.isFinite(value) ? Math.max(1, value) : 20;
         }

         private static float health(float value, float maximum) {
            return Float.isFinite(value) ? Math.clamp(value, 0, maximum) : 0;
         }

         public static String format(float health) {
            float rounded = Math.round(health * 10) / 10f;
            return rounded == Math.round(rounded) ? Integer.toString(Math.round(rounded))
               : String.format(Locale.ROOT, "%.1f", rounded);
         }
      }

      public static final class OpaiTargetHudPainter {
         public static final int WIDTH = 122;
         public static final int HEIGHT = 40;
         public static final int RADIUS = 6;
         public static final int SHADOW_MARGIN = 8;
         public static final int TEXTURE_SCALE = 4;
         public static final float FACE_SIZE = 26;
         public static final float FACE_RADIUS = 4.5f;
         public static final float TEXT_SIZE = 8;
         public static final String PANEL_RESOURCE = "textures/hud/target-opai.png";
         public static final String BAR_RESOURCE = "textures/hud/target-opai-bar.png";

         public record Bounds(int x, int y, int width, int height) {
            public boolean contains(double x, double y) {
               return x >= this.x && x < this.x + this.width && y >= this.y && y < this.y + this.height;
            }
         }

         public interface Surface {
            float measure(String text);
            void panel(Bounds bounds);
            void face(float x, float y, float size, float radius);
            void armor(int slot, float x, float y);
            void text(String text, float x, float y, int color);
            void bar(float x, float y, float width, float height, int color);
         }

         private OpaiTargetHudPainter() { }

         public static Bounds bounds(Surface surface, String name, float maximum, int viewportWidth,
                                     int viewportHeight, int offsetX, int offsetY) {
            // Reserve a decimal so changes in the health label do not resize the shell.
            float labelWidth = labelWidth(surface, maximum);
            int width = Math.min(Math.max(WIDTH, (int)Math.ceil(35 + surface.measure(name) + labelWidth + 4)),
               Math.max(1, viewportWidth - 4));
            int x = Math.clamp(viewportWidth / 2 + offsetX, 2, Math.max(2, viewportWidth - width - 2));
            int y = Math.clamp(viewportHeight / 2 + offsetY, 2, Math.max(2, viewportHeight - HEIGHT - 2));
            return new Bounds(x, y, width, HEIGHT);
         }

         public static void paint(Surface surface, Bounds bounds, String name, OpaiTargetHudHealth.Sample health,
                                  boolean armor, OpaiStyle.Palette palette) {
            float x = bounds.x(), y = bounds.y();
            surface.panel(bounds);
            surface.face(x + 3, y + 3, FACE_SIZE, FACE_RADIUS);
            String label = health.label();
            String fittedName = fit(surface, name, bounds.width() - 35 - labelWidth(surface, health.maximum()) - 3);
            surface.text(fittedName, x + 32, y + 5, 0xFFFFFFFF);
            surface.text(label, x + 32 + surface.measure(fittedName) + 1.5f, y + 5, palette.accent());
            if (armor) {
               for (int slot = 0; slot < 4; slot++) surface.armor(slot, x + 31.5f + slot * 15.5f, y + 14.5f);
            }
            float barWidth = Math.max(0, bounds.width() - 7);
            surface.bar(x + 3, y + 32, barWidth, 5, 0x66000000);
            // Draw only the remaining damage interval: the theme fill covers the front of it.
            surface.bar(x + 3, y + 31.25f, barWidth * health.trailFraction(), 4.5f, 0x66FFFFFF);
            surface.bar(x + 3, y + 31.25f, barWidth * health.fraction(), 4.5f, palette.accent());
         }

         private static String fit(Surface surface, String text, float width) {
            if (surface.measure(text) <= width) return text;
            int end = text.length();
            while (end > 0 && surface.measure(text.substring(0, end) + "…") > width)
               end = text.offsetByCodePoints(end, -1);
            return end == 0 ? "" : text.substring(0, end) + "…";
         }

         private static float labelWidth(Surface surface, float maximum) {
            String label = OpaiTargetHudHealth.format(maximum);
            return Math.max(surface.measure(label.contains(".") ? label : label + ".0"), surface.measure("00.0"));
         }
      }

      public static final class OpaiTargetHudSurface implements OpaiTargetHudPainter.Surface {
         private static final Style FONT = Style.EMPTY.withFont(new FontDescription.Resource(
            Identifier.fromNamespaceAndPath("samsara", "terminal-google")));
         private final float fontScale;
         private final String panelResource;
         private final int panelWidth, panelHeight;
         private final float panelRadius;
         private final float opacity;
         private static final EquipmentSlot[] ARMOR = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
         };
         private final Minecraft mc = Minecraft.getInstance();
         private final GuiGraphicsExtractor graphics;
         private final Player player;

         public OpaiTargetHudSurface(GuiGraphicsExtractor graphics, Player player) {
            this(graphics, player, OpaiTargetHudPainter.TEXT_SIZE, OpaiTargetHudPainter.PANEL_RESOURCE, OpaiTargetHudPainter.WIDTH, OpaiTargetHudPainter.HEIGHT, OpaiTargetHudPainter.RADIUS, 1);
         }

         public OpaiTargetHudSurface(GuiGraphicsExtractor graphics, Player player, float fontSize,
            String panelResource, int panelWidth, int panelHeight, float panelRadius, float opacity) {
            this.graphics = graphics;
            this.player = player;
            this.fontScale = fontSize / 10;
            this.panelResource = panelResource; this.panelWidth = panelWidth; this.panelHeight = panelHeight;
            this.panelRadius = panelRadius; this.opacity = Math.clamp(opacity, 0, 1);
         }

         private int tint(int color) { return ((int)((color >>> 24) * this.opacity) << 24) | (color & 0xFFFFFF); }

         private static Component label(String text) { return Component.literal(text).withStyle(FONT); }

         @Override public float measure(String text) { return this.mc.font.width(label(text)) * this.fontScale; }

         @Override public void panel(OpaiTargetHudPainter.Bounds bounds) {
            Identifier texture = ModTextures.register(this.panelResource);
            int sourceWidth = (this.panelWidth + OpaiTargetHudPainter.SHADOW_MARGIN * 2) * OpaiTargetHudPainter.TEXTURE_SCALE;
            int sourceHeight = (this.panelHeight + OpaiTargetHudPainter.SHADOW_MARGIN * 2) * OpaiTargetHudPainter.TEXTURE_SCALE;
            // Stretch only the neutral center. Corners and the soft shadow keep their measured radius.
            int edge = Math.round((OpaiTargetHudPainter.SHADOW_MARGIN + this.panelRadius) * OpaiTargetHudPainter.TEXTURE_SCALE);
            int destinationWidth = (bounds.width() + OpaiTargetHudPainter.SHADOW_MARGIN * 2) * OpaiTargetHudPainter.TEXTURE_SCALE;
            int[] src = {0, edge, sourceWidth - edge, sourceWidth};
            int[] dst = {0, edge, destinationWidth - edge, destinationWidth};
            this.graphics.nextStratum();
            this.graphics.pose().pushMatrix();
            try {
               this.graphics.pose().translate(bounds.x() - OpaiTargetHudPainter.SHADOW_MARGIN, bounds.y() - OpaiTargetHudPainter.SHADOW_MARGIN);
               this.graphics.pose().scale(1f / OpaiTargetHudPainter.TEXTURE_SCALE, 1f / OpaiTargetHudPainter.TEXTURE_SCALE);
               for (int i = 0; i < 3; i++) {
                  this.graphics.blit(RenderPipelines.GUI_TEXTURED, texture, dst[i], 0, src[i], 0,
                     dst[i + 1] - dst[i], sourceHeight, src[i + 1] - src[i], sourceHeight, sourceWidth, sourceHeight, tint(0xFFFFFFFF));
               }
            } finally { this.graphics.pose().popMatrix(); }
            this.graphics.nextStratum();
         }

         @Override public void face(float x, float y, float size, float radius) {
            if (this.player == null) return;
            Identifier skin = (this.player instanceof AbstractClientPlayer client
               ? client.getSkin() : DefaultPlayerSkin.get(this.player.getGameProfile())).body().texturePath();
            NativePlayerFace.extract(this.graphics,skin,x,y,size,radius,this.opacity);
         }

         @Override public void armor(int slot, float x, float y) {
            if (this.player == null) return;
            var stack = this.player.getItemBySlot(ARMOR[slot]);
            if (stack.isEmpty()) return;
            this.graphics.pose().pushMatrix();
            try {
               this.graphics.pose().translate(x, y);
               this.graphics.pose().scale(.95f, .95f);
               this.graphics.item(this.player, stack, 0, 0, slot);
            } finally { this.graphics.pose().popMatrix(); }
         }

         @Override public void text(String text, float x, float y, int color) {
            // Vanilla treats near-zero text alpha as an unspecified opaque color.
            if (((tint(color) >>> 24) & 0xFF) < 4) return;
            this.graphics.pose().pushMatrix();
            try {
               this.graphics.pose().translate(x, y);
               this.graphics.pose().scale(this.fontScale, this.fontScale);
               this.graphics.text(this.mc.font, label(text), 0, 0, tint(color), false);
            } finally { this.graphics.pose().popMatrix(); }
         }

         @Override public void bar(float x, float y, float width, float height, int color) {
            if (width <= 0) return;
            color = tint(color);
            Identifier texture = ModTextures.register(OpaiTargetHudPainter.BAR_RESOURCE);
            this.graphics.pose().pushMatrix();
            try {
               this.graphics.pose().translate(x, y);
               this.graphics.pose().scale(1f / OpaiTargetHudPainter.TEXTURE_SCALE, 1f / OpaiTargetHudPainter.TEXTURE_SCALE);
               int w = Math.max(1, Math.round(width * OpaiTargetHudPainter.TEXTURE_SCALE));
               int h = Math.max(1, Math.round(height * OpaiTargetHudPainter.TEXTURE_SCALE));
               // Below one diameter, squeeze the pill; otherwise keep both round caps unscaled.
               if (w <= h) {
                  this.graphics.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0, 0, 0, w, h, 20, 20, 20, 20, color);
               } else {
                  int cap = h / 2;
                  this.graphics.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0, 0, 0, cap, h, 10, 20, 20, 20, color);
                  this.graphics.blit(RenderPipelines.GUI_TEXTURED, texture, cap, 0, 9, 0, w - cap * 2, h, 2, 20, 20, 20, color);
                  this.graphics.blit(RenderPipelines.GUI_TEXTURED, texture, w - cap, 0, 10, 0, cap, h, 10, 20, 20, 20, color);
               }
            } finally { this.graphics.pose().popMatrix(); }
         }
      }

      /** Rounded skin head extraction uses the same face/hat UVs as vanilla PlayerFaceExtractor. */
      public static final class NativePlayerFace {
         private NativePlayerFace() { }
         public static void extract(GuiGraphicsExtractor graphics, Identifier skin, float x,float y,float size,float radius,float opacity) {
            int scale=OpaiTargetHudPainter.TEXTURE_SCALE;
            graphics.pose().pushMatrix();
            try {
               graphics.pose().translate(x,y);graphics.pose().scale(1f/scale,1f/scale);
               int pixels=Math.round(size*scale),round=Math.clamp(Math.round(radius*scale),0,pixels/2);
               int color=((int)(255*Math.clamp(opacity,0,1))<<24)|0xFFFFFF;
               for (int u:new int[]{8,40}) {
                  for (int row=0;row<round;row++) {
                     double dy=round-row-.5;
                     int inset=(int)Math.round(round-Math.sqrt(round*round-dy*dy));
                     strip(graphics,skin,u,pixels,inset,row,pixels-inset*2,1,color);
                     strip(graphics,skin,u,pixels,inset,pixels-row-1,pixels-inset*2,1,color);
                  }
                  strip(graphics,skin,u,pixels,0,round,pixels,pixels-round*2,color);
               }
            } finally { graphics.pose().popMatrix(); }
         }
         private static void strip(GuiGraphicsExtractor graphics,Identifier skin,int u,int pixels,int x,int y,int width,int height,int color) {
            if (width<=0 || height<=0) return;
            float factor=8f/pixels;
            ((mixins.GuiGraphicsExtractorAccessor)graphics).samsara$blitTinted(RenderPipelines.GUI_TEXTURED,skin,x,x+width,y,y+height,
               (u+x*factor)/64,(u+(x+width)*factor)/64,(8+y*factor)/64,(8+(y+height)*factor)/64,color);
         }
      }
   }

   public static final class SessionHud {
      public SessionHud() { }
      public void onEvent(Event event) {
         if (event == Events.f5 && !HudEditorScreen.active()) renderSession(Events.f5.m89());
      }
      public void renderSession(GuiGraphicsExtractor graphics) {
         if (mc.player == null || mc.level == null || mc.gui.overlay() != null) return;
         var stats = SessionTracker.STATS;
         var surface = new OpaiTargetHudSurface(graphics, mc.player, SessionHudPainter.FONT_SIZE, "textures/hud/session.png",
            144, SessionHudPainter.HEIGHT, SessionHudPainter.RADIUS, HudEditorScreen.previewOpacity());
         SessionHudPainter.Canvas canvas = new SessionHudPainter.Canvas() {
            public float measure(String text) { return surface.measure(text); }
            public void panel(com.samsara.module.visual.Hud.TargetHud.OpaiTargetHudPainter.Bounds bounds) { surface.panel(bounds); }
            public void face(float x, float y, float size, float radius) { surface.face(x,y,size,radius); }
            public void armor(int slot, float x, float y) { }
            public void text(String text, float x, float y, int color) { surface.text(text,x,y,color); }
            public void bar(float x, float y, float w, float h, int color) { }
            public void icon(boolean skull, float x, float y, float size) {
               var texture = ModTextures.register("textures/hud/session-" + (skull ? "skull" : "wins") + ".png");
               graphics.pose().pushMatrix();
               try {
                  graphics.pose().translate(x,y); graphics.pose().scale(size/32, size/32);
                  graphics.blit(RenderPipelines.GUI_TEXTURED,texture,0,0,0,0,32,32,32,32,32,32,
                     ((int)(255 * HudEditorScreen.previewOpacity()) << 24) | 0xFFFFFF);
               } finally { graphics.pose().popMatrix(); }
            }
         };
         int width = SessionHudPainter.width(canvas, stats.time(), stats.kills(), stats.wins());
         var box = HudLayouts.INSTANCE.fit(HudLayouts.Element.SESSION,width,SessionHudPainter.HEIGHT,
            mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight(),false);
         HudLayouts.INSTANCE.drawn(HudLayouts.Element.SESSION,box);
         float scale = box.width()/width;
         HudBackdrop.widget(box,SessionHudPainter.RADIUS*scale);
         graphics.pose().pushMatrix();
         try {
            graphics.pose().translate(box.x(),box.y()); graphics.pose().scale(scale,scale);
            SessionHudPainter.paint(canvas,width,stats.time(),stats.kills(),stats.wins());
         } finally { graphics.pose().popMatrix(); }
      }

      /** Session timing and deduplicated combat/title facts, independent of HUD enablement. */
      public static final class SessionStats {
         private long lastTick = -1;
         private long playedMillis;
         private boolean wasPlaying;
         private int kills, wins;
         private UUID attacked;
         private int attackedLife;
         private long attackedAt, lastVictory = Long.MIN_VALUE / 2;
         private record Death(int life, long time) { }
         private final HashMap<UUID, Death> counted = new HashMap<>();
         public void tick(long now, boolean playing) {
            if (this.lastTick >= 0 && playing && this.wasPlaying) this.playedMillis += Math.clamp(now - this.lastTick, 0, 1000);
            this.lastTick = now;
            this.wasPlaying = playing;
            this.counted.entrySet().removeIf(entry -> now - entry.getValue().time() > 60000);
            if (!playing || now - this.attackedAt > 15000) this.attacked = null;
         }
         public void attack(UUID victim, long now) { attack(victim, 0, now); }
         public void attack(UUID victim, int life, long now) { this.attacked = victim; this.attackedLife = life; this.attackedAt = now; }
         public boolean death(UUID victim, long now) {
            return death(victim, 0, now);
         }
         public boolean death(UUID victim, int life, long now) {
            var previous = this.counted.get(victim);
            if (!victim.equals(this.attacked) || life != this.attackedLife || now - this.attackedAt > 15000
               || (previous != null && previous.life() == life)) return false;
            this.counted.put(victim, new Death(life, now)); this.attacked = null; this.kills++; return true;
         }
         public UUID attacked() { return this.attacked; }
         public void victory(String title, long now) {
            String value = title.replaceAll("§.", "").strip().toUpperCase(Locale.ROOT);
            if ((value.equals("VICTORY!") || value.equals("VICTORY") || value.equals("YOU WIN!")
                 || value.equals("胜利!") || value.equals("胜利！")) && now - this.lastVictory > 15000) {
               this.wins++; this.lastVictory = now;
            }
         }
         public void clearCombat() { this.attacked = null; this.counted.clear(); }
         public int kills() { return this.kills; }
         public int wins() { return this.wins; }
         public String time() { return formatTime(this.playedMillis / 1000); }
         public static String formatTime(long seconds) {
            long hours = seconds / 3600, minutes = seconds / 60 % 60, secs = seconds % 60;
            return (hours > 0 ? hours + "h " : "") + (hours > 0 || minutes > 0 ? minutes + "m " : "") + secs + "s";
         }
      }

      /** Receives actual local attacks/death events rather than treating entity unloading as a kill. */
      public static final class SessionTracker {
         public static final SessionStats STATS = new SessionStats();
         private static Object world;
         private SessionTracker() { }
         private static long now() { return System.nanoTime() / 1_000_000L; }
         public static void tick() {
            var mc = Minecraft.getInstance();
            if (world != mc.level) { world = mc.level; STATS.clearCombat(); }
            STATS.tick(now(), mc.player != null && mc.level != null && !mc.isPaused());
            if (mc.level != null && STATS.attacked() != null) {
               Entity entity = mc.level.getEntity(STATS.attacked());
               if (entity instanceof LivingEntity living && living.isDeadOrDying()) death(entity);
            }
         }
         public static void attack(Entity entity) {
            if (entity instanceof LivingEntity living && !living.isDeadOrDying() && entity != Minecraft.getInstance().player)
               STATS.attack(entity.getUUID(), entity.getId(), now());
         }
         public static void death(Entity entity) { if (entity != null) STATS.death(entity.getUUID(), entity.getId(), now()); }
         public static void title(String title) { STATS.victory(title, now()); }
      }

      public static final class SessionHudPainter {
         public static final int HEIGHT = 49;
         public static final float RADIUS = 7.5f;
         public static final float FONT_SIZE = 9.2f;
         public interface Canvas extends Surface { void icon(boolean skull, float x, float y, float size); }
         private SessionHudPainter() { }
         public static int width(Canvas surface, String time, int kills, int wins) {
            // Reserve the time bucket's full width to prevent resizing on each second.
            if (!time.contains("h ")) return 150;
            String reserved = "8".repeat(time.indexOf("h ")) + "h 59m 59s";
            return Math.max(150, (int)Math.ceil(58 + surface.measure("Time elapsed " + reserved)));
         }
         public static void paint(Canvas surface, int width, String time, int kills, int wins) {
            surface.panel(new com.samsara.module.visual.Hud.TargetHud.OpaiTargetHudPainter.Bounds(0, 0, width, HEIGHT));
            surface.face(6, 6, 34.5f, 5.5f);
            surface.text("Time elapsed " + time, 50, 13, 0xFFAAAAAA);
            surface.icon(true, 50, 28, 8);
            String killLabel = kills + " kills";
            surface.text(killLabel, 62, 28.5f, 0xFFFFFFFF);
            float winX = 62 + surface.measure(killLabel) + 10;
            surface.icon(false, winX, 28.5f, 6);
            surface.text(wins + " wins", winX + 11, 28.5f, 0xFFFFFFFF);
         }
      }
   }

   public static final class PotionStatus {
      private final PotionStatusMotion motion=new PotionStatusMotion();
      private boolean preview;
      public PotionStatus() { }
      public void onDisable() { motion.clear(); }
      public void onEvent(Event event) {
         if (event==Events.f5 && !HudEditorScreen.active()) renderStatus(Events.f5.m89());
      }
      public void renderStatus(GuiGraphicsExtractor graphics) {
         if (mc.player==null || mc.level==null || mc.gui.overlay()!=null) { motion.clear(); return; }
         var effects=new ArrayList<PotionStatusData.Effect>();
         for (var instance:mc.player.getActiveEffects()) {
            if (!instance.showIcon()) continue;
            var effect=instance.getEffect().value();var id=BuiltInRegistries.MOB_EFFECT.getKey(effect);
            String key=id.getNamespace().equals("minecraft")?id.getPath():id.toString();
            effects.add(new PotionStatusData.Effect(key,PotionStatusData.englishName(key),effect.getColor(),
               instance.getDuration(),instance.getAmplifier(),instance.isInfiniteDuration()));
         }
         boolean demo=effects.stream().noneMatch(effect->PotionStatusData.ICONS.contains(effect.key())) && HudEditorScreen.active();
         boolean initializeDemo=demo!=preview;
         if (initializeDemo) { motion.clear();preview=demo; }
         if (demo) effects.addAll(List.of(
            new PotionStatusData.Effect("night_vision","Night Vision",MobEffects.NIGHT_VISION.value().getColor(),9580,0,false),
            new PotionStatusData.Effect("speed","Speed",MobEffects.SPEED.value().getColor(),1780,1,false)));
         long now=System.nanoTime()/1_000_000L;
         var surface=new PotionStatusSurface(graphics,HudEditorScreen.previewOpacity());
         if (demo && initializeDemo) motion.update(effects,now-2000,surface::measure);
         var frame=motion.update(effects,now,surface::measure);
         if (frame.rows().isEmpty() || frame.height()<.01 || frame.width()<1) return;
         var box=HudLayouts.INSTANCE.fit(HudLayouts.Element.POTION,frame.width()*PotionStatusPainter.DEFAULT_SCALE,frame.height()*PotionStatusPainter.DEFAULT_SCALE,
            mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight(),false);
         HudLayouts.INSTANCE.drawn(HudLayouts.Element.POTION,box);
         float scale=box.width()/frame.width();
         for (var row:frame.rows()) {
            float x=box.x()+row.x()*scale,y=box.y()+(row.y()+frame.height()/2)*scale;
            HudBackdrop.widget(new HudLayouts.Box(x,y,row.width()*scale,PotionStatusMotion.HEIGHT*scale),PotionStatusPainter.RADIUS*scale);
            graphics.pose().pushMatrix();
            try {
               graphics.pose().translate(x,y);graphics.pose().scale(scale,scale);
               PotionStatusPainter.paint(surface,row);
            } finally { graphics.pose().popMatrix(); }
         }
      }

      /** English labels ignore the game language; unsupported icons are omitted. */
      public static final class PotionStatusData {
         public record Effect(String key, String name, int color, int ticks, int amplifier, boolean infinite) {
            public String title() { return name + (amplifier > 0 ? " " + roman(amplifier + 1) : ""); }
            public String timer() { return duration(ticks, infinite); }
            public int timerColor() { return !infinite && ticks <= 600 ? 0xFFFF5555 : 0xFFAAAAAA; }
            public String icon() { return ICONS.contains(key) ? "textures/hud/potion/" + key + ".png" : null; }
            public String titleTexture() {
               if (!ICONS.contains(key) || (amplifier!=0 && amplifier!=referenceAmplifier(key))) return null;
               return "textures/hud/potion/"+key+(amplifier>0?"-"+roman(amplifier+1).toLowerCase(Locale.ROOT):"")+"-title.png";
            }
            public float titleTextureX() { return key.equals("absorption") ? 126/6.4f : 20; }
         }
         public static final Set<String> ICONS = Set.of("speed", "slowness", "strength", "jump_boost", "regeneration",
            "fire_resistance", "water_breathing", "invisibility", "night_vision", "weakness", "poison",
            "absorption", "saturation", "haste", "mining_fatigue", "nausea", "resistance");
         public static int referenceAmplifier(String key) {
            return switch (key) {
               case "speed", "regeneration", "haste" -> 1;
               case "mining_fatigue" -> 2;
               case "absorption" -> 3;
               default -> 0;
            };
         }
         private static final Map<String, String> SPECIAL = Map.of("dolphins_grace", "Dolphin's Grace", "unluck", "Bad Luck");
         private PotionStatusData() { }
         public static String englishName(String key) {
            if (SPECIAL.containsKey(key)) return SPECIAL.get(key);
            StringBuilder result = new StringBuilder();
            for (String word : key.substring(key.indexOf(':') + 1).split("_")) {
               if (word.isEmpty()) continue;
               if (!result.isEmpty()) result.append(' ');
               result.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
            }
            return result.toString();
         }
         public static String duration(int ticks, boolean infinite) {
            if (infinite) return "∞";
            int seconds = Math.max(0, ticks) / 20;
            return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
         }
         private static String roman(int value) {
            if (value > 20) return Integer.toString(value);
            int[] values = {10,9,5,4,1}; String[] numerals = {"X","IX","V","IV","I"};
            StringBuilder result = new StringBuilder();
            for (int i=0;i<values.length;i++) while (value>=values[i]) { result.append(numerals[i]); value-=values[i]; }
            return result.toString();
         }
         public static int order(String key) {
            return switch (key) {
               case "night_vision" -> 0; case "speed" -> 1; case "slowness" -> 2;
               case "haste" -> 3; case "mining_fatigue" -> 4; case "strength" -> 5;
               case "jump_boost" -> 8; case "nausea" -> 9; case "regeneration" -> 10;
               case "resistance" -> 11; case "fire_resistance" -> 12;
               case "water_breathing" -> 13; case "invisibility" -> 14; case "weakness" -> 18; case "poison" -> 19;
               case "absorption" -> 22; case "saturation" -> 23;
               default -> 32;
            };
         }
      }

      /** Separate single/group exits with delayed list reflow. */
      public static final class PotionStatusMotion {
         public static final float HEIGHT = 20, GAP = 2.25f;
         private static final long REFLOW_DELAY = 300;
         private static final double REFLOW_DURATION = .200;

         private static final class Spring {
            private double value, velocity;
            private long last;
            Spring(double value, double velocity, long now) {
               this.value=value;this.velocity=velocity;this.last=now;
            }
            void advance(long now) {
               double dt=Math.max(0,now-last)/1000.0, offset=value-1, rate=18;
               double decay=Math.exp(-rate*dt),linear=velocity+rate*offset;
               value=1+(offset+linear*dt)*decay;
               velocity=(velocity-rate*linear*dt)*decay;
               last=now;
            }
         }

         private static final class Space {
            private Spring growth;
            private long collapseAt;
            private boolean retiring, collapsing;
            private double value, velocity, collapseFrom;
            Space(boolean firstGroup,long now) {
               value=firstGroup?1:0;growth=new Spring(value,0,now);
            }
            void advance(long now) {
               if (!collapsing) {
                  growth.advance(retiring?Math.min(now,collapseAt):now);
                  value=growth.value;velocity=growth.velocity;
                  if (retiring && now>=collapseAt) { collapseFrom=value;collapsing=true; }
               }
               if (collapsing) {
                  double t=Math.clamp((now-collapseAt)/1000.0/REFLOW_DURATION,0,1);
                  value=collapseFrom*(1-t);velocity=t<1?-collapseFrom/REFLOW_DURATION:0;
               }
            }
            void present(boolean present,long now) {
               if (present && retiring) {
                  growth=new Spring(value,velocity,now);retiring=false;collapsing=false;
               } else if (!present && !retiring) {
                  retiring=true;collapseAt=now+REFLOW_DELAY;
               }
            }
         }

         private static final class Slide {
            private static final double ENTRY_DURATION=.430, GROUP_EXIT_DURATION=.170, BACK=1.70158;
            private static final double SINGLE_EXIT_SPEED=530;
            private double value,velocity,start,target,startVelocity,endVelocity,duration;
            private long changedAt;
            private boolean moving,groupExit;
            Slide(long now) { changedAt=now; }
            void advance(long now) {
               if (!moving) return;
               double t=Math.clamp((now-changedAt)/1000.0/duration,0,1),d=target-start;
               double a=startVelocity*duration,b=endVelocity*duration;
               double raw=start+a*t+(3*d-2*a-b)*t*t+(-2*d+a+b)*t*t*t;
               double speed=(a+2*(3*d-2*a-b)*t+3*(-2*d+a+b)*t*t)/duration;
               // Hold group cards in place during exit anticipation.
               value=groupExit?Math.min(start,raw):raw;
               velocity=groupExit && raw>start?0:speed;
               if (t>=1) { value=target;velocity=0;moving=false; }
            }
            void to(double target,boolean groupExit,float distance,long now) {
               if (target==this.target) return;
               start=value;this.groupExit=target==0 && groupExit;
               duration=target==1?ENTRY_DURATION:this.groupExit?GROUP_EXIT_DURATION:distance/SINGLE_EXIT_SPEED;
               double d=target-start;
               startVelocity=moving?velocity:target==1?(3+BACK)*d/duration:this.groupExit?0:3*d/duration;
               endVelocity=this.groupExit?(3+BACK)*d/duration:0;
               this.target=target;changedAt=now;moving=true;
            }
         }

         private static final class Entry {
            PotionStatusData.Effect effect;
            final Slide slide;
            final Space space;
            boolean present,justRemoved;
            float exitY,width;
            Entry(PotionStatusData.Effect effect,long now,boolean firstGroup) {
               this.effect=effect;slide=new Slide(now);space=new Space(firstGroup,now);
            }
         }
         public record Row(PotionStatusData.Effect effect, float x, float y, float width, float opacity) { }
         public record Frame(List<Row> rows, float width, float height) { }
         private final Map<String,Entry> entries=new HashMap<>();
         public Frame update(List<PotionStatusData.Effect> effects, long now, ToDoubleFunction<String> measure) {
            boolean firstGroup=entries.isEmpty();
            long previousCount=entries.values().stream().filter(entry->entry.present).count();
            var present=new HashSet<String>();
            for (var effect : effects) {
               if (!PotionStatusData.ICONS.contains(effect.key())) continue;
               var entry=entries.computeIfAbsent(effect.key(), key -> new Entry(effect,now,firstGroup));
               entry.effect=effect;present.add(effect.key());
            }
            for (var entry:entries.values()) {
               entry.slide.advance(now);entry.space.advance(now);
               boolean next=present.contains(entry.effect.key());
               entry.justRemoved=entry.present && !next;
               entry.width=PotionStatusPainter.width(entry.effect,measure);
               entry.slide.to(next?1:0,Math.max(previousCount,present.size())>1,entry.width+8,now);
               entry.space.present(next,now);entry.present=next;
            }
            // A final card exits at its existing vertical anchor; there is no shrinking empty group.
            if (present.isEmpty() && entries.values().stream().noneMatch(entry->entry.slide.moving)) {
               clear();return new Frame(List.of(),0,0);
            }
            entries.values().removeIf(entry->!entry.present && !entry.slide.moving && entry.space.value<=.001);
            var sorted=new ArrayList<>(entries.values());
            sorted.sort(Comparator.comparingInt((Entry entry)->PotionStatusData.order(entry.effect.key()))
               .thenComparing(entry->entry.effect.key()));
            float height=0,width=0;
            for (var entry:sorted) height+=(HEIGHT+GAP)*(float)Math.clamp(entry.space.value,0,1);
            height=Math.max(0,height-GAP);
            var rows=new ArrayList<Row>();float y=-height/2;
            for (var entry:sorted) {
               if (entry.justRemoved) entry.exitY=y;
               if (entry.present || entry.slide.moving) {
                  float w=entry.width;
                  rows.add(new Row(entry.effect,-(w+8)*(1-(float)entry.slide.value),entry.present?y:entry.exitY,w,1));width=Math.max(width,w);
               }
               y+=(HEIGHT+GAP)*(float)Math.clamp(entry.space.value,0,1);
            }
            return new Frame(List.copyOf(rows),width,height);
         }
         public void clear() { entries.clear(); }
      }

      /** Apply the default scale before the user-selected HUD scale. */
      public static final class PotionStatusPainter {
         public static final float DEFAULT_SCALE=1.6f;
         public static final float FONT_SIZE=5.75f, RADIUS=3.75f;
         public interface Surface {
            float measure(String text);
            void panel(float width,int color);
            void icon(String resource,int color);
            void text(String text,float x,float y,int color);
            default void title(PotionStatusData.Effect effect,int color) { text(effect.title(),19.75f,4.75f,color); }
            default void timer(PotionStatusData.Effect effect) { text(effect.timer(),19.75f,11.75f,effect.timerColor()); }
         }
         private PotionStatusPainter() { }
         public static float width(PotionStatusData.Effect effect,java.util.function.ToDoubleFunction<String> measure) {
            // Fixed base widths prevent panel jitter from font advance rounding.
            int pixels=switch (effect.key()) {
               case "jump_boost" -> 365; case "speed" -> 268; case "strength" -> 312;
               case "weakness" -> 334; case "night_vision" -> 370; case "water_breathing" -> 443;
               case "slowness" -> 320; case "fire_resistance" -> 423; case "invisibility" -> 339;
               case "poison" -> 275; case "regeneration" -> 423;
               case "absorption" -> 398; case "saturation" -> 341; case "haste" -> 289;
               case "mining_fatigue" -> 456; case "nausea" -> 286; case "resistance" -> 347;
               default -> 0;
            };
            if (pixels==0) return (float)(23.75+Math.max(measure.applyAsDouble(effect.title()),measure.applyAsDouble(effect.timer())));
            int amplifier=effect.key().equals("speed")?0:PotionStatusData.referenceAmplifier(effect.key());
            String referenceTitle=new PotionStatusData.Effect(effect.key(),effect.name(),effect.color(),0,amplifier,false).title();
            return (float)Math.max(23.75+measure.applyAsDouble(effect.timer()),pixels/6.4
               +measure.applyAsDouble(effect.title())-measure.applyAsDouble(referenceTitle));
         }
         public static void paint(Surface surface, PotionStatusMotion.Row row) {
            var effect=row.effect();int color=0xFF000000 | effect.color() & 0xFFFFFF;
            surface.panel(row.width(),color);
            if (effect.icon()!=null) surface.icon(effect.icon(),color);
            surface.title(effect,color);
            surface.timer(effect);
         }
      }

      public static final class PotionStatusSurface implements PotionStatusPainter.Surface {
         private final GuiGraphicsExtractor graphics;
         private final OpaiTargetHudSurface text;
         private final float opacity;
         private static final java.util.Map<String,int[]> TEXTURE_SIZES=new java.util.HashMap<>();
         public PotionStatusSurface(GuiGraphicsExtractor graphics,float opacity) {
            this.graphics=graphics;this.opacity=opacity;
            this.text=new OpaiTargetHudSurface(graphics,null,PotionStatusPainter.FONT_SIZE,"textures/hud/potion/glass.png",80,20,3.75f,opacity);
         }
         @Override public float measure(String value) { return text.measure(value); }
         @Override public void text(String value,float x,float y,int color) { text.text(value,x,y,color); }
         @Override public void title(PotionStatusData.Effect effect,int color) {
            String resource=effect.titleTexture();
            if (resource==null) { PotionStatusPainter.Surface.super.title(effect,color);return; }
            sprite(resource,effect.titleTextureX(),3.125f,color);
         }
         @Override public void timer(PotionStatusData.Effect effect) {
            if (effect.infinite()) { PotionStatusPainter.Surface.super.timer(effect);return; }
            float x=20;
            for (char digit:effect.timer().toCharArray()) {
               sprite("textures/hud/potion/digit-"+(digit==':'?"colon":digit)+".png",x,10.15625f,effect.timerColor());
               x+=(digit==':'?10:22)/6.4f;
            }
         }
         private void sprite(String resource,float x,float y,int color) {
            int[] size=TEXTURE_SIZES.computeIfAbsent(resource,path->{
               try(var image=ModTextures.read(net.minecraft.resources.Identifier.fromNamespaceAndPath("samsara",path))) {
                  if (image==null) throw new IllegalStateException("Missing referenced potion glyph: "+path);
                  return new int[]{image.getWidth(),image.getHeight()};
               }
            });
            graphics.pose().pushMatrix();
            try {
               graphics.pose().translate(x,y);graphics.pose().scale(1f/6.4f,1f/6.4f);
               graphics.blit(RenderPipelines.GUI_TEXTURED,ModTextures.register(resource),0,0,0,0,size[0],size[1],size[0],size[1],size[0],size[1],tint(color));
            } finally { graphics.pose().popMatrix(); }
         }
         @Override public void icon(String resource,int color) {
            graphics.pose().pushMatrix();
            try {
               graphics.pose().scale(20f/128,20f/128);
               graphics.blit(RenderPipelines.GUI_TEXTURED,ModTextures.register(resource),0,0,0,0,128,128,128,128,128,128,tint(color));
            } finally { graphics.pose().popMatrix(); }
         }
         private int tint(int color) { return ((int)((color>>>24)*opacity)<<24)|(color&0xFFFFFF); }
         @Override public void panel(float width,int color) {
            graphics.nextStratum();
            panelTexture("shadow",width,0xFFFFFFFF);
            panelTexture("glass",width,color);
            graphics.nextStratum();
         }
         private void panelTexture(String name,float width,int color) {
            var texture=ModTextures.register("textures/hud/potion/"+name+".png");
            int sourceWidth=720,sourceHeight=240,edge=70,destinationWidth=Math.round((width+10)*8);
            int[] src={0,edge,sourceWidth-edge,sourceWidth},dst={0,edge,destinationWidth-edge,destinationWidth};
            graphics.pose().pushMatrix();
            try {
               graphics.pose().translate(-5,-5);graphics.pose().scale(1f/8,1f/8);
               for (int i=0;i<3;i++) graphics.blit(RenderPipelines.GUI_TEXTURED,texture,dst[i],0,src[i],0,
                  dst[i+1]-dst[i],sourceHeight,src[i+1]-src[i],sourceHeight,sourceWidth,sourceHeight,tint(color));
            } finally { graphics.pose().popMatrix(); }
         }
      }
   }
}
