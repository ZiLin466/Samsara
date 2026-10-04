package com.samsara.module.misc;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import java.nio.charset.StandardCharsets;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.world.phys.Vec3;

public class Whitelist extends Feature {
   public boolean f19;
   private static final String f355 = "Protect your bed and destroy the enemy beds.";
   private static final String f354 = "Whitelist";
   private static final String f356 = "whitelist";
   private boolean f357;
   public Vec3 f20;

   public Whitelist() {
      super(f354, Category.MISC);
   }

   @Override
   public void onEvent(Event var1) {
      if (var1 == Events.f11) {
         if (Events.f11.m41() instanceof ClientboundDisconnectPacket) {
            this.f19 = false;
            this.f20 = null;
         }

         if (Events.f11.m41() instanceof ClientboundSystemChatPacket var2) {
            String var5 = var2.content().getString();
            if (var5.contains(f355)) {
               this.f357 = true;
            }
         }

         if (Events.f11.m41() instanceof ClientboundPlayerPositionPacket var4 && this.f357) {
            this.f19 = true;
            this.f357 = false;
            System.out.println(f356);
            this.f20 = var4.change().position();
         }
      }
   }
}
