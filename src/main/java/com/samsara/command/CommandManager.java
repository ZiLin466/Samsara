package com.samsara.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandManager {
   private static final String ARGUMENT_SEPARATOR = " ";
   private static List<Command> commands = new ArrayList<>();
   private static final String COMMAND_PREFIX = ".";

   public static Command getCommand(String alias) {
      for (Command command : commands) {
         if (command.name.equalsIgnoreCase(alias)) {
            return command;
         }

         for (String registeredAlias : command.aliases) {
            if (registeredAlias.equalsIgnoreCase(alias)) {
               return command;
            }
         }
      }

      return null;
   }

   public static void dispatch(String commandLine) {
      if (commandLine.startsWith(COMMAND_PREFIX)) {
         commandLine = commandLine.substring(1);
         String[] arguments = commandLine.split(ARGUMENT_SEPARATOR);
         if (arguments.length > 0) {
            String commandName = arguments[0];
            int commandIndex = 0;

            for (int size = commands.size(); commandIndex < size; commandIndex++) {
               Command command = (Command)commands.get(commandIndex);

               for (int index = 0; index < command.aliases.length; index++) {
                  if (command.aliases[index].equalsIgnoreCase(commandName)) {
                     command.execute(arguments, commandLine);
                     return;
                  }
               }
            }
         }
      }
   }

   public static List<Command> getCommands() {
      return commands;
   }

   public static void registerCommands() {
      commands.add(new BindCommand());
      commands.add(new HelpCommand());
      commands.add(new SayCommand());
      commands.add(new ToggleCommand());
      commands.add(new ConfigCommand());
      commands.add(new FriendCommand());
   }

   public static List<String> getAliases() {
      ArrayList<String> aliases = new ArrayList<>();

      for (Command command : commands) {
         aliases.addAll(Arrays.asList(command.aliases));
      }

      return aliases;
   }
}
