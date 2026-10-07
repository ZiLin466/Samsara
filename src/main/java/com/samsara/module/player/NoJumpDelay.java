package com.samsara.module.player;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import mixins.LivingEntityAccessor;

public class NoJumpDelay extends Feature {
   private static final String NO_JUMP_DELAY_LABEL = "NoJumpDelay";

   public NoJumpDelay() {
      super(NO_JUMP_DELAY_LABEL, Category.PLAYER);
   }

   @Override
   public void onEvent(Event event) {
      if (event == Events.MOVE_INPUT) {
         ((LivingEntityAccessor)mc.player).setNoJumpDelay(0);
      }
   }
}
