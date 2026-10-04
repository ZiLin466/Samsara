package com.samsara.module.player;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.NumberSetting;
import java.nio.charset.StandardCharsets;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

public class AutoHead extends Feature {
   private int f437;
   private final NumberSetting f436;
   private static final String f434 = "AutoHead";
   private static final String f435 = "Health";
   private boolean f438;

   public AutoHead() {
      super(f434, Category.PLAYER);
      this.f436 = new NumberSetting(f435, this, 4.0, 1.0, 15.0, 1.0);
      this.f437 = -1;
   }

   @Override
   public void onEvent(Event var1) {
      if (var1 == Events.f3) {
         if (this.f437 != -1 && !this.f438) {
            mc.player.getInventory().setSelectedSlot(this.f437);
            this.f437 = -1;
            return;
         }

         if ((double)mc.player.getHealth() > this.f436.m220()) {
            return;
         }

         for (int var2 = 0; var2 < 9; var2++) {
            System.out.println(mc.player.getInventory().getItem(var2));
            if (mc.player.getInventory().getItem(var2).is(Items.HONEY_BOTTLE)) {
               this.f437 = mc.player.getInventory().getSelectedSlot();
               if (!this.f438) {
                  this.f438 = true;
                  return;
               }

               mc.player.getInventory().setSelectedSlot(var2);
               mc.getConnection().send(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 0, Events.f3.m76(), Events.f3.m82()));
               this.f438 = false;
            }
         }
      }
   }
}
