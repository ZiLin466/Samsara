package com.samsara.module;


public enum Category {
   MOVEMENT,
   VISUAL,
   MISC,
   COMBAT,
   PLAYER;


   public static Category fromName(String categoryName) {
      return Enum.valueOf(Category.class, categoryName);
   }

   public static Category[] getAll() {
      return values();
   }
}
