package com.samsara.ui.clickgui.opai;

import com.samsara.module.FeatureManager;
import com.samsara.module.combat.TargetSettings;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class OpaiTargetsPanelTest {
   @Test void multiSelectStaysOpenScrollsAndDoesNotChangeTheOtherGroup() {
      FeatureManager.targets = new TargetSettings();
      var panel = new OpaiTargetsPanel();
      panel.update(146, 0); panel.click(15, 26, 152);
      panel.update(146, 0); panel.update(146, 1000);
      assertEquals(146, panel.height(), .01);
      panel.click(15, 50, 152);
      assertFalse(FeatureManager.targets.combat.contains("Players"));
      assertTrue(FeatureManager.targets.visual.contains("Players"));
      panel.click(15, 50, 152);
      assertTrue(FeatureManager.targets.combat.contains("Players"));
      panel.wheel(-10); panel.click(15, 91, 152);
      assertTrue(FeatureManager.targets.combat.contains("Friends"));
      panel.close(); panel.update(146, 1001); panel.update(146, 2000);
      assertEquals(86, panel.height(), .01);
   }
}
