package com.samsara.module.combat;

import com.mojang.blaze3d.platform.InputConstants;
import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.module.FeatureManager;
import com.samsara.setting.BooleanSetting;
import com.samsara.setting.ModeSetting;
import com.samsara.setting.NumberSetting;
import com.samsara.util.InventoryUtil;
import com.samsara.util.PacketBlinkQueue;
import com.samsara.util.TargetFinder;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.Fireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.sdl.SDLMouse;

public class KillAura extends Feature {
   private static final double PREDICT_BLOCK_RANGE = 3.5;
   private static final double PREDICT_BLOCK_TOLERANCE = 0.3;
   private static final int PREDICT_BLOCK_STEPS = 16;
   private float f301;
   private static final String f262 = "Watchdog";
   private float f300;
   private int f288;
   private boolean f296;
   private boolean usingAutoBlockItem;
   private static final String f273 = "AutoBlock RMB";
   private static final String f274 = "Attack Teammates";
   private int f297;
   private static final String f263 = "Watchdog2";
   private final BooleanSetting f284;
   private static final String f255 = "Rotations";
   private static final String f266 = "Cycle";
   private boolean f291;
   private boolean f292;
   private boolean f293;
   private boolean f289;
   private static final String f254 = "KillAura";
   private final NumberSetting f281;
   private final NumberSetting wallRange;
   private int f298;
   private static final String f267 = "Attack Delay";
   private static final String f259 = "None";
   private int f294;
   private static final String f276 = "shield";
   private static final String f270 = "Attack Cooldown";
   private static final String f268 = "Attack Delay2";
   private final BooleanSetting f285;
   private final BooleanSetting f286;
   private static final String f269 = "Rotation Range";
   private static final String f261 = "Vanilla";
   private boolean f290;
   public boolean f16;
   private static final String f272 = "Require Sword";
   private final BooleanSetting f287;
   private static final String f256 = "Head";
   public boolean f15;
   private final ModeSetting f278;
   private final BooleanSetting predict;
   // Rotation samples use fractional ticks from a monotonic clock, independent of rendering interpolation.
   private final Map<Player, double[]> blockRotations = new IdentityHashMap<>();
   private final Map<Player, double[]> blockPositions = new IdentityHashMap<>();
   private boolean predictedBlockThreat;
   private Player blockThreatTarget;
   private boolean predictiveBlockActive;
   private final NumberSetting f280;
   private static final String f264 = "Swap";
   public Entity f17;
   private final NumberSetting f279;
   private boolean f299;
   private final BooleanSetting f283;
   private static final String f257 = "Optimal";
   private final ModeSetting f277 = new ModeSetting(f255, this, f256, new String[]{f256, f257});
   private static final String f271 = "Ignore Shield";
   private static final String f258 = "AutoBlock";
   private final BooleanSetting f282;
   private static final String f265 = "Swap2";
   private int f295;
   private static final String f275 = "Attack Fireballs";

