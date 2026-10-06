package com.samsara.test.module.visual;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.samsara.config.ModuleConfigCodec;
import com.samsara.module.FeatureManager;
import com.samsara.module.visual.Hud;
import com.samsara.module.visual.Hud.Widget;
import com.samsara.setting.ModeSetting;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class HudWidgetsTest {
   private static final class Fixture extends Hud {
      private boolean enabled;
      @Override public void setEnabled(boolean enabled) { this.enabled = enabled; }
      @Override public boolean isEnabled() { return this.enabled; }
   }

   private static JsonObject legacy(boolean array, boolean target, boolean inventory, boolean potion, boolean session) {
      var root = new JsonObject();
      String[] names = {"HUD", "TargetHUD", "InventoryHUD", "Potion Status", "SessionHUD"};
      boolean[] enabled = {array, target, inventory, potion, session};
      for (int i = 0; i < names.length; i++) {
         var data = new JsonObject(); data.addProperty("enabled", enabled[i]);
         data.add("settings", new JsonObject()); root.add(names[i], data);
      }
      return root;
   }

   @Test void sixChoicesControlActualHudGatesAndAllowEverythingToBeOff() {
      var hud = new Fixture();
      assertEquals("6 Selected", hud.widgets.selectionLabel());
      assertFalse(hud.widgetEnabled(Widget.STATUS_BAR));
      hud.setEnabled(true);
      assertTrue(hud.widgetEnabled(Widget.STATUS_BAR));
      hud.widgets.select(0);
      assertFalse(hud.widgetEnabled(Widget.STATUS_BAR));
      assertTrue(hud.widgetEnabled(Widget.ARRAY_LIST));
      hud.widgets.setSelected(List.of());
      assertEquals("0 Selected", hud.widgets.selectionLabel());
      for (var widget : Widget.values()) assertFalse(hud.widgetEnabled(widget));
      assertFalse(hud.settings.stream().filter(s -> !s.getName().equals("Widgets")).anyMatch(s -> s.isVisible()));
   }

   @Test void migrationPreservesFormerlyIndependentVisibilityAndTargetStyle() {
      var old = legacy(false, true, false, true, true);
      old.getAsJsonObject("TargetHUD").getAsJsonObject("settings").addProperty("Mode", "Opai");
      old.getAsJsonObject("TargetHUD").getAsJsonObject("settings").addProperty("Show Armor", false);
      var original = old.deepCopy();
      var hud = new Fixture();
      ModuleConfigCodec.prepare(List.of(hud), old, ModuleConfigCodec.Scope.ALL).run();
      assertEquals(original, old);
      assertTrue(hud.isEnabled());
      assertEquals(List.of("Dynamic Island", "Potion Status", "TargetHUD", "SessionHUD"), hud.widgets.selectedValues());
      assertEquals("Opai", ((ModeSetting)hud.settings.stream().filter(s -> s.getName().equals("Target Mode")).findFirst().orElseThrow()).m224());
      var saved = ModuleConfigCodec.snapshot(List.of(hud), ModuleConfigCodec.Scope.ALL, false);
      assertEquals(java.util.Set.of("HUD"), saved.keySet());
      assertFalse(saved.getAsJsonObject("HUD").getAsJsonObject("settings").get("Target Show Armor").getAsBoolean());
   }

   @Test void newSelectionsAndMasterSwitchTakePrecedenceOverOldModuleFlags() {
      var old = legacy(true, true, true, true, true);
      var values = old.getAsJsonObject("HUD").getAsJsonObject("settings");
      var widgets = new JsonArray(); widgets.add("InventoryHUD"); values.add("Widgets", widgets);
      values.addProperty("Target Mode", "Classic");
      old.getAsJsonObject("HUD").addProperty("enabled", false);
      old.getAsJsonObject("TargetHUD").getAsJsonObject("settings").addProperty("Mode", "Opai");
      var hud = new Fixture();
      ModuleConfigCodec.prepare(List.of(hud), old, ModuleConfigCodec.Scope.ALL).run();
      assertFalse(hud.isEnabled());
      assertEquals(List.of("InventoryHUD"), hud.widgets.selectedValues());
      assertEquals("Classic", ((ModeSetting)hud.settings.stream().filter(s -> s.getName().equals("Target Mode")).findFirst().orElseThrow()).m224());
   }

   @Test void newWidgetSelectionsRoundTripAndGameplayPresetsPreserveThem() {
      var first = new Fixture(); first.setEnabled(true);
      first.widgets.setSelected(List.of("ArrayList", "InventoryHUD"));
      var saved = ModuleConfigCodec.snapshot(List.of(first), ModuleConfigCodec.Scope.ALL, false);
      var restored = new Fixture();
      ModuleConfigCodec.prepare(List.of(restored), saved, ModuleConfigCodec.Scope.ALL).run();
      assertTrue(restored.isEnabled()); assertEquals(first.widgets.selectedValues(), restored.widgets.selectedValues());
      restored.widgets.setSelected(List.of("SessionHUD")); restored.setEnabled(false);
      ModuleConfigCodec.prepare(List.of(restored), saved, ModuleConfigCodec.Scope.GAMEPLAY).run();
      assertFalse(restored.isEnabled()); assertEquals(List.of("SessionHUD"), restored.widgets.selectedValues());
   }

   @Test void retiredModulesAreAbsentFromEveryClickGuiModuleSource() {
      FeatureManager.registerModules();
      var names = FeatureManager.getModules().stream().map(m -> m.getName()).toList();
      assertTrue(names.contains("HUD"));
      for (String retired : List.of("InventoryHUD", "TargetHUD", "SessionHUD", "Potion Status")) assertFalse(names.contains(retired));
   }

   @Test void corruptWidgetChoiceDoesNotLoseOtherValidStartupSettings() {
      var hud = new Fixture(); var root = legacy(false, false, false, false, false);
      var settings = root.getAsJsonObject("HUD").getAsJsonObject("settings");
      var selected = new JsonArray(); selected.add("unknown"); settings.add("Widgets", selected);
      settings.addProperty("ArrayList mode", "Opai");
      var warnings = new java.util.ArrayList<String>();
      ModuleConfigCodec.prepare(List.of(hud), root, ModuleConfigCodec.Scope.ALL, true, warnings::add).run();
      assertEquals(1, warnings.size()); assertEquals(6, hud.widgets.selectedValues().size());
      assertEquals("Opai", ((ModeSetting)hud.settings.stream().filter(s -> s.getName().equals("ArrayList mode")).findFirst().orElseThrow()).m224());
   }
}
