package com.samsara.util;

import com.samsara.event.impl.EventRender2D;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class WorldToScreenProjector implements Wrapper {
   private static double f761;
   private static final Matrix4f f759 = new Matrix4f();
   private static int f764;
   private static double f763;
   private static final Vector4f f760 = new Vector4f();
   private static int f765;
   private static double f762;

   public static void m44(EventRender2D var0, int var1, int var2, int var3, int var4, double var5, int var7, int var8, int var9) {
      var0.m89().fill(var1 - 1, var2 - 1, var1 + var3 + 1, var2 + var4 + 1, var7);
      var0.m89().fill(var1, var2, var1 + var3, var2 + var4, var8);
      var0.m89().fill(var1, var2, (int)((double)var1 + (double)var3 * var5), var2 + var4, var9);
   }

   public static void m42() {
      Camera var0 = Wrapper.mc.gameRenderer.mainCamera();
      Vec3 var1 = var0.position();
      f761 = var1.x;
      f762 = var1.y;
      f763 = var1.z;
      f759.set(var0.getViewRotationProjectionMatrix(f759));
      f764 = Wrapper.mc.getWindow().getGuiScaledWidth();
      f765 = Wrapper.mc.getWindow().getGuiScaledHeight();
   }

   public static Vector3d m43(double var0, double var2, double var4, Vector3d var6) {
      f760.set((float)(var0 - f761), (float)(var2 - f762), (float)(var4 - f763), 1.0F);
      f760.mul(f759);
      if (f760.w <= 0.0F) {
         return null;
      } else {
         float var7 = 1.0F / f760.w;
         double var8 = (double)(f760.x * var7);
         double var10 = (double)(f760.y * var7);
         double var12 = (double)(f760.z * var7);
         var6.m230((var8 * 0.5 + 0.5) * (double)f764, (0.5 - var10 * 0.5) * (double)f765, var12);
         return var6;
      }
   }
}
