package com.samsara.module;

import com.samsara.module.combat.AimAssist;
import com.samsara.module.combat.AntiBot;
import com.samsara.module.combat.AutoClicker;
import com.samsara.module.combat.AutoRod;
import com.samsara.module.combat.Criticals;
import com.samsara.module.combat.KillAura;
import com.samsara.module.combat.SprintReset;
import com.samsara.module.combat.TriggerBot;
import com.samsara.module.combat.Velocity;
import com.samsara.module.misc.Disabler;
import com.samsara.module.misc.Whitelist;
import com.samsara.module.misc.WindCharge;
import com.samsara.module.movement.AntiSwim;
import com.samsara.module.movement.AutoWalk;
import com.samsara.module.movement.Blink;
import com.samsara.module.movement.Flight;
import com.samsara.module.movement.InventoryMove;
import com.samsara.module.movement.KeepSprint;
import com.samsara.module.movement.LongJump;
import com.samsara.module.movement.MovementCorrection;
import com.samsara.module.movement.NoSlow;
import com.samsara.module.movement.Speed;
import com.samsara.module.movement.Sprint;
import com.samsara.module.movement.Stasis;
import com.samsara.module.movement.Timer;
import com.samsara.module.player.AutoHead;
import com.samsara.module.player.AutoTool;
import com.samsara.module.player.Backtrack;
import com.samsara.module.player.BedAura;
import com.samsara.module.player.ChestStealer;
import com.samsara.module.player.Eagle;
import com.samsara.module.player.FastMine;
import com.samsara.module.player.FastPlace;
import com.samsara.module.player.InventoryManager;
import com.samsara.module.player.LagRange;
import com.samsara.module.player.NoFall;
import com.samsara.module.player.NoJumpDelay;
import com.samsara.module.player.Scaffold;
import com.samsara.module.visual.Ambience;
import com.samsara.module.visual.Animations;
import com.samsara.module.visual.AntiFire;
import com.samsara.module.visual.BedPlates;
import com.samsara.module.visual.Cape;
import com.samsara.module.visual.ClickGui;
import com.samsara.module.visual.FullBright;
import com.samsara.module.visual.Hud;
import com.samsara.module.visual.NameTags;
import com.samsara.module.visual.NoHurtCamera;
import com.samsara.module.visual.PlayerEsp;
import com.samsara.module.visual.Scoreboard;
import com.samsara.module.visual.Theme;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;

public class FeatureManager {
   public static Velocity velocity;
   public static AutoRod autoRod;
   public static com.samsara.module.combat.TargetSettings targets;
   public static Cape cape;
   public static Scaffold scaffold;
   public static Theme theme;
   public static AntiBot antiBot;
   public static KillAura killAura;
   public static Scoreboard scoreboard;
   public static AntiFire antiFire;
   public static BedAura bedAura;
   public static LongJump longJump;
   public static Animations animations;
   public static Hud hud;
   public static ClickGui clickGui;
   public static NameTags nameTags;
   public static ChestStealer chestStealer;
   public static Whitelist whitelist;
   public static KeepSprint keepSprint;
   public static AntiSwim antiSwim;
   public static Blink blink;
   public static Stasis stasis;

   private static List<Feature> modules;
   private static List<Feature> sortedModules;

   public static void loadEnabled() {
      sortedModules = new ArrayList<>(modules);
      sortModules();
   }

   public static void clientTick() {
      for (Feature feature : modules) feature.initializeWorldState();
      for (Feature feature : modules) feature.clientTick();
   }

   public static void clientTickEnd() {
      if (modules == null) return;
      for (Feature feature : modules) feature.clientTickEnd();
   }

   public static void registerModules() {
      modules = new ArrayList<>();
      modules.add(targets = new com.samsara.module.combat.TargetSettings());
      modules.add(new AimAssist());
      modules.add(antiBot = new AntiBot());
      modules.add(new AutoClicker());
      modules.add(new Criticals());
      modules.add(killAura = new KillAura());
      modules.add(new SprintReset());
      modules.add(new TriggerBot());
      modules.add(velocity = new Velocity());
      modules.add(autoRod = new AutoRod());
      modules.add(antiSwim = new AntiSwim());
      modules.add(new AutoWalk());
      modules.add(new Flight());
      modules.add(new InventoryMove());
      modules.add(keepSprint = new KeepSprint());
      modules.add(longJump = new LongJump());
      modules.add(new MovementCorrection());
      modules.add(new NoSlow());
      modules.add(new Speed());
      modules.add(new Sprint());
      modules.add(stasis = new Stasis());
      modules.add(new Timer());
      modules.add(blink = new Blink());
      modules.add(new AutoTool());
      modules.add(new AutoHead());
      modules.add(new Backtrack());
      modules.add(bedAura = new BedAura());
      modules.add(chestStealer = new ChestStealer());
      modules.add(new Eagle());
      modules.add(new FastMine());
      modules.add(new FastPlace());
      modules.add(new InventoryManager());
      modules.add(new LagRange());
      modules.add(new NoFall());
      modules.add(new NoJumpDelay());
      modules.add(scaffold = new Scaffold());
      modules.add(new Ambience());
      modules.add(animations = new Animations());
      modules.add(antiFire = new AntiFire());
      modules.add(new BedPlates());
      modules.add(cape = new Cape());
      modules.add(clickGui = new ClickGui());
      modules.add(new FullBright());
      modules.add(hud = new Hud());
      modules.add(nameTags = new NameTags());
      modules.add(new NoHurtCamera());
      modules.add(new PlayerEsp());
      modules.add(scoreboard = new Scoreboard());
      modules.add(theme = new Theme());
      modules.add(new Disabler());
      modules.add(whitelist = new Whitelist());
      modules.add(new WindCharge());
   }

   public static List<Feature> getModulesByCategory(Category category) {
      ArrayList<Feature> matchingFeatures = new ArrayList<>();

      for (Feature feature : modules) {
         if (feature.getCategory() == category) {
            matchingFeatures.add(feature);
         }
      }

      return matchingFeatures;
   }

   public static void sortModules() {
      sortedModules.sort(
         (leftFeature, rightFeature) -> Integer.compare(Minecraft.getInstance().font.width(rightFeature.getDisplayName()), Minecraft.getInstance().font.width(leftFeature.getDisplayName()))
      );
   }

   public static List<Feature> getSortedModules() {
      return sortedModules;
   }

   public static List<Feature> getModules() {
      return modules;
   }
}
