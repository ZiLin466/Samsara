package com.samsara.command;

import com.samsara.SamsaraClient;
import java.nio.charset.StandardCharsets;

public class HelpCommand extends Command {
   private static final String f66 = ", ";
   private static final String f63 = "h";
   private static final String f65 = "Available Commands:";
   private static final String f64 = "?";
   private static final String f62 = "help";
   private static final String f61 = "Help";

   @Override
   public void execute(String[] var1, String var2) {
      SamsaraClient.sendPrefixedMessage(f65);

      for (Command var4 : CommandManager.m7()) {
         String var5 = String.join(f66, var4.aliases);
         SamsaraClient.sendPrefixedMessage("." + var4.syntax + " §7[" + var5 + "]");
      }
   }

   public HelpCommand() {
      super(f61, f62, new String[]{f62, f63, f64});
   }
}
