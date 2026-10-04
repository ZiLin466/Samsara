package com.samsara.event.impl;

import com.samsara.event.Event;
import net.minecraft.resources.Identifier;

public class EventSound extends Event {
   private Identifier f95;

   public Identifier m46() {
      return this.f95;
   }

   public EventSound m45(Identifier var1) {
      this.f95 = var1;
      return this;
   }
}
