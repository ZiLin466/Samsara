package com.samsara.module.player;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.BooleanSetting;
import com.samsara.setting.NumberSetting;
import java.nio.charset.StandardCharsets;
import mixins.MultiPlayerGameModeAccessor;

public class FastMine extends Feature {
   private final BooleanSetting f499;
   private final NumberSetting f498;
   private static final String f497 = "Remove Delay";
   private static final String f495 = "FastMine";
   private static final String f496 = "Speed";

   @Override
   public void onEvent(Event var1) {
      if (var1 == Events.f3) {
         if (mc.gameMode == null) {
            return;
         }

         MultiPlayerGameModeAccessor var2 = (MultiPlayerGameModeAccessor)mc.gameMode;
         if (this.f499.m215()) {
            var2.setDestroyDelay(0);
         }

         float var3 = var2.getDestroyProgress();
         if (var3 > 0.0F && var3 < 0.99F) {
            var2.setDestroyProgress((float)((double)var3 * this.f498.m220()));
         }
      }
   }

   public FastMine() {
      super(f495, Category.PLAYER);
      this.f498 = new NumberSetting(f496, this, 1.5, 1.0, 5.0, 0.1);
      this.f499 = new BooleanSetting(f497, this, true);
   }
}
