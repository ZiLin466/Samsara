package com.samsara.ui.clickgui.neverlose;

import com.samsara.ClientBranding;
import com.samsara.config.ConfigManager;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.module.FeatureManager;
import com.samsara.setting.BooleanSetting;
import com.samsara.setting.ChoiceSetting;
import com.samsara.setting.NumberSetting;
import com.samsara.setting.Setting;
import com.samsara.ui.NanoGui;
import com.samsara.ui.hud.ClickGuiLayouts;
import com.samsara.ui.hud.HudLayouts;
import com.samsara.ui.hud.editor.HudEditorScreen;
import com.samsara.util.render.FontRepository;
import com.samsara.util.render.HudBackdrop;
import com.samsara.util.render.NVGRenderer;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.sdl.SDLKeyboard;
import org.lwjgl.sdl.SDLScancode;

import static com.samsara.ui.clickgui.neverlose.NeverloseLayout.*;
import static com.samsara.ui.clickgui.neverlose.NeverloseRenderer.*;

public final class NeverloseClickGuiScreen extends Screen implements NanoGui {
   private static final String POSITION = "neverlose";
   private static final Category[] CATEGORIES = {Category.COMBAT, Category.COMBAT, Category.MOVEMENT,
      Category.PLAYER, Category.VISUAL, Category.MISC};
   private final Map<Feature, ModuleState> modules = new HashMap<>();
   private final Map<Object, Motion> controls = new HashMap<>();
   private final Map<Object, Motion> hover = new HashMap<>();
   private final Map<Object, Double> pulses = new HashMap<>();
   private final Motion menu = new Motion(0, 22), pageMotion = new Motion(1, 30);
   private final Motion selection = new Motion(navigation(0).y(), 30);
   private final Motion scrolling = new Motion(0, 28);
   private final List<Target> targets = new ArrayList<>();
   private List<Section> sections = List.of();
   private List<Config> configs = List.of();
   private List<String> configNames = List.of();
   private List<Feature> packedModules = List.of();
   private List<Integer> moduleColumns = List.of();
   private EditBox search, configName, draggingEditor;
   private Viewport viewport;
   private int navigation;
   private float mouseX, mouseY, opacity, openingScale = 1, scrollTarget, scroll, maximumScroll;
   private float windowGrabX, windowGrabY, scrollbarGrab;
   private boolean draggingWindow, draggingScrollbar, closing, detached = true;
   private Target draggingSlider;
   private Feature binding;
   private Popup popup;
   private Screen destination;
   private String preset = "", status = "", deleteCandidate = "";
   private double statusUntil, deleteUntil;
   private boolean statusError;

   public NeverloseClickGuiScreen() { super(Component.literal("Neverlose")); }
   private static double now() { return System.nanoTime() / 1_000_000_000.0; }

   @Override
   protected void init() {
      if (search == null) {
         search = new EditorBox(minecraft.font, "Search modules");
         search.setMaxLength(120);
         search.setResponder(value -> resetScroll());
         configName = new EditorBox(minecraft.font, "Configuration name");
         configName.setMaxLength(69);
      }
      Viewport center = centered(width, height);
      viewport = viewport(width, height, ClickGuiLayouts.x(POSITION, center.x()), ClickGuiLayouts.y(POSITION, center.y()));
      if (detached) {
         double time = now();
         menu.snap(0, time); menu.to(1, time);
         closing = false; destination = null; detached = false;
         popup = null; binding = null; releaseDrag();
         search.setFocused(false); configName.setFocused(false);
      }
      refreshConfigs();
      refresh(now());
   }

   private void resetScroll() {
      scrollTarget = scroll = 0;
      scrolling.snap(0, now());
      if (popup != null) popup.open = false;
   }

