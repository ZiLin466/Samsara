package com.samsara.event.impl;

import com.samsara.event.Event;

public class EventSprint extends Event {
   private boolean sprinting;
   private int sprintTriggerTime;

   public EventSprint reset(int sprintTriggerTime, boolean sprinting) {
      this.sprintTriggerTime = sprintTriggerTime;
      this.sprinting = sprinting;
      return this;
   }

   public boolean isSprinting() {
      return this.sprinting;
   }

   public int getSprintTriggerTime() {
      return this.sprintTriggerTime;
   }

   public void setSprinting(boolean sprinting) {
      this.sprinting = sprinting;
   }

   public void setSprintTriggerTime(int sprintTriggerTime) {
      this.sprintTriggerTime = sprintTriggerTime;
   }
}
