package com.samsara.test.module.player;

import com.samsara.module.FeatureManager;
import com.samsara.test.module.player.experimental.Scaffold;
import com.samsara.setting.ModeSetting;
import java.lang.reflect.Method;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class ScaffoldClosetTest {
   @BeforeAll static void initializeModules() {
      SharedConstants.tryDetectVersion();
      Bootstrap.bootStrap();
      if (FeatureManager.getModules() == null) FeatureManager.registerModules();
   }

   private static Object invoke(String name, Class<?>[] signature, Object... args) throws Exception {
      Method method = Scaffold.class.getDeclaredMethod(name, signature);
      method.setAccessible(true);
      return method.invoke(null, args);
   }

   private static final class Axis {
      private final Object instance;
      private final Method advance;
      private final java.lang.reflect.Field angle;
      Axis(float initial) throws Exception {
         Class<?> type = Class.forName(Scaffold.class.getName() + "$TurnAxis");
         var constructor = type.getDeclaredConstructor(); constructor.setAccessible(true);
         this.instance = constructor.newInstance();
         var reset = type.getDeclaredMethod("reset", float.class); reset.setAccessible(true);
         reset.invoke(this.instance, initial);
         this.advance = type.getDeclaredMethod("advance", float.class, float.class, float.class, double.class, boolean.class);
         this.advance.setAccessible(true);
         this.angle = type.getDeclaredField("angle"); this.angle.setAccessible(true);
      }
      float value() throws Exception { return this.angle.getFloat(this.instance); }
      void step(float goal, float speed, float acceleration, double sensitivity, boolean wrap) throws Exception {
         this.advance.invoke(this.instance, goal, speed, acceleration, sensitivity, wrap);
      }
   }

   @Test void legacyOptionsAndClosetSettingsAreMutuallyExclusive() {
      var scaffold = new Scaffold();
      var mode = (ModeSetting)scaffold.settings.stream().filter(s -> s.getName().equals("Mode")).findFirst().orElseThrow();
      assertEquals("Blatant", mode.getValue());
      assertArrayEquals(new String[]{"Blatant", "Closet"}, mode.getOptions());
      for (var setting : scaffold.settings) {
         if (setting == mode) continue;
         assertEquals(!setting.getName().startsWith("Closet "), setting.isVisible(), setting.getName());
      }
      mode.setValue("Closet");
      for (var setting : scaffold.settings) {
         if (setting == mode) continue;
         assertEquals(setting.getName().startsWith("Closet "), setting.isVisible(), setting.getName());
      }
      var rotations = (ModeSetting)scaffold.settings.stream().filter(s -> s.getName().equals("Rotations")).findFirst().orElseThrow();
      rotations.setValue("Backward");
      mode.setValue("Blatant");
      assertEquals("Backward", rotations.getValue());
   }

   @Test void yawCrossesTheWrapBoundaryByTheShortPath() throws Exception {
      Axis yaw = new Axis(179);
      float previous = yaw.value();
      for (int tick = 0; tick < 40; tick++) {
         yaw.step(-179, 22, 5, 0.15, true);
         assertTrue(Math.abs(yaw.value() - previous) <= 2.01);
         previous = yaw.value();
      }
      assertEquals(181, yaw.value(), 0.15);
   }

   @Test void sharpReversalRespectsSpeedAndAccelerationAndConverges() throws Exception {
      Axis yaw = new Axis(0);
      float previous = 0, previousDelta = 0;
      for (int tick = 0; tick < 100; tick++) {
         float goal = tick < 6 ? 130 : -65;
         yaw.step(goal, 22, 5, 0.15, true);
         float delta = yaw.value() - previous;
         assertTrue(Math.abs(delta) <= 22.001, "speed at tick " + tick);
         assertTrue(Math.abs(delta - previousDelta) <= 5.3, "acceleration at tick " + tick);
         previous = yaw.value(); previousDelta = delta;
      }
      assertEquals(0, Mth.wrapDegrees(yaw.value() + 65), 0.2);
   }

   @Test void pitchIsClampedAndQuantizedWithoutChangingMouseSensitivity() throws Exception {
      Axis pitch = new Axis(0);
      for (int tick = 0; tick < 60; tick++) {
         float before = pitch.value();
         pitch.step(82, 14, 5, 0.15, false);
         float delta = pitch.value() - before;
         assertTrue(pitch.value() >= -90 && pitch.value() <= 90);
         assertTrue(Math.abs(delta) <= 14.001);
         assertEquals(Math.rint(delta / 0.15), delta / 0.15, 0.001);
      }
      assertEquals(82, pitch.value(), 0.2);
   }

   @Test void edgeHysteresisHoldsUntilTheLongerPredictionIsSupported() throws Exception {
      Class<?>[] signature = {boolean.class, boolean.class, boolean.class, boolean.class};
      assertEquals(false, invoke("edgeState", signature, false, true, false, true));
      assertEquals(true, invoke("edgeState", signature, false, true, true, true));
      assertEquals(true, invoke("edgeState", signature, true, true, false, true));
      assertEquals(false, invoke("edgeState", signature, true, true, false, false));
      assertEquals(false, invoke("edgeState", signature, true, false, true, true));
   }

   private static boolean supported(Vec3 start, Vec3 motion, double margin, AABB... blocks) throws Exception {
      Predicate<AABB> collision = feet -> List.of(blocks).stream().anyMatch(feet::intersects);
      return (boolean)invoke("hasSupport", new Class<?>[]{Vec3.class, Vec3.class, float.class, double.class, Predicate.class},
         start, motion, 0.6F, margin, collision);
   }

   @Test void newlyPlacedSupportReleasesSneakWithoutAPlacementDelay() throws Exception {
      Vec3 start = new Vec3(1.1, 1, 0.5), motion = new Vec3(0.18, 0, 0);
      AABB oldBlock = new AABB(0, 0, 0, 1, 1, 1), newBlock = oldBlock.move(1, 0, 0);
      assertFalse(supported(start, motion, 0.04, oldBlock));
      assertFalse(supported(start, motion, 0.07, oldBlock));
      assertTrue(supported(start, motion, 0.07, oldBlock, newBlock));
      assertTrue(supported(new Vec3(0.8, 1, 0.5), motion, 0.07, oldBlock));
   }

   @Test void predictionRejectsGapsEvenWhenBothEndpointsHaveSupport() throws Exception {
      AABB first = new AABB(0, 0, 0, 0.5, 1, 1), last = new AABB(1.5, 0, 0, 2, 1, 1);
      assertFalse(supported(new Vec3(0.3, 1, 0.5), new Vec3(1.4, 0, 0), 0.07, first, last));
      assertFalse(supported(new Vec3(0.9, 1, 0.9), new Vec3(0.35, 0, 0.35), 0.07,
         new AABB(0, 0, 0, 1, 1, 1)));
   }

   @Test void diagonalPlanningIncludesBothAdjacentStepsAndHandlesNegativeCoordinates() throws Exception {
      Class<?>[] signature = {Vec3.class, Vec3.class, double.class};
      @SuppressWarnings("unchecked")
      Set<BlockPos> path = (Set<BlockPos>)invoke("walkingPath", signature,
         new Vec3(-0.1, 1, -0.1), new Vec3(-0.2, 0, -0.2), 1.3);
      assertTrue(path.contains(new BlockPos(-2, 0, -2)));
      assertTrue(path.contains(new BlockPos(-2, 0, -1)));
      assertTrue(path.contains(new BlockPos(-1, 0, -2)));
      assertFalse(path.contains(new BlockPos(0, 0, 0)));
      assertEquals(Set.of(new BlockPos(-1, 0, -1)), invoke("walkingPath", signature,
         new Vec3(-0.1, 1, -0.1), Vec3.ZERO, 0.65));
   }

   @Test void projectingTheExistingViewOntoAVisibleSideRetainsItsYaw() throws Exception {
      Vec3 eyes = new Vec3(1.2, 2.62, 0.5);
      AABB box = new AABB(0, 0, 0, 1, 1, 1);
      Class<?>[] signature = {Vec3.class, float.class, float.class, AABB.class, Direction.class};
      Vec3 point = (Vec3)invoke("pointAtYaw", signature, eyes, 90F, 82F, box, Direction.EAST);
      assertEquals(1, point.x, 1.0E-7);
      assertTrue(point.y >= 0.04 && point.y <= 0.96);
      float[] rotation = (float[])invoke("angles", new Class<?>[]{Vec3.class, Vec3.class}, eyes, point);
      assertEquals(0, Mth.wrapDegrees(rotation[0] - 90), 0.01);
      assertTrue(rotation[1] > 0 && rotation[1] < 90);
   }

   @Test void fixedYawFacesStayInsidePartialCollisionShapes() throws Exception {
      AABB slab = new AABB(-3, 0, -2, -2, 0.5, -1);
      for (Direction face : new Direction[]{Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH, Direction.UP}) {
         Vec3 eyes = slab.getCenter().add(face.getStepX() * 0.7, 1.62, face.getStepZ() * 0.7);
         float yaw = switch (face) { case EAST -> 90; case WEST -> -90; case SOUTH -> 180; default -> 0; };
         Vec3 point = (Vec3)invoke("pointAtYaw", new Class<?>[]{Vec3.class, float.class, float.class, AABB.class, Direction.class},
            eyes, yaw, 80F, slab, face);
         assertNotNull(point, face.toString());
         assertTrue(point.x >= slab.minX && point.x <= slab.maxX);
         assertTrue(point.y >= slab.minY && point.y <= slab.maxY);
         assertTrue(point.z >= slab.minZ && point.z <= slab.maxZ);
      }
   }

   private static Object nested(String name) throws Exception {
      var type = Class.forName(Scaffold.class.getName() + "$" + name);
      var constructor = type.getDeclaredConstructor(); constructor.setAccessible(true);
      return constructor.newInstance();
   }

   private static Object call(Object object, String name, Class<?>[] types, Object... args) throws Exception {
      var method = object.getClass().getDeclaredMethod(name, types); method.setAccessible(true);
      return method.invoke(object, args);
   }

   private static boolean route(Object route, float camera, int forward, int strafe, Vec3 position) throws Exception {
      return (boolean)call(route, "update", new Class<?>[]{float.class, int.class, int.class, Vec3.class}, camera, forward, strafe, position);
   }

   private static Vec3 worldMovement(Vec2 input, float yaw) {
      double angle = Math.toRadians(yaw);
      return new Vec3(input.x * Math.cos(angle) - input.y * Math.sin(angle), 0,
         input.y * Math.cos(angle) + input.x * Math.sin(angle));
   }

   private static Vec3 vanillaMovement(Vec3 direction, float yaw, boolean sneaking) throws Exception {
      Vec2 input = (Vec2)invoke("relativeInput", new Class<?>[]{Vec3.class, float.class}, direction, yaw);
      var squareMovement = LocalPlayer.class.getDeclaredMethod("modifyInputSpeedForSquareMovement", Vec2.class);
      squareMovement.setAccessible(true);
      input = (Vec2)squareMovement.invoke(null, input.scale(0.98F * (sneaking ? 0.3F : 1)));
      if (input.length() > 1) input = input.normalized();
      return worldMovement(input, yaw);
   }

   @Test void smallMouseChangesAndSideFacingDoNotBendAThousandTickBridge() throws Exception {
      Object route = nested("BridgeRoute");
      Vec3 position = new Vec3(0.5, 1, 0.5), velocity = Vec3.ZERO;
      assertTrue(route(route, 0, 1, 0, position));
      for (int tick = 0; tick < 1000; tick++) {
         assertFalse(route(route, (float)(Math.sin(tick * 0.1) * 20), 1, 0, position));
         Vec3 travel = (Vec3)call(route, "travel", new Class<?>[]{Vec3.class, Vec3.class}, position, velocity);
         float sideFacing = 135 + (float)(Math.sin(tick * 0.07) * 10);
         Vec2 input = (Vec2)invoke("relativeInput", new Class<?>[]{Vec3.class, float.class}, travel, sideFacing);
         velocity = worldMovement(input, sideFacing).scale(0.2);
         position = position.add(velocity);
         assertEquals(0.5, position.x, 0.0001);
      }
      assertEquals(200.5, position.z, 0.001);
      assertTrue(route(route, 50, 1, 0, position));
      Vec3 turned = (Vec3)call(route, "direction", new Class<?>[]{});
      assertEquals(-Math.sqrt(0.5), turned.x, 0.0001);
      assertEquals(Math.sqrt(0.5), turned.z, 0.0001);
   }

   @Test void explicitLeftAndRightDiagonalCommandsAreIndependentOfSmallCameraDrift() throws Exception {
      Object route = nested("BridgeRoute");
      route(route, 0, 1, 0, Vec3.ZERO);
      assertTrue(route(route, 15, 1, 1, Vec3.ZERO));
      Vec3 left = (Vec3)call(route, "direction", new Class<?>[]{});
      assertEquals(Math.sqrt(0.5), left.x, 0.0001);
      assertEquals(Math.sqrt(0.5), left.z, 0.0001);
      assertFalse(route(route, -12, 1, 1, Vec3.ZERO));
      assertTrue(route(route, -12, 1, -1, Vec3.ZERO));
      Vec3 right = (Vec3)call(route, "direction", new Class<?>[]{});
      assertEquals(-Math.sqrt(0.5), right.x, 0.0001);
      assertEquals(Math.sqrt(0.5), right.z, 0.0001);
      Vec2 controls = (Vec2)invoke("relativeInput", new Class<?>[]{Vec3.class, float.class}, right, 180F);
      assertTrue(Math.abs(controls.x) > 0.7 && Math.abs(controls.y) > 0.7);
      assertEquals(1, controls.length(), 0.0001);
   }

   @Test void sideCrouchingReceivesVanillasSquareRootTwoInputGain() throws Exception {
      var squareMovement = LocalPlayer.class.getDeclaredMethod("modifyInputSpeedForSquareMovement", Vec2.class);
      squareMovement.setAccessible(true);
      Vec2 straight = (Vec2)invoke("relativeInput", new Class<?>[]{Vec3.class, float.class}, new Vec3(0, 0, 1), 180F);
      Vec2 sideways = (Vec2)invoke("relativeInput", new Class<?>[]{Vec3.class, float.class}, new Vec3(0, 0, 1), 135F);
      Vec2 straightSlow = (Vec2)squareMovement.invoke(null, straight.scale(0.98F * 0.3F));
      Vec2 sidewaysSlow = (Vec2)squareMovement.invoke(null, sideways.scale(0.98F * 0.3F));
      assertEquals(Math.sqrt(2), sidewaysSlow.length() / straightSlow.length(), 0.0001);
      assertEquals(0, worldMovement(sidewaysSlow, 135).x, 0.0001);
   }

   @Test void routeCrossesTheCameraWrapWithoutChangingDirection() throws Exception {
      Object route = nested("BridgeRoute");
      route(route, 179, 1, 0, Vec3.ZERO);
      assertFalse(route(route, -179, 1, 0, Vec3.ZERO));
      Vec3 direction = (Vec3)call(route, "direction", new Class<?>[]{});
      assertEquals(0, direction.x, 0.0001);
      assertEquals(-1, direction.z, 0.0001);
   }

   @Test void jumpKeepsOnePlacementLevelThroughoutTheArcAndRelease() throws Exception {
      Object step = nested("BridgeStep");
      call(step, "reset", new Class<?>[]{double.class}, 1.0);
      call(step, "update", new Class<?>[]{double.class, boolean.class, boolean.class}, 1.0, true, true);
      assertEquals(1, call(step, "placementY", new Class<?>[]{}));
      assertEquals(true, call(step, "needsClearance", new Class<?>[]{double.class}, 1.8));
      for (double height : new double[]{1.42, 1.9, 2.25, 2.02}) {
         call(step, "update", new Class<?>[]{double.class, boolean.class, boolean.class}, height, false, false);
         assertEquals(1, call(step, "placementY", new Class<?>[]{}));
      }
      assertEquals(false, call(step, "needsClearance", new Class<?>[]{double.class}, 2.02));
      call(step, "update", new Class<?>[]{double.class, boolean.class, boolean.class}, 2.0, true, false);
      assertEquals(1, call(step, "placementY", new Class<?>[]{}));
      call(step, "update", new Class<?>[]{double.class, boolean.class, boolean.class}, 2.0, true, true);
      assertEquals(2, call(step, "placementY", new Class<?>[]{}));
   }

   @Test void ascendingNeverAddsSneakWhileDeferredPreparingAirborneOrLanding() throws Exception {
      Object step = nested("BridgeStep");
      Class<?>[] sneak = {boolean.class, boolean.class, boolean.class, boolean.class};
      call(step, "reset", new Class<?>[]{double.class}, 1.0);
      // The user's jump remains requested even when the input jump is deferred for support repair.
      assertEquals(false, call(step, "shouldSneak", sneak, false, true, true, true));
      call(step, "update", new Class<?>[]{double.class, boolean.class, boolean.class}, 1.0, true, true);
      for (double height : new double[]{1.0, 1.42, 2.25, 2.02}) {
         call(step, "update", new Class<?>[]{double.class, boolean.class, boolean.class}, height, height == 1.0, true);
         assertEquals(false, call(step, "shouldSneak", sneak, false, true, true, true));
         assertEquals(false, call(step, "shouldSneak", sneak, false, false, true, true));
         assertEquals(true, call(step, "shouldSneak", sneak, true, true, true, true));
      }
      call(step, "update", new Class<?>[]{double.class, boolean.class, boolean.class}, 2.0, true, true);
      assertEquals(false, call(step, "shouldSneak", sneak, false, true, true, false));
      call(step, "update", new Class<?>[]{double.class, boolean.class, boolean.class}, 2.0, true, false);
      assertEquals(true, call(step, "shouldSneak", sneak, false, false, true, false));
   }

   private static Object aim(Vec3 eyes, float yaw, float pitch, float sideYaw, Object previous,
                             Set<BlockPos> path, Map<BlockPos, AABB> blocks) throws Exception {
      return aim(eyes, yaw, pitch, sideYaw, null, null, previous, path, blocks);
   }

   private static BlockHitResult traceBlocks(Vec3 eyes, Vec3 end, Map<BlockPos, AABB> blocks) {
      BlockHitResult closest = null;
      for (var entry : blocks.entrySet()) {
         BlockHitResult hit = AABB.clip(List.of(entry.getValue()), eyes, end, entry.getKey());
         if (hit != null && (closest == null || hit.getLocation().distanceToSqr(eyes) < closest.getLocation().distanceToSqr(eyes))) closest = hit;
      }
      return closest;
   }

   private static Object aim(Vec3 eyes, float yaw, float pitch, float sideYaw, Vec3 offset, Direction face, Object previous,
                             Set<BlockPos> path, Map<BlockPos, AABB> blocks) throws Exception {
      Function<BlockPos, List<AABB>> shapes = pos -> blocks.containsKey(pos) ? List.of(blocks.get(pos)) : List.of();
      Function<Vec3, BlockHitResult> trace = point -> {
         Vec3 end = eyes.add(point.subtract(eyes).normalize().scale(4.5));
         return traceBlocks(eyes, end, blocks);
      };
      BiPredicate<BlockHitResult, BlockPos> valid = (hit, pos) -> hit != null && hit.getType() == HitResult.Type.BLOCK
         && !blocks.containsKey(pos) && hit.getBlockPos().relative(hit.getDirection()).equals(pos);
      Class<?> aimType = Class.forName(Scaffold.class.getName() + "$ClosetAim");
      return invoke("searchFaces", new Class<?>[]{Vec3.class, float.class, float.class, Vec3.class, Direction.class,
         aimType, Set.class, Function.class, Function.class, BiPredicate.class}, eyes, pitch, sideYaw,
         offset, face, previous, path, shapes, trace, valid);
   }

   @Test void upwardAimIsFoundStartingFromAHorizontalView() throws Exception {
      var origin = new BlockPos(0, 0, 0);
      Object aim = aim(new Vec3(0.5, 2.62, 0.5), 0, 0, 135, null,
         Set.of(origin.above()), Map.of(origin, new AABB(0, 0, 0, 1, 1, 1)));
      assertNotNull(aim);
      assertEquals(Direction.UP, call(aim, "face", new Class<?>[]{}));
      assertTrue((float)call(aim, "pitch", new Class<?>[]{}) > 60);
   }

   @Test void diagonalBridgeFindsAnAdjacentStepBeforeTheUnreachableCorner() throws Exception {
      var origin = new BlockPos(0, 0, 0);
      var path = new LinkedHashSet<>(List.of(new BlockPos(1, 0, 1), new BlockPos(1, 0, 0), new BlockPos(0, 0, 1)));
      var blocks = Map.of(origin, new AABB(0, 0, 0, 1, 1, 1));
      Object aim = aim(new Vec3(1.1, 2.62, 1.1), 168, 80, 168, null, path, blocks);
      assertNotNull(aim);
      BlockPos placed = (BlockPos)call(aim, "place", new Class<?>[]{});
      assertTrue(placed.equals(new BlockPos(1, 0, 0)) || placed.equals(new BlockPos(0, 0, 1)));
      assertTrue((float)call(aim, "pitch", new Class<?>[]{}) > 60);
   }

   @Test void straightBridgeKeepsSideFacingWhileTrackingTheSameSupportFace() throws Exception {
      var origin = new BlockPos(0, 0, 0);
      var path = Set.of(new BlockPos(0, 0, 1));
      var blocks = Map.of(origin, new AABB(0, 0, 0, 1, 1, 1));
      Object first = aim(new Vec3(0.5, 2.62, 1.12), 135, 78, 135, null, path, blocks);
      assertNotNull(first);
      assertEquals(0F, Mth.wrapDegrees((float)call(first, "yaw", new Class<?>[]{}) - 135), 0.1);
      Object next = aim(new Vec3(0.5, 2.62, 1.13), 135, 78, 135, first, path, blocks);
      assertNotNull(next);
      assertEquals(call(first, "support", new Class<?>[]{}), call(next, "support", new Class<?>[]{}));
      assertEquals(call(first, "face", new Class<?>[]{}), call(next, "face", new Class<?>[]{}));
      assertEquals(0F, Mth.wrapDegrees((float)call(next, "yaw", new Class<?>[]{}) - 135), 0.01);
      Object stationary = aim(new Vec3(0.5, 2.62, 1.13), 135, 78, 135, next, path, blocks);
      assertEquals(call(next, "point", new Class<?>[]{}), call(stationary, "point", new Class<?>[]{}));
   }

   @Test void diagonalAimKeepsYawWhileThePlayerCrossesAConnectingFace() throws Exception {
      for (int sign : new int[]{1, -1}) {
         var origin = new BlockPos(0, 0, 0);
         var connector = new BlockPos(sign, 0, 0);
         var diagonal = new BlockPos(sign, 0, 1);
         var path = new LinkedHashSet<>(List.of(diagonal, connector, new BlockPos(0, 0, 1)));
         var blocks = Map.of(origin, new AABB(0, 0, 0, 1, 1, 1));
         float fixedYaw = sign > 0 ? 168 : 192;
         Object previous = null;
         for (double distance : new double[]{0.02, 0.05, 0.09, 0.14, 0.19, 0.249}) {
            Vec3 eyes = new Vec3(sign > 0 ? 1 + distance : -distance, 2.27, 1 + distance);
            Object next = aim(eyes, fixedYaw, 75, fixedYaw, previous, path, blocks);
            assertNotNull(next, "connecting face at " + eyes);
            assertEquals(0, Mth.wrapDegrees((float)call(next, "yaw", new Class<?>[]{}) - fixedYaw), 0.01,
               "route yaw changed at " + eyes);
            previous = next;
         }
      }
   }

   private static boolean worldSupport(Vec3 position, Vec3 motion, double margin, Map<BlockPos, AABB> blocks) throws Exception {
      AABB[] shapes = blocks.entrySet().stream().map(entry -> entry.getValue().move(entry.getKey())).toArray(AABB[]::new);
      return supported(position, motion, margin, shapes);
   }

   @Test void straightAndBothDiagonalRoutesKeepPlacingAcrossSuccessiveBlocks() throws Exception {
      for (float camera : new float[]{0, 90, -90, 180}) {
         for (Vec3 start : new Vec3[]{new Vec3(0.5, 1, 0.5), new Vec3(0.35, 1, 0.65), new Vec3(0.65, 1, 0.35), new Vec3(0.1, 1, 0.9), new Vec3(0.9, 1, 0.1)}) {
            for (int strafe : new int[]{0, 1, -1}) {
               Object route = nested("BridgeRoute"), target = null;
               Axis yaw = new Axis(camera), pitch = new Axis(0);
               Vec3 position = start, lastOffset = null;
               Direction lastFace = null;
               Map<BlockPos, AABB> blocks = new HashMap<>();
               blocks.put(BlockPos.ZERO, new AABB(0, 0, 0, 1, 1, 1));
               route(route, camera, 1, strafe, position);
               float sideYaw = (float)call(route, "aimYaw", new Class<?>[]{int.class}, strafe > 0 ? -1 : 1);
               boolean sneaking = false;
               double settledYawMovement = 0;
               for (int tick = 0; tick < 300; tick++) {
                  route(route, camera + (float)(Math.sin(tick * 0.1) * 18), 1, strafe, position);
                  Vec3 direction = (Vec3)call(route, "direction", new Class<?>[]{});
                  Vec3 line = (Vec3)call(route, "linePosition", new Class<?>[]{Vec3.class}, position);
                  @SuppressWarnings("unchecked")
                  Set<BlockPos> path = (Set<BlockPos>)invoke("walkingPath", new Class<?>[]{Vec3.class, Vec3.class, double.class}, line, direction, 0.65);
                  Vec3 eyes = position.add(0, sneaking ? 1.27 : 1.62, 0);
                  target = aim(eyes, yaw.value(), pitch.value(), sideYaw, lastOffset, lastFace, target, path, blocks);
                  if (target == null) target = aim(eyes.add(direction.scale(0.65)), yaw.value(), pitch.value(), sideYaw,
                     lastOffset, lastFace, null, path, blocks);
                  if (target != null) assertEquals(0, Mth.wrapDegrees((float)call(target, "yaw", new Class<?>[]{}) - sideYaw), 0.01,
                     "yaw hunting, strafe=" + strafe + ", tick=" + tick);
                  float previousYaw = yaw.value();
                  yaw.step(target == null ? sideYaw : (float)call(target, "yaw", new Class<?>[]{}), 22, 5, 0.15, true);
                  if (tick > 40) settledYawMovement += Math.abs(Mth.wrapDegrees(yaw.value() - previousYaw));
                  pitch.step(target == null ? pitch.value() : (float)call(target, "pitch", new Class<?>[]{}), 14, 5, 0.15, false);
                  sneaking = !worldSupport(position, direction.scale(0.22), sneaking ? 0.07 : 0.04, blocks);
                  Vec3 motion = vanillaMovement(direction, yaw.value(), sneaking).scale(0.22);
                  if (sneaking) {
                     for (int i = 0; i < 30 && !worldSupport(position, motion, 0.001, blocks); i++) motion = motion.scale(0.8);
                     if (!worldSupport(position, motion, 0.001, blocks)) motion = Vec3.ZERO;
                  }
                  position = position.add(motion);
                  assertTrue(worldSupport(position, Vec3.ZERO, 0.001, blocks), "lost support, strafe=" + strafe + ", tick=" + tick + ", position=" + position);
                  eyes = position.add(0, sneaking ? 1.27 : 1.62, 0);
                  BlockHitResult hit = traceBlocks(eyes, eyes.add(Vec3.directionFromRotation(pitch.value(), yaw.value()).scale(4.5)), blocks);
                  if (target != null && hit != null && hit.getBlockPos().equals(call(target, "support", new Class<?>[]{}))
                      && hit.getDirection() == call(target, "face", new Class<?>[]{})) {
                     BlockPos place = (BlockPos)call(target, "place", new Class<?>[]{});
                     if (place.equals(hit.getBlockPos().relative(hit.getDirection())) && !blocks.containsKey(place)) {
                        blocks.put(place, new AABB(0, 0, 0, 1, 1, 1));
                        lastOffset = ((Vec3)call(target, "point", new Class<?>[]{})).subtract(Vec3.atLowerCornerOf(hit.getBlockPos()));
                        lastFace = hit.getDirection();
                        target = null;
                     }
                  }
               }
               assertTrue(blocks.size() > 10, "too few placements, strafe=" + strafe + ", camera=" + camera + ", start=" + start + ", position=" + position);
               assertTrue(settledYawMovement < 0.3, "continued yaw oscillation: " + settledYawMovement);
               assertTrue(position.subtract(start).horizontalDistance() > 16,
                  "too little continuous progress, strafe=" + strafe + ", camera=" + camera + ", start=" + start + ", position=" + position);
               Vec3 line = (Vec3)call(route, "linePosition", new Class<?>[]{Vec3.class}, position);
               assertEquals(line.x, position.x, 0.001);
               assertEquals(line.z, position.z, 0.001);
            }
         }
      }
   }

   @Test void risingWorksFromAHorizontalViewOnStraightAndDiagonalRoutes() throws Exception {
      for (float camera : new float[]{0, 90, -90, 180}) {
         for (Vec3 start : new Vec3[]{new Vec3(0.5, 1, 0.5), new Vec3(0.35, 1, 0.65), new Vec3(0.65, 1, 0.35), new Vec3(0.1, 1, 0.9), new Vec3(0.9, 1, 0.1)}) {
            for (int strafe : new int[]{0, 1, -1}) {
               Object route = nested("BridgeRoute"), step = nested("BridgeStep"), target = null;
               Axis yaw = new Axis(camera), pitch = new Axis(0);
               Vec3 position = start;
               route(route, camera, 1, strafe, position);
               call(step, "reset", new Class<?>[]{double.class}, position.y);
               Map<BlockPos, AABB> blocks = new HashMap<>();
               blocks.put(BlockPos.ZERO, new AABB(0, 0, 0, 1, 1, 1));
               float sideYaw = (float)call(route, "aimYaw", new Class<?>[]{int.class}, strafe > 0 ? -1 : 1);
               boolean grounded = true;
               double verticalVelocity = 0;
               Vec3 horizontalVelocity = Vec3.ZERO;
               String lastState = "";
               for (int tick = 0; tick < 250; tick++) {
                  final Vec3 feet = position;
                  boolean center = blocks.entrySet().stream().anyMatch(entry -> {
                     AABB box = entry.getValue().move(entry.getKey());
                     return feet.x > box.minX && feet.x < box.maxX && feet.z > box.minZ && feet.z < box.maxZ
                        && Math.abs(feet.y - box.maxY) < 0.01;
                  });
                  Vec3 direction = (Vec3)call(route, "direction", new Class<?>[]{});
                  boolean deferred = grounded && (!center || !(boolean)invoke("canRiseAt", new Class<?>[]{Vec3.class, Vec3.class}, position, direction));
                  call(step, "update", new Class<?>[]{double.class, boolean.class, boolean.class}, position.y, grounded, !deferred);
                  assertEquals(false, call(step, "shouldSneak", new Class<?>[]{boolean.class, boolean.class, boolean.class, boolean.class},
                     false, true, true, false), "auto sneak while ascending at tick " + tick);
                  int layer = (int)call(step, "placementY", new Class<?>[]{});
                  Vec3 line = (Vec3)call(route, "linePosition", new Class<?>[]{Vec3.class}, position);
                  @SuppressWarnings("unchecked")
                  Set<BlockPos> path = (Set<BlockPos>)invoke("walkingPath", new Class<?>[]{Vec3.class, Vec3.class, double.class},
                     new Vec3(line.x, layer + 1, line.z), direction, 0.65);
                  Vec3 eyes = position.add(0, 1.62, 0);
                  target = aim(eyes, yaw.value(), pitch.value(), sideYaw, target, path, blocks);
                  if (target == null) target = aim(eyes.add(direction.scale(0.65)), yaw.value(), pitch.value(), sideYaw, null, path, blocks);
                  if (target != null) {
                     yaw.step((float)call(target, "yaw", new Class<?>[]{}), 22, 5, 0.15, true);
                     pitch.step((float)call(target, "pitch", new Class<?>[]{}), 14, 5, 0.15, false);
                  }
                  BlockHitResult before = traceBlocks(eyes, eyes.add(Vec3.directionFromRotation(pitch.value(), yaw.value()).scale(4.5)), blocks);
                  boolean aligned = target != null && before != null && before.getBlockPos().equals(call(target, "support", new Class<?>[]{}))
                     && before.getDirection() == call(target, "face", new Class<?>[]{});
                  boolean canJump = grounded && !deferred && center && aligned;
                  lastState = "deferred=" + deferred + ", aligned=" + aligned + ", center=" + center + ", yaw=" + yaw.value()
                     + ", pitch=" + pitch.value() + ", target=" + target + ", blocks=" + blocks.keySet();
                  if (canJump) { verticalVelocity = 0.42; grounded = false; }
                  double acceleration = grounded ? 0.1 : 0.02;
                  Vec3 prediction = horizontalVelocity.add(vanillaMovement(direction, yaw.value(), false).scale(acceleration));
                  boolean hold = grounded && !deferred || !deferred && ((boolean)call(step, "needsClearance", new Class<?>[]{double.class}, position.y)
                     || !worldSupport(new Vec3(position.x, layer + 1, position.z), prediction.scale(2), 0.07, blocks))
                     || grounded && deferred && !worldSupport(position, prediction, 0.04, blocks);
                  if (!hold) horizontalVelocity = prediction;
                  else {
                     Vec3 brake = (Vec3)invoke("brakingInput", new Class<?>[]{Vec3.class, double.class}, horizontalVelocity, acceleration);
                     horizontalVelocity = horizontalVelocity.add(vanillaMovement(brake, yaw.value(), false).scale(acceleration));
                  }
                  Vec3 horizontal = horizontalVelocity;
                  double oldY = position.y;
                  position = position.add(horizontal).add(0, grounded ? 0 : verticalVelocity, 0);
                  if (!grounded) {
                     verticalVelocity = (verticalVelocity - 0.08) * 0.98;
                     if (position.y <= oldY) {
                        double landing = Double.NEGATIVE_INFINITY;
                        for (var entry : blocks.entrySet()) {
                           AABB box = entry.getValue().move(entry.getKey());
                           if (position.x + 0.29 > box.minX && position.x - 0.29 < box.maxX
                               && position.z + 0.29 > box.minZ && position.z - 0.29 < box.maxZ
                               && box.maxY <= oldY + 0.001 && box.maxY >= position.y) landing = Math.max(landing, box.maxY);
                        }
                        if (Double.isFinite(landing)) { position = new Vec3(position.x, landing, position.z); grounded = true; verticalVelocity = 0; }
                     }
                  }
                  eyes = position.add(0, 1.62, 0);
                  BlockHitResult hit = traceBlocks(eyes, eyes.add(Vec3.directionFromRotation(pitch.value(), yaw.value()).scale(4.5)), blocks);
                  if (target != null && hit != null && hit.getBlockPos().equals(call(target, "support", new Class<?>[]{}))
                      && hit.getDirection() == call(target, "face", new Class<?>[]{})) {
                     BlockPos place = (BlockPos)call(target, "place", new Class<?>[]{});
                     if (place.equals(hit.getBlockPos().relative(hit.getDirection())) && !blocks.containsKey(place) && position.y >= place.getY() + 1) {
                        blocks.put(place, new AABB(0, 0, 0, 1, 1, 1));
                        target = null;
                     }
                  }
                  horizontalVelocity = horizontalVelocity.scale(grounded ? 0.546 : 0.91);
                  assertTrue(position.y >= 0.99, "fell while rising: strafe=" + strafe + ", tick=" + tick + ", at=" + position);
               }
               assertTrue(position.y >= 10, "insufficient ascent: " + strafe + ", camera=" + camera + ", start=" + start + " at " + position + ", " + lastState);
               assertTrue(position.subtract(start).horizontalDistance() > 5, "failed to move along rising route: " + strafe + " at " + position);
            }
         }
      }
   }
}
