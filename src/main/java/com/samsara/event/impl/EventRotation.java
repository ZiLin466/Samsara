package com.samsara.event.impl;

import com.samsara.event.Event;
import com.samsara.util.KeybindHandler;

public class EventRotation extends Event {
   private boolean useClientRotation;
   private boolean movementCorrection;
   private float yaw;
   private float previousYaw;
   private float pitch;
   private float previousPitch;

   public float getYaw() {
      return this.yaw;
   }

   public EventRotation reset(float yaw, float pitch) {
      KeybindHandler.updateKeybinds();
      this.previousYaw = this.yaw;
      this.previousPitch = this.pitch;
      this.yaw = yaw;
      this.pitch = pitch;
      this.movementCorrection = false;
      this.useClientRotation = true;
      return this;
   }

   public void setPreviousPitch(float previousPitch) {
      this.previousPitch = previousPitch;
   }

   public boolean hasMovementCorrection() {
      return this.movementCorrection;
   }

   public float getPreviousPitch() {
      return this.previousPitch;
   }

   public void setPitch(float pitch) {
      this.pitch = pitch;
   }

   public float getPreviousYaw() {
      return this.previousYaw;
   }

   public void setUseClientRotation(boolean useClientRotation) {
      this.useClientRotation = useClientRotation;
   }

   public float getPitch() {
      return this.pitch;
   }

   public void setPreviousYaw(float previousYaw) {
      this.previousYaw = previousYaw;
   }

   public void setMovementCorrection(boolean modified) {
      this.movementCorrection = modified;
   }

   public void setYaw(float yaw) {
      this.yaw = yaw;
   }

   public boolean usesClientRotation() {
      return this.useClientRotation;
   }
}
