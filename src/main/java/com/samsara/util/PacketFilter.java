package com.samsara.util;

import java.nio.charset.StandardCharsets;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;

public class PacketFilter {
   private static int f741;
   private static int f742;
   private static final String f739 = "bad packet";
   private static int f744;
   private static boolean f740;
   private static int f743;

   public static boolean m21() {
      return !f740;
   }

   public static boolean m20(Packet var0) {
      if (var0 instanceof ServerboundPlayerActionPacket var1 && var1.getAction() == Action.RELEASE_USE_ITEM) {
         f740 = false;
         f742 = Minecraft.getInstance().player.tickCount;
      }

      if (var0 instanceof ServerboundUseItemPacket) {
         ItemStack var3 = Minecraft.getInstance().player.getMainHandItem();
         if (var3.is(ItemTags.SWORDS)) {
            f740 = true;
            f743 = Minecraft.getInstance().player.tickCount;
         }
      }

      if (var0 instanceof ServerboundSetCarriedItemPacket var4) {
         int var2 = var4.getSlot();
         if (var2 == f741) {
            System.out.println(f739);
            return true;
         }

         f741 = var2;
         f744 = Minecraft.getInstance().player.tickCount;
      }

      return false;
   }
}
