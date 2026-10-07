package com.samsara.command;

import com.samsara.SamsaraClient;
import net.minecraft.client.Minecraft;

public class SayCommand extends Command {
   private static final String COMMAND_NAME = "Say";
   private static final String S_LABEL = "s";
   private static final String SAY_MESSAGE_LABEL = "say <message>";
   private static final String PRIMARY_ALIAS = "say";

   public SayCommand() {
      super(COMMAND_NAME, SAY_MESSAGE_LABEL, new String[]{PRIMARY_ALIAS, S_LABEL});
   }

   @Override
   public void execute(String[] arguments, String commandLine) {
      if (arguments.length > 1) {
         String message = commandLine.substring(commandLine.indexOf(32) + 1);
         Minecraft.getInstance().getConnection().sendChat(message);
      } else {
         SamsaraClient.sendPrefixedMessage("Usage: ." + this.syntax);
      }
   }
}
