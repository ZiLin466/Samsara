package com.samsara.ui.clickgui.opai;

import org.junit.jupiter.api.Test;
import static com.samsara.ui.clickgui.opai.OpaiContentLayout.Kind.*;
import static com.samsara.ui.clickgui.opai.OpaiLayout.*;
import static org.junit.jupiter.api.Assertions.*;

final class OpaiContentLayoutTest {
   @Test void bottomPaddingRemainsOutsideLastRowAtRestAndAfterScrolling() {
      var content = new OpaiContentLayout<String>();
      for (int i = 0; i < 30; i++) content.add("Row " + i, MODULE);
      float body = content.bodyHeight(120);
      float scroll = content.maxScroll(body);
      assertEquals("Row 29", content.hit(body - FOOTER_H - .01, scroll, body).target());
      assertNull(content.hit(body - FOOTER_H, scroll, body));
      assertNull(content.hit(body - .01, scroll, body));
      assertEquals(body - FOOTER_H, content.height() - scroll, .001);
      var bar = OpaiContentLayout.scrollbar(body, content.height(), scroll);
      assertEquals(scroll, OpaiContentLayout.scrollFromThumb(bar.top(), body, content.height()), .001);
      float full = content.bodyHeight(1000);
      assertEquals(FOOTER_H, full - content.height(), .001);
      assertEquals(0, content.maxScroll(full), .001);
   }

   @Test void expandedSettingsRetainTheirClipAndFooterThroughReveal() {
      var content = new OpaiContentLayout<String>();content.add("Module", MODULE);
      var section = new OpaiContentLayout<String>();
      section.add("Categories", MODE);section.add("Flag", BOOLEAN);
      content.reveal(section, .5f);
      float body = content.bodyHeight(200);
      assertEquals(ROW_H + section.height() * .5f + FOOTER_H, body, .001);
      assertEquals("Categories", content.hit(body - FOOTER_H - .01, 0, body).target());
      assertNull(content.hit(body - FOOTER_H + .01, 0, body));
      assertFalse(content.entries().getLast().visible());
   }
}
