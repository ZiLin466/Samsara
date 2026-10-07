package com.samsara.event.impl;

import com.samsara.event.Event;
import net.minecraft.network.protocol.Packet;

public class EventPacketSend extends Event {
   private Packet packet;

   public EventPacketSend reset(Packet packet) {
      this.packet = packet;
      return this;
   }

   public Packet getPacket() {
      return this.packet;
   }
}
