package com.samsara.ui.clickgui;

import com.samsara.util.render.ColorUtility;
import com.samsara.ui.MouseButtons;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.module.FeatureManager;
import com.samsara.config.ConfigManager;
import com.samsara.setting.BooleanSetting;
import com.samsara.setting.ModeSetting;
import com.samsara.setting.MultiSelectSetting;
import com.samsara.setting.NumberSetting;
import com.samsara.setting.Setting;
import com.samsara.ui.NanoGui;
import com.samsara.util.animation.Animation;
import com.samsara.util.animation.Easing;
import com.samsara.util.render.FontRepository;
import com.samsara.util.render.NVGRenderer;
import com.samsara.util.render.NVGTextRenderer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import static org.lwjgl.nanovg.NanoVG.*;

public class RockstarClickGuiScreen extends Screen implements NanoGui {
   public enum Style {
      MODERN,
      DROPDOWN
   }

   private static final int WIN_W = 500;
   private static final int WIN_H = 343;
   private static final float SIDEBAR_W = 109.0f;
   private static final float CARD_W = 177.0f;
   private static final float CARD_H = 28.0f;
   private static final float CARD_STEP_X = 183.5f;
   private static final float CARD_STEP_Y = 34.0f;
   private static final float CARDS_X = 127.0f;
   private static final float SETTINGS_W = 152.0f;
   private static final float SETTINGS_MAX_H = 200.0f;
   private static final int WINDOW_BG = 0xF20C0D12;
   private static final int SIDEBAR_BG = 0x8C23252E;
   private static final int CARD_BASE = 0x0FFFFFFF;
   private static final int CARD_HOVER = 0x1CFFFFFF;
   private static final int SEARCH_BG = 0x26FFFFFF;
   private static final int ACCENT = 0xFF9747FF;
   private static final int TOGGLE_OFF = 0xFF565B64;
   private static final int WHITE = 0xFFFFFFFF;
   private static final int TEXT = 0xFFFFFFFF;
   private static final int TEXT_MUTED = 0xB3FFFFFF;
   private static final int TEXT_FAINT = 0x4DFFFFFF;
   private static final int SEP = 0x0AFFFFFF;
   private static final int GLASS_TEXT = 0xE52A344A;
   private static final int GLASS_TEXT_MUTED = 0xA83F4B66;
   private static final int GLASS_TEXT_FAINT = 0x713F4B66;
   private static final int GLASS_ACCENT = 0xFF648FE8;
   private static final int GLASS_ACCENT_SOFT = 0xB66E9CEB;
   private static final int GLASS_TOGGLE_OFF = 0xC05D687C;
   private static final int GLASS_TOGGLE_ON = 0xFF6A98E7;
   private static final int GLASS_SEPARATOR = 0x302D3B55;

   private static final Category[] CATEGORY_ORDER = {
      Category.COMBAT,
      Category.MOVEMENT,
      Category.VISUAL,
      Category.PLAYER,
      Category.MISC
   };
   private static final String[] CATEGORY_NAMES = {"Combat", "Movement", "Visuals", "Player", "Other"};

   private ModeSetting renderMode;
   private NumberSetting draggingSlider;

   private final Style style;
   private final List<CategorySection> sections = new ArrayList<>();
   private final List<SettingsWindow> windows = new ArrayList<>();
   private final Animation menuAnimation = new Animation(Easing.EASE_OUT_QUART, 400);
   private final Animation sidebarPill = new Animation(Easing.EASE_OUT_QUART, 150);
   private final com.samsara.ui.hud.editor.HudEditButton hudEdit = new com.samsara.ui.hud.editor.HudEditButton();

   private CategorySection currentSection;
   private float winX;
   private float winY;
   private boolean dragWindow;
   private float dragX;
   private float dragY;
   private boolean closing;
   private Screen closeDestination;
   private float hudEditOpacity;
   private boolean initialized;
   private float scrollValue;
   private float scrollTarget;
   private float maxScroll;
   private long lastFrameTime;
   private String search = "";

   private final Animation configsAnim = new Animation(Easing.EASE_OUT_QUART, 300);

   private boolean searchFocused;
   private ModuleCard bindingCard;
   private SettingsWindow draggingSettings;
   private float settingsDragX;
   private float settingsDragY;
   private SettingsWindow sliderWindow;
   private boolean configsOpen;
   private float configsX;
   private float configsY;
   private float configsScroll;
   private float configsScrollTarget;
   private boolean dragConfigs;
   private float configsDragX;
   private float configsDragY;
   private double mouseX;
   private double mouseY;

   public RockstarClickGuiScreen(Style style) {
      super(Component.literal("ClickGUI"));
      this.style = style == null ? Style.MODERN : style;
   }

   public void setRenderMode(ModeSetting mode) {
      this.renderMode = mode;
   }

   private boolean glass() {
      return this.renderMode != null && this.renderMode.is("LiquidGlass");
   }

   @Override
   protected void init() {
      super.init();
      this.winX = Math.max(10.0f, (this.width - WIN_W) / 2.0f);
      this.winY = Math.max(10.0f, (this.height - WIN_H) / 2.0f);
      this.winX = Math.clamp(com.samsara.ui.hud.ClickGuiLayouts.x("modern", this.winX), 4, Math.max(4, this.width - WIN_W - 4));
      this.winY = Math.clamp(com.samsara.ui.hud.ClickGuiLayouts.y("modern", this.winY), 4, Math.max(4, this.height - WIN_H - 4));
      this.closeDestination = null; this.hudEdit.open();
      this.sections.clear();
      for (int i = 0; i < CATEGORY_ORDER.length; i++) {
         Category category = CATEGORY_ORDER[i];
         CategorySection section = new CategorySection(category, CATEGORY_NAMES[i], i);
         List<Feature> modules = new ArrayList<>();
         for (Feature module : FeatureManager.getModules()) {
            if (module.getCategory() == category && !module.isHidden()) {
               modules.add(module);
            }
         }
         modules.sort(Comparator.comparing(Feature::getName, String.CASE_INSENSITIVE_ORDER));
         for (Feature module : modules) {
            section.cards.add(new ModuleCard(module));
         }
         this.sections.add(section);
      }
      this.currentSection = this.sections.getFirst();
      if (!this.initialized) {
         this.initialized = true;
         this.menuAnimation.setStartValue(0.0f);
      }
      this.menuAnimation.run(1.0f);
      this.closing = false;
   }

