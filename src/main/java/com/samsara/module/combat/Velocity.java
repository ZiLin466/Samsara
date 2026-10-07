package com.samsara.module.combat;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.module.FeatureManager;
import com.samsara.setting.BooleanSetting;
import com.samsara.setting.ModeSetting;
import com.samsara.setting.NumberSetting;
import com.samsara.util.TargetFinder;
import com.samsara.util.Wrapper;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundStartConfigurationPacket;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class Velocity extends Feature {
   public final ModeSetting mode = new ModeSetting("Mode", this, "Original", new String[]{"Original", "Reduce", "Delay", "JumpReset"});
   private final Original original = new Original(this, () -> this.mode.is("Original"));
   private final Knockback knockback = new Knockback(this, this.mode::getValue);
   private final JumpReset jumpReset = new JumpReset(this, () -> this.mode.is("JumpReset"), ThreadLocalRandom.current());
   public final List<Packet> delayedPackets = this.original.delayedPackets;
   private String activeMode = "Original";
   private net.minecraft.client.multiplayer.ClientPacketListener connection;
   private net.minecraft.client.multiplayer.ClientLevel world;
   private LocalPlayer player;

   public Velocity() { super("Velocity", Category.COMBAT); }
   public boolean blocksAttacks() { return !this.delayedPackets.isEmpty() || this.knockback.delaying(); }
   public boolean blocksBacktrack() { return !this.mode.is("Original") && this.knockback.blocksBacktrack(); }

   @Override public int getPriority(Event event) { return event == Events.PACKET_RECEIVE ? -10 : 0; }
   private boolean synchronizeState() {
      if (mc.player == null || mc.level == null || mc.getConnection() == null) {
         reset(false); this.jumpReset.clearDamage(); return false;
      }
      if (this.connection != mc.getConnection() || this.world != mc.level || this.player != mc.player) {
         reset(false); this.jumpReset.clearDamage();
         this.connection = mc.getConnection(); this.world = mc.level; this.player = mc.player;
      }
      if (!this.activeMode.equals(this.mode.getValue())) {
         reset(true); this.activeMode = this.mode.getValue();
      }
      return true;
   }
   public void clientTick() {
      if (!synchronizeState()) return;
      this.jumpReset.tick();
      if (isEnabled() && (this.mode.is("Reduce") || this.mode.is("Delay"))) this.knockback.clientTick();
   }
   public void recordDamage(ClientboundDamageEventPacket packet) {
      if (synchronizeState()) {
         this.jumpReset.damage(packet.entityId(), mc.player.getId(), packet.sourceType().is(DamageTypeTags.IS_FALL));
      }
   }
   @Override public void onEvent(Event event) {
      if (!synchronizeState()) return;
      if (event == Events.ROTATION) this.setSuffix(this.mode.getValue());
      if (this.mode.is("Original")) this.original.onEvent(event);
      else if (this.mode.is("JumpReset")) {
         if (event == Events.MOVE_INPUT) this.jumpReset.input();
      } else if (event == Events.PACKET_RECEIVE && !event.isCancelled()) this.knockback.receive(event, Events.PACKET_RECEIVE.getPacket());
      else if (event == Events.ROTATION) {
         this.knockback.rotate();
         if (this.mode.is("Delay")) this.knockback.playerTick();
      } else if (event == Events.POST_MOTION && this.mode.is("Reduce")) this.knockback.playerTick();
      else if (event == Events.MOVE_INPUT) this.knockback.input();
   }
   private void reset(boolean flush) {
      this.original.reset(flush);
      if (flush) this.knockback.flush(); else this.knockback.reset();
      this.jumpReset.reset();
   }
   @Override public void onEnable() { reset(false); }
   @Override public void onDisable() { reset(true); }

   static final class JumpReset {
      private final BooleanSetting byHits, byDelay;
      private final NumberSetting chance, hitsMin, hitsMax, ticksMin, ticksMax;

      private final RandomGenerator random;

      private int limit, hitsUntilJump, ticksUntilJump, fallDamageTicks;

      JumpReset(Feature owner, BooleanSupplier visible, RandomGenerator random) {
         this.random = random;
         this.chance = new NumberSetting("Chance", owner, 100, 0, 100, 1);
         this.byHits = new BooleanSetting("Jump By Received Hits", owner, false);
         this.hitsMin = new NumberSetting("Hits Until Jump Min", owner, 2, 0, 10, 1);
         this.hitsMax = new NumberSetting("Hits Until Jump Max", owner, 2, 0, 10, 1);
         this.byDelay = new BooleanSetting("Jump By Delay", owner, true);
         this.ticksMin = new NumberSetting("Ticks Until Jump Min", owner, 2, 0, 20, 1);
         this.ticksMax = new NumberSetting("Ticks Until Jump Max", owner, 2, 0, 20, 1);
         for (var setting : List.of(this.chance, this.byHits, this.byDelay)) setting.setVisible(visible);
         this.hitsMin.setVisible(() -> visible.getAsBoolean() && this.byHits.getValue());
         this.hitsMax.setVisible(() -> visible.getAsBoolean() && this.byHits.getValue());
         this.ticksMin.setVisible(() -> visible.getAsBoolean() && this.byDelay.getValue());
         this.ticksMax.setVisible(() -> visible.getAsBoolean() && this.byDelay.getValue());
         reset();
      }

      void input() {
         if (jump(mc.player.hurtTime, mc.player.onGround(), mc.player.isSprinting())) Events.MOVE_INPUT.setJump(true);
      }

      boolean jump(int hurtTime, boolean onGround, boolean sprinting) {
         boolean ready = this.byHits.getValue() ? this.limit >= this.hitsUntilJump
            : !this.byDelay.getValue() || this.limit >= this.ticksUntilJump;
         if (hurtTime != 9 || !onGround || !sprinting || this.fallDamageTicks > 0 || !ready || !chance()) {
            if (!this.byHits.getValue() || hurtTime == 9) this.limit++;
            return false;
         }
         this.limit = 0;
         sampleLimits();
         return true;
      }

      private boolean chance() {
         double value = this.chance.getValue();
         return value >= 100 || value > 0 && this.random.nextFloat(100) < value;
      }

      void damage(int entityId, int localId, boolean fall) {
         if (entityId == localId) this.fallDamageTicks = fall ? 10 : 0;
      }

      void tick() { if (this.fallDamageTicks > 0) this.fallDamageTicks--; }

      void clearDamage() { this.fallDamageTicks = 0; }

      void reset() {
         this.limit = 0;
         sampleLimits();
      }

      private void sampleLimits() {
         this.hitsUntilJump = sample(this.hitsMin, this.hitsMax);
         this.ticksUntilJump = sample(this.ticksMin, this.ticksMax);
      }

      private int sample(NumberSetting first, NumberSetting second) {
         int min = (int)Math.min(first.getValue(), second.getValue()), max = (int)Math.max(first.getValue(), second.getValue());
         return min == max ? min : this.random.nextInt(min, max + 1);
      }
   }

   private static final class Original implements com.samsara.util.Wrapper {
      private static final String DISABLED_LABEL = "Disabled";
      private static final String REDUCE_TICKS_LABEL = "Reduce Ticks";
      private static final String REDUCE_LABEL = "Reduce";
      private static final String VERTICAL_LABEL = "Vertical";
      private static final String REVERSE_LABEL = "Reverse";
      private static final String DELAY_LABEL = "Delay";
      private static final String DELAY_RANGE_LABEL = "Delay Range";
      private static final String HORIZONTAL_LABEL = "Horizontal";
      private static final String JUMP_RESET_LABEL = "Jump Reset";
      private static final String DELAY_UNTIL_GROUND_LABEL = "Delay Until Ground";
      private static final String DELAY_TICKS_LABEL = "Delay Ticks";
      private static final String NORMAL_LABEL = "Normal";
      private static final String REDUCE_MOTION_LABEL = "Reduce Motion";
      private static final String AIR_PUSH_LABEL = "AirPush";

      private final NumberSetting horizontal;
      private final NumberSetting vertical;
      private final NumberSetting reduceMotion;
      private final NumberSetting reduceTicks;
      private final ModeSetting reduce;
      private final BooleanSetting jumpReset;
      private final BooleanSetting delay;
      private final NumberSetting delayTicks;
      private final NumberSetting delayRange;
      private final BooleanSetting delayUntilGround;
      private final BooleanSetting reverse;

      public final List<Packet> delayedPackets;

      private int delayTicksElapsed;
      private int reductionTicks;
      private int lastAttackTick;
      private boolean pendingJump;
      private boolean suppressActionPackets;

      public void onEvent(Event event) {
         if (event == Events.POST_MOVE_INPUT && mc.player.onGround() && this.pendingJump) {
            mc.player.input.makeJump();
            this.pendingJump = false;
         }

         if (event == Events.ROTATION) {
            if (!this.delayedPackets.isEmpty()) {
               this.delayTicksElapsed++;
               if ((double)this.delayTicksElapsed > this.delayTicks.getValue() || this.delayUntilGround.getValue() && mc.player.onGround()) {
                  this.flushDelayedPackets();
               }
            }

            if (this.reductionTicks >= 0 && (double)this.reductionTicks < this.reduceTicks.getValue() && mc.gui.screen() == null) {
               this.reductionTicks++;
               Vec3 motion = mc.player.getDeltaMovement();
               double motionMultiplier = this.reduceMotion.getValue();
               if (this.reduce.is(NORMAL_LABEL)) {
                  LivingEntity nearbyTarget = TargetFinder.nearestTarget(3.0, false);
                  if (nearbyTarget != null && nearbyTarget instanceof Player && this.lastAttackTick != mc.player.tickCount && this.delayedPackets.isEmpty() && nearbyTarget.isAlive()) {
                     Vec3 playerEye = mc.player.getEyePosition();
                     Vec3 targetEye = nearbyTarget.getEyePosition();
                     double deltaX = targetEye.x - playerEye.x;
                     double deltaY = targetEye.y - playerEye.y;
                     double deltaZ = targetEye.z - playerEye.z;
                     double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
                     float yaw = (float)(Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0);
                     float pitch = (float)(-Math.toDegrees(Math.atan2(deltaY, horizontalDistance)));
                     Events.ROTATION.setYaw(yaw);
                     Events.ROTATION.setPitch(pitch);
                     this.attackTarget(nearbyTarget);
                  }
               } else if (this.reduce.is(AIR_PUSH_LABEL)) {
                  LivingEntity airPushTarget = TargetFinder.farthestDistantPlayer(false);
                  if (airPushTarget != null && this.lastAttackTick != mc.player.tickCount && this.delayedPackets.isEmpty() && airPushTarget.isAlive()) {
                     this.attackTarget(airPushTarget);
                  } else {
                     airPushTarget = TargetFinder.nearestTarget(3.0, false);
                     if (airPushTarget != null && airPushTarget instanceof Player && this.lastAttackTick != mc.player.tickCount && this.delayedPackets.isEmpty() && airPushTarget.isAlive()) {
                        Vec3 playerEye = mc.player.getEyePosition();
                        Vec3 targetEye = airPushTarget.getEyePosition();
                        double deltaX = targetEye.x - playerEye.x;
                        double deltaY = targetEye.y - playerEye.y;
                        double deltaZ = targetEye.z - playerEye.z;
                        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
                        float yaw = (float)(Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0);
                        float pitch = (float)(-Math.toDegrees(Math.atan2(deltaY, horizontalDistance)));
                        Events.ROTATION.setYaw(yaw);
                        Events.ROTATION.setPitch(pitch);
                        this.attackTarget(airPushTarget);
                     }
                  }
               } else {
                  mc.player.setDeltaMovement(motion.x * motionMultiplier, motion.y, motion.z * motionMultiplier);
               }
            }
         }

         if (event == Events.PACKET_SEND) {
            if (Events.PACKET_SEND.getPacket() instanceof ServerboundAttackPacket) {
               this.lastAttackTick = mc.player.tickCount;
            }

            if (Events.PACKET_SEND.getPacket() instanceof ServerboundPlayerActionPacket && this.suppressActionPackets) {
               event.setCancelled(true);
            }
         }

         if (event == Events.PACKET_RECEIVE) {
            Packet packet = Events.PACKET_RECEIVE.getPacket();
            if (packet instanceof ClientboundSetEntityMotionPacket setEntityMotionPacket) {
               if (setEntityMotionPacket.id() == mc.player.getId()) {
                  Vec3 knockback = setEntityMotionPacket.movement();
                  if (knockback.y > 0.0 && !FeatureManager.longJump.isEnabled()) {
                     double horizontalMultiplier = this.reverse.getValue() ? -this.horizontal.getValue() * 0.01 : this.horizontal.getValue() * 0.01;
                     double knockbackX = knockback.x * horizontalMultiplier;
                     double knockbackY = knockback.y * this.vertical.getValue() * 0.01;
                     double knockbackZ = knockback.z * horizontalMultiplier;
                     if (this.horizontal.getValue() != 100.0 || this.reverse.getValue()) {
                        event.setCancelled(true);
                     }

                     if (this.reduceTicks.getValue() != 0.0) {
                        this.reductionTicks = 0;
                     }

                     if (this.delay.getValue() && this.shouldDelayKnockback()) {
                        this.delayedPackets.add(setEntityMotionPacket);
                        event.setCancelled(true);
                     } else {
                        if (this.horizontal.getValue() != 100.0 || this.reverse.getValue()) {
                           mc.player.setDeltaMovement(new Vec3(knockbackX, knockbackY, knockbackZ));
                        }

                        if (this.jumpReset.getValue() && mc.player.onGround() && mc.player.isSprinting()) {
                           this.pendingJump = true;
                        }
                     }
                  }
               }
            } else if (this.delay.getValue() && !this.delayedPackets.isEmpty()) {
               if (packet instanceof ClientboundStartConfigurationPacket || packet instanceof ClientboundDisconnectPacket) {
                  this.flushDelayedPackets();
                  return;
               }

               synchronized (this.delayedPackets) {
                  this.delayedPackets.add(packet);
                  event.setCancelled(true);
               }
            }
         }
      }

      void reset(boolean flush) {
         if (flush && mc.getConnection() != null) this.flushDelayedPackets(); else this.delayedPackets.clear();
         this.reductionTicks = -1; this.delayTicksElapsed = 0; this.pendingJump = this.suppressActionPackets = false;
      }

      private boolean shouldDelayKnockback() {
         LivingEntity nearbyTarget = TargetFinder.nearestTarget(6.0, false);
         LivingEntity distantTarget = TargetFinder.farthestDistantPlayer(false);
         return (!mc.player.onGround() || !this.delayUntilGround.getValue())
            && (nearbyTarget == null || (double)mc.player.distanceTo(nearbyTarget) > this.delayRange.getValue())
            && (!this.reduce.is(AIR_PUSH_LABEL) || !mc.player.isSprinting() && (distantTarget == null || !(mc.player.distanceTo(distantTarget) > 11.0F)));
      }

      private void attackTarget(Entity entity) {
         if (!FeatureManager.bedAura.isEnabled() || !FeatureManager.bedAura.rotatingToBed) {
            this.suppressActionPackets = true;
            mc.gameMode.attack(mc.player, entity);
            mc.player.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, false);
            this.suppressActionPackets = false;
         }
      }

      private void flushDelayedPackets() {
         if (!this.delayedPackets.isEmpty()) {
            synchronized (this.delayedPackets) {
               for (Packet packet : this.delayedPackets) {
                  mc.execute(() -> packet.handle(mc.getConnection()));
               }

               this.delayedPackets.clear();
            }

            this.delayTicksElapsed = 0;
         }
      }

      Original(Feature owner, java.util.function.BooleanSupplier visible) {
         this.horizontal = new NumberSetting(HORIZONTAL_LABEL, owner, 100.0, 0.0, 100.0, 5.0);
         this.vertical = new NumberSetting(VERTICAL_LABEL, owner, 100.0, 0.0, 100.0, 5.0);
         this.reduceMotion = new NumberSetting(REDUCE_MOTION_LABEL, owner, 1.0, 0.0, 1.0, 0.1);
         this.reduceTicks = new NumberSetting(REDUCE_TICKS_LABEL, owner, 0.0, 0.0, 5.0, 1.0);
         this.reduce = new ModeSetting(REDUCE_LABEL, owner, DISABLED_LABEL, new String[]{DISABLED_LABEL, NORMAL_LABEL, AIR_PUSH_LABEL});
         this.jumpReset = new BooleanSetting(JUMP_RESET_LABEL, owner, false);
         this.delay = new BooleanSetting(DELAY_LABEL, owner, false);
         this.delayTicks = new NumberSetting(DELAY_TICKS_LABEL, owner, 4.0, 1.0, 15.0, 1.0);
         this.delayRange = new NumberSetting(DELAY_RANGE_LABEL, owner, 3.0, 1.0, 4.0, 0.5);
         this.delayUntilGround = new BooleanSetting(DELAY_UNTIL_GROUND_LABEL, owner, false);
         this.reverse = new BooleanSetting(REVERSE_LABEL, owner, false);
         this.delayedPackets = new ArrayList<>();
         this.reductionTicks = -1;
         for (var setting : owner.settings) if (!setting.getName().equals("Mode")) setting.setVisible(visible);
         this.delayTicks.setVisible(() -> visible.getAsBoolean() && this.delay.getValue());
         this.delayRange.setVisible(() -> visible.getAsBoolean() && this.delay.getValue());
         this.delayUntilGround.setVisible(() -> visible.getAsBoolean() && this.delay.getValue());
      }
   }

   private static final class Knockback implements Wrapper {
      private static final long CORRECTION_GRACE_MILLIS = 100;
      private static final double ATTACK_REACH = 3;
      private static final double ATTACK_REACH_SQUARED = ATTACK_REACH * ATTACK_REACH;
      private static final int JUMP_RESET_HURT_TICK = 9;

      private final NumberSetting reduceAttackCount;
      private final NumberSetting reduceSprintTicks;
      private final NumberSetting maxReduceDelay;
      private final BooleanSetting swingOnReduceAttack;

      private final NumberSetting delayModeTicks;
      private final BooleanSetting delayModeJumpReset;

      private final Supplier<String> mode;
      private final Object packetLock = new Object();
      private final Queue<Packet<?>> reducePackets = new ArrayDeque<>();
      private final DelayQueue<Packet<?>> delayQueue = new DelayQueue<>();
      private ClientPacketListener packetListener;

      private volatile long lastReduceCorrectionTime;
      private volatile long reduceDelayStartedAt;
      private volatile int remainingReduceAttacks;
      private volatile int remainingSprintTicks;
      private volatile float knockbackYaw;
      private volatile boolean reduceDelayActive;
      private volatile boolean reduceJumpPending;

      private volatile long lastDelayCorrectionTime;

      Knockback(Feature owner, Supplier<String> mode) {
         this.mode = mode;
         this.reduceAttackCount = new NumberSetting("Attack Counts", owner, 1, 1, 5, 1);
         this.reduceSprintTicks = new NumberSetting("Sprint Ticks", owner, 3, 1, 5, 1);
         this.maxReduceDelay = new NumberSetting("Max Delay", owner, 1000, 0, 1000, 50);
         this.swingOnReduceAttack = new BooleanSetting("Swing Hand", owner, false);
         this.delayModeTicks = new NumberSetting("Delay Mode Ticks", owner, 3, 1, 5, 1);
         this.delayModeTicks.setDisplayName("Delay Ticks");
         this.delayModeJumpReset = new BooleanSetting("Delay Mode Jump Reset", owner, false);
         this.delayModeJumpReset.setDisplayName("Jump Reset");
         for (var setting : List.of(this.reduceAttackCount, this.reduceSprintTicks, this.maxReduceDelay, this.swingOnReduceAttack)) {
            setting.setVisible(() -> mode.get().equals("Reduce"));
         }
         this.delayModeTicks.setVisible(() -> mode.get().equals("Delay"));
         this.delayModeJumpReset.setVisible(() -> mode.get().equals("Delay"));
      }

      boolean delaying() { return this.reduceDelayActive || this.delayQueue.active(); }
      boolean blocksBacktrack() { return delaying() || this.remainingReduceAttacks > 0; }

      void receive(Event event, Packet<?> packet) {
         synchronized (this.packetLock) {
            if (packet instanceof ClientboundDisconnectPacket || packet instanceof ClientboundStartConfigurationPacket
               || packet instanceof ClientboundLoginPacket || packet instanceof ClientboundRespawnPacket) { reset(); return; }
            if (this.packetListener != null && this.packetListener != mc.getConnection()) reset();
            this.packetListener = mc.getConnection();
            if (this.mode.get().equals("Reduce")) receiveReducePacket(event, packet); else receiveDelayPacket(event, packet);
         }
      }

      private void receiveReducePacket(Event event, Packet<?> packet) {
         if (this.reduceDelayActive && (packet instanceof ClientboundSetEntityMotionPacket || packet instanceof ClientboundMoveEntityPacket
            || packet instanceof ClientboundTeleportEntityPacket || packet instanceof ClientboundPingPacket
            || packet instanceof ClientboundPlayerLookAtPacket || packet instanceof ClientboundPlayerPositionPacket)) {
            event.setCancelled(true); this.reducePackets.add(packet);
         }
         if (packet instanceof ClientboundPlayerPositionPacket || packet instanceof ClientboundExplodePacket) this.lastReduceCorrectionTime = System.currentTimeMillis();
         if (packet instanceof ClientboundSetEntityMotionPacket motion && motion.id() == mc.player.getId()) {
            var movement = motion.movement();
            if (System.currentTimeMillis() - this.lastReduceCorrectionTime >= CORRECTION_GRACE_MILLIS
               && hasUpwardHorizontalMotion(movement.x, movement.y, movement.z) && !this.reduceDelayActive) {
               this.reduceDelayActive = true;
               event.setCancelled(true);
               this.reducePackets.add(packet);
               this.reduceDelayStartedAt = System.currentTimeMillis();
            }
            this.knockbackYaw = Mth.wrapDegrees((float)(Math.toDegrees(Math.atan2(movement.z, movement.x)) + 90));
         }
      }

      private void receiveDelayPacket(Event event, Packet<?> packet) {
         if (packet instanceof ClientboundPlayerPositionPacket || packet instanceof ClientboundExplodePacket) this.lastDelayCorrectionTime = System.currentTimeMillis();
         if (this.delayQueue.offer(packet)) { event.setCancelled(true); return; }
         if (packet instanceof ClientboundSetEntityMotionPacket motion && motion.id() == mc.player.getId()
            && System.currentTimeMillis() - this.lastDelayCorrectionTime >= CORRECTION_GRACE_MILLIS) {
            var movement = motion.movement();
            // Apply the initiating motion immediately; delay subsequent inbound packets.
            if (hasUpwardHorizontalMotion(movement.x, movement.y, movement.z)) this.delayQueue.start((int)this.delayModeTicks.getValue());
         }
      }

      static boolean hasUpwardHorizontalMotion(double x, double y, double z) { return y > 0 && (x != 0 || z != 0); }

      void clientTick() {
         if (!this.mode.get().equals("Reduce")) return;
         if (this.reduceDelayActive && (mc.player.onGround() || System.currentTimeMillis() - this.lastReduceCorrectionTime < CORRECTION_GRACE_MILLIS
            || System.currentTimeMillis() - this.reduceDelayStartedAt >= this.maxReduceDelay.getValue()) && flushReducePackets()) {
            if (System.currentTimeMillis() - this.lastReduceCorrectionTime >= CORRECTION_GRACE_MILLIS) {
               this.remainingReduceAttacks = (int)this.reduceAttackCount.getValue();
               this.remainingSprintTicks = (int)this.reduceSprintTicks.getValue();
            }
         }
      }

      void rotate() {
         if (this.mode.get().equals("Reduce") && this.remainingSprintTicks >= 1
            && FeatureManager.killAura.target == null && !FeatureManager.scaffold.isEnabled()) {
            Events.ROTATION.setYaw(this.knockbackYaw);
         }
      }

      void playerTick() {
         if (this.mode.get().equals("Delay")) {
            List<Packet<?>> pending = List.of();
            synchronized (this.packetLock) {
               pending = this.delayQueue.tick();
            }
            replayPackets(pending);
         } else if (this.mode.get().equals("Reduce") && this.remainingReduceAttacks >= 1) {
            if (mc.gui.screen() != null || !mc.isWindowActive()) {
               this.remainingReduceAttacks = 0;
               return;
            }
            Player target = findAttackTarget();
            if (target != null && target.isAlive() && !FeatureManager.bedAura.rotatingToBed) {
               mc.gameMode.attack(mc.player, target);
               if (this.swingOnReduceAttack.getValue()) {
                  mc.player.swing(InteractionHand.MAIN_HAND, mc.player.getMainHandItem().getAttackAnimation(), false);
                  mc.getConnection().send(ServerboundPunchPacket.INSTANCE);
               }
            }
            this.remainingReduceAttacks--;
         }
      }

      private Player findAttackTarget() {
         var origin = mc.player.getEyePosition();
         var direction = net.minecraft.world.phys.Vec3.directionFromRotation(Events.ROTATION.getPitch(), Events.ROTATION.getYaw());
         var end = origin.add(direction.scale(ATTACK_REACH));
         var hit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(mc.player, origin, end,
            mc.player.getBoundingBox().expandTowards(direction.scale(ATTACK_REACH)).inflate(1),
            entity -> entity instanceof Player && FeatureManager.targets.shouldAttack(entity), ATTACK_REACH_SQUARED);
         if (hit == null || !(hit.getEntity() instanceof Player player)) return null;
         var block = mc.level.clip(new net.minecraft.world.level.ClipContext(origin, hit.getLocation(),
            net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, mc.player));
         return block.getType() == net.minecraft.world.phys.HitResult.Type.MISS ? player : null;
      }

      void input() {
         if (this.mode.get().equals("Delay")) {
            if (this.delayModeJumpReset.getValue() && mc.player.onGround() && mc.player.hurtTime == JUMP_RESET_HURT_TICK) Events.MOVE_INPUT.setJump(true);
            return;
         }
         if (this.reduceDelayActive && mc.player.fallDistance == 0 && findAttackTarget() != null
            && System.currentTimeMillis() - this.lastReduceCorrectionTime >= CORRECTION_GRACE_MILLIS) pressForward();
         if (this.reduceJumpPending && mc.player.onGround()) {
            if (System.currentTimeMillis() - this.lastReduceCorrectionTime >= CORRECTION_GRACE_MILLIS) { Events.MOVE_INPUT.setJump(true); pressForward(); }
            this.reduceJumpPending = false;
         }
         if (this.remainingSprintTicks-- >= 1 && FeatureManager.killAura.target == null && !FeatureManager.scaffold.isEnabled()
            && System.currentTimeMillis() - this.lastReduceCorrectionTime >= CORRECTION_GRACE_MILLIS) pressForward();
      }

      private static void pressForward() {
         Events.MOVE_INPUT.setForward(true);
         Events.MOVE_INPUT.setBackward(false);
         Events.MOVE_INPUT.setLeft(false);
         Events.MOVE_INPUT.setRight(false);
      }

      private boolean flushReducePackets() {
         List<Packet<?>> pending;
         synchronized (this.packetLock) {
            if (!this.reduceDelayActive) return false;
            this.reduceDelayActive = false; pending = drainPackets(this.reducePackets);
         }
         replayPackets(pending); this.reduceJumpPending = true; return true;
      }

      void flush() {
         List<Packet<?>> pending;
         synchronized (this.packetLock) {
            this.reduceDelayActive = false;
            pending = drainPackets(this.reducePackets); pending.addAll(this.delayQueue.drain());
         }
         replayPackets(pending); reset();
      }

      void reset() {
         synchronized (this.packetLock) {
            this.reducePackets.clear();
            this.delayQueue.reset();
            this.reduceDelayActive = this.reduceJumpPending = false;
            this.remainingReduceAttacks = this.remainingSprintTicks = 0;
            this.lastReduceCorrectionTime = this.lastDelayCorrectionTime = this.reduceDelayStartedAt = 0;
            this.knockbackYaw = 0;
            this.packetListener = null;
         }
      }

      private static List<Packet<?>> drainPackets(Queue<Packet<?>> queue) {
         var result = new ArrayList<Packet<?>>(queue); queue.clear(); return result;
      }
      @SuppressWarnings({"rawtypes", "unchecked"})
      private void replayPackets(List<Packet<?>> pending) {
         ClientPacketListener captured = this.packetListener;
         var level = mc.level;
         if (captured == null || captured != mc.getConnection()) return;
         mc.execute(() -> {
            if (captured != mc.getConnection() || level != mc.level) return;
            for (Packet packet : pending) packet.handle(captured);
         });
      }
   }

   static final class DelayQueue<P> {
      private final List<P> packets = new ArrayList<>();
      private int remaining;
      public synchronized void start(int ticks) { this.remaining = ticks; }
      public synchronized boolean active() { return this.remaining > 0; }
      public synchronized boolean offer(P packet) {
         if (!active()) return false;
         this.packets.add(packet); return true;
      }
      public synchronized List<P> tick() {
         if (this.remaining <= 0 || --this.remaining > 0) return List.of();
         return drain();
      }
      public synchronized List<P> drain() {
         var pending = new ArrayList<>(this.packets); this.packets.clear(); this.remaining = 0; return pending;
      }
      public synchronized void reset() { this.packets.clear(); this.remaining = 0; }
   }
}
