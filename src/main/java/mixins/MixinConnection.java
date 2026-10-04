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
   private void pm$20(Packet var1, ChannelFutureListener var2, boolean var3, CallbackInfo var4) {
      EventPacketSend var5 = Events.f10.m43(var1);
      var5.call();
      if (var5.isCancelled()) {
         var4.cancel();
      } else {
         if (PacketBlinkQueue.m25(var5.m44())) {
            var4.cancel();
         }

         if (PacketFilter.m20(var5.m44())) {
            var4.cancel();
         }
         if (!var4.isCancelled() && com.samsara.module.FeatureManager.autoRod != null) {
            com.samsara.module.FeatureManager.autoRod.observePacket(var5.m44());
         }
      }
   }

   @Inject(
      method = {"channelRead0"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pm$21(ChannelHandlerContext var1, Packet var2, CallbackInfo var3) {
      EventPacketReceive var4 = Events.f11.m40(var2);
      var4.call();
      if (var2 instanceof ClientboundDisconnectPacket || var2 instanceof ClientboundStartConfigurationPacket) {
         PacketBlinkQueue.m23();
      }

      if (var4.isCancelled()) {
         var3.cancel();
      }
   }
}
