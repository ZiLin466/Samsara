package com.samsara.test.render;

import com.samsara.util.ClientColors;
import com.samsara.util.render.ColorUtility;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ColorUtilityTest {
   @Test void replacingOpacityAndScalingMaterialOpacityHaveDifferentSemantics() {
      assertEquals(0x7F123456, ColorUtility.applyOpacity(0x40123456, .5f));
      assertEquals(0x20123456, ColorUtility.multiplyOpacity(0x40123456, .5f));
      assertEquals(0x40123456, ColorUtility.multiplyOpacity(0x40123456, 1));
      assertEquals(0x00123456, ColorUtility.multiplyOpacity(0x40123456, 0));
   }

   @Test void opacityPreservesEachComponentAndExistingRoundingRules() {
      assertEquals(0x00123456, ColorUtility.multiplyOpacity(0x01123456, .5f));
      assertEquals(0x01123456, ColorUtility.multiplyOpacityRounded(0x01123456, .5f));
      assertEquals(0xFF123456, ColorUtility.applyOpacity(0x40123456, 2));
      assertEquals(0x00123456, ColorUtility.applyOpacity(0x40123456, -1));
   }

   @Test void colorTransitionsKeepAlphaAndUseTheExistingTruncatedMidpoint() {
      assertEquals(0x7F7F7F7F, ColorUtility.mix(0, 0xFFFFFFFF, .5f));
      assertEquals(0x12345678, ColorUtility.mix(0x12345678, 0xABCDEF01, -1));
      assertEquals(0xABCDEF01, ColorUtility.mix(0x12345678, 0xABCDEF01, 2));
      assertEquals(0x18304858, ColorUtility.interpolateColors(0x10203040, 0x20406070, .5f));
   }

   @Test void healthThresholdsRetainTheirCurrentColorsIncludingUnknownHealth() {
      assertEquals(0xFFFF5555, ClientColors.healthColor(.29f));
      assertEquals(0xFFFFAA00, ClientColors.healthColor(.3f));
      assertEquals(0xFFFFFF55, ClientColors.healthColor(.5f));
      assertEquals(0xFF55FF55, ClientColors.healthColor(.71f));
      assertEquals(0xFF55FF55, ClientColors.healthColor(Float.NaN));
   }
}