   private List<Feature> visibleModules() {
      String query = search.getValue().strip().toLowerCase(Locale.ROOT).replace(" ", "");
      return FeatureManager.getModules().stream()
         .filter(module -> module == FeatureManager.targets || !module.isHidden())
         .filter(module -> !query.isEmpty() || (navigation == 1 ? module == FeatureManager.targets
            : module != FeatureManager.targets && module.getCategory() == CATEGORIES[navigation]))
         .filter(module -> query.isEmpty() || module.getName().toLowerCase(Locale.ROOT).replace(" ", "").contains(query)
            || module.settings.stream().anyMatch(setting -> setting.isVisible()
               && setting.getDisplayName().toLowerCase(Locale.ROOT).replace(" ", "").contains(query)))
         .sorted(Comparator.<Feature>comparingInt(module -> module == FeatureManager.killAura ? 0 : module == FeatureManager.velocity ? 1 : 2)
            .thenComparing(Feature::getName, String.CASE_INSENSITIVE_ORDER)).toList();
   }

   private static List<Setting> visibleSettings(Feature module) {
      return module.settings.stream().filter(Setting::isVisible)
         .filter(setting -> setting instanceof BooleanSetting || setting instanceof NumberSetting || setting instanceof ChoiceSetting).toList();
   }

   private void refresh(double time) {
      targets.clear();
      if (navigation == 7) { refreshConfigLayout(); return; }
      List<Feature> visible = visibleModules();
      List<List<Setting>> settings = new ArrayList<>();
      List<Float> heights = new ArrayList<>();
      for (Feature module : visible) {
         ModuleState state = modules.computeIfAbsent(module, ignored -> new ModuleState());
         List<Setting> values = visibleSettings(module);
         settings.add(values);
         float reveal = state.motion.to(state.expanded ? 1 : 0, time);
         heights.add(SECTION_HEADER + (values.size() + (module == FeatureManager.targets ? 0 : 1)) * ROW * reveal);
      }
      if (!visible.equals(packedModules)) {
         packedModules = visible;
         moduleColumns = pack(heights, 0).sections().stream().map(rect -> rect.x() == CONTENT.x() ? 0 : 1).toList();
      }
      Packed packed = pack(heights, moduleColumns, scroll);
      maximumScroll = Math.max(0, packed.height() - CONTENT.height());
      scrollTarget = Math.clamp(scrollTarget, 0, maximumScroll);
      if (scroll > maximumScroll) {
         scroll = maximumScroll; scrolling.snap(scroll, time);
         packed = pack(heights, moduleColumns, scroll);
      }
      List<Section> result = new ArrayList<>();
      for (int i = 0; i < visible.size(); i++) {
         Feature module = visible.get(i);
         Rect section = packed.sections().get(i);
         ModuleState state = modules.get(module);
         Rect header = new Rect(section.x(), section.y(), section.width(), SECTION_HEADER);
         targets.add(new Target(header.intersect(CONTENT), header, module, null, Action.HEADER));
         Rect key = binding(section);
         if (module != FeatureManager.targets) targets.add(new Target(key.intersect(CONTENT), key, module, null, Action.BIND));
         List<Row> rows = new ArrayList<>();
         float y = section.y() + SECTION_HEADER;
         if (module != FeatureManager.targets) {
            Rect rowBounds = new Rect(section.x(), y, section.width(), ROW);
            rows.add(row(Kind.ENABLED, rowBounds, "Enabled", "", module, module.isEnabled() ? 1 : 0, 45, time));
            addRowTarget(rowBounds, section, state, module, null, Action.ENABLED);
            y += ROW;
         }
         for (Setting setting : settings.get(i)) {
            Rect rowBounds = new Rect(section.x(), y, section.width(), ROW);
            if (setting instanceof BooleanSetting value) {
               rows.add(row(Kind.BOOLEAN, rowBounds, setting.getDisplayName(), "", setting, value.getValue() ? 1 : 0, 45, time));
               addRowTarget(rowBounds, section, state, module, setting, Action.BOOLEAN);
            } else if (setting instanceof NumberSetting value) {
               float fraction = value.getMaximum() <= value.getMinimum() ? 0 : (float)((value.getValue() - value.getMinimum()) / (value.getMaximum() - value.getMinimum()));
               rows.add(row(Kind.NUMBER, rowBounds, setting.getDisplayName(), BigDecimal.valueOf(value.getValue()).stripTrailingZeros().toPlainString(),
                  setting, fraction, 24, time));
               addRowTarget(rowBounds, section, state, module, setting, Action.NUMBER);
            } else if (setting instanceof ChoiceSetting choices) {
               String label = choices.selectionLabel();
               if (choices.multiple()) {
                  List<String> selected = new ArrayList<>();
                  String[] options = choices.options();
                  for (int index = 0; index < options.length; index++) if (choices.selected(index)) selected.add(options[index]);
                  label = selected.isEmpty() ? "None" : String.join(", ", selected);
               }
               rows.add(row(Kind.CHOICE, rowBounds, setting.getDisplayName(), label, setting, 0, 20, time));
               addRowTarget(rowBounds, section, state, module, setting, Action.CHOICE);
            }
            y += ROW;
         }
         result.add(new Section(section, displayName(module.getName()), module == FeatureManager.targets ? "GLOBAL" : keyLabel(module), state.motion.value(), List.copyOf(rows)));
      }
      sections = List.copyOf(result);
      if (draggingSlider != null) {
         Target live = targets.stream().filter(target -> target.setting == draggingSlider.setting && target.action == Action.NUMBER).findFirst().orElse(null);
         draggingSlider = live != null && live.hit.height() > 0 ? live : null;
      }
      if (binding != null && !visible.contains(binding)) binding = null;
      updatePopup(time);
   }

