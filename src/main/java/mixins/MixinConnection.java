package mixins;

import com.samsara.event.Events;
import com.samsara.event.impl.EventPacketReceive;
import com.samsara.event.impl.EventPacketSend;
import com.samsara.util.PacketBlinkQueue;
import com.samsara.util.PacketFilter;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.game.ClientboundStartConfigurationPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Connection.class})
public class MixinConnection {
   @Inject(
      method = {"send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void samsara$dispatchPacketSend(Packet packet, ChannelFutureListener listener, boolean flush, CallbackInfo callback) {
      EventPacketSend packetSendEvent = Events.PACKET_SEND.reset(packet);
      packetSendEvent.call();
      if (packetSendEvent.isCancelled()) {
         callback.cancel();
      } else {
         if (PacketBlinkQueue.enqueue(packetSendEvent.getPacket())) {
            callback.cancel();
         }

         if (PacketFilter.isRedundant(packetSendEvent.getPacket())) {
            callback.cancel();
         }
         if (!callback.isCancelled() && com.samsara.module.FeatureManager.autoRod != null) {
            com.samsara.module.FeatureManager.autoRod.observePacket(packetSendEvent.getPacket());
         }
      }
   }

   @Inject(
      method = {"channelRead0"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void samsara$dispatchPacketReceive(ChannelHandlerContext channelContext, Packet packet, CallbackInfo callback) {
      EventPacketReceive packetReceiveEvent = Events.PACKET_RECEIVE.reset(packet);
      packetReceiveEvent.call();
      if (packet instanceof ClientboundDisconnectPacket || packet instanceof ClientboundStartConfigurationPacket) {
         PacketBlinkQueue.disable();
      }

      if (packetReceiveEvent.isCancelled()) {
         callback.cancel();
      }
   }
}