   public void renderNano() {
      long time = System.currentTimeMillis();
      float dt = Math.min(64.0f, time - this.lastFrameTime);
      this.lastFrameTime = time;
      float smoothing = 1.0f - (float)Math.exp(-dt * 0.014);

      this.menuAnimation.run(this.closing ? 0.0f : 1.0f);
      float alpha = Math.min(1.0f, this.menuAnimation.getValue());
      this.hudEditOpacity = alpha;
      if (alpha <= 0.01f) {
         if (this.closing) {
            this.minecraft.gui.setScreen(this.closeDestination);
         }
         return;
      }
      if (this.scrollTarget > this.maxScroll) {
         this.scrollTarget = this.maxScroll;
      }
      if (this.scrollTarget < 0.0f) {
         this.scrollTarget = 0.0f;
      }
      this.scrollValue += (this.scrollTarget - this.scrollValue) * smoothing;

      float cx = this.winX + WIN_W / 2.0f;
      float cy = this.winY + WIN_H / 2.0f;
      float scale = 0.5f + 0.5f * this.menuAnimation.getValue();
      NVGRenderer.globalAlpha(alpha);
      NVGRenderer.scale(scale, cx, cy, 0.0f, 0.0f, this::drawWindow);
      NVGRenderer.globalAlpha(1.0f);

      Iterator<SettingsWindow> iterator = this.windows.iterator();
      while (iterator.hasNext()) {
         SettingsWindow window = iterator.next();
         window.anim.run(window.showing && !this.closing ? 1.0f : 0.0f);
         if (window.anim.getValue() <= 0.01f && !window.showing) {
            iterator.remove();
            continue;
         }
         window.alpha = Math.min(1.0f, window.anim.getValue());
         window.height = window.computeHeight();
         float windowMax = Math.max(0.0f, window.contentHeight() - (window.height - 29.0f));
         if (window.scrollTarget > windowMax) {
            window.scrollTarget = windowMax;
         }
         if (window.scrollTarget < 0.0f) {
            window.scrollTarget = 0.0f;
         }
         window.scroll += (window.scrollTarget - window.scroll) * smoothing;
         float wScale = 0.5f + 0.5f * window.anim.getValue();
         NVGRenderer.globalAlpha(window.alpha);
         NVGRenderer.scale(wScale, window.x + SETTINGS_W / 2.0f, window.y + window.height / 2.0f, 0.0f, 0.0f, () -> this.drawSettings(window));
         NVGRenderer.globalAlpha(1.0f);
      }

      this.configsAnim.run(this.configsOpen && !this.closing ? 1.0f : 0.0f);
      if (this.configsAnim.getValue() > 0.01f) {
         float configsAlpha = Math.min(1.0f, this.configsAnim.getValue());
         float configsHeight = this.configsHeight();
         float configsMax = Math.max(0.0f, this.configsContentHeight() - (configsHeight - 29.0f));
         if (this.configsScrollTarget > configsMax) {
            this.configsScrollTarget = configsMax;
         }
         if (this.configsScrollTarget < 0.0f) {
            this.configsScrollTarget = 0.0f;
         }
         this.configsScroll += (this.configsScrollTarget - this.configsScroll) * smoothing;
         float configsScale = 0.5f + 0.5f * this.configsAnim.getValue();
         NVGRenderer.globalAlpha(configsAlpha);
         NVGRenderer.scale(configsScale, this.configsX + SETTINGS_W / 2.0f, this.configsY + configsHeight / 2.0f, 0.0f, 0.0f, () -> this.drawConfigs());
         NVGRenderer.globalAlpha(1.0f);
      }

      CategorySection top = this.sections.getFirst();
      for (CategorySection section : this.sections) {
         if (section.y >= 0.0f && section.y <= this.scrollTarget + 0.5f) {
            top = section;
         }
         section.selected.run(section == top ? 1.0f : 0.0f);
      }
      if (top != this.currentSection) {
         this.currentSection = top;
      }
      this.sidebarPill.run(this.currentSection.index * 18.0f);
   }

   private void drawWindow() {
      float x = this.winX;
      float y = this.winY;
      boolean glass = glass();

      if (glass) {
         drawGlassPanel(x, y, WIN_W, WIN_H, 16.0f, 1.0f);
         drawGlassSidebarLayer(x + 5.0f, y + 5.0f, SIDEBAR_W, WIN_H - 10.0f, 12.0f);
      } else {
         for (int i = 3; i >= 1; i--) {
            NVGRenderer.roundedRect(x - i * 2.0f, y - i * 2.0f, WIN_W + i * 4.0f, WIN_H + i * 4.0f, 16.0f + i * 2.0f, 0x16000000);
         }
         NVGRenderer.roundedRect(x, y, WIN_W, WIN_H, 16.0f, WINDOW_BG);
         NVGRenderer.roundedRect(x + 5.0f, y + 5.0f, SIDEBAR_W, WIN_H - 10.0f, 12.0f, SIDEBAR_BG);
      }

      NVGTextRenderer font = FontRepository.getFont("productsans-medium");

      float searchX = x + 13.0f;
      float searchY = y + 13.0f;
      NVGRenderer.roundedRect(searchX, searchY, 93.0f, 14.0f, 3.0f, glass ? 0x45FFFFFF : SEARCH_BG);
      if (glass) {
         drawGlassStroke(searchX, searchY, 93.0f, 14.0f, 3.0f, 0x4DFFFFFF, 0.7f);
      }
      drawMagnifierIcon(searchX + 3.0f, searchY + 3.0f, 8.0f, glass ? GLASS_TEXT_MUTED : TEXT_FAINT);
      if (this.search.isEmpty() && !this.searchFocused) {
         drawText(font, "Search", searchX + 14.0f, searchY + 9.5f, 6.0f, glass ? GLASS_TEXT_FAINT : TEXT_FAINT);
      } else {
         drawText(font, this.search, searchX + 14.0f, searchY + 9.5f, 6.0f, glass ? GLASS_TEXT : TEXT_MUTED);
         if (this.searchFocused && System.currentTimeMillis() / 400L % 2L == 0L) {
            float caretX = searchX + 14.0f + font.getStringWidth(this.search, 6.0f) + 0.5f;
            NVGRenderer.rect(caretX, searchY + 3.5f, 0.7f, 7.0f, glass ? GLASS_TEXT : TEXT_MUTED);
         }
      }

      drawText(font, "Functions", x + 14.0f, y + 40.5f, 6.0f, glass ? GLASS_TEXT_MUTED : TEXT_FAINT);

      float pillY = y + 43.0f + this.sidebarPill.getValue();
      if (glass) {
         NVGRenderer.roundedRectGradient(x + 12.0f, pillY, 95.0f, 16.0f, 5.0f,
            ColorUtility.applyOpacity(GLASS_ACCENT_SOFT, 0.82f), ColorUtility.applyOpacity(GLASS_ACCENT, 0.70f), 90.0f);
         drawGlassStroke(x + 12.0f, pillY, 95.0f, 16.0f, 5.0f, 0x52FFFFFF, 0.7f);
      } else {
         NVGRenderer.roundedRect(x + 12.0f, pillY, 95.0f, 16.0f, 4.0f, ACCENT);
      }
      for (CategorySection section : this.sections) {
         float rowY = y + 43.0f + section.index * 18.0f;
         float selected = section.selected.getValue();
         int iconColor = glass ? GLASS_TEXT_MUTED : ColorUtility.mix(TEXT_MUTED, WHITE, selected);
         drawCategoryIcon(section.category, x + 18.0f, rowY + 4.0f, 8.0f, iconColor);
         drawText(font, section.name, x + 32.0f, rowY + 10.5f, 7.0f, iconColor);
      }
      if (glass) {
         NVGRenderer.scissor(x + 12.0f, pillY, 95.0f, 16.0f, () -> {
            for (CategorySection section : this.sections) {
               float rowY = y + 43.0f + section.index * 18.0f;
               drawCategoryIcon(section.category, x + 18.0f, rowY + 4.0f, 8.0f, WHITE);
               drawText(font, section.name, x + 32.0f, rowY + 10.5f, 7.0f, WHITE);
            }
         });
      }

      float configsRowY = y + 43.0f + CATEGORY_ORDER.length * 18.0f + 6.0f;
      boolean configsHovered = isHovered(x + 12.0f, configsRowY, 95.0f, 16.0f);
      if (this.configsOpen || configsHovered) {
         NVGRenderer.roundedRect(x + 12.0f, configsRowY, 95.0f, 16.0f, 4.0f,
            glass ? (this.configsOpen ? 0x50FFFFFF : 0x32FFFFFF) : (this.configsOpen ? 0x2EFFFFFF : 0x1CFFFFFF));
      }
      int configsColor = this.configsOpen || configsHovered ? (glass ? GLASS_TEXT : WHITE) : (glass ? GLASS_TEXT_MUTED : TEXT_MUTED);
      drawFolderIcon(x + 18.0f, configsRowY + 4.0f, 8.0f, configsColor);
      drawText(font, "Configs", x + 32.0f, configsRowY + 10.5f, 7.0f, configsColor);

      NVGRenderer.scissor(x, y + 1.0f, WIN_W, WIN_H - 2.0f, () -> this.drawSections(font));
   }

