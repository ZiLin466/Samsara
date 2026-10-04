package com.samsara.module.movement;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.module.FeatureManager;
import com.samsara.setting.BooleanSetting;
import com.samsara.setting.ModeSetting;
import com.samsara.setting.NumberSetting;
import com.samsara.util.ColorUtil;
import com.samsara.util.WorldToScreenProjector;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import mixins.MultiPlayerGameModeAccessor;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

public class LongJump extends Feature {
   private static final String f380 = "Mode";
   private NumberSetting f386;
   private double f393;
   private int f389;
   private int f394;
   private ModeSetting f385;
   private static final String f381 = "Fireball";
   private static final String f382 = "Fireball2";
   private int f390;
   public final List<TimedPacket> f21;
   private boolean f391;
   private double f392;
   private static final String f379 = "LongJump";
   private BooleanSetting f387;
   private static final String f383 = "Fireball Delay";
   private int f395;
   private int f388;
   private static final String f384 = "Show Progress";

   private void pm$79() {
      this.f393 = this.f392;
      if (this.f391) {
         this.f390++;
         this.f392 = (double)((float)this.f390 * 50.0F / (float)this.f386.m220());
      }

      if ((double)(this.f390 * 50) > this.f386.m220() || !this.f391) {
         this.f390 = 0;
         this.f392 = 0.0;
         this.f391 = false;
      }

      int var1 = this.f391 ? 255 : 0;
      this.f395 = this.f394;
      this.f394 = (int)Mth.lerp(0.5F, (float)this.f394, (float)var1);
      synchronized (this.f21) {
         Iterator var3 = this.f21.iterator();

         while (var3.hasNext()) {
            TimedPacket var4 = (TimedPacket)var3.next();
            if (var4.m229() || !this.f391) {
               var3.remove();
               mc.execute(() -> var4.f36.handle(mc.getConnection()));
            }
         }
      }
   }

   @Override
   public void onEnable() {
      this.f389 = 0;
      this.f388 = -1;
      this.f390 = 0;
      this.f391 = false;
      this.f393 = 0.0;
      this.f392 = 0.0;
      this.f395 = 0;
      this.f394 = 0;
   }

   @Override
   public void onDisable() {
      synchronized (this.f21) {
         for (TimedPacket var3 : this.f21) {
            mc.execute(() -> var3.f36.handle(mc.getConnection()));
         }

         this.f21.clear();
         this.f391 = false;
      }

      this.f389 = 0;
      this.f388 = -1;
      this.f390 = 0;
   }

   private int pm$80() {
      for (int var1 = 0; var1 < 9; var1++) {
         if (mc.player.getInventory().getItem(var1).is(Items.FIRE_CHARGE)) {
            return var1;
         }
      }

      return -1;
   }

   @Override
   public void onEvent(Event var1) {
      if (var1 == Events.f5 && this.f387.m215()) {
         if (!this.f391 && this.f394 <= 5) {
            this.f393 = 0.0;
         } else {
            float var2 = 100.0F;
            float var3 = 4.0F;
            float var4 = (float)mc.getWindow().getGuiScaledWidth() * 0.5F - var2 * 0.5F;
            float var5 = (float)mc.getWindow().getGuiScaledHeight() * 0.5F + 20.0F;
            float var6 = Events.f5.m91();
            double var7 = this.f393 + (this.f392 - this.f393) * (double)var6;
            int var9 = (int)((float)this.f395 + (float)(this.f394 - this.f395) * var6);
            int var10 = ColorUtil.m27() & 16777215 | var9 << 24;
            int var11 = var9 << 24;
            int var12 = (int)((float)var9 * 0.5F) << 24 | 6316128;
            WorldToScreenProjector.m44(Events.f5, (int)var4, (int)var5, (int)var2, (int)var3, var7, var11, var12, var10);
         }
      }

      if (var1 == Events.f3) {
         this.setSuffix(this.f385.m224());
         if (FeatureManager.f29.isEnabled()) {
            FeatureManager.f29.toggle();
         }

         int var17 = this.pm$80();
         if (var17 != -1) {
            Events.f3.m83(90.0F);
            switch (this.f389) {
               case 0:
                  this.f388 = mc.player.getInventory().getSelectedSlot();
                  mc.player.getInventory().setSelectedSlot(var17);
                  break;
               case 1:
                  ((MultiPlayerGameModeAccessor)mc.gameMode)
                     .invokeStartPrediction(mc.level, var0 -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, var0, Events.f3.m76(), Events.f3.m82()));
                  break;
               case 2:
                  if (this.f385.m228(f381)) {
                     mc.player.getInventory().setSelectedSlot(this.f388);
                  }
                  break;
               case 15:
                  if (this.f385.m228(f382)) {
                     ((MultiPlayerGameModeAccessor)mc.gameMode)
                        .invokeStartPrediction(
                           mc.level, var0 -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, var0, Events.f3.m76(), Events.f3.m82())
                        );
                  }
                  break;
               case 16:
                  mc.player.getInventory().setSelectedSlot(this.f388);
            }

            this.f389++;
         } else {
            this.f389++;
         }

         this.pm$79();
         if (mc.player.onGround() && this.f389 > 20) {
            this.toggle();
         }
      }

      if (var1 == Events.f8 && this.f389 == (this.f385.m228(f381) ? 4 : 20)) {
         mc.player.input.makeJump();
      }

      if (var1 == Events.f11) {
         if (Events.f11.m41() instanceof ClientboundSetEntityMotionPacket var18) {
            if (var18.id() == mc.player.getId()) {
               synchronized (this.f21) {
                  this.f21.add(new TimedPacket(var18, this.f385.m228(f381) ? (long)this.f386.m220() : (long)this.f386.m220() + 800L));
                  var1.setCancelled(true);
               }

               this.f391 = true;
            }
         } else if (this.f391) {
            synchronized (this.f21) {
               this.f21.add(new TimedPacket(Events.f11.m41(), this.f385.m228(f381) ? (long)this.f386.m220() : (long)this.f386.m220() + 800L));
               var1.setCancelled(true);
            }
         }
      }
   }

   public LongJump() {
      super(f379, Category.MOVEMENT);
      this.f385 = new ModeSetting(f380, this, f381, new String[]{f381, f382});
      this.f386 = new NumberSetting(f383, this, 200.0, 100.0, 1000.0, 50.0);
      this.f387 = new BooleanSetting(f384, this, false);
      this.f21 = new ArrayList<>();
      this.f388 = -1;
   }

   public static class TimedPacket {
      public final Packet f36;
      private final long f757;

      public TimedPacket(Packet var1, long var2) {
         this.f36 = var1;
         this.f757 = System.currentTimeMillis() + var2;
      }

      public boolean m229() {
         return System.currentTimeMillis() >= this.f757;
      }
   }
}
