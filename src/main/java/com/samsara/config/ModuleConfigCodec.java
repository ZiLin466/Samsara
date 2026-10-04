package com.samsara.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.*;
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
         var data = new JsonObject(); var settings = new JsonObject();
         data.addProperty("key", defaults ? module.getDefaultKey() : module.getKey());
         if (!included(module, scope)) {
            root.add(module.getName(), data);
            continue;
         }
         data.addProperty("enabled", !defaults && !module.getName().equals("ClickGUI") && module.isEnabled());
         data.addProperty("hidden", !defaults && module.isHidden());
         for (Setting setting : module.settings) {
            if (scope == Scope.GAMEPLAY && module instanceof com.samsara.module.combat.TargetSettings && setting.getName().equals("Visual")) continue;
            if (setting instanceof BooleanSetting v) settings.addProperty(setting.getName(), defaults ? v.m216() : v.m215());
            else if (setting instanceof NumberSetting v) settings.addProperty(setting.getName(), defaults ? v.m221() : v.m220());
            else if (setting instanceof ModeSetting v) settings.addProperty(setting.getName(), defaults ? v.m225() : v.m224());
            else if (setting instanceof MultiSelectSetting v) {
               var selected = new JsonArray();
               (defaults ? v.defaultValues() : v.selectedValues()).forEach(selected::add);
               settings.add(setting.getName(), selected);
            }
         }
         data.add("settings", settings); root.add(module.getName(), data);
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
      root = CombatConfigMigration.migrate(root);
      for (String name : List.of("InvManager", "ChestStealer")) {
         var module = root.get(name);
         if (module == null || !module.isJsonObject()) continue;
         var values = module.getAsJsonObject().get("settings");
         if (values == null || !values.isJsonObject()) continue;
         var data = values.getAsJsonObject();
         if (data.has("Delay") && !data.has("Delay Max")) data.add("Delay Max", data.get("Delay").deepCopy());
      }
      var settings = new ArrayList<Runnable>(); var lifecycle = new ArrayList<Runnable>();
      for (Feature module : modules) {
         if (!root.has(module.getName())) continue;
         try {
            var data = root.getAsJsonObject(module.getName());
            if (data.has("key")) { int key = data.get("key").getAsInt(); settings.add(() -> module.setKey(key)); }
            if (!included(module, scope)) continue;
            if (data.has("settings")) {
               var values = data.getAsJsonObject("settings");
               for (Setting setting : module.settings) {
                  if (scope == Scope.GAMEPLAY && module instanceof com.samsara.module.combat.TargetSettings && setting.getName().equals("Visual")) continue;
                  if (!values.has(setting.getName()) && !(setting instanceof MultiSelectSetting)) continue;
                  try {
                     var value = values.get(setting.getName());
                     if (setting instanceof BooleanSetting bool) {
                        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) throw new IllegalArgumentException("Invalid boolean");
                        boolean desired = value.getAsBoolean(); settings.add(() -> bool.m217(desired));
                     } else if (setting instanceof NumberSetting number) {
                        double desired = value.getAsDouble();
                        if (!Double.isFinite(desired)) throw new IllegalArgumentException("Non-finite setting");
                        settings.add(() -> number.m223(desired));
                     } else if (setting instanceof ModeSetting mode) {
                        String selected = mode.canonical(value.getAsString());
                        settings.add(() -> mode.m226(selected));
                     } else if (setting instanceof MultiSelectSetting choices) {
                        List<String> selected;
                        if (value == null) selected = choices.legacySelection(values);
                        else {
                           if (!value.isJsonArray()) throw new IllegalArgumentException("Invalid selection array");
                           var entries = new ArrayList<String>();
                           for (var entry : value.getAsJsonArray()) {
                              if (!entry.isJsonPrimitive() || !entry.getAsJsonPrimitive().isString()) {
                                 throw new IllegalArgumentException("Invalid choice");
                              }
                              entries.add(entry.getAsString());
                           }
                           selected = choices.canonical(entries);
                        }
                        settings.add(() -> choices.setSelected(selected));
                     }
                  } catch (RuntimeException error) {
                     if (!startup) throw error;
                     warning.accept(module.getName()+" / "+setting.getName()+": "+error.getMessage());
                  }
               }
            }
            if (data.has("hidden")) { boolean hidden = data.get("hidden").getAsBoolean(); settings.add(() -> module.setHidden(hidden)); }
            if (!module.getName().equals("ClickGUI") && data.has("enabled")) {
               boolean enabled = data.get("enabled").getAsBoolean();
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
