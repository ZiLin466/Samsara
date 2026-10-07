package com.samsara.util;

import com.samsara.module.FeatureManager;
import java.util.function.Predicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class TargetFinder implements Wrapper {
   public static LivingEntity nearestEnemy(double range, boolean checkBots) { return nearest(range, checkBots, true, entity -> true); }
   public static LivingEntity nearestTarget(double range, boolean checkBots) { return nearest(range, checkBots, false, entity -> true); }

   public static LivingEntity nearest(double range, boolean checkBots, boolean ignoreTeammates, Predicate<LivingEntity> eligible) {
      if (mc.player == null || mc.level == null || FeatureManager.scaffold.isEnabled()) return null;
      LivingEntity nearest = null;
      double best = range * range;
      for (Entity entity : mc.level.entitiesForRendering()) {
         if (!(entity instanceof LivingEntity living) || !FeatureManager.targets.shouldAttack(entity)
            || ignoreTeammates && entity instanceof Player player && teammate(player)
            || !eligible.test(living)) continue;
         double distance = mc.player.distanceToSqr(entity);
         if (distance <= best) { best = distance; nearest = living; }
      }
      return nearest;
   }

   public static Entity nearestNonBotPlayer() {
      if (mc.player == null || mc.level == null) return null;
      Player nearest = null; double best = Double.MAX_VALUE;
      for (Player player : mc.level.players()) {
         if (player == mc.player || !player.isAlive() || FeatureManager.antiBot.isBot(player)) continue;
         double distance = mc.player.distanceToSqr(player);
         if (distance < best) { best = distance; nearest = player; }
      }
      return nearest;
   }

   private static boolean teammate(Player player) {
      if (mc.player.isAlliedTo(player)) return true;
      String local = mc.player.getDisplayName().getString(), other = player.getDisplayName().getString();
      return local.length() >= 2 && other.length() >= 2 && local.substring(0, 2).equals(other.substring(0, 2));
   }

   public static LivingEntity farthestDistantPlayer(boolean checkBots) {
      if (mc.player == null || mc.level == null || FeatureManager.scaffold.isEnabled()) return null;
      LivingEntity farthest = null;
      double best = 11;
      for (Entity entity : mc.level.entitiesForRendering()) {
         if (entity instanceof Player living && FeatureManager.targets.shouldAttack(entity)) {
            double distance = mc.player.distanceTo(entity);
            if (distance >= best) { best = distance; farthest = living; }
         }
      }
      return farthest;
   }
}
