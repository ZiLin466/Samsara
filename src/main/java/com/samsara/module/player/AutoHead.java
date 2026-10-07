package com.samsara.module.player;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.NumberSetting;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

public class AutoHead extends Feature {
   private int previousSlot;
   private final NumberSetting health;
   private static final String AUTO_HEAD_LABEL = "AutoHead";
   private static final String HEALTH_LABEL = "Health";
   private boolean pendingUse;

   public AutoHead() {
      super(AUTO_HEAD_LABEL, Category.PLAYER);
      this.health = new NumberSetting(HEALTH_LABEL, this, 4.0, 1.0, 15.0, 1.0);
      this.previousSlot = -1;
   }

   @Override
   public void onEvent(Event event) {
      if (event == Events.ROTATION) {
         if (this.previousSlot != -1 && !this.pendingUse) {
            mc.player.getInventory().setSelectedSlot(this.previousSlot);
            this.previousSlot = -1;
            return;
         }

         if ((double)mc.player.getHealth() > this.health.getValue()) {
            return;
         }

         for (int slot = 0; slot < 9; slot++) {
            if (mc.player.getInventory().getItem(slot).is(Items.HONEY_BOTTLE)) {
               this.previousSlot = mc.player.getInventory().getSelectedSlot();
               if (!this.pendingUse) {
                  this.pendingUse = true;
                  return;
               }

               mc.player.getInventory().setSelectedSlot(slot);
               mc.getConnection().send(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 0, Events.ROTATION.getYaw(), Events.ROTATION.getPitch()));
               this.pendingUse = false;
            }
         }
      }
   }
}
