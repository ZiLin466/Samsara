package com.samsara.util;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.protocol.Packet;

public class PacketBlinkQueue implements Wrapper {
   private static boolean enabled;
   private static final List<Packet> queuedPackets = new ArrayList<>();
   private static boolean flushing;

   public static boolean enqueue(Packet packet) {
      if (enabled && !flushing) {
         synchronized (queuedPackets) {
            queuedPackets.add(packet);
            return true;
         }
      } else {
         return false;
      }
   }

   private static void flush() {
      flushing = true;
      synchronized (queuedPackets) {
         for (Packet packet : queuedPackets) {
            Wrapper.mc.getConnection().send(packet);
         }

         queuedPackets.clear();
      }

      flushing = false;
   }

   public static boolean isEnabled() {
      return enabled;
   }

   public static void disable() {
      enabled = false;
      flush();
   }

   public static void enable() {
      if (!enabled) {
         queuedPackets.clear();
      }

      enabled = true;
   }
}
