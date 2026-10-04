package com.samsara.module;

import java.nio.charset.StandardCharsets;

public enum Category {
   MOVEMENT,
   VISUAL,
   MISC,
   COMBAT,
   PLAYER;
   private static final String f214 = "PLAYER";
   private static final String f213 = "MOVEMENT";
   private static final String f212 = "COMBAT";
   private static final String f216 = "MISC";
   private static final String f215 = "VISUAL";

   public static Category fromName(String var0) {
      return Enum.valueOf(Category.class, var0);
   }

   public static Category[] getAll() {
      return values();
   }
}
