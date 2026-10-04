package com.samsara.util;

import net.minecraft.world.phys.Vec3;

public final class Vector3d {
   public double y;
   public double x;
   public double z;

   public Vector3d(double var1, double var3, double var5) {
      this.m230(var1, var3, var5);
   }

   public Vec3 m233() {
      return new Vec3(this.x, this.y, this.z);
   }

   public Vector3d m232(Vec3 var1) {
      return this.m230(var1.x, var1.y, var1.z);
   }

   public Vector3d m230(double var1, double var3, double var5) {
      this.x = var1;
      this.y = var3;
      this.z = var5;
      return this;
   }

   public Vector3d() {
   }

   public Vector3d m231(Vector3d var1) {
      return this.m230(var1.x, var1.y, var1.z);
   }
}
