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
      assertEquals(Set.of("Mode", "Epsilon Delay Ticks", "Epsilon Jump Reset"), visible(velocity));
      velocity.mode.setValue("JumpReset");
      assertEquals(Set.of("Mode", "Chance", "Jump By Received Hits", "Jump By Delay",
         "Ticks Until Jump Min", "Ticks Until Jump Max"), visible(velocity));
      ((com.samsara.setting.BooleanSetting)velocity.settings.stream()
         .filter(s -> s.getName().equals("Jump By Received Hits")).findFirst().orElseThrow()).setValue(true);
      assertTrue(visible(velocity).containsAll(Set.of("Hits Until Jump Min", "Hits Until Jump Max")));
      velocity.mode.setValue("Original");
      assertTrue(visible(velocity).containsAll(Set.of("Horizontal", "Vertical", "Reduce", "Delay", "Reverse")));
      assertFalse(visible(velocity).contains("Max Delay")); assertFalse(visible(velocity).contains("Epsilon Delay Ticks"));
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
      assertEquals(Set.of("Mode", "Epsilon Delay Ticks", "Epsilon Jump Reset"), visible(restored));
   }
   @Test void legacyConfigurationsMigrateWithoutMutatingInput() {
      var velocity = new Velocity(); var aura = new KillAura(); var animations = new Animations();
      var modules = List.<Feature>of(velocity, aura, animations);
      var config = ModuleConfigCodec.snapshot(modules, ModuleConfigCodec.Scope.ALL, false);
      modules.forEach(m -> config.getAsJsonObject(m.getName()).remove("enabled"));
      var values = config.getAsJsonObject("Velocity").getAsJsonObject("settings");
      values.remove("Mode"); values.addProperty("Reduce", "Delay");
      values.addProperty("Delay Ticks", 4); values.addProperty("Jump Reset", true);
      config.getAsJsonObject("KillAura").getAsJsonObject("settings").addProperty("AutoBlock", "Fake");
      config.getAsJsonObject("Animations").getAsJsonObject("settings").remove("Fake Block");
      var unchanged = config.deepCopy();
      ModuleConfigCodec.prepare(modules, config, ModuleConfigCodec.Scope.ALL).run();
      assertEquals("Delay", velocity.mode.getValue()); assertTrue(animations.fakeBlock.getValue());
      var saved = ModuleConfigCodec.snapshot(modules, ModuleConfigCodec.Scope.ALL, false);
      assertEquals("None", saved.getAsJsonObject("KillAura").getAsJsonObject("settings").get("AutoBlock").getAsString());
      assertEquals(4, saved.getAsJsonObject("Velocity").getAsJsonObject("settings").get("Epsilon Delay Ticks").getAsInt());
      assertEquals(unchanged, config);
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
