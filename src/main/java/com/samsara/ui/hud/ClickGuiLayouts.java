package com.samsara.ui.hud;
import com.google.gson.JsonObject;

public final class ClickGuiLayouts {
   private static JsonObject data = new JsonObject();
   private ClickGuiLayouts() { }
   public static JsonObject snapshot() { return data.deepCopy(); }
   public static JsonObject validate(JsonObject input) {
      for (var entry : input.entrySet()) {
         var point = entry.getValue().getAsJsonObject();
         for (String key : new String[]{"x", "y"}) {
            double value = point.get(key).getAsDouble();
            if (!Double.isFinite(value) || Math.abs(value) > 10000) throw new IllegalArgumentException("Invalid ClickGUI position");
         }
      }
      return input.deepCopy();
   }
   public static void load(JsonObject input) { data = validate(input); }
   public static float x(String id, float fallback) { return data.has(id) ? data.getAsJsonObject(id).get("x").getAsFloat() : fallback; }
   public static float y(String id, float fallback) { return data.has(id) ? data.getAsJsonObject(id).get("y").getAsFloat() : fallback; }
   public static void put(String id, float x, float y) {
      if (!Float.isFinite(x) || !Float.isFinite(y)) return;
      var point = new JsonObject(); point.addProperty("x", x); point.addProperty("y", y); data.add(id, point);
   }
}
