package com.samsara.event.impl;

import com.samsara.event.Event;
import com.samsara.util.ClientColors;
import com.samsara.util.WorldToScreenProjector;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class EventRender2D extends Event {
   private GuiGraphicsExtractor f116;
   private DeltaTracker f117;

   public EventRender2D m88(GuiGraphicsExtractor var1, DeltaTracker var2) {
      this.f116 = var1;
      this.f117 = var2;
      WorldToScreenProjector.m42();
      ClientColors.m51();
      return this;
   }

   public DeltaTracker m90() {
      return this.f117;
   }

   public GuiGraphicsExtractor m89() {
      return this.f116;
   }

   public float m91() {
      return this.f117.getGameTimeDeltaPartialTick(false);
   }
}
