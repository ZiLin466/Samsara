package com.samsara.module.misc;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.BooleanSetting;
import com.samsara.setting.NumberSetting;
import java.nio.charset.StandardCharsets;
import mixins.MultiPlayerGameModeAccessor;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

public class WindCharge extends Feature {
   private final BooleanSetting f362;
   private int f364;
   private static final String f358 = "WindCharge";
   private static final String f360 = "Jump";
   private float f363;
   private static final String f359 = "RotationSpeed";
   private boolean f365;
   private final NumberSetting f361 = new NumberSetting(f359, this, 5.0, 1.0, 10.0, 0.1);

   @Override
   public void onDisable() {
      if (this.f364 != -1) {
         mc.player.getInventory().setSelectedSlot(this.f364);
         this.f364 = -1;
      }
   }

   @Override
   public void onEvent(Event var1) {
      if (var1 == Events.f8 && this.f365) {
         mc.player.input.makeJump();
         this.f365 = false;
         this.toggle();
      }

      if (var1 == Events.f3) {
         boolean var2 = false;

         for (int var3 = 0; var3 < 9; var3++) {
            if (mc.player.getInventory().getItem(var3).is(Items.WIND_CHARGE)) {
               if (this.f364 == -1) {
                  this.f364 = mc.player.getInventory().getSelectedSlot();
                  mc.player.getInventory().setSelectedSlot(var3);
               }

               var2 = true;
               break;
            }
         }

         if (!var2) {
            return;
         }

         float var5 = (float)this.f361.m220() * 10.0F;
         float var4 = 90.0F - this.f363;
         this.f363 = this.f363 + Math.copySign(Math.min(Math.abs(var4), var5), var4);
         Events.f3.m83(this.f363);
         if (this.f363 >= 90.0F && mc.player.getMainHandItem().is(Items.WIND_CHARGE)) {
            ((MultiPlayerGameModeAccessor)mc.gameMode)
               .invokeStartPrediction(mc.level, var0 -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, var0, Events.f3.m76(), Events.f3.m82()));
            this.f365 = true;
         }
      }
   }

   public WindCharge() {
      super(f358, Category.MISC);
      this.f362 = new BooleanSetting(f360, this, true);
      this.f364 = -1;
   }

   @Override
   public void onEnable() {
      this.f363 = mc.player.getXRot();
      this.f363 = -1.0F;
   }
}
