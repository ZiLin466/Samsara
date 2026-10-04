package com.samsara.event.impl;

import com.samsara.event.Event;
import com.samsara.util.KeybindHandler;

public class EventRotation extends Event {
   private boolean f115;
   private boolean f114;
   private float f110;
   private float f112;
   private float f111;
   private float f113;

   public float m76() {
      return this.f110;
   }

   public EventRotation m75(float var1, float var2) {
      KeybindHandler.m5();
      this.f112 = this.f110;
      this.f113 = this.f111;
      this.f110 = var1;
      this.f111 = var2;
      this.f114 = false;
      this.f115 = true;
      return this;
   }

   public void m80(float var1) {
      this.f113 = var1;
   }

   public boolean m84() {
      return this.f114;
   }

   public float m79() {
      return this.f113;
   }

   public void m83(float var1) {
      this.f111 = var1;
   }

   public float m78() {
      return this.f112;
   }

   public void m87(boolean var1) {
      this.f115 = var1;
   }

   public float m82() {
      return this.f111;
   }

   public void m81(float var1) {
      this.f112 = var1;
   }

   public void m85(boolean var1) {
      this.f114 = var1;
   }

   public void m77(float var1) {
      this.f110 = var1;
   }

   public boolean m86() {
      return this.f115;
   }
}
