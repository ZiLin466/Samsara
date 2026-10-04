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
   private static final String f431 = "Blink";
   private static final String f432 = "Tick";
   private static final String f433 = "Slow Release";
   private static final String f434 = "Slow Move";
   private static final String f435 = "Slow Move Tick";
   private static final String f436 = "Fake Player";
   private final NumberSetting f437;
   private final BooleanSetting f438;
   private final BooleanSetting f439;
   private final NumberSetting f440;
   private final BooleanSetting f441;
   private final ConcurrentLinkedQueue<Packet> f442;
   private RemotePlayer f443;
   private boolean f444;
   private double f445;
   private double f446;
   private double f447;
   private double f448;
   private double f449;
   private double f450;
   private float f451;
   private float f452;
   private float f453;
   private float f454;
   private float f455;
   private float f456;
   private double f457;
   private double f458;
   private double f459;

   @Override
   public void onEvent(Event var1) {
      if (var1 == Events.f10) {
         if (this.f444 || mc.player == null) {
            return;
         }

         Packet var2 = Events.f10.m44();
         if (var2 instanceof ServerboundHelloPacket || var2 instanceof ClientIntentionPacket) {
            return;
         }

         if (mc.gui.screen() instanceof RecoverWorldDataScreen) {
            this.setEnabled(false);
            return;
         }

         if (var2 instanceof ServerboundMovePlayerPacket) {
            if (this.pm$155() > this.f437.m220()) {
               if (this.f438.m215()) {
                  this.pm$156();
               } else {
                  this.pm$157();
               }
            }

            if (this.f439.m215() && mc.player.tickCount % (int)this.f440.m220() == 0) {
               this.pm$156();
            }
         }

         var1.setCancelled(true);
         this.f442.add(var2);
      }

      if (var1 == Events.f5) {
         if (this.f443 != null && this.f441.m215() && mc.player != null) {
            float var3 = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
            double var4 = this.f448 + (this.f445 - this.f448) * (double)var3;
            double var6 = this.f449 + (this.f446 - this.f449) * (double)var3;
            double var8 = this.f450 + (this.f447 - this.f450) * (double)var3;
            this.f443.xOld = this.f457;
            this.f443.yOld = this.f458;
            this.f443.zOld = this.f459;
            this.f443.setPos(var4, var6, var8);
            this.f457 = var4;
            this.f458 = var6;
            this.f459 = var8;
            float var10 = this.f454 + (this.f451 - this.f454) * var3;
            float var11 = this.f455 + (this.f452 - this.f455) * var3;
            float var12 = this.f456 + (this.f453 - this.f456) * var3;
            this.f443.setYRot(var10);
            this.f443.setXRot(var11);
            this.f443.setYHeadRot(var12);
            this.f443.setYBodyRot(var10);
         }
      }
   }

   @Override
   public void onEnable() {
      if (mc.player != null) {
         this.f445 = this.f448 = mc.player.getX();
         this.f446 = this.f449 = mc.player.getY();
         this.f447 = this.f450 = mc.player.getZ();
         this.f451 = this.f454 = mc.player.getYRot();
         this.f452 = this.f455 = mc.player.getXRot();
         this.f453 = this.f456 = mc.player.getYHeadRot();
         this.f443 = null;
         if (this.f441.m215()) {
            this.f443 = new RemotePlayer(mc.level, new GameProfile(UUID.nameUUIDFromBytes("".getBytes(StandardCharsets.UTF_8)), ""));
            this.f443.setId(-1337);
            this.f443.copyPosition(mc.player);
            this.f443.setYRot(mc.player.getYRot());
            this.f443.setXRot(mc.player.getXRot());
            this.f443.setYHeadRot(mc.player.getYHeadRot());
            this.f443.setHealth(mc.player.getHealth());
            this.f443.setAbsorptionAmount(mc.player.getAbsorptionAmount());
            mc.level.addEntity(this.f443);
         }
      }
   }

   @Override
   public void onDisable() {
      if (this.f443 != null && mc.level != null) {
         mc.level.removeEntity(this.f443.getId(), Entity.RemovalReason.DISCARDED);
         this.f443 = null;
      }

      this.pm$157();
   }

   private void pm$151(ServerboundMovePlayerPacket var1) {
      this.f448 = this.f445;
      this.f449 = this.f446;
      this.f450 = this.f447;
      this.f445 = var1.getX(this.f445);
      this.f446 = var1.getY(this.f446);
      this.f447 = var1.getZ(this.f447);
      this.f454 = this.f451;
      this.f455 = this.f452;
      this.f456 = this.f453;
      this.f451 = var1.getYRot(this.f451);
      this.f452 = var1.getXRot(this.f452);
      if (var1.hasRotation()) {
         this.f453 = var1.getYRot(this.f453);
      }
   }

   private double pm$155() {
      double var1 = 0.0;

      for (Packet var3 : this.f442) {
         if (var3 instanceof ServerboundMovePlayerPacket) {
            ++var1;
         }
      }

      return var1;
   }

   private void pm$156() {
      while (!this.f442.isEmpty()) {
         Packet var1 = (Packet)this.f442.poll();
         this.pm$158(var1);
         if (var1 instanceof ServerboundMovePlayerPacket var2) {
            this.pm$151(var2);
            break;
         }
      }
   }

   private void pm$157() {
      Packet var1;
      while ((var1 = (Packet)this.f442.poll()) != null) {
         this.pm$158(var1);
         if (var1 instanceof ServerboundMovePlayerPacket var2) {
            this.pm$151(var2);
         }
      }
   }

   private void pm$158(Packet var1) {
      if (mc.getConnection() != null) {
         this.f444 = true;

         try {
            mc.getConnection().send(var1);
         } finally {
            this.f444 = false;
         }
      }
   }

   public Blink() {
      super(f431, Category.MOVEMENT);
      this.f437 = new NumberSetting(f432, this, 30.0, 5.0, 200.0, 1.0);
      this.f438 = new BooleanSetting(f433, this, true);
      this.f439 = new BooleanSetting(f434, this, false);
      this.f440 = new NumberSetting(f435, this, 5.0, 2.0, 5.0, 1.0);
      this.f441 = new BooleanSetting(f436, this, false);
      this.f442 = new ConcurrentLinkedQueue<>();
   }
}