   private void drawSections(NVGTextRenderer font) {
      float x = this.winX;
      float y = this.winY;
      String needle = this.search.trim().toLowerCase(Locale.ROOT);
      float headerOffset = 0.0f;
      this.maxScroll = 0.0f;

      for (CategorySection section : this.sections) {
         List<ModuleCard> visible = new ArrayList<>();
         for (ModuleCard card : section.cards) {
            if (matchesSearch(card.module, needle)) {
               visible.add(card);
            } else {
               card.x = -1.0f;
            }
         }
         if (visible.isEmpty() && !needle.isEmpty()) {
            for (ModuleCard card : section.cards) {
               card.x = -1.0f;
            }
            section.y = -1000.0f;
            continue;
         }

         section.y = headerOffset;
         float headerY = y + 16.0f + headerOffset - this.scrollValue;
         int sectionText = glass() ? GLASS_TEXT : TEXT;
         drawCategoryIcon(section.category, x + 129.0f, headerY, 10.0f, sectionText);
         drawText(font, section.name, x + 143.0f, headerY + 9.0f, 12.0f, sectionText);

         float rowY = headerOffset + 33.0f;
         for (int i = 0; i < visible.size(); i++) {
            ModuleCard card = visible.get(i);
            int col = i % 2;
            card.x = x + CARDS_X + col * CARD_STEP_X;
            card.y = y + rowY - this.scrollValue;
            drawCard(font, card);
            if (col == 1 || i == visible.size() - 1) {
               rowY += CARD_STEP_Y;
            }
         }
         headerOffset += (rowY - headerOffset - 33.0f) + 42.0f;
         this.maxScroll = Math.max(0.0f, headerOffset - 9.0f - (WIN_H - 33.0f));
      }
   }

   private void drawCard(NVGTextRenderer font, ModuleCard card) {
      Feature module = card.module;
      boolean hovered = isHovered(card.x, card.y, CARD_W, CARD_H);
      card.hover.run(hovered ? 1.0f : 0.0f);
      card.enable.run(module.isEnabled() ? 1.0f : 0.0f);

      if (glass()) {
         float hoverAmount = card.hover.getValue();
         int cardTop = ColorUtility.mix(0x3EFFFFFF, 0x68FFFFFF, hoverAmount);
         int cardBottom = ColorUtility.mix(0x26EAF0FF, 0x4EE8E2F4, hoverAmount);
         NVGRenderer.roundedRectGradient(card.x, card.y, CARD_W, CARD_H, 7.0f, cardTop, cardBottom, 90.0f);
         drawGlassStroke(card.x, card.y, CARD_W, CARD_H, 7.0f,
            ColorUtility.applyOpacity(0xFFFFFFFF, 0.18f + 0.12f * hoverAmount), 0.7f);
      } else {
         int base = CARD_BASE;
         int hoverColor = CARD_HOVER;
         NVGRenderer.roundedRect(card.x, card.y, CARD_W, CARD_H, 6.0f, ColorUtility.mix(base, hoverColor, card.hover.getValue()));
      }

      float toggleX = card.x + CARD_W - 25.0f;
      float toggleY = card.y + (CARD_H - 7.0f) / 2.0f;
      int toggleColor = ColorUtility.mix(glass() ? GLASS_TOGGLE_OFF : TOGGLE_OFF,
         glass() ? GLASS_TOGGLE_ON : ACCENT, card.enable.getValue());
      NVGRenderer.roundedRect(toggleX, toggleY, 14.5f, 7.0f, 3.5f, toggleColor);
      if (glass()) {
         drawGlassStroke(toggleX, toggleY, 14.5f, 7.0f, 3.5f,
            ColorUtility.mix(0x80333F53, 0x526A98E7, card.enable.getValue()), 0.65f);
      }
      float knobX = toggleX + 1.0f + 5.0f * card.enable.getValue();
      NVGRenderer.roundedRect(knobX, toggleY + 1.0f, 7.5f, 5.0f, 2.5f, WHITE);
      if (glass()) {
         drawGlassStroke(knobX, toggleY + 1.0f, 7.5f, 5.0f, 2.5f, 0x4D445066, 0.55f);
      }

      if (card == this.bindingCard) {
         drawText(font, "Press a key", card.x + 7.0f, card.y + 16.5f, 7.0f, glass() ? GLASS_ACCENT : ACCENT);
      } else {
         float nameAlpha = 0.85f + 0.15f * Math.max(card.enable.getValue(), card.hover.getValue());
         drawText(font, module.getName(), card.x + 7.0f, card.y + 16.5f, 7.0f,
            ColorUtility.applyOpacity(glass() ? GLASS_TEXT : TEXT, nameAlpha));
      }
   }

   private void drawSettings(SettingsWindow window) {
      float x = window.x;
      float y = window.y;
      float w = SETTINGS_W;
      float h = window.height;

      if (glass()) {
         drawGlassPanel(x, y, w, h, 6.0f + 5.0f * window.alpha, window.alpha);
      } else {
         for (int i = 3; i >= 1; i--) {
            NVGRenderer.roundedRect(x - i * 2.0f, y - i * 2.0f, w + i * 4.0f, h + i * 4.0f, 8.0f + i * 2.0f, 0x16000000);
         }
         NVGRenderer.roundedRect(x, y, w, h, 6.0f + 5.0f * window.alpha, 0xF71B1C23);
      }
      NVGTextRenderer font = FontRepository.getFont("productsans-medium");

      drawText(font, window.card.module.getName(), x + 9.0f, y + 16.0f, 9.0f, glass() ? GLASS_TEXT : TEXT);
      drawCloseIcon(x + w - 17.0f, y + 9.0f, 8.0f,
         isHovered(x + w - 17.0f, y + 9.0f, 8.0f, 8.0f)
            ? (glass() ? GLASS_TEXT : TEXT) : (glass() ? GLASS_TEXT_MUTED : TEXT_MUTED));

      NVGRenderer.scissor(x, y + 24.0f, w, h - 29.0f, () -> this.drawSettingsRows(window, font));
   }

   private void drawSettingsRows(SettingsWindow window, NVGTextRenderer font) {
      float x = window.x;
      float y = window.y;
      float w = SETTINGS_W;
      Feature module = window.card.module;
      float rowY = y + 24.0f - window.scroll;
      int separator = glass() ? GLASS_SEPARATOR : SEP;

      window.enable.run(module.isEnabled() ? 1.0f : 0.0f);
      float enable = window.enable.getValue();

      float nameBaseline = rowY + 12.0f;
      float nameAlpha = 0.7f + 0.3f * enable;
      drawText(font, "Enabled", x + 10.0f, nameBaseline, 8.0f,
         ColorUtility.applyOpacity(glass() ? GLASS_TEXT : TEXT, nameAlpha));
      drawCheck(x + w - 22.0f, rowY + 5.0f, enable);
      NVGRenderer.rect(x, rowY + 18.0f, w, 0.5f, separator);
      rowY += 18.0f;

      for (Setting setting : module.settings) {
         if (!setting.isVisible()) {
            continue;
         }
         if (setting instanceof BooleanSetting bool) {
            window.boolAnims.computeIfAbsent(setting.getName(), name -> new Animation(Easing.EASE_OUT_QUART, 300)).run(bool.getValue() ? 1.0f : 0.0f);
            float enableAnim = window.boolAnims.get(setting.getName()).getValue();
            drawText(font, setting.getDisplayName(), x + 10.0f, rowY + 12.0f, 8.0f,
               ColorUtility.applyOpacity(glass() ? GLASS_TEXT : TEXT, 0.7f + 0.3f * enableAnim));
            drawCheck(x + w - 22.0f, rowY + 5.0f, enableAnim);
            NVGRenderer.rect(x, rowY + 18.0f, w, 0.5f, separator);
            rowY += 18.0f;
         } else if (setting instanceof ModeSetting mode) {
            drawText(font, setting.getDisplayName(), x + 10.0f, rowY + 12.0f, 8.0f, glass() ? GLASS_TEXT_MUTED : TEXT_MUTED);
            String value = mode.getValue();
            float valueWidth = font.getStringWidth(value, 7.0f);
            drawText(font, value, x + w - 10.0f - valueWidth, rowY + 11.5f, 7.0f, glass() ? GLASS_ACCENT : ACCENT);
            NVGRenderer.rect(x, rowY + 18.0f, w, 0.5f, separator);
            rowY += 18.0f;
         } else if (setting instanceof MultiSelectSetting choices) {
            drawText(font, setting.getDisplayName(), x + 10.0f, rowY + 12.0f, 8.0f, glass() ? GLASS_TEXT_MUTED : TEXT_MUTED);
            String value = choices.selectionLabel();
            drawText(font, value, x + w - 10.0f - font.getStringWidth(value, 7.0f), rowY + 11.5f, 7.0f, glass() ? GLASS_ACCENT : ACCENT);
            NVGRenderer.rect(x, rowY + 18.0f, w, 0.5f, separator);
            rowY += 18.0f;
            if (window.expandedChoices.contains(choices)) {
               String[] options = choices.options();
               for (int i = 0; i < options.length; i++) {
                  drawText(font, options[i], x + 17.0f, rowY + 12.0f, 8.0f, glass() ? GLASS_TEXT : TEXT);
                  drawCheck(x + w - 22.0f, rowY + 5.0f, choices.selected(i) ? 1 : 0);
                  NVGRenderer.rect(x, rowY + 18.0f, w, 0.5f, separator);
                  rowY += 18.0f;
               }
            }
         } else if (setting instanceof NumberSetting number) {
            drawText(font, setting.getDisplayName(), x + 10.0f, rowY + 12.0f, 8.0f, glass() ? GLASS_TEXT_MUTED : TEXT_MUTED);
            String value = formatNumber(number.getValue());
            float valueWidth = font.getStringWidth(value, 7.0f);
            drawText(font, value, x + w - 10.0f - valueWidth, rowY + 11.5f, 7.0f, glass() ? GLASS_TEXT_MUTED : TEXT_MUTED);
            float trackX = x + 10.0f;
            float trackW = w - 20.0f;
            float trackY = rowY + 20.0f;
            NVGRenderer.roundedRect(trackX, trackY, trackW, 2.0f, 1.0f, glass() ? 0x3A33425E : 0x2EFFFFFF);
            double range = number.getMaximum() - number.getMinimum();
            double fraction = range == 0.0 ? 0.0 : (number.getValue() - number.getMinimum()) / range;
            float fillW = (float)Math.max(0.0, Math.min(1.0, fraction)) * trackW;
            NVGRenderer.roundedRect(trackX, trackY, fillW, 2.0f, 1.0f, glass() ? GLASS_ACCENT : ACCENT);
            NVGRenderer.roundedRect(trackX + fillW - 1.5f, trackY - 3.0f, 3.0f, 8.0f, 1.5f, WHITE);
            NVGRenderer.rect(x, rowY + 26.0f, w, 0.5f, separator);
            rowY += 26.0f;
         }
      }
      window.contentHeight = rowY - (y + 24.0f) + window.scroll;
   }

