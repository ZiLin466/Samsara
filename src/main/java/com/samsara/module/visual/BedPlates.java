package com.samsara.module.visual;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.event.impl.EventRender2D;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.NumberSetting;
import com.samsara.util.Vector3d;
import com.samsara.util.WorldToScreenProjector;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

public class BedPlates extends Feature {
   private MutableBlockPos f609;
   private static final String f607 = "Range";
   private NumberSetting f608 = new NumberSetting(f607, this, 16.0, 8.0, 32.0, 1.0);
   private final List<BedEntry> f610;
   private static final String f606 = "BedPlates";

   @Override
   public void onEvent(Event var1) {
      if (var1 == Events.f3) {
         this.f610.clear();
         BlockPos var2 = mc.player.blockPosition();
         int var3 = var2.getX();
         int var4 = var2.getY();
         int var5 = var2.getZ();
         int var6 = (int)this.f608.m220();
         int var7 = var6 * var6;

         for (int var8 = -var6; var8 <= var6; var8++) {
            if (var8 * var8 <= var7) {
               int var9 = var3 + var8;

               for (int var10 = -var6; var10 <= var6; var10++) {
                  if (var8 * var8 + var10 * var10 <= var7) {
                     int var11 = var5 + var10;

                     for (int var12 = -4; var12 <= 4; var12++) {
                        int var13 = var4 + var12;
                        this.f609.set(var9, var13, var11);
                        BlockState var14 = mc.level.getBlockState(this.f609);
                        if (var14.getBlock() instanceof BedBlock && var14.getValue(BedBlock.PART) == BedPart.FOOT) {
                           this.f609.set(var9, var13 + 1, var11);
                           BlockState var15 = mc.level.getBlockState(this.f609);
                           if (!var15.isAir()) {
                              ItemStack var16 = new ItemStack(var15.getBlock().asItem());
                              if (!var16.isEmpty()) {
                                 this.f610.add(new BedEntry(var9, var13, var11, var16));
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      if (var1 == Events.f5) {
         EventRender2D var17 = (EventRender2D)var1;
         Vector3d var18 = new Vector3d();

         for (BedEntry var20 : this.f610) {
            Vector3d var21 = WorldToScreenProjector.m43((double)var20.x() + 0.5, (double)var20.y() + 1.5, (double)var20.z() + 0.5, var18);
            if (var21 != null) {
               int var22 = (int)var21.x;
               int var23 = (int)var21.y;
               byte var24 = 10;
               var17.m89().fill(var22 - var24, var23 - var24, var22 + var24, var23 + var24, Integer.MIN_VALUE);
               var17.m89().item(var20.stack(), var22 - var24 + 2, var23 - var24 + 2);
            }
         }
      }
   }

   public BedPlates() {
      super(f606, Category.VISUAL);
      this.f609 = new MutableBlockPos();
      this.f610 = new ArrayList();
   }

   public static record BedEntry(int x, int y, int z, ItemStack stack) {
   }
}
