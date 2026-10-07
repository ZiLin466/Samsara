package com.samsara.test.input;

import com.samsara.util.RotationUtil;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class RotationUtilTest {
   @Test void viewDirectionUsesYawFirstAndPitchSecond() {
      Vec3 forward = RotationUtil.lookVector(0, 0);
      assertEquals(0, forward.x, .0001);
      assertEquals(0, forward.y, .0001);
      assertEquals(1, forward.z, .0001);
      assertEquals(-1, RotationUtil.lookVector(90, 0).x, .0001);
      assertEquals(-1, RotationUtil.lookVector(0, 90).y, .0001);
   }

   @Test void quantizationPreservesThePreviousAngleAndTruncatesBothDirections() {
      assertEquals(20.5f, RotationUtil.quantize(20.74f, 20, .25f));
      assertEquals(19.5f, RotationUtil.quantize(19.26f, 20, .25f));
      assertEquals(20, RotationUtil.quantize(20.24f, 20, .25f));
      assertEquals(20, RotationUtil.quantize(19.76f, 20, .25f));
   }

   @Test void sensitivityUsesMinecraftMouseStepAtTheSupportedRangeBoundaries() {
      assertEquals(.0096f, RotationUtil.mouseSensitivityStep(0), .000001f);
      assertEquals(.15f, RotationUtil.mouseSensitivityStep(.5f), .000001f);
      assertEquals(.6144f, RotationUtil.mouseSensitivityStep(1), .000001f);
   }
}
