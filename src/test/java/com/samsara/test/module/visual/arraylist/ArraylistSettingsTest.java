package com.samsara.test.module.visual.arraylist;

import com.google.gson.JsonArray;
import com.samsara.config.ModuleConfigCodec;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.module.visual.Hud.ArraylistSettings;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class ArraylistSettingsTest {
   static final class Hud extends Feature {
      final ArraylistSettings normal = new ArraylistSettings(this, false, () -> true);
      final ArraylistSettings opai = new ArraylistSettings(this, true, () -> true);
      Hud() { super("ArrayList", Category.VISUAL); }
   }

   @Test void multipleCategoriesToggleIndependentlyAndCanAllBeDisabled() {
      var hud = new Hud();
      hud.opai.categories.select(3);
      hud.opai.categories.select(0);
      assertFalse(hud.opai.shows(Category.VISUAL));
      assertFalse(hud.opai.shows(Category.COMBAT));
      assertTrue(hud.opai.shows(Category.PLAYER));
      assertTrue(hud.normal.shows(Category.VISUAL));
      assertEquals("3 Selected", hud.opai.categories.selectionLabel());
      hud.opai.categories.setSelected(List.of());
      for (Category category : Category.values()) assertFalse(hud.opai.shows(category));
      hud.opai.categories.select(0);
      assertTrue(hud.opai.shows(Category.COMBAT));
   }

   @Test void oldBooleanCategoriesMigrateIndependentlyForEachStyleAndSurviveAnotherSave() {
      var hud = new Hud();
      var saved = ModuleConfigCodec.snapshot(List.of(hud), ModuleConfigCodec.Scope.ALL, false);
      var settings = saved.getAsJsonObject("ArrayList").getAsJsonObject("settings");
      settings.remove("Categories");settings.remove("Opai Categories");
      settings.addProperty("Visual", false);settings.addProperty("Opai Combat", false);
      settings.addProperty("Opai Misc", false);settings.addProperty("Opai Lowercase", true);
      ModuleConfigCodec.prepare(List.of(hud), saved, ModuleConfigCodec.Scope.ALL).run();
      assertFalse(hud.normal.shows(Category.VISUAL));assertTrue(hud.opai.shows(Category.VISUAL));
      assertFalse(hud.opai.shows(Category.COMBAT));assertFalse(hud.opai.shows(Category.MISC));
      assertTrue(hud.opai.lowercase.getValue());
      var next = ModuleConfigCodec.snapshot(List.of(hud), ModuleConfigCodec.Scope.ALL, false);
      var restarted = new Hud();
      ModuleConfigCodec.prepare(List.of(restarted), next, ModuleConfigCodec.Scope.ALL).run();
      assertEquals(hud.opai.categories.selectedValues(), restarted.opai.categories.selectedValues());
      assertEquals(hud.normal.categories.selectedValues(), restarted.normal.categories.selectedValues());
      assertFalse(next.getAsJsonObject("ArrayList").getAsJsonObject("settings").has("Opai Combat"));
      assertTrue(hud.settings.stream().allMatch(s -> !s.getDisplayName().startsWith("Opai ")));
   }

   @Test void defaultsResetBothSelectionsWhileMalformedPresetCannotPartiallyApply() {
      var hud = new Hud();
      hud.opai.categories.setSelected(List.of("player"));hud.normal.categories.setSelected(List.of());
      var defaults = ModuleConfigCodec.snapshot(List.of(hud), ModuleConfigCodec.Scope.ALL, true);
      ModuleConfigCodec.prepare(List.of(hud), defaults, ModuleConfigCodec.Scope.ALL).run();
      assertEquals(5, hud.opai.categories.selectedValues().size());
      assertEquals(5, hud.normal.categories.selectedValues().size());
      var settings = defaults.getAsJsonObject("ArrayList").getAsJsonObject("settings");
      settings.addProperty("Opai Lowercase", true);
      var invalid = new JsonArray();invalid.add("Invented");settings.add("Opai Categories", invalid);
      assertThrows(IllegalArgumentException.class,
         () -> ModuleConfigCodec.prepare(List.of(hud), defaults, ModuleConfigCodec.Scope.ALL));
      assertFalse(hud.opai.lowercase.getValue());
      List<String> warnings = new java.util.ArrayList<>();
      ModuleConfigCodec.prepare(List.of(hud), defaults, ModuleConfigCodec.Scope.ALL, true, warnings::add).run();
      assertEquals(1, warnings.size());assertTrue(hud.opai.lowercase.getValue());
      assertEquals(5, hud.opai.categories.selectedValues().size());
   }
}
