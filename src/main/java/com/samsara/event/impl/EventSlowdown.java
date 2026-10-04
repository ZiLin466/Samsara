package com.samsara.event.impl;

import com.samsara.event.Event;

public class EventSlowdown extends Event {
   private float f118;

   public EventSlowdown m92(float var1) {
      this.f118 = var1;
      return this;
   }

   public float m93() {
      return this.f118;
   }

   public void m94(float var1) {
      this.f118 = var1;
   }
}
