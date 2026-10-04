package com.samsara.event.impl;

import com.samsara.event.Event;
import net.minecraft.world.entity.Entity;

public class EventEntityOutline extends Event {
   private Entity f91;
   private int f92;

   public EventEntityOutline m36(int var1, Entity var2) {
      this.f92 = var1;
      this.f91 = var2;
      return this;
   }

   public Entity m38() {
      return this.f91;
   }

   public void m39(int var1) {
      this.f92 = var1;
   }

   public int m37() {
      return this.f92;
   }
}
