package com.samsara.util;

import net.minecraft.world.phys.Vec3;

public final class MutableVector3d {
   public double y;
   public double x;
   public double z;

   public MutableVector3d(double x, double y, double z) {
      this.set(x, y, z);
   }

   public Vec3 toVec3() {
      return new Vec3(this.x, this.y, this.z);
   }

   public MutableVector3d set(Vec3 source) {
      return this.set(source.x, source.y, source.z);
   }

   public MutableVector3d set(double x, double y, double z) {
      this.x = x;
      this.y = y;
      this.z = z;
      return this;
   }

   public MutableVector3d() {
   }

   public MutableVector3d set(MutableVector3d source) {
      return this.set(source.x, source.y, source.z);
   }
}
