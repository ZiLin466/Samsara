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
      List<Feature> registeredFeatures = FeatureManager.getModules();
      this.moduleCount = 0;
      int featureIndex = 0;

      for (int featureCount = registeredFeatures.size(); featureIndex < featureCount; featureIndex++) {
         Feature feature = registeredFeatures.get(featureIndex);
         if (feature.isEnabled()) {
            int priority = feature.getPriority(this);

            int insertionIndex;
            for (insertionIndex = this.moduleCount - 1; insertionIndex >= 0 && this.modules[insertionIndex].getPriority(this) > priority; insertionIndex--) {
               this.modules[insertionIndex + 1] = this.modules[insertionIndex];
            }

            this.modules[insertionIndex + 1] = feature;
            this.moduleCount++;
         }
      }
   }

   public void call() {
      this.cancelled = false;

      for (int listenerIndex = 0; listenerIndex < this.moduleCount; listenerIndex++) {
         this.modules[listenerIndex].onEvent(this);
      }
   }

   public void setCancelled(boolean cancelled) {
      this.cancelled = cancelled;
   }
}
