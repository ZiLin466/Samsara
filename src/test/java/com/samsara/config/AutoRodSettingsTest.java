package com.samsara.config;

import com.google.gson.JsonObject;
import com.samsara.module.combat.AutoRod;
import com.samsara.setting.ModeSetting;
import com.samsara.setting.MultiSelectSetting;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class AutoRodSettingsTest {
   @Test void oldFixedDelaysAndIgnoresMigrateWithoutChangingOtherModules() {
      var rod = new AutoRod();
      var config = new JsonObject(); var module = new JsonObject(); var values = new JsonObject();
      values.addProperty("Cooldown", 6); values.addProperty("Slot Reset Delay", 3); values.addProperty("Scan Extra Range", 1.5);
      values.addProperty("Ignore Open Inventory", true); values.addProperty("Ignore Using Item", false);
      values.addProperty("Ignore Holding Consumable", true);
      module.add("settings", values); config.add("AutoRod", module);
      var untouched = config.deepCopy();
      ModuleConfigCodec.prepare(List.of(rod), config, ModuleConfigCodec.Scope.GAMEPLAY).run();
      var saved = ModuleConfigCodec.snapshot(List.of(rod), ModuleConfigCodec.Scope.ALL, false).getAsJsonObject("AutoRod").getAsJsonObject("settings");
      assertEquals(6, saved.get("Cooldown Max").getAsInt()); assertEquals(3, saved.get("Slot Reset Delay Max").getAsInt());
      assertEquals(1.5, saved.get("Scan Extra Range Min").getAsDouble());
      assertEquals(List.of("Open Inventory", "Holding Consumable"), selection(rod, "Ignores").selectedValues());
      assertEquals(untouched, config);
   }
   @Test void randomIntervalsAndItemRequirementSelectionsRoundTrip() {
      var rod = new AutoRod();
      selection(rod, "Requires").setSelected(List.of("Click", "Not Breaking"));
      selection(rod, "Holding Items For Ignore").setSelected(List.of("minecraft:bow", "minecraft:trident"));
      selection(rod, "Target Priority").setSelected(List.of("Health", "Type"));
      var snapshot = ModuleConfigCodec.snapshot(List.of(rod), ModuleConfigCodec.Scope.ALL, false);
      var loaded = new AutoRod(); ModuleConfigCodec.prepare(List.of(loaded), snapshot, ModuleConfigCodec.Scope.ALL).run();
      assertEquals(snapshot, ModuleConfigCodec.snapshot(List.of(loaded), ModuleConfigCodec.Scope.ALL, false));
      assertEquals(List.of("Health", "Type"), selection(loaded, "Target Priority").selectedValues());
      assertTrue(selection(new AutoRod(), "Requires").selectedValues().isEmpty());
      var priority = selection(loaded, "Target Priority");
      priority.setSelected(List.of("Type")); priority.select(0);
      assertEquals(List.of("Type"), priority.selectedValues());
      assertThrows(IllegalArgumentException.class, () -> priority.setSelected(List.of()));
   }
   @Test void explicitNewRangeEndpointsWinOverLegacyMigration() {
      var module = new JsonObject(); var settings = new JsonObject(); var config = new JsonObject();
      settings.addProperty("Cooldown", 4); settings.addProperty("Cooldown Max", 8);
      settings.addProperty("Scan Extra Range", 2); settings.addProperty("Scan Extra Range Min", .5);
      module.add("settings", settings); config.add("AutoRod", module);
      var migrated = CombatConfigMigration.migrate(config).getAsJsonObject("AutoRod").getAsJsonObject("settings");
      assertEquals(8, migrated.get("Cooldown Max").getAsInt()); assertEquals(.5, migrated.get("Scan Extra Range Min").getAsDouble());
   }
   @Test void eachRotationModeHidesSettingsFromOtherModes() {
      var rod = new AutoRod();
      var smoothing = (ModeSetting)rod.settings.stream().filter(s -> s.getName().equals("Rotation Smoothing")).findFirst().orElseThrow();
      smoothing.setValue("Acceleration");
      assertFalse(rod.settings.stream().filter(s -> s.getName().equals("Rotation Yaw Speed Min")).findFirst().orElseThrow().isVisible());
      assertTrue(rod.settings.stream().filter(s -> s.getName().equals("Rotation Yaw Acceleration Min")).findFirst().orElseThrow().isVisible());
      smoothing.setValue("Linear");
      assertFalse(rod.settings.stream().filter(s -> s.getName().equals("Rotation Yaw Acceleration Min")).findFirst().orElseThrow().isVisible());
   }
   private static MultiSelectSetting selection(AutoRod rod, String name) {
      return (MultiSelectSetting)rod.settings.stream().filter(setting -> setting.getName().equals(name)).findFirst().orElseThrow();
   }
}
