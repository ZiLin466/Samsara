package com.samsara.event.impl;

import com.samsara.event.Event;

public class EventMoveInput extends Event {
   private boolean sprint;
   private boolean forward;
   private boolean sneak;
   private boolean right;
   private boolean jump;
   private boolean backward;
   private boolean left;

   public boolean isJump() {
      return this.jump;
   }

   public boolean isRight() {
      return this.right;
   }

   public void setJump(boolean jump) {
      this.jump = jump;
   }

   public EventMoveInput reset(boolean forward, boolean backward, boolean left, boolean right, boolean jump, boolean sneak, boolean sprint) {
      this.forward = forward;
      this.backward = backward;
      this.left = left;
      this.right = right;
      this.jump = jump;
      this.sneak = sneak;
      this.sprint = sprint;
      return this;
   }

   public boolean isForward() {
      return this.forward;
   }

   public boolean isBackward() {
      return this.backward;
   }
   public void setBackward(boolean value) { this.backward = value; }

   public boolean isSprint() {
      return this.sprint;
   }

   public boolean isLeft() {
      return this.left;
   }

   public boolean isSneak() {
      return this.sneak;
   }

   public void setRight(boolean right) {
      this.right = right;
   }

   public void setSprint(boolean sprint) {
      this.sprint = sprint;
   }

   public void setSneak(boolean sneak) {
      this.sneak = sneak;
   }

   public void setForward(boolean forward) {
      this.forward = forward;
   }

   public void setBackwardPressed(boolean backward) {
      this.backward = backward;
   }

   public void setLeft(boolean left) {
      this.left = left;
   }
}
