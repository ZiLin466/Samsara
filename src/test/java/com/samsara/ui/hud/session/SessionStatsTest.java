package com.samsara.ui.hud.session;

import com.samsara.module.visual.Hud.SessionHud.SessionStats;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class SessionStatsTest {
   @Test void timeIncludesOnlyActivePlayAndNoPauseReturnInterval() {
      var stats=new SessionStats(); stats.tick(0,true);
      for(int i=1;i<=90;i++)stats.tick(i*1000,true);
      assertEquals("1m 30s",stats.time());
      stats.tick(91000,false); stats.tick(100000,false); stats.tick(200000,true);
      assertEquals("1m 30s",stats.time()); stats.tick(201000,true); assertEquals("1m 31s",stats.time());
      assertEquals("1h 1m 1s",SessionStats.formatTime(3661));
   }
   @Test void killsRequireRecentLocalAttackAndConfirmedDeathWithDeduplication() {
      var stats=new SessionStats(); var victim=UUID.randomUUID();
      assertFalse(stats.death(victim,0)); stats.attack(victim,100);
      assertTrue(stats.death(victim,200)); assertFalse(stats.death(victim,300));
      stats.attack(victim,400); assertFalse(stats.death(victim,500));
      var other=UUID.randomUUID(); stats.attack(other,600); assertFalse(stats.death(other,16000));
      stats.clearCombat(); assertEquals(1,stats.kills());
   }
   @Test void repeatedVictoryPacketsAreOneWinAndOtherTitlesAreIgnored() {
      var stats=new SessionStats(); stats.victory("§aVICTORY!",0);stats.victory("VICTORY!",1000);
      stats.victory("DEFEAT!",16000);assertEquals(1,stats.wins());
      stats.victory("VICTORY!",30000);assertEquals(2,stats.wins());
   }
   @Test void theSamePlayerCanBeCountedAgainAfterRespawning() {
      var stats=new SessionStats();var player=UUID.randomUUID();
      stats.attack(player,101,0);assertTrue(stats.death(player,101,100));
      stats.attack(player,102,200);assertFalse(stats.death(player,101,250));
      assertTrue(stats.death(player,102,300));assertEquals(2,stats.kills());
   }
}
