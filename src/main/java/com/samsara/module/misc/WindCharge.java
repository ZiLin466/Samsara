package com.samsara.module.misc;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.BooleanSetting;
import com.samsara.setting.NumberSetting;
import mixins.MultiPlayerGameModeAccessor;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

public class WindCharge extends Feature {
   private static final String WIND_CHARGE_LABEL = "WindCharge";
   private static final String JUMP_LABEL = "Jump";
   private static final String ROTATION_SPEED_LABEL = "RotationSpeed";

   private final NumberSetting rotationSpeed = new NumberSetting(ROTATION_SPEED_LABEL, this, 5.0, 1.0, 10.0, 0.1);
   private final BooleanSetting jump;

   private int previousSlot;
   private float aimPitch;
   private boolean pendingJump;

   @Override
   public void onDisable() {
      if (this.previousSlot != -1) {
         mc.player.getInventory().setSelectedSlot(this.previousSlot);
         this.previousSlot = -1;
      }
   }

   @Override
   public void onEvent(Event event) {
      if (event == Events.POST_MOVE_INPUT && this.pendingJump) {
         mc.player.input.makeJump();
         this.pendingJump = false;
         this.toggle();
      }

      if (event == Events.ROTATION) {
         boolean hasWindCharge = false;

         for (int slot = 0; slot < 9; slot++) {
            if (mc.player.getInventory().getItem(slot).is(Items.WIND_CHARGE)) {
               if (this.previousSlot == -1) {
                  this.previousSlot = mc.player.getInventory().getSelectedSlot();
                  mc.player.getInventory().setSelectedSlot(slot);
               }

               hasWindCharge = true;
               break;
            }
         }

         if (!hasWindCharge) {
            return;
         }

         float rotationStep = (float)this.rotationSpeed.getValue() * 10.0F;
         float pitchDelta = 90.0F - this.aimPitch;
         this.aimPitch = this.aimPitch + Math.copySign(Math.min(Math.abs(pitchDelta), rotationStep), pitchDelta);
         Events.ROTATION.setPitch(this.aimPitch);
         if (this.aimPitch >= 90.0F && mc.player.getMainHandItem().is(Items.WIND_CHARGE)) {
            ((MultiPlayerGameModeAccessor)mc.gameMode)
               .invokeStartPrediction(mc.level, sequence -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, sequence, Events.ROTATION.getYaw(), Events.ROTATION.getPitch()));
            this.pendingJump = true;
         }
      }
   }

   public WindCharge() {
      super(WIND_CHARGE_LABEL, Category.MISC);
      this.jump = new BooleanSetting(JUMP_LABEL, this, true);
      this.previousSlot = -1;
   }

   @Override
   public void onEnable() {
      this.aimPitch = mc.player.getXRot();
      this.aimPitch = -1.0F;
   }
}
