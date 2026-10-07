package com.samsara.config;

import com.google.gson.JsonObject;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.Setting;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Strict portable presets and recoverable startup state share the same module schema. */
public final class ModuleConfigCodec {
   public enum Scope { ALL, GAMEPLAY }
   private ModuleConfigCodec() { }
   private static boolean included(Feature module, Scope scope) {
      return scope == Scope.ALL || module.getCategory() != Category.VISUAL;
   }
   public static JsonObject snapshot(List<Feature> modules, Scope scope, boolean defaults) {
      var root = new JsonObject();
      for (Feature module : modules) {
         var moduleState = new JsonObject(); var settings = new JsonObject();
         moduleState.addProperty("key", defaults ? module.getDefaultKey() : module.getKey());
         if (!included(module, scope)) {
            root.add(module.getName(), moduleState);
            continue;
         }
         moduleState.addProperty("enabled", !defaults && !module.getName().equals("ClickGUI") && module.isEnabled());
         moduleState.addProperty("hidden", !defaults && module.isHidden());
         for (Setting setting : module.settings) {
            if (scope == Scope.GAMEPLAY && module instanceof com.samsara.module.combat.TargetSettings && setting.getName().equals("Visual")) continue;
            settings.add(setting.getName(), setting.snapshot(defaults));
         }
         moduleState.add("settings", settings); root.add(module.getName(), moduleState);
      }
      return root;
   }
   public static Runnable prepare(List<Feature> modules, JsonObject root, Scope scope) {
      return prepare(modules, root, scope, false);
   }
   public static Runnable prepare(List<Feature> modules, JsonObject root, Scope scope, boolean startup) {
      return prepare(modules,root,scope,startup,message -> { });
   }
   public static Runnable prepare(List<Feature> modules, JsonObject root, Scope scope, boolean startup, Consumer<String> warning) {
      root = com.samsara.module.visual.Hud.Widget.migrate(CombatConfigMigration.migrate(root));
      for (String name : List.of("InvManager", "ChestStealer")) {
         var module = root.get(name);
         if (module == null || !module.isJsonObject()) continue;
         var values = module.getAsJsonObject().get("settings");
         if (values == null || !values.isJsonObject()) continue;
         var moduleState = values.getAsJsonObject();
         if (moduleState.has("Delay") && !moduleState.has("Delay Max")) moduleState.add("Delay Max", moduleState.get("Delay").deepCopy());
      }
      var settings = new ArrayList<Runnable>(); var lifecycle = new ArrayList<Runnable>();
      for (Feature module : modules) {
         if (!root.has(module.getName())) continue;
         try {
            var moduleState = root.getAsJsonObject(module.getName());
            if (moduleState.has("key")) { int key = moduleState.get("key").getAsInt(); settings.add(() -> module.setKey(key)); }
            if (!included(module, scope)) continue;
            if (moduleState.has("settings")) {
               var values = moduleState.getAsJsonObject("settings");
               for (Setting setting : module.settings) {
                  if (scope == Scope.GAMEPLAY && module instanceof com.samsara.module.combat.TargetSettings && setting.getName().equals("Visual")) continue;
                  try {
                     settings.add(setting.prepareRestore(values));
                  } catch (RuntimeException error) {
                     if (!startup) throw error;
                     warning.accept(module.getName()+" / "+setting.getName()+": "+error.getMessage());
                  }
               }
            }
            if (moduleState.has("hidden")) { boolean hidden = moduleState.get("hidden").getAsBoolean(); settings.add(() -> module.setHidden(hidden)); }
            if (!module.getName().equals("ClickGUI") && moduleState.has("enabled")) {
               boolean enabled = moduleState.get("enabled").getAsBoolean();
               lifecycle.add(() -> {
                  try { if (startup) module.restoreEnabled(enabled); else module.setEnabled(enabled); }
                  catch (RuntimeException error) {
                     if (!startup) throw error;
                     warning.accept(module.getName()+" / enabled: "+error.getMessage());
                  }
               });
            }
         } catch (RuntimeException error) {
            if (!startup) throw error;
            warning.accept(module.getName()+": "+error.getMessage());
         }
      }
      return () -> { settings.forEach(Runnable::run); lifecycle.forEach(Runnable::run); };
   }

}
