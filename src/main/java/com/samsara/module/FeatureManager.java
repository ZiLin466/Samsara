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
import com.samsara.module.movement.InvMove;
import com.samsara.module.movement.KeepSprint;
import com.samsara.module.movement.LongJump;
import com.samsara.module.movement.MoveFix;
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
import com.samsara.module.player.InvManager;
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
import com.samsara.module.visual.InventoryHud;
import com.samsara.module.visual.NameTags;
import com.samsara.module.visual.NoHurtCam;
import com.samsara.module.visual.PlayerEsp;
import com.samsara.module.visual.Scoreboard;
import com.samsara.module.visual.TargetHud;
import com.samsara.module.visual.Theme;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;

public class FeatureManager {
   public static Velocity f28;
   public static AutoRod autoRod;
   public static com.samsara.module.combat.TargetSettings targets;
   public static Cape f38;
   public static Scaffold f29;
   public static Theme f24;
   public static AntiBot f27;
   public static KillAura f26;
   public static Scoreboard f32;
   private static List<Feature> sortedModules;
   public static AntiFire f31;
   public static BedAura f33;
   public static LongJump f35;
   public static Animations f34;
   public static Hud f25;
   public static ClickGui clickGui;
   public static InventoryHud inventoryHud;
   public static TargetHud targetHud;
   public static NameTags nameTags;
   public static com.samsara.module.visual.SessionHud sessionHud;
   public static com.samsara.module.visual.PotionStatus potionStatus;
   public static ChestStealer chestStealer;
   public static Whitelist f30;
   private static List<Feature> modules;
   public static KeepSprint f36;
   public static AntiSwim f37;
   public static Blink f39;
   public static Stasis f40;

   public static void loadEnabled() {
      sortedModules = new ArrayList<>(modules);
      sortModules();
   }

   public static void registerModules() {
      modules = new ArrayList<>();
      modules.add(targets = new com.samsara.module.combat.TargetSettings());
      modules.add(new AimAssist());
      modules.add(f27 = new AntiBot());
      modules.add(new AutoClicker());
      modules.add(new Criticals());
      modules.add(f26 = new KillAura());
      modules.add(new SprintReset());
      modules.add(new TriggerBot());
      modules.add(f28 = new Velocity());
      modules.add(autoRod = new AutoRod());
      modules.add(f37 = new AntiSwim());
      modules.add(new AutoWalk());
      modules.add(new Flight());
      modules.add(new InvMove());
      modules.add(f36 = new KeepSprint());
      modules.add(f35 = new LongJump());
      modules.add(new MoveFix());
      modules.add(new NoSlow());
      modules.add(new Speed());
      modules.add(new Sprint());
      modules.add(f40 = new Stasis());
      modules.add(new Timer());
      modules.add(f39 = new Blink());
      modules.add(new AutoTool());
      modules.add(new AutoHead());
      modules.add(new Backtrack());
      modules.add(f33 = new BedAura());
      modules.add(chestStealer = new ChestStealer());
      modules.add(new Eagle());
      modules.add(new FastMine());
      modules.add(new FastPlace());
      modules.add(new InvManager());
      modules.add(new LagRange());
      modules.add(new NoFall());
      modules.add(new NoJumpDelay());
      modules.add(f29 = new Scaffold());
      modules.add(new Ambience());
      modules.add(f34 = new Animations());
      modules.add(f31 = new AntiFire());
      modules.add(new BedPlates());
      modules.add(f38 = new Cape());
      modules.add(clickGui = new ClickGui());
      modules.add(new FullBright());
      modules.add(f25 = new Hud());
      modules.add(inventoryHud = new InventoryHud());
      modules.add(sessionHud = new com.samsara.module.visual.SessionHud());
      modules.add(potionStatus = new com.samsara.module.visual.PotionStatus());
      modules.add(nameTags = new NameTags());
      modules.add(new NoHurtCam());
      modules.add(new PlayerEsp());
      modules.add(f32 = new Scoreboard());
      modules.add(targetHud = new TargetHud());
      modules.add(f24 = new Theme());
      modules.add(new Disabler());
      modules.add(f30 = new Whitelist());
      modules.add(new WindCharge());
   }

   public static List<Feature> m19(Category var0) {
      ArrayList<Feature> var1 = new ArrayList<>();

      for (Feature var3 : modules) {
         if (var3.getCategory() == var0) {
            var1.add(var3);
         }
      }

      return var1;
   }

   public static void sortModules() {
      sortedModules.sort(
         (var0, var1) -> Integer.compare(Minecraft.getInstance().font.width(var1.getDisplayName()), Minecraft.getInstance().font.width(var0.getDisplayName()))
      );
   }

   public static List<Feature> m18() {
      return sortedModules;
   }

   public static List<Feature> getModules() {
      return modules;
   }
}
