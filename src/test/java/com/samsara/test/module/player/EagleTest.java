package com.samsara.test.module.player;

import com.samsara.event.Events;
import com.samsara.event.impl.EventMoveInput;
import com.samsara.module.player.Eagle;
import com.samsara.module.FeatureManager;
import com.samsara.setting.BooleanSetting;
import com.samsara.setting.MultiSelectSetting;
import com.samsara.setting.NumberSetting;
import java.lang.reflect.Method;
import java.util.List;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import static org.junit.jupiter.api.Assertions.*;

final class EagleTest {
   @BeforeAll static void initializeEvents() {
      if (FeatureManager.getModules() == null) FeatureManager.registerModules();
   }
   private static Object invoke(Eagle eagle, String name, Class<?>[] types, Object... args) {
      try {
         Method method = Eagle.class.getDeclaredMethod(name, types);
         method.setAccessible(true);
         return method.invoke(eagle, args);
      } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
   }

   private static boolean edge(Vec3 from, Vec3 to, AABB... boxes) {
      return (boolean)invoke(null, "crossesEdge", new Class<?>[]{Vec3.class, Vec3.class, List.class}, from, to, List.of(boxes));
   }

   private static boolean sneak(Eagle eagle, boolean manual, boolean conditions, boolean active) {
      return (boolean)invoke(eagle, "applySneak", new Class<?>[]{boolean.class, boolean.class, boolean.class}, manual, conditions, active);
   }

   private static MultiSelectSetting conditions(Eagle eagle) {
      return (MultiSelectSetting)eagle.settings.stream().filter(s -> s.getName().equals("Conditions")).findFirst().orElseThrow();
   }

   private static void number(Eagle eagle, String name, double value) {
      ((NumberSetting)eagle.settings.stream().filter(s -> s.getName().equals(name)).findFirst().orElseThrow()).setValue(value);
   }

   private static double distance(Eagle eagle) throws Exception {
      var field = Eagle.class.getDeclaredField("currentEdgeDistance"); field.setAccessible(true);
      return field.getDouble(eagle);
   }

   @Test void edgeDetectionUsesExpandedSupportAndJoinsAdjacentBlockSeams() {
      var block = new AABB(-0.3, -1, -0.3, 1.3, 0.55, 1.3);
      var from = new Vec3(0.5, -0.1, 0.5);
      assertFalse(edge(from, new Vec3(1.2, -0.1, 0.5), block));
      assertTrue(edge(from, new Vec3(1.6, -0.1, 0.5), block));
      assertFalse(edge(from, new Vec3(1.6, -0.1, 0.5), block.move(1, 0, 0), block));
      assertTrue(edge(from, new Vec3(0.8, -0.1, 0.5)));
   }

   @Test void gapsInsideTheSegmentCannotBeHiddenBySupportedEndpoints() {
      var start = new AABB(-1, -1, -1, 0.5, 1, 1);
      var end = new AABB(0.51, -1, -1, 2, 1, 1);
      assertTrue(edge(Vec3.ZERO, new Vec3(1, 0, 0), start, end));
      assertTrue(edge(new Vec3(1, 0, 0), Vec3.ZERO, end, start));
      assertFalse(edge(Vec3.ZERO, new Vec3(1, 0, 0), start, end.move(-0.01, 0, 0)));
   }

   @Test void diagonalMovementAndPartialHeightShapesUseAllThreeAxes() {
      var low = new AABB(-1, -1, -1, 0.6, 0.05, 0.6);
      var high = new AABB(0.4, -1, 0.4, 2, 1, 2);
      assertFalse(edge(new Vec3(0, -0.1, 0), new Vec3(1, -0.1, 1), low, high));
      assertTrue(edge(new Vec3(0, 0.1, 0), new Vec3(1, 0.1, 1), low, high));
      assertTrue(edge(Vec3.ZERO, new Vec3(1, 0, 1), low));
      assertFalse(edge(Vec3.ZERO, Vec3.ZERO));
   }

   @Test void manualSneakIsPreservedWithoutSneakControl() {
      var eagle = new Eagle();
      assertFalse(sneak(eagle, false, true, false));
      assertTrue(sneak(eagle, false, true, true));
      assertTrue(sneak(eagle, true, true, false));
      assertTrue(sneak(eagle, true, false, false));
   }

   @Test void sneakConditionCapturesShiftAtTheEdgeAndReleasesOnSafeGround() {
      var eagle = new Eagle(); conditions(eagle).setSelected(List.of("OnGround", "Sneak"));
      assertTrue(sneak(eagle, true, true, false));
      assertTrue(sneak(eagle, true, true, true));
      assertFalse(sneak(eagle, true, true, false));
      assertTrue(sneak(eagle, true, false, false));
      assertFalse(sneak(eagle, true, true, false));
      assertFalse(sneak(eagle, false, false, false));
      assertTrue(sneak(eagle, true, true, false));
      sneak(eagle, true, true, true);
      eagle.onDisable();
      assertTrue(sneak(eagle, true, true, false));
   }

