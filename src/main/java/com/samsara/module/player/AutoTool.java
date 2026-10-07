package com.samsara.module.player;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.BooleanSetting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class AutoTool extends Feature {
   private static final String AUTO_TOOL_LABEL = "AutoTool";
   private static final String SWAP_BACK_LABEL = "Swap Back";

   private final BooleanSetting swapBack;

   private int previousSlot;

   public AutoTool() {
      super(AUTO_TOOL_LABEL, Category.PLAYER);
      this.swapBack = new BooleanSetting(SWAP_BACK_LABEL, this, true);
      this.previousSlot = -1;
   }

   @Override
   public void onEvent(Event event) {
      if (event == Events.ROTATION) {
         if (!mc.gameMode.isDestroying()) {
            if (this.swapBack.getValue() && this.previousSlot != -1) {
               mc.player.getInventory().setSelectedSlot(this.previousSlot);
               this.previousSlot = -1;
            }

            return;
         }

         if (!(mc.hitResult instanceof BlockHitResult blockHit)) {
            return;
         }

         BlockPos blockPosition = blockHit.getBlockPos();
         BlockState blockState = mc.level.getBlockState(blockPosition);
         if (blockState.isAir()) {
            return;
         }

         int bestSlot = -1;
         float bestDestroySpeed = 1.0F;

         for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc.player.getInventory().getItem(slot);
            if (!stack.isEmpty()) {
               float destroySpeed = stack.getDestroySpeed(blockState);
               if (destroySpeed > bestDestroySpeed) {
                  bestDestroySpeed = destroySpeed;
                  bestSlot = slot;
               }
            }
         }

         if (bestSlot != -1 && bestSlot != mc.player.getInventory().getSelectedSlot()) {
            if (this.previousSlot == -1) {
               this.previousSlot = mc.player.getInventory().getSelectedSlot();
            }

            mc.player.getInventory().setSelectedSlot(bestSlot);
         }
      }
   }
}
