package com.samsara.ui.clickgui.opai;

import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.module.FeatureManager;
import com.samsara.setting.BooleanSetting;
import com.samsara.setting.ChoiceSetting;
import com.samsara.setting.ModeSetting;
import com.samsara.setting.NumberSetting;
import com.samsara.setting.Setting;
import com.samsara.ui.NanoGui;
import com.samsara.ui.clickgui.opai.OpaiContentLayout.Entry;
import com.samsara.ui.clickgui.opai.OpaiContentLayout.Kind;
import com.samsara.ui.clickgui.opai.OpaiContentLayout.Rect;
import com.samsara.ui.clickgui.opai.OpaiContentLayout.Scrollbar;
import com.samsara.ui.clickgui.opai.OpaiInteraction.Action;
import com.samsara.util.render.FontRepository;
import com.samsara.util.render.NVGRenderer;
import com.samsara.util.render.NVGTextRenderer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.lwjgl.sdl.SDLScancode;

import static com.samsara.ui.clickgui.opai.OpaiContentLayout.*;
import static com.samsara.ui.clickgui.opai.OpaiLayout.*;
import static com.samsara.ui.clickgui.opai.OpaiStyle.*;
import static org.lwjgl.nanovg.NanoVG.*;

public class OpaiClickGuiScreen extends Screen implements NanoGui {
   private static final Category[] CATEGORY_ORDER = {
      Category.COMBAT, Category.MOVEMENT, Category.PLAYER, Category.VISUAL, Category.MISC
   };
   private static final String[] CATEGORY_NAMES = {"Combat", "Movement", "Player", "Visual", "Misc"};

   private final ModeSetting colorSetting;
   private OpaiStyle.Palette palette = OpaiStyle.LAVENDER;
   private final List<OpaiColumn> columns = new ArrayList<>();
   private final OpaiMotion menuAnimation = new OpaiMotion(0, 30);
   private final OpaiConfigPanel configs = new OpaiConfigPanel(new OpaiConfigRepository());
   private EditBox configName;
   private double openedAt;
   private float menuScale = 1;
   private float menuOpacity;
   private double closeStarted;
   private float closingAlpha, closingScale;
   private float transformCenterY;
   private boolean configFront;
   private float colW = COL_W;
   private float maxBodyH;
   private float horizontalScroll;
   private float maxHorizontalScroll;
   private boolean shiftDown;
   private OpaiColumn draggedColumn;
   private float dragX;
   private float dragY;
   private OpaiColumn draggedScrollbar;
   private float scrollbarGrab;
   private NumberSetting draggingSlider;
   private OpaiRow sliderRow;
   private OpaiColumn sliderColumn;
   private OpaiRow bindingRow;
   private final Map<BooleanSetting, OpaiMotion> toggleAnims = new HashMap<>();
   private final Map<NumberSetting, OpaiMotion> sliderAnims = new HashMap<>();
   private final Map<Setting, OpaiFeedback> controlFeedback = new HashMap<>();
   private boolean closing;
   private Screen closeDestination;
   private final com.samsara.ui.hud.editor.HudEditButton hudEdit = new com.samsara.ui.hud.editor.HudEditButton();
   private boolean initialized;
   private boolean detached = true;
   private int layoutWidth, layoutHeight;
   private double mouseX;
   private double mouseY;
   private double lastFrameTime;

   public OpaiClickGuiScreen(ModeSetting colorSetting) {
      super(Component.literal("ClickGUI"));
      this.colorSetting = colorSetting;
   }

   @Override
   protected void init() {
      super.init();
      this.closeDestination = null;
      this.hudEdit.open();
      if (this.initialized) {
         Viewport layout = viewport(this.width, this.height, CATEGORY_ORDER.length);
         Viewport previous = viewport(this.layoutWidth, this.layoutHeight, CATEGORY_ORDER.length);
         float previousScroll = this.horizontalScroll;
         this.colW = layout.columnWidth();
         this.maxBodyH = layout.maxBodyHeight();
         this.transformCenterY = layout.top() + (HEADER_H + layout.maxBodyHeight()) / 2;
         this.maxHorizontalScroll = horizontalOverflow(this.width, CATEGORY_ORDER.length, layout);
         this.horizontalScroll = Math.clamp(this.horizontalScroll, 0, this.maxHorizontalScroll);
         if (this.width != this.layoutWidth || this.height != this.layoutHeight) {
            for (OpaiColumn column : this.columns) {
               column.x = Math.clamp(column.x + layout.left() - previous.left() + previousScroll - this.horizontalScroll,
                  4 - this.maxHorizontalScroll, Math.max(4, this.width - this.colW - 4 + this.maxHorizontalScroll));
               column.y = Math.clamp(column.y + layout.top() - previous.top(), 4,
                  Math.max(4, this.height - HEADER_H - ROW_H - 8));
            }
         }
         this.configs.fitViewport(this.width, this.height);
         this.configs.dock(this.width, layout.top() - 2);
         this.configs.refresh();
         if (this.detached) {
            this.configs.collapse();
            this.configFront = true;
            this.openedAt = OpaiMotion.now();
            this.menuAnimation.snap(0, this.openedAt);
            this.menuAnimation.approach(1, this.openedAt);
         }
         this.detached = false;
         this.closing = false;
         this.lastFrameTime = OpaiMotion.now();
         this.layoutWidth = this.width;
         this.layoutHeight = this.height;
         this.refreshLayouts();
         return;
      }
      this.columns.clear();
      this.bindingRow = null;
      this.clearDrag();
      Viewport layout = viewport(this.width, this.height, CATEGORY_ORDER.length);
      this.colW = layout.columnWidth();
      this.maxBodyH = layout.maxBodyHeight();
      this.horizontalScroll = 0;
      this.maxHorizontalScroll = horizontalOverflow(this.width, CATEGORY_ORDER.length, layout);
      this.shiftDown = false;
      for (int i = 0; i < CATEGORY_ORDER.length; i++) {
         OpaiColumn column = new OpaiColumn(CATEGORY_NAMES[i]);
         for (Feature module : FeatureManager.getModules()) {
            // Keep hidden modules in the cached screen so loading another config can reveal them.
            if (module.getCategory() == CATEGORY_ORDER[i]) {
               column.rows.add(new OpaiRow(module));
            }
         }
         column.rows.sort(Comparator.comparing(row -> row.module.getName(), String.CASE_INSENSITIVE_ORDER));
         column.x = com.samsara.ui.hud.ClickGuiLayouts.x("opai:" + column.name, layout.left() + i * (this.colW + COL_GAP));
         column.y = Math.clamp(com.samsara.ui.hud.ClickGuiLayouts.y("opai:" + column.name, layout.top()), 4, Math.max(4, this.height - HEADER_H - ROW_H - 8));
         this.columns.add(column);
      }
      this.transformCenterY = layout.top() + (HEADER_H + layout.maxBodyHeight()) / 2;
      this.configName = new EditBox(this.minecraft.font, 0, 0, 120, 20, Component.literal("Configuration name"));
      this.configName.setMaxLength(69);
      this.configName.setValue(this.configs.input());
      if (!this.initialized) {
         this.initialized = true;
         this.openedAt = OpaiMotion.now();
         this.menuAnimation.snap(0, this.openedAt);
         this.configs.position(Math.max(4, this.width - this.colW - layout.left()), 8, this.colW);
         this.configs.refresh();
      }
      this.configs.fitViewport(this.width, this.height);
      this.configs.dock(this.width, layout.top() - 2);
      this.configFront = true;
      this.lastFrameTime = OpaiMotion.now();
      this.closing = false;
      this.detached = false;
      this.layoutWidth = this.width;
      this.layoutHeight = this.height;
      this.refreshLayouts();
   }

