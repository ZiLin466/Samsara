package com.samsara.module.combat;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class AntiBot extends Feature {
   private static final String ANTI_BOT_LABEL = "AntiBot";
   private final Set suspectedBots = new HashSet();

   @Override
   public void onEvent(Event event) {
      if (event == Events.ROTATION) {
         this.suspectedBots.clear();
         ClientPacketListener connection = mc.getConnection();
         if (connection == null) {
            return;
         }

         for (PlayerInfo playerInfo : connection.getOnlinePlayers()) {
            this.suspectedBots.add(playerInfo.getProfile().id());
         }
      }
   }

   public boolean isBot(Entity entity) {
      if (!this.isEnabled()) {
         return false;
      } else if (entity instanceof Player player) {
         ClientPacketListener connection = mc.getConnection();
         return connection == null ? true : !this.suspectedBots.contains(player.getUUID());
      } else {
         return true;
      }
   }

   public AntiBot() {
      super(ANTI_BOT_LABEL, Category.COMBAT);
   }
}
