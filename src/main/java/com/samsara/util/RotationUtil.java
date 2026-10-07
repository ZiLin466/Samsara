package com.samsara.util;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class RotationUtil {
   private static final float RADIANS_PER_DEGREE = (float)(Math.PI / 180.0);

   private RotationUtil() { }

   public static float mouseSensitivityStep(float sensitivity) {
      float factor = sensitivity * 0.6F + 0.2F;
      return factor * factor * factor * 8.0F * 0.15F;
   }

   public static float quantize(float targetAngle, float previousAngle, float sensitivityStep) {
      float delta = targetAngle - previousAngle;
      delta -= delta % sensitivityStep;
      return previousAngle + delta;
   }

   public static Vec3 lookVector(float yaw, float pitch) {
      float yawCos = Mth.cos((double)(-yaw * RADIANS_PER_DEGREE - (float)Math.PI));
      float yawSin = Mth.sin((double)(-yaw * RADIANS_PER_DEGREE - (float)Math.PI));
      float pitchCos = -Mth.cos((double)(-pitch * RADIANS_PER_DEGREE));
      float pitchSin = Mth.sin((double)(-pitch * RADIANS_PER_DEGREE));
      return new Vec3(yawSin * pitchCos, pitchSin, yawCos * pitchCos);
   }
}
