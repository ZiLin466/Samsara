package com.samsara.event.impl;

import com.samsara.event.Event;
import net.minecraft.network.protocol.Packet;

public class EventPacketReceive extends Event {
   private Packet packet;

   public EventPacketReceive reset(Packet packet) {
      this.packet = packet;
      return this;
   }

   public Packet getPacket() {
      return this.packet;
   }

   public void setPacket(Packet packet) {
      this.packet = packet;
   }
}
