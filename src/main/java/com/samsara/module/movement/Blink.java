package com.samsara.module.movement;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.BooleanSetting;
import com.samsara.setting.NumberSetting;
import com.mojang.authlib.GameProfile;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.client.gui.screens.RecoverWorldDataScreen;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.world.entity.Entity;

public class Blink extends Feature {
   private static final String BLINK_LABEL = "Blink";
   private static final String TICK_LABEL = "Tick";
   private static final String SLOW_RELEASE_LABEL = "Slow Release";
   private static final String SLOW_MOVE_LABEL = "Slow Move";
   private static final String SLOW_MOVE_TICK_LABEL = "Slow Move Tick";
   private static final String FAKE_PLAYER_LABEL = "Fake Player";
   private final NumberSetting tick;
   private final BooleanSetting slowRelease;
   private final BooleanSetting slowMove;
   private final NumberSetting slowMoveTick;
   private final BooleanSetting fakePlayer;
   private final ConcurrentLinkedQueue<Packet> queuedPackets;
   private RemotePlayer fakePlayerEntity;
   private boolean flushing;
   private double serverX;
   private double serverY;
   private double serverZ;
   private double previousServerX;
   private double previousServerY;
   private double previousServerZ;
   private float serverYaw;
   private float serverPitch;
   private float serverHeadYaw;
   private float previousServerYaw;
   private float previousServerPitch;
   private float previousServerHeadYaw;
   private double renderedX;
   private double renderedY;
   private double renderedZ;

   @Override
   public void onEvent(Event event) {
      if (event == Events.PACKET_SEND) {
         if (this.flushing || mc.player == null) {
            return;
         }

         Packet packet = Events.PACKET_SEND.getPacket();
         if (packet instanceof ServerboundHelloPacket || packet instanceof ClientIntentionPacket) {
            return;
         }

         if (mc.gui.screen() instanceof RecoverWorldDataScreen) {
            this.setEnabled(false);
            return;
         }

         if (packet instanceof ServerboundMovePlayerPacket) {
            if (this.countQueuedMovementPackets() > this.tick.getValue()) {
               if (this.slowRelease.getValue()) {
                  this.flushOneMovement();
               } else {
                  this.flushAllPackets();
               }
            }

            if (this.slowMove.getValue() && mc.player.tickCount % (int)this.slowMoveTick.getValue() == 0) {
               this.flushOneMovement();
            }
         }

         event.setCancelled(true);
         this.queuedPackets.add(packet);
      }

      if (event == Events.RENDER_2D) {
         if (this.fakePlayerEntity != null && this.fakePlayer.getValue() && mc.player != null) {
            float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
            double renderX = this.previousServerX + (this.serverX - this.previousServerX) * (double)partialTick;
            double renderY = this.previousServerY + (this.serverY - this.previousServerY) * (double)partialTick;
            double renderZ = this.previousServerZ + (this.serverZ - this.previousServerZ) * (double)partialTick;
            this.fakePlayerEntity.xOld = this.renderedX;
            this.fakePlayerEntity.yOld = this.renderedY;
            this.fakePlayerEntity.zOld = this.renderedZ;
            this.fakePlayerEntity.setPos(renderX, renderY, renderZ);
            this.renderedX = renderX;
            this.renderedY = renderY;
            this.renderedZ = renderZ;
            float renderYaw = this.previousServerYaw + (this.serverYaw - this.previousServerYaw) * partialTick;
            float renderPitch = this.previousServerPitch + (this.serverPitch - this.previousServerPitch) * partialTick;
            float renderHeadYaw = this.previousServerHeadYaw + (this.serverHeadYaw - this.previousServerHeadYaw) * partialTick;
            this.fakePlayerEntity.setYRot(renderYaw);
            this.fakePlayerEntity.setXRot(renderPitch);
            this.fakePlayerEntity.setYHeadRot(renderHeadYaw);
            this.fakePlayerEntity.setYBodyRot(renderYaw);
         }
      }
   }