   @Test void conditionsRequireEverySelectionAndUseCurrentInputAndPitch() {
      var eagle = new Eagle();
      var input = new EventMoveInput().reset(false, true, true, false, false, true, false);
      conditions(eagle).setSelected(List.of("Backwards", "Left", "HoldingBlocks", "OnGround", "Sneak"));
      Class<?>[] signature = {EventMoveInput.class, float.class, boolean.class, boolean.class};
      assertTrue((boolean)invoke(eagle, "conditionsMet", signature, input, 75F, true, true));
      assertFalse((boolean)invoke(eagle, "conditionsMet", signature, input, 75F, false, true));
      assertFalse((boolean)invoke(eagle, "conditionsMet", signature, input, 75F, true, false));
      input.setSneak(false);
      assertFalse((boolean)invoke(eagle, "conditionsMet", signature, input, 75F, true, true));
      input.setSneak(true); number(eagle, "Pitch Min", 70);
      assertFalse((boolean)invoke(eagle, "conditionsMet", signature, input, 65F, true, true));
      ((BooleanSetting)eagle.settings.stream().filter(s -> s.getName().equals("Conditional")).findFirst().orElseThrow()).setValue(false);
      assertTrue((boolean)invoke(eagle, "conditionsMet", signature, input, 0F, false, false));
      assertFalse(conditions(eagle).isVisible());
   }

   @Test void stationaryFallbackRespectsBackwardAndDiagonalInputs() {
      Class<?>[] signature = {EventMoveInput.class, float.class};
      var input = new EventMoveInput().reset(true, false, false, true, false, false, false);
      assertEquals(45F, invoke(null, "movementYaw", signature, input, 0F));
      input.reset(false, true, true, false, false, false, false);
      assertEquals(225F, invoke(null, "movementYaw", signature, input, 0F));
      input.reset(false, false, true, false, false, false, false);
      assertEquals(-90F, invoke(null, "movementYaw", signature, input, 0F));
      assertTrue(new Eagle().getPriority(Events.MOVE_INPUT) > 0);
   }

   @Test void edgeDistanceIsRetainedUntilSneakingFinishesAndSupportsReversedRanges() throws Exception {
      var eagle = new Eagle();
      double first = distance(eagle); assertTrue(first >= 0.4 && first <= 0.6);
      sneak(eagle, false, true, true); sneak(eagle, false, true, true);
      assertEquals(first, distance(eagle));
      number(eagle, "Edge Distance Min", 0.7); number(eagle, "Edge Distance Max", 0.7);
      sneak(eagle, false, true, false); assertEquals(0.7, distance(eagle), 1.0E-9);
      number(eagle, "Edge Distance Min", 0.9); number(eagle, "Edge Distance Max", 0.5);
      eagle.onEnable(); assertTrue(distance(eagle) >= 0.5 && distance(eagle) <= 0.9);
   }

   private static boolean stable(Eagle eagle, boolean permitted, boolean entry, boolean release) {
      return (boolean)invoke(eagle, "updateEdgeState", new Class<?>[]{boolean.class, boolean.class, boolean.class}, permitted, entry, release);
   }

   @Test void sameBlockThresholdNoiseDoesNotRepeatSneakTransitions() {
      var eagle = new Eagle();
      var support = new AABB(-.3, -1, -.3, 1.3, .55, 1.3);
      double[] positions = {.79, .81, .795, .805, .78, .8, .77, .81};
      int transitions = 0;
      boolean last = false;
      for (double x : positions) {
         Vec3 from = new Vec3(x, -.1, .5);
         boolean active = stable(eagle, true, edge(from, from.add(.5, 0, 0), support),
            edge(from, from.add(.68, 0, 0), support));
         if (active != last) transitions++;
         last = active;
      }
      assertEquals(1, transitions);
      assertTrue(last);
      Vec3 safe = new Vec3(.6, -.1, .5);
      assertFalse(stable(eagle, true, edge(safe, safe.add(.5, 0, 0), support), edge(safe, safe.add(.68, 0, 0), support)));
      // Even the next maximum random sample cannot recapture this safe position.
      assertFalse(stable(eagle, true, edge(safe, safe.add(.6, 0, 0), support), edge(safe, safe.add(.68, 0, 0), support)));
   }

   @Test void placingSupportTurningAwayAndLeavingConditionsReleaseTheLatch() {
      var eagle = new Eagle();
      var support = new AABB(-.3, -1, -.3, 1.3, .55, 1.3);
      Vec3 from = new Vec3(.9, -.1, .5);
      assertTrue(stable(eagle, true, true, true));
      assertFalse(stable(eagle, true, edge(from, from.add(.5, 0, 0), support, support.move(1, 0, 0)),
         edge(from, from.add(.68, 0, 0), support, support.move(1, 0, 0))));
      assertTrue(stable(eagle, true, true, true));
      assertFalse(stable(eagle, true, edge(from, from.add(-.5, 0, 0), support), edge(from, from.add(-.68, 0, 0), support)));
      assertTrue(stable(eagle, true, true, true));
      assertFalse(stable(eagle, false, true, true));
      assertFalse(stable(eagle, true, false, true));
      assertTrue(stable(eagle, true, true, true));
      eagle.onDisable();
      assertFalse(stable(eagle, true, false, true));
   }
}
