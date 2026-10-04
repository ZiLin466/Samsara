package com.samsara.module.player;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import java.nio.charset.StandardCharsets;
import mixins.LivingEntityAccessor;

public class NoJumpDelay extends Feature {
   private static final String f538 = "NoJumpDelay";

   public NoJumpDelay() {
      super(f538, Category.PLAYER);
   }

   @Override
   public void onEvent(Event var1) {
      if (var1 == Events.f7) {
         ((LivingEntityAccessor)mc.player).setNoJumpDelay(0);
      }
   }
}
