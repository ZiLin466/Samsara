package com.samsara.ui.hud.target;

import com.samsara.module.visual.Hud.TargetHud.OpaiTargetHudHealth;
import com.samsara.module.visual.Hud.TargetHud.OpaiTargetHudPainter;
import com.samsara.ui.clickgui.opai.OpaiStyle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class OpaiTargetHudPainterTest {
   @Test void longNamesRemainStableWhileHealthChangesAndStayInsideThePanel() {
      var surface = new Labels();
      String name = "PlayerWithAVeryLongName";
      var bounds = OpaiTargetHudPainter.bounds(surface, name, 20, 160, 100, 4096, -4096);
      assertTrue(bounds.x() >= 2 && bounds.x() + bounds.width() <= 158);
      assertTrue(bounds.y() >= 2 && bounds.y() + bounds.height() <= 98);
      String fitted = null;
      for (float health : new float[]{20, 10.6f, 9.9f, 0}) {
         OpaiTargetHudPainter.paint(surface, bounds, name,
            new OpaiTargetHudHealth.Sample(health, health, 20), false, OpaiStyle.LAVENDER);
         if (fitted == null) fitted = surface.name;
         assertEquals(fitted, surface.name, "The name must not flicker with the numeric health width");
         assertTrue(surface.right <= bounds.x() + bounds.width() - 3);
      }
      assertTrue(fitted.endsWith("…"));
   }

   private static final class Labels implements OpaiTargetHudPainter.Surface {
      String name;
      float right;
      int labels;
      @Override public float measure(String text) { return text.codePointCount(0, text.length()) * 5; }
      @Override public void panel(OpaiTargetHudPainter.Bounds bounds) { this.labels = 0; }
      @Override public void face(float x, float y, float size, float radius) { }
      @Override public void armor(int slot, float x, float y) { }
      @Override public void bar(float x, float y, float width, float height, int color) { }
      @Override public void text(String text, float x, float y, int color) {
         if (this.labels++ == 0) this.name = text;
         this.right = x + measure(text);
      }
   }
}
