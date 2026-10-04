package com.samsara.ui.dynamicIsland;

import java.util.Optional;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.TeamColor;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class DynamicIslandServerLabelTest {
   private static final String ADDRESS = "private-relay.example:25565";

   @BeforeAll static void bootstrap() {
      SharedConstants.tryDetectVersion();
      Bootstrap.bootStrap();
   }

   private static Objective sidebar(Scoreboard scoreboard, String name, DisplaySlot slot) {
      var objective = scoreboard.addObjective(name, ObjectiveCriteria.DUMMY, Component.literal(name),
         ObjectiveCriteria.RenderType.INTEGER, false, null);
      scoreboard.setDisplayObjective(slot, objective);
      return objective;
   }

   private static void line(Scoreboard scoreboard, Objective objective, String owner, int value, String display) {
      var score = scoreboard.getOrCreatePlayerScore(ScoreHolder.forNameOnly(owner), objective);
      score.set(value);
      if (display != null) score.display(Component.literal(display));
   }

   @Test void footerDomainsAndFormattingReplaceRelayAddressInIslandContent() {
      for (String footer : new String[]{"www.hypixel.net", "§eWWW.HYPIXEL.COM§r", "Visit www.hypixel.net"}) {
         var scoreboard = new Scoreboard();
         line(scoreboard, sidebar(scoreboard, "game", DisplaySlot.SIDEBAR), "footer", 0, footer);
         var labels = new DynamicIslandManager.ServerLabel();
         var address = labels.address(new Object(), false, ADDRESS, scoreboard, "Player");
         assertEquals("mc.hypixel.net", address);
         var status = new DynamicIslandStatus("Player", address, 56, 240);
         String rendered = status.layout((text, size) -> text.length() * 4, 1000).parts().stream()
            .map(DynamicIslandStatus.Part::text).collect(java.util.stream.Collectors.joining());
         assertTrue(rendered.contains("56ms to mc.hypixel.net"));
         assertFalse(rendered.contains(ADDRESS));
      }
   }

   @Test void teamPrefixOwnerAndSuffixAreReadAsOneVisibleLine() {
      var scoreboard = new Scoreboard();
      var objective = sidebar(scoreboard, "game", DisplaySlot.SIDEBAR);
      var team = scoreboard.addPlayerTeam("footer");
      team.setPlayerPrefix(Component.literal("§ewww.hypi"));
      team.setPlayerSuffix(Component.literal("xel.net§r"));
      scoreboard.addPlayerToTeam("§a", team);
      line(scoreboard, objective, "§a", 0, null);
      assertEquals("mc.hypixel.net", new DynamicIslandManager.ServerLabel()
         .address(new Object(), false, ADDRESS, scoreboard, "Player"));
   }

   @Test void recognitionSurvivesBoardRefreshButResetsForNewConnectionsAndSingleplayer() {
      var scoreboard = new Scoreboard();
      line(scoreboard, sidebar(scoreboard, "game", DisplaySlot.SIDEBAR), "www.hypixel.net", 0, null);
      var labels = new DynamicIslandManager.ServerLabel();
      var connection = new Object();
      assertEquals("mc.hypixel.net", labels.address(connection, false, ADDRESS, scoreboard, "Player"));
      scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, null);
      assertEquals("mc.hypixel.net", labels.address(connection, false, ADDRESS, scoreboard, "Player"));
      assertEquals(ADDRESS, labels.address(new Object(), false, ADDRESS, scoreboard, "Player"));
      assertNull(labels.address(connection, true, ADDRESS, scoreboard, "Player"));
      assertEquals("server", labels.address(null, false, ADDRESS, scoreboard, "Player"));
      assertEquals(ADDRESS, labels.address(connection, false, ADDRESS, scoreboard, "Player"));
   }

   @Test void hiddenLinesInactiveObjectivesAndSimilarDomainsCannotIdentifyHypixel() {
      var scoreboard = new Scoreboard();
      var sidebar = sidebar(scoreboard, "game", DisplaySlot.SIDEBAR);
      line(scoreboard, sidebar, "#hidden", 10, "www.hypixel.net");
      line(scoreboard, sidebar, "fake", 9, "www.hypixel.net.evil.example");
      line(scoreboard, sidebar, "other", 8, "fakewww.hypixel.com");
      line(scoreboard, sidebar(scoreboard, "tab", DisplaySlot.LIST), "www.hypixel.net", 1, null);
      assertEquals(ADDRESS, new DynamicIslandManager.ServerLabel()
         .address(new Object(), false, ADDRESS, scoreboard, "Player"));
   }

   @Test void viewersColoredSidebarHasPriorityOverDefaultSidebar() {
      var scoreboard = new Scoreboard();
      line(scoreboard, sidebar(scoreboard, "default", DisplaySlot.SIDEBAR), "www.hypixel.net", 0, null);
      var viewerTeam = scoreboard.addPlayerTeam("viewer");
      viewerTeam.setColor(Optional.of(TeamColor.GREEN));
      scoreboard.addPlayerToTeam("Player", viewerTeam);
      var colored = sidebar(scoreboard, "green", DisplaySlot.TEAM_GREEN);
      line(scoreboard, colored, "footer", 0, "other.example");
      var labels = new DynamicIslandManager.ServerLabel();
      var connection = new Object();
      assertEquals(ADDRESS, labels.address(connection, false, ADDRESS, scoreboard, "Player"));
      line(scoreboard, colored, "footer", 0, "www.hypixel.net");
      assertEquals("mc.hypixel.net", labels.address(connection, false, ADDRESS, scoreboard, "Player"));
   }

   @Test void entriesBelowVanillasFifteenLineLimitAreIgnored() {
      var scoreboard = new Scoreboard();
      var objective = sidebar(scoreboard, "game", DisplaySlot.SIDEBAR);
      for (int i = 1; i <= 15; i++) line(scoreboard, objective, "line" + i, i, null);
      line(scoreboard, objective, "www.hypixel.net", 0, null);
      assertEquals(ADDRESS, new DynamicIslandManager.ServerLabel()
         .address(new Object(), false, ADDRESS, scoreboard, "Player"));
   }
}