   private static Kind settingKind(Setting setting) {
      if (setting instanceof BooleanSetting) {
         return Kind.BOOLEAN;
      }
      if (setting instanceof NumberSetting) {
         return Kind.NUMBER;
      }
      return setting instanceof ChoiceSetting ? Kind.MODE : null;
   }

   private void refreshLayouts() {
      double now = OpaiMotion.now();
      for (OpaiColumn column : this.columns) {
         float columnFactor = column.expand.approach(column.expanded ? 1 : 0, now);
         OpaiContentLayout<Target> content = new OpaiContentLayout<>();
         for (OpaiRow row : column.rows) {
            if (row.module.isHidden()) continue;
            content.add(new Target(row, null), Kind.MODULE);
            float rowFactor = row.expand.approach(row.expanded ? 1 : 0, now);
            for (Setting setting : row.module.settings) {
               if (setting instanceof ChoiceSetting) {
                  row.modes.computeIfAbsent(setting, ignored -> new OpaiMotion(0, 40)).approach(
                     row.expanded && column.expanded && setting.isVisible() && row.openMode == setting ? 1 : 0, now);
               }
            }
            if (rowFactor <= 0) continue;
            if (row.openMode != null && !row.openMode.isVisible()) {
               row.openMode = null;
            }
            OpaiContentLayout<Target> section = new OpaiContentLayout<>();
            for (Setting setting : row.module.settings) {
               Kind kind = settingKind(setting);
               if (!setting.isVisible() || kind == null) {
                  continue;
               }
               Target target = new Target(row, setting);
               section.add(target, kind);
            }
            content.reveal(section, rowFactor);
         }
         column.content = content;
         float available = Math.max(ROW_H + FOOTER_H, this.height - column.y - HEADER_H - 8);
         column.targetBodyH = content.bodyHeight(Math.min(this.maxBodyH, available));
         column.bodyH = column.targetBodyH * columnFactor;
         column.maxScroll = content.maxScroll(column.targetBodyH);
         if (columnFactor <= 0.01f && !column.expanded) {
            column.scroll = column.scrollTarget = 0;
         }
         column.scrollTarget = Math.clamp(column.scrollTarget, 0, column.maxScroll);
         column.dropdowns = this.layoutDropdowns(column);
      }
      this.configs.update(now);
      if (this.draggingSlider != null && (!this.sliderRow.expanded || this.sliderRow.module.isHidden() || !this.draggingSlider.isVisible())) {
         this.clearDrag();
      }
   }

   @Override
   public void renderNano() {
      double now = OpaiMotion.now();
      float dt = (float)Math.max(0, now - this.lastFrameTime);
      this.lastFrameTime = now;
      float smoothing = 1 - (float)Math.exp(-dt * 0.014);
      float alpha = this.closing ? this.closingAlpha * (float)Math.exp(-30 * (now - this.closeStarted) / 1000)
         : this.menuAnimation.approach(1, now);
      this.menuOpacity = alpha;
      this.menuScale = this.closing ? this.closingScale + .6f * (1 - alpha / Math.max(.001f, this.closingAlpha))
         : OpaiMotion.openingScale(now - this.openedAt);
      if (alpha <= 0.01f) {
         if (this.closing) {
            this.minecraft.gui.setScreen(this.closeDestination);
         }
         return;
      }
      for (OpaiColumn column : this.columns) {
         column.scroll += (column.scrollTarget - column.scroll) * smoothing;
      }
      this.refreshLayouts();
      this.configName.setFocused(this.configs.focused());
      this.syncConfigEditor();
      NVGRenderer.globalAlpha(alpha);
      try {
         NVGRenderer.scale(this.menuScale, this.width / 2f, this.transformCenterY, 0, 0, () -> {
            Canvas canvas = new Canvas(null);
            this.palette = OpaiStyle.palette(this.colorSetting.m224());
            if (!this.configFront) this.drawConfigs(canvas);
            this.drawColumns(canvas);
            if (this.configFront) this.drawConfigs(canvas);
         });
      } finally {
         NVGRenderer.globalAlpha(1);
      }
   }

   private void drawConfigs(Canvas canvas) {
      this.configs.paint(canvas, this.palette, this.mouseX, this.mouseY, OpaiMotion.now());
   }

   public void drawHudEditButton() { this.hudEdit.draw(this.width, this.height, this.menuOpacity, this.closing); }

