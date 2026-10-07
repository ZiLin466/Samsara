package com.samsara.event.impl;

import com.samsara.event.Event;
import net.minecraft.network.protocol.Packet;

public final class EventPacketReceive extends Event {
   private Packet<?> packet;

   public EventPacketReceive() { }

   public EventPacketReceive(Packet<?> packet) {
      this.packet = packet;
   }

   public Packet<?> getPacket() {
      return this.packet;
   }

   public void setPacket(Packet<?> packet) {
      this.packet = packet;
   }
}
