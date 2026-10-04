package com.samsara.module.combat;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.module.FeatureManager;
import java.nio.charset.StandardCharsets;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;

public class TriggerBot extends Feature {
   private static final String f314 = "TriggerBot";

   @Override
   public void onEvent(Event var1) {
      if (var1 == Events.f3) {
         if (mc.player == null || mc.level == null || mc.gameMode == null
             || mc.gui.screen() != null || !mc.isWindowActive()) return;
         Entity var3 = mc.crosshairPickEntity;
         if (FeatureManager.targets.shouldAttack(var3) && mc.player != null && mc.player.isAlive() && mc.player.getAttackStrengthScale(0.0F) >= 1.0F) {
            mc.gameMode.attack(mc.player, var3);
            mc.player.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, false);
         }
      }
   }

   public TriggerBot() {
      super(f314, Category.COMBAT);
   }
}
