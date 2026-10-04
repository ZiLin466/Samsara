package com.samsara.module.combat;

import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.module.FeatureManager;
import com.samsara.setting.MultiSelectSetting;
import java.util.List;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.entity.animal.AgeableWaterCreature;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/** Global selections are persisted with modules but edited in the Settings panel. */
public final class TargetSettings extends Feature {
   private static final String[] COMBAT = {"Players", "Hostile", "Angerable", "Water Creature", "Passive", "Invisible", "Dead", "Sleeping", "Friends"};
   private static final String[] VISUAL = {"Self", "Players", "Hostile", "Angerable", "Water Creature", "Passive", "Invisible", "Dead", "Sleeping", "Friends"};
   private static final List<String> DEFAULTS = List.of("Players", "Hostile", "Angerable", "Water Creature", "Invisible");
   public final MultiSelectSetting combat = new MultiSelectSetting("Combat", this, COMBAT, DEFAULTS);
   public final MultiSelectSetting visual = new MultiSelectSetting("Visual", this, VISUAL, DEFAULTS);

   public TargetSettings() { super("Targets", Category.COMBAT); }
   @Override public boolean isHidden() { return true; }
   @Override public void setEnabled(boolean enabled) { }
   @Override public void restoreEnabled(boolean enabled) { }
   @Override public void toggle() { }

   public boolean shouldAttack(Entity entity) { return accepts(entity, false); }
   public boolean shouldShow(Entity entity) { return accepts(entity, true); }

   private boolean accepts(Entity entity, boolean rendering) {
      if (!(entity instanceof LivingEntity living) || entity.isRemoved() || mc.player == null) return false;
      MultiSelectSetting selection = rendering ? this.visual : this.combat;
      if (entity == mc.player || entity.hasPassenger(mc.player)) {
         return rendering && selection.contains("Self") && !mc.options.getCameraType().isFirstPerson();
      }
      boolean friend = entity instanceof Player player && com.samsara.util.Friends.contains(player.getGameProfile().name());
      String kind = classify(entity);
      return allowed(selection.selectedValues(), kind, living.isAlive() && living.deathTime <= 0,
         living.isInvisible(), living.isSleeping(), friend)
         && (!(entity instanceof Player) || FeatureManager.f27 == null || !FeatureManager.f27.m136(entity));
   }

   private static String classify(Entity entity) {
      if (entity instanceof Player) return "Players";
      if (entity instanceof WaterAnimal || entity instanceof AgeableWaterCreature) return "Water Creature";
      if (entity instanceof NeutralMob) return "Angerable";
      if (entity instanceof Enemy) return "Hostile";
      if (entity instanceof AgeableMob || entity instanceof Bat || entity instanceof Allay) return "Passive";
      return "";
   }

   public static boolean allowed(List<String> selected, String kind, boolean alive, boolean invisible, boolean sleeping, boolean friend) {
      return (!friend || selected.contains("Friends"))
         && (alive || selected.contains("Dead"))
         && (!invisible || selected.contains("Invisible"))
         && (!sleeping || selected.contains("Sleeping"))
         && (selected.contains(kind) || kind.equals("Players") && friend && selected.contains("Friends"));
   }
}