   private void drawColumns(Canvas canvas) {
      this.palette = OpaiStyle.palette(this.colorSetting.m224());
      for (OpaiColumn column : this.columns) {
         float bodyY = column.y + HEADER_H;
         if (column.bodyH <= 0.5f) {
            canvas.rounded(column.x, column.y, this.colW, HEADER_H, RADIUS, this.palette.header());
            canvas.text(column.name, column.x + TEXT_PAD, column.y + HEADER_H / 2,
               HEADER_TEXT_SIZE, this.colW - TEXT_PAD * 2, this.palette.text());
            continue;
         }
         canvas.panel(column.x, bodyY, this.colW, column.bodyH, false, this.palette.body());
         canvas.panel(column.x, column.y, this.colW, HEADER_H, true, this.palette.header());
         canvas.text(column.name, column.x + TEXT_PAD, column.y + HEADER_H / 2,
            HEADER_TEXT_SIZE, this.colW - TEXT_PAD * 2, this.palette.text());
         if (column.bodyH > FOOTER_H + 0.5f) {
            canvas.clip(column.x + 1, bodyY, this.colW - 2, column.bodyH - FOOTER_H, () -> {
               for (Entry<Target> entry : column.content.entries()) {
                  float y = bodyY + entry.top() - column.scroll;
                  if (entry.visible() && bodyY + entry.visibleBottom() - column.scroll > bodyY
                        && bodyY + entry.visibleTop() - column.scroll < bodyY + column.bodyH - FOOTER_H) {
                     canvas.clip(column.x, bodyY + entry.visibleTop() - column.scroll, this.colW,
                        entry.visibleBottom() - entry.visibleTop(), () -> this.drawEntry(canvas, column, entry, y));
                  }
               }
            });
         }
         if (column.maxScroll > 0) {
            Scrollbar bar = scrollbar(column.bodyH, column.content.height(), column.scroll);
            canvas.rounded(column.x + this.colW - 3.5f, bodyY + bar.top(), 2.5f, bar.height(), 1.25f, this.palette.scrollbar());
         }
         this.drawDropdowns(canvas, column);
      }
   }

   /** Options float over following controls. Their reveal never contributes to the column layout. */
   private List<Dropdown> dropdowns(OpaiColumn column) {
      return column.dropdowns;
   }

   private List<Dropdown> layoutDropdowns(OpaiColumn column) {
      List<Dropdown> dropdowns = new ArrayList<>();
      for (Entry<Target> entry : column.content.entries()) {
         if (entry.kind() != Kind.MODE || !(entry.target().setting() instanceof ChoiceSetting choices)) continue;
         Setting mode = entry.target().setting();
         OpaiRow row = entry.target().row();
         OpaiMotion motion = row.modes.get(mode);
         if (motion == null || motion.value() <= 0 || choices.options().length == 0) continue;
         float y = column.y + HEADER_H + entry.top() - column.scroll;
         Rect field = field(column.x, y, this.colW);
         if (entry.visibleBottom() < entry.top() + FIELD_Y + FIELD_H - .5f
             || field.y() < column.y + HEADER_H || field.y() + FIELD_H > column.y + HEADER_H + column.bodyH - FOOTER_H) continue;
         float fullHeight = Math.min(choices.options().length * OPTION_H, Math.max(0, this.height - 4 - field.y() - FIELD_H));
         if (fullHeight <= 0) continue;
         float scroll = Math.clamp(row.modeScroll.getOrDefault(mode, 0f), 0, choices.options().length * OPTION_H - fullHeight);
         dropdowns.add(new Dropdown(column, row, mode, new Rect(field.x(), field.y() + FIELD_H, field.width(), fullHeight * motion.value()), scroll, fullHeight));
      }
      return dropdowns;
   }

   private Dropdown dropdownAt(OpaiColumn column) {
      List<Dropdown> menus = dropdowns(column);
      for (int i = menus.size() - 1; i >= 0; i--) if (menus.get(i).bounds().contains(this.mouseX, this.mouseY)) return menus.get(i);
      return null;
   }

   private void drawDropdowns(Canvas canvas, OpaiColumn column) {
      for (Dropdown menu : dropdowns(column)) {
         Rect bounds = menu.bounds();
         canvas.rounded(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 2, this.choiceField(menu.choices()));
         canvas.clip(bounds.x(), bounds.y(), bounds.width(), bounds.height(), () -> {
            for (int option = 0; option < menu.choices().options().length; option++) {
               float y = bounds.y() + option * OPTION_H - menu.scroll();
               if (y + OPTION_H <= bounds.y() || y >= bounds.y() + bounds.height()) continue;
               Entry<Target> entry = new Entry<>(new Target(menu.row(), menu.mode()), Kind.OPTION, 0, OPTION_H, option, 0, OPTION_H);
               this.drawEntry(canvas, column, entry, y);
            }
         });
      }
   }

