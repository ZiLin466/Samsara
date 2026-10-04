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
   private final Original original = new Original(this, () -> this.mode.m228("Original"));
   private final Knockback knockback = new Knockback(this, this.mode::m224);
   private final JumpReset jumpReset = new JumpReset(this, () -> this.mode.m228("JumpReset"), ThreadLocalRandom.current());
   public final List<Packet> f18 = this.original.f18;
   private String activeMode = "Original";
   private net.minecraft.client.multiplayer.ClientPacketListener connection;
   private net.minecraft.client.multiplayer.ClientLevel world;
   private LocalPlayer player;

   public Velocity() { super("Velocity", Category.COMBAT); }
   public boolean blocksAttacks() { return !this.f18.isEmpty() || this.knockback.delaying(); }
   public boolean blocksBacktrack() { return !this.mode.m228("Original") && this.knockback.blocksBacktrack(); }

   @Override public int getPriority(Event event) { return event == Events.f11 ? -10 : 0; }
   private boolean synchronizeState() {
      if (mc.player == null || mc.level == null || mc.getConnection() == null) {
         reset(false); this.jumpReset.clearDamage(); return false;
      }
      if (this.connection != mc.getConnection() || this.world != mc.level || this.player != mc.player) {
         reset(false); this.jumpReset.clearDamage();
         this.connection = mc.getConnection(); this.world = mc.level; this.player = mc.player;
      }
      if (!this.activeMode.equals(this.mode.m224())) {
         reset(true); this.activeMode = this.mode.m224();
      }
      return true;
   }
   public void clientTick() {
      if (!synchronizeState()) return;
      this.jumpReset.tick();
      if (isEnabled() && (this.mode.m228("Reduce") || this.mode.m228("Delay"))) this.knockback.clientTick();
   }
   public void recordDamage(ClientboundDamageEventPacket packet) {
      if (synchronizeState()) {
         this.jumpReset.damage(packet.entityId(), mc.player.getId(), packet.sourceType().is(DamageTypeTags.IS_FALL));
      }
   }
   @Override public void onEvent(Event event) {
      if (!synchronizeState()) return;
      if (event == Events.f3) this.setSuffix(this.mode.m224());
      if (this.mode.m228("Original")) this.original.onEvent(event);
      else if (this.mode.m228("JumpReset")) {
         if (event == Events.f7) this.jumpReset.input();
      } else if (event == Events.f11 && !event.isCancelled()) this.knockback.receive(event, Events.f11.m41());
      else if (event == Events.f3) {
         this.knockback.rotate();
         if (this.mode.m228("Delay")) this.knockback.playerTick();
      } else if (event == Events.f2 && this.mode.m228("Reduce")) this.knockback.playerTick();
      else if (event == Events.f7) this.knockback.input();
   }
   private void reset(boolean flush) {
      this.original.reset(flush);
      if (flush) this.knockback.flush(); else this.knockback.reset();
      this.jumpReset.reset();
   }
   @Override public void onEnable() { reset(false); }
   @Override public void onDisable() { reset(true); }

   static final class JumpReset {
      private final NumberSetting chance, hitsMin, hitsMax, ticksMin, ticksMax;
      private final BooleanSetting byHits, byDelay;
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
         this.hitsMin.setVisible(() -> visible.getAsBoolean() && this.byHits.m215());
         this.hitsMax.setVisible(() -> visible.getAsBoolean() && this.byHits.m215());
         this.ticksMin.setVisible(() -> visible.getAsBoolean() && this.byDelay.m215());
         this.ticksMax.setVisible(() -> visible.getAsBoolean() && this.byDelay.m215());
         reset();
      }

      void input() {
         if (jump(mc.player.hurtTime, mc.player.onGround(), mc.player.isSprinting())) Events.f7.m59(true);
      }

      boolean jump(int hurtTime, boolean onGround, boolean sprinting) {
         boolean ready = this.byHits.m215() ? this.limit >= this.hitsUntilJump
            : !this.byDelay.m215() || this.limit >= this.ticksUntilJump;
         if (hurtTime != 9 || !onGround || !sprinting || this.fallDamageTicks > 0 || !ready || !chance()) {
            if (!this.byHits.m215() || hurtTime == 9) this.limit++;
            return false;
         }
         this.limit = 0;
         sampleLimits();
         return true;
      }

      private boolean chance() {
         double value = this.chance.m220();
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
         int min = (int)Math.min(first.m220(), second.m220()), max = (int)Math.max(first.m220(), second.m220());
         return min == max ? min : this.random.nextInt(min, max + 1);
      }
   }

   private static final class Original implements com.samsara.util.Wrapper {
      private final ModeSetting f335;
      private static final String f321 = "Disabled";
      private static final String f319 = "Reduce Ticks";
      private final NumberSetting f338;
      private static final String f320 = "Reduce";
      private final BooleanSetting f337;
      private final NumberSetting f331;
      private static final String f317 = "Vertical";
      private static final String f329 = "Reverse";
      private int f343;
      private static final String f325 = "Delay";
      private final BooleanSetting f336;
      private static final String f330 = "Jump";
      private final NumberSetting f333;
      private final BooleanSetting f340;
      private static final String f327 = "Delay Range";
      private static final String f316 = "Horizontal";
      private int f342;
      private static final String f324 = "Jump Reset";
      public final List<Packet> f18;
      private static final String f328 = "Delay Until Ground";
      private static final String f326 = "Delay Ticks";
      private final NumberSetting f339;
      private static final String f322 = "Normal";
      private final BooleanSetting f341;
      private static final String f318 = "Reduce Motion";
      private int f346;
      private final NumberSetting f332;
      private static final String f323 = "AirPush";
      private boolean f344;
      private final NumberSetting f334;
      private boolean f345;
      private static final String f315 = "Velocity";

      public void onEvent(Event var1) {
         if (var1 == Events.f8 && mc.player.onGround() && this.f344) {
            mc.player.input.makeJump();
            this.f344 = false;
         }

         if (var1 == Events.f3) {
            if (!this.f18.isEmpty()) {
               this.f343++;
               if ((double)this.f343 > this.f338.m220() || this.f340.m215() && mc.player.onGround()) {
                  this.pm$76();
               }
            }

            if (this.f342 >= 0 && (double)this.f342 < this.f334.m220() && mc.gui.screen() == null) {
               this.f342++;
               Vec3 var2 = mc.player.getDeltaMovement();
               double var3 = this.f333.m220();
               if (this.f335.m228(f322)) {
                  LivingEntity var5 = TargetFinder.m47(3.0, false);
                  if (var5 != null && var5 instanceof Player && this.f346 != mc.player.tickCount && this.f18.isEmpty() && var5.isAlive()) {
                     Vec3 var6 = mc.player.getEyePosition();
                     Vec3 var7 = var5.getEyePosition();
                     double var8 = var7.x - var6.x;
                     double var10 = var7.y - var6.y;
                     double var12 = var7.z - var6.z;
                     double var14 = Math.sqrt(var8 * var8 + var12 * var12);
                     float var16 = (float)(Math.toDegrees(Math.atan2(var12, var8)) - 90.0);
                     float var17 = (float)(-Math.toDegrees(Math.atan2(var10, var14)));
                     Events.f3.m77(var16);
                     Events.f3.m83(var17);
                     this.pm$74(var5);
                  }
               } else if (this.f335.m228(f323)) {
                  LivingEntity var23 = TargetFinder.m49(false);
                  if (var23 != null && this.f346 != mc.player.tickCount && this.f18.isEmpty() && var23.isAlive()) {
                     this.pm$74(var23);
                  } else {
                     var23 = TargetFinder.m47(3.0, false);
                     if (var23 != null && var23 instanceof Player && this.f346 != mc.player.tickCount && this.f18.isEmpty() && var23.isAlive()) {
                        Vec3 var26 = mc.player.getEyePosition();
                        Vec3 var27 = var23.getEyePosition();
                        double var29 = var27.x - var26.x;
                        double var30 = var27.y - var26.y;
                        double var31 = var27.z - var26.z;
                        double var32 = Math.sqrt(var29 * var29 + var31 * var31);
                        float var33 = (float)(Math.toDegrees(Math.atan2(var31, var29)) - 90.0);
                        float var34 = (float)(-Math.toDegrees(Math.atan2(var30, var32)));
                        Events.f3.m77(var33);
                        Events.f3.m83(var34);
                        this.pm$74(var23);
                     }
                  }
               } else {
                  mc.player.setDeltaMovement(var2.x * var3, var2.y, var2.z * var3);
               }
            }
         }

         if (var1 == Events.f10) {
            if (Events.f10.m44() instanceof ServerboundAttackPacket) {
               this.f346 = mc.player.tickCount;
            }

            if (Events.f10.m44() instanceof ServerboundPlayerActionPacket && this.f345) {
               var1.setCancelled(true);
            }
         }

         if (var1 == Events.f11) {
            Packet var20 = Events.f11.m41();
            if (var20 instanceof ClientboundSetEntityMotionPacket var21) {
               if (var21.id() == mc.player.getId()) {
                  Vec3 var4 = var21.movement();
                  if (var4.y > 0.0 && !FeatureManager.f35.isEnabled()) {
                     double var25 = this.f341.m215() ? -this.f331.m220() * 0.01 : this.f331.m220() * 0.01;
                     double var28 = var4.x * var25;
                     double var9 = var4.y * this.f332.m220() * 0.01;
                     double var11 = var4.z * var25;
                     if (this.f331.m220() != 100.0 || this.f341.m215()) {
                        var1.setCancelled(true);
                     }

                     if (this.f334.m220() != 0.0) {
                        this.f342 = 0;
                     }

                     if (this.f337.m215() && this.pm$75()) {
                        this.f18.add(var21);
                        var1.setCancelled(true);
                     } else {
                        if (this.f331.m220() != 100.0 || this.f341.m215()) {
                           mc.player.setDeltaMovement(new Vec3(var28, var9, var11));
                        }

                        if (this.f336.m215() && mc.player.onGround() && mc.player.isSprinting()) {
                           this.f344 = true;
                        }
                     }
                  }
               }
            } else if (this.f337.m215() && !this.f18.isEmpty()) {
               if (var20 instanceof ClientboundStartConfigurationPacket || var20 instanceof ClientboundDisconnectPacket) {
                  this.pm$76();
                  return;
               }

               synchronized (this.f18) {
                  this.f18.add(var20);
                  var1.setCancelled(true);
               }
            }
         }
      }

      void reset(boolean flush) {
         if (flush && mc.getConnection() != null) this.pm$76(); else this.f18.clear();
         this.f342 = -1; this.f343 = 0; this.f344 = this.f345 = false;
      }

      private boolean pm$75() {
         LivingEntity var1 = TargetFinder.m47(6.0, false);
         LivingEntity var2 = TargetFinder.m49(false);
         return (!mc.player.onGround() || !this.f340.m215())
            && (var1 == null || (double)mc.player.distanceTo(var1) > this.f339.m220())
            && (!this.f335.m228(f323) || !mc.player.isSprinting() && (var2 == null || !(mc.player.distanceTo(var2) > 11.0F)));
      }

      private void pm$74(Entity var1) {
         if (!FeatureManager.f33.isEnabled() || !FeatureManager.f33.f22) {
            this.f345 = true;
            mc.gameMode.attack(mc.player, var1);
            mc.player.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, false);
            this.f345 = false;
         }
      }

      private void pm$76() {
         if (!this.f18.isEmpty()) {
            synchronized (this.f18) {
               for (Packet var3 : this.f18) {
                  mc.execute(() -> var3.handle(mc.getConnection()));
               }

               this.f18.clear();
            }

            this.f343 = 0;
         }
      }

      Original(Feature owner, java.util.function.BooleanSupplier visible) {
         this.f331 = new NumberSetting(f316, owner, 100.0, 0.0, 100.0, 5.0);
         this.f332 = new NumberSetting(f317, owner, 100.0, 0.0, 100.0, 5.0);
         this.f333 = new NumberSetting(f318, owner, 1.0, 0.0, 1.0, 0.1);
         this.f334 = new NumberSetting(f319, owner, 0.0, 0.0, 5.0, 1.0);
         this.f335 = new ModeSetting(f320, owner, f321, new String[]{f321, f322, f323});
         this.f336 = new BooleanSetting(f324, owner, false);
         this.f337 = new BooleanSetting(f325, owner, false);
         this.f338 = new NumberSetting(f326, owner, 4.0, 1.0, 15.0, 1.0);
         this.f339 = new NumberSetting(f327, owner, 3.0, 1.0, 4.0, 0.5);
         this.f340 = new BooleanSetting(f328, owner, false);
         this.f341 = new BooleanSetting(f329, owner, false);
         this.f18 = new ArrayList<>();
         this.f342 = -1;
         for (var setting : owner.settings) if (!setting.getName().equals("Mode")) setting.setVisible(visible);
         this.f338.setVisible(() -> visible.getAsBoolean() && this.f337.m215());
         this.f339.setVisible(() -> visible.getAsBoolean() && this.f337.m215());
         this.f340.setVisible(() -> visible.getAsBoolean() && this.f337.m215());
      }
   }

   private static final class Knockback implements Wrapper {
      private final Object packetLock = new Object();
      private final Supplier<String> mode;
      private final NumberSetting attackCounts, sprintTicks, maxDelay, delayTicks;
      private final BooleanSetting swingHand, jumpReset;
      private final Queue<Packet<?>> packets = new ArrayDeque<>();
      private final DelayQueue<Packet<?>> delayPackets = new DelayQueue<>();
      private volatile long lag, delayLag, startDelay;
      private volatile int attackQueue, sprintQueue;
      private volatile float yaw;
      private volatile boolean delay, jump;
      private ClientPacketListener listener;

      Knockback(Feature owner, Supplier<String> mode) {
         this.mode = mode;
         this.attackCounts = new NumberSetting("Attack Counts", owner, 1, 1, 5, 1);
         this.sprintTicks = new NumberSetting("Sprint Ticks", owner, 3, 1, 5, 1);
         this.maxDelay = new NumberSetting("Max Delay", owner, 1000, 0, 1000, 50);
         this.swingHand = new BooleanSetting("Swing Hand", owner, false);
         this.delayTicks = new NumberSetting("Epsilon Delay Ticks", owner, 3, 1, 5, 1);
         this.delayTicks.setDisplayName("Delay Ticks");
         this.jumpReset = new BooleanSetting("Epsilon Jump Reset", owner, false);
         this.jumpReset.setDisplayName("Jump Reset");
         for (var setting : List.of(this.attackCounts, this.sprintTicks, this.maxDelay, this.swingHand)) setting.setVisible(() -> mode.get().equals("Reduce"));
         this.delayTicks.setVisible(() -> mode.get().equals("Delay"));
         this.jumpReset.setVisible(() -> mode.get().equals("Delay"));
      }

      boolean delaying() { return this.delay || this.delayPackets.active(); }
      boolean blocksBacktrack() { return delaying() || this.attackQueue > 0; }

      void receive(Event event, Packet<?> packet) {
         synchronized (this.packetLock) {
            if (packet instanceof ClientboundDisconnectPacket || packet instanceof ClientboundStartConfigurationPacket
               || packet instanceof ClientboundLoginPacket || packet instanceof ClientboundRespawnPacket) { reset(); return; }
            if (this.listener != null && this.listener != mc.getConnection()) reset();
            this.listener = mc.getConnection();
            if (this.mode.get().equals("Reduce")) reducePacket(event, packet); else delayPacket(event, packet);
         }
      }

      private void reducePacket(Event event, Packet<?> packet) {
         if (this.delay && (packet instanceof ClientboundSetEntityMotionPacket || packet instanceof ClientboundMoveEntityPacket
            || packet instanceof ClientboundTeleportEntityPacket || packet instanceof ClientboundPingPacket
            || packet instanceof ClientboundPlayerLookAtPacket || packet instanceof ClientboundPlayerPositionPacket)) {
            event.setCancelled(true); this.packets.add(packet);
         }
         if (packet instanceof ClientboundPlayerPositionPacket || packet instanceof ClientboundExplodePacket) this.lag = System.currentTimeMillis();
         if (packet instanceof ClientboundSetEntityMotionPacket motion && motion.id() == mc.player.getId()) {
            var movement = motion.movement();
            if (System.currentTimeMillis() - this.lag >= 100 && knockback(movement.x, movement.y, movement.z) && !this.delay) {
               this.delay = true; event.setCancelled(true); this.packets.add(packet); this.startDelay = System.currentTimeMillis();
            }
            this.yaw = Mth.wrapDegrees((float)(Math.toDegrees(Math.atan2(movement.z, movement.x)) + 90));
         }
      }

      private void delayPacket(Event event, Packet<?> packet) {
         if (packet instanceof ClientboundPlayerPositionPacket || packet instanceof ClientboundExplodePacket) this.delayLag = System.currentTimeMillis();
         if (this.delayPackets.offer(packet)) { event.setCancelled(true); return; }
         if (packet instanceof ClientboundSetEntityMotionPacket motion && motion.id() == mc.player.getId()
            && System.currentTimeMillis() - this.delayLag >= 100) {
            var movement = motion.movement();
            // Apply the initiating motion immediately; delay subsequent inbound packets.
            if (knockback(movement.x, movement.y, movement.z)) this.delayPackets.start((int)this.delayTicks.m220());
         }
      }

      static boolean knockback(double x, double y, double z) { return y > 0 && (x != 0 || z != 0); }

      void clientTick() {
         if (!this.mode.get().equals("Reduce")) return;
         if (this.delay && (mc.player.onGround() || System.currentTimeMillis() - this.lag < 100
            || System.currentTimeMillis() - this.startDelay >= this.maxDelay.m220()) && flushReduce()) {
            if (System.currentTimeMillis() - this.lag >= 100) {
               this.attackQueue = (int)this.attackCounts.m220(); this.sprintQueue = (int)this.sprintTicks.m220();
            }
         }
      }

      void rotate() {
         if (this.mode.get().equals("Reduce") && this.sprintQueue >= 1 && FeatureManager.f26.f17 == null && !FeatureManager.f29.isEnabled()) {
            Events.f3.m77(this.yaw);
         }
      }

      void playerTick() {
         if (this.mode.get().equals("Delay")) {
            List<Packet<?>> pending = List.of();
            synchronized (this.packetLock) {
               pending = this.delayPackets.tick();
            }
            handle(pending);
         } else if (this.mode.get().equals("Reduce") && this.attackQueue >= 1) {
            if (mc.gui.screen() != null || !mc.isWindowActive()) {
               this.attackQueue = 0;
               return;
            }
            Player target = hitPlayer();
            if (target != null && target.isAlive() && !FeatureManager.f33.f22) {
               mc.gameMode.attack(mc.player, target);
               if (this.swingHand.m215()) {
                  mc.player.swing(InteractionHand.MAIN_HAND, mc.player.getMainHandItem().getAttackAnimation(), false);
                  mc.getConnection().send(ServerboundPunchPacket.INSTANCE);
               }
            }
            this.attackQueue--;
         }
      }

      private Player hitPlayer() {
         var origin = mc.player.getEyePosition();
         var direction = net.minecraft.world.phys.Vec3.directionFromRotation(Events.f3.m82(), Events.f3.m76());
         var end = origin.add(direction.scale(3));
         var hit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(mc.player, origin, end,
            mc.player.getBoundingBox().expandTowards(direction.scale(3)).inflate(1),
            entity -> entity instanceof Player && FeatureManager.targets.shouldAttack(entity), 9);
         if (hit == null || !(hit.getEntity() instanceof Player player)) return null;
         var block = mc.level.clip(new net.minecraft.world.level.ClipContext(origin, hit.getLocation(),
            net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, mc.player));
         return block.getType() == net.minecraft.world.phys.HitResult.Type.MISS ? player : null;
      }

      void input() {
         if (this.mode.get().equals("Delay")) {
            if (this.jumpReset.m215() && mc.player.onGround() && mc.player.hurtTime == 9) Events.f7.m59(true);
            return;
         }
         if (this.delay && mc.player.fallDistance == 0 && hitPlayer() != null && System.currentTimeMillis() - this.lag >= 100) forward();
         if (this.jump && mc.player.onGround()) {
            if (System.currentTimeMillis() - this.lag >= 100) { Events.f7.m59(true); forward(); }
            this.jump = false;
         }
         if (this.sprintQueue-- >= 1 && FeatureManager.f26.f17 == null && !FeatureManager.f29.isEnabled() && System.currentTimeMillis() - this.lag >= 100) forward();
      }

      private static void forward() { Events.f7.m55(true); Events.f7.setBackward(false); Events.f7.m57(false); Events.f7.m58(false); }

      private boolean flushReduce() {
         List<Packet<?>> pending;
         synchronized (this.packetLock) {
            if (!this.delay) return false;
            this.delay = false; pending = drain(this.packets);
         }
         handle(pending); this.jump = true; return true;
      }

      void flush() {
         List<Packet<?>> pending;
         synchronized (this.packetLock) {
            this.delay = false;
            pending = drain(this.packets); pending.addAll(this.delayPackets.drain());
         }
         handle(pending); reset();
      }

      void reset() {
         synchronized (this.packetLock) {
            this.packets.clear(); this.delayPackets.reset(); this.delay = this.jump = false;
            this.attackQueue = this.sprintQueue = 0;
            this.lag = this.delayLag = this.startDelay = 0; this.yaw = 0; this.listener = null;
         }
      }

      private static List<Packet<?>> drain(Queue<Packet<?>> queue) {
         var result = new ArrayList<Packet<?>>(queue); queue.clear(); return result;
      }
      @SuppressWarnings({"rawtypes", "unchecked"})
      private void handle(List<Packet<?>> pending) {
         ClientPacketListener captured = this.listener;
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
