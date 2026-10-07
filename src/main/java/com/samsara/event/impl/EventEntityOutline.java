package com.samsara.event.impl;

import com.samsara.event.Event;
import net.minecraft.world.entity.Entity;

public class EventEntityOutline extends Event {
   private Entity entity;
   private int color;

   public EventEntityOutline reset(int color, Entity entity) {
      this.color = color;
      this.entity = entity;
      return this;
   }

   public Entity getEntity() {
      return this.entity;
   }

   public void setColor(int color) {
      this.color = color;
   }

   public int getColor() {
      return this.color;
   }
}
