package com.samsara.command;

import java.util.Collection;
import java.util.List;

public abstract class Command {
   public String syntax;
   public String name;
   public String[] aliases;

   public Command(String var1, String var2, String[] var3) {
      this.name = var1;
      this.syntax = var2;
      this.aliases = var3;
   }

   public Collection complete(String[] var1) {
      return List.of();
   }

   public abstract void execute(String[] var1, String var2);
}
