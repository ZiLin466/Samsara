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
   private static final Class<?> ROTATION_SAMPLE = nestedType("RotationSample");
   private static final Class<?> MOVEMENT_SAMPLE = nestedType("MovementSample");
   private static final MethodHandle SAMPLE = handle("sampleBlockRotation", ROTATION_SAMPLE,
      ROTATION_SAMPLE, float.class, float.class, double.class).asType(MethodType.methodType(Object.class,
         Object.class, float.class, float.class, double.class));
   private static final MethodHandle MOVEMENT = handle("sampleBlockMovement", MOVEMENT_SAMPLE, MOVEMENT_SAMPLE, Vec3.class, double.class)
      .asType(MethodType.methodType(Object.class, Object.class, Vec3.class, double.class));
   private static final MethodHandle VELOCITY = velocityAccessor();
   private static final MethodHandle MOVING = handle("predictsMovingBlockThreat", boolean.class,
      double.class, Vec3.class, ROTATION_SAMPLE, double.class, Vec3.class, Vec3.class, AABB.class, double.class, BiPredicate.class)
      .asType(MethodType.methodType(boolean.class, double.class, Vec3.class, Object.class, double.class,
         Vec3.class, Vec3.class, AABB.class, double.class, BiPredicate.class));
   private static final MethodHandle BLOCK = method("shouldAutoBlock", boolean.class, boolean.class, boolean.class, boolean.class);

   private static MethodHandle method(String name, Class<?>... parameters) {
      return handle(name, boolean.class, parameters);
   }

   private static MethodHandle handle(String name, Class<?> result, Class<?>... parameters) {
      try {
         Class<?> declaringType = name.equals("shouldAutoBlock") ? KillAura.class : nestedType("BlockPrediction");
         return MethodHandles.privateLookupIn(declaringType, MethodHandles.lookup())
            .findStatic(declaringType, name, MethodType.methodType(result, parameters));
      } catch (ReflectiveOperationException error) { throw new ExceptionInInitializerError(error); }
   }

   private static Class<?> nestedType(String name) {
      String container = name.equals("RotationSample") || name.equals("MovementSample") ? "$BlockPrediction$" : "$";
      try { return Class.forName(KillAura.class.getName() + container + name); }
      catch (ClassNotFoundException error) { throw new ExceptionInInitializerError(error); }
   }

   private static MethodHandle velocityAccessor() {
      try {
         return MethodHandles.privateLookupIn(MOVEMENT_SAMPLE, MethodHandles.lookup())
            .findVirtual(MOVEMENT_SAMPLE, "velocity", MethodType.methodType(Vec3.class))
            .asType(MethodType.methodType(Vec3.class, Object.class));
      } catch (ReflectiveOperationException error) { throw new ExceptionInInitializerError(error); }
   }

   private record ViewSample(float yaw, float pitch, double tick) { }

   private static boolean predictsBlockThreat(double distance, Vec3 eyes, ViewSample current, ViewSample previous,
      AABB target, double wallRange, Predicate<Vec3> occluded) {
      Object history = previous == null ? null : sample(null, previous.yaw(), previous.pitch(), previous.tick());
      Object rotation = sample(history, current.yaw(), current.pitch(), current.tick());
      BiPredicate<Vec3, Vec3> blocked = (origin, hit) -> occluded.test(hit);
      try { return (boolean)MOVING.invokeExact(distance, eyes, rotation, current.tick(), Vec3.ZERO, Vec3.ZERO, target, wallRange, blocked); }
      catch (Throwable error) { throw new AssertionError(error); }
   }

   private static boolean shouldAutoBlock(boolean predict, boolean rmb, boolean held, boolean threat) {
      try { return (boolean)BLOCK.invokeExact(predict, rmb, held, threat); }
      catch (Throwable error) { throw new AssertionError(error); }
   }

   private static final Vec3 EYES = new Vec3(0, 1.62, 0);
   private static final AABB PLAYER = new AABB(-0.3, 0, 2.7, 0.3, 1.8, 3.3);
   private static final Predicate<Vec3> CLEAR = hit -> false;

   private static ViewSample rotation(float yaw, float pitch, int tick) {
      return new ViewSample(yaw, pitch, tick);
   }

   private static Object sample(Object previous, float yaw, float pitch, double now) {
      try { return (Object)SAMPLE.invokeExact(previous, yaw, pitch, now); }
      catch (Throwable error) { throw new AssertionError(error); }
   }

   private static Object movement(Object previous, Vec3 position, double now) {
      try { return (Object)MOVEMENT.invokeExact(previous, position, now); }
      catch (Throwable error) { throw new AssertionError(error); }
   }

   private static Vec3 velocity(Object sample) {
      try { return (Vec3)VELOCITY.invokeExact(sample); }
      catch (Throwable error) { throw new AssertionError(error); }
   }

   private static boolean moving(Vec3 eyes, Object sample, double now, Vec3 attackerVelocity, Vec3 localVelocity, AABB target) {
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
      assertEquals(0.3, velocity(moved).x, 1.0E-9);
      assertEquals(0, velocity(movement(moved, new Vec3(0.3, 0, 0), 12)).x);
      assertEquals(0, velocity(movement(moved, new Vec3(10, 0, 0), 12)).x);
      assertEquals(0, velocity(movement(moved, new Vec3(0.6, 0, 0), 15)).x);
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

   @Test void disabledFireballAttacksDoNotAccessTheWorld() throws Exception {
      var aura = new KillAura();
      var setting = (BooleanSetting)aura.settings.stream()
         .filter(candidate -> candidate.getName().equals("Attack Fireballs")).findFirst().orElseThrow();
      setting.setValue(false);
      var attack = KillAura.class.getDeclaredMethod("attackFireballIfPresent");
      attack.setAccessible(true);
      assertEquals(false, attack.invoke(aura));
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

   private static Object component(KillAura aura, String name) {
      try {
         var field = KillAura.class.getDeclaredField(name);
         field.setAccessible(true);
         return field.get(aura);
      } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
   }

   private static void setBlockState(KillAura aura, String name, boolean active) {
      try {
         Object blocking = component(aura, "blocking");
         var field = blocking.getClass().getDeclaredField(name);
         field.setAccessible(true);
         field.setBoolean(blocking, active);
      } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
   }

   @Test void resettingPredictionPreservesAnUnreleasedServerBlock() throws Exception {
      var aura = new KillAura();
      setBlockState(aura, "serverBlocking", true);
      setBlockState(aura, "predictiveBlockActive", true);
      var reset = KillAura.class.getDeclaredMethod("resetBlockPrediction");
      reset.setAccessible(true);
      reset.invoke(aura);
      assertTrue(aura.isServerBlocking());
      assertTrue(aura.isAutoBlocking());
      Object blocking = component(aura, "blocking");
      var predictive = blocking.getClass().getDeclaredField("predictiveBlockActive");
      predictive.setAccessible(true);
      assertFalse(predictive.getBoolean(blocking));
   }

   @Test void blockStateBelongsToItsAuraInstance() {
      var firstAura = new KillAura();
      var secondAura = new KillAura();
      setBlockState(firstAura, "serverBlocking", true);
      assertTrue(firstAura.isServerBlocking());
      assertFalse(secondAura.isServerBlocking());
      assertFalse(secondAura.isAutoBlocking());
   }

   @Test void predictKeepsItsAnimationAndRmbFollowsTheActiveBlockCycle() {
      var aura = new KillAura() {
         @Override public boolean isAutoBlockInputAllowed() { return true; }
      };
      var mode = (ModeSetting)aura.settings.stream().filter(setting -> setting.getName().equals("AutoBlock")).findFirst().orElseThrow();
      var predict = (BooleanSetting)aura.settings.stream().filter(setting -> setting.getName().equals("Predict")).findFirst().orElseThrow();
      var rmb = (BooleanSetting)aura.settings.stream().filter(setting -> setting.getName().equals("AutoBlock RMB")).findFirst().orElseThrow();
      mode.setValue("Watchdog"); predict.setValue(true);
      assertFalse(aura.isAutoBlocking()); assertTrue(aura.hasAutoBlockAnimation());
      rmb.setValue(true);
      assertFalse(aura.hasAutoBlockAnimation());
      setBlockState(aura, "autoBlockActive", true);
      assertTrue(aura.hasAutoBlockAnimation());
      setBlockState(aura, "serverBlocking", true);
      assertTrue(aura.hasAutoBlockAnimation());
      setBlockState(aura, "serverBlocking", false);
      assertTrue(aura.hasAutoBlockAnimation());
      setBlockState(aura, "autoBlockActive", false);
      assertFalse(aura.hasAutoBlockAnimation());
      rmb.setValue(false);
      assertTrue(aura.hasAutoBlockAnimation());
      mode.setValue("None");
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
      predict.setValue(true); rmb.setValue(true);
      for (String value : mode.getOptions()) {
         if (value.equals("None")) continue;
         mode.setValue(value);
         setBlockState(aura, "autoBlockActive", false); setBlockState(aura, "serverBlocking", false);
         assertFalse(aura.hasAutoBlockAnimation(), value);
         setBlockState(aura, "autoBlockActive", true);
         assertTrue(aura.hasAutoBlockAnimation(), value + " begins its cycle");
         setBlockState(aura, "serverBlocking", true);
         assertTrue(aura.hasAutoBlockAnimation(), value + " sends the use packet");
         setBlockState(aura, "serverBlocking", false);
         assertTrue(aura.hasAutoBlockAnimation(), value + " temporarily releases or swaps slots");
         inputAllowed[0] = false;
         assertFalse(aura.hasAutoBlockAnimation(), value + " pauses for UI or AutoRod");
         inputAllowed[0] = true;
         setBlockState(aura, "autoBlockActive", false);
         assertFalse(aura.hasAutoBlockAnimation(), value + " stops");
      }
   }
}
