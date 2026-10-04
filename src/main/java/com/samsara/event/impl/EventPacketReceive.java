package com.samsara.event.impl;

import com.samsara.event.Event;
import net.minecraft.network.protocol.Packet;

public class EventPacketReceive extends Event {
   private Packet f93;

   public EventPacketReceive m40(Packet var1) {
      this.f93 = var1;
      return this;
   }

   public Packet m41() {
      return this.f93;
   }

   public void m42(Packet var1) {
      this.f93 = var1;
   }
}
