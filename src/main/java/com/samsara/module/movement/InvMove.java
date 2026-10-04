package com.samsara.module.movement;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.NumberSetting;
import java.nio.charset.StandardCharsets;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.world.phys.Vec3;

public class InvMove extends Feature {
   private NumberSetting f375;
   private static final String f373 = "InvMove";
   private static final String f374 = "Motion";

   @Override
   public void onEvent(Event var1) {
      if (var1 == Events.f3 && mc.gui.screen() != null && !(mc.gui.screen() instanceof ChatScreen)) {
         KeyMapping.setAll();
         this.pm$78();
      }
   }

   private void pm$78() {
      Vec3 var1 = mc.player.getDeltaMovement();
      mc.player.setDeltaMovement(var1.x * this.f375.m220(), var1.y, var1.z * this.f375.m220());
   }

   public InvMove() {
      super(f373, Category.MOVEMENT);
      this.f375 = new NumberSetting(f374, this, 1.0, 0.1, 1.0, 0.1);
   }
}
