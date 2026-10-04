package com.samsara.command;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandManager {
   private static final String f50 = " ";
   private static List<Command> commands = new ArrayList<>();
   private static final String f49 = ".";

   public static Command getCommand(String var0) {
      for (Command var2 : commands) {
         if (var2.name.equalsIgnoreCase(var0)) {
            return var2;
         }

         for (String var6 : var2.aliases) {
            if (var6.equalsIgnoreCase(var0)) {
               return var2;
            }
         }
      }

      return null;
   }

   public static void dispatch(String var0) {
      if (var0.startsWith(f49)) {
         var0 = var0.substring(1);
         String[] var1 = var0.split(f50);
         if (var1.length > 0) {
            String var2 = var1[0];
            int var3 = 0;

            for (int var4 = commands.size(); var3 < var4; var3++) {
               Command var5 = (Command)commands.get(var3);

               for (int var6 = 0; var6 < var5.aliases.length; var6++) {
                  if (var5.aliases[var6].equalsIgnoreCase(var2)) {
                     var5.execute(var1, var0);
                     return;
                  }
               }
            }
         }
      }
   }

   public static List<Command> m7() {
      return commands;
   }

   public static void m6() {
      commands.add(new BindCommand());
      commands.add(new HelpCommand());
      commands.add(new SayCommand());
      commands.add(new ToggleCommand());
      commands.add(new ConfigCommand());
      commands.add(new FriendCommand());
   }

   public static List<String> m9() {
      ArrayList<String> var0 = new ArrayList<>();

      for (Command var2 : commands) {
         var0.addAll(Arrays.asList(var2.aliases));
      }

      return var0;
   }
}
