package com.samsara.module.player;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.event.impl.EventPacketReceive;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.NumberSetting;
import com.samsara.util.TargetFinder;
import java.nio.charset.StandardCharsets;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.game.ClientboundStartConfigurationPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class Backtrack extends Feature {
   private static final String f443 = "Backtrack";
   private boolean f449;
   private final NumberSetting f447;
   private static final String f445 = "Ticks";
   private int f451;
   private final NumberSetting f446;
   private static final String f444 = "Range";
   private Vec3 f450;
   private final Queue f448;

   @Override
   public void onEvent(Event var1) {
      if (var1 == Events.f11) {
         if (var1.isCancelled() || com.samsara.module.FeatureManager.f28.blocksBacktrack()) return;
         EventPacketReceive var2 = (EventPacketReceive)var1;
         Packet var3 = var2.m41();
         if (var3 instanceof ClientboundStartConfigurationPacket || var3 instanceof ClientboundDisconnectPacket) {
            this.pm$82();
            return;
         }

         if (this.f449 && (var3 instanceof ClientboundPingPacket || var3 instanceof ClientboundKeepAlivePacket) && mc.level != null) {
            var2.setCancelled(true);
            this.f448.add(new QueuedPacket(var3, mc.level.getGameTime()));
         }
      }

      if (var1 == Events.f3) {
         if (com.samsara.module.FeatureManager.f28.blocksBacktrack()) {
            this.f451 = 0; this.f450 = null; this.f449 = false; this.pm$82(); return;
         }
         LivingEntity var4 = TargetFinder.m47(this.f446.m220(), true);
         if (var4 != null) {
            Vec3 var5 = var4.position();
            if (!this.f449) {
               this.f450 = new Vec3(var5.x, var5.y, var5.z);
            }

            if (this.f450.distanceTo(mc.player.position()) < var4.position().distanceTo(mc.player.position())) {
               this.pm$82();
            }

            if ((double)this.f451 >= this.f447.m220()) {
               this.pm$82();
               this.f451 = 0;
            }

            this.f449 = true;
            this.f451++;
         } else {
            this.f451 = 0;
            this.f450 = null;
            this.f449 = false;
            this.pm$82();
         }
      }
   }

   private void pm$82() {
      while (!this.f448.isEmpty()) {
         QueuedPacket var1 = (QueuedPacket)this.f448.peek();
         this.f448.poll();
         var1.packet.handle(mc.getConnection());
      }
   }

   public Backtrack() {
      super(f443, Category.PLAYER);
      this.f446 = new NumberSetting(f444, this, 6.0, 4.0, 8.0, 0.5);
      this.f447 = new NumberSetting(f445, this, 2.0, 1.0, 10.0, 1.0);
      this.f448 = new ConcurrentLinkedQueue();
   }

   public static class QueuedPacket {
      public Packet packet;
      public long time;

      public QueuedPacket(Packet var1, long var2) {
         this.packet = var1;
         this.time = var2;
      }
   }
}
