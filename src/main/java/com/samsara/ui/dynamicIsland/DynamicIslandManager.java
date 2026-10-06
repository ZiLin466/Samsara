package com.samsara.ui.dynamicIsland;

import com.samsara.module.Feature;
import com.samsara.module.FeatureManager;
import com.samsara.module.player.Scaffold.ScaffoldBpsTracker;
import com.samsara.module.player.ChestStealer;
import com.samsara.module.player.ChestStealer.IslandGeometry;
import com.samsara.module.player.ChestStealer.IslandView;
import com.samsara.module.visual.ClickGui;
import com.samsara.module.visual.Hud;
import com.samsara.ui.clickgui.neverlose.NeverloseClickGuiScreen;
import com.samsara.ui.dynamicIsland.DynamicIslandState.Icon;
import com.samsara.util.InventoryUtil;
import com.samsara.util.TimerController;
import com.samsara.util.render.FontRepository;
import com.samsara.util.render.HudBackdrop;
import com.samsara.util.render.NVGRenderer;
import java.util.Locale;
import java.util.Comparator;
import java.util.regex.Pattern;
import mixins.MinecraftAccessor;
import mixins.PlayerTabOverlayAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.locale.Language;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import static org.lwjgl.nanovg.NanoVG.*;

public final class DynamicIslandManager {
   private static final Minecraft MC = Minecraft.getInstance();
   private static final DynamicIslandState STATE = new DynamicIslandState();
   private static final ScaffoldBpsTracker SCAFFOLD_BPS = new ScaffoldBpsTracker();
   private static final ServerLabel SERVER_LABEL = new ServerLabel();
   private static Sample extracted;
   private record Sample(DynamicIslandState.Frame frame, float viewportWidth, float scale,
                         IslandView chest, Screen screen) {
      IslandGeometry geometry() { return new IslandGeometry(frame, viewportWidth, scale); }
   }

   private DynamicIslandManager() { }

   public static synchronized boolean shouldRender(Screen current) {
      if (current instanceof NeverloseClickGuiScreen) {
         // Neverlose uses the full window; stale island frames must not leak into it.
         extracted = null;
         STATE.clear();
         SCAFFOLD_BPS.reset();
         return false;
      }
      if (MC.player == null || MC.level == null) {
         extracted = null;
         STATE.clear();
         SCAFFOLD_BPS.reset();
         if (MC.getConnection() == null) SERVER_LABEL.reset();
         return false;
      }
      if (!Hud.enabled(Hud.Widget.STATUS_BAR)) {
         extracted = null;
         STATE.clear();
         SCAFFOLD_BPS.reset();
         return false;
      }
      return MC.gui.overlay() == null && (MC.gui.hud.isHidden()
         || !((PlayerTabOverlayAccessor)MC.gui.hud.getTabList()).isVisible());
   }

   public static synchronized void onModuleToggled(Feature module) {
      if (module == null) {
         return;
      }
      if (module == FeatureManager.f29) {
         SCAFFOLD_BPS.reset();
         if (!module.isEnabled()) {
            STATE.remove("scaffold");
         }
      }
      if (MC.player == null || !Hud.enabled(Hud.Widget.STATUS_BAR)) {
         return;
      }
      String label = module.getName().replaceAll("(?<=[a-z])(?=[A-Z])", " ");
      STATE.post("module:" + module.getName(), "Module Toggled", label + " has been ",
         module.isEnabled() ? "Enabled" : "Disabled", Icon.TOGGLE, module.isEnabled(), now(), DynamicIslandState.TOGGLE_MS);
   }

   public static synchronized void notifySuccess(String title, String description) {
      notify(title, description, Icon.SUCCESS, 2600);
   }

   public static synchronized void notifyWarning(String title, String description) {
      notify(title, description, Icon.WARNING, 3000);
   }

   public static synchronized void notifyInfo(String title, String description) {
      notify(title, description, Icon.INFO, 2600);
   }

   private static void notify(String title, String description, Icon icon, long duration) {
      if (MC.player != null && Hud.enabled(Hud.Widget.STATUS_BAR)) {
         STATE.post(icon + ":" + title + ":" + description, title, description, "", icon, false, now(), duration);
      }
   }

   private static long now() {
      return System.nanoTime() / 1_000_000L;
   }

