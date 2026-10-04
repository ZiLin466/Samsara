package com.samsara.module.movement;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.BooleanSetting;
import com.samsara.setting.ModeSetting;
import com.samsara.setting.NumberSetting;
import java.nio.charset.StandardCharsets;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;

public class NoSlow extends Feature {
   private static final String f403 = "NoSlow";
   private static final String f410 = "Always";
   private static final String f407 = "NoGround";
   private static final String f413 = "Block";
   private NumberSetting f414;
   private BooleanSetting f416;
   private ModeSetting f419;
   private static final String f412 = "SecondTick";
   private BooleanSetting f417;
   private static final String f404 = "Speed";
   private static final String f409 = "Disabled";
   private static final String f405 = "Force Sprinting";
   private ModeSetting f418;
   private BooleanSetting f415;
   private static final String f411 = "FirstTick";
   private static final String f406 = "Rotate";
   private static final String f408 = "Swap";
   private int f420;

   public NoSlow() {
      super(f403, Category.MOVEMENT);
      this.f414 = new NumberSetting(f404, this, 0.2, 0.2, 1.0, 0.1);
      this.f415 = new BooleanSetting(f405, this, false);
      this.f416 = new BooleanSetting(f406, this, false);
      this.f417 = new BooleanSetting(f407, this, false);
      this.f418 = new ModeSetting(f408, this, f409, new String[]{f409, f410, f411, f412});
      this.f419 = new ModeSetting(f413, this, f409, new String[]{f409, f410, f411, f412});
   }

   @Override
   public void onEvent(Event var1) {
      this.setSuffix(this.f414.m220() + "");
      if (var1 == Events.f9) {
         Events.f9.m94((float)this.f414.m220());
      }

      if (var1 == Events.f12 && this.f415.m215()) {
         Events.f12.m97(1);
         Events.f12.m99(true);
      }

      if (var1 == Events.f3) {
         if (mc.player.isUsingItem()) {
            int var2 = this.f418.m228(f412) ? 2 : 1;
            int var3 = this.f419.m228(f412) ? 2 : 1;
            this.f420++;
            if ((!mc.options.keyJump.isDown() || !mc.player.onGround()) && this.f416.m215()) {
               Events.f3.m77(mc.player.getYRot() + 45.0F);
            }

            if (!this.f418.m228(f409) && (this.f420 == var2 || this.f418.m228(f410))) {
               int var4 = mc.player.getInventory().getSelectedSlot();
               mc.player.connection.send(new ServerboundSetCarriedItemPacket(var4 % 8 + 1));
               mc.player.connection.send(new ServerboundSetCarriedItemPacket(var4));
            }

            if (!this.f419.m228(f409) && (this.f420 == var3 || this.f419.m228(f410))) {
               mc.getConnection().send(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 0, mc.player.getYRot(), mc.player.getXRot()));
            }
         } else {
            this.f420 = 0;
         }
      }

      if (var1 == Events.f1 && mc.player.isUsingItem() && this.f417.m215()) {
         Events.f1.m72(false);
      }
   }
}