   private void drawEntry(Canvas canvas, OpaiColumn column, Entry<Target> entry, float y) {
      OpaiRow row = entry.target().row();
      Setting setting = entry.target().setting();
      float x = column.x;
      float labelW = this.colW - TEXT_PAD * 2;
      Dropdown overlay = this.dropdownAt(column);
      boolean hovered = !(this.configFront && this.configs.contains(this.mouseX, this.mouseY)) && this.columnAt() == column
         && (entry.kind() == Kind.OPTION ? overlay != null && overlay.mode() == setting
               && new Rect(column.x + TEXT_PAD - 2, y, labelW + 4, OPTION_H).contains(this.mouseX, this.mouseY)
            : overlay == null && column.content.hit(this.mouseY - column.y - HEADER_H, column.scroll, column.bodyH) == entry);
      if (entry.kind() == Kind.MODULE) {
         double now = OpaiMotion.now();
         float enabled = row.enabled.approach(row.module.isEnabled() ? 1 : 0, now);
         float hover = row.hover.approach(hovered && this.draggedColumn == null ? 1 : 0, now);
         if (hover > 0.01f && enabled < 0.99f) {
            canvas.rect(x, y, this.colW, ROW_H, applyAlpha(this.palette.hover(), hover * (1 - enabled)));
         }
         if (enabled > 0.01f) {
            canvas.rect(x, y, this.colW, ROW_H,
               applyAlpha(mixColor(this.palette.enabled(), this.palette.enabledHover(), hover), enabled));
         }
         row.feedback.paint(canvas, x, y, this.colW, ROW_H, 0, this.palette.text(), now);
         canvas.text(row == this.bindingRow ? "Press a key" : row.name,
            x + TEXT_PAD, y + ROW_H / 2, ROW_TEXT_SIZE, labelW, mixColor(this.palette.text(), this.palette.enabledText(), enabled));
         return;
      }

      // Settings share the column's single translucent surface; a second fill changes its tint.
      switch (entry.kind()) {
         case BOOLEAN -> {
            BooleanSetting bool = (BooleanSetting)setting;
            canvas.text(displayName(setting.getDisplayName()), x + TEXT_PAD, y + BOOLEAN_H / 2,
               8.5f, labelW - 30, this.palette.text());
            this.drawToggle(canvas, x + this.colW - TEXT_PAD - 26, y + (BOOLEAN_H - 16) / 2, bool);
         }
         case NUMBER -> {
            NumberSetting number = (NumberSetting)setting;
            String value = formatNumber(number);
            float valueWidth = Math.min(labelW * 0.45f, canvas.textWidth(value, 8.5f));
            canvas.text(displayName(setting.getDisplayName()), x + TEXT_PAD, y + 6.5f,
               8.5f, labelW - valueWidth - 4, this.palette.text());
            canvas.text(value, x + this.colW - TEXT_PAD - valueWidth, y + 6.5f, 8.5f, valueWidth, this.palette.text());
            float trackW = this.colW - TEXT_PAD * 2;
            float trackY = y + SLIDER_Y;
            double range = number.m219() - number.m218();
            float fraction = range <= 0 ? 0 : (float)Math.clamp((number.m220() - number.m218()) / range, 0, 1);
            OpaiMotion slider = this.sliderAnims.computeIfAbsent(number, ignored -> new OpaiMotion(fraction, 35));
            if (this.draggingSlider == number) slider.snap(fraction, OpaiMotion.now());
            float displayed = slider.approach(fraction, OpaiMotion.now());
            canvas.rounded(x + TEXT_PAD, trackY - 1.25f, trackW, 2.5f, 1.25f, this.palette.track());
            canvas.rounded(x + TEXT_PAD, trackY - 1.25f, trackW * displayed, 2.5f, 1.25f, this.palette.accent());
            float knobX = x + TEXT_PAD + trackW * displayed;
            float pulse = this.feedback(number).press(OpaiMotion.now());
            if (pulse > 0) canvas.rounded(knobX - 7, trackY - 7, 14, 14, 7, applyAlpha(this.palette.accent(), .25f * pulse));
            canvas.rounded(knobX - 4.5f, trackY - 3.5f, 9, 9, 4.5f, 0x33000000);
            canvas.rounded(knobX - 4, trackY - 4, 8, 8, 4, this.palette.accent());
         }
         case MODE -> {
            ChoiceSetting choices = (ChoiceSetting)setting;
            canvas.text(displayName(setting.getDisplayName()), x + TEXT_PAD, y + 8,
               8.5f, labelW, this.palette.text());
            Rect field = field(x, y, this.colW);
            boolean open = row.openMode == setting || row.modes.containsKey(setting) && row.modes.get(setting).value() > 0;
            int fieldColor = this.choiceField(choices);
            canvas.rounded(field.x(), field.y(), field.width(), field.height(), 2, fieldColor);
            if (open) {
               canvas.rect(field.x(), field.y() + 3, field.width(), MODE_H - FIELD_Y - 3, fieldColor);
            }
            canvas.text(displayName(choices.selectionLabel()), field.x() + 6, field.y() + FIELD_H / 2,
               8.5f, field.width() - 12, this.palette.text());
            canvas.rect(field.x() + 1, field.y() + FIELD_H - 1, field.width() - 2, 1,
               open ? this.palette.enabled() : this.palette.fieldLine());
            this.feedback(setting).paint(canvas, field.x(), field.y(), field.width(), field.height(), 2,
               this.palette.accent(), OpaiMotion.now());
         }
         case OPTION -> {
            ChoiceSetting choices = (ChoiceSetting)setting;
            String[] options = choices.options();
            if (entry.option() >= options.length) {
               return;
            }
            String value = options[entry.option()];
            boolean selected = choices.selected(entry.option());
            float left = x + TEXT_PAD - 2;
            float width = labelW + 4;
            if (selected || hovered) {
               int selection = choices.multiple() ? mixColor(this.choiceField(choices), this.palette.accent(), .10f) : this.palette.selected();
               canvas.rect(left + 1, y, width - 2, OPTION_H, selected ? selection : this.palette.hover());
            }
            if (selected) {
               canvas.check(left + (choices.multiple() ? 5.5f : 5), y + (choices.multiple() ? 6 : 7),
                  choices.multiple() ? 8 : 6, this.palette.text());
            }
            boolean indent = selected || choices.multiple();
            canvas.text(displayName(value), left + (indent ? 17 : 6), y + OPTION_H / 2,
               8.5f, width - (indent ? 23 : 12), this.palette.text());
            row.optionFeedback.computeIfAbsent(new Option(setting, entry.option()), ignored -> new OpaiFeedback())
               .paint(canvas, left, y, width, OPTION_H, 0, this.palette.accent(), OpaiMotion.now());
         }
         default -> { }
      }
   }

   private int choiceField(ChoiceSetting choices) {
      return choices.multiple() ? 0xFF2A2A2D : this.palette.field();
   }

   private void drawToggle(Canvas canvas, float x, float y, BooleanSetting bool) {
      OpaiMotion anim = this.toggleAnims.computeIfAbsent(bool, setting -> new OpaiMotion(setting.m215() ? 1 : 0, 30));
      float t = anim.approach(bool.m215() ? 1 : 0, OpaiMotion.now());
      canvas.rounded(x, y, 26, 16, 8, mixColor(this.palette.toggleOutline(), this.palette.accent(), t));
      if (t < 1f) {
         canvas.rounded(x + 1, y + 1, 24, 14, 7, applyAlpha(this.palette.field(), 1 - t));
      }
      OpaiFeedback feedback = this.feedback(bool);
      float stretch = 3 * feedback.press(OpaiMotion.now());
      float knobX = x + 2 + (10 - stretch) * t;
      canvas.rounded(knobX, y + 2, 12 + stretch, 12, 6, mixColor(this.palette.fieldLine(), this.palette.enabledText(), t));
      feedback.paint(canvas, x, y, 26, 16, 8, this.palette.accent(), OpaiMotion.now());
      if (t > 0.01f) {
         canvas.check(knobX + 3, y + 5, 6, applyAlpha(this.palette.accent(), t));
      }
   }

   private OpaiFeedback feedback(Setting setting) {
      return this.controlFeedback.computeIfAbsent(setting, ignored -> new OpaiFeedback());
   }

