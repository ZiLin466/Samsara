package com.samsara.event.impl;

import com.samsara.event.Event;

public class EventMouseButton extends Event {
   private int f82;
   private int f83;
   private int f81;

   public EventMouseButton m12(int var1, int var2, int var3) {
      this.f81 = var1;
      this.f82 = var2;
      this.f83 = var3;
      return this;
   }

   public boolean m19() {
      return this.f81 == 0;
   }

   public boolean m20() {
      return this.f81 == 1;
   }

   public boolean m18() {
      return this.f82 == 2;
   }

   public int m13() {
      return this.f81;
   }

   public int m14() {
      return this.f82;
   }

   public boolean m16() {
      return this.f82 == 1;
   }

   public boolean m21() {
      return this.f81 == 2;
   }

   public boolean m17() {
      return this.f82 == 0;
   }

   public int m15() {
      return this.f83;
   }
}
