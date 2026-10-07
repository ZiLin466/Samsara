package com.samsara.util;

import net.minecraft.client.Minecraft;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class InventoryUtil {
   private static final Minecraft mc = Minecraft.getInstance();

   public static int countHotbarBlocks() {
      int blockCount = 0;

      for (int slot = 0; slot < 9; slot++) {
         ItemStack stack = mc.player.getInventory().getItem(slot);
         if (stack.getItem() instanceof BlockItem) {
            blockCount += stack.getCount();
         }
      }

      return blockCount;
   }

   public static boolean isHoldingSword() {
      return mc.player != null && mc.player.getMainHandItem().is(ItemTags.SWORDS);
   }

   public static boolean isHoldingPlaceableBlock() {
      return mc.player != null && mc.player.getMainHandItem().getItem() instanceof BlockItem;
   }
}
