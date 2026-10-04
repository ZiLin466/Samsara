package com.samsara.module.player;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.BooleanSetting;
import com.samsara.setting.NumberSetting;
import com.samsara.util.InventoryUtil;
import java.nio.charset.StandardCharsets;
import mixins.MinecraftAccessor;

public class FastPlace extends Feature {
   private static final String f501 = "Delay";
   private static final String f502 = "Blocks Only";
   private static final String f500 = "FastPlace";
   private int f505;
   private BooleanSetting f504;
   private NumberSetting f503 = new NumberSetting(f501, this, 1.0, 0.0, 3.0, 1.0);

   @Override
   public void onEvent(Event var1) {
      if (var1 == Events.f3) {
         if (this.f504.m215() && !InventoryUtil.m40()) {
            return;
         }

         if ((int)this.f503.m220() == 0) {
            ((MinecraftAccessor)mc).setRightClickDelay(0);
         } else {
            if ((double)this.f505 >= this.f503.m220()) {
               ((MinecraftAccessor)mc).setRightClickDelay(0);
               this.f505 = 0;
            }

            this.f505++;
         }
      }
   }

   public FastPlace() {
      super(f500, Category.PLAYER);
      this.f504 = new BooleanSetting(f502, this, true);
   }
}
