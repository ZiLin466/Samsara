package com.samsara.module.misc;

import com.samsara.event.Event;
import com.samsara.event.impl.EventPacketReceive;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.world.phys.Vec3;

public class Whitelist extends Feature {
   private static final String PROTECT_YOUR_BED_AND_DESTROY_THE_ENEMY_BEDS_LABEL = "Protect your bed and destroy the enemy beds.";
   private static final String MODULE_NAME = "Whitelist";

   public boolean bedSpawnKnown;
   private boolean awaitingBedSpawnPosition;
   public Vec3 bedSpawnPosition;

   public Whitelist() {
      super(MODULE_NAME, Category.MISC);
   }

   @Override
   public void onEvent(Event event) {
      if (event instanceof EventPacketReceive receiving) {
         if (receiving.getPacket() instanceof ClientboundDisconnectPacket) {
            this.bedSpawnKnown = false;
            this.bedSpawnPosition = null;
         }

         if (receiving.getPacket() instanceof ClientboundSystemChatPacket systemChatPacket) {
            String message = systemChatPacket.content().getString();
            if (message.contains(PROTECT_YOUR_BED_AND_DESTROY_THE_ENEMY_BEDS_LABEL)) {
               this.awaitingBedSpawnPosition = true;
            }
         }

         if (receiving.getPacket() instanceof ClientboundPlayerPositionPacket playerPositionPacket && this.awaitingBedSpawnPosition) {
            this.bedSpawnKnown = true;
            this.awaitingBedSpawnPosition = false;
            this.bedSpawnPosition = playerPositionPacket.change().position();
         }
      }
   }
}
