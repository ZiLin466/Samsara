package com.samsara.ui;

public final class MouseButtons {
   private MouseButtons() { }

   /** Converts SDL ids (left=1, middle=2, right=3) to the UI's zero-based ordering. */
   public static int normalize(int button) {
      return switch (button) {
         case 1 -> 0;
         case 3 -> 1;
         case 4 -> 3;
         case 5 -> 4;
         default -> button;
      };
   }
}
