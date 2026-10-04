package com.samsara.test.module.visual.animations;

import com.mojang.blaze3d.vertex.PoseStack;
import com.samsara.module.visual.Animations.BlockingAnimation;
import net.minecraft.world.entity.HumanoidArm;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class BlockingAnimationTest {
   @Test void everyStyleProducesFiniteChangingMatricesAndPreservesThePoseStack() {
      var distinct = new java.util.HashSet<String>();
      for (String style : BlockingAnimation.STYLES) {
         for (HumanoidArm arm : HumanoidArm.values()) {
            var start = new PoseStack(); BlockingAnimation.transform(start, arm, .3f, 0, style, .1f, .9f);
            var swing = new PoseStack(); BlockingAnimation.transform(swing, arm, .3f, .4f, style, .1f, .9f);
            float[] matrix = swing.last().pose().get(new float[16]);
            for (float value : matrix) assertTrue(Float.isFinite(value), style);
            assertFalse(start.last().pose().equals(swing.last().pose(), .00001f), style);
            assertTrue(swing.isEmpty());
            if (arm == HumanoidArm.RIGHT) assertTrue(distinct.add(java.util.Arrays.toString(matrix)), style);
         }
      }
   }
}