   @Override
   public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
      if (this.minecraft.level == null) {
         this.extractPanorama(graphics, delta);
      }
      // GuiRenderer skips the blur pass when there are no vanilla draws after its marker.
      // NanoVG draws later in endFrame, so keep that stratum alive with an invisible quad.
      this.extractBlurredBackground(graphics);
      graphics.fill(0, 0, this.width, this.height, 0x00000000);
      this.minecraft.gui.hud.extractDeferredSubtitles();
   }

   @Override
   public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
      this.setMouse(mouseX, mouseY);
      this.refreshLayouts();
      if (!NVGRenderer.isAvailable()) {
         for (OpaiColumn column : this.columns) {
            column.scroll = column.scrollTarget;
         }
         this.menuScale = 1;
         this.palette = OpaiStyle.palette(this.colorSetting.m224());
         Canvas canvas = new Canvas(graphics);
         if (!this.configFront) this.drawConfigs(canvas);
         this.drawColumns(canvas);
         if (this.configFront) this.drawConfigs(canvas);
      }
   }

   private OpaiColumn columnAt() {
      for (int i = this.columns.size() - 1; i >= 0; i--) {
         OpaiColumn column = this.columns.get(i);
         if (this.dropdownAt(column) != null || new Rect(column.x, column.y, this.colW, HEADER_H + column.bodyH).contains(this.mouseX, this.mouseY)) {
            return column;
         }
      }
      return null;
   }

   private void setMouse(double x, double y) {
      this.mouseX = this.width / 2.0 + (x - this.width / 2.0) / this.menuScale;
      this.mouseY = this.transformCenterY + (y - this.transformCenterY) / this.menuScale;
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      if (!this.closing && this.hudEdit.click(event.x(), event.y(), event.button())) {
         this.closeDestination = new com.samsara.ui.hud.editor.HudEditorScreen(); this.onClose(); return true;
      }
      if (this.closing) {
         return true;
      }
      this.setMouse(event.x(), event.y());
      this.shiftDown = event.hasShiftDown();
      OpaiColumn column = this.columnAt();
      int button = normalizeMouseButton(event.button());
      if ((this.configFront || column == null) && this.configs.click(this.mouseX, this.mouseY, button)) {
         this.bindingRow = null;
         this.draggedColumn = null;
         this.draggedScrollbar = null;
         this.draggingSlider = null;
         this.sliderRow = null;
         this.sliderColumn = null;
         this.configFront = true;
         this.configName.setFocused(this.configs.focused());
         if (button == 0 && this.configs.focused()) {
            int cursor = NVGRenderer.isAvailable() ? this.configs.cursorAt(new Canvas(null), this.mouseX) : this.configName.getValue().length();
            this.configName.moveCursorTo(cursor, event.hasShiftDown());
            this.syncConfigEditor();
         }
         return true;
      }
      this.configName.setFocused(false);
      this.configs.blur();
      if (column == null) {
         this.closeDropdowns();
         this.refreshLayouts();
         return super.mouseClicked(event, doubleClick);
      }
      this.columns.remove(column);
      this.columns.add(column);
      this.configFront = false;
      Dropdown overlay = this.dropdownAt(column);
      if (overlay != null) {
         if (button == 0 && overlay.row().openMode == overlay.mode()) {
            int option = (int)((this.mouseY - overlay.bounds().y() + overlay.scroll()) / OPTION_H);
            if (option >= 0 && option < overlay.choices().options().length) {
               overlay.choices().select(option);
               overlay.row().optionFeedback.computeIfAbsent(new Option(overlay.mode(), option), ignored -> new OpaiFeedback())
                  .click((float)this.mouseX - overlay.bounds().x(), (float)this.mouseY - overlay.bounds().y() + overlay.scroll() - option * OPTION_H,
                     overlay.bounds().width(), OPTION_H, OpaiMotion.now());
               if (!overlay.choices().multiple()) overlay.row().openMode = null;
               this.refreshLayouts();
            }
         } else if (button == 1) { this.closeDropdowns(); this.refreshLayouts(); }
         return true;
      }
      if (this.mouseY < column.y + HEADER_H) {
         Action action = OpaiInteraction.header(button);
         if (action == Action.EXPAND) {
            this.clearDrag();
            column.expanded = !column.expanded;
            for (OpaiRow row : column.rows) {
               if (this.bindingRow == row) {
                  this.bindingRow = null;
               }
            }
            this.refreshLayouts();
         } else if (action == Action.DRAG) {
            this.clearDrag();
            this.draggedColumn = column;
            this.dragX = (float)this.mouseX - column.x;
            this.dragY = (float)this.mouseY - column.y;
         }
         return true;
      }
      float localY = (float)this.mouseY - column.y - HEADER_H;
      if (!column.expanded) return true;
      if (button == 0 && column.maxScroll > 0 && new Rect(column.x + this.colW - 6,
            column.y + HEADER_H, 6, column.bodyH - FOOTER_H).contains(this.mouseX, this.mouseY)) {
         this.clearDrag();
         this.draggedScrollbar = column;
         Scrollbar bar = scrollbar(column.bodyH, column.content.height(), column.scroll);
         this.scrollbarGrab = localY >= bar.top() && localY < bar.top() + bar.height()
            ? localY - bar.top() : bar.height() / 2;
         this.updateScrollbar();
         return true;
      }
      Entry<Target> entry = column.content.hit(localY, column.scroll, column.bodyH);
      if (entry == null) {
         return true;
      }
      OpaiRow row = entry.target().row();
      Setting setting = entry.target().setting();
      Action action = OpaiInteraction.content(entry.kind(), button);
      if (entry.kind() == Kind.MODULE) {
         if (action == Action.TOGGLE) {
            row.feedback.click((float)this.mouseX - column.x,
               (float)this.mouseY - column.y - HEADER_H - entry.top() + column.scroll, this.colW, ROW_H, OpaiMotion.now());
            row.enabled.approach(row.module.isEnabled() ? 1 : 0, OpaiMotion.now());
            row.module.toggle();
            row.enabled.approach(row.module.isEnabled() ? 1 : 0, OpaiMotion.now());
         } else if (action == Action.EXPAND) {
            this.clearDrag();
            row.expanded = !row.expanded;
            if (this.bindingRow == row && !row.expanded) this.bindingRow = null;
            this.refreshLayouts();
            if (row.expanded) {
               float visible = column.targetBodyH - FOOTER_H;
               float reveal = Math.min(column.content.height(), entry.top() + ROW_H + MODE_H);
               column.scrollTarget = Math.clamp(
                  Math.max(column.scroll, Math.min(entry.top(), reveal - visible)), 0, column.maxScroll);
            }
         } else if (action == Action.BIND) {
            this.bindingRow = row;
         }
      } else if (action != Action.NONE && row.expanded && setting != null && setting.isVisible()) {
         float y = column.y + HEADER_H + entry.top() - column.scroll;
         switch (action) {
            case TOGGLE -> {
               BooleanSetting bool = (BooleanSetting)setting;
               OpaiMotion toggle = this.toggleAnims.computeIfAbsent(bool, ignored -> new OpaiMotion(bool.m215() ? 1 : 0, 30));
               toggle.approach(bool.m215() ? 1 : 0, OpaiMotion.now());
               this.feedback(bool).click((float)this.mouseX - column.x - this.colW + TEXT_PAD + 26,
                  (float)this.mouseY - y - (BOOLEAN_H - 16) / 2, 26, 16, OpaiMotion.now());
               bool.m217(!bool.m215());
               toggle.approach(bool.m215() ? 1 : 0, OpaiMotion.now());
            }
            case SLIDE -> {
               if (sliderHit(column.x, y, this.colW).contains(this.mouseX, this.mouseY)) {
                  this.feedback(setting).click((float)this.mouseX - column.x - TEXT_PAD,
                     4, this.colW - TEXT_PAD * 2, 8, OpaiMotion.now());
                  this.clearDrag();
                  this.draggingSlider = (NumberSetting)setting;
                  this.sliderRow = row;
                  this.sliderColumn = column;
                  column.scrollTarget = column.scroll;
                  this.updateSlider();
               }
            }
            case EXPAND -> {
               if (field(column.x, y, this.colW).contains(this.mouseX, this.mouseY)) {
                  Rect clicked = field(column.x, y, this.colW);
                  this.feedback(setting).click((float)this.mouseX - clicked.x(), (float)this.mouseY - clicked.y(),
                     clicked.width(), clicked.height(), OpaiMotion.now());
                  boolean wasOpen = row.openMode == setting;
                  this.closeDropdowns();
                  row.openMode = wasOpen ? null : setting;
               }
            }
            default -> { }
         }
      }
      this.refreshLayouts();
      return true;
   }

   /** 26.3 reports SDL button ids (1=left, 2=middle, 3=right); handlers use GLFW-style (0=left, 1=right). */
   private static int normalizeMouseButton(int button) {
      return switch (button) {
         case 1 -> 0;
         case 3 -> 1;
         case 4 -> 3;
         case 5 -> 4;
         default -> button;
      };
   }

   private void updateSlider() {
      double fraction = sliderFraction(this.mouseX, this.sliderColumn.x, this.colW);
      this.draggingSlider.m223(this.draggingSlider.m218()
         + fraction * (this.draggingSlider.m219() - this.draggingSlider.m218()));
   }

   private void updateScrollbar() {
      OpaiColumn column = this.draggedScrollbar;
      float thumb = (float)this.mouseY - column.y - HEADER_H - this.scrollbarGrab;
      column.scroll = column.scrollTarget = scrollFromThumb(thumb, column.bodyH, column.content.height());
   }

   private void clearDrag() {
      this.configs.release();
      this.draggedColumn = null;
      this.draggedScrollbar = null;
      this.draggingSlider = null;
      this.sliderRow = null;
      this.sliderColumn = null;
   }

   private boolean closeDropdowns() {
      boolean closed = false;
      for (OpaiColumn column : this.columns) {
         for (OpaiRow row : column.rows) {
            closed |= row.openMode != null;
            row.openMode = null;
         }
      }
      return closed;
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent event) {
      if (normalizeMouseButton(event.button()) == 0) {
         boolean captured = this.configs.captured() || this.draggedColumn != null || this.draggingSlider != null || this.draggedScrollbar != null;
         this.clearDrag();
         if (captured) {
            return true;
         }
      }
      return super.mouseReleased(event);
   }

   @Override
   public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
      if (this.closing) {
         return true;
      }
      this.setMouse(event.x(), event.y());
      if (normalizeMouseButton(event.button()) == 0) {
         if (this.configs.captured()) {
            this.configs.drag(this.mouseX, this.mouseY, this.width, this.height);
            return true;
         }
         if (this.configs.focused()) {
            int cursor = NVGRenderer.isAvailable() ? this.configs.cursorAt(new Canvas(null), this.mouseX) : this.configName.getValue().length();
            this.configName.moveCursorTo(cursor, true);
            this.syncConfigEditor();
            return true;
         }
         if (this.draggedColumn != null) {
            this.draggedColumn.x = Math.clamp((float)this.mouseX - this.dragX, 4, this.width - this.colW - 4);
            this.draggedColumn.y = Math.clamp((float)this.mouseY - this.dragY, 4,
               Math.max(4, this.height - HEADER_H - this.draggedColumn.bodyH - 8));
            com.samsara.ui.hud.ClickGuiLayouts.put("opai:" + this.draggedColumn.name, this.draggedColumn.x, this.draggedColumn.y);
            this.refreshLayouts();
            return true;
         }
         if (this.draggedScrollbar != null) {
            this.updateScrollbar();
            return true;
         }
         if (this.draggingSlider != null) {
            if (this.sliderRow.expanded && this.draggingSlider.isVisible()) {
               this.updateSlider();
               this.refreshLayouts();
            } else {
               this.clearDrag();
            }
            return true;
         }
      }
      return super.mouseDragged(event, deltaX, deltaY);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      this.setMouse(mouseX, mouseY);
      if (this.closing || this.draggingSlider != null || this.draggedScrollbar != null || this.draggedColumn != null) {
         return true;
      }
      OpaiColumn column = this.columnAt();
      if (this.configs.contains(this.mouseX, this.mouseY) && (this.configFront || column == null)) return this.configs.wheel(verticalAmount);
      Dropdown overlay = column == null ? null : this.dropdownAt(column);
      if (overlay != null) {
         overlay.row().modeScroll.put(overlay.mode(), Math.clamp(overlay.scroll() - (float)verticalAmount * OPTION_H * 2,
            0, overlay.choices().options().length * OPTION_H - overlay.fullHeight()));
         column.dropdowns = this.layoutDropdowns(column);
         return true;
      }
      if (horizontalAmount != 0 || this.shiftDown || column == null) {
         float amount = (float)(horizontalAmount != 0 ? horizontalAmount : verticalAmount);
         float next = Math.clamp(this.horizontalScroll - amount * ROW_H * 2, 0, this.maxHorizontalScroll);
         float delta = next - this.horizontalScroll;
         for (OpaiColumn item : this.columns) {
            item.x -= delta;
         }
         this.horizontalScroll = next;
         return true;
      }
      if (column != null) {
         column.scrollTarget = Math.clamp(column.scrollTarget - (float)verticalAmount * ROW_H * 2, 0, column.maxScroll);
         return true;
      }
      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   @Override
   public boolean keyPressed(KeyEvent event) {
      if (this.closing) {
         return true;
      }
      int key = event.key();
      this.shiftDown = event.hasShiftDown() || key == SDLScancode.SDL_SCANCODE_LSHIFT || key == SDLScancode.SDL_SCANCODE_RSHIFT;
      if (this.bindingRow != null) {
         this.bindingRow.module.setKey(event.isEscape() || key == SDLScancode.SDL_SCANCODE_BACKSPACE ? 0 : key);
         this.bindingRow = null;
         return true;
      }
      if (event.isEscape()) {
         if (this.configs.escape()) {
            this.configName.setFocused(false);
         } else if (this.closeDropdowns()) {
            this.refreshLayouts();
         } else {
            this.onClose();
         }
         return true;
      }
      if (this.configs.focused()) {
         this.configName.setFocused(true);
         if (event.isConfirmation()) {
            this.syncConfigEditor();
            this.configs.create(false);
            return true;
         }
         boolean handled = this.configName.keyPressed(event);
         this.syncConfigEditor();
         return handled;
      }
      return super.keyPressed(event);
   }

   private void syncConfigEditor() {
      int cursor = this.configName.getCursorPosition();
      String highlighted = this.configName.getHighlighted();
      int selection = cursor;
      if (!highlighted.isEmpty()) {
         String value = this.configName.getValue();
         selection = value.startsWith(highlighted, cursor) ? cursor + highlighted.length() : cursor - highlighted.length();
      }
      this.configs.editor(this.configName.getValue(), cursor, selection);
   }

   @Override
   public boolean charTyped(CharacterEvent event) {
      if (this.closing) return true;
      if (this.configs.focused()) {
         this.configName.setFocused(true);
         boolean handled = this.configName.charTyped(event);
         this.syncConfigEditor();
         return handled;
      }
      return super.charTyped(event);
   }

   @Override
   public boolean keyReleased(KeyEvent event) {
      this.shiftDown = event.key() != SDLScancode.SDL_SCANCODE_LSHIFT && event.key() != SDLScancode.SDL_SCANCODE_RSHIFT
         && event.hasShiftDown();
      return super.keyReleased(event);
   }

   @Override
   public void onClose() {
      if (this.closing) return;
      this.clearDrag();
      this.bindingRow = null;
      if (NVGRenderer.isAvailable()) {
         this.closing = true;
         this.closeStarted = OpaiMotion.now();
         this.closingAlpha = this.menuOpacity;
         this.closingScale = this.menuScale;
      } else {
         this.minecraft.gui.setScreen(this.closeDestination);
      }
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   @Override
   public void removed() {
      super.removed();
      for (OpaiColumn column : this.columns) com.samsara.ui.hud.ClickGuiLayouts.put("opai:" + column.name, column.x, column.y);
      com.samsara.config.ConfigManager.saveState();
      this.detached = true;
      this.bindingRow = null;
      this.configs.blur();
      this.clearDrag();
   }

   private static String formatNumber(NumberSetting number) {
      int precision = Math.clamp(BigDecimal.valueOf(number.m222()).stripTrailingZeros().scale(), 0, 6);
      return BigDecimal.valueOf(number.m220()).setScale(precision, RoundingMode.HALF_UP).toPlainString();
   }

   private static int applyAlpha(int color, float alpha) {
      int a = (int)(Math.clamp(alpha, 0, 1) * ((color >>> 24) & 0xFF));
      return (a << 24) | (color & 0x00FFFFFF);
   }

   private static int mixColor(int from, int to, float t) {
      t = Math.clamp(t, 0, 1);
      int result = 0;
      for (int shift = 0; shift <= 24; shift += 8) {
         int start = (from >>> shift) & 0xFF;
         int end = (to >>> shift) & 0xFF;
         result |= (int)(start + (end - start) * t) << shift;
      }
      return result;
   }

   private final class Canvas implements OpaiSurface {
      private final GuiGraphicsExtractor graphics;
      private final NVGTextRenderer font;
      private final NVGTextRenderer plainFont;
      private float localOpacity = 1;

      Canvas(GuiGraphicsExtractor graphics) {
         this.graphics = graphics;
         this.font = graphics == null ? FontRepository.getFont("googlesans-medium") : null;
         this.plainFont = graphics == null ? FontRepository.getFont("googlesans-regular") : null;
      }

      public void rect(float x, float y, float w, float h, int color) {
         if (w <= 0 || h <= 0) {
            return;
         }
         if (this.graphics == null) {
            NVGRenderer.rect(x, y, w, h, color);
         } else {
            this.graphics.fill(Math.round(x), Math.round(y), Math.round(x + w), Math.round(y + h), color);
         }
      }

      public void rounded(float x, float y, float w, float h, float radius, int color) {
         if (w <= 0 || h <= 0) {
            return;
         }
         if (this.graphics == null) {
            NVGRenderer.roundedRect(x, y, w, h, radius, color);
         } else {
            this.rect(x, y, w, h, color);
         }
      }

      public void panel(float x, float y, float w, float h, boolean header, int color) {
         if (this.graphics == null) {
            NVGRenderer.roundedRectVarying(x, y, w, h,
               header ? RADIUS : 0, header ? RADIUS : 0, header ? 0 : RADIUS, header ? 0 : RADIUS, color);
         } else {
            this.rect(x, y, w, h, color);
         }
      }

      public float textWidth(String text, float size) {
         return this.font == null ? minecraft.font.width(text) : this.font.getStringWidth(text, size) + TEXT_WEIGHT_OFFSET;
      }

      public float plainTextWidth(String text, float size) {
         return this.plainFont == null ? minecraft.font.width(text) : this.plainFont.getStringWidth(text, size);
      }

      public void text(String text, float x, float centerY, float size, float maxWidth, int color) {
         this.drawText(text, x, centerY, size, maxWidth, color, true);
      }

      public void plainText(String text, float x, float centerY, float size, float maxWidth, int color) {
         this.drawText(text, x, centerY, size, maxWidth, color, false);
      }

      private void drawText(String text, float x, float centerY, float size, float maxWidth, int color, boolean weighted) {
         if (maxWidth <= 0) {
            return;
         }
         String label = text;
         NVGTextRenderer face = weighted ? this.font : this.plainFont;
         if (this.labelWidth(label, size, weighted) > maxWidth) {
            if (this.labelWidth("...", size, weighted) > maxWidth) {
               return;
            }
            while (!label.isEmpty() && this.labelWidth(label + "...", size, weighted) > maxWidth) {
               label = label.substring(0, label.length() - 1);
            }
            label += "...";
         }
         if (this.graphics == null) {
            face.drawString(label, x, centerY, size, color, false, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
            if (weighted) face.drawString(label, x + TEXT_WEIGHT_OFFSET, centerY, size, color, false, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
         } else {
            this.graphics.text(minecraft.font, label, Math.round(x), Math.round(centerY - 4), color);
         }
      }

      private float labelWidth(String text, float size, boolean weighted) {
         return weighted ? this.textWidth(text, size) : this.plainTextWidth(text, size);
      }

      public void clip(float x, float y, float w, float h, Runnable content) {
         if (this.graphics == null) {
            long vg = NVGRenderer.getContext();
            nvgSave(vg);
            try { nvgIntersectScissor(vg, x, y, w, h); content.run(); }
            finally { nvgRestore(vg); }
         } else {
            Rect clip = new Rect(x, y, w, h);
            // 26.3 GuiGraphicsExtractor owns a nested scissor stack; disableScissor pops this level.
            this.useClip(clip);
            try {
               content.run();
            } finally {
               this.graphics.disableScissor();
            }
         }
      }

      private void useClip(Rect clip) {
         int left = (int)Math.ceil(clip.x()), top = (int)Math.ceil(clip.y());
         this.graphics.enableScissor(left, top, Math.max(left, (int)Math.floor(clip.x() + clip.width())),
            Math.max(top, (int)Math.floor(clip.y() + clip.height())));
      }

      public void opacity(float factor, Runnable content) {
         if (this.graphics != null) { content.run(); return; }
         long vg = NVGRenderer.getContext();
         nvgSave(vg);
         float previous = this.localOpacity;
         try { this.localOpacity *= factor; nvgGlobalAlpha(vg, menuOpacity * this.localOpacity); content.run(); }
         finally { this.localOpacity = previous; nvgRestore(vg); }
      }

      public void icon(OpaiIcons.Icon icon, float x, float y, float size, int color) {
         if (this.graphics == null) OpaiIcons.draw(NVGRenderer.getContext(), icon, x, y, size, color);
         else this.text(switch (icon) {
            case PLUS -> "+"; case BACK -> "<"; case GEAR -> "*"; case REFRESH -> "R"; case CHECK -> "✓";
            case SAVE -> "S"; case LOAD -> "L"; case DELETE -> "X"; case FOLDER -> "F"; case UPLOAD -> "U";
         }, x, y + size / 2, size, size + 2, color);
      }

      public void scale(float factor, float centerX, float centerY, Runnable content) {
         if (this.graphics != null) { content.run(); return; }
         long vg = NVGRenderer.getContext();
         nvgSave(vg);
         try {
            nvgTranslate(vg, centerX, centerY);
            nvgScale(vg, factor, factor);
            nvgTranslate(vg, -centerX, -centerY);
            content.run();
         } finally { nvgRestore(vg); }
      }

      public void ripple(float x, float y, float w, float h, float corner,
                         float centerX, float centerY, float radius, int color) {
         if (this.graphics != null) {
            this.clip(x, y, w, h, () -> this.rounded(centerX - radius, centerY - radius, radius * 2, radius * 2, radius, color));
            return;
         }
         long vg = NVGRenderer.getContext();
         try (org.lwjgl.system.MemoryStack stack = org.lwjgl.system.MemoryStack.stackPush()) {
            var ink = org.lwjgl.nanovg.NVGColor.malloc(stack);
            var clear = org.lwjgl.nanovg.NVGColor.malloc(stack);
            var paint = org.lwjgl.nanovg.NVGPaint.malloc(stack);
            NVGRenderer.applyColor(color, ink);
            NVGRenderer.applyColor(color & 0xFFFFFF, clear);
            nvgRadialGradient(vg, centerX, centerY, Math.max(0, radius - .6f), radius, ink, clear, paint);
            nvgBeginPath(vg);
            nvgRoundedRect(vg, x, y, w, h, corner);
            nvgFillPaint(vg, paint);
            nvgFill(vg);
         }
      }

      void check(float x, float y, float size, int color) {
         if (this.graphics == null) {
            long vg = NVGRenderer.getContext();
            NVGRenderer.applyColor(color, NVGRenderer.NVG_COLOR_1);
            nvgBeginPath(vg);
            nvgStrokeColor(vg, NVGRenderer.NVG_COLOR_1);
            nvgStrokeWidth(vg, 1);
            nvgMoveTo(vg, x, y + size * 0.5f);
            nvgLineTo(vg, x + size * 0.38f, y + size * 0.88f);
            nvgLineTo(vg, x + size, y);
            nvgStroke(vg);
         } else {
            for (int i = 0; i <= 6; i++) {
               float px = x + size * i / 6;
               float py = i <= 2 ? y + size * (0.5f + i * 0.19f) : y + size * (6 - i) * 0.22f;
               this.rect(px, py, 1, 1, color);
            }
         }
      }
   }

   private record Target(OpaiRow row, Setting setting) {
   }
   private record Option(Setting mode, int index) { }
   private record Dropdown(OpaiColumn column, OpaiRow row, Setting mode, Rect bounds, float scroll, float fullHeight) {
      ChoiceSetting choices() { return (ChoiceSetting)this.mode; }
   }

   private static final class OpaiColumn {
      final String name;
      final List<OpaiRow> rows = new ArrayList<>();
      final OpaiMotion expand = new OpaiMotion(1, 40);
      OpaiContentLayout<Target> content = new OpaiContentLayout<>();
      List<Dropdown> dropdowns = List.of();
      float x;
      float y;
      float scroll;
      float scrollTarget;
      float maxScroll;
      float bodyH;
      float targetBodyH;
      boolean expanded = true;

      OpaiColumn(String name) {
         this.name = name;
      }
   }

   private static final class OpaiRow {
      final Feature module;
      final String name;
      final OpaiMotion hover = new OpaiMotion(0, 30);
      final OpaiMotion expand = new OpaiMotion(0, 40);
      final OpaiMotion enabled;
      final OpaiFeedback feedback = new OpaiFeedback();
      final Map<Setting, OpaiMotion> modes = new HashMap<>();
      final Map<Setting, Float> modeScroll = new HashMap<>();
      final Map<Option, OpaiFeedback> optionFeedback = new HashMap<>();
      boolean expanded;
      Setting openMode;

      OpaiRow(Feature module) {
         this.module = module;
         this.enabled = new OpaiMotion(module.isEnabled() ? 1 : 0, 30);
         this.name = displayName(module.getName());
      }
   }
}
