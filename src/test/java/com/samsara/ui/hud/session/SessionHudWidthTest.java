package com.samsara.ui.hud.session;

import com.samsara.module.visual.SessionHud.SessionHudPainter;
import com.samsara.module.visual.SessionHud.SessionStats;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class SessionHudWidthTest {
   private final SessionHudPainter.Canvas canvas=new SessionHudPainter.Canvas() {
      public float measure(String text) { return text.length()*5; }
      public void panel(com.samsara.module.visual.TargetHud.OpaiTargetHudPainter.Bounds bounds) { }
      public void face(float x,float y,float size,float radius) { }
      public void armor(int slot,float x,float y) { }
      public void text(String text,float x,float y,int color) { }
      public void bar(float x,float y,float w,float h,int color) { }
      public void icon(boolean skull,float x,float y,float size) { }
   };
   @Test void referenceWidthIsStableUntilOneHourAndReservesAllMinuteSecondDigits() {
      for (long seconds:new long[]{0,9,10,59,60,599,600,3599})
         assertEquals(150,SessionHudPainter.width(canvas,SessionStats.formatTime(seconds),0,0));
      int hourly=SessionHudPainter.width(canvas,SessionStats.formatTime(3600),0,0);
      assertTrue(hourly>150);
      assertEquals(hourly,SessionHudPainter.width(canvas,SessionStats.formatTime(7199),0,0));
      assertEquals(hourly,SessionHudPainter.width(canvas,SessionStats.formatTime(32399),0,0));
   }
}
