package com.samsara.setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.samsara.module.Feature;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class MultiSelectSetting extends Setting implements ChoiceSetting {
   private final List<String> options;
   private final List<String> defaults;
   private List<String> selected;
   private String legacyPrefix;
   private boolean orderSensitive;
   private boolean allowEmpty = true;

   public MultiSelectSetting(String name, Feature owner, String[] options, Collection<String> defaults) {
      super(name, owner);
      this.options = List.of(options.clone());
      if (this.options.isEmpty() || new LinkedHashSet<>(this.options).size() != this.options.size()) {
         throw new IllegalArgumentException("Choices must be nonempty and unique");
      }
      this.defaults = canonical(defaults);
      this.selected = this.defaults;
   }

   public void setLegacyBooleanPrefix(String prefix) { this.legacyPrefix = prefix; }
   public void setOrderSensitive(boolean value) { this.orderSensitive = value; }
   public void setAllowEmpty(boolean value) { this.allowEmpty = value; }
   public List<String> selectedValues() { return this.selected; }
   public List<String> defaultValues() { return this.defaults; }
   public void setSelected(Collection<String> values) { this.selected = canonical(values); }
   public boolean contains(String value) { return this.selected.contains(value); }

   public List<String> canonical(Collection<String> values) {
      Set<String> result = new LinkedHashSet<>();
      for (String value : values) {
         String option = this.options.stream().filter(candidate -> candidate.equalsIgnoreCase(value)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown choice: " + value));
         result.add(option);
      }
      if (!this.allowEmpty && result.isEmpty()) throw new IllegalArgumentException("At least one choice is required");
      return this.orderSensitive ? List.copyOf(result) : this.options.stream().filter(result::contains).toList();
   }

   /** Only used when a saved selection array is absent. */
   public List<String> legacySelection(JsonObject values) {
      if (this.legacyPrefix == null) return this.selected;
      Set<String> result = new LinkedHashSet<>(this.selected);
      for (String option : this.options) {
         JsonElement value = values.get(this.legacyPrefix + option);
         if (value == null) continue;
         if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
            throw new IllegalArgumentException("Invalid legacy choice: " + option);
         }
         if (value.getAsBoolean()) result.add(option); else result.remove(option);
      }
      return canonical(result);
   }

   @Override public String[] options() { return this.options.toArray(String[]::new); }
   @Override public String selectionLabel() { return this.selected.size() + " Selected"; }
   @Override public boolean selected(int index) { return contains(this.options.get(index)); }
   @Override public boolean multiple() { return true; }
   @Override public void select(int index) {
      Set<String> result = new LinkedHashSet<>(this.selected);
      String option = this.options.get(index);
      if (!result.remove(option)) result.add(option);
      if (!this.allowEmpty && result.isEmpty()) return;
      setSelected(result);
   }

   @Override public JsonElement snapshot(boolean defaults) {
      JsonArray values = new JsonArray();
      (defaults ? this.defaults : this.selected).forEach(values::add);
      return values;
   }

   @Override protected Runnable prepareMissing(JsonObject values) {
      List<String> desired = legacySelection(values);
      return () -> setSelected(desired);
   }

   @Override protected Runnable prepareValue(JsonElement value) {
      if (!value.isJsonArray()) throw new IllegalArgumentException("Invalid selection array");
      var entries = new ArrayList<String>();
      for (JsonElement entry : value.getAsJsonArray()) {
         if (!entry.isJsonPrimitive() || !entry.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Invalid choice");
         }
         entries.add(entry.getAsString());
      }
      List<String> desired = canonical(entries);
      return () -> setSelected(desired);
   }
}