   private Row row(Kind kind, Rect rowBounds, String label, String value, Object key, float target, double rate, double time) {
      boolean hovered = rowBounds.intersect(CONTENT).contains(mouseX, mouseY) && popup == null;
      float progress = controls.computeIfAbsent(key, ignored -> new Motion(target, rate)).to(target, time);
      float feedback = hover.computeIfAbsent(key, ignored -> new Motion(0, 24)).to(
         hovered || draggingSlider != null && draggingSlider.setting == key ? 1 : 0, time);
      Double pulse = pulses.get(key);
      if (pulse != null) feedback = Math.max(feedback, (float)Math.exp(-18 * (time - pulse)));
      return new Row(kind, rowBounds, label, value, Math.clamp(progress, 0, 1), feedback, hovered);
   }

   private void addRowTarget(Rect row, Rect section, ModuleState state, Feature module, Setting setting, Action action) {
      if (state.expanded && state.motion.value() > .15f) {
         Rect hit = row.intersect(section).intersect(CONTENT);
         targets.add(new Target(hit, row, module, setting, action));
      }
   }

   private void refreshConfigs() { configNames = List.of(ConfigManager.getConfigNames()); }

   private void refreshConfigLayout() {
      String query = search.getValue().strip().toLowerCase(Locale.ROOT);
      List<String> names = configNames.stream().filter(name -> name.toLowerCase(Locale.ROOT).contains(query)).toList();
      maximumScroll = Math.max(0, names.size() * 66f - (CONTENT.bottom() - 161));
      scrollTarget = Math.clamp(scrollTarget, 0, maximumScroll);
      if (scroll > maximumScroll) { scroll = maximumScroll; scrolling.snap(scroll, now()); }
      List<Config> entries = new ArrayList<>();
      Rect clip = new Rect(CONTENT.x(), 156, CONTENT.width(), CONTENT.bottom() - 156);
      for (int i = 0; i < names.size(); i++) {
         String name = names.get(i);
         Rect configBounds = new Rect(CONTENT.x(), 161 + i * 66 - scroll, CONTENT.width(), 54);
         entries.add(new Config(configBounds, name, "Gameplay configuration", name.equals(preset), name.equals(deleteCandidate) && now() < deleteUntil));
         for (int action = 0; action < 3; action++) {
            Rect button = new Rect(configBounds.right() - 202 + action * 66, configBounds.y() + 14, 58, 25);
            targets.add(new Target(button.intersect(clip), button, null, null, Action.values()[Action.LOAD.ordinal() + action], name));
         }
      }
      configs = List.copyOf(entries);
      sections = List.of();
      updatePopup(now());
   }

   private String keyLabel(Feature module) {
      if (module == binding) return "PRESS KEY";
      int key = module.getKey();
      if (key == 0) return "NONE";
      if (key == 344) key = SDLScancode.SDL_SCANCODE_RSHIFT;
      String label = SDLKeyboard.SDL_GetScancodeName(key);
      return label == null || label.isEmpty() ? Integer.toString(key) : label.toUpperCase(Locale.ROOT);
   }

