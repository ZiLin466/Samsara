package com.samsara.util;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.protocol.Packet;

public class PacketBlinkQueue implements Wrapper {
   private static boolean f746;
   private static final List<Packet> f745 = new ArrayList<>();
   private static boolean f747;

   public static boolean m25(Packet var0) {
      if (f746 && !f747) {
         synchronized (f745) {
            f745.add(var0);
            return true;
         }
      } else {
         return false;
      }
   }

   private static void pm$111() {
      f747 = true;
      synchronized (f745) {
         for (Packet var2 : f745) {
            Wrapper.mc.getConnection().send(var2);
         }

         f745.clear();
      }

      f747 = false;
   }

   public static boolean m24() {
      return f746;
   }

   public static void m23() {
      f746 = false;
      pm$111();
   }

   public static void m22() {
      if (!f746) {
         f745.clear();
      }

      f746 = true;
   }
}
