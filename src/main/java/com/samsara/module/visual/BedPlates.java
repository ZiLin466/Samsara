package com.samsara.module.visual;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.event.impl.EventRender2D;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.NumberSetting;
import com.samsara.util.MutableVector3d;
import com.samsara.util.WorldToScreenProjector;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

public class BedPlates extends Feature {
   private static final String RANGE_LABEL = "Range";
   private static final String BED_PLATES_LABEL = "BedPlates";

   private NumberSetting range = new NumberSetting(RANGE_LABEL, this, 16.0, 8.0, 32.0, 1.0);

   private final List<BedEntry> beds;

   private MutableBlockPos searchPosition;

   @Override
   public void onEvent(Event event) {
      if (event == Events.ROTATION) {
         this.beds.clear();
         BlockPos playerPosition = mc.player.blockPosition();
         int playerX = playerPosition.getX();
         int playerY = playerPosition.getY();
         int playerZ = playerPosition.getZ();
         int searchRange = (int)this.range.getValue();
         int rangeSquared = searchRange * searchRange;

         for (int offsetX = -searchRange; offsetX <= searchRange; offsetX++) {
            if (offsetX * offsetX <= rangeSquared) {
               int searchX = playerX + offsetX;

               for (int offsetZ = -searchRange; offsetZ <= searchRange; offsetZ++) {
                  if (offsetX * offsetX + offsetZ * offsetZ <= rangeSquared) {
                     int searchZ = playerZ + offsetZ;

                     for (int offsetY = -4; offsetY <= 4; offsetY++) {
                        int searchY = playerY + offsetY;
                        this.searchPosition.set(searchX, searchY, searchZ);
                        BlockState bedState = mc.level.getBlockState(this.searchPosition);
                        if (bedState.getBlock() instanceof BedBlock && bedState.getValue(BedBlock.PART) == BedPart.FOOT) {
                           this.searchPosition.set(searchX, searchY + 1, searchZ);
                           BlockState coverState = mc.level.getBlockState(this.searchPosition);
                           if (!coverState.isAir()) {
                              ItemStack coverItem = new ItemStack(coverState.getBlock().asItem());
                              if (!coverItem.isEmpty()) {
                                 this.beds.add(new BedEntry(searchX, searchY, searchZ, coverItem));
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      if (event == Events.RENDER_2D) {
         EventRender2D render2DEvent = (EventRender2D)event;
         MutableVector3d scratchPosition = new MutableVector3d();

         for (BedEntry bed : this.beds) {
            MutableVector3d screenPosition = WorldToScreenProjector.project((double)bed.x() + 0.5, (double)bed.y() + 1.5, (double)bed.z() + 0.5, scratchPosition);
            if (screenPosition != null) {
               int screenX = (int)screenPosition.x;
               int screenY = (int)screenPosition.y;
               byte iconHalfWidth = 10;
               render2DEvent.getGraphics().fill(screenX - iconHalfWidth, screenY - iconHalfWidth, screenX + iconHalfWidth, screenY + iconHalfWidth, Integer.MIN_VALUE);
               render2DEvent.getGraphics().item(bed.stack(), screenX - iconHalfWidth + 2, screenY - iconHalfWidth + 2);
            }
         }
      }
   }

   public BedPlates() {
      super(BED_PLATES_LABEL, Category.VISUAL);
      this.searchPosition = new MutableBlockPos();
      this.beds = new ArrayList();
   }

   public static record BedEntry(int x, int y, int z, ItemStack stack) {
   }
}
