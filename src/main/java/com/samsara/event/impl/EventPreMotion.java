package com.samsara.event.impl;

import com.samsara.event.Event;
import com.samsara.event.Events;
import net.minecraft.client.Minecraft;

public class EventPreMotion extends Event {
   private float f107;
   private double f105;
   private double f103;
   private float f106;
   private boolean f109;
   private boolean f108;
   private double f104;

   public float m69() {
      return this.f106;
   }

   public boolean m71() {
      return this.f108;
   }

   public boolean m73() {
      return this.f109;
   }

   public double m63() {
      return this.f103;
   }

   public void m68(double var1) {
      this.f105 = var1;
   }

   public void m66(double var1) {
      this.f104 = var1;
   }

   public double m67() {
      return this.f105;
   }

   public double m65() {
      return this.f104;
   }

   public void m72(boolean var1) {
      this.f108 = var1;
   }

   public EventPreMotion m62(double var1, double var3, double var5, boolean var7, boolean var8) {
      if (!Events.f3.m86()) {
         this.f106 = Events.f3.m76();
         this.f107 = Events.f3.m82();
      }

      if (Events.f3.m76() != Minecraft.getInstance().player.getYRot()) {
         Minecraft.getInstance().player.yBodyRot = Events.f3.m76();
      }

      this.f103 = var1;
      this.f104 = var3;
      this.f105 = var5;
      this.f108 = var7;
      this.f109 = var8;
      Minecraft.getInstance().player.yHeadRot = Events.f3.m76();
      return this;
   }

   public float m70() {
      return this.f107;
   }

   public void m64(double var1) {
      this.f103 = var1;
   }

   public void m74(boolean var1) {
      this.f109 = var1;
   }
}
