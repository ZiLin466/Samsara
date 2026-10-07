package com.samsara.module.combat;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.ModeSetting;
import com.samsara.util.TimerController;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot;

public class Criticals extends Feature {
   private double movementTimeBalanceMillis;
   private static final String CRITICALS_LABEL = "Criticals";
   private static final String PACKET_LABEL = "Packet";
   private static final String MODE_LABEL = "Mode";
   private ModeSetting mode;
   private static final String TIMER_LABEL = "Timer";
   private long lastMovementTimeMillis;

   private void sendCriticalJumpPackets() {
      double x = mc.player.getX();
      double y = mc.player.getY();
      double z = mc.player.getZ();
      mc.getConnection().send(new Pos(x, y + 0.0625, z, false, mc.player.horizontalCollision));
      mc.getConnection().send(new Pos(x, y, z, false, mc.player.horizontalCollision));
      mc.getConnection().send(new Pos(x, y + 1.0E-6, z, false, mc.player.horizontalCollision));
   }

   @Override
   public void onEnable() {
      this.lastMovementTimeMillis = System.currentTimeMillis();
      this.movementTimeBalanceMillis = 0.0;
   }

   @Override
   public void onEvent(Event event) {
      if (event == Events.PACKET_SEND) {
         if (Events.PACKET_SEND.getPacket() instanceof ServerboundAttackPacket attackPacket) {
            if (mc.player == null) {
               return;
            }

            if (!mc.player.onGround()) {
               return;
            }

            if (mc.player.isInWater() || mc.player.isInLava()) {
               return;
            }

            if (this.mode.is(PACKET_LABEL)) {
               this.sendCriticalJumpPackets();
            }
         }

         if (Events.PACKET_SEND.getPacket() instanceof ServerboundMovePlayerPacket
            || Events.PACKET_SEND.getPacket() instanceof Pos
            || Events.PACKET_SEND.getPacket() instanceof Rot
            || Events.PACKET_SEND.getPacket() instanceof PosRot) {
            if (!event.isCancelled()) {
               this.movementTimeBalanceMillis -= 50.0;
            }

            this.movementTimeBalanceMillis = this.movementTimeBalanceMillis + (double)(System.currentTimeMillis() - this.lastMovementTimeMillis);
            this.lastMovementTimeMillis = System.currentTimeMillis();
         }
      }

      if (event == Events.ROTATION) {
         if (System.currentTimeMillis() - this.lastMovementTimeMillis > 200L) {
            this.lastMovementTimeMillis = System.currentTimeMillis();
            this.movementTimeBalanceMillis = 0.0;
         }

         double verticalMotion = mc.player.getDeltaMovement().y;
         if (verticalMotion > 0.0) {
            TimerController.setMultiplier(2.5F);
         } else if (this.movementTimeBalanceMillis < 0.0) {
            TimerController.setMultiplier(0.5F);
         } else {
            TimerController.setMultiplier(1.0F);
         }
      }
   }

   public Criticals() {
      super(CRITICALS_LABEL, Category.COMBAT);
      this.mode = new ModeSetting(MODE_LABEL, this, PACKET_LABEL, new String[]{PACKET_LABEL, TIMER_LABEL});
   }
}
