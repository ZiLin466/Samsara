package com.samsara.module.movement;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.BooleanSetting;
import com.samsara.util.TargetFinder;
import java.nio.charset.StandardCharsets;
import net.minecraft.world.entity.Entity;

public class AutoWalk extends Feature {
   private static final String f368 = "Rotate to Entity";
   private static final String f367 = "AutoWalk";
   private BooleanSetting f369 = new BooleanSetting(f368, this, false);

   private void pm$77(Entity var1) {
      double var2 = var1.getX() - mc.player.getX();
      double var4 = var1.getZ() - mc.player.getZ();
      float var6 = (float)Math.toDegrees(Math.atan2(var4, var2)) - 90.0F;
      mc.player.setYRot(var6);
   }

   public AutoWalk() {
      super(f367, Category.MOVEMENT);
   }

   @Override
   public void onEvent(Event var1) {
      if (var1 == Events.f3 && mc.gui.screen() == null) {
         mc.options.keyUp.setDown(true);
         if (this.f369.m215()) {
            Entity var2 = TargetFinder.m50();
            if (var2 != null) {
               this.pm$77(var2);
            }
         }
      }
   }

   @Override
   public void onDisable() {
      mc.options.keyUp.setDown(false);
   }
}
