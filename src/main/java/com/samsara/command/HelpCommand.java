package com.samsara.command;

import com.samsara.SamsaraClient;

public class HelpCommand extends Command {
   private static final String ALIAS_SEPARATOR = ", ";
   private static final String H_LABEL = "h";
   private static final String AVAILABLE_COMMANDS_LABEL = "Available Commands:";
   private static final String QUESTION_ALIAS = "?";
   private static final String PRIMARY_ALIAS = "help";
   private static final String COMMAND_NAME = "Help";

   @Override
   public void execute(String[] arguments, String commandLine) {
      SamsaraClient.sendPrefixedMessage(AVAILABLE_COMMANDS_LABEL);

      for (Command command : CommandManager.getCommands()) {
         String aliases = String.join(ALIAS_SEPARATOR, command.aliases);
         SamsaraClient.sendPrefixedMessage("." + command.syntax + " §7[" + aliases + "]");
      }
   }

   public HelpCommand() {
      super(COMMAND_NAME, PRIMARY_ALIAS, new String[]{PRIMARY_ALIAS, H_LABEL, QUESTION_ALIAS});
   }
}
