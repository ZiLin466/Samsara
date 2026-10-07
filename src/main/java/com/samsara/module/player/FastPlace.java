package com.samsara.module.player;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.BooleanSetting;
import com.samsara.setting.NumberSetting;
import com.samsara.util.InventoryUtil;
import mixins.MinecraftAccessor;

public class FastPlace extends Feature {
   private static final String DELAY_LABEL = "Delay";
   private static final String BLOCKS_ONLY_LABEL = "Blocks Only";
   private static final String FAST_PLACE_LABEL = "FastPlace";

   private NumberSetting delay = new NumberSetting(DELAY_LABEL, this, 1.0, 0.0, 3.0, 1.0);
   private BooleanSetting blocksOnly;

   private int placementTicks;

   @Override
   public void onEvent(Event event) {
      if (event == Events.ROTATION) {
         if (this.blocksOnly.getValue() && !InventoryUtil.isHoldingPlaceableBlock()) {
            return;
         }

         if ((int)this.delay.getValue() == 0) {
            ((MinecraftAccessor)mc).setRightClickDelay(0);
         } else {
            if ((double)this.placementTicks >= this.delay.getValue()) {
               ((MinecraftAccessor)mc).setRightClickDelay(0);
               this.placementTicks = 0;
            }

            this.placementTicks++;
         }
      }
   }

   public FastPlace() {
      super(FAST_PLACE_LABEL, Category.PLAYER);
      this.blocksOnly = new BooleanSetting(BLOCKS_ONLY_LABEL, this, true);
   }
}
