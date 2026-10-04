package com.samsara.ui.clickgui.opai;

final class OpaiInteraction {
   enum Action { NONE, DRAG, EXPAND, TOGGLE, SLIDE, SELECT, BIND }

   private OpaiInteraction() {
   }

   static Action header(int button) {
      return button == 0 ? Action.DRAG : button == 1 ? Action.EXPAND : Action.NONE;
   }

   static Action content(OpaiContentLayout.Kind kind, int button) {
      return switch (kind) {
         case MODULE -> button == 0 ? Action.TOGGLE : button == 1 ? Action.EXPAND
            : button == 2 ? Action.BIND : Action.NONE;
         case MODE -> button == 0 ? Action.EXPAND : Action.NONE;
         case BOOLEAN -> button == 0 ? Action.TOGGLE : Action.NONE;
         case NUMBER -> button == 0 ? Action.SLIDE : Action.NONE;
         case OPTION -> button == 0 ? Action.SELECT : Action.NONE;
         case PADDING -> Action.NONE;
      };
   }
}
