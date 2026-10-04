package com.samsara.util.render;

public class ScreenPosition {
   protected float x;
   protected float y;
   protected float width;
   protected float height;

   public ScreenPosition(float x, float y, float width, float height) {
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
   }

   public ScreenPosition(float x, float y) {
      this.x = x;
      this.y = y;
   }

   public ScreenPosition() {
   }

   public float getX() {
      return this.x;
   }

   public void setX(float x) {
      this.x = x;
   }

   public float getY() {
      return this.y;
   }

   public void setY(float y) {
      this.y = y;
   }

   public float getWidth() {
      return this.width;
   }

   public void setWidth(float width) {
      this.width = width;
   }

   public float getHeight() {
      return this.height;
   }

   public void setHeight(float height) {
      this.height = height;
   }

   public void setDimensions(float x, float y, float width, float height) {
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
   }
}
