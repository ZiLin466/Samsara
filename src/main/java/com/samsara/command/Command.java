package com.samsara.command;

import java.util.Collection;
import java.util.List;

public abstract class Command {
   public String syntax;
   public String name;
   public String[] aliases;

   public Command(String name, String syntax, String[] aliases) {
      this.name = name;
      this.syntax = syntax;
      this.aliases = aliases;
   }

   public Collection complete(String[] arguments) {
      return List.of();
   }

   public abstract void execute(String[] arguments, String commandLine);
}
