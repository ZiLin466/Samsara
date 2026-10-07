package com.samsara.util;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;

public class PacketFilter {
   private static int serverSlot;
   private static boolean serverBlocking;

   public static boolean isNotBlocking() {
      return !serverBlocking;
   }

   public static boolean isRedundant(Packet packet) {
      if (packet instanceof ServerboundPlayerActionPacket playerActionPacket && playerActionPacket.getAction() == Action.RELEASE_USE_ITEM) {
         serverBlocking = false;
      }

      if (packet instanceof ServerboundUseItemPacket) {
         ItemStack stack = Minecraft.getInstance().player.getMainHandItem();
         if (stack.is(ItemTags.SWORDS)) {
            serverBlocking = true;
         }
      }

      if (packet instanceof ServerboundSetCarriedItemPacket setCarriedItemPacket) {
         int slot = setCarriedItemPacket.getSlot();
         if (slot == serverSlot) {
            return true;
         }

         serverSlot = slot;
      }

      return false;
   }
}
