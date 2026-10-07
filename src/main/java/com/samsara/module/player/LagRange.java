package com.samsara.module.player;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.BooleanSetting;
import com.samsara.setting.NumberSetting;
import com.samsara.util.PacketBlinkQueue;
import com.samsara.util.TargetFinder;
import java.util.Objects;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class LagRange extends Feature {
   private static final String BLINK_TICKS_LABEL = "Blink Ticks";
   private static final String RANGE_LABEL = "Range";
   private static final String LAG_WHEN_CLOSE_LABEL = "Lag When Close";
   private static final String LAG_RANGE_LABEL = "LagRange";

   private final NumberSetting range = new NumberSetting(RANGE_LABEL, this, 6.0, 4.0, 8.0, 0.5);
   private final NumberSetting blinkTicks;
   private final BooleanSetting lagWhenClose;

   private int blinkTicksElapsed;
   private double cachedBlinkTicks;
   private Vec3 previousPlayerPosition;
   private boolean blinkActive;


   private void stopBlink() {
      if (this.blinkActive) {
         this.blinkActive = false;
         PacketBlinkQueue.disable();
      }

      this.previousPlayerPosition = null;
      this.blinkTicksElapsed = 0;
   }

   @Override
   public void onEnable() {
   }

   public LagRange() {
      super(LAG_RANGE_LABEL, Category.PLAYER);
      this.blinkTicks = new NumberSetting(BLINK_TICKS_LABEL, this, 5.0, 2.0, 10.0, 1.0);
      this.lagWhenClose = new BooleanSetting(LAG_WHEN_CLOSE_LABEL, this, false);
      this.cachedBlinkTicks = Double.NaN;
   }

   private void updateDelaySuffix() {
      double blinkTicksValue = this.blinkTicks.getValue();
      if (blinkTicksValue != this.cachedBlinkTicks) {
         this.cachedBlinkTicks = blinkTicksValue;
         this.setSuffix((int)(blinkTicksValue * 50.0) + " ms");
      }
   }

   @Override
   public void onEvent(Event event) {
      if (event == Events.PACKET_SEND && Events.PACKET_SEND.getPacket() instanceof ServerboundAttackPacket attackPacket) {
         if (this.lagWhenClose.getValue() && mc.player.distanceTo(Objects.requireNonNull(mc.level.getEntity(attackPacket.entityId()))) < 2.0F) {
            return;
         }

         this.stopBlink();
      }

      if (event == Events.ROTATION) {
         this.updateDelaySuffix();
         LivingEntity target = TargetFinder.nearestTarget(this.range.getValue(), true);
         if (target != null) {
            PacketBlinkQueue.enable();
            this.blinkActive = true;
            this.blinkTicksElapsed++;
            Vec3 targetPosition = target.position();
            if (this.previousPlayerPosition != null && this.previousPlayerPosition.distanceTo(targetPosition) <= mc.player.position().distanceTo(targetPosition)) {
               this.stopBlink();
            } else if ((double)this.blinkTicksElapsed > this.blinkTicks.getValue()) {
               this.stopBlink();
            }

            Vec3 playerPosition = mc.player.position();
            this.previousPlayerPosition = new Vec3(playerPosition.x, playerPosition.y, playerPosition.z);
         } else {
            this.stopBlink();
         }
      }
   }

   @Override
   public void onDisable() {
      this.stopBlink();
   }
}
