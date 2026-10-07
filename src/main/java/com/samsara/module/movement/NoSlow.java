package com.samsara.module.movement;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.BooleanSetting;
import com.samsara.setting.ModeSetting;
import com.samsara.setting.NumberSetting;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;

public class NoSlow extends Feature {
   private static final String NO_SLOW_LABEL = "NoSlow";
   private static final String ALWAYS_LABEL = "Always";
   private static final String NO_GROUND_LABEL = "NoGround";
   private static final String BLOCK_LABEL = "Block";
   private static final String SECOND_TICK_LABEL = "SecondTick";
   private static final String SPEED_LABEL = "Speed";
   private static final String DISABLED_LABEL = "Disabled";
   private static final String FORCE_SPRINTING_LABEL = "Force Sprinting";
   private static final String FIRST_TICK_LABEL = "FirstTick";
   private static final String ROTATE_LABEL = "Rotate";
   private static final String SWAP_LABEL = "Swap";

   private NumberSetting speed;
   private BooleanSetting forceSprinting;
   private BooleanSetting rotate;
   private BooleanSetting noGround;
   private ModeSetting swap;
   private ModeSetting block;

   private int itemUseTicks;

   public NoSlow() {
      super(NO_SLOW_LABEL, Category.MOVEMENT);
      this.speed = new NumberSetting(SPEED_LABEL, this, 0.2, 0.2, 1.0, 0.1);
      this.forceSprinting = new BooleanSetting(FORCE_SPRINTING_LABEL, this, false);
      this.rotate = new BooleanSetting(ROTATE_LABEL, this, false);
      this.noGround = new BooleanSetting(NO_GROUND_LABEL, this, false);
      this.swap = new ModeSetting(SWAP_LABEL, this, DISABLED_LABEL, new String[]{DISABLED_LABEL, ALWAYS_LABEL, FIRST_TICK_LABEL, SECOND_TICK_LABEL});
      this.block = new ModeSetting(BLOCK_LABEL, this, DISABLED_LABEL, new String[]{DISABLED_LABEL, ALWAYS_LABEL, FIRST_TICK_LABEL, SECOND_TICK_LABEL});
   }

   @Override
   public void onEvent(Event event) {
      this.setSuffix(this.speed.getValue() + "");
      if (event == Events.SLOWDOWN) {
         Events.SLOWDOWN.setSpeedMultiplier((float)this.speed.getValue());
      }

      if (event == Events.SPRINT && this.forceSprinting.getValue()) {
         Events.SPRINT.setSprintTriggerTime(1);
         Events.SPRINT.setSprinting(true);
      }

      if (event == Events.ROTATION) {
         if (mc.player.isUsingItem()) {
            int swapTick = this.swap.is(SECOND_TICK_LABEL) ? 2 : 1;
            int blockTick = this.block.is(SECOND_TICK_LABEL) ? 2 : 1;
            this.itemUseTicks++;
            if ((!mc.options.keyJump.isDown() || !mc.player.onGround()) && this.rotate.getValue()) {
               Events.ROTATION.setYaw(mc.player.getYRot() + 45.0F);
            }

            if (!this.swap.is(DISABLED_LABEL) && (this.itemUseTicks == swapTick || this.swap.is(ALWAYS_LABEL))) {
               int selectedSlot = mc.player.getInventory().getSelectedSlot();
               mc.player.connection.send(new ServerboundSetCarriedItemPacket(selectedSlot % 8 + 1));
               mc.player.connection.send(new ServerboundSetCarriedItemPacket(selectedSlot));
            }

            if (!this.block.is(DISABLED_LABEL) && (this.itemUseTicks == blockTick || this.block.is(ALWAYS_LABEL))) {
               mc.getConnection().send(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 0, mc.player.getYRot(), mc.player.getXRot()));
            }
         } else {
            this.itemUseTicks = 0;
         }
      }

      if (event == Events.PRE_MOTION && mc.player.isUsingItem() && this.noGround.getValue()) {
         Events.PRE_MOTION.setOnGround(false);
      }
   }
}
