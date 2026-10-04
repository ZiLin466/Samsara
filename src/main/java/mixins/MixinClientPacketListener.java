package mixins;

import com.samsara.ui.dynamicIsland.DynamicIslandLatency;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.ping.ClientboundPongResponsePacket;
import net.minecraft.network.protocol.ping.ServerboundPingRequestPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class MixinClientPacketListener implements DynamicIslandLatency.Source {
   @Inject(method = "handleMoveEntity", at = @At("TAIL"))
   private void samsara$predictMovedPlayer(net.minecraft.network.protocol.game.ClientboundMoveEntityPacket packet, CallbackInfo callback) {
      var mc = Minecraft.getInstance();
      var aura = com.samsara.module.FeatureManager.f26;
      if (aura != null && mc.level != null && mc.getConnection() == (ClientPacketListener)(Object)this) {
         aura.onPredictEntityUpdate(packet.getEntity(mc.level), packet.hasRotation(), packet.hasPosition(), false);
      }
   }

   @Inject(method = "handleRotateMob", at = @At("TAIL"))
   private void samsara$predictPlayerHead(net.minecraft.network.protocol.game.ClientboundRotateHeadPacket packet, CallbackInfo callback) {
      var mc = Minecraft.getInstance();
      var aura = com.samsara.module.FeatureManager.f26;
      if (aura != null && mc.level != null && mc.getConnection() == (ClientPacketListener)(Object)this) {
         aura.onPredictHeadUpdate(packet.getEntity(mc.level), packet.getYHeadRot());
      }
   }

   @Inject(method = "handleEntityPositionSync", at = @At("TAIL"))
   private void samsara$predictSyncedPlayer(net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket packet, CallbackInfo callback) {
      var mc = Minecraft.getInstance();
      var aura = com.samsara.module.FeatureManager.f26;
      if (aura != null && mc.level != null && mc.getConnection() == (ClientPacketListener)(Object)this) {
         aura.onPredictEntityUpdate(mc.level.getEntity(packet.id()), true, true, false);
      }
   }

   @Inject(method = "handleTeleportEntity", at = @At("TAIL"))
   private void samsara$predictTeleportedPlayer(net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket packet, CallbackInfo callback) {
      var mc = Minecraft.getInstance();
      var aura = com.samsara.module.FeatureManager.f26;
      if (aura != null && mc.level != null && mc.getConnection() == (ClientPacketListener)(Object)this) {
         aura.onPredictEntityUpdate(mc.level.getEntity(packet.id()), true, true, true);
      }
   }

   @Inject(method = "handleDamageEvent", at = @At("TAIL"))
   private void samsara$recordVelocityDamage(net.minecraft.network.protocol.game.ClientboundDamageEventPacket packet, CallbackInfo callback) {
      var mc = Minecraft.getInstance();
      var velocity = com.samsara.module.FeatureManager.f28;
      if (velocity != null && mc.getConnection() == (ClientPacketListener)(Object)this) velocity.recordDamage(packet);
   }
   @Inject(method = "handleLogin", at = @At("TAIL"))
   private void samsara$initializeRestoredModules(net.minecraft.network.protocol.game.ClientboundLoginPacket packet, CallbackInfo callback) {
      com.samsara.module.FeatureManager.getModules().forEach(com.samsara.module.Feature::initializeWorldState);
   }
   @Inject(method = "setTitleText", at = @At("TAIL"))
   private void samsara$recordVictory(net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket packet, CallbackInfo callback) {
      com.samsara.module.visual.SessionHud.SessionTracker.title(packet.text().getString());
   }
   @Inject(method = "handleEntityEvent", at = @At("TAIL"))
   private void samsara$recordDeath(net.minecraft.network.protocol.game.ClientboundEntityEventPacket packet, CallbackInfo callback) {
      var level = Minecraft.getInstance().level;
      if (level != null && packet.getEventId() == 3) com.samsara.module.visual.SessionHud.SessionTracker.death(packet.getEntity(level));
   }
   @Unique
   private final DynamicIslandLatency samsara$islandLatency = new DynamicIslandLatency();

   @Inject(method = "tick", at = @At("TAIL"))
   private void samsara$sampleIslandLatency(CallbackInfo callback) {
      Minecraft mc = Minecraft.getInstance();
      ClientPacketListener listener = (ClientPacketListener)(Object)this;
      if (mc.getConnection() != listener || mc.player == null || mc.hasSingleplayerServer()) {
         this.samsara$islandLatency.reset();
         return;
      }
      long token = this.samsara$islandLatency.request(System.nanoTime() / 1_000_000L);
      if (token != 0) listener.send(new ServerboundPingRequestPacket(token));
   }

   @Inject(method = "handlePongResponse", at = @At("HEAD"), cancellable = true)
   private void samsara$receiveIslandLatency(ClientboundPongResponsePacket packet, CallbackInfo callback) {
      if (this.samsara$islandLatency.receive(packet.time(), System.nanoTime() / 1_000_000L)) {
         // Our opaque IDs are not vanilla wall-clock timestamps; do not log them in the F3 chart.
         callback.cancel();
      }
   }

   @Override
   public int samsara$getLivePing(long now, int fallback) {
      return this.samsara$islandLatency.latency(now, fallback);
   }
}
