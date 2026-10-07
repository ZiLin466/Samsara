package com.samsara.module.movement;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.NumberSetting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.world.phys.Vec3;

public class InventoryMove extends Feature {
   private static final String INV_MOVE_LABEL = "InvMove";
   private static final String MOTION_LABEL = "Motion";

   private NumberSetting motion;

   @Override
   public void onEvent(Event event) {
      if (event == Events.ROTATION && mc.gui.screen() != null && !(mc.gui.screen() instanceof ChatScreen)) {
         KeyMapping.setAll();
         this.scaleHorizontalMotion();
      }
   }

   private void scaleHorizontalMotion() {
      Vec3 motion = mc.player.getDeltaMovement();
      mc.player.setDeltaMovement(motion.x * this.motion.getValue(), motion.y, motion.z * this.motion.getValue());
   }

   public InventoryMove() {
      super(INV_MOVE_LABEL, Category.MOVEMENT);
      this.motion = new NumberSetting(MOTION_LABEL, this, 1.0, 0.1, 1.0, 0.1);
   }
}
