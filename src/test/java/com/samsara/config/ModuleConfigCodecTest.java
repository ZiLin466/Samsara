package com.samsara.config;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.module.FeatureManager;
import com.samsara.setting.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ModuleConfigCodecTest {
   private static final class TextSetting extends Setting {
      private String value = "default";
      TextSetting(Feature owner) { super("Text", owner); }
      @Override public com.google.gson.JsonElement snapshot(boolean defaults) {
         return new com.google.gson.JsonPrimitive(defaults ? "default" : this.value);
      }
      @Override protected Runnable prepareValue(com.google.gson.JsonElement value) {
         if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Invalid text");
         }
         String desired = value.getAsString();
         return () -> this.value = desired;
      }
   }

   @Test void newSettingTypesPersistWithoutAddingTypeBranchesToTheModuleCodec() {
      var module = new Fixture("Extensible", Category.COMBAT);
      var text = new TextSetting(module); text.value = "custom";
      var saved = ModuleConfigCodec.snapshot(List.of(module), ModuleConfigCodec.Scope.ALL, false);
      var defaults = ModuleConfigCodec.snapshot(List.of(module), ModuleConfigCodec.Scope.ALL, true);
      assertEquals("default", defaults.getAsJsonObject("Extensible").getAsJsonObject("settings").get("Text").getAsString());
      text.value = "changed";
      var restore = ModuleConfigCodec.prepare(List.of(module), saved, ModuleConfigCodec.Scope.ALL);
      assertEquals("changed", text.value); restore.run(); assertEquals("custom", text.value);
      var values = saved.getAsJsonObject("Extensible").getAsJsonObject("settings");
      values.addProperty("Flag", true); values.addProperty("Text", 12);
      assertThrows(IllegalArgumentException.class, () -> ModuleConfigCodec.prepare(List.of(module), saved, ModuleConfigCodec.Scope.ALL));
      assertFalse(module.flag.getValue());
   }

   @Test void registeredSettingsHaveStableOrderAndUniqueKeysWithoutExternalListMutation() {
      var module = new Fixture("Settings", Category.COMBAT);
      assertEquals(List.of("Flag", "Number", "Mode"), module.settings.stream().map(Setting::getName).toList());
      assertThrows(UnsupportedOperationException.class, () -> module.settings.clear());
      assertThrows(IllegalArgumentException.class, () -> new BooleanSetting("Flag", module, true));
      var foreign = new Fixture("Foreign", Category.COMBAT);
      assertThrows(IllegalArgumentException.class, () -> module.registerSetting(foreign.flag));
      assertEquals(3, module.settings.size());
   }
   static final class Fixture extends Feature {
      int enabledCalls;
      @Override public void onEnable() { enabledCalls++; }
      final BooleanSetting flag=new BooleanSetting("Flag",this,false);
      final NumberSetting number=new NumberSetting("Number",this,1,0,10,.5);
      final ModeSetting mode=new ModeSetting("Mode",this,"A",new String[]{"A","B"});
      Fixture(String name,Category category) { this(name,0,category); }
      Fixture(String name,int key,Category category) { super(name,key,category); }
   }
   @Test void gameplayPresetsCarryVisualBindingsButPreserveVisualParametersEvenFromOldFullConfigs() {
      var gameplay=new Fixture("Gameplay",Category.COMBAT);var visual=new Fixture("Visual",Category.VISUAL);
      var modules=List.<Feature>of(gameplay,visual);
      gameplay.flag.setValue(true);visual.flag.setValue(true);visual.setKey(33);
      var full=ModuleConfigCodec.snapshot(modules,ModuleConfigCodec.Scope.ALL,false);
      var visualPreset=ModuleConfigCodec.snapshot(modules,ModuleConfigCodec.Scope.GAMEPLAY,false).getAsJsonObject("Visual");
      assertEquals(java.util.Set.of("key"),visualPreset.keySet());assertEquals(33,visualPreset.get("key").getAsInt());
      full.getAsJsonObject("Gameplay").remove("enabled");full.getAsJsonObject("Visual").remove("enabled");
      gameplay.flag.setValue(false);visual.flag.setValue(false);visual.setKey(44);
      ModuleConfigCodec.prepare(modules,full,ModuleConfigCodec.Scope.GAMEPLAY).run();
      assertTrue(gameplay.flag.getValue());assertFalse(visual.flag.getValue());assertEquals(33,visual.getKey());
      ModuleConfigCodec.prepare(modules,full,ModuleConfigCodec.Scope.ALL).run();
      assertTrue(visual.flag.getValue());assertEquals(33,visual.getKey());
   }
   @Test void defaultAndSavedPresetsRestoreBindingsForEveryCategoryWithoutVisualLifecycleChanges() {
      var modules=java.util.Arrays.stream(Category.values())
         .map(category->new Fixture(category.name(),category.ordinal()+4,category)).toList();
      for (var module:modules) module.setKey(module.getDefaultKey()+20);
      var saved=ModuleConfigCodec.snapshot(List.copyOf(modules),ModuleConfigCodec.Scope.GAMEPLAY,false);
      var defaults=ModuleConfigCodec.snapshot(List.copyOf(modules),ModuleConfigCodec.Scope.GAMEPLAY,true);
      for (var module:modules) module.setKey(0);
      var visual=modules.stream().filter(m->m.getCategory()==Category.VISUAL).findFirst().orElseThrow();
      visual.setHidden(true);visual.flag.setValue(true);
      ModuleConfigCodec.prepare(List.copyOf(modules),saved,ModuleConfigCodec.Scope.GAMEPLAY).run();
      for (var module:modules) assertEquals(module.getDefaultKey()+20,module.getKey());
      ModuleConfigCodec.prepare(List.copyOf(modules),defaults,ModuleConfigCodec.Scope.GAMEPLAY).run();
      for (var module:modules) assertEquals(module.getDefaultKey(),module.getKey());
      assertTrue(visual.flag.getValue());assertTrue(visual.isHidden());assertFalse(visual.isEnabled());assertEquals(0,visual.enabledCalls);
   }
   @Test void invalidLaterSettingCannotPartiallyApplyEarlierSettings() {
      var module=new Fixture("Gameplay",Category.COMBAT);
      var config=ModuleConfigCodec.snapshot(List.of(module),ModuleConfigCodec.Scope.ALL,false);
      var values=config.getAsJsonObject("Gameplay").getAsJsonObject("settings");
      values.addProperty("Flag",true);values.addProperty("Mode","missing");
      assertThrows(IllegalArgumentException.class,()->ModuleConfigCodec.prepare(List.of(module),config,ModuleConfigCodec.Scope.ALL));
      assertFalse(module.flag.getValue());
   }
   @Test void startupRestoresGameplayFlagsWithoutCallingWorldDependentHooks() {
      var module=new Fixture("WorldModule",Category.PLAYER);
      module.restoreEnabled(true); assertTrue(module.isEnabled()); assertEquals(0,module.enabledCalls);
      module.initializeWorldState(); assertEquals(0,module.enabledCalls);
   }
   @Test void everyRegisteredModuleCanReloadItsOwnSavedDefaultModes() {
      FeatureManager.registerModules();
      var modules=FeatureManager.getModules();
      var saved=ModuleConfigCodec.snapshot(modules,ModuleConfigCodec.Scope.ALL,false);
      assertDoesNotThrow(()->ModuleConfigCodec.prepare(modules,saved,ModuleConfigCodec.Scope.ALL));
   }
   @Test void oldSprintResetCapitalizationLoadsAndCannotCreateAnInvalidFutureSnapshot() {
      var module=new com.samsara.module.combat.SprintReset();
      var config=ModuleConfigCodec.snapshot(List.of(module),ModuleConfigCodec.Scope.ALL,false);
      var values=config.getAsJsonObject("SprintReset").getAsJsonObject("settings");
      for (String old:List.of("WTap","Wtap","wtap")) {
         values.addProperty("Mode",old);
         ModuleConfigCodec.prepare(List.of(module),config,ModuleConfigCodec.Scope.GAMEPLAY,true).run();
         var saved=ModuleConfigCodec.snapshot(List.of(module),ModuleConfigCodec.Scope.ALL,false);
         assertEquals("WTap",saved.getAsJsonObject("SprintReset").getAsJsonObject("settings").get("Mode").getAsString());
      }
      var mode=(ModeSetting)module.settings.stream().filter(s->s instanceof ModeSetting).findFirst().orElseThrow();
      assertThrows(IllegalArgumentException.class,()->mode.setValue("invalid"));assertEquals("WTap",mode.getValue());
   }
}
