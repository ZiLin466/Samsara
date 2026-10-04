package com.samsara.ui.hud;

import com.google.gson.JsonObject;
import java.util.EnumMap;

/** Persisted visual geometry. Named gameplay configurations never serialize this store. */
public final class HudLayouts {
   public enum Element {
      INVENTORY(true, 10, 83), TARGET(true, 8, 4), SESSION(true, 12, 16), POTION(true, 6.8, -2.6), ISLAND(false, 0, 0), ARRAYLIST(false, 0, 0);
      public final boolean draggable;
      final double x, y;
      Element(boolean draggable, double x, double y) { this.draggable = draggable; this.x = x; this.y = y; }
   }
   public record Box(float x, float y, float width, float height) {
      public boolean contains(double x, double y) {
         return x >= this.x && x < this.x + this.width && y >= this.y && y < this.y + this.height;
      }
   }
   public static final class Placement {
      private double x, y, scale = 1;
      Placement(Element element) { this.x = element.x; this.y = element.y; }
      public double x() { return this.x; }
      public double y() { return this.y; }
      public float scale() { return (float)this.scale; }
      public void move(double x, double y) {
         if (Double.isFinite(x) && Double.isFinite(y)) { this.x = Math.clamp(x, -4096, 4096); this.y = Math.clamp(y, -4096, 4096); }
      }
      public void scale(double scale) { if (Double.isFinite(scale)) this.scale = Math.clamp(scale, .5, 2); }
      public void resetScale() { this.scale = 1; }
   }
   public static final HudLayouts INSTANCE = new HudLayouts();
   private final EnumMap<Element, Placement> placements = new EnumMap<>(Element.class);
   private final EnumMap<Element, Box> bounds = new EnumMap<>(Element.class);
   public HudLayouts() { for (Element element : Element.values()) this.placements.put(element, new Placement(element)); }
   public Placement get(Element element) { return this.placements.get(element); }
   public void drawn(Element element, Box box) { this.bounds.put(element, box); }
   public Box bounds(Element element) { return this.bounds.get(element); }
   public void beginFrame() { this.bounds.clear(); }
   public Element hit(double x, double y) {
      Element[] elements = Element.values();
      for (int i = elements.length - 1; i >= 0; i--) {
         Box box = bounds(elements[i]); if (box != null && box.contains(x, y)) return elements[i];
      }
      return null;
   }
   public Box fit(Element element, float width, float height, int viewportWidth, int viewportHeight, boolean centered) {
      Placement placement = get(element);
      float scale = placement.scale();
      scale = Math.min(scale, Math.min(Math.max(1, viewportWidth - 4) / width, Math.max(1, viewportHeight - 4) / height));
      float w = width * scale, h = height * scale;
      float x = (float)(placement.x + (centered ? viewportWidth / 2 : 0));
      float y = (float)(placement.y + (centered ? viewportHeight / 2 : element == Element.POTION ? (viewportHeight - h) / 2 : 0));
      return new Box(Math.clamp(x, 2, Math.max(2, viewportWidth - w - 2)),
         Math.clamp(y, 2, Math.max(2, viewportHeight - h - 2)), w, h);
   }
   public JsonObject snapshot() {
      JsonObject result = new JsonObject();
      for (var entry : this.placements.entrySet()) {
         var value = entry.getValue(); var data = new JsonObject();
         data.addProperty("x", value.x); data.addProperty("y", value.y); data.addProperty("scale", value.scale);
         result.add(entry.getKey().name(), data);
      }
      return result;
   }
   public void load(JsonObject root) {
      // Validate the entire layout before changing any live geometry.
      var parsed = new EnumMap<Element, Placement>(Element.class);
      for (Element element : Element.values()) {
         if (!root.has(element.name())) continue;
         JsonObject data = root.getAsJsonObject(element.name()); Placement next = new Placement(element);
         double x = data.get("x").getAsDouble(), y = data.get("y").getAsDouble(), scale = data.get("scale").getAsDouble();
         if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(scale)) throw new IllegalArgumentException("Invalid HUD layout");
         next.move(x, y); next.scale(scale); parsed.put(element, next);
      }
      this.placements.putAll(parsed);
   }
}