   private void drawCheck(float x, float y, float enable) {
      int off = glass() ? GLASS_TOGGLE_OFF : TOGGLE_OFF;
      int on = glass() ? GLASS_TOGGLE_ON : ACCENT;
      NVGRenderer.roundedRect(x, y, 13.0f, 8.0f, 4.0f, ColorUtility.mix(off, on, enable));
      if (glass()) {
         drawGlassStroke(x, y, 13.0f, 8.0f, 4.0f, ColorUtility.mix(0x80333F53, 0x526A98E7, enable), 0.65f);
      }
      float knobX = x + 1.0f + 5.0f * enable;
      NVGRenderer.roundedRect(knobX, y + 1.0f, 6.0f, 6.0f, 3.0f, WHITE);
      if (glass()) {
         drawGlassStroke(knobX, y + 1.0f, 6.0f, 6.0f, 3.0f, 0x4D445066, 0.55f);
      }
   }

   private static void drawGlassPanel(float x, float y, float width, float height, float radius, float alpha) {
      drawGlassPanel(x, y, width, height, radius, alpha, false);
   }

   private static void drawGlassPanel(float x, float y, float width, float height, float radius, float alpha, boolean sidebar) {
      // applyOpacity replaces the source alpha, so the material opacity is kept
      // explicitly here instead of being hidden in the ARGB color constants.
      int top = sidebar ? 0xFFF5F9FF : 0xFFF3F7FF;
      int bottom = sidebar ? 0xFFE0E8F5 : 0xFFE7D9EF;
      float topOpacity = (sidebar ? 0.20f : 0.14f) * alpha;
      float bottomOpacity = (sidebar ? 0.16f : 0.11f) * alpha;
      NVGRenderer.roundedRectGradient(x, y, width, height, radius,
         ColorUtility.applyOpacity(top, topOpacity), ColorUtility.applyOpacity(bottom, bottomOpacity), 90.0f);
      drawGlassTint(x, y, width, height, radius, alpha, sidebar);
   }

   private static void drawGlassSidebarLayer(float x, float y, float width, float height, float radius) {
      NVGRenderer.roundedRectGradient(x, y, width, height, radius,
         ColorUtility.applyOpacity(0xFFF6FAFF, 0.10f), ColorUtility.applyOpacity(0xFFDDE7F5, 0.14f), 0.0f);
      NVGRenderer.roundedRectGradient(x + width - 8.0f, y + 3.0f, 8.0f, height - 6.0f, 2.0f,
         ColorUtility.applyOpacity(0xFFFFFFFF, 0.0f), ColorUtility.applyOpacity(0xFFFFFFFF, 0.12f), 0.0f);
      drawGlassLine(x + width - 0.5f, y + 10.0f, x + width - 0.5f, y + height - 10.0f,
         ColorUtility.applyOpacity(0xFFFFFFFF, 0.28f), 0.75f);
      drawGlassLine(x + width + 0.5f, y + 12.0f, x + width + 0.5f, y + height - 12.0f,
         ColorUtility.applyOpacity(0xFF46536B, 0.10f), 0.65f);
   }

   private static void drawGlassTint(float x, float y, float width, float height, float radius, float alpha, boolean sidebar) {
      NVGRenderer.roundedRectGradient(x, y, width, height, radius,
         ColorUtility.applyOpacity(0xFFF9FCFF, (sidebar ? 0.08f : 0.06f) * alpha),
         ColorUtility.applyOpacity(0xFFE8D7F0, (sidebar ? 0.07f : 0.08f) * alpha), 90.0f);
      drawGlassTopSheen(x + 1.0f, y + 1.0f, width - 2.0f, Math.min(height * 0.34f, 28.0f),
         Math.max(1.0f, radius - 1.0f), alpha);

      // Keep the stroke inside the fill to avoid clipping at rounded corners.
      drawGlassStroke(x + 0.75f, y + 0.75f, width - 1.5f, height - 1.5f,
         Math.max(1.0f, radius - 0.75f), ColorUtility.applyOpacity(0xFFFFFFFF, 0.52f * alpha), 1.0f);
   }

   private static void drawGlassTopSheen(float x, float y, float width, float height, float radius, float alpha) {
      long vg = NVGRenderer.getContext();
      NVGRenderer.applyColor(ColorUtility.applyOpacity(0xFFFFFFFF, 0.12f * alpha), NVGRenderer.NVG_COLOR_1);
      NVGRenderer.applyColor(ColorUtility.applyOpacity(0xFFFFFFFF, 0.0f), NVGRenderer.NVG_COLOR_2);
      nvgLinearGradient(vg, x, y, x, y + height, NVGRenderer.NVG_COLOR_1, NVGRenderer.NVG_COLOR_2, NVGRenderer.NVG_PAINT);
      nvgBeginPath(vg);
      nvgRoundedRectVarying(vg, x, y, width, height, radius, radius, 0.0f, 0.0f);
      nvgFillPaint(vg, NVGRenderer.NVG_PAINT);
      nvgFill(vg);
   }

   private static void drawGlassStroke(float x, float y, float width, float height, float radius, int color, float thickness) {
      long vg = NVGRenderer.getContext();
      NVGRenderer.applyColor(color, NVGRenderer.NVG_COLOR_1);
      nvgBeginPath(vg);
      nvgRoundedRect(vg, x, y, width, height, Math.max(0.0f, radius));
      nvgStrokeWidth(vg, thickness);
      nvgStrokeColor(vg, NVGRenderer.NVG_COLOR_1);
      nvgStroke(vg);
   }

