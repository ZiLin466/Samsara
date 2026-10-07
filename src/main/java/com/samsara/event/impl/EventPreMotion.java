package com.samsara.event.impl;

import com.samsara.event.Event;
import com.samsara.event.Events;
import net.minecraft.client.Minecraft;

public class EventPreMotion extends Event {
   private float pitch;
   private double z;
   private double x;
   private float yaw;
   private boolean horizontalCollision;
   private boolean onGround;
   private double y;

   public float getYaw() {
      return this.yaw;
   }

   public boolean isOnGround() {
      return this.onGround;
   }

   public boolean isHorizontalCollision() {
      return this.horizontalCollision;
   }

   public double getX() {
      return this.x;
   }

   public void setZ(double z) {
      this.z = z;
   }

   public void setY(double y) {
      this.y = y;
   }

   public double getZ() {
      return this.z;
   }

   public double getY() {
      return this.y;
   }

   public void setOnGround(boolean onGround) {
      this.onGround = onGround;
   }

   public EventPreMotion reset(double x, double y, double z, boolean onGround, boolean horizontalCollision) {
      if (!Events.ROTATION.usesClientRotation()) {
         this.yaw = Events.ROTATION.getYaw();
         this.pitch = Events.ROTATION.getPitch();
      }

      if (Events.ROTATION.getYaw() != Minecraft.getInstance().player.getYRot()) {
         Minecraft.getInstance().player.yBodyRot = Events.ROTATION.getYaw();
      }

      this.x = x;
      this.y = y;
      this.z = z;
      this.onGround = onGround;
      this.horizontalCollision = horizontalCollision;
      Minecraft.getInstance().player.yHeadRot = Events.ROTATION.getYaw();
      return this;
   }

   public float getPitch() {
      return this.pitch;
   }

   public void setX(double x) {
      this.x = x;
   }

   public void setHorizontalCollision(boolean horizontalCollision) {
      this.horizontalCollision = horizontalCollision;
   }
}