   private static DynamicIslandNanoSurface surface() {
      return new DynamicIslandNanoSurface(NVGRenderer.getContext(),
         FontRepository.getFont(DynamicIslandNanoSurface.FONT).getFontId(),
         FontRepository.getFont(DynamicIslandNanoSurface.IDLE_FONT).getFontId());
   }

   public static synchronized void beginExtraction() { extracted = null; }

   public static synchronized boolean replacesContainer(Screen screen) {
      return FeatureManager.chestStealer != null && NVGRenderer.isAvailable() && shouldRender(screen)
         && FeatureManager.chestStealer.replacesContainer(screen);
   }

   public static synchronized boolean extractChestItems(Screen screen, GuiGraphicsExtractor graphics) {
      if (!replacesContainer(screen)) return false;
      extracted = sample(now(), screen);
      if (extracted.chest() == null) return false;
      graphics.nextStratum();
      screen.extractTransparentBackground(graphics);
      MC.gui.hud.extractDeferredSubtitles();
      graphics.nextStratum();
      FeatureManager.chestStealer.extractIslandItems(graphics, extracted.chest(), extracted.geometry());
      return true;
   }

   private static Sample sample(long now, Screen screen) {
      boolean reducedMotion = MC.options.screenEffectScale().get() <= 0;
      IslandView chest = FeatureManager.chestStealer == null ? null
         : FeatureManager.chestStealer.islandView(screen, now, reducedMotion);
      if (chest == null) {
         updateScaffold(now);
         updateBedAura(now);
      }
      float screenWidth = MC.getWindow().getGuiScaledWidth();
      float scale = com.samsara.ui.hud.HudLayouts.INSTANCE.get(com.samsara.ui.hud.HudLayouts.Element.ISLAND).scale();
      // The complete grid also fits at large HUD scales or in narrow windows.
      if (chest != null) scale = Math.min(scale, Math.min(Math.max(1, screenWidth - 16) / 190f,
         Math.max(1, MC.getWindow().getGuiScaledHeight() - DynamicIslandState.IDLE_TOP - 8) / chest.panel().height()));
      float logicalWidth = screenWidth / scale;
      long vg = NVGRenderer.getContext();
      nvgSave(vg);
      try {
         var frame = STATE.frame(now, logicalWidth, defaultStatus(now), surface(), reducedMotion,
            chest == null ? null : chest.panel());
         return new Sample(frame, logicalWidth, scale, chest, screen);
      } finally {
         nvgRestore(vg);
      }
   }

   public static synchronized void renderNano() {
      if (!shouldRender(MC.gui.screen())) return;
      Sample sample = extracted != null ? extracted : sample(now(), MC.gui.screen());
      var frame = sample.frame();
      float scale = sample.scale();
      float screenWidth = MC.getWindow().getGuiScaledWidth();
      var box = new com.samsara.ui.hud.HudLayouts.Box((screenWidth - frame.width() * scale) / 2,
         DynamicIslandState.top(frame), frame.width() * scale, frame.height() * scale);
      com.samsara.ui.hud.HudLayouts.INSTANCE.drawn(com.samsara.ui.hud.HudLayouts.Element.ISLAND, box);
      HudBackdrop.island(box.x(), box.y(), box.width(), box.height(), DynamicIslandState.radius(frame) * scale);
      long vg = NVGRenderer.getContext();
      nvgSave(vg);
      try {
         nvgTranslate(vg, 0, DynamicIslandState.top(frame) * (1 - scale));
         nvgScale(vg, scale, scale);
         // Chest extraction draws its shell between the vanilla dimmer and native items.
         if (sample.chest() == null)
            DynamicIslandPainter.paint(surface(), frame, sample.viewportWidth(), ClickGui.currentOpaiPalette());
      } finally {
         nvgRestore(vg);
      }
   }

   public static synchronized boolean hasChestOverlay() {
      return extracted != null && extracted.chest() != null && extracted.screen() == MC.gui.screen()
         && replacesContainer(MC.gui.screen());
   }

   public static synchronized void renderChestOverlay() {
      if (!hasChestOverlay()) return;
      long vg = NVGRenderer.getContext();
      nvgSave(vg);
      try {
         nvgTranslate(vg, 0, extracted.frame().top() * (1 - extracted.scale()));
         nvgScale(vg, extracted.scale(), extracted.scale());
         ChestStealer.paintIslandOverlay(surface(), extracted.chest(), extracted.geometry());
      } finally {
         nvgRestore(vg);
      }
   }