   private void pm$64() {
      if (this.f17 == null || this.f278.m228(f259) || !this.pm$73()) return;
      mc.getConnection().send(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 0, Events.f3.m76(), Events.f3.m82()));
      this.f16 = true;
   }

   private void pm$62() {
      if (this.f291) {
         mc.getConnection().send(new ServerboundSetCarriedItemPacket(mc.player.getInventory().getSelectedSlot()));
         this.f291 = false;
      }
   }

   private float pm$70() {
      float var1 = ((Double)mc.options.sensitivity().get()).floatValue();
      float var2 = var1 * 0.6F + 0.2F;
      return var2 * var2 * var2 * 8.0F * 0.15F;
   }

   private void pm$63(float var1, float var2, boolean var3) {
      if (!FeatureManager.f33.f22) {
         double var4 = this.pm$66(this.f17, var1, var2, 10.0);
         if (var4 <= 3.0 && var4 >= 0.0) {
            mc.gameMode.attack(mc.player, this.f17);
            mc.player.swing(InteractionHand.MAIN_HAND, mc.player.getMainHandItem().getAttackAnimation(), false);
            // 26.3 的 swing 只播放本地动画；原版 startAttack 在之后单独发送 Punch。
            mc.getConnection().send(ServerboundPunchPacket.INSTANCE);
            if (var3) {
               mc.getConnection().send(new ServerboundInteractPacket(this.f17.getId(), InteractionHand.MAIN_HAND, Vec3.ZERO, false));
            }
         }
      }
   }

   private void pm$65() {
      this.f289 = false;
      mc.getConnection().send(new ServerboundPlayerActionPacket(Action.RELEASE_USE_ITEM, BlockPos.ZERO, Direction.DOWN));
      this.f289 = true;
      this.f16 = false;
      stopAutoBlockItem();
   }

   private void startAutoBlockItem() {
      mc.player.startUsingItem(InteractionHand.MAIN_HAND);
      this.usingAutoBlockItem = true;
   }

   private void stopAutoBlockItem() {
      if (this.usingAutoBlockItem && mc.player != null) mc.player.stopUsingItem();
      this.usingAutoBlockItem = false;
   }

   private void pm$58() {
      if (!this.pm$73()) return;
      this.f289 = false;
      this.f15 = true;
      switch (this.f278.m224()) {
         case f261 -> {
            mc.getConnection().send(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 0, mc.player.getYRot(), mc.player.getXRot()));
            this.f16 = true;
         }
         case f266 -> {
            this.f289 = true;
            this.f294++;
            if (this.f294 < 3) {
               return;
            }

            if (this.f291) {
               this.pm$61(mc.player.getInventory().getSelectedSlot());
            }

            this.pm$63(this.f300, this.f301, true);
            this.pm$64();
            startAutoBlockItem();
            PacketBlinkQueue.m23();
            this.f296 = true;
            if (!this.f291) {
               this.pm$61(mc.player.getInventory().getSelectedSlot() % 8 + 1);
            }

            PacketBlinkQueue.m22();
            this.f294 = 0;
         }
         case f262 -> {
            this.f289 = true;
            this.f294++;
            if (this.f294 < 2) {
               if (this.f16) {
                  startAutoBlockItem();
               }

               return;
            }

            this.pm$65();
            this.pm$63(this.f300, this.f301, true);
            this.pm$64();
            startAutoBlockItem();
            this.f294 = 0;
         }
         case f263 -> {
            this.f289 = true;
            this.f294++;
            if (this.f294 < 2) {
               if (this.f16) {
                  startAutoBlockItem();
               }

               return;
            }

            this.pm$65();
            boolean var6 = this.pm$59(this.f17, 45.0);
            if (this.predict.m215() || mc.player.hurtTime <= 5 && var6 && !((double)mc.player.distanceTo(this.f17) > 3.0)) {
               this.pm$63(this.f300, this.f301, true);
               this.pm$64();
               startAutoBlockItem();
            } else {
               this.pm$63(this.f300, this.f301, false);
            }

            this.f294 = 0;
         }
         case f264 -> {
            this.f289 = true;
            this.f294++;
            if (this.f294 < 3) {
               if (this.f294 != 2 && this.f16) {
                  startAutoBlockItem();
               }

               return;
            }

            this.pm$60();
            if (this.f297 != -1 && this.f298 != -1) {
               int var7 = this.f299 ? this.f298 : this.f297;
               this.f299 = !this.f299;
               this.pm$61(var7);
               if (mc.player.getInventory().getItem(var7).is(ItemTags.SWORDS)) {
                  this.pm$63(this.f300, this.f301, true);
                  this.pm$64();
                  startAutoBlockItem();
               }

               this.f294 = 0;
            } else if (mc.player.getInventory().getItem(mc.player.getInventory().getSelectedSlot()).is(ItemTags.SWORDS)) {
               this.pm$63(this.f300, this.f301, false);
            }
         }
         case f265 -> {
            this.f294++;
            this.f289 = true;
            if (this.f16) {
               this.pm$65();
            }

            if (!this.f291 && this.f292) {
               this.pm$61(mc.player.getInventory().getSelectedSlot() % 8 + 1);
               return;
            }

            if (this.f294 < 3) {
               return;
            }

            if (this.f291) {
               this.pm$61(mc.player.getInventory().getSelectedSlot());
               this.pm$63(this.f300, this.f301, false);
            }

            this.f295++;
            if (this.f295 < 3) {
               this.f292 = true;
               this.f299 = !this.f299;
               this.pm$63(this.f300, this.f301, true);
               this.pm$64();
            } else {
               this.f292 = false;
               this.f299 = !this.f299;
               if (this.f299) {
                  this.pm$63(this.f300, this.f301, true);
                  this.f293 = true;
               } else {
                  this.pm$63(this.f300, this.f301, false);
               }

               this.f295 = 0;
            }

            this.f294 = 0;
         }
         default -> { }
      }
   }

   private float pm$71(float var1, float var2) {
      float var3 = this.pm$70();
      float var4 = var1 - var2;
      var4 -= var4 % var3;
      return var2 + var4;
   }

   private Vec3 pm$68(float var1, float var2) {
      float var3 = Mth.cos((double)(-var2 * (float) (Math.PI / 180.0) - (float) Math.PI));
      float var4 = Mth.sin((double)(-var2 * (float) (Math.PI / 180.0) - (float) Math.PI));
      float var5 = -Mth.cos((double)(-var1 * (float) (Math.PI / 180.0)));
      float var6 = Mth.sin((double)(-var1 * (float) (Math.PI / 180.0)));
      return new Vec3((double)(var4 * var5), (double)var6, (double)(var3 * var5));
   }

   @Override
   public int getPriority(Event var1) {
      return var1 == Events.f3 ? -2 : 0;
   }

   private void pm$72() {
      this.f17 = null;
      resetBlockPrediction();
      stopAutoBlock();
   }

   private void stopAutoBlock() {
      this.predictiveBlockActive = false;
      this.f15 = false;
      this.f289 = false;
      this.f293 = this.f292 = this.f299 = false;
      this.f294 = this.f295 = 0;
      if (mc.player == null || mc.getConnection() == null) {
         this.f16 = this.f291 = this.f296 = this.usingAutoBlockItem = false;
         return;
      }
      if (this.f291) {
         this.pm$62();
      }

      if (this.f16) {
         this.pm$65();
      }
      stopAutoBlockItem();

      if (this.f296) {
         PacketBlinkQueue.m23();
         this.f296 = false;
      }

      this.f289 = false;
   }

   private void pm$61(int var1) {
      mc.getConnection().send(new ServerboundSetCarriedItemPacket(var1));
      this.f291 = true;
      if (var1 == mc.player.getInventory().getSelectedSlot()) {
         this.f291 = false;
      }
   }

   @Override
   public void onEvent(Event var1) {
      if (mc.player == null || mc.level == null || mc.getConnection() == null) {
         this.pm$72();
         return;
      }
      if ((var1 == Events.f3 || var1 == Events.f2) && (mc.gui.screen() != null || !mc.isWindowActive())) {
         this.pm$72();
         return;
      }
      if ((var1 == Events.f3 || var1 == Events.f2) && FeatureManager.autoRod != null
          && FeatureManager.autoRod.isCombatHandReserved()) {
         this.prepareForAutoRod();
         return;
      }
      if (var1 == Events.f15 && this.f285.m215()
          && Events.f15.m13() == InputConstants.MOUSE_BUTTON_RIGHT && Events.f15.m17()
          && (!this.predict.m215() || !this.predictedBlockThreat)) {
         stopAutoBlock();
      }
      if (var1 == Events.f3) {
         updateBlockPrediction();
      }
      if (this.predict.m215() && (var1 == Events.f2 || var1 == Events.f7 || var1 == Events.f4 || var1 == Events.f5
          || var1 == Events.f15 && Events.f15.m13() == InputConstants.MOUSE_BUTTON_RIGHT)) {
         refreshPredictAutoBlock();
      }
      if ((var1 == Events.f2 || var1 == Events.f3)
          && (!this.pm$73() || this.f278.m228(f259) || !mc.player.getMainHandItem().is(ItemTags.SWORDS))) {
         stopAutoBlock();
      }
      if (var1 == Events.f2 && this.f293 && this.f17 != null) {
         this.f293 = false;
         if (!this.f278.m228(f263)) {
            this.pm$64();
         } else {
            this.pm$65();
         }
      }

      if (var1 == Events.f15) {
         if (mc.gui.screen() == null && this.f17 != null) {
            var1.setCancelled(true);
         }
      }

      if (var1 == Events.f3) {
         this.setSuffix(this.f278.m228(f259) ? null : this.f278.m224());
         this.f15 = false;
         if (!this.f282.m215() || mc.player.getAttackStrengthScale(0.0F) >= 1.0F) {
            this.f288++;
         }

         this.f17 = TargetFinder.nearest(this.f281.m220(), true, !this.f286.m215(), entity -> {
            this.pm$69(entity);
            // Keep distant visible targets for rotations; reject occluded targets outside the wall range.
            return this.pm$66(entity, this.f300, this.f301, this.f281.m220()) >= 0;
         });
         if (this.f17 == null && this.predictedBlockThreat) this.f17 = this.blockThreatTarget;

         if (this.f287.m215()) {
            Entity var2 = this.pm$67(3.0);
            if (var2 != null) {
               this.pm$69(var2);
               Events.f3.m77(this.f300);
               Events.f3.m83(this.f301);
               mc.gameMode.attack(mc.player, var2);
               mc.player.swing(InteractionHand.MAIN_HAND, mc.player.getMainHandItem().getAttackAnimation(), false);
               mc.getConnection().send(ServerboundPunchPacket.INSTANCE);
               this.pm$72();
               return;
            }
         }

         if (this.f284.m215() && !InventoryUtil.m39()) {
            this.f17 = null;
         }

         if (this.f17 != null) {
            this.pm$69(this.f17);
            Events.f3.m77(this.f300);
            Events.f3.m83(this.f301);
            double var6 = this.f290 ? this.f280.m220() : this.f279.m220();
            if (!FeatureManager.f28.blocksAttacks() && (double)this.f288 > var6 && (!this.f282.m215() || mc.player.getAttackStrengthScale(0.0F) >= 1.0F)) {
               if (this.f283.m215() && this.f17 instanceof Player var4 && var4.isUsingItem() && var4.getUseItem().is(Items.SHIELD)) {
                  System.out.println(f276);
                  return;
               }

               if (!this.f278.m228(f262) && !this.f278.m228(f263) && !this.f278.m228(f264) && !this.f278.m228(f265) && !this.f278.m228(f266)
                  || !this.pm$73() && !this.f16) {
                  this.f288 = 0;
                  this.pm$63(this.f300, this.f301, false);
               }

               this.f290 = !this.f290;
            }

            if (!this.f278.m228(f259) && mc.player.getMainHandItem().is(ItemTags.SWORDS)) {
               if (this.pm$73()) {
                  mc.options.keyUse.setDown(false);
                  this.pm$58();
                  if (this.predict.m215()) synchronizePredictAutoBlock();
               } else {
                  stopAutoBlock();
               }
            } else {
               stopAutoBlock();
            }
         } else {
            this.pm$72();
         }
      }

      if (var1 == Events.f10 && Events.f10.m44() instanceof ServerboundPlayerActionPacket && this.f289) {
         var1.setCancelled(true);
      }
   }

   public boolean hasAutoBlockAnimation() {
      // Cyclic modes release or swap before reblocking; keep their working phase visible.
      return hasAutoBlockMode() && isAutoBlockInputAllowed()
         && (this.f15 || isAutoBlocking() || this.predict.m215() && !this.f285.m215());
   }

   public boolean hasAutoBlockMode() { return !this.f278.m228(f259); }

   public boolean isAutoBlocking() { return this.f16 || this.usingAutoBlockItem; }

   public boolean prepareForAutoRod() {
      if (!isEnabled()) return true;
      if (mc.player == null || mc.getConnection() == null) return false;
      resetBlockPrediction();
      stopAutoBlock();
      return true;
   }

   private void resetBlockPrediction() {
      this.predictedBlockThreat = false;
      this.blockThreatTarget = null;
      this.predictiveBlockActive = false;
      this.blockRotations.clear();
      this.blockPositions.clear();
   }

   public void onPredictEntityUpdate(Entity entity, boolean rotationUpdated, boolean positionUpdated, boolean teleport) {
      if (!isEnabled() || !this.predict.m215() || !hasAutoBlockMode() || mc.player == null || mc.level == null
          || !(entity instanceof Player)) return;
      if (entity instanceof Player player && player != mc.player) {
         double now = predictionTime();
         var latest = player.getClientPositionAndRotation();
         if (teleport) {
            this.blockRotations.remove(player);
            this.blockPositions.remove(player);
         }
         if (rotationUpdated) this.blockRotations.put(player,
            sampleBlockRotation(this.blockRotations.get(player), latest.yRot(), latest.xRot(), now));
         if (positionUpdated) this.blockPositions.put(player,
            sampleBlockMovement(this.blockPositions.get(player), latest.position(), now));
      }
      refreshPredictAutoBlock();
   }

   public void onPredictHeadUpdate(Entity entity, float yaw) {
      if (!isEnabled() || !this.predict.m215() || !hasAutoBlockMode() || mc.player == null || mc.level == null
          || !(entity instanceof Player)) return;
      if (entity instanceof Player player && player != mc.player) {
         var previous = this.blockRotations.get(player);
         float pitch = previous == null ? player.getClientPositionAndRotation().xRot() : (float)previous[1];
         // Head packets interpolate separately; their received angle is already authoritative.
         this.blockRotations.put(player, sampleBlockRotation(previous, yaw, pitch, predictionTime()));
      }
      refreshPredictAutoBlock();
   }

   private void refreshPredictAutoBlock() {
      if (mc.player == null || mc.level == null || mc.getConnection() == null || !isAutoBlockInputAllowed()) {
         resetBlockPrediction();
         stopAutoBlock();
         return;
      }
      updateBlockPrediction();
      synchronizePredictAutoBlock();
   }

   private void synchronizePredictAutoBlock() {
      if (!hasAutoBlockMode() || !this.pm$73() || this.f17 == null || this.f17.isRemoved() || !this.f17.isAlive()
          || !mc.player.getMainHandItem().is(ItemTags.SWORDS)) {
         stopAutoBlock();
         return;
      }
      this.f15 = true;
      if (!this.predictiveBlockActive) {
         // A brief threat must block immediately, before the normal mode cycle can advance.
         if (!this.f16) this.pm$64();
         if (!this.usingAutoBlockItem) startAutoBlockItem();
         this.predictiveBlockActive = true;
      }
   }

   private static double predictionTime() { return System.nanoTime() / 50_000_000.0; }

   private void updateBlockPrediction() {
      if (!this.predict.m215() || this.f278.m228(f259)) {
         resetBlockPrediction();
         return;
      }
      this.blockRotations.keySet().removeIf(player -> player.isRemoved() || player.level() != mc.level);
      this.blockPositions.keySet().removeIf(player -> player.isRemoved() || player.level() != mc.level);
      double now = predictionTime();
      var threat = TargetFinder.nearest(Double.POSITIVE_INFINITY, true, !this.f286.m215(), entity -> {
         if (!(entity instanceof Player player) || !player.isAlive() || player.isSleeping() || player.isSpectator()) return false;
         var latest = player.getClientPositionAndRotation();
         Vec3 position = latest.position();
         double distanceSquared = mc.player.position().distanceToSqr(position);
         if (distanceSquared > PREDICT_BLOCK_RANGE * PREDICT_BLOCK_RANGE) return false;
         var rotation = this.blockRotations.computeIfAbsent(player,
            ignored -> sampleBlockRotation(null, latest.yRot(), latest.xRot(), now));
         var movement = this.blockPositions.computeIfAbsent(player, ignored -> sampleBlockMovement(null, position, now));
         Vec3 eyes = position.add(0, player.getEyeHeight(), 0);
         Vec3 velocity = now - movement[3] <= 3 ? new Vec3(movement[4], movement[5], movement[6]) : Vec3.ZERO;
         return predictsMovingBlockThreat(distanceSquared, eyes, rotation, now, velocity,
            mc.player.getDeltaMovement(), mc.player.getBoundingBox(), this.wallRange.m220(), (origin, hit) -> mc.level.clip(
               new ClipContext(origin, hit, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() != HitResult.Type.MISS);
      });
      this.predictedBlockThreat = threat != null;
      this.blockThreatTarget = threat instanceof Player player ? player : null;
      if (threat != null && (this.f17 == null || this.f17.isRemoved() || !this.f17.isAlive())) this.f17 = threat;
   }

   private boolean pm$59(Entity var1, double var2) {
      Vec3 var4 = var1.getEyePosition(1.0F);
      Vec3 var5 = var1.getViewVector(1.0F).normalize();
      Vec3 var6 = mc.player.getEyePosition(1.0F).subtract(var4).normalize();
      double var7 = var5.dot(var6);
      return var7 >= Math.cos(Math.toRadians(var2));
   }

   private boolean pm$73() {
      return isAutoBlockInputAllowed() && shouldAutoBlock(this.predict.m215(), this.f285.m215(),
         isManualAutoBlockRequested(), this.predictedBlockThreat);
   }

   static boolean shouldAutoBlock(boolean predict, boolean rmbEnabled, boolean rightMouseDown, boolean threat) {
      return predict ? threat || rmbEnabled && rightMouseDown : !rmbEnabled || rightMouseDown;
   }

   private boolean isManualAutoBlockRequested() {
      return this.f285.m215() && (SDLMouse.SDL_GetMouseState((java.nio.FloatBuffer)null,
         (java.nio.FloatBuffer)null) & SDLMouse.SDL_BUTTON_RMASK) != 0;
   }

   public boolean isAutoBlockInputAllowed() {
      if (mc == null || mc.gui.screen() != null || !mc.isWindowActive()) return false;
      if (FeatureManager.autoRod != null && FeatureManager.autoRod.isCombatHandReserved()) return false;
      // MouseHandler retains its last gameplay button state while a Screen handles input.
      return this.predict.m215() && hasAutoBlockMode() || !this.f285.m215() || isManualAutoBlockRequested();
   }

   private Entity pm$67(double var1) {
      AABB var3 = mc.player.getBoundingBox().inflate(var1);
      Entity var4 = null;
      double var5 = Double.MAX_VALUE;

      for (Entity var8 : mc.level.getEntities(mc.player, var3)) {
         if (var8 instanceof Fireball || var8 instanceof SmallFireball || var8 instanceof LargeFireball) {
            this.pm$69(var8);
            double var9 = this.pm$66(var8, this.f300, this.f301, 10.0);
            if (var9 >= 0.0 && var9 <= var1 && var9 < var5) {
               var4 = var8;
               var5 = var9;
            }
         }
      }

      return var4;
   }

   @Override
   public void onDisable() {
      this.pm$72();
   }

   private void pm$69(Entity var1) {
      double var2;
      double var4;
      double var6;
      if (this.f277.m228(f257)) {
         AABB var8 = var1.getBoundingBox();
         var2 = Mth.clamp(mc.player.getX(), var8.minX, var8.maxX);
         var4 = Mth.clamp(mc.player.getEyeY(), var8.minY, var8.maxY);
         var6 = Mth.clamp(mc.player.getZ(), var8.minZ, var8.maxZ);
      } else {
         var2 = var1.getX();
         var4 = var1.getEyeY();
         var6 = var1.getZ();
      }

      double var18 = var2 - mc.player.getX();
      double var10 = var4 - mc.player.getEyeY();
      double var12 = var6 - mc.player.getZ();
      double var14 = Math.sqrt(var18 * var18 + var12 * var12);
      float var16 = (float)Math.toDegrees(Math.atan2(var12, var18)) - 90.0F;
      float var17 = (float)(-Math.toDegrees(Math.atan2(var10, var14)));
      var16 = mc.player.getYRot() + Mth.wrapDegrees(var16 - mc.player.getYRot());
      this.f300 = this.pm$71(var16, mc.player.getYRot());
      this.f301 = this.pm$71(var17, mc.player.getXRot());
   }

   private void pm$60() {
      this.f297 = -1;
      this.f298 = -1;

      for (int var1 = 0; var1 < 9; var1++) {
         if (mc.player.getInventory().getItem(var1).is(ItemTags.SWORDS)) {
            if (this.f297 != -1) {
               this.f298 = var1;
               break;
            }

            this.f297 = var1;
         }
      }
   }

   public KillAura() {
      super(f254, Category.COMBAT);
      this.f278 = new ModeSetting(f258, this, f259, new String[]{f259, f261, f262, f263, f264, f265, f266});
      this.predict = new BooleanSetting("Predict", this, false);
      this.predict.setVisible(() -> !this.f278.m228(f259));
      this.f279 = new NumberSetting(f267, this, 3.0, 0.0, 10.0, 1.0);
      this.f280 = new NumberSetting(f268, this, 3.0, 0.0, 10.0, 1.0);
      this.f281 = new NumberSetting(f269, this, 5.0, 3.0, 10.0, 0.5);
      this.wallRange = new NumberSetting("Wall Range", this, 0.0, 0.0, 3.0, 0.1);
      this.f282 = new BooleanSetting(f270, this, true);
      this.f283 = new BooleanSetting(f271, this, false);
      this.f284 = new BooleanSetting(f272, this, false);
      this.f285 = new BooleanSetting(f273, this, false);
      this.f286 = new BooleanSetting(f274, this, true);
      this.f287 = new BooleanSetting(f275, this, false);
      this.f297 = -1;
      this.f298 = -1;
   }

   private double pm$66(Entity var1, float var2, float var3, double var4) {
      Vec3 origin = mc.player.getEyePosition(1.0F);
      return raycastDistance(origin, this.pm$68(var3, var2), var1.getBoundingBox(), var4, this.wallRange.m220(),
         hit -> mc.level.clip(new ClipContext(origin, hit, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player))
            .getType() != HitResult.Type.MISS);
   }

   static double raycastDistance(Vec3 origin, Vec3 direction, AABB target, double range, double wallRange, Predicate<Vec3> occluded) {
      var hit = target.contains(origin) ? java.util.Optional.of(origin) : target.clip(origin, origin.add(direction.scale(range)));
      if (hit.isEmpty()) return -0.1;
      double distance = origin.distanceTo(hit.get());
      return distance > wallRange && occluded.test(hit.get()) ? -0.1 : distance;
   }

   static boolean predictsBlockThreat(double distanceSquared, Vec3 eyes, double[] current, double[] previous,
                                     AABB target, double wallRange, Predicate<Vec3> occluded) {
      var sample = sampleBlockRotation(previous, (float)current[0], (float)current[1], current[2]);
      return predictsMovingBlockThreat(distanceSquared, eyes, sample, current[2], Vec3.ZERO, Vec3.ZERO,
         target, wallRange, (origin, hit) -> occluded.test(hit));
   }

   // Samples store yaw, pitch, time, yaw/tick and pitch/tick. Passive checks never replace them.
   static double[] sampleBlockRotation(double[] previous, float yaw, float pitch, double now) {
      double yawStep = 0, pitchStep = 0;
      double elapsed = previous == null ? 0 : now - previous[2];
      if (previous != null && previous.length >= 5 && elapsed >= 0 && elapsed < 0.5
          && Math.abs(Mth.wrapDegrees(yaw - (float)previous[0])) < 0.001 && Math.abs(pitch - previous[1]) < 0.001) {
         return previous;
      }
      if (elapsed > 0 && elapsed <= 2) {
         double ticks = Math.max(0.5, elapsed);
         yawStep = Mth.clamp(Mth.wrapDegrees(yaw - (float)previous[0]) / ticks, -60, 60);
         pitchStep = Mth.clamp((pitch - previous[1]) / ticks, -45, 45);
      }
      return new double[]{yaw, pitch, now, yawStep, pitchStep};
   }

   static double[] sampleBlockMovement(double[] previous, Vec3 position, double now) {
      Vec3 velocity = Vec3.ZERO;
      double elapsed = previous == null ? 0 : now - previous[3];
      if (elapsed > 0 && elapsed <= 3) {
         var change = position.subtract(previous[0], previous[1], previous[2]);
         // A teleport is a new observation, rather than movement to extrapolate.
         if (change.lengthSqr() <= 16) velocity = change.scale(1 / Math.max(0.5, elapsed));
      }
      return new double[]{position.x, position.y, position.z, now, velocity.x, velocity.y, velocity.z};
   }

   static boolean predictsMovingBlockThreat(double distanceSquared, Vec3 eyes, double[] rotation, double now,
                                           Vec3 attackerVelocity, Vec3 localVelocity, AABB target,
                                           double wallRange, BiPredicate<Vec3, Vec3> occluded) {
      if (distanceSquared > PREDICT_BLOCK_RANGE * PREDICT_BLOCK_RANGE) return false;
      double age = now - rotation[2];
      double yawStep = age >= 0 && age <= 2 ? rotation[3] : 0;
      double pitchStep = age >= 0 && age <= 2 ? rotation[4] : 0;
      // Sample the turn over the next two ticks so a fast sweep cannot skip the hitbox.
      for (int step = 0; step <= PREDICT_BLOCK_STEPS; step++) {
         double ticksAhead = step * 2.0 / PREDICT_BLOCK_STEPS;
         float yaw = (float)(rotation[0] + yawStep * ticksAhead);
         float pitch = (float)Mth.clamp(rotation[1] + pitchStep * ticksAhead, -90, 90);
         Vec3 origin = eyes.add(attackerVelocity.scale(ticksAhead));
         AABB predictedBox = target.move(localVelocity.scale(ticksAhead));
         Vec3 direction = Vec3.directionFromRotation(pitch, yaw);
         if (direction.dot(predictedBox.getCenter().subtract(origin)) <= 0) continue;
         if (raycastDistance(origin, direction, predictedBox.inflate(PREDICT_BLOCK_TOLERANCE),
             PREDICT_BLOCK_RANGE, wallRange, hit -> occluded.test(origin, hit)) >= 0) return true;
      }
      return false;
   }
}
