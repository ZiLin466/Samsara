package com.samsara.module.player;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.ModeSetting;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;

public class NoFall extends Feature {
   private static final String NO_GROUND_LABEL = "NoGround";
   private static final String GROUND_LABEL = "Ground";
   private static final String UNIVERSAL_LABEL = "Universal";
   private static final String NO_FALL_LABEL = "NoFall";
   private static final String MODE_LABEL = "Mode";
   private final ModeSetting mode = new ModeSetting(MODE_LABEL, this, UNIVERSAL_LABEL, new String[]{UNIVERSAL_LABEL, GROUND_LABEL, NO_GROUND_LABEL});

   @Override
   public void onEvent(Event event) {
      if (event == Events.PRE_MOTION) {
         switch (this.mode.getValue()) {
            case GROUND_LABEL -> Events.PRE_MOTION.setOnGround(true);
            case NO_GROUND_LABEL -> Events.PRE_MOTION.setOnGround(false);
            default -> { }
         }
      }

      if (event == Events.ROTATION && mc.player.fallDistance >= 3.0 && this.mode.is(UNIVERSAL_LABEL)) {
         mc.player.fallDistance = 0.0;
         mc.getConnection().send(new Pos(mc.player.getX(), mc.player.getY(), mc.player.getZ(), true, mc.player.horizontalCollision));
      }
   }

   public NoFall() {
      super(NO_FALL_LABEL, Category.PLAYER);
   }

}
