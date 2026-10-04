package com.samsara.setting;

public interface ChoiceSetting {
   String[] options();
   String selectionLabel();
   boolean selected(int index);
   void select(int index);
   default boolean multiple() { return false; }
}