   private static void drawGlassLine(float x1, float y1, float x2, float y2, int color, float thickness) {
      long vg = NVGRenderer.getContext();
      NVGRenderer.applyColor(color, NVGRenderer.NVG_COLOR_1);
      nvgBeginPath(vg);
      nvgMoveTo(vg, x1, y1);
      nvgLineTo(vg, x2, y2);
      nvgStrokeWidth(vg, thickness);
      nvgStrokeColor(vg, NVGRenderer.NVG_COLOR_1);
      nvgStroke(vg);
   }

   private void drawMagnifierIcon(float x, float y, float size, int color) {
      long vg = NVGRenderer.getContext();
      NVGRenderer.applyColor(color, NVGRenderer.NVG_COLOR_1);
      nvgBeginPath(vg);
      nvgStrokeColor(vg, NVGRenderer.NVG_COLOR_1);
      nvgStrokeWidth(vg, Math.max(0.8f, size / 9.0f));
      nvgCircle(vg, x + size * 0.4f, y + size * 0.4f, size * 0.27f);
      nvgStroke(vg);
      nvgBeginPath(vg);
      nvgMoveTo(vg, x + size * 0.6f, y + size * 0.6f);
      nvgLineTo(vg, x + size * 0.85f, y + size * 0.85f);
      nvgStroke(vg);
   }

   private void drawCloseIcon(float x, float y, float size, int color) {
      long vg = NVGRenderer.getContext();
      NVGRenderer.applyColor(color, NVGRenderer.NVG_COLOR_1);
      nvgBeginPath(vg);
      nvgStrokeColor(vg, NVGRenderer.NVG_COLOR_1);
      nvgStrokeWidth(vg, 1.0f);
      nvgMoveTo(vg, x + 2.0f, y + 2.0f);
      nvgLineTo(vg, x + size - 2.0f, y + size - 2.0f);
      nvgMoveTo(vg, x + size - 2.0f, y + 2.0f);
      nvgLineTo(vg, x + 2.0f, y + size - 2.0f);
      nvgStroke(vg);
   }

   private void drawCategoryIcon(Category category, float x, float y, float size, int color) {
      long vg = NVGRenderer.getContext();
      NVGRenderer.applyColor(color, NVGRenderer.NVG_COLOR_1);
      nvgBeginPath(vg);
      nvgStrokeColor(vg, NVGRenderer.NVG_COLOR_1);
      nvgStrokeWidth(vg, Math.max(0.9f, size / 8.0f));
      nvgLineCap(vg, NVG_ROUND);
      switch (category) {
         case COMBAT -> {
            float cx = x + size * 0.5f;
            float cy = y + size * 0.5f;
            nvgCircle(vg, cx, cy, size * 0.3f);
            nvgStroke(vg);
            nvgBeginPath(vg);
            nvgMoveTo(vg, cx, y + size * 0.05f);
            nvgLineTo(vg, cx, y + size * 0.3f);
            nvgMoveTo(vg, cx, y + size * 0.7f);
            nvgLineTo(vg, cx, y + size * 0.95f);
            nvgMoveTo(vg, x + size * 0.05f, cy);
            nvgLineTo(vg, x + size * 0.3f, cy);
            nvgMoveTo(vg, x + size * 0.7f, cy);
            nvgLineTo(vg, x + size * 0.95f, cy);
            nvgStroke(vg);
         }
         case MOVEMENT -> {
            for (int i = 0; i < 2; i++) {
               float baseX = x + size * (0.15f + i * 0.38f);
               nvgMoveTo(vg, baseX, y + size * 0.18f);
               nvgLineTo(vg, baseX + size * 0.32f, y + size * 0.5f);
               nvgLineTo(vg, baseX, y + size * 0.82f);
            }
            nvgStroke(vg);
         }
         case VISUAL -> {
            nvgEllipse(vg, x + size * 0.5f, y + size * 0.5f, size * 0.45f, size * 0.28f);
            nvgStroke(vg);
            nvgBeginPath(vg);
            nvgFillColor(vg, NVGRenderer.NVG_COLOR_1);
            nvgCircle(vg, x + size * 0.5f, y + size * 0.5f, size * 0.12f);
            nvgFill(vg);
         }
         case PLAYER -> {
            nvgCircle(vg, x + size * 0.5f, y + size * 0.3f, size * 0.16f);
            nvgStroke(vg);
            nvgBeginPath(vg);
            nvgArc(vg, x + size * 0.5f, y + size * 0.88f, size * 0.32f, 3.5308f, 5.8936f, NVG_CW);
            nvgStroke(vg);
         }
         default -> {
            nvgBeginPath(vg);
            nvgFillColor(vg, NVGRenderer.NVG_COLOR_1);
            for (int i = 0; i < 3; i++) {
               nvgCircle(vg, x + size * (0.2f + i * 0.3f), y + size * 0.5f, size * 0.09f);
            }
            nvgFill(vg);
         }
      }
   }

   private void drawFolderIcon(float x, float y, float size, int color) {
      long vg = NVGRenderer.getContext();
      NVGRenderer.applyColor(color, NVGRenderer.NVG_COLOR_1);
      nvgBeginPath(vg);
      nvgStrokeColor(vg, NVGRenderer.NVG_COLOR_1);
      nvgStrokeWidth(vg, Math.max(0.9f, size / 8.0f));
      nvgLineJoin(vg, NVG_ROUND);
      nvgLineCap(vg, NVG_ROUND);
      nvgMoveTo(vg, x + size * 0.12f, y + size * 0.78f);
      nvgLineTo(vg, x + size * 0.12f, y + size * 0.24f);
      nvgLineTo(vg, x + size * 0.38f, y + size * 0.24f);
      nvgLineTo(vg, x + size * 0.5f, y + size * 0.4f);
      nvgLineTo(vg, x + size * 0.88f, y + size * 0.4f);
      nvgLineTo(vg, x + size * 0.88f, y + size * 0.78f);
      nvgClosePath(vg);
      nvgStroke(vg);
   }

   private float configsContentHeight() {
      return 18.0f * ConfigManager.getConfigNames().length + 18.0f;
   }

   private float configsHeight() {
      return Math.min(SETTINGS_MAX_H, 29.0f + this.configsContentHeight() + 4.0f);
   }

   private void drawConfigs() {
      float x = this.configsX;
      float y = this.configsY;
      float w = SETTINGS_W;
      float h = this.configsHeight();

      if (glass()) {
         drawGlassPanel(x, y, w, h, 6.0f + 5.0f * this.configsAnim.getValue(), Math.min(1.0f, this.configsAnim.getValue()));
      } else {
         for (int i = 3; i >= 1; i--) {
            NVGRenderer.roundedRect(x - i * 2.0f, y - i * 2.0f, w + i * 4.0f, h + i * 4.0f, 8.0f + i * 2.0f, 0x16000000);
         }
         NVGRenderer.roundedRect(x, y, w, h, 6.0f + 5.0f * this.configsAnim.getValue(), 0xF71B1C23);
      }
      NVGTextRenderer font = FontRepository.getFont("productsans-medium");
      drawText(font, "Configs", x + 9.0f, y + 16.0f, 9.0f, glass() ? GLASS_TEXT : TEXT);
      drawCloseIcon(x + w - 17.0f, y + 9.0f, 8.0f,
         isHovered(x + w - 17.0f, y + 9.0f, 8.0f, 8.0f)
            ? (glass() ? GLASS_TEXT : TEXT) : (glass() ? GLASS_TEXT_MUTED : TEXT_MUTED));
      NVGRenderer.scissor(x, y + 24.0f, w, h - 29.0f, () -> this.drawConfigsRows(font));
   }

