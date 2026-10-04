package com.samsara.module.combat;

import java.util.List;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class CombatBehaviorTest {
   @Test void delayAppliesInitiatorAndReleasesSubsequentPacketsInOrderAfterExactTicks() {
      var queue = new Velocity.DelayQueue<String>();
      assertFalse(queue.offer("initiating motion")); queue.start(3);
      assertTrue(queue.offer("move")); assertTrue(queue.offer("ping"));
      assertTrue(queue.tick().isEmpty()); assertTrue(queue.tick().isEmpty());
      assertEquals(List.of("move", "ping"), queue.tick()); assertFalse(queue.active());
      assertFalse(queue.offer("new motion")); assertTrue(queue.tick().isEmpty());
      queue.start(2); queue.offer("old world"); queue.reset(); assertTrue(queue.drain().isEmpty());
      queue.start(5); queue.offer("disabled"); assertEquals(List.of("disabled"), queue.drain()); assertFalse(queue.active());
   }
   @Test void wallRangeOnlyAllowsOccludedHitsInsideConfiguredDistance() {
      Vec3 eye = Vec3.ZERO, direction = new Vec3(0, 0, 1);
      AABB target = new AABB(-.3, -.3, 2, .3, .3, 2.6);
      assertEquals(-.1, KillAura.raycastDistance(eye, direction, target, 3, 0, hit -> true));
      assertEquals(-.1, KillAura.raycastDistance(eye, direction, target, 3, 1.9, hit -> true));
      assertEquals(2, KillAura.raycastDistance(eye, direction, target, 3, 2, hit -> true));
      assertEquals(2, KillAura.raycastDistance(eye, direction, target, 3, 0, hit -> false));
      assertEquals(2, KillAura.raycastDistance(eye, direction, target, 3, 0, hit -> {
         assertEquals(new Vec3(0, 0, 2), hit); return false;
      }));
      assertEquals(-.1, KillAura.raycastDistance(eye, new Vec3(1, 0, 0), target, 3, 3, hit -> false));
   }
   @Test void entityStatesAndFriendsAreIndependentFilters() {
      List<String> selected = List.of("Players", "Invisible", "Hostile");
      assertTrue(TargetSettings.allowed(selected, "Players", true, true, false, false));
      assertTrue(TargetSettings.allowed(selected, "Hostile", true, false, false, false));
      assertFalse(TargetSettings.allowed(selected, "Passive", true, false, false, false));
      assertFalse(TargetSettings.allowed(selected, "Players", true, false, false, true));
      assertFalse(TargetSettings.allowed(selected, "Players", false, false, false, false));
      assertFalse(TargetSettings.allowed(selected, "Players", true, false, true, false));
      assertTrue(TargetSettings.allowed(List.of("Friends", "Dead", "Sleeping"), "Players", false, false, true, true));
   }
}
