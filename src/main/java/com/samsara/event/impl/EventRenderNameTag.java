package com.samsara.event.impl;

import com.samsara.event.Event;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

public class EventRenderNameTag extends Event {
   private Vec3 f87;
   private PoseStack f84;
   private int f88;
   private int f89;
   private Component f86;
   private boolean f90;
   private Component f85;

   public void m30(Component var1) {
      this.f85 = var1;
   }

   public Component m23() {
      return this.f85;
   }

   public void m31(Component var1) {
      this.f86 = var1;
   }

   public boolean m28() {
      return this.f90;
   }

   public int m26() {
      return this.f88;
   }

   public void m34(int var1) {
      this.f89 = var1;
   }

   public void m33(int var1) {
      this.f88 = var1;
   }

   public void m32(Vec3 var1) {
      this.f87 = var1;
   }

   public Component m24() {
      return this.f86;
   }

   public EventRenderNameTag m22(PoseStack var1, Component var2, Vec3 var3, int var4, int var5, boolean var6) {
      this.f85 = var2;
      this.f86 = var2;
      this.f87 = var3;
      this.f88 = var4;
      this.f89 = var5;
      this.f90 = var6;
      this.f84 = var1;
      return this;
   }

   public int m27() {
      return this.f89;
   }

   public Vec3 m25() {
      return this.f87;
   }

   public void m35(boolean var1) {
      this.f90 = var1;
   }

   public PoseStack m29() {
      return this.f84;
   }
}
