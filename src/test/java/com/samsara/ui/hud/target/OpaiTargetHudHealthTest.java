package com.samsara.ui.hud.target;

import com.samsara.module.visual.Hud.TargetHud.OpaiTargetHudHealth;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class OpaiTargetHudHealthTest {
   @Test void damageLeavesATrailAndTheNumberFollowsTheFill() {
      var state = new OpaiTargetHudHealth();
      state.reset(20, 20, 0);
      state.update(8, 20, 100);
      var moving = state.sample(200);
      assertTrue(moving.health() > 8 && moving.health() < 20);
      assertTrue(moving.trail() > moving.health());
      assertEquals(20, moving.trail());
      assertNotEquals("20", moving.label());
      assertEquals("8", state.sample(2100).label());
      assertEquals(8, state.sample(2100).trail());
   }

   @Test void repeatedDamageAndHealingRetargetContinuously() {
      var state = new OpaiTargetHudHealth();
      state.reset(20, 20, 0);
      state.update(12, 20, 100);
      var before = state.sample(180);
      var after = state.update(4, 20, 180);
      assertEquals(before.health(), after.health(), .0001);
      assertEquals(before.trail(), after.trail(), .0001);
      var beforeHealing = state.sample(240);
      var healing = state.update(16, 20, 240);
      assertEquals(beforeHealing.health(), healing.health(), .0001);
      assertEquals(healing.health(), healing.trail());
      assertTrue(state.sample(340).health() > healing.health());
      state.reset(7, 40, 350);
      assertEquals(7, state.sample(350).health());
      assertEquals(7, state.sample(350).trail());
   }

   @Test void samplingDoesNotDependOnFrameRate() {
      for (int fps : new int[]{30, 60, 144}) {
         var state = new OpaiTargetHudHealth();
         state.reset(40, 40, 0);
         state.update(5, 40, 100);
         for (int frame = 0; frame < fps; frame++) state.sample(100 + Math.round(frame * 1000.0 / fps));
         var expected = new OpaiTargetHudHealth();
         expected.reset(40, 40, 0);
         expected.update(5, 40, 100);
         assertEquals(expected.sample(1100), state.sample(1100));
      }
   }

   @Test void zeroExtendedHealthAndInvalidDataStayBounded() {
      var state = new OpaiTargetHudHealth();
      state.reset(80, 40, 0);
      assertEquals("40", state.sample(0).label());
      assertEquals(1, state.sample(0).fraction());
      state.update(0, 40, 100);
      assertEquals(0, state.sample(2500).fraction());
      state.reset(Float.NaN, Float.NaN, 3000);
      assertEquals(0, state.sample(3000).health());
      assertTrue(Float.isFinite(state.sample(3000).fraction()));
   }
}
