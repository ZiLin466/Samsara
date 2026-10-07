package com.samsara.test.module.combat;

import com.samsara.module.Feature;
import com.samsara.module.Category;
import com.samsara.module.combat.Velocity;
import com.samsara.setting.BooleanSetting;
import com.samsara.setting.NumberSetting;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.function.BooleanSupplier;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class VelocityJumpResetTest {
   static final class Owner extends Feature {
      Owner() { super("JumpReset", Category.COMBAT); }
   }
   static final class Random implements RandomGenerator {
      float percent;
      boolean maximum;
      int samples;
      @Override public long nextLong() { return 0; }
      @Override public float nextFloat(float bound) { return percent; }
      @Override public int nextInt(int origin, int bound) { samples++; return maximum ? bound - 1 : origin; }
   }
   static final class Fixture {
      final Owner owner = new Owner();
      final Random random = new Random();
      final Object jump;
      final Class<?> type;

      Fixture() throws Exception {
         type = Class.forName(Velocity.class.getName() + "$JumpReset");
         Constructor<?> constructor = type.getDeclaredConstructor(Feature.class, BooleanSupplier.class, RandomGenerator.class);
         constructor.setAccessible(true);
         jump = constructor.newInstance(owner, (BooleanSupplier)() -> true, random);
      }
      void flag(String name, boolean value) {
         ((BooleanSetting)owner.settings.stream().filter(s -> s.getName().equals(name)).findFirst().orElseThrow()).setValue(value);
      }
      void number(String name, double value) {
         ((NumberSetting)owner.settings.stream().filter(s -> s.getName().equals(name)).findFirst().orElseThrow()).setValue(value);
      }
      Object invoke(String name, Class<?>[] parameters, Object... values) throws Exception {
         Method method = type.getDeclaredMethod(name, parameters); method.setAccessible(true);
         return method.invoke(jump, values);
      }
      boolean input(int hurt, boolean grounded, boolean sprinting) throws Exception {
         return (boolean)invoke("jump", new Class[]{int.class, boolean.class, boolean.class}, hurt, grounded, sprinting);
      }
      boolean hit() throws Exception { return input(9, true, true); }
      void tick() throws Exception { invoke("tick", new Class[0]); }
      void reset() throws Exception { invoke("reset", new Class[0]); }
      void damage(int entity, boolean fall) throws Exception {
         invoke("damage", new Class[]{int.class, int.class, boolean.class}, entity, 7, fall);
      }
      void automatic() { flag("Jump By Delay", false); }
   }

   @Test void onlyTheExactHurtWindowGroundAndSprintCanJump() throws Exception {
      var f = new Fixture(); f.automatic();
      for (int hurt : new int[]{0, 1, 8, 10}) assertFalse(f.input(hurt, true, true));
      assertFalse(f.input(9, false, true)); assertFalse(f.input(9, true, false));
      assertTrue(f.hit());
   }

   @Test void defaultDelayCountsMovementTicksAndRestartsAfterEachJump() throws Exception {
      var f = new Fixture();
      assertFalse(f.hit()); assertFalse(f.input(0, true, true)); assertTrue(f.hit());
      assertFalse(f.hit()); assertFalse(f.hit()); assertTrue(f.hit());
      f.reset(); assertFalse(f.hit());
   }

   @Test void hitBasedCooldownCountsReceivedHitsAndTakesPriorityOverDelay() throws Exception {
      var f = new Fixture(); f.flag("Jump By Received Hits", true);
      for (int i = 0; i < 20; i++) assertFalse(f.input(0, true, true));
      assertFalse(f.hit());
      for (int i = 0; i < 20; i++) assertFalse(f.input(8, true, true));
      assertFalse(f.hit()); assertTrue(f.hit());
      assertFalse(f.hit());
   }

   @Test void bothCooldownsOffAndZeroLimitsAllowTheFirstEligibleHit() throws Exception {
      var f = new Fixture(); f.automatic(); assertTrue(f.hit());
      f.flag("Jump By Received Hits", true);
      f.number("Hits Until Jump Min", 0); f.number("Hits Until Jump Max", 0); f.reset();
      assertTrue(f.hit());
   }

   @Test void probabilityBoundariesAndFailedChanceDoNotConsumeTheCooldown() throws Exception {
      var f = new Fixture(); f.automatic(); f.number("Chance", 0);
      assertFalse(f.hit());
      f.number("Chance", 50); f.random.percent = 50; assertFalse(f.hit());
      f.random.percent = 49.99f; assertTrue(f.hit());
      f.number("Chance", 100); f.random.percent = 99.99f; assertTrue(f.hit());
      f.flag("Jump By Delay", true); f.reset();
      assertFalse(f.hit()); assertFalse(f.hit());
      f.number("Chance", 0); assertFalse(f.hit());
      f.number("Chance", 100); assertTrue(f.hit());
   }

   @Test void rangesIncludeBothEndpointsNormalizeReversedValuesAndResampleAfterSuccess() throws Exception {
      var f = new Fixture();
      f.number("Ticks Until Jump Min", 4); f.number("Ticks Until Jump Max", 1); f.reset();
      assertFalse(f.hit());
      f.random.maximum = true; assertTrue(f.hit());
      for (int i = 0; i < 4; i++) assertFalse(f.hit());
      assertTrue(f.hit()); assertEquals(3, f.random.samples);
   }

   @Test void fallDamageExpiresAndNewCombatDamageOverridesItWithoutOtherEntitiesInterfering() throws Exception {
      var f = new Fixture(); f.automatic(); f.damage(7, true);
      for (int i = 0; i < 9; i++) { f.tick(); assertFalse(f.hit()); }
      f.tick(); assertTrue(f.hit());
      f.damage(7, true); f.damage(8, false); assertFalse(f.hit());
      f.damage(7, false); assertTrue(f.hit());
      f.damage(8, true); assertTrue(f.hit());
   }

   @Test void modeChangesResetCooldownWithoutForgettingTheCurrentFallCycle() throws Exception {
      var f = new Fixture(); f.automatic(); f.damage(7, true); f.reset(); assertFalse(f.hit());
      f.invoke("clearDamage", new Class[0]); assertTrue(f.hit());
      f.flag("Jump By Delay", true); f.reset();
      assertFalse(f.hit()); assertFalse(f.hit()); assertTrue(f.hit());
      f.reset(); assertFalse(f.hit());
   }
}
