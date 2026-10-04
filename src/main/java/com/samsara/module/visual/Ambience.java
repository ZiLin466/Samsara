package com.samsara.module.visual;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.BooleanSetting;
import com.samsara.setting.NumberSetting;
import java.nio.charset.StandardCharsets;
import mixins.ClientClockManagerAccessor;
import mixins.ClockInstanceAccessor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.world.clock.WorldClocks;

public class Ambience extends Feature {
   private static final String f589 = "minecraft:entity.player.attack.crit";
   public final NumberSetting f26;
   private final BooleanSetting f593;
   private static final String f592 = "minecraft:entity.player.attack.sweep";
   private static final String f586 = "Time";
   private static final String f588 = "minecraft:entity.player.attack.weak";
   private static final String f587 = "minecraft:entity.player.attack.nodamage";
   private static final String f585 = "Cancel Hit Sound";
   private int f594;
   private static final String f591 = "minecraft:entity.player.attack.knockback";
   private static final String f584 = "Ambience";
   private ClientLevel f595;
   private static final String f590 = "minecraft:entity.player.attack.strong";

   @Override
   public void onEvent(Event var1) {
      if (var1 == Events.f3) {
         int var2 = (int)this.f26.m220();
         ClientLevel var3 = mc.level;
         if (var2 != this.f594 || var3 != this.f595) {
            ClientClockManagerAccessor var4 = (ClientClockManagerAccessor)var3.clockManager();
            var3.registryAccess().get(WorldClocks.OVERWORLD).ifPresent(var2x -> {
               Object var3x = var4.getClocks().get(var2x);
               if (var3x != null) {
                  ((ClockInstanceAccessor)var3x).setTotalTicks((long)var2);
               }
            });
         }

         this.f594 = var2;
         this.f595 = mc.level;
      }

      if (var1 == Events.f11 && Events.f11.m41() instanceof ClientboundSetTimePacket) {
         var1.setCancelled(true);
      }

      if (var1 == Events.f16 && this.f593.m215()) {
         String var5 = Events.f16.m46().toString();
         System.out.println(var5);
         if (var5.equals(f587) || var5.equals(f588) || var5.equals(f589) || var5.equals(f590) || var5.equals(f591) || var5.equals(f592)) {
            var1.setCancelled(true);
         }
      }
   }

   public Ambience() {
      super(f584, Category.VISUAL);
      this.f593 = new BooleanSetting(f585, this, false);
      this.f26 = new NumberSetting(f586, this, 6000.0, 0.0, 24000.0, 100.0);
   }
}
