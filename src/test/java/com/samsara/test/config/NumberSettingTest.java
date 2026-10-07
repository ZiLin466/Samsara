package com.samsara.test.config;

import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.NumberSetting;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class NumberSettingTest {
   private static final class Owner extends Feature {
      Owner() { super("Fixture", Category.PLAYER); }
   }

   @Test void repeatedSliderUpdatesKeepDecimalStepsAndRangeBoundaries() {
      var setting = new NumberSetting("Amount", new Owner(), 1, 0, 10, .05);
      setting.setValue(1.23);
      assertEquals(1.25, setting.getValue());
      setting.setValue(1.27);
      assertEquals(1.25, setting.getValue());
      setting.setValue(12);
      assertEquals(10, setting.getValue());
      setting.setValue(-1);
      assertEquals(0, setting.getValue());
      setting.setValue(.15);
      assertEquals(.15, setting.getValue());
   }

   @Test void roundingRetainsNegativeValuesAndHalfStepBehavior() {
      var setting = new NumberSetting("Amount", new Owner(), 0, -10, 10, .5);
      setting.setValue(-1.25);
      assertEquals(-1, setting.getValue());
      setting.setValue(1.25);
      assertEquals(1.5, setting.getValue());
   }
}
