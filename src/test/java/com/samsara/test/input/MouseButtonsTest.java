package com.samsara.test.input;

import com.samsara.ui.MouseButtons;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class MouseButtonsTest {
   @Test void sdlButtonsRetainUiToggleExpandBindAndSideButtonOrdering() {
      assertEquals(0, MouseButtons.normalize(1));
      assertEquals(1, MouseButtons.normalize(3));
      assertEquals(2, MouseButtons.normalize(2));
      assertEquals(3, MouseButtons.normalize(4));
      assertEquals(4, MouseButtons.normalize(5));
      assertEquals(6, MouseButtons.normalize(6));
   }
}
