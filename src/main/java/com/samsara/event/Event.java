package com.samsara.event;

public abstract class Event {
   private boolean cancelled;

   public boolean isCancelled() {
      return this.cancelled;
   }

   public void call() {
      Events.dispatch(this);
   }

   public void setCancelled(boolean cancelled) {
      this.cancelled = cancelled;
   }
}
