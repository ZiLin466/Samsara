package com.samsara.module.player;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.BooleanSetting;
import java.nio.charset.StandardCharsets;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class AutoTool extends Feature {
   private final BooleanSetting f441;
   private static final String f439 = "AutoTool";
   private int f442;
   private static final String f440 = "Swap Back";

   public AutoTool() {
      super(f439, Category.PLAYER);
      this.f441 = new BooleanSetting(f440, this, true);
      this.f442 = -1;
   }

   @Override
   public void onEvent(Event var1) {
      if (var1 == Events.f3) {
         if (!mc.gameMode.isDestroying()) {
            if (this.f441.m215() && this.f442 != -1) {
               mc.player.getInventory().setSelectedSlot(this.f442);
               this.f442 = -1;
            }

            return;
         }

         if (!(mc.hitResult instanceof BlockHitResult var2)) {
            return;
         }

         BlockPos var10 = var2.getBlockPos();
         BlockState var4 = mc.level.getBlockState(var10);
         if (var4.isAir()) {
            return;
         }

         int var5 = -1;
         float var6 = 1.0F;

         for (int var7 = 0; var7 < 9; var7++) {
            ItemStack var8 = mc.player.getInventory().getItem(var7);
            if (!var8.isEmpty()) {
               float var9 = var8.getDestroySpeed(var4);
               if (var9 > var6) {
                  var6 = var9;
                  var5 = var7;
               }
            }
         }

         if (var5 != -1 && var5 != mc.player.getInventory().getSelectedSlot()) {
            if (this.f442 == -1) {
               this.f442 = mc.player.getInventory().getSelectedSlot();
            }

            mc.player.getInventory().setSelectedSlot(var5);
         }
      }
   }
}
