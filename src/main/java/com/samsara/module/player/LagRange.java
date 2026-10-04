package com.samsara.module.player;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.BooleanSetting;
import com.samsara.setting.NumberSetting;
import com.samsara.util.PacketBlinkQueue;
import com.samsara.util.TargetFinder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class LagRange extends Feature {
   private int f528;
   private static final String f522 = "Blink Ticks";
   private final NumberSetting f525;
   private double f531;
   private static final String f521 = "Range";
   private static final String f523 = "Lag When Close";
   private final NumberSetting f524 = new NumberSetting(f521, this, 6.0, 4.0, 8.0, 0.5);
   private static final String f520 = "LagRange";
   private final BooleanSetting f526;
   private Vec3 f529;
   private boolean f527;
   private String f530;

   private void pm$97() {
      if (this.f527) {
         this.f527 = false;
         PacketBlinkQueue.m23();
      }

      this.f529 = null;
      this.f528 = 0;
   }

   @Override
   public void onEnable() {
   }

   public LagRange() {
      super(f520, Category.PLAYER);
      this.f525 = new NumberSetting(f522, this, 5.0, 2.0, 10.0, 1.0);
      this.f526 = new BooleanSetting(f523, this, false);
      this.f531 = Double.NaN;
   }

   private void pm$98() {
      double var1 = this.f525.m220();
      if (var1 != this.f531) {
         this.f531 = var1;
         this.setSuffix((int)(var1 * 50.0) + " ms");
      }
   }

   @Override
   public void onEvent(Event var1) {
      if (var1 == Events.f10 && Events.f10.m44() instanceof ServerboundAttackPacket var2) {
         if (this.f526.m215() && mc.player.distanceTo(Objects.requireNonNull(mc.level.getEntity(var2.entityId()))) < 2.0F) {
            return;
         }

         this.pm$97();
      }

      if (var1 == Events.f3) {
         this.pm$98();
         LivingEntity var5 = TargetFinder.m47(this.f524.m220(), true);
         if (var5 != null) {
            PacketBlinkQueue.m22();
            this.f527 = true;
            this.f528++;
            Vec3 var6 = var5.position();
            if (this.f529 != null && this.f529.distanceTo(var6) <= mc.player.position().distanceTo(var6)) {
               this.pm$97();
            } else if ((double)this.f528 > this.f525.m220()) {
               this.pm$97();
            }

            Vec3 var4 = mc.player.position();
            this.f529 = new Vec3(var4.x, var4.y, var4.z);
         } else {
            this.pm$97();
         }
      }
   }

   @Override
   public void onDisable() {
      this.pm$97();
   }
}
