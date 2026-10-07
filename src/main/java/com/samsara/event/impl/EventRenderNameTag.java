package com.samsara.event.impl;

import com.samsara.event.Event;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

public class EventRenderNameTag extends Event {
   private Vec3 position;
   private PoseStack poseStack;
   private int packedLight;
   private int color;
   private Component originalName;
   private boolean discrete;
   private Component name;

   public void setName(Component name) {
      this.name = name;
   }

   public Component getName() {
      return this.name;
   }

   public void setOriginalName(Component originalName) {
      this.originalName = originalName;
   }

   public boolean isDiscrete() {
      return this.discrete;
   }

   public int getPackedLight() {
      return this.packedLight;
   }

   public void setColor(int color) {
      this.color = color;
   }

   public void setPackedLight(int packedLight) {
      this.packedLight = packedLight;
   }

   public void setPosition(Vec3 position) {
      this.position = position;
   }

   public Component getOriginalName() {
      return this.originalName;
   }

   public EventRenderNameTag reset(PoseStack poseStack, Component name, Vec3 position, int packedLight, int color, boolean discrete) {
      this.name = name;
      this.originalName = name;
      this.position = position;
      this.packedLight = packedLight;
      this.color = color;
      this.discrete = discrete;
      this.poseStack = poseStack;
      return this;
   }

   public int getColor() {
      return this.color;
   }

   public Vec3 getPosition() {
      return this.position;
   }

   public void setDiscrete(boolean discrete) {
      this.discrete = discrete;
   }

   public PoseStack getPoseStack() {
      return this.poseStack;
   }
}
