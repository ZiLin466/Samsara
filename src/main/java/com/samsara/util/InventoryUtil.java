package com.samsara.util;

import net.minecraft.client.Minecraft;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class InventoryUtil {
   private static final Minecraft f758 = Minecraft.getInstance();

   public static int m41() {
      int var0 = 0;

      for (int var1 = 0; var1 < 9; var1++) {
         ItemStack var2 = f758.player.getInventory().getItem(var1);
         if (var2.getItem() instanceof BlockItem) {
            var0 += var2.getCount();
         }
      }

      return var0;
   }

   public static boolean m39() {
      if (f758.player == null) {
         return false;
      } else {
         Item var0 = f758.player.getMainHandItem().getItem();
         return f758.player.getMainHandItem().is(ItemTags.SWORDS);
      }
   }

   public static boolean m40() {
      if (f758.player == null) {
         return false;
      } else {
         Item var0 = f758.player.getMainHandItem().getItem();
         return var0 instanceof BlockItem;
      }
   }
}