   private static DynamicIslandStatus defaultStatus(long now) {
      String username = "Player";
      if (MC instanceof MinecraftAccessor accessor && accessor.getUser() != null) {
         username = accessor.getUser().getName();
      }
      int ping = -1;
      if (MC.player != null && MC.getConnection() != null) {
         var connection = MC.getConnection();
         PlayerInfo info = connection.getPlayerInfo(MC.player.getUUID());
         if (info != null) {
            ping = Math.max(0, info.getLatency());
         }
         if (connection instanceof DynamicIslandLatency.Source source) {
            ping = source.samsara$getLivePing(now, ping);
         }
      }
      var server = MC.getCurrentServer();
      String address = SERVER_LABEL.address(MC.getConnection(), MC.hasSingleplayerServer(),
         server != null ? server.ip : "server", MC.level == null ? null : MC.level.getScoreboard(),
         MC.player == null ? null : MC.player.getScoreboardName());
      return new DynamicIslandStatus(username, address, ping, MC.getFps());
   }

   static final class ServerLabel {
      private static final Pattern HYPIXEL = Pattern.compile("(?i)(?<![\\w.-])www\\.hypixel\\.(?:net|com)(?![\\w.-])");
      private static final Comparator<PlayerScoreEntry> DISPLAY_ORDER = Comparator
         .comparingInt(PlayerScoreEntry::value).reversed().thenComparing(PlayerScoreEntry::owner, String.CASE_INSENSITIVE_ORDER);
      private Object connection;
      private boolean hypixel;

      String address(Object connection, boolean singleplayer, String address, Scoreboard scoreboard, String playerName) {
         if (singleplayer || connection == null) {
            reset();
            return singleplayer ? null : "server";
         }
         if (this.connection != connection) {
            reset();
            this.connection = connection;
         }
         if (!this.hypixel && scoreboard != null) this.hypixel = hasHypixelFooter(scoreboard, playerName);
         return this.hypixel ? "mc.hypixel.net" : address;
      }

      void reset() {
         this.connection = null;
         this.hypixel = false;
      }

      private static boolean hasHypixelFooter(Scoreboard scoreboard, String playerName) {
         var team = playerName == null ? null : scoreboard.getPlayersTeam(playerName);
         var objective = team == null || team.getColor().isEmpty() ? null
            : scoreboard.getDisplayObjective(team.getColor().get().displaySlot());
         if (objective == null) objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
         if (objective == null) return false;
         return scoreboard.listPlayerScores(objective).stream().filter(entry -> !entry.isHidden())
            .sorted(DISPLAY_ORDER).limit(15).anyMatch(entry -> {
               var entryTeam = scoreboard.getPlayersTeam(entry.owner());
               String text = PlayerTeam.formatNameForTeam(entryTeam, entry.ownerName()).getString().replaceAll("§.", "");
               return HYPIXEL.matcher(text).find();
            });
      }
   }

   /** Called by Scaffold's existing post-motion event; this only observes player movement. */
   public static synchronized void sampleScaffoldMovement() {
      if (MC.player == null || MC.level == null) {
         SCAFFOLD_BPS.reset();
         return;
      }
      SCAFFOLD_BPS.sample(MC.player, MC.level, MC.player.tickCount,
         MC.player.getX(), MC.player.getZ(), TimerController.getMultiplier());
   }

   private static void updateScaffold(long now) {
      if (FeatureManager.f29 == null || !FeatureManager.f29.isEnabled() || MC.player == null || MC.level == null) {
         STATE.remove("scaffold");
         SCAFFOLD_BPS.reset();
         return;
      }
      int blocks = Math.max(0, InventoryUtil.m41());
      String detail = String.format(Locale.ROOT, "%d blocks left - %.1f block/s",
         blocks, SCAFFOLD_BPS.blocksPerSecond(MC.player, MC.level));
      STATE.postScaffold(detail, Math.min(1, blocks / 100f), now);
   }

   private static void updateBedAura(long now) {
      var bedAura = FeatureManager.f33;
      var target = bedAura == null ? null : bedAura.diggingTarget(MC.getDeltaTracker().getGameTimeDeltaPartialTick(false));
      if (target == null) {
         STATE.remove("bed-aura");
         return;
      }
      // DEFAULT_INSTANCE is the immutable built-in en_us language, independent of Language.inject().
      String blockName = Language.DEFAULT_INSTANCE.getOrDefault(target.state().getBlock().getDescriptionId());
      STATE.postBreaking(blockName, target.progress(), now);
   }

}
