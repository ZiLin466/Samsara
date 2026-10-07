package com.samsara.event.impl;

import com.samsara.event.Event;
import com.samsara.util.ClientColors;
import com.samsara.util.WorldToScreenProjector;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class EventRender2D extends Event {
   private GuiGraphicsExtractor graphics;
   private DeltaTracker deltaTracker;

   public EventRender2D reset(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
      this.graphics = graphics;
      this.deltaTracker = deltaTracker;
      WorldToScreenProjector.updateCamera();
      ClientColors.updateTheme();
      return this;
   }

   public DeltaTracker getDeltaTracker() {
      return this.deltaTracker;
   }

   public GuiGraphicsExtractor getGraphics() {
      return this.graphics;
   }

   public float getPartialTick() {
      return this.deltaTracker.getGameTimeDeltaPartialTick(false);
   }
}
