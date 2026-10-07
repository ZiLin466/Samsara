package com.samsara.ui.clickgui.neverlose;

import java.util.List;
import org.junit.jupiter.api.Test;

import static com.samsara.ui.clickgui.neverlose.NeverloseLayout.*;
import static org.junit.jupiter.api.Assertions.*;

class NeverloseLayoutTest {
   @Test
   void scaledAndAnimatedCoordinatesRoundTrip() {
      for (int[] size : List.of(new int[]{1920, 1080}, new int[]{960, 540}, new int[]{320, 180})) {
         Viewport v = centered(size[0], size[1]);
         for (float animation : new float[]{.975f, .99f, 1}) {
            Rect track = slider(new Rect(202, 140, 256, ROW));
            float x = track.x() + track.width() * .72f, y = 151;
            float screenX = v.x() + WIDTH * v.scale() / 2 + (x - WIDTH / 2) * v.scale() * animation;
            float screenY = v.y() + HEIGHT * v.scale() / 2 + (y - HEIGHT / 2) * v.scale() * animation;
            assertEquals(x, v.localX(screenX, animation), .001f);
            assertEquals(y, v.localY(screenY, animation), .001f);
            assertEquals(.72f, fraction(v.localX(screenX, animation), track), .0001f);
            Rect backdrop = v.screen(SIDEBAR_BACKDROP, animation);
            assertEquals(0, v.localX(backdrop.x(), animation), .001f);
            assertEquals(0, v.localY(backdrop.y(), animation), .001f);
            assertEquals(SIDEBAR + RADIUS, v.localX(backdrop.right(), animation), .001f);
            assertEquals(HEIGHT, v.localY(backdrop.bottom(), animation), .001f);
            assertTrue(v.x() >= 4 && v.x() + WIDTH * v.scale() <= size[0] - 4);
            assertTrue(v.y() >= 4 && v.y() + HEIGHT * v.scale() <= size[1] - 4);
         }
      }
   }

   @Test
   void columnsPackWithoutOverlapAndKeepSameGeometryWhenScrolling() {
      Packed layout = pack(List.of(490f, 145f, 112f, 300f, 29f), 0);
      assertEquals(CONTENT.x(), layout.sections().get(0).x());
      assertEquals(CONTENT.y(), layout.sections().get(0).y());
      assertEquals(CONTENT.y(), layout.sections().get(1).y());
      for (int i = 0; i < layout.sections().size(); i++) {
         Rect sectionBounds = layout.sections().get(i);
         for (int j = i + 1; j < layout.sections().size(); j++) {
            Rect overlap = sectionBounds.intersect(layout.sections().get(j));
            assertEquals(0, overlap.width() * overlap.height());
         }
      }
      Packed scrolled = pack(List.of(490f, 145f, 112f, 300f, 29f), 89);
      for (int i = 0; i < layout.sections().size(); i++) {
         assertEquals(layout.sections().get(i).y() - 89, scrolled.sections().get(i).y());
         assertEquals(layout.sections().get(i).x(), scrolled.sections().get(i).x());
      }
      assertEquals(layout.height(), scrolled.height());
   }

   @Test
   void hiddenRowsAndOffscreenRowsHaveNoHitArea() {
      Rect section = new Rect(CONTENT.x(), 70, 256, 57);
      Rect enabled = new Rect(section.x(), section.y() + SECTION_HEADER, section.width(), ROW);
      Rect hidden = new Rect(section.x(), enabled.bottom(), section.width(), ROW);
      Rect enabledHit = enabled.intersect(section).intersect(CONTENT);
      assertTrue(enabledHit.contains(enabled.x() + 10, enabled.y() + 10));
      assertFalse(enabledHit.contains(enabled.x() + 10, CONTENT.y() - 1));
      assertEquals(0, hidden.intersect(section).intersect(CONTENT).height());
   }

   @Test
   void changingSettingsAndCollapsingSectionsPreservesColumnMembership() {
      List<Float> original = List.of(490f, 300f, 112f, 100f, 80f);
      List<Integer> columns = pack(original, 0).sections().stream().map(rect -> rect.x() == CONTENT.x() ? 0 : 1).toList();
      Packed changed = pack(List.of(28f, 74f, 112f, 100f, 80f), columns, 0);
      for (int i = 0; i < original.size(); i++) {
         assertEquals(pack(original, columns, 0).sections().get(i).x(), changed.sections().get(i).x());
      }
   }

   @Test
   void longDropdownFitsAndFlipsAboveTheBottomRow() {
      Rect anchor = new Rect(612, 575, 126, 20);
      Rect popup = dropdown(anchor, 31);
      assertTrue(popup.bottom() < anchor.y());
      assertTrue(popup.y() >= HEADER);
      assertTrue(popup.right() <= WIDTH - 10);
      assertTrue(popup.height() < OPTION * 31);
      assertTrue(dropdown(PRESET, 2).y() > PRESET.bottom());
   }

   @Test
   void scrollThumbReachesBothEndsAndNeverLeavesTheTrack() {
      for (float maximum : new float[]{10, 450, 8000}) {
         Rect first = thumb(0, maximum), last = thumb(maximum, maximum);
         assertEquals(CONTENT.y(), first.y(), .001f);
         assertEquals(CONTENT.bottom(), last.bottom(), .001f);
         assertTrue(first.height() >= 24);
      }
   }

   @Test
   void animationMatchesElapsedTimeAcrossFrameRatesAndReversesContinuously() {
      Motion coarse = new Motion(0, 18);
      coarse.snap(0, 0); coarse.to(1, 0);
      float finalValue = coarse.to(1, .2);
      for (int fps : new int[]{30, 60, 144}) {
         Motion fine = new Motion(0, 18);
         fine.snap(0, 0); fine.to(1, 0);
         for (int i = 1; i <= (int)(fps * .2); i++) fine.to(1, i / (double)fps);
         fine.to(1, .2);
         assertEquals(finalValue, fine.value(), .00001f);
         assertEquals(coarse.velocity(), fine.velocity(), .00001);
      }
      float beforeReverse = coarse.to(1, .21);
      assertEquals(beforeReverse, coarse.to(0, .21));
      float exiting = coarse.to(0, .24);
      assertTrue(exiting > 0 && exiting < beforeReverse);
      double velocity = coarse.velocity();
      assertEquals(exiting, coarse.to(1, .24));
      assertEquals(velocity, coarse.velocity(), .00001);
      assertTrue(coarse.to(1, .55) > exiting);
   }

   @Test
   void sdlButtonsKeepLeftRightAndMiddleDistinct() {
      assertEquals(0, mouseButton(1));
      assertEquals(1, mouseButton(3));
      assertEquals(2, mouseButton(2));
   }
}
