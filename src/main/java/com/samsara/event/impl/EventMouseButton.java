package com.samsara.event.impl;

import com.samsara.event.Event;

public class EventMouseButton extends Event {
   private int action;
   private int modifiers;
   private int button;

   public EventMouseButton reset(int button, int action, int modifiers) {
      this.button = button;
      this.action = action;
      this.modifiers = modifiers;
      return this;
   }

   public boolean isLeftButton() {
      return this.button == 0;
   }

   public boolean isRightButton() {
      return this.button == 1;
   }

   public boolean isRepeated() {
      return this.action == 2;
   }

   public int getButton() {
      return this.button;
   }

   public int getAction() {
      return this.action;
   }

   public boolean isPressed() {
      return this.action == 1;
   }

   public boolean isMiddleButton() {
      return this.button == 2;
   }

   public boolean isReleased() {
      return this.action == 0;
   }

   public int getModifiers() {
      return this.modifiers;
   }
}
