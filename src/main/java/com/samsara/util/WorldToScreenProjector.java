package com.samsara.util;

import com.samsara.event.impl.EventRender2D;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class WorldToScreenProjector implements Wrapper {
   private static final Matrix4f viewProjection = new Matrix4f();
   private static final Vector4f projectedPosition = new Vector4f();

   private static double cameraX;
   private static int screenWidth;
   private static double cameraZ;
   private static int screenHeight;
   private static double cameraY;

   public static void drawProgressBar(EventRender2D render2DEvent, int x, int y, int width, int height, double progress, int borderColor, int backgroundColor, int fillColor) {
      render2DEvent.getGraphics().fill(x - 1, y - 1, x + width + 1, y + height + 1, borderColor);
      render2DEvent.getGraphics().fill(x, y, x + width, y + height, backgroundColor);
      render2DEvent.getGraphics().fill(x, y, (int)((double)x + (double)width * progress), y + height, fillColor);
   }

   public static void updateCamera() {
      Camera camera = Wrapper.mc.gameRenderer.mainCamera();
      Vec3 position = camera.position();
      cameraX = position.x;
      cameraY = position.y;
      cameraZ = position.z;
      viewProjection.set(camera.getViewRotationProjectionMatrix(viewProjection));
      screenWidth = Wrapper.mc.getWindow().getGuiScaledWidth();
      screenHeight = Wrapper.mc.getWindow().getGuiScaledHeight();
   }

   public static MutableVector3d project(double worldX, double worldY, double worldZ, MutableVector3d destination) {
      projectedPosition.set((float)(worldX - cameraX), (float)(worldY - cameraY), (float)(worldZ - cameraZ), 1.0F);
      projectedPosition.mul(viewProjection);
      if (projectedPosition.w <= 0.0F) {
         return null;
      } else {
         float inverseW = 1.0F / projectedPosition.w;
         double normalizedX = (double)(projectedPosition.x * inverseW);
         double normalizedY = (double)(projectedPosition.y * inverseW);
         double z = (double)(projectedPosition.z * inverseW);
         destination.set((normalizedX * 0.5 + 0.5) * (double)screenWidth, (0.5 - normalizedY * 0.5) * (double)screenHeight, z);
         return destination;
      }
   }
}
