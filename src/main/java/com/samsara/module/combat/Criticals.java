package com.samsara.module.combat;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.ModeSetting;
import com.samsara.util.TimerController;
import java.nio.charset.StandardCharsets;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot;

public class Criticals extends Feature {
   private double f252;
   private static final String f247 = "Criticals";
   private static final String f249 = "Packet";
   private static final String f248 = "Mode";
   private ModeSetting f251;
   private static final String f250 = "Timer";
   private long f253;

   private void pm$57() {
      double var1 = mc.player.getX();
      double var3 = mc.player.getY();
      double var5 = mc.player.getZ();
      mc.getConnection().send(new Pos(var1, var3 + 0.0625, var5, false, mc.player.horizontalCollision));
      mc.getConnection().send(new Pos(var1, var3, var5, false, mc.player.horizontalCollision));
      mc.getConnection().send(new Pos(var1, var3 + 1.0E-6, var5, false, mc.player.horizontalCollision));
   }

   @Override
   public void onEnable() {
      this.f253 = System.currentTimeMillis();
      this.f252 = 0.0;
   }

   @Override
   public void onEvent(Event var1) {
      if (var1 == Events.f10) {
         if (Events.f10.m44() instanceof ServerboundAttackPacket var2) {
            if (mc.player == null) {
               return;
            }

            if (!mc.player.onGround()) {
               return;
            }

            if (mc.player.isInWater() || mc.player.isInLava()) {
               return;
            }

            if (this.f251.m228(f249)) {
               this.pm$57();
            }
         }

         if (Events.f10.m44() instanceof ServerboundMovePlayerPacket
            || Events.f10.m44() instanceof Pos
            || Events.f10.m44() instanceof Rot
            || Events.f10.m44() instanceof PosRot) {
            if (!var1.isCancelled()) {
               this.f252 -= 50.0;
            }

            this.f252 = this.f252 + (double)(System.currentTimeMillis() - this.f253);
            this.f253 = System.currentTimeMillis();
         }
      }

      if (var1 == Events.f3) {
         if (System.currentTimeMillis() - this.f253 > 200L) {
            this.f253 = System.currentTimeMillis();
            this.f252 = 0.0;
         }

         double var4 = mc.player.getDeltaMovement().y;
         if (var4 > 0.0) {
            TimerController.setMultiplier(2.5F);
         } else if (this.f252 < 0.0) {
            TimerController.setMultiplier(0.5F);
         } else {
            TimerController.setMultiplier(1.0F);
         }
      }
   }

   public Criticals() {
      super(f247, Category.COMBAT);
      this.f251 = new ModeSetting(f248, this, f249, new String[]{f249, f250});
   }
}
