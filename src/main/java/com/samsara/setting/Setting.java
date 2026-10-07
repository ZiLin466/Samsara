package com.samsara.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.samsara.module.Feature;
import java.util.function.BooleanSupplier;

public abstract class Setting {
   private final Feature module;
   private final String name;
   private String displayName;
   private BooleanSupplier visibility;

   public String getName() {
      return this.name;
   }

   public String getDisplayName() {
      return this.displayName == null ? this.name : this.displayName;
   }

   public Setting setDisplayName(String displayName) {
      this.displayName = displayName;
      return this;
   }

   public Feature getModule() {
      return this.module;
   }

   public Setting(String name, Feature module) {
      this.name = name;
      this.module = module;
      this.module.registerSetting(this);
   }

   /** Hides the setting from click gui windows while keeping it in configs. */
   public Setting setVisible(BooleanSupplier visible) {
      this.visibility = visible;
      return this;
   }

   public boolean isVisible() {
      return this.visibility == null || this.visibility.getAsBoolean();
   }

   public abstract JsonElement snapshot(boolean defaults);

   /** Validate now and apply later, so a failed preset cannot partially change earlier values. */
   public final Runnable prepareRestore(JsonObject values) {
      JsonElement saved = values.get(this.name);
      return saved == null ? prepareMissing(values) : prepareValue(saved);
   }

   protected Runnable prepareMissing(JsonObject values) { return () -> { }; }

   protected abstract Runnable prepareValue(JsonElement value);
}
