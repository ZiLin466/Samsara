package com.samsara.event.impl;

import com.samsara.event.Event;

public class EventSprint extends Event {
   private boolean f120;
   private int f119;

   public EventSprint m95(int var1, boolean var2) {
      this.f119 = var1;
      this.f120 = var2;
      return this;
   }

   public boolean m98() {
      return this.f120;
   }

   public int m96() {
      return this.f119;
   }

   public void m99(boolean var1) {
      this.f120 = var1;
   }

   public void m97(int var1) {
      this.f119 = var1;
   }
}
