package com.samsara.module.combat;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.module.FeatureManager;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;

public class TriggerBot extends Feature {
   private static final String TRIGGER_BOT_LABEL = "TriggerBot";

   @Override
   public void onEvent(Event event) {
      if (event == Events.ROTATION) {
         if (mc.player == null || mc.level == null || mc.gameMode == null
             || mc.gui.screen() != null || !mc.isWindowActive()) return;
         Entity entity = mc.crosshairPickEntity;
         if (FeatureManager.targets.shouldAttack(entity) && mc.player != null && mc.player.isAlive() && mc.player.getAttackStrengthScale(0.0F) >= 1.0F) {
            mc.gameMode.attack(mc.player, entity);
            mc.player.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, false);
         }
      }
   }

   public TriggerBot() {
      super(TRIGGER_BOT_LABEL, Category.COMBAT);
   }
}
