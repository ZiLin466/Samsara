package com.samsara;

import com.samsara.command.CommandManager;
import com.samsara.config.ConfigManager;
import com.samsara.module.FeatureManager;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class SamsaraClient implements ModInitializer {
   public void onInitialize() {
      FeatureManager.registerModules();
      CommandManager.registerCommands();
      ConfigManager.init();
   }

   public static void sendMessage(String message) {
      Minecraft.getInstance().player.sendSystemMessage(Component.literal(message));
   }

   public static void sendPrefixedMessage(String message) {
      Minecraft.getInstance().player.sendSystemMessage(prefixedMessage(message));
   }

   private static MutableComponent prefixedMessage(String message) {
      MutableComponent output = Component.empty();
      output.append(Component.literal("[").withColor(0x555555));
      String name = ClientBranding.NAME;
      for (int i = 0; i < name.length(); i++) {
         float progress = (float)i / Math.max(1, name.length() - 1);
         int red = Math.round(0x74 + (0xFF - 0x74) * progress);
         int green = Math.round(0xE4 + (0x8A - 0xE4) * progress);
         int blue = Math.round(0xFF + (0xCB - 0xFF) * progress);
         output.append(Component.literal(String.valueOf(name.charAt(i))).withColor(red << 16 | green << 8 | blue));
      }
      output.append(Component.literal("] ").withColor(0x555555));
      output.append(Component.literal(message).withColor(0xFFFFFF));
      return output;
   }
}
