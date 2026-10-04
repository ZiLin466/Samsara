package com.samsara.event;

import com.samsara.module.Feature;
import com.samsara.module.FeatureManager;
import java.util.List;

public abstract class Event {
   private boolean cancelled;
   private int moduleCount;
   private Feature[] modules = new Feature[FeatureManager.getModules().size()];

   public boolean isCancelled() {
      return this.cancelled;
   }

   public void sortModules() {
      List var1 = FeatureManager.getModules();
      this.moduleCount = 0;
      int var2 = 0;

      for (int var3 = var1.size(); var2 < var3; var2++) {
         Feature var4 = (Feature)var1.get(var2);
         if (var4.isEnabled()) {
            int var5 = var4.getPriority(this);

            int var6;
            for (var6 = this.moduleCount - 1; var6 >= 0 && this.modules[var6].getPriority(this) > var5; var6--) {
               this.modules[var6 + 1] = this.modules[var6];
            }

            this.modules[var6 + 1] = var4;
            this.moduleCount++;
         }
      }
   }

   public void call() {
      this.cancelled = false;

      for (int var1 = 0; var1 < this.moduleCount; var1++) {
         this.modules[var1].onEvent(this);
      }
   }

   public void setCancelled(boolean var1) {
      this.cancelled = var1;
   }
}
