package com.samsara.module.combat;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.ModeSetting;
import com.samsara.setting.NumberSetting;
import mixins.ClientInputAccessor;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;

public class SprintReset extends Feature {
   private static final String W_TAP_MODE = "WTap";
   private static final String MODE_LABEL = "Mode";
   private static final String DEFAULT_MODE = "WTap";
   private static final String RESET_DELAY_LABEL = "Reset Delay";
   private static final String SPRINT_RESET_LABEL = "SprintReset";
   private static final String RELEASE_DELAY_LABEL = "Release Delay";
   private static final String S_TAP_LABEL = "STap";

   private final NumberSetting releaseDelay;
   private final NumberSetting resetDelay;
   private final ModeSetting mode;

   private int resetTicksRemaining;
   private int releaseTicksRemaining;

   @Override
   public void onEvent(Event event) {
      if (event == Events.ROTATION) {
         this.setSuffix(this.mode.getValue());
      }

      if (event == Events.PACKET_SEND && Events.PACKET_SEND.getPacket() instanceof ServerboundAttackPacket attackPacket) {
         Entity entity = mc.level.getEntity(attackPacket.entityId());
         if (entity != null && mc.player.isSprinting() && this.resetTicksRemaining <= 0) {
            this.releaseTicksRemaining = (int)this.releaseDelay.getValue();
            this.resetTicksRemaining = (int)this.resetDelay.getValue();
         }
      }

      if (event == Events.POST_MOVE_INPUT) {
         if (this.resetTicksRemaining > 0) {
            this.resetTicksRemaining--;
         }

         if (this.releaseTicksRemaining > 0) {
            this.releaseTicksRemaining--;
            Input input = mc.player.input.keyPresses;
            ClientInputAccessor inputAccessor = (ClientInputAccessor)mc.player.input;
            boolean backwardTap = this.mode.is(S_TAP_LABEL);
            Vec2 movement = new Vec2(0.0F, backwardTap ? -1.0F : 0.0F);
            if (movement.length() > 1.0F) {
               movement = movement.normalized();
            }

            inputAccessor.setMoveVector(movement);
            mc.player.input.keyPresses = new Input(false, backwardTap, false, false, input.jump(), input.shift(), input.sprint());
         } else if (this.releaseTicksRemaining == 0) {
            this.releaseTicksRemaining--;
         }
      }
   }

   public SprintReset() {
      super(SPRINT_RESET_LABEL, Category.COMBAT);
      this.releaseDelay = new NumberSetting(RELEASE_DELAY_LABEL, this, 2.0, 1.0, 5.0, 1.0);
      this.resetDelay = new NumberSetting(RESET_DELAY_LABEL, this, 0.0, 0.0, 10.0, 1.0);
      this.mode = new ModeSetting(MODE_LABEL, this, DEFAULT_MODE, new String[]{W_TAP_MODE, S_TAP_LABEL});
   }
}
