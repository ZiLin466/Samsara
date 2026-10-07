package com.samsara.config;

import com.google.gson.JsonObject;
import com.samsara.module.Feature;
import com.samsara.module.combat.KillAura;
import com.samsara.module.combat.TargetSettings;
import com.samsara.module.combat.Velocity;
import com.samsara.module.visual.Animations;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class CombatSettingsTest {
   @Test void eagleRangesAndConditionsRoundTripWithTheReferenceDefaults() {
      var eagle = new com.samsara.module.player.Eagle();
      var conditions = (com.samsara.setting.MultiSelectSetting)eagle.settings.stream()
         .filter(s -> s.getName().equals("Conditions")).findFirst().orElseThrow();
      assertEquals(List.of("OnGround"), conditions.selectedValues());
      conditions.setSelected(List.of("Backwards", "HoldingBlocks", "Sneak"));
      for (var setting : eagle.settings) {
         if (setting.getName().equals("Edge Distance Min")) ((com.samsara.setting.NumberSetting)setting).setValue(0.35);
         if (setting.getName().equals("Edge Distance Max")) ((com.samsara.setting.NumberSetting)setting).setValue(0.55);
      }
      var saved = ModuleConfigCodec.snapshot(List.of(eagle), ModuleConfigCodec.Scope.ALL, false);
      var restored = new com.samsara.module.player.Eagle();
      ModuleConfigCodec.prepare(List.of(restored), saved, ModuleConfigCodec.Scope.ALL).run();
      assertEquals(saved, ModuleConfigCodec.snapshot(List.of(restored), ModuleConfigCodec.Scope.ALL, false));
   }
   @Test void eachVelocityModeExposesOnlyItsOwnSettings() {
      var velocity = new Velocity();
      velocity.mode.setValue("Reduce");
      assertEquals(Set.of("Mode", "Attack Counts", "Sprint Ticks", "Max Delay", "Swing Hand"), visible(velocity));
      velocity.mode.setValue("Delay");
      assertEquals(Set.of("Mode", "Delay Mode Ticks", "Delay Mode Jump Reset"), visible(velocity));
      assertEquals(List.of("Mode", "Delay Ticks", "Jump Reset"), velocity.settings.stream()
         .filter(setting -> setting.isVisible()).map(setting -> setting.getDisplayName()).toList());
      velocity.mode.setValue("JumpReset");
      assertEquals(Set.of("Mode", "Chance", "Jump By Received Hits", "Jump By Delay",
         "Ticks Until Jump Min", "Ticks Until Jump Max"), visible(velocity));
      ((com.samsara.setting.BooleanSetting)velocity.settings.stream()
         .filter(s -> s.getName().equals("Jump By Received Hits")).findFirst().orElseThrow()).setValue(true);
      assertTrue(visible(velocity).containsAll(Set.of("Hits Until Jump Min", "Hits Until Jump Max")));
      velocity.mode.setValue("Original");
      assertTrue(visible(velocity).containsAll(Set.of("Horizontal", "Vertical", "Reduce", "Delay", "Reverse")));
      assertFalse(visible(velocity).contains("Max Delay")); assertFalse(visible(velocity).contains("Delay Mode Ticks"));
      assertFalse(visible(velocity).contains("Delay Range"));
   }
   private Set<String> visible(Feature module) {
      return module.settings.stream().filter(s -> s.isVisible()).map(s -> s.getName()).collect(java.util.stream.Collectors.toSet());
   }
   @Test void jumpResetSettingsRoundTripWithoutOverwritingTheOtherVelocityModes() {
      var velocity = new Velocity(); velocity.mode.setValue("JumpReset");
      for (var setting : velocity.settings) {
         if (setting.getName().equals("Chance")) ((com.samsara.setting.NumberSetting)setting).setValue(60);
         if (setting.getName().equals("Jump By Received Hits")) ((com.samsara.setting.BooleanSetting)setting).setValue(true);
         if (setting.getName().equals("Hits Until Jump Max")) ((com.samsara.setting.NumberSetting)setting).setValue(5);
      }
      var saved = ModuleConfigCodec.snapshot(List.of(velocity), ModuleConfigCodec.Scope.ALL, false);
      var restored = new Velocity();
      ModuleConfigCodec.prepare(List.of(restored), saved, ModuleConfigCodec.Scope.ALL).run();
      assertEquals(saved, ModuleConfigCodec.snapshot(List.of(restored), ModuleConfigCodec.Scope.ALL, false));
      assertEquals("JumpReset", restored.mode.getValue());
      restored.mode.setValue("Delay");
      assertEquals(Set.of("Mode", "Delay Mode Ticks", "Delay Mode Jump Reset"), visible(restored));
   }
   @Test void legacyConfigurationsMigrateWithoutMutatingInput() {
      var velocity = new Velocity(); var aura = new KillAura(); var animations = new Animations();
      var modules = List.<Feature>of(velocity, aura, animations);
      var config = ModuleConfigCodec.snapshot(modules, ModuleConfigCodec.Scope.ALL, false);
      modules.forEach(m -> config.getAsJsonObject(m.getName()).remove("enabled"));
      var values = config.getAsJsonObject("Velocity").getAsJsonObject("settings");
      values.remove("Mode"); values.addProperty("Reduce", "Delay");
      values.remove("Delay Mode Ticks"); values.remove("Delay Mode Jump Reset");
      values.addProperty("Delay Ticks", 4); values.addProperty("Jump Reset", true);
      config.getAsJsonObject("KillAura").getAsJsonObject("settings").addProperty("AutoBlock", "Fake");
      config.getAsJsonObject("Animations").getAsJsonObject("settings").remove("Fake Block");
      var unchanged = config.deepCopy();
      ModuleConfigCodec.prepare(modules, config, ModuleConfigCodec.Scope.ALL).run();
      assertEquals("Delay", velocity.mode.getValue()); assertTrue(animations.fakeBlock.getValue());
      var saved = ModuleConfigCodec.snapshot(modules, ModuleConfigCodec.Scope.ALL, false);
      assertEquals("None", saved.getAsJsonObject("KillAura").getAsJsonObject("settings").get("AutoBlock").getAsString());
      assertEquals(4, saved.getAsJsonObject("Velocity").getAsJsonObject("settings").get("Delay Mode Ticks").getAsInt());
      assertEquals(unchanged, config);
   }

   @Test void prefixedDelaySettingsMigrateInPresetsAndStartupWithoutOverwritingOriginalSettings() {
      for (var scope : ModuleConfigCodec.Scope.values()) {
         for (boolean startup : List.of(false, true)) {
            var velocity = new Velocity();
            var config = ModuleConfigCodec.snapshot(List.of(velocity), scope, false);
            var settings = config.getAsJsonObject("Velocity").getAsJsonObject("settings");
            settings.addProperty("Mode", "Delay");
            settings.addProperty("Delay Ticks", 12);
            settings.addProperty("Jump Reset", false);
            settings.remove("Delay Mode Ticks"); settings.remove("Delay Mode Jump Reset");
            settings.addProperty("Epsilon Delay Ticks", 5);
            settings.addProperty("Epsilon Jump Reset", true);
            var unchanged = config.deepCopy();

            ModuleConfigCodec.prepare(List.of(velocity), config, scope, startup).run();
            var saved = ModuleConfigCodec.snapshot(List.of(velocity), scope, false);
            var restored = saved.getAsJsonObject("Velocity").getAsJsonObject("settings");
            assertEquals("Delay", velocity.mode.getValue());
            assertEquals(12, restored.get("Delay Ticks").getAsInt());
            assertFalse(restored.get("Jump Reset").getAsBoolean());
            assertEquals(5, restored.get("Delay Mode Ticks").getAsInt());
            assertTrue(restored.get("Delay Mode Jump Reset").getAsBoolean());
            assertFalse(restored.has("Epsilon Delay Ticks"));
            assertFalse(restored.has("Epsilon Jump Reset"));
            assertEquals(unchanged, config);

            var reloaded = new Velocity();
            ModuleConfigCodec.prepare(List.of(reloaded), saved, scope, startup).run();
            assertEquals(saved, ModuleConfigCodec.snapshot(List.of(reloaded), scope, false));
         }
      }
   }

   @Test void explicitDelayModeSettingsTakePrecedenceOverBothLegacyFormats() {
      var velocity = new Velocity();
      var config = ModuleConfigCodec.snapshot(List.of(velocity), ModuleConfigCodec.Scope.ALL, false);
      var settings = config.getAsJsonObject("Velocity").getAsJsonObject("settings");
      settings.remove("Mode"); settings.addProperty("Reduce", "Delay");
      settings.addProperty("Delay Ticks", 7); settings.addProperty("Jump Reset", true);
      settings.addProperty("Epsilon Delay Ticks", 5); settings.addProperty("Epsilon Jump Reset", true);
      settings.addProperty("Delay Mode Ticks", 2); settings.addProperty("Delay Mode Jump Reset", false);
      var unchanged = config.deepCopy();

      ModuleConfigCodec.prepare(List.of(velocity), config, ModuleConfigCodec.Scope.ALL).run();
      var saved = ModuleConfigCodec.snapshot(List.of(velocity), ModuleConfigCodec.Scope.ALL, false)
         .getAsJsonObject("Velocity").getAsJsonObject("settings");
      assertEquals("Delay", velocity.mode.getValue());
      assertEquals("Disabled", saved.get("Reduce").getAsString());
      assertEquals(2, saved.get("Delay Mode Ticks").getAsInt());
      assertFalse(saved.get("Delay Mode Jump Reset").getAsBoolean());
      assertEquals(7, saved.get("Delay Ticks").getAsInt());
      assertTrue(saved.get("Jump Reset").getAsBoolean());
      assertEquals(unchanged, config);
   }

   @Test void originalDelaySettingsDoNotPopulateTheSeparateDelayMode() {
      var velocity = new Velocity();
      var config = ModuleConfigCodec.snapshot(List.of(velocity), ModuleConfigCodec.Scope.ALL, false);
      var settings = config.getAsJsonObject("Velocity").getAsJsonObject("settings");
      settings.addProperty("Delay Ticks", 12); settings.addProperty("Jump Reset", true);
      settings.remove("Delay Mode Ticks"); settings.remove("Delay Mode Jump Reset");

      ModuleConfigCodec.prepare(List.of(velocity), config, ModuleConfigCodec.Scope.ALL).run();
      var saved = ModuleConfigCodec.snapshot(List.of(velocity), ModuleConfigCodec.Scope.ALL, false)
         .getAsJsonObject("Velocity").getAsJsonObject("settings");
      assertEquals("Original", velocity.mode.getValue());
      assertEquals(12, saved.get("Delay Ticks").getAsInt());
      assertTrue(saved.get("Jump Reset").getAsBoolean());
      assertEquals(3, saved.get("Delay Mode Ticks").getAsInt());
      assertFalse(saved.get("Delay Mode Jump Reset").getAsBoolean());
   }
   @Test void targetsPersistSeparatelyAndGameplayPresetsPreserveVisualSelection() {
      var targets = new TargetSettings();
      targets.combat.setSelected(List.of("Passive")); targets.visual.setSelected(List.of("Self", "Players"));
      var config = ModuleConfigCodec.snapshot(List.of(targets), ModuleConfigCodec.Scope.ALL, false);
      assertFalse(ModuleConfigCodec.snapshot(List.of(targets), ModuleConfigCodec.Scope.GAMEPLAY, false)
         .getAsJsonObject("Targets").getAsJsonObject("settings").has("Visual"));
      targets.combat.setSelected(List.of()); targets.visual.setSelected(List.of("Hostile"));
      ModuleConfigCodec.prepare(List.of(targets), config, ModuleConfigCodec.Scope.GAMEPLAY).run();
      assertEquals(List.of("Passive"), targets.combat.selectedValues()); assertEquals(List.of("Hostile"), targets.visual.selectedValues());
      ModuleConfigCodec.prepare(List.of(targets), config, ModuleConfigCodec.Scope.ALL).run();
      assertEquals(List.of("Self", "Players"), targets.visual.selectedValues());
   }
   @Test void damagedCombatSectionCannotPreventOtherStartupSettingsFromRestoring() {
      var velocity = new Velocity(); var animations = new Animations();
      var config = ModuleConfigCodec.snapshot(List.of(velocity, animations), ModuleConfigCodec.Scope.ALL, false);
      config.addProperty("Velocity", "broken");
      config.getAsJsonObject("Animations").getAsJsonObject("settings").addProperty("Fake Block", true);
      ModuleConfigCodec.prepare(List.of(velocity, animations), config, ModuleConfigCodec.Scope.ALL, true).run();
      assertTrue(animations.fakeBlock.getValue());
   }
}
