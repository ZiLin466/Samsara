package com.samsara.module;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.setting.Setting;
import com.samsara.ui.dynamicIsland.DynamicIslandManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.Minecraft;

public abstract class Feature {
   private static final String EMPTY_SUFFIX = "";

   protected static final Minecraft mc = Minecraft.getInstance();

   private final Category category;
   private final String name;

   public List<Setting> settings = new ArrayList<>();
   private boolean enabled;
   private boolean pendingWorldEnable;
   private int defaultKey = 0;
   private int keyCode;
   private String suffix;
   private boolean hidden;
   private String displayName;

   public String getDisplayName() {
      return this.displayName;
   }

   public void onEvent(Event event) {
   }

   public int getDefaultKey() {
      return this.defaultKey;
   }

   public String getName() {
      return this.name;
   }

   public void setKey(int key) {
      this.keyCode = key;
   }

   public void setEnabled(boolean enabled) {
      if (this.enabled != enabled) {
         this.toggle();
      }
   }

   /** Restore gameplay flags at startup; world-dependent hooks run after joining. */
   public void restoreEnabled(boolean desired) {
      if (this.category == Category.VISUAL || (mc != null && mc.player != null)) { setEnabled(desired); return; }
      this.enabled = desired; this.pendingWorldEnable = desired;
   }

   public void initializeWorldState() {
      if (this.pendingWorldEnable && this.enabled && mc != null && mc.player != null && mc.level != null) {
         this.pendingWorldEnable = false; this.onEnable();
      }
   }

   public void onDisable() {
   }

   public int getPriority(Event event) {
      return 0;
   }

   public Category getCategory() {
      return this.category;
   }

   public void setSuffix(String suffix) {
      if (!Objects.equals(this.suffix, suffix)) {
         this.suffix = suffix;
         this.displayName = suffix == null ? this.name : this.name + "§7 " + suffix;
         FeatureManager.sortModules();
      }
   }

   public void setHidden(boolean hidden) {
      this.hidden = hidden;
   }

   public void toggle() {
      this.enabled = !this.enabled;
      if (this.enabled) {
         this.onEnable();
      } else if (!this.pendingWorldEnable) {
         this.onDisable();
      }
      this.pendingWorldEnable = false;

      Events.refreshListeners();
      DynamicIslandManager.onModuleToggled(this);
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public Feature(String name, int keyCode, Category category) {
      this.category = category;
      this.name = name;
      this.displayName = name;
      this.suffix = EMPTY_SUFFIX;
      this.keyCode = keyCode;
      this.defaultKey = keyCode;
      this.enabled = false;
   }

   public void onEnable() {
   }

   public String getSuffix() {
      return this.suffix;
   }

   public Feature(String name, Category category) {
      this(name, 0, category);
   }

   public int getKey() {
      return this.keyCode;
   }

   public boolean isHidden() {
      return this.hidden;
   }
}
