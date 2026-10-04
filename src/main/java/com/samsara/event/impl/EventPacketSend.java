package com.samsara.event.impl;

import com.samsara.event.Event;
import net.minecraft.network.protocol.Packet;

public class EventPacketSend extends Event {
   private Packet f94;

   public EventPacketSend m43(Packet var1) {
      this.f94 = var1;
      return this;
   }

   public Packet m44() {
      return this.f94;
   }
}