   private void drawConfigsRows(NVGTextRenderer font) {
      float x = this.configsX;
      float y = this.configsY;
      float w = SETTINGS_W;
      String[] configs = ConfigManager.getConfigNames();
      float rowY = y + 24.0f - this.configsScroll;
      int separator = glass() ? GLASS_SEPARATOR : SEP;
      for (String name : configs) {
         float saveWidth = font.getStringWidth("Save", 7.0f);
         float delWidth = font.getStringWidth("Del", 7.0f);
         boolean rowHovered = isHovered(x, rowY, w - 52.0f, 18.0f);
         boolean saveHovered = isHovered(x + w - 38.0f - saveWidth, rowY, saveWidth + 8.0f, 18.0f);
         boolean delHovered = isHovered(x + w - 14.0f - delWidth, rowY, delWidth + 8.0f, 18.0f);
         drawText(font, name, x + 10.0f, rowY + 12.0f, 8.0f,
            rowHovered ? (glass() ? GLASS_TEXT : TEXT) : (glass() ? GLASS_TEXT_MUTED : TEXT_MUTED));
         drawText(font, "Save", x + w - 34.0f - saveWidth, rowY + 11.5f, 7.0f,
            saveHovered ? (glass() ? GLASS_ACCENT : ACCENT) : (glass() ? GLASS_TEXT_FAINT : TEXT_FAINT));
         drawText(font, "Del", x + w - 10.0f - delWidth, rowY + 11.5f, 7.0f,
            delHovered ? (glass() ? 0xFFE16D82 : 0xFFFF6B6B) : (glass() ? GLASS_TEXT_FAINT : TEXT_FAINT));
         NVGRenderer.rect(x, rowY + 18.0f, w, 0.5f, separator);
         rowY += 18.0f;
      }
      boolean createHovered = isHovered(x + 10.0f, rowY, 48.0f, 18.0f);
      drawText(font, "Create", x + 10.0f, rowY + 11.5f, 7.0f,
         createHovered ? (glass() ? GLASS_TEXT : WHITE) : (glass() ? GLASS_TEXT_MUTED : TEXT_MUTED));
      boolean folderHovered = isHovered(x + 58.0f, rowY, 76.0f, 18.0f);
      drawText(font, "Open Folder", x + 58.0f, rowY + 11.5f, 7.0f,
         folderHovered ? (glass() ? GLASS_TEXT : WHITE) : (glass() ? GLASS_TEXT_MUTED : TEXT_MUTED));
   }

   private void handleConfigsClick(int button) {
      float x = this.configsX;
      float y = this.configsY;
      float w = SETTINGS_W;

      if (button == 0 && isHovered(x + w - 17.0f, y + 9.0f, 8.0f, 8.0f)) {
         this.configsOpen = false;
         return;
      }
      if (button == 0 && isHovered(x, y, w, 24.0f)) {
         this.dragConfigs = true;
         this.configsDragX = (float)this.mouseX - x;
         this.configsDragY = (float)this.mouseY - y;
         return;
      }
      if (button != 0) {
         return;
      }

      NVGTextRenderer font = FontRepository.getFont("productsans-medium");
      float saveWidth = font.getStringWidth("Save", 7.0f);
      float delWidth = font.getStringWidth("Del", 7.0f);
      String[] configs = ConfigManager.getConfigNames();
      float rowY = y + 24.0f - this.configsScroll;
      for (String name : configs) {
         if (isHovered(x + w - 38.0f - saveWidth, rowY, saveWidth + 8.0f, 18.0f)) {
            ConfigManager.saveConfig(name);
            return;
         }
         if (isHovered(x + w - 14.0f - delWidth, rowY, delWidth + 8.0f, 18.0f)) {
            ConfigManager.deleteConfig(name);
            return;
         }
         if (isHovered(x, rowY, w - 52.0f, 18.0f)) {
            ConfigManager.loadConfig(name);
            return;
         }
         rowY += 18.0f;
      }
      if (isHovered(x + 10.0f, rowY, 48.0f, 18.0f)) {
         this.createConfig();
      } else if (isHovered(x + 58.0f, rowY, 76.0f, 18.0f)) {
         this.openConfigFolder();
      }
   }

   private void createConfig() {
      String[] existing = ConfigManager.getConfigNames();
      String name = "config";
      int index = 1;
      while (containsName(existing, name)) {
         name = "config" + ++index;
      }
      ConfigManager.saveConfig(name);
   }

   private static boolean containsName(String[] array, String value) {
      for (String entry : array) {
         if (entry.equals(value)) {
            return true;
         }
      }
      return false;
   }

   private void openConfigFolder() {
      try {
         ConfigManager.openConfigDirectory(ConfigManager.getConfigDir().toPath());
      } catch (Exception error) {
         org.slf4j.LoggerFactory.getLogger("samsara-config").warn("Unable to open configuration directory", error);
      }
   }

   private boolean matchesSearch(Feature module, String needle) {
      return needle.isEmpty() || module.getName().toLowerCase(Locale.ROOT).contains(needle);
   }

   private boolean isHovered(float x, float y, float w, float h) {
      return this.mouseX >= x && this.mouseX <= x + w && this.mouseY >= y && this.mouseY <= y + h;
   }

   private void updateInputPosition(double x, double y) {
      // 26.3 already delivers GUI-scaled mouse coordinates.
      this.mouseX = x;
      this.mouseY = y;
   }

   private static void drawText(NVGTextRenderer font, String text, float x, float baselineY, float size, int color) {
      font.drawString(text, x, baselineY, size, color, false, NVG_ALIGN_LEFT | NVG_ALIGN_BASELINE);
   }

   private static String formatNumber(double value) {
      if (value == Math.rint(value)) {
         return Integer.toString((int)value);
      }
      String formatted = String.format(Locale.ROOT, "%.2f", value);
      if (formatted.endsWith("0")) {
         formatted = formatted.substring(0, formatted.length() - 1);
      }
      return formatted;
   }