   private static String displayName(String name) { return name.replaceAll("(?<=[a-z0-9])(?=[A-Z])", " "); }

   @Override
   public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      if (minecraft.level == null) {
         super.extractBackground(graphics, mouseX, mouseY, partialTick);
         return;
      }
      // Keep the GUI stratum alive for NanoVG without applying a second world-wide background pass.
      graphics.fill(0, 0, width, height, 0x00000000);
      minecraft.gui.hud.extractDeferredSubtitles();
   }

   @Override
   public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      if (viewport == null) return;
      opacity = Math.clamp(menu.to(closing ? 0 : 1, now()), 0, 1);
      openingScale = .975f + opacity * .025f;
      setMouse(mouseX, mouseY);
      Rect backdrop = viewport.screen(SIDEBAR_BACKDROP, openingScale);
      HudBackdrop.widget(new HudLayouts.Box(backdrop.x(), backdrop.y(), backdrop.width(), backdrop.height()),
         RADIUS * viewport.scale() * openingScale, opacity, HudBackdrop.Blur.MENU);
   }

   @Override
   public void renderNano() {
      if (search == null || viewport == null) return;
      double time = now();
      if (closing && opacity < .01f) { minecraft.gui.setScreen(destination); return; }
      scroll = scrolling.to(scrollTarget, time);
      refresh(time);
      Frame frame = new Frame(viewport, opacity, openingScale, navigation,
         selection.to(navigation(navigation).y(), time), minecraft.getUser().getName(), ClientBranding.DISPLAY_VERSION,
         editor(search), preset.isEmpty() ? "Global" : preset, sections, configs, editor(configName), popupFrame(time),
         scroll, maximumScroll, pageMotion.to(1, time), time < statusUntil ? status : "", statusError, mouseX, mouseY, buttonFeedback(time));
      NeverloseRenderer.paint(NVGRenderer.getContext(), FontRepository.getFont(REGULAR_FONT).getFontId(),
         FontRepository.getFont(MEDIUM_FONT).getFontId(), FontRepository.getFont(BOLD_FONT).getFontId(), frame);
   }

   private static Editor editor(EditBox box) {
      return new Editor(box.getValue(), box.getCursorPosition(), ((EditorBox)box).selection, box.isFocused());
   }

   private Map<String, Float> buttonFeedback(double time) {
      Map<String, Float> feedback = new HashMap<>();
      buttonFeedback(feedback, "save", SAVE, time);
      buttonFeedback(feedback, "create", CREATE, time);
      for (Target target : targets) {
         if (target.action == Action.LOAD || target.action == Action.STORE || target.action == Action.DELETE)
            buttonFeedback(feedback, target.action.name().toLowerCase(Locale.ROOT) + ":" + target.name, target.hit, time);
      }
      return Map.copyOf(feedback);
   }

   private void buttonFeedback(Map<String, Float> result, String key, Rect bounds, double time) {
      float value = hover.computeIfAbsent(key, ignored -> new Motion(0, 32)).to(bounds.contains(mouseX, mouseY) && popup == null ? 1 : 0, time);
      if (pulses.containsKey(key)) value = Math.max(value, (float)Math.exp(-12 * (time - pulses.get(key))));
      result.put(key, value);
   }

   private void setMouse(double x, double y) {
      if (viewport == null) return;
      mouseX = viewport.localX(x, openingScale); mouseY = viewport.localY(y, openingScale);
   }

   private void selectPage(int index) {
      if (index == 6) { destination = new HudEditorScreen(); onClose(); return; }
      if (navigation == index) return;
      navigation = index; binding = null; popup = null;
      search.setFocused(false); configName.setFocused(false); search.setValue("");
      resetScroll(); pageMotion.snap(0, now()); pageMotion.to(1, now());
      refreshConfigs(); refresh(now());
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      if (closing) return true;
      setMouse(event.x(), event.y()); refresh(now());
      int button = mouseButton(event.button());
      if (popup != null) {
         if (popupClick(button)) return true;
         popup.open = false;
         return true;
      }
      if (binding != null) { binding = null; return true; }
      if (button == 0 && SEARCH.contains(mouseX, mouseY)) { focusEditor(search, SEARCH, 30, doubleClick); return true; }
      if (button == 0 && navigation == 7 && CONFIG_INPUT.contains(mouseX, mouseY)) {
         focusEditor(configName, CONFIG_INPUT, 10, doubleClick); return true;
      }
      search.setFocused(false); configName.setFocused(false);
      for (int i = 0; i < NAV_NAMES.length; i++) if (navigation(i).contains(mouseX, mouseY)) {
         if (button == 0) selectPage(i);
         return true;
      }
      if (button == 0 && SAVE.contains(mouseX, mouseY)) { save(preset); return true; }
      if (button == 0 && PRESET.contains(mouseX, mouseY)) { openPopup(null, PRESET); return true; }
      if (button == 0 && navigation == 7 && CREATE.contains(mouseX, mouseY)) { create(); return true; }
      Rect track = scrollTrack();
      if (button == 0 && maximumScroll > 0 && new Rect(743, track.y(), 12, track.height()).contains(mouseX, mouseY)) {
         Rect thumb = thumb(scroll, maximumScroll, track);
         scrollbarGrab = thumb.contains(mouseX, mouseY) ? mouseY - thumb.y() : thumb.height() / 2;
         draggingScrollbar = true; updateScrollbar(); return true;
      }
      for (int i = targets.size() - 1; i >= 0; i--) {
         Target target = targets.get(i);
         if (!target.hit.contains(mouseX, mouseY)) continue;
         Object pulseKey = target.setting == null ? target.module : target.setting;
         if (pulseKey != null) pulses.put(pulseKey, now());
         switch (target.action) {
            case HEADER -> {
               if (button == 1 || button == 0 && mouseX >= target.bounds.right() - 17) {
                  ModuleState state = modules.get(target.module); state.expanded = !state.expanded;
               } else if (target.module != FeatureManager.targets) {
                  if (button == 2) binding = target.module;
                  else if (button == 0) target.module.toggle();
               }
            }
            case BIND -> { if (button == 0 || button == 2) binding = target.module; }
            case ENABLED -> { if (button == 0) target.module.toggle(); }
            case BOOLEAN -> { if (button == 0) { BooleanSetting value = (BooleanSetting)target.setting; value.setValue(!value.getValue()); } }
            case NUMBER -> { if (button == 0 && control(target.bounds).contains(mouseX, mouseY)) { draggingSlider = target; updateSlider(); } }
            case CHOICE -> { if (button == 0) openPopup(target.setting, control(target.bounds)); }
            case LOAD -> { if (button == 0) { pulses.put("load:" + target.name, now()); load(target.name); } }
            case STORE -> { if (button == 0) { pulses.put("store:" + target.name, now()); save(target.name); } }
            case DELETE -> { if (button == 0) { pulses.put("delete:" + target.name, now()); delete(target.name); } }
         }
         refresh(now()); return true;
      }
      if (button == 0 && new Rect(0, 0, WIDTH, HEADER).contains(mouseX, mouseY)) {
         draggingWindow = true; windowGrabX = (float)event.x() - viewport.x(); windowGrabY = (float)event.y() - viewport.y();
         return true;
      }
      return new Rect(0, 0, WIDTH, HEIGHT).contains(mouseX, mouseY) || super.mouseClicked(event, doubleClick);
   }

   private void focusEditor(EditBox box, Rect rect, float padding, boolean selectAll) {
      search.setFocused(box == search); configName.setFocused(box == configName);
      if (selectAll) { box.moveCursorToEnd(false); box.moveCursorToStart(true); }
      else box.moveCursorTo(cursorAt(box, rect, padding), false);
      draggingEditor = box;
   }

   private int cursorAt(EditBox box, Rect rect, float padding) {
      if (!NVGRenderer.isAvailable()) return box.getValue().length();
      long vg = NVGRenderer.getContext();
      int font = FontRepository.getFont(REGULAR_FONT).getFontId();
      float x = mouseX - rect.x() - padding + editorOffset(vg, font, editor(box), rect, padding);
      String value = box.getValue();
      for (int index = 0; index < value.length();) {
         int next = value.offsetByCodePoints(index, 1);
         float left = textWidth(vg, font, value.substring(0, index), 12), right = textWidth(vg, font, value.substring(0, next), 12);
         if (x < (left + right) / 2) return index;
         index = next;
      }
      return value.length();
   }

   private void updateSlider() {
      if (draggingSlider == null || !draggingSlider.setting.isVisible()) { draggingSlider = null; return; }
      NumberSetting setting = (NumberSetting)draggingSlider.setting;
      setting.setValue(setting.getMinimum() + fraction(mouseX, slider(draggingSlider.bounds)) * (setting.getMaximum() - setting.getMinimum()));
   }

   private void updateScrollbar() {
      Rect track = scrollTrack(), thumb = thumb(scroll, maximumScroll, track);
      scroll = scrollTarget = Math.clamp((mouseY - track.y() - scrollbarGrab) / (track.height() - thumb.height()) * maximumScroll, 0, maximumScroll);
      scrolling.snap(scroll, now());
   }

   private Rect scrollTrack() { return navigation == 7 ? new Rect(CONTENT.x(), 156, CONTENT.width(), CONTENT.bottom() - 156) : CONTENT; }

   @Override
   public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
      if (closing) return true;
      setMouse(event.x(), event.y());
      if (mouseButton(event.button()) != 0) return false;
      if (draggingWindow) {
         viewport = viewport(width, height, (float)event.x() - windowGrabX, (float)event.y() - windowGrabY);
         ClickGuiLayouts.put(POSITION, viewport.x(), viewport.y());
         setMouse(event.x(), event.y()); return true;
      }
      if (draggingScrollbar) { updateScrollbar(); refresh(now()); return true; }
      if (draggingEditor != null) {
         draggingEditor.moveCursorTo(cursorAt(draggingEditor, draggingEditor == search ? SEARCH : CONFIG_INPUT, draggingEditor == search ? 30 : 10), true);
         return true;
      }
      if (draggingSlider != null) { refresh(now()); updateSlider(); refresh(now()); return true; }
      return super.mouseDragged(event, deltaX, deltaY);
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent event) {
      if (mouseButton(event.button()) == 0) {
         boolean captured = draggingWindow || draggingScrollbar || draggingSlider != null || draggingEditor != null;
         releaseDrag(); if (captured) return true;
      }
      return super.mouseReleased(event);
   }

   private void releaseDrag() { draggingWindow = draggingScrollbar = false; draggingSlider = null; draggingEditor = null; }

   @Override
   public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
      setMouse(x, y);
      if (closing || draggingWindow || draggingScrollbar || draggingSlider != null) return true;
      refresh(now());
      if (popup != null) {
         if (popup.bounds.contains(mouseX, mouseY)) {
            popup.scroll = Math.clamp(popup.scroll - (float)vertical * OPTION * 2, 0, popup.maximumScroll());
         } else popup.open = false;
         return true;
      }
      if (CONTENT.contains(mouseX, mouseY)) {
         scrollTarget = Math.clamp(scrollTarget - (float)vertical * ROW * 2, 0, maximumScroll);
         return true;
      }
      return super.mouseScrolled(x, y, horizontal, vertical);
   }

   private void openPopup(Setting setting, Rect anchor) {
      popup = new Popup(setting, anchor);
      List<String> labels = popup.labels();
      for (int i = 0; i < labels.size(); i++) {
         if (setting instanceof ChoiceSetting choices ? choices.selected(i)
            : i == 0 ? preset.isEmpty() : labels.get(i).equals(preset)) { popup.highlight = i; break; }
      }
      popup.motion.snap(0, now()); popup.motion.to(1, now());
      binding = null;
      updatePopup(now());
   }

   private void updatePopup(double time) {
      if (popup == null) return;
      if (popup.setting != null && popup.open) {
         Target target = targets.stream().filter(entry -> entry.setting == popup.setting && entry.action == Action.CHOICE).findFirst().orElse(null);
         if (target == null || target.hit.height() < ROW - 1) popup.open = false;
         else popup.anchor = control(target.bounds);
      }
      popup.bounds = dropdown(popup.anchor, popup.labels().size());
      popup.scroll = Math.clamp(popup.scroll, 0, popup.maximumScroll());
      if (popup.motion.to(popup.open ? 1 : 0, time) < .005f && !popup.open) popup = null;
   }

   private Dropdown popupFrame(double time) {
      if (popup == null) return null;
      List<Option> options = new ArrayList<>();
      List<String> labels = popup.labels();
      for (int i = 0; i < labels.size(); i++) {
         boolean selected = popup.setting instanceof ChoiceSetting choices ? choices.selected(i)
            : i == 0 ? preset.isEmpty() : labels.get(i).equals(preset);
         options.add(new Option(labels.get(i), selected, i == popup.highlight));
      }
      return new Dropdown(popup.bounds, popup.motion.to(popup.open ? 1 : 0, time), popup.scroll, List.copyOf(options));
   }

   private boolean popupClick(int button) {
      if (!popup.bounds.contains(mouseX, mouseY)) return false;
      if (button != 0 || !popup.open || popup.motion.value() < .3f) return true;
      Rect clip = new Rect(popup.bounds.x() + 4, popup.bounds.y() + 4, popup.bounds.width() - 8,
         Math.min(popup.bounds.height() - 8, popup.bounds.height() * popup.motion.value() - 4));
      if (clip.contains(mouseX, mouseY)) {
         int index = (int)((mouseY - popup.bounds.y() - 4 + popup.scroll) / OPTION);
         selectOption(index);
      }
      return true;
   }

   private void selectOption(int index) {
      if (popup == null || index < 0 || index >= popup.labels().size()) return;
      if (popup.setting instanceof ChoiceSetting choices) {
         choices.select(index); pulses.put(popup.setting, now());
         if (!choices.multiple()) popup.open = false;
      } else {
         String name = popup.labels().get(index);
         popup.open = false;
         if (index == 0) { preset = ""; show("Local state selected", false); }
         else load(name);
      }
      refresh(now());
   }

   @Override
   public boolean keyPressed(KeyEvent event) {
      if (closing) return true;
      if (binding != null) {
         binding.setKey(event.isEscape() || event.key() == SDLScancode.SDL_SCANCODE_BACKSPACE || event.key() == SDLScancode.SDL_SCANCODE_DELETE ? 0 : event.key());
         binding = null; return true;
      }
      if (event.isEscape()) {
         if (popup != null && popup.open) popup.open = false;
         else if (search.isFocused()) { search.setFocused(false); if (!search.getValue().isEmpty()) search.setValue(""); }
         else if (configName.isFocused()) configName.setFocused(false);
         else onClose();
         return true;
      }
      if (event.hasControlDown() && event.key() == SDLScancode.SDL_SCANCODE_F) {
         popup = null; search.setFocused(true); configName.setFocused(false);
         search.moveCursorToEnd(false); search.moveCursorToStart(true); return true;
      }
      if (popup != null && popup.open) {
         if (event.key() == SDLScancode.SDL_SCANCODE_DOWN || event.key() == SDLScancode.SDL_SCANCODE_UP) {
            int direction = event.key() == SDLScancode.SDL_SCANCODE_DOWN ? 1 : -1;
            popup.highlight = popup.highlight < 0 ? direction > 0 ? 0 : popup.labels().size() - 1
               : Math.floorMod(popup.highlight + direction, popup.labels().size());
            float y = popup.highlight * OPTION;
            popup.scroll = Math.clamp(popup.scroll, Math.max(0, y + OPTION - popup.bounds.height() + 8), Math.min(y, popup.maximumScroll()));
            return true;
         }
         if (event.isConfirmation()) { selectOption(Math.max(0, popup.highlight)); return true; }
         return true;
      }
      if (event.hasControlDown() && event.key() == SDLScancode.SDL_SCANCODE_S) { save(preset); return true; }
      EditBox focused = search.isFocused() ? search : configName.isFocused() ? configName : null;
      if (focused != null) {
         if (event.isConfirmation()) { if (focused == configName) create(); else search.setFocused(false); return true; }
         boolean handled = focused.keyPressed(event); refresh(now()); return handled;
      }
      return super.keyPressed(event);
   }

   @Override
   public boolean charTyped(CharacterEvent event) {
      if (closing || binding != null || popup != null) return true;
      EditBox focused = search.isFocused() ? search : configName.isFocused() ? configName : null;
      if (focused == null) return super.charTyped(event);
      boolean handled = focused.charTyped(event); refresh(now()); return handled;
   }

   @Override
   public boolean preeditUpdated(PreeditEvent event) {
      if (closing || binding != null || popup != null) return true;
      EditBox focused = search.isFocused() ? search : configName.isFocused() ? configName : null;
      return focused != null && focused.preeditUpdated(event);
   }

   private void show(String message, boolean error) { status = message; statusError = error; statusUntil = now() + 5; }

   private void save(String name) {
      pulses.put("save", now());
      try {
         if (name.isEmpty()) ConfigManager.saveState(); else { ConfigManager.saveConfig(name); preset = name; }
         show(name.isEmpty() ? "Local state saved" : "Saved " + name, false); refreshConfigs();
      } catch (RuntimeException error) { show(error.getMessage() == null ? "Could not save configuration" : error.getMessage(), true); }
   }

   private void load(String name) {
      try { ConfigManager.loadConfig(name); preset = name; show("Loaded " + name, false); refresh(now()); }
      catch (RuntimeException error) { show(error.getMessage() == null ? "Could not load configuration" : error.getMessage(), true); }
   }

   private void create() {
      pulses.put("create", now());
      try {
         String name = ConfigManager.validName(configName.getValue());
         if (ConfigManager.configExists(name)) { show("A configuration with that name already exists", true); return; }
         ConfigManager.saveConfig(name); preset = name; configName.setValue(""); configName.setFocused(false);
         show("Created " + name, false); refreshConfigs(); refresh(now());
      } catch (RuntimeException error) { show(error.getMessage() == null ? "Could not create configuration" : error.getMessage(), true); }
   }

   private void delete(String name) {
      if (!name.equals(deleteCandidate) || now() >= deleteUntil) {
         deleteCandidate = name; deleteUntil = now() + 3; show("Click Confirm to delete " + name, false); return;
      }
      try {
         if (!ConfigManager.deleteConfig(name)) { show("Could not delete configuration", true); return; }
         if (preset.equals(name)) preset = "";
         deleteCandidate = ""; show("Deleted " + name, false); refreshConfigs(); refresh(now());
      } catch (RuntimeException error) { show("Could not delete configuration", true); }
   }

   @Override
   public void onClose() {
      if (closing) return;
      releaseDrag(); binding = null; popup = null;
      closing = true; menu.to(0, now());
      if (!NVGRenderer.isAvailable()) minecraft.gui.setScreen(destination);
   }

   @Override public boolean isPauseScreen() { return false; }

   @Override
   public void removed() {
      if (viewport != null) ClickGuiLayouts.put(POSITION, viewport.x(), viewport.y());
      ConfigManager.saveState();
      detached = true; releaseDrag(); popup = null; binding = null;
      super.removed();
   }

   private enum Action { HEADER, BIND, ENABLED, BOOLEAN, NUMBER, CHOICE, LOAD, STORE, DELETE }
   private record Target(Rect hit, Rect bounds, Feature module, Setting setting, Action action, String name) {
      Target(Rect hit, Rect bounds, Feature module, Setting setting, Action action) { this(hit, bounds, module, setting, action, ""); }
   }
   private static final class ModuleState {
      boolean expanded = true;
      final Motion motion = new Motion(1, 30);
   }
   private static final class EditorBox extends EditBox {
      private int selection;
      EditorBox(net.minecraft.client.gui.Font font, String label) {
         super(font, 0, 0, 320, 20, Component.literal(label));
      }
      @Override public void setHighlightPos(int position) {
         super.setHighlightPos(position);
         selection = Math.clamp(position, 0, getValue().length());
      }
   }
   private final class Popup {
      final Setting setting;

      final Motion motion = new Motion(0, 36);

      Rect anchor, bounds;
      boolean open = true;
      float scroll;
      int highlight = -1;
      Popup(Setting setting, Rect anchor) { this.setting = setting; this.anchor = anchor; }
      List<String> labels() {
         if (setting instanceof ChoiceSetting choices) return List.of(choices.options());
         List<String> names = new ArrayList<>(); names.add("Global"); names.addAll(configNames); return names;
      }
      float maximumScroll() { return Math.max(0, labels().size() * OPTION - bounds.height() + 8); }
   }
}
