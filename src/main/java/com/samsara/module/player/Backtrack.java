package com.samsara.module.player;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.event.impl.EventPacketReceive;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.NumberSetting;
import com.samsara.util.TargetFinder;
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
   private static final String BACKTRACK_LABEL = "Backtrack";
   private boolean trackingTarget;
   private final NumberSetting ticks;
   private static final String TICKS_LABEL = "Ticks";
   private int trackingTicks;
   private final NumberSetting range;
   private static final String RANGE_LABEL = "Range";
   private Vec3 initialTargetPosition;
   private final Queue queuedPackets;

   @Override
   public void onEvent(Event event) {
      if (event == Events.PACKET_RECEIVE) {
         if (event.isCancelled() || com.samsara.module.FeatureManager.velocity.blocksBacktrack()) return;
         EventPacketReceive packetReceiveEvent = (EventPacketReceive)event;
         Packet packet = packetReceiveEvent.getPacket();
         if (packet instanceof ClientboundStartConfigurationPacket || packet instanceof ClientboundDisconnectPacket) {
            this.flushQueuedPackets();
            return;
         }

         if (this.trackingTarget && (packet instanceof ClientboundPingPacket || packet instanceof ClientboundKeepAlivePacket) && mc.level != null) {
            packetReceiveEvent.setCancelled(true);
            this.queuedPackets.add(new QueuedPacket(packet, mc.level.getGameTime()));
         }
      }

      if (event == Events.ROTATION) {
         if (com.samsara.module.FeatureManager.velocity.blocksBacktrack()) {
            this.trackingTicks = 0; this.initialTargetPosition = null; this.trackingTarget = false; this.flushQueuedPackets(); return;
         }
         LivingEntity target = TargetFinder.nearestTarget(this.range.getValue(), true);
         if (target != null) {
            Vec3 position = target.position();
            if (!this.trackingTarget) {
               this.initialTargetPosition = new Vec3(position.x, position.y, position.z);
            }

            if (this.initialTargetPosition.distanceTo(mc.player.position()) < target.position().distanceTo(mc.player.position())) {
               this.flushQueuedPackets();
            }

            if ((double)this.trackingTicks >= this.ticks.getValue()) {
               this.flushQueuedPackets();
               this.trackingTicks = 0;
            }

            this.trackingTarget = true;
            this.trackingTicks++;
         } else {
            this.trackingTicks = 0;
            this.initialTargetPosition = null;
            this.trackingTarget = false;
            this.flushQueuedPackets();
         }
      }
   }

   private void flushQueuedPackets() {
      while (!this.queuedPackets.isEmpty()) {
         QueuedPacket queuedPacket = (QueuedPacket)this.queuedPackets.peek();
         this.queuedPackets.poll();
         queuedPacket.packet.handle(mc.getConnection());
      }
   }

   public Backtrack() {
      super(BACKTRACK_LABEL, Category.PLAYER);
      this.range = new NumberSetting(RANGE_LABEL, this, 6.0, 4.0, 8.0, 0.5);
      this.ticks = new NumberSetting(TICKS_LABEL, this, 2.0, 1.0, 10.0, 1.0);
      this.queuedPackets = new ConcurrentLinkedQueue();
   }

   public static class QueuedPacket {
      public Packet packet;
      public long time;

      public QueuedPacket(Packet packet, long time) {
         this.packet = packet;
         this.time = time;
      }
   }
}