   @Override
   public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
      this.mouseX = mouseX;
      this.mouseY = mouseY;
      if (NVGRenderer.isAvailable()) {
         try {
            this.extractBlurredBackground(graphics);
         } catch (Exception ignored) {
            graphics.fill(0, 0, this.width, this.height, 0x66000000);
         }
         return;
      }
      super.extractRenderState(graphics, mouseX, mouseY, delta);
      this.updateScroll(-1.0f);
      renderVanillaFallback(graphics);
   }

   private void updateScroll(float ignoredDelta) {
      if (this.scrollTarget > this.maxScroll) {
         this.scrollTarget = this.maxScroll;
      }
      if (this.scrollTarget < 0.0f) {
         this.scrollTarget = 0.0f;
      }
      this.lastFrameTime = System.currentTimeMillis();
      float dt = 16.0f;
      float smoothing = 1.0f - (float)Math.exp(-dt * 0.014);
      this.scrollValue += (this.scrollTarget - this.scrollValue) * smoothing;
      if (Math.abs(this.scrollTarget - this.scrollValue) < 0.01f) {
         this.scrollValue = this.scrollTarget;
      }
   }

   private void renderVanillaFallback(GuiGraphicsExtractor graphics) {
      int x = Math.round(this.winX);
      int y = Math.round(this.winY);
      graphics.fill(x, y, x + WIN_W, y + WIN_H, WINDOW_BG | 0xFF000000);
      graphics.fill(x + 5, y + 5, x + 5 + (int)SIDEBAR_W, y + WIN_H - 5, SIDEBAR_BG | 0xFF000000);

      var font = this.minecraft.font;
      graphics.fill(x + 13, y + 13, x + 106, y + 27, SEARCH_BG | 0xFF000000);
      graphics.text(font, this.search.isEmpty() ? "Search" : this.search, x + 16, y + 18, TEXT_FAINT | 0xFF000000);
      graphics.text(font, "Functions", x + 14, y + 37, TEXT_FAINT | 0xFF000000);

      int pillIndex = this.currentSection == null ? 0 : this.currentSection.index;
      graphics.fill(x + 12, y + 43 + pillIndex * 18, x + 107, y + 59 + pillIndex * 18, ACCENT);
      for (CategorySection section : this.sections) {
         int rowY = y + 43 + section.index * 18;
         int color = section == this.currentSection ? WHITE : TEXT_MUTED;
         graphics.text(font, section.name, x + 32, rowY + 5, (color & 0x00FFFFFF) | 0xFF000000);
      }
      int configsRowY = y + 43 + CATEGORY_ORDER.length * 18 + 6;
      graphics.fill(x + 12, configsRowY, x + 107, configsRowY + 16, (this.configsOpen ? 0x2EFFFFFF : 0x1CFFFFFF) | 0xFF000000);
      graphics.text(font, "Configs", x + 32, configsRowY + 5, WHITE);

      String needle = this.search.trim().toLowerCase(Locale.ROOT);
      int contentTop = y + 16 - Math.round(this.scrollValue);
      int headerOffset = 0;
      for (CategorySection section : this.sections) {
         List<ModuleCard> visible = new ArrayList<>();
         for (ModuleCard card : section.cards) {
            if (matchesSearch(card.module, needle)) {
               visible.add(card);
            } else {
               card.x = -1.0f;
            }
         }
         if (visible.isEmpty() && !needle.isEmpty()) {
            for (ModuleCard card : section.cards) {
               card.x = -1.0f;
            }
            continue;
         }
         section.y = headerOffset;
         graphics.text(font, section.name, x + 143, contentTop + headerOffset, WHITE);
         int rowY = contentTop + headerOffset + 33;
         for (int i = 0; i < visible.size(); i++) {
            ModuleCard card = visible.get(i);
            int col = i % 2;
            card.x = x + (int)CARDS_X + col * (int)CARD_STEP_X;
            card.y = rowY;
            int cardX = Math.round(card.x);
            int cy = rowY;
            graphics.fill(cardX, cy, cardX + (int)CARD_W, cy + (int)CARD_H, CARD_BASE | 0xFF000000);
            graphics.text(font, card.module.getName(), cardX + 7, cy + 10, TEXT);
            int toggleX = cardX + (int)CARD_W - 25;
            int toggleColor = card.module.isEnabled() ? ACCENT : TOGGLE_OFF;
            graphics.fill(toggleX, cy + 10, toggleX + 14, cy + 17, (toggleColor & 0x00FFFFFF) | 0xFF000000);
            if (col == 1 || i == visible.size() - 1) {
               rowY += (int)CARD_STEP_Y;
            }
         }
         headerOffset += (rowY - (contentTop + headerOffset) - 33) + 42;
         this.maxScroll = Math.max(0.0f, headerOffset - 9.0f - (WIN_H - 33.0f));
      }
   }

   @Override
   public void mouseMoved(double mouseX, double mouseY) {
      this.updateInputPosition(mouseX, mouseY);
      super.mouseMoved(mouseX, mouseY);
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      if (!this.closing && this.hudEdit.click(event.x(), event.y(), event.button())) {
         this.closeDestination = new com.samsara.ui.hud.editor.HudEditorScreen(); this.onClose(); return true;
      }
      if (this.closing) {
         return true;
      }
      this.updateInputPosition(event.x(), event.y());
      int button = MouseButtons.normalize(event.button());

      for (SettingsWindow window : this.windows) {
         boolean insideWindow = isHovered(window.x, window.y, SETTINGS_W, window.height);
         if (insideWindow) {
            this.handleSettingsClick(window, button);
            return true;
         }
      }
      if (this.configsOpen && isHovered(this.configsX, this.configsY, SETTINGS_W, this.configsHeight())) {
         this.handleConfigsClick(button);
         return true;
      }
      boolean insideMainWindow = isHovered(this.winX, this.winY, WIN_W, WIN_H);
      if (!insideMainWindow) {
         for (SettingsWindow window : this.windows) {
            window.showing = false;
         }
         this.configsOpen = false;
         return super.mouseClicked(event, doubleClick);
      }

      if (isHovered(this.winX + 13.0f, this.winY + 13.0f, 93.0f, 14.0f)) {
         this.searchFocused = true;
         return true;
      }
      if (this.searchFocused) {
         this.searchFocused = false;
      }

      float configsRowY = this.winY + 43.0f + CATEGORY_ORDER.length * 18.0f + 6.0f;
      if (isHovered(this.winX + 12.0f, configsRowY, 95.0f, 16.0f)) {
         this.configsOpen = !this.configsOpen;
         if (this.configsOpen) {
            this.configsX = this.winX + WIN_W + 10.0f;
            this.configsY = this.winY;
            if (this.configsX + SETTINGS_W > this.width - 4.0f) {
               this.configsX = Math.max(4.0f, this.winX - SETTINGS_W - 10.0f);
            }
            this.configsX = Math.clamp(com.samsara.ui.hud.ClickGuiLayouts.x("modern:configs",this.configsX),4,Math.max(4,this.width-SETTINGS_W-4));
            this.configsY = Math.clamp(com.samsara.ui.hud.ClickGuiLayouts.y("modern:configs",this.configsY),4,Math.max(4,this.height-40));
            this.configsScroll = 0.0f;
            this.configsScrollTarget = 0.0f;
         }
         return true;
      }

      for (CategorySection section : this.sections) {
         float rowY = this.winY + 43.0f + section.index * 18.0f;
         if (isHovered(this.winX + 12.0f, rowY, 95.0f, 16.0f)) {
            this.scrollTarget = Math.max(0.0f, Math.min(section.y, this.maxScroll));
            return true;
         }
      }

      ModuleCard card = this.cardAt(this.mouseX, this.mouseY);
      if (card != null) {
         this.handleCardClick(card, button);
         return true;
      }

      if (button == 0) {
         this.dragWindow = true;
         this.dragX = (float)this.mouseX - this.winX;
         this.dragY = (float)this.mouseY - this.winY;
      }
      return true;
   }

   private ModuleCard cardAt(double mx, double my) {
      for (CategorySection section : this.sections) {
         for (ModuleCard card : section.cards) {
            if (card.x >= 0.0f && isHovered(card.x, card.y, CARD_W, CARD_H)) {
               return card;
            }
         }
      }
      return null;
   }

   private void handleCardClick(ModuleCard card, int button) {
      if (button == 0) {
         card.module.toggle();
      } else if (button == 1) {
         this.openSettings(card);
      } else if (button == 2) {
         this.bindingCard = card;
      }
   }

   private void openSettings(ModuleCard card) {
      for (SettingsWindow window : this.windows) {
         if (window.card == card) {
            return;
         }
      }
      float x = this.winX + WIN_W + 10.0f;
      float y = this.winY;
      if (!this.windows.isEmpty()) {
         SettingsWindow last = this.windows.getLast();
         if (last.y + last.height < this.winY + WIN_H) {
            y = last.y + last.height + 10.0f;
            x = last.x;
         } else {
            x = last.x + SETTINGS_W + 10.0f;
         }
      }
      if (!this.windows.isEmpty() && (this.windows.size() > 4 || x + SETTINGS_W > this.width - 4.0f)) {
         SettingsWindow first = this.windows.getFirst();
         x = first.x;
         y = first.y;
         first.showing = false;
      }
      if (x + SETTINGS_W > this.width - 4.0f) {
         x = Math.max(4.0f, this.winX - SETTINGS_W - 10.0f);
      }
      y = Math.max(4.0f, Math.min(y, this.height - SETTINGS_MAX_H));
      this.windows.add(new SettingsWindow(card, x, y));
   }

   private void handleSettingsClick(SettingsWindow window, int button) {
      float x = window.x;
      float y = window.y;
      float w = SETTINGS_W;
      Feature module = window.card.module;

      if (button == 0 && isHovered(x + w - 17.0f, y + 9.0f, 8.0f, 8.0f)) {
         window.showing = false;
         return;
      }
      if (button == 0 && isHovered(x, y, w, 24.0f)) {
         this.draggingSettings = window;
         this.settingsDragX = (float)this.mouseX - x;
         this.settingsDragY = (float)this.mouseY - y;
         return;
      }

      float rowY = y + 24.0f - window.scroll;
      if (button == 0 && isHovered(x, rowY, w, 18.0f)) {
         module.toggle();
         return;
      }
      rowY += 18.0f;

      for (Setting setting : module.settings) {
         if (!setting.isVisible()) {
            continue;
         }
         if (setting instanceof BooleanSetting bool) {
            if (button == 0 && isHovered(x, rowY, w, 18.0f)) {
               bool.setValue(!bool.getValue());
               return;
            }
            rowY += 18.0f;
         } else if (setting instanceof ModeSetting mode) {
            if (button == 0 && isHovered(x, rowY, w, 18.0f)) {
               String[] values = mode.getOptions();
               if (values.length > 0) {
                  int index = 0;
                  for (int i = 0; i < values.length; i++) {
                     if (values[i].equals(mode.getValue())) {
                        index = i;
                        break;
                     }
                  }
                  mode.setValue(values[(index + 1) % values.length]);
               }
               return;
            }
            rowY += 18.0f;
         } else if (setting instanceof MultiSelectSetting choices) {
            if (button == 0 && isHovered(x, rowY, w, 18.0f)) {
               if (!window.expandedChoices.remove(choices)) window.expandedChoices.add(choices);
               return;
            }
            rowY += 18.0f;
            if (window.expandedChoices.contains(choices)) {
               for (int i = 0; i < choices.options().length; i++) {
                  if (button == 0 && isHovered(x, rowY, w, 18.0f)) {
                     choices.select(i);
                     return;
                  }
                  rowY += 18.0f;
               }
            }
         } else if (setting instanceof NumberSetting number) {
            if (button == 0 && isHovered(x, rowY, w, 26.0f)) {
               this.draggingSlider = number;
               this.sliderWindow = window;
               this.updateSlider(number, window, x, rowY);
               return;
            }
            rowY += 26.0f;
         }
      }
   }

   private void updateSlider(NumberSetting number, SettingsWindow window, float windowX, float rowY) {
      float trackX = windowX + 10.0f;
      float trackW = SETTINGS_W - 20.0f;
      double fraction = (this.mouseX - trackX) / trackW;
      fraction = Math.max(0.0, Math.min(1.0, fraction));
      number.setValue(number.getMinimum() + fraction * (number.getMaximum() - number.getMinimum()));
      window.scrollTarget = window.scroll;
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent event) {
      this.dragWindow = false;
      this.draggingSettings = null;
      this.draggingSlider = null;
      this.sliderWindow = null;
      this.dragConfigs = false;
      return super.mouseReleased(event);
   }

   @Override
   public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
      this.updateInputPosition(event.x(), event.y());
      int button = MouseButtons.normalize(event.button());
      if (this.dragWindow && button == 0) {
         this.winX = Math.max(4.0f, Math.min(this.width - WIN_W - 4.0f, (float)this.mouseX - this.dragX));
         this.winY = Math.max(4.0f, Math.min(this.height - WIN_H - 4.0f, (float)this.mouseY - this.dragY));
         com.samsara.ui.hud.ClickGuiLayouts.put("modern", this.winX, this.winY);
         return true;
      }
      if (this.draggingSettings != null && button == 0) {
         this.draggingSettings.x = Math.max(4.0f, Math.min(this.width - SETTINGS_W - 4.0f, (float)this.mouseX - this.settingsDragX));
         this.draggingSettings.y = Math.max(4.0f, Math.min(this.height - 40.0f, (float)this.mouseY - this.settingsDragY));
         return true;
      }
      if (this.dragConfigs && button == 0) {
         this.configsX = Math.max(4.0f, Math.min(this.width - SETTINGS_W - 4.0f, (float)this.mouseX - this.configsDragX));
         this.configsY = Math.max(4.0f, Math.min(this.height - 40.0f, (float)this.mouseY - this.configsDragY));
         com.samsara.ui.hud.ClickGuiLayouts.put("modern:configs", this.configsX, this.configsY);
         return true;
      }
      if (this.draggingSlider != null && this.sliderWindow != null && button == 0) {
         float rowY = this.sliderWindow.y + 24.0f - this.sliderWindow.scroll + this.sliderWindow.settingRowY(this.draggingSlider);
         this.updateSlider(this.draggingSlider, this.sliderWindow, this.sliderWindow.x, rowY);
         return true;
      }
      return super.mouseDragged(event, deltaX, deltaY);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      this.updateInputPosition(mouseX, mouseY);
      if (this.closing) {
         return true;
      }
      for (SettingsWindow window : this.windows) {
         if (isHovered(window.x, window.y, SETTINGS_W, window.height)) {
            window.scrollTarget -= (float)verticalAmount * 24.0f;
            return true;
         }
      }
      if (this.configsOpen && isHovered(this.configsX, this.configsY, SETTINGS_W, this.configsHeight())) {
         this.configsScrollTarget -= (float)verticalAmount * 24.0f;
         return true;
      }
      if (isHovered(this.winX, this.winY, WIN_W, WIN_H)) {
         this.scrollTarget -= (float)verticalAmount * 28.0f;
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
      if (this.bindingCard != null) {
         this.bindingCard.module.setKey(key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_BACKSPACE ? 0 : key);
         this.bindingCard = null;
         return true;
      }
      if (key == GLFW.GLFW_KEY_ESCAPE) {
         if (this.searchFocused) {
            this.searchFocused = false;
            return true;
         }
         this.onClose();
         return true;
      }
      if (this.searchFocused && key == GLFW.GLFW_KEY_BACKSPACE && !this.search.isEmpty()) {
         this.search = this.search.substring(0, this.search.length() - 1);
         return true;
      }
      return super.keyPressed(event);
   }

   @Override
   public boolean charTyped(CharacterEvent event) {
      if (this.closing) {
         return true;
      }
      if (this.searchFocused && event.isAllowedChatCharacter()) {
         this.search += event.codepointAsString();
         return true;
      }
      return super.charTyped(event);
   }

   @Override
   public void onClose() {
      com.samsara.ui.hud.ClickGuiLayouts.put("modern", this.winX, this.winY);
      this.closing = true;
      for (SettingsWindow window : this.windows) {
         window.showing = false;
      }
      this.bindingCard = null;
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   public void drawHudEditButton() { this.hudEdit.draw(this.width, this.height, this.hudEditOpacity, this.closing); }

   private static final class CategorySection {
      final Category category;
      final String name;
      final int index;
      final List<ModuleCard> cards = new ArrayList<>();
      final Animation selected = new Animation(Easing.EASE_OUT_QUART, 200);
      float y;

      CategorySection(Category category, String name, int index) {
         this.category = category;
         this.name = name;
         this.index = index;
      }
   }

   private final class ModuleCard {
      final Feature module;
      final Animation hover = new Animation(Easing.EASE_OUT_QUART, 300);
      final Animation enable = new Animation(Easing.EASE_OUT_QUART, 300);
      float x = -1.0f;
      float y;

      ModuleCard(Feature module) {
         this.module = module;
      }
   }

   private static final class SettingsWindow {
      final ModuleCard card;
      final Animation anim = new Animation(Easing.EASE_OUT_QUART, 300);
      final Animation enable = new Animation(Easing.EASE_OUT_QUART, 300);
      final java.util.Map<String, Animation> boolAnims = new java.util.HashMap<>();
      final java.util.Set<MultiSelectSetting> expandedChoices = new java.util.HashSet<>();
      float x;
      float y;
      float height = 100.0f;
      float scroll;
      float scrollTarget;
      float contentHeight;
      boolean showing = true;
      float alpha;

      SettingsWindow(ModuleCard card, float x, float y) {
         this.card = card;
         this.x = x;
         this.y = y;
         this.anim.setStartValue(0.0f);
         this.anim.run(1.0f);
      }

      float contentHeight() {
         float offset = 18.0f;
         for (Setting setting : this.card.module.settings) {
            if (!setting.isVisible()) {
               continue;
            }
            if (setting instanceof NumberSetting) {
               offset += 26.0f;
            } else {
               offset += 18.0f;
            }
            if (setting instanceof MultiSelectSetting choices && this.expandedChoices.contains(choices)) {
               offset += choices.options().length * 18.0f;
            }
         }
         return offset;
      }

      float computeHeight() {
         return Math.min(SETTINGS_MAX_H, 29.0f + this.contentHeight());
      }

      float settingRowY(Setting target) {
         float offset = 18.0f;
         for (Setting setting : this.card.module.settings) {
            if (!setting.isVisible()) {
               continue;
            }
            if (setting == target) {
               return offset;
            }
            offset += setting instanceof NumberSetting ? 26.0f : 18.0f;
            if (setting instanceof MultiSelectSetting choices && this.expandedChoices.contains(choices)) {
               offset += choices.options().length * 18.0f;
            }
         }
         return 0.0f;
      }
   }
}
