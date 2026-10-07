package com.samsara.module.misc;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.event.impl.EventPacketSend;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.BooleanSetting;
import mixins.ClientInputAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec2;

public class Disabler extends Feature {
   private static final String WATCHDOG_INV_MOVE_LABEL = "Watchdog InvMove";
   private static final String KEEP_ALIVE_PACKET_LABEL = "KeepAlive Packet";
   private static final String DISABLER_LABEL = "Disabler";
   private static final String SPRINT_PACKET_LABEL = "Sprint Packet";

   private final BooleanSetting keepAlivePacket = new BooleanSetting(KEEP_ALIVE_PACKET_LABEL, this, false);
   private final BooleanSetting sprintPacket;
   private final BooleanSetting watchdogInvMove;

   int ticks;

   @Override
   public void onEvent(Event event) {
      if (event == Events.POST_MOVE_INPUT && this.ticks > 0) {
         mc.player.input.keyPresses = new Input(false, false, false, false, false, false, false);
         ClientInputAccessor inputAccessor = (ClientInputAccessor)mc.player.input;
         inputAccessor.setMoveVector(new Vec2(0.0F, 0.0F));
         this.ticks--;
      }

      if (event instanceof EventPacketSend sending) {
         Packet packet = sending.getPacket();
         if (this.keepAlivePacket.getValue() && packet instanceof ServerboundKeepAlivePacket) {
            event.setCancelled(true);
         }

         if (this.sprintPacket.getValue()
            && packet instanceof ServerboundPlayerCommandPacket playerCommandPacket
            && (playerCommandPacket.getAction() == Action.START_SPRINTING || playerCommandPacket.getAction() == Action.STOP_SPRINTING)) {
            event.setCancelled(true);
         }

         if (this.watchdogInvMove.getValue() && sending.getPacket() instanceof ServerboundPlayerCommandPacket playerCommandPacket && playerCommandPacket.getAction() == Action.OPEN_INVENTORY) {
            event.setCancelled(true);
         }

         if (this.watchdogInvMove.getValue() && packet instanceof ServerboundContainerClickPacket containerClickPacket) {
            Minecraft minecraft = Minecraft.getInstance();
            if (containerClickPacket.containerInput() == ContainerInput.PICKUP && minecraft.getConnection() != null && minecraft.player.containerMenu instanceof InventoryMenu) {
               this.ticks = 5;
            }
         }
      }
   }

   public Disabler() {
      super(DISABLER_LABEL, Category.MISC);
      this.sprintPacket = new BooleanSetting(SPRINT_PACKET_LABEL, this, false);
      this.watchdogInvMove = new BooleanSetting(WATCHDOG_INV_MOVE_LABEL, this, false);
      this.ticks = -1;
   }
}
