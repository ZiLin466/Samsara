package com.samsara.event.impl;

import com.samsara.event.Event;

public class EventMoveInput extends Event {
   private boolean f102;
   private boolean f96;
   private boolean f101;
   private boolean f99;
   private boolean f100;
   private boolean f97;
   private boolean f98;

   public boolean m52() {
      return this.f100;
   }

   public boolean m51() {
      return this.f99;
   }

   public void m59(boolean var1) {
      this.f100 = var1;
   }

   public EventMoveInput m47(boolean var1, boolean var2, boolean var3, boolean var4, boolean var5, boolean var6, boolean var7) {
      this.f96 = var1;
      this.f97 = var2;
      this.f98 = var3;
      this.f99 = var4;
      this.f100 = var5;
      this.f101 = var6;
      this.f102 = var7;
      return this;
   }

   public boolean m48() {
      return this.f96;
   }

   public boolean m49() {
      return this.f97;
   }
   public void setBackward(boolean value) { this.f97 = value; }

   public boolean m54() {
      return this.f102;
   }

   public boolean m50() {
      return this.f98;
   }

   public boolean m53() {
      return this.f101;
   }

   public void m58(boolean var1) {
      this.f99 = var1;
   }

   public void m61(boolean var1) {
      this.f102 = var1;
   }

   public void m60(boolean var1) {
      this.f101 = var1;
   }

   public void m55(boolean var1) {
      this.f96 = var1;
   }

   public void m56(boolean var1) {
      this.f97 = var1;
   }

   public void m57(boolean var1) {
      this.f98 = var1;
   }
}
