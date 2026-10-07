package com.samsara.module.player;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.event.impl.EventMoveInput;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.BooleanSetting;
import com.samsara.setting.MultiSelectSetting;
import com.samsara.setting.NumberSetting;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class Eagle extends Feature {
   private final NumberSetting edgeMin = new NumberSetting("Edge Distance Min", this, 0.4, 0.01, 1.3, 0.01);
   private final NumberSetting edgeMax = new NumberSetting("Edge Distance Max", this, 0.6, 0.01, 1.3, 0.01);
   private final BooleanSetting conditional = new BooleanSetting("Conditional", this, true);
   private final MultiSelectSetting conditions = new MultiSelectSetting("Conditions", this,
      new String[]{"Left", "Right", "Forwards", "Backwards", "HoldingBlocks", "OnGround", "Sneak"}, List.of("OnGround"));
   private final NumberSetting pitchMin = new NumberSetting("Pitch Min", this, -90, -90, 90, 1);
   private final NumberSetting pitchMax = new NumberSetting("Pitch Max", this, 90, -90, 90, 1);
   private double currentEdgeDistance;
   private double sampledMin;
   private double sampledMax;
   private boolean wasSneaking;
   private boolean sneakCaptured;
   private boolean edgeActive;
   private double releaseDistance;
   private Object world, player;

   public Eagle() {
      super("Eagle", Category.PLAYER);
      this.conditions.setVisible(this.conditional::getValue);
      this.pitchMin.setVisible(this.conditional::getValue);
      this.pitchMax.setVisible(this.conditional::getValue);
      sampleEdgeDistance();
   }

   @Override public int getPriority(Event event) { return event == Events.MOVE_INPUT ? 50 : 0; }

   @Override public void onEnable() {
      onDisable();
      sampleEdgeDistance();
   }

   @Override public void onDisable() {
      this.wasSneaking = this.sneakCaptured = this.edgeActive = false;
      this.world = this.player = null;
   }

   @Override public void onEvent(Event event) {
      if (event != Events.MOVE_INPUT) return;
      if (mc.player == null || mc.level == null) {
         onDisable();
         return;
      }
      if (this.sampledMin != this.edgeMin.getValue() || this.sampledMax != this.edgeMax.getValue()) {
         sampleEdgeDistance();
         this.releaseDistance = Math.max(this.releaseDistance, Math.max(this.sampledMin, this.sampledMax) + .08);
      }
      if (this.world != mc.level || this.player != mc.player) {
         this.edgeActive = this.wasSneaking = this.sneakCaptured = false;
      }
      this.world = mc.level;
      this.player = mc.player;
      boolean originalSneak = mc.options.keyShift.isDown();
      boolean conditionsMet = conditionsMet(Events.MOVE_INPUT, mc.player.getXRot(), mc.player.onGround(),
         isValidBlock(mc.player.getMainHandItem()) || isValidBlock(mc.player.getOffhandItem()));
      boolean permitted = !mc.player.getAbilities().flying && conditionsMet;
      MovementPrediction simulated = simulateMovement(Events.MOVE_INPUT);
      Vec3 nextVelocity = simulated.velocity();
      double lookAhead = simulated.position().add(nextVelocity.x, 0, nextVelocity.z).subtract(mc.player.position()).horizontalDistance();
      if (!this.edgeActive) this.releaseDistance = Math.max(Math.max(this.sampledMin, this.sampledMax), lookAhead) + 0.08;
      boolean active = updateEdgeState(permitted,
         isCloseToEdge(Events.MOVE_INPUT, simulated, this.currentEdgeDistance),
         isCloseToEdge(Events.MOVE_INPUT, simulated, this.releaseDistance));
      Events.MOVE_INPUT.setSneak(applySneak(originalSneak, conditionsMet, active));
   }

   private boolean updateEdgeState(boolean permitted, boolean enterEdge, boolean releaseEdge) {
      // Release beyond every possible sampled threshold, so slowing down cannot restart the same edge.
      this.edgeActive = permitted && (this.edgeActive ? releaseEdge : enterEdge);
      return this.edgeActive;
   }

   private boolean conditionsMet(EventMoveInput input, float pitch, boolean onGround, boolean holdingBlocks) {
      if (!this.conditional.getValue()) return true;
      pitch = Mth.clamp(pitch, -90, 90);
      if (pitch < Math.min(this.pitchMin.getValue(), this.pitchMax.getValue())
          || pitch > Math.max(this.pitchMin.getValue(), this.pitchMax.getValue())) return false;
      for (String condition : this.conditions.selectedValues()) {
         boolean matches = switch (condition) {
            case "Left" -> input.isLeft();
            case "Right" -> input.isRight();
            case "Forwards" -> input.isForward();
            case "Backwards" -> input.isBackward();
            case "HoldingBlocks" -> holdingBlocks;
            case "OnGround" -> onGround;
            case "Sneak" -> input.isSneak();
            default -> false;
         };
         if (!matches) return false;
      }
      return true;
   }

   private boolean applySneak(boolean originalSneak, boolean conditionsMet, boolean active) {
      boolean controlsSneak = this.conditional.getValue() && this.conditions.contains("Sneak");
      if (!controlsSneak || !originalSneak) this.sneakCaptured = false;
      else if (active) this.sneakCaptured = true;
      boolean override = conditionsMet && controlsSneak && (active || this.sneakCaptured);
      boolean sneak = override ? active : originalSneak || active;
      if (sneak) this.wasSneaking = true;
      else if (this.wasSneaking) {
         sampleEdgeDistance();
         this.wasSneaking = false;
      }
      return sneak;
   }

   private void sampleEdgeDistance() {
      this.sampledMin = this.edgeMin.getValue();
      this.sampledMax = this.edgeMax.getValue();
      double min = Math.min(this.sampledMin, this.sampledMax), max = Math.max(this.sampledMin, this.sampledMax);
      this.currentEdgeDistance = min == max ? min : ThreadLocalRandom.current().nextDouble(min, max);
   }

   private boolean isValidBlock(ItemStack stack) {
      if (!(stack.getItem() instanceof BlockItem item)) return false;
      var block = item.getBlock();
      return !(block instanceof FallingBlock) && block != Blocks.TNT && block != Blocks.COBWEB && block != Blocks.NETHER_PORTAL
         && block.defaultBlockState().entityCanStandOnFace(mc.level, BlockPos.ZERO, mc.player, Direction.UP);
   }

   private boolean isCloseToEdge(EventMoveInput input, MovementPrediction simulated, double distance) {
      Vec3 position = mc.player.position();
      Vec3 nextVelocity = simulated.velocity();
      boolean moving = input.isForward() != input.isBackward() || input.isLeft() != input.isRight();
      Vec3 direction = !moving && nextVelocity.horizontalDistanceSqr() > 0.003 * 0.003
         ? new Vec3(nextVelocity.x, 0, nextVelocity.z).normalize()
         : Vec3.directionFromRotation(0, movementYaw(input, mc.player.getYRot()));
      Vec3 from = position.add(0, -0.1, 0);
      Vec3 to = from.add(direction.scale(distance));
      return crossesEdge(from, to, collectSupportBoxes(from, to)) || wouldFallOff(position)
         || wouldFallOff(simulated.position().add(nextVelocity.x, 0, nextVelocity.z));
   }

   static float movementYaw(EventMoveInput input, float facingYaw) {
      float multiplier = 1;
      if (input.isBackward() && !input.isForward()) { facingYaw += 180; multiplier = -0.5F; }
      else if (input.isForward() && !input.isBackward()) multiplier = 0.5F;
      if (input.isLeft() && !input.isRight()) facingYaw -= 90 * multiplier;
      if (input.isRight() && !input.isLeft()) facingYaw += 90 * multiplier;
      return facingYaw;
   }

   private record MovementPrediction(Vec3 position, Vec3 velocity) { }

   private MovementPrediction simulateMovement(EventMoveInput input) {
      float friction = mc.level.getBlockState(mc.player.getBlockPosBelowThatAffectsMyMovement()).getBlock().getFriction();
      boolean water = mc.player.isInWater(), lava = mc.player.isInLava();
      double acceleration = water || lava ? 0.02 : mc.player.onGround()
         ? mc.player.getSpeed() * 0.21600002 / (friction * friction * friction) : input.isSprint() ? 0.026 : 0.02;
      Vec3 motion = mc.player.getDeltaMovement();
      motion = new Vec3(Math.abs(motion.x) < 0.003 ? 0 : motion.x,
         Math.abs(motion.y) < 0.003 ? 0 : motion.y, Math.abs(motion.z) < 0.003 ? 0 : motion.z);
      Vec3 movementInput = new Vec3((input.isLeft() ? 0.98 : 0) - (input.isRight() ? 0.98 : 0), 0,
         (input.isForward() ? 0.98 : 0) - (input.isBackward() ? 0.98 : 0));
      if (movementInput.lengthSqr() > 1) movementInput = movementInput.normalize();
      movementInput = movementInput.scale(acceleration);
      double yaw = Math.toRadians(mc.player.getYRot());
      motion = motion.add(movementInput.x * Math.cos(yaw) - movementInput.z * Math.sin(yaw), 0,
         movementInput.z * Math.cos(yaw) + movementInput.x * Math.sin(yaw));
      AABB box = mc.player.getDimensions(Pose.STANDING).makeBoundingBox(mc.player.position());
      Vec3 moved = collideMovement(motion, box);
      double damping = water ? 0.8 : lava ? 0.5 : mc.player.onGround() ? friction * 0.91 : 0.91;
      if (mc.player.shouldDiscardFriction()) damping = 1;
      Vec3 nextVelocity = new Vec3(moved.x == motion.x ? motion.x * damping : 0,
         moved.y == motion.y ? motion.y : 0, moved.z == motion.z ? motion.z * damping : 0);
      return new MovementPrediction(mc.player.position().add(moved), nextVelocity);
   }

   private Vec3 collideMovement(Vec3 motion, AABB box) {
      Vec3 moved = Entity.collideBoundingBox(mc.player, motion, box, mc.level, List.of());
      double stepHeight = mc.player.maxUpStep();
      if (stepHeight > 0 && (mc.player.onGround() || moved.y != motion.y && motion.y < 0)
          && (moved.x != motion.x || moved.z != motion.z)) {
         Vec3 stepped = Entity.collideBoundingBox(mc.player, new Vec3(motion.x, stepHeight, motion.z), box, mc.level, List.of());
         Vec3 up = Entity.collideBoundingBox(mc.player, new Vec3(0, stepHeight, 0),
            box.expandTowards(motion.x, 0, motion.z), mc.level, List.of());
         Vec3 alternative = Entity.collideBoundingBox(mc.player, new Vec3(motion.x, 0, motion.z), box.move(up), mc.level, List.of()).add(up);
         if (up.y < stepHeight && alternative.horizontalDistanceSqr() > stepped.horizontalDistanceSqr()) stepped = alternative;
         if (stepped.horizontalDistanceSqr() > moved.horizontalDistanceSqr()) {
            moved = stepped.add(Entity.collideBoundingBox(mc.player, new Vec3(0, motion.y - stepped.y, 0),
               box.move(stepped), mc.level, List.of()));
         }
      }
      return moved;
   }

   private boolean wouldFallOff(Vec3 position) {
      AABB box = mc.player.getBoundingBox().move(position.subtract(mc.player.position())).inflate(-0.05, 0, -0.05)
         .move(0, mc.player.fallDistance - mc.player.maxUpStep(), 0);
      return mc.level.noCollision(mc.player, box);
   }

   private List<AABB> collectSupportBoxes(Vec3 from, Vec3 to) {
      var dimensions = mc.player.getDimensions(Pose.STANDING);
      AABB union = dimensions.makeBoundingBox(from).minmax(dimensions.makeBoundingBox(to));
      var min = BlockPos.containing(union.minX - 0.3000001, union.minY - 0.5000001, union.minZ - 0.3000001);
      var max = BlockPos.containing(union.maxX + 0.3000001, union.minY + 0.0000001, union.maxZ + 0.3000001);
      var boxes = new ArrayList<AABB>();
      for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
         for (AABB shape : mc.level.getBlockState(pos).getCollisionShape(mc.level, pos).toAabbs()) {
            boxes.add(new AABB(shape.minX - 0.3, shape.minY - 1, shape.minZ - 0.3,
               shape.maxX + 0.3, shape.maxY + 0.55, shape.maxZ + 0.3).move(pos));
         }
      }
      return boxes;
   }

   static boolean crossesEdge(Vec3 from, Vec3 to, List<AABB> supports) {
      Vec3 delta = to.subtract(from);
      if (delta.lengthSqr() <= 1.0E-12) return false;
      var intervals = new ArrayList<SupportInterval>();
      for (AABB box : supports) {
         SupportInterval interval = supportInterval(from, delta, box);
         if (interval != null) intervals.add(interval);
      }
      intervals.sort(Comparator.comparingDouble(SupportInterval::start));
      double covered = 0;
      // Merge continuous support along the entire segment, including seams between blocks.
      for (SupportInterval interval : intervals) {
         if (interval.start() > covered + 1.0E-7) return true;
         covered = Math.max(covered, interval.end());
         if (covered >= 1 - 1.0E-7) return false;
      }
      return true;
   }

   private record SupportInterval(double start, double end) { }

   private static SupportInterval supportInterval(Vec3 from, Vec3 delta, AABB box) {
      double start = 0, end = 1;
      for (int axis = 0; axis < 3; axis++) {
         double origin = axis == 0 ? from.x : axis == 1 ? from.y : from.z;
         double direction = axis == 0 ? delta.x : axis == 1 ? delta.y : delta.z;
         double min = axis == 0 ? box.minX : axis == 1 ? box.minY : box.minZ;
         double max = axis == 0 ? box.maxX : axis == 1 ? box.maxY : box.maxZ;
         if (Math.abs(direction) < 1.0E-12) {
            if (origin < min || origin > max) return null;
         } else {
            double firstIntersection = (min - origin) / direction, secondIntersection = (max - origin) / direction;
            start = Math.max(start, Math.min(firstIntersection, secondIntersection));
            end = Math.min(end, Math.max(firstIntersection, secondIntersection));
            if (start > end) return null;
         }
      }
      return new SupportInterval(start, end);
   }
}