   @Override
   public void onEnable() {
      if (mc.player != null) {
         this.serverX = this.previousServerX = mc.player.getX();
         this.serverY = this.previousServerY = mc.player.getY();
         this.serverZ = this.previousServerZ = mc.player.getZ();
         this.serverYaw = this.previousServerYaw = mc.player.getYRot();
         this.serverPitch = this.previousServerPitch = mc.player.getXRot();
         this.serverHeadYaw = this.previousServerHeadYaw = mc.player.getYHeadRot();
         this.fakePlayerEntity = null;
         if (this.fakePlayer.getValue()) {
            this.fakePlayerEntity = new RemotePlayer(mc.level, new GameProfile(UUID.nameUUIDFromBytes("".getBytes(StandardCharsets.UTF_8)), ""));
            this.fakePlayerEntity.setId(-1337);
            this.fakePlayerEntity.copyPosition(mc.player);
            this.fakePlayerEntity.setYRot(mc.player.getYRot());
            this.fakePlayerEntity.setXRot(mc.player.getXRot());
            this.fakePlayerEntity.setYHeadRot(mc.player.getYHeadRot());
            this.fakePlayerEntity.setHealth(mc.player.getHealth());
            this.fakePlayerEntity.setAbsorptionAmount(mc.player.getAbsorptionAmount());
            mc.level.addEntity(this.fakePlayerEntity);
         }
      }
   }

   @Override
   public void onDisable() {
      if (this.fakePlayerEntity != null && mc.level != null) {
         mc.level.removeEntity(this.fakePlayerEntity.getId(), Entity.RemovalReason.DISCARDED);
         this.fakePlayerEntity = null;
      }

      this.flushAllPackets();
   }

   private void updateServerPosition(ServerboundMovePlayerPacket movePlayerPacket) {
      this.previousServerX = this.serverX;
      this.previousServerY = this.serverY;
      this.previousServerZ = this.serverZ;
      this.serverX = movePlayerPacket.getX(this.serverX);
      this.serverY = movePlayerPacket.getY(this.serverY);
      this.serverZ = movePlayerPacket.getZ(this.serverZ);
      this.previousServerYaw = this.serverYaw;
      this.previousServerPitch = this.serverPitch;
      this.previousServerHeadYaw = this.serverHeadYaw;
      this.serverYaw = movePlayerPacket.getYRot(this.serverYaw);
      this.serverPitch = movePlayerPacket.getXRot(this.serverPitch);
      if (movePlayerPacket.hasRotation()) {
         this.serverHeadYaw = movePlayerPacket.getYRot(this.serverHeadYaw);
      }
   }

   private double countQueuedMovementPackets() {
      double movementPacketCount = 0.0;

      for (Packet packet : this.queuedPackets) {
         if (packet instanceof ServerboundMovePlayerPacket) {
            ++movementPacketCount;
         }
      }

      return movementPacketCount;
   }

   private void flushOneMovement() {
      while (!this.queuedPackets.isEmpty()) {
         Packet packet = (Packet)this.queuedPackets.poll();
         this.sendQueuedPacket(packet);
         if (packet instanceof ServerboundMovePlayerPacket movePlayerPacket) {
            this.updateServerPosition(movePlayerPacket);
            break;
         }
      }
   }

   private void flushAllPackets() {
      Packet packet;
      while ((packet = (Packet)this.queuedPackets.poll()) != null) {
         this.sendQueuedPacket(packet);
         if (packet instanceof ServerboundMovePlayerPacket movePlayerPacket) {
            this.updateServerPosition(movePlayerPacket);
         }
      }
   }

   private void sendQueuedPacket(Packet packet) {
      if (mc.getConnection() != null) {
         this.flushing = true;

         try {
            mc.getConnection().send(packet);
         } finally {
            this.flushing = false;
         }
      }
   }

   public Blink() {
      super(BLINK_LABEL, Category.MOVEMENT);
      this.tick = new NumberSetting(TICK_LABEL, this, 30.0, 5.0, 200.0, 1.0);
      this.slowRelease = new BooleanSetting(SLOW_RELEASE_LABEL, this, true);
      this.slowMove = new BooleanSetting(SLOW_MOVE_LABEL, this, false);
      this.slowMoveTick = new NumberSetting(SLOW_MOVE_TICK_LABEL, this, 5.0, 2.0, 5.0, 1.0);
      this.fakePlayer = new BooleanSetting(FAKE_PLAYER_LABEL, this, false);
      this.queuedPackets = new ConcurrentLinkedQueue<>();
   }
}
