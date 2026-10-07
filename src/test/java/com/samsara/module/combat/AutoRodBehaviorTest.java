package com.samsara.module.combat;

import java.util.List;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class AutoRodBehaviorTest {
   @Test void timeoutAndCooldownHaveNoExtraTickAndKeepTheOriginalTarget() {
      var cycle = new AutoRod.Cycle<String>();
      cycle.start("first target", 30, 3.5, 5);
      for (int tick = 1; tick < 30; tick++) {
         assertFalse(cycle.tick(false, false, false, 16));
         assertFalse(cycle.ready()); assertEquals("first target", cycle.target());
      }
      assertTrue(cycle.tick(false, false, false, 16));
      cycle.pulled(4);
      for (int tick = 1; tick < 4; tick++) { cycle.tick(false, false, false, 0); assertFalse(cycle.ready()); }
      cycle.tick(false, false, false, 0); assertTrue(cycle.ready()); assertNull(cycle.target());
   }
   @Test void everyRetrieveConditionResumesTheWaitingCast() {
      var cycle = new AutoRod.Cycle<String>();
      cycle.start("hooked", 30, 3.5, 5); assertTrue(cycle.tick(true, false, false, 16));
      cycle.start("stopped", 30, 3.5, 5); assertTrue(cycle.tick(false, true, false, 16));
      cycle.start("too close", 30, 3.5, 5); assertTrue(cycle.tick(false, false, true, 12));
      cycle.start("too far", 30, 3.5, 5); assertTrue(cycle.tick(false, false, true, 26));
      cycle.start("disabled range check", 30, 3.5, 5); assertFalse(cycle.tick(false, false, false, 200));
      cycle.start("inclusive endpoints", 30, 3.5, 5);
      assertFalse(cycle.tick(false, false, true, 12.25)); assertFalse(cycle.tick(false, false, true, 25));
      cycle.reset(); assertTrue(cycle.ready()); assertNull(cycle.target());
   }
   @Test void silentSlotZeroDelayRestoresNextTickAndKeepsUserSelection() {
      var slot = new AutoRod.Slot();
      slot.select(7, 0); assertEquals(7, slot.selected(2));
      assertTrue(slot.tick()); assertEquals(2, slot.selected(2)); assertFalse(slot.tick());
      slot.select(5, 2); assertFalse(slot.tick()); assertFalse(slot.tick()); assertTrue(slot.tick());
      assertEquals(4, slot.selected(4));
      slot.select(7, 200); slot.clear(); assertEquals(3, slot.selected(3));
   }
   @Test void retrievalReplacesTheLongCastSlotLease() {
      var slot = new AutoRod.Slot();
      slot.select(8, 35);
      for (int tick = 0; tick < 7; tick++) assertFalse(slot.tick());
      slot.select(8, 2);
      assertFalse(slot.tick()); assertFalse(slot.tick()); assertTrue(slot.tick());
      assertEquals(0, slot.selected(0));
   }
   @Test void directionThresholdIncludesPitchAndYawWrap() {
      var rotation = new AutoRod.Aim.Rotation(0, 0);
      assertEquals(45, rotation.angleTo(new AutoRod.Aim.Rotation(0, 45)), .01);
      assertEquals(2, new AutoRod.Aim.Rotation(179, 0).angleTo(new AutoRod.Aim.Rotation(-179, 0)), .01);
      assertEquals(0, new AutoRod.Aim.Rotation(0, 90).angleTo(new AutoRod.Aim.Rotation(180, 90)), 1e-4);
      assertEquals(rotation, AutoRod.Aim.turnLinear(rotation, rotation, 180, 180));
      var step = AutoRod.Aim.turnLinear(rotation, new AutoRod.Aim.Rotation(90, 0), 20, 20);
      assertEquals(20, step.yaw()); assertEquals(0, step.pitch());
   }
   @Test void projectedAimPointsLieOnVisibleFacesAcrossAllDirections() {
      var box = new AABB(-.3, 0, -.3, .3, 1.8, .3);
      for (Vec3 eye : List.of(new Vec3(5, 1.6, 0), new Vec3(0, 1.6, 5), new Vec3(0, 8, 0), new Vec3(-5, -2, -3))) {
         var points = AutoRod.Aim.projectedPoints(eye, box);
         assertFalse(points.isEmpty(), eye.toString()); assertTrue(points.size() < 300);
         for (Vec3 point : points) {
            assertEquals(0, AutoRod.Aim.nearest(box, point).distanceTo(point), 1e-7);
            assertEquals(0, point.distanceTo(box.clip(eye, point.lerp(eye, -100)).orElseThrow()), 1e-7);
         }
      }
      assertTrue(AutoRod.Aim.projectedPoints(box.getCenter(), box).isEmpty());
   }
   @Test void nearProjectileUsesThePolynomialAngleAndPredictsMotion() {
      Vec3 eyes = new Vec3(0, 1.6, 0);
      var stationary = AutoRod.Aim.projectile(eyes, ticks -> new Vec3(0, 0, 4), .6, 1.8, (rayStart, rayEnd) -> true);
      assertNotNull(stationary); assertEquals(0, stationary.yaw());
      double discriminant = Math.pow(2.25, 2) - .03 * (.03 * 16 - 3.2 * 2.25);
      double expected = -Math.toDegrees(Math.atan((2.25 - Math.sqrt(discriminant)) / (.03 * 4)));
      assertEquals(expected, stationary.pitch(), 1e-5);
      var moving = AutoRod.Aim.projectile(eyes, ticks -> new Vec3(ticks * .2, 0, 4), .6, 1.8, (rayStart, rayEnd) -> true);
      assertNotNull(moving); assertTrue(moving.yaw() < 0);
      assertNull(AutoRod.Aim.projectile(Vec3.ZERO, ticks -> new Vec3(0, 100, 1), .6, 1.8, (rayStart, rayEnd) -> true));
   }
   @Test void distantProjectileUsesDragGravityAndRejectsBlockedImpact() {
      Vec3 eye = new Vec3(0, 1.6, 0), target = new Vec3(0, 0, 7);
      var rotation = AutoRod.Aim.projectile(eye, ticks -> target, .6, 1.8, (rayStart, rayEnd) -> true);
      assertNotNull(rotation); assertTrue(Float.isFinite(rotation.pitch()));
      assertNull(AutoRod.Aim.projectile(eye, ticks -> target, .6, 1.8, (rayStart, rayEnd) -> false));
      double time = 5;
      Vec3 direction = AutoRod.Aim.directionByTime(eye, target, time);
      double drag = .92, power = Math.pow(drag, time), sum = (power - 1) / (drag - 1);
      Vec3 reached = eye.add(direction.scale(1.5 * sum)).add(0, -.04 * (power - drag * time + time - 1) / Math.pow(drag - 1, 2), 0);
      assertEquals(0, reached.distanceTo(target), 1e-10);
   }
   @Test void silentMovementPreservesTheWorldDirectionForQuarterTurns() {
      assertEquals(new Vec2(0, 1), AutoRod.Aim.correctMovement(new Vec2(0, 1), 0, 0));
      assertEquals(new Vec2(1, 0), AutoRod.Aim.correctMovement(new Vec2(0, 1), 0, 90));
      assertEquals(new Vec2(-1, 0), AutoRod.Aim.correctMovement(new Vec2(0, 1), 0, -90));
      assertEquals(Vec2.ZERO, AutoRod.Aim.correctMovement(Vec2.ZERO, 35, 120));
   }
}
