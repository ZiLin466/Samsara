package com.samsara.command;

import com.samsara.SamsaraClient;
import java.nio.charset.StandardCharsets;
import net.minecraft.client.Minecraft;

public class SayCommand extends Command {
   private static final String f67 = "Say";
   private static final String f70 = "s";
   private static final String f68 = "say <message>";
   private static final String f69 = "say";

   public SayCommand() {
      super(f67, f68, new String[]{f69, f70});
   }

   @Override
   public void execute(String[] var1, String var2) {
      if (var1.length > 1) {
         String var3 = var2.substring(var2.indexOf(32) + 1);
         Minecraft.getInstance().getConnection().sendChat(var3);
      } else {
         SamsaraClient.sendPrefixedMessage("Usage: ." + this.syntax);
      }
   }
}
