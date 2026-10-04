package com.samsara.config;

import com.google.gson.JsonObject;

final class CombatConfigMigration {
   private CombatConfigMigration() { }
   static JsonObject migrate(JsonObject source) {
      JsonObject result = source.deepCopy();
      var aura = object(result, "KillAura");
      var settings = aura == null ? null : object(aura, "settings");
      if (settings != null) {
         if (settings.has("AutoBlock") && settings.get("AutoBlock").isJsonPrimitive()) {
            String mode = settings.get("AutoBlock").getAsString();
            if (mode.equalsIgnoreCase("Disabled") || mode.equalsIgnoreCase("Fake")) settings.addProperty("AutoBlock", "None");
            if (mode.equalsIgnoreCase("Fake")) {
               JsonObject animations = object(result, "Animations");
               if (animations == null) animations = new JsonObject();
               JsonObject values = object(animations, "settings");
               if (values == null) values = new JsonObject();
               if (!values.has("Fake Block")) values.addProperty("Fake Block", true);
               animations.add("settings", values); result.add("Animations", animations);
            }
         }
      }
      var rod = object(result, "AutoRod");
      var rodSettings = rod == null ? null : object(rod, "settings");
      if (rodSettings != null) {
         copyLegacyRange(rodSettings, "Scan Extra Range", "Scan Extra Range Min");
         copyLegacyRange(rodSettings, "Slot Reset Delay", "Slot Reset Delay Max");
         copyLegacyRange(rodSettings, "Cooldown", "Cooldown Max");
      }
      var velocity = object(result, "Velocity");
      settings = velocity == null ? null : object(velocity, "settings");
      if (settings != null) {
         if (!settings.has("Mode") && settings.has("Reduce") && settings.get("Reduce").isJsonPrimitive()) {
            String oldMode = settings.get("Reduce").getAsString();
            if (oldMode.equalsIgnoreCase("Reduce") || oldMode.equalsIgnoreCase("Delay")) {
               settings.addProperty("Mode", oldMode); settings.addProperty("Reduce", "Disabled");
               if (oldMode.equalsIgnoreCase("Delay")) {
                  if (settings.has("Delay Ticks")) settings.add("Epsilon Delay Ticks", settings.get("Delay Ticks"));
                  if (settings.has("Jump Reset")) settings.add("Epsilon Jump Reset", settings.get("Jump Reset"));
               }
            } else settings.addProperty("Mode", "Original");
         }
      }
      return result;
   }
   private static void copyLegacyRange(JsonObject settings, String oldKey, String newEndpoint) {
      if (settings.has(oldKey) && !settings.has(newEndpoint)) settings.add(newEndpoint, settings.get(oldKey).deepCopy());
   }
   private static JsonObject object(JsonObject parent, String key) {
      var value = parent.get(key); return value != null && value.isJsonObject() ? value.getAsJsonObject() : null;
   }
}
