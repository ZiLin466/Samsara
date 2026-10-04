package com.samsara.test.module.combat;

import com.samsara.module.combat.KillAura;
import com.samsara.setting.BooleanSetting;
import com.samsara.setting.ModeSetting;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.Predicate;
import java.util.function.BiPredicate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class KillAuraPredictionTest {
   private static final MethodHandle SAMPLE = handle("sampleBlockRotation", double[].class,
      double[].class, float.class, float.class, double.class);
   private static final MethodHandle MOVEMENT = handle("sampleBlockMovement", double[].class, double[].class, Vec3.class, double.class);
   private static final MethodHandle MOVING = handle("predictsMovingBlockThreat", boolean.class,
      double.class, Vec3.class, double[].class, double.class, Vec3.class, Vec3.class, AABB.class, double.class, BiPredicate.class);
   private static final MethodHandle PREDICT = method("predictsBlockThreat", double.class, Vec3.class,
      double[].class, double[].class, AABB.class, double.class, Predicate.class);
   private static final MethodHandle BLOCK = method("shouldAutoBlock", boolean.class, boolean.class, boolean.class, boolean.class);

   private static MethodHandle method(String name, Class<?>... parameters) {
      return handle(name, boolean.class, parameters);
   }

   private static MethodHandle handle(String name, Class<?> result, Class<?>... parameters) {
      try {
         return MethodHandles.privateLookupIn(KillAura.class, MethodHandles.lookup())
            .findStatic(KillAura.class, name, MethodType.methodType(result, parameters));
      } catch (ReflectiveOperationException error) { throw new ExceptionInInitializerError(error); }
   }

   private static boolean predictsBlockThreat(double distance, Vec3 eyes, double[] current, double[] previous,
      AABB target, double wallRange, Predicate<Vec3> occluded) {
      try { return (boolean)PREDICT.invokeExact(distance, eyes, current, previous, target, wallRange, occluded); }
      catch (Throwable error) { throw new AssertionError(error); }
   }

   private static boolean shouldAutoBlock(boolean predict, boolean rmb, boolean held, boolean threat) {
      try { return (boolean)BLOCK.invokeExact(predict, rmb, held, threat); }
      catch (Throwable error) { throw new AssertionError(error); }
   }

   private static final Vec3 EYES = new Vec3(0, 1.62, 0);
   private static final AABB PLAYER = new AABB(-0.3, 0, 2.7, 0.3, 1.8, 3.3);
   private static final Predicate<Vec3> CLEAR = hit -> false;

   private static double[] rotation(float yaw, float pitch, int tick) {
      return new double[]{yaw, pitch, tick};
   }

   private static double[] sample(double[] previous, float yaw, float pitch, double now) {
      try { return (double[])SAMPLE.invokeExact(previous, yaw, pitch, now); }
      catch (Throwable error) { throw new AssertionError(error); }
   }

   private static double[] movement(double[] previous, Vec3 position, double now) {
      try { return (double[])MOVEMENT.invokeExact(previous, position, now); }
      catch (Throwable error) { throw new AssertionError(error); }
   }

   private static boolean moving(Vec3 eyes, double[] sample, double now, Vec3 attackerVelocity, Vec3 localVelocity, AABB target) {
      BiPredicate<Vec3, Vec3> clear = (origin, hit) -> false;
      try { return (boolean)MOVING.invokeExact(9.0, eyes, sample, now, attackerVelocity, localVelocity, target, 0.0, clear); }
      catch (Throwable error) { throw new AssertionError(error); }
   }

   @Test void repeatedChecksAndDuplicateHeadPacketsPreserveAFreshTurn() {
      var turn = sample(sample(null, 70, 0, 10), 45, 0, 11);
      assertTrue(moving(EYES, turn, 11, Vec3.ZERO, Vec3.ZERO, PLAYER));
      assertTrue(moving(EYES, turn, 11.1, Vec3.ZERO, Vec3.ZERO, PLAYER));
      var samePacket = sample(turn, 45, 0, 11.1);
      assertTrue(moving(EYES, samePacket, 11.1, Vec3.ZERO, Vec3.ZERO, PLAYER));
      assertFalse(moving(EYES, turn, 13.1, Vec3.ZERO, Vec3.ZERO, PLAYER));
      assertFalse(moving(EYES, sample(turn, 45, 0, 12), 12, Vec3.ZERO, Vec3.ZERO, PLAYER));
   }

   @Test void twoRotationUpdatesWithinOneTickCanPredictAndThenCancelATurn() {
      var initial = sample(null, 75, 0, 10);
      var toward = sample(initial, 45, 0, 10.5);
      assertTrue(moving(EYES, toward, 10.5, Vec3.ZERO, Vec3.ZERO, PLAYER));
      var reversed = sample(toward, 60, 0, 10.8);
      assertFalse(moving(EYES, reversed, 10.8, Vec3.ZERO, Vec3.ZERO, PLAYER));
   }

   @Test void movingPlayersCanCrossTheViewBeforeTheCurrentRayHits() {
      var facing = sample(null, 0, 0, 10);
      var offset = PLAYER.move(1, 0, 0);
      assertFalse(moving(EYES, facing, 10, Vec3.ZERO, Vec3.ZERO, offset));
      assertTrue(moving(EYES, facing, 10, Vec3.ZERO, new Vec3(-0.5, 0, 0), offset));
      assertTrue(moving(EYES, facing, 10, new Vec3(0.5, 0, 0), Vec3.ZERO, offset));
      assertFalse(moving(EYES, sample(null, 180, 0, 10), 10, new Vec3(0.5, 0, 0), Vec3.ZERO, offset));
   }

   @Test void movementSamplesHandleStopsTeleportsAndStaleHistory() {
      var initial = movement(null, Vec3.ZERO, 10);
      var moved = movement(initial, new Vec3(0.3, 0, 0), 11);
      assertEquals(0.3, moved[4], 1.0E-9);
      assertEquals(0, movement(moved, new Vec3(0.3, 0, 0), 12)[4]);
      assertEquals(0, movement(moved, new Vec3(10, 0, 0), 12)[4]);
      assertEquals(0, movement(moved, new Vec3(0.6, 0, 0), 15)[4]);
   }

   @Test void currentViewMustPointTowardThePlayerInYawAndPitch() {
      assertTrue(predictsBlockThreat(9, EYES, rotation(0, 0, 10), null, PLAYER, 0, CLEAR));
      assertFalse(predictsBlockThreat(9, EYES, rotation(180, 0, 10), null, PLAYER, 0, CLEAR));
      assertFalse(predictsBlockThreat(9, EYES, rotation(90, 0, 10), null, PLAYER, 0, CLEAR));
      assertFalse(predictsBlockThreat(9, EYES, rotation(0, -75, 10), null, PLAYER, 0, CLEAR));
      assertFalse(predictsBlockThreat(9, EYES, rotation(0, 75, 10), null, PLAYER, 0, CLEAR));
   }

   @Test void theThreeAndAHalfBlockBoundaryIsInclusive() {
      var boundary = PLAYER.move(0, 0, 0.5);
      var facing = rotation(0, 0, 10);
      assertTrue(predictsBlockThreat(3.5 * 3.5, EYES, facing, null, boundary, 0, CLEAR));
      assertFalse(predictsBlockThreat(3.501 * 3.501, EYES, facing, null, boundary, 0, CLEAR));
   }

   @Test void turningTowardThePlayerBlocksBeforeTheCurrentRayHits() {
      var current = rotation(45, 0, 10);
      assertFalse(predictsBlockThreat(9, EYES, current, null, PLAYER, 0, CLEAR));
      assertTrue(predictsBlockThreat(9, EYES, current, rotation(70, 0, 9), PLAYER, 0, CLEAR));
      assertFalse(predictsBlockThreat(9, EYES, current, rotation(20, 0, 9), PLAYER, 0, CLEAR));
   }

   @Test void pitchIsPredictedWithoutAcceptingAStationaryUpwardView() {
      var current = rotation(0, -45, 10);
      assertFalse(predictsBlockThreat(9, EYES, current, null, PLAYER, 0, CLEAR));
      assertTrue(predictsBlockThreat(9, EYES, current, rotation(0, -70, 9), PLAYER, 0, CLEAR));
      assertFalse(predictsBlockThreat(9, EYES, current, rotation(0, -20, 9), PLAYER, 0, CLEAR));
   }

   @Test void aFastTurnIsCheckedAlongItsPathAndNotOnlyAtTheEnd() {
      assertTrue(predictsBlockThreat(9, EYES, rotation(50, 0, 10),
         rotation(110, 0, 9), PLAYER, 0, CLEAR));
   }

   @Test void slowTurnsThatCannotReachUsWithinTwoTicksDoNotBlock() {
      assertFalse(predictsBlockThreat(9, EYES, rotation(60, 0, 10),
         rotation(65, 0, 9), PLAYER, 0, CLEAR));
   }

   @Test void crossingTheYawBoundaryKeepsTheActualTurnDirection() {
      var behind = PLAYER.move(0, 0, -6);
      assertFalse(predictsBlockThreat(9, EYES, rotation(-150, 0, 10),
         rotation(170, 0, 9), behind, 0, CLEAR));
      assertFalse(predictsBlockThreat(9, EYES, rotation(150, 0, 10),
         rotation(-170, 0, 9), behind, 0, CLEAR));
      assertTrue(predictsBlockThreat(9, EYES, rotation(-150, 0, 10),
         rotation(-120, 0, 9), behind, 0, CLEAR));
   }

   @Test void oldOrDuplicateSamplesCannotInventATurn() {
      var current = rotation(45, 0, 10);
      assertFalse(predictsBlockThreat(9, EYES, current, rotation(70, 0, 7), PLAYER, 0, CLEAR));
      assertFalse(predictsBlockThreat(9, EYES, current, rotation(70, 0, 10), PLAYER, 0, CLEAR));
   }

   @Test void releasingDangerDoesNotKeepThePreviousAngularVelocity() {
      var facing = rotation(0, 0, 10);
      var away = rotation(90, 0, 11);
      assertTrue(predictsBlockThreat(9, EYES, facing, null, PLAYER, 0, CLEAR));
      assertFalse(predictsBlockThreat(9, EYES, away, facing, PLAYER, 0, CLEAR));
      assertFalse(predictsBlockThreat(9, EYES, rotation(90, 0, 12), away, PLAYER, 0, CLEAR));
   }

   @Test void wallsOnlyPermitDangerInsideTheConfiguredWallRange() {
      var facing = rotation(0, 0, 10);
      assertFalse(predictsBlockThreat(9, EYES, facing, null, PLAYER, 0, hit -> true));
      assertFalse(predictsBlockThreat(9, EYES, facing, null, PLAYER, 2.3, hit -> true));
      assertTrue(predictsBlockThreat(9, EYES, facing, null, PLAYER, 2.5, hit -> true));
      assertTrue(predictsBlockThreat(9, EYES, rotation(45, 0, 10),
         rotation(70, 0, 9), PLAYER, 0, CLEAR));
      assertFalse(predictsBlockThreat(9, EYES, rotation(45, 0, 10),
         rotation(70, 0, 9), PLAYER, 0, hit -> true));
   }

   @Test void overlappingHitboxesDoNotForceBlockingAgainstSomeoneLookingAway() {
      var close = PLAYER.move(0, 0, -2.8);
      assertTrue(predictsBlockThreat(0.04, EYES, rotation(0, 0, 10), null, close, 0, CLEAR));
      assertFalse(predictsBlockThreat(0.04, EYES, rotation(180, 0, 10), null, close, 0, CLEAR));
   }

   @Test void predictAloneIgnoresManualRightClicksAndOnlyBlocksForDanger() {
      assertFalse(shouldAutoBlock(true, false, false, false));
      assertFalse(shouldAutoBlock(true, false, true, false));
      assertTrue(shouldAutoBlock(true, false, false, true));
      assertTrue(shouldAutoBlock(true, false, true, true));
   }

   @Test void rmbAloneStillRequiresTheButtonEvenWhenThereIsDanger() {
      assertFalse(shouldAutoBlock(false, true, false, false));
      assertFalse(shouldAutoBlock(false, true, false, true));
      assertTrue(shouldAutoBlock(false, true, true, false));
      assertTrue(shouldAutoBlock(false, true, true, true));
   }

   @Test void predictWithRmbAcceptsBothAutomaticDangerAndManualFallback() {
      assertFalse(shouldAutoBlock(true, true, false, false));
      assertTrue(shouldAutoBlock(true, true, false, true));
      assertTrue(shouldAutoBlock(true, true, true, false));
      assertTrue(shouldAutoBlock(true, true, true, true));
   }

   @Test void predictKeepsItsAnimationAndRmbFollowsTheActiveBlockCycle() {
      var aura = new KillAura() {
         @Override public boolean isAutoBlockInputAllowed() { return true; }
      };
      var mode = (ModeSetting)aura.settings.stream().filter(setting -> setting.getName().equals("AutoBlock")).findFirst().orElseThrow();
      var predict = (BooleanSetting)aura.settings.stream().filter(setting -> setting.getName().equals("Predict")).findFirst().orElseThrow();
      var rmb = (BooleanSetting)aura.settings.stream().filter(setting -> setting.getName().equals("AutoBlock RMB")).findFirst().orElseThrow();
      mode.m226("Watchdog"); predict.m217(true);
      assertFalse(aura.isAutoBlocking()); assertTrue(aura.hasAutoBlockAnimation());
      rmb.m217(true);
      assertFalse(aura.hasAutoBlockAnimation());
      aura.f15 = true;
      assertTrue(aura.hasAutoBlockAnimation());
      aura.f16 = true;
      assertTrue(aura.hasAutoBlockAnimation());
      aura.f16 = false;
      assertTrue(aura.hasAutoBlockAnimation());
      aura.f15 = false;
      assertFalse(aura.hasAutoBlockAnimation());
      rmb.m217(false);
      assertTrue(aura.hasAutoBlockAnimation());
      mode.m226("None");
      assertFalse(aura.hasAutoBlockMode()); assertFalse(aura.hasAutoBlockAnimation());
   }

   @Test void rmbAnimationFollowsModeActivityThroughPacketGapsAndInputPauses() {
      boolean[] inputAllowed = {true};
      var aura = new KillAura() {
         @Override public boolean isAutoBlockInputAllowed() { return inputAllowed[0]; }
      };
      var mode = (ModeSetting)aura.settings.stream().filter(setting -> setting.getName().equals("AutoBlock")).findFirst().orElseThrow();
      var predict = (BooleanSetting)aura.settings.stream().filter(setting -> setting.getName().equals("Predict")).findFirst().orElseThrow();
      var rmb = (BooleanSetting)aura.settings.stream().filter(setting -> setting.getName().equals("AutoBlock RMB")).findFirst().orElseThrow();
      predict.m217(true); rmb.m217(true);
      for (String value : mode.m227()) {
         if (value.equals("None")) continue;
         mode.m226(value);
         aura.f15 = false; aura.f16 = false;
         assertFalse(aura.hasAutoBlockAnimation(), value);
         aura.f15 = true;
         assertTrue(aura.hasAutoBlockAnimation(), value + " begins its cycle");
         aura.f16 = true;
         assertTrue(aura.hasAutoBlockAnimation(), value + " sends the use packet");
         aura.f16 = false;
         assertTrue(aura.hasAutoBlockAnimation(), value + " temporarily releases or swaps slots");
         inputAllowed[0] = false;
         assertFalse(aura.hasAutoBlockAnimation(), value + " pauses for UI or AutoRod");
         inputAllowed[0] = true;
         aura.f15 = false;
         assertFalse(aura.hasAutoBlockAnimation(), value + " stops");
      }
   }
}
