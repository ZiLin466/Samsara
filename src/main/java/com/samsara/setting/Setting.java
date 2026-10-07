package com.samsara.setting;

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
      this.module.settings.add(this);
   }

   /** Hides the setting from click gui windows while keeping it in configs. */
   public Setting setVisible(BooleanSupplier visible) {
      this.visibility = visible;
      return this;
   }

   public boolean isVisible() {
      return this.visibility == null || this.visibility.getAsBoolean();
   }
}
