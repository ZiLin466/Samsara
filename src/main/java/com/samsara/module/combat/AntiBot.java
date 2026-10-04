package com.samsara.module.combat;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class AntiBot extends Feature {
   private static final String f234 = "AntiBot";
   private final Set f235 = new HashSet();

   @Override
   public void onEvent(Event var1) {
      if (var1 == Events.f3) {
         this.f235.clear();
         ClientPacketListener var2 = mc.getConnection();
         if (var2 == null) {
            return;
         }

         for (PlayerInfo var4 : var2.getOnlinePlayers()) {
            this.f235.add(var4.getProfile().id());
         }
      }
   }

   public boolean m136(Entity var1) {
      if (!this.isEnabled()) {
         return false;
      } else if (var1 instanceof Player var2) {
         ClientPacketListener var3 = mc.getConnection();
         return var3 == null ? true : !this.f235.contains(var2.getUUID());
      } else {
         return true;
      }
   }

   public AntiBot() {
      super(f234, Category.COMBAT);
   }
}
