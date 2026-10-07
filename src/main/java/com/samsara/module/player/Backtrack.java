package com.samsara.module.player;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.event.impl.EventPacketReceive;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.module.FeatureManager;
import com.samsara.setting.NumberSetting;
import com.samsara.util.TargetFinder;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.common.ClientCommonPacketListener;
import net.minecraft.network.protocol.game.ClientboundStartConfigurationPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class Backtrack extends Feature {
   private static final String BACKTRACK_LABEL = "Backtrack";
   private static final String TICKS_LABEL = "Ticks";
   private static final String RANGE_LABEL = "Range";

   private final NumberSetting range;
   private final NumberSetting ticks;

   private final Queue<Packet<ClientCommonPacketListener>> queuedPackets = new ConcurrentLinkedQueue<>();

   private boolean trackingTarget;
   private int trackingTicks;
   private Vec3 initialTargetPosition;

   @Override
   public void onEvent(Event event) {
      if (event instanceof EventPacketReceive receiving) {
         if (receiving.isCancelled() || FeatureManager.velocity.blocksBacktrack()) return;
         Packet<?> packet = receiving.getPacket();
         if (packet instanceof ClientboundStartConfigurationPacket || packet instanceof ClientboundDisconnectPacket) {
            this.flushQueuedPackets();
            return;
         }

         if (this.trackingTarget && mc.level != null) {
            Packet<ClientCommonPacketListener> deferred = switch (packet) {
               case ClientboundPingPacket ping -> ping;
               case ClientboundKeepAlivePacket keepAlive -> keepAlive;
               default -> null;
            };
            if (deferred != null) {
               receiving.setCancelled(true);
               this.queuedPackets.add(deferred);
            }
         }
      }

      if (event == Events.ROTATION) {
         if (FeatureManager.velocity.blocksBacktrack()) {
            this.trackingTicks = 0; this.initialTargetPosition = null; this.trackingTarget = false; this.flushQueuedPackets(); return;
         }
         LivingEntity target = TargetFinder.nearestTarget(this.range.getValue(), true);
         if (target != null) {
            Vec3 position = target.position();
            if (!this.trackingTarget) {
               this.initialTargetPosition = position;
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
      Packet<ClientCommonPacketListener> packet;
      while ((packet = this.queuedPackets.poll()) != null) {
         packet.handle(mc.getConnection());
      }
   }

   public Backtrack() {
      super(BACKTRACK_LABEL, Category.PLAYER);
      this.range = new NumberSetting(RANGE_LABEL, this, 6.0, 4.0, 8.0, 0.5);
      this.ticks = new NumberSetting(TICKS_LABEL, this, 2.0, 1.0, 10.0, 1.0);
   }
}
