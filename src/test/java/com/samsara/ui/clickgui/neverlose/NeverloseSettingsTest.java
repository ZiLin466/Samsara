package com.samsara.ui.clickgui.neverlose;

import com.samsara.config.ModuleConfigCodec;
import com.samsara.module.visual.ClickGui;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NeverloseSettingsTest {
   @Test
   void styleChoiceExposesOnlyItsApplicableControls() {
      ClickGui gui = new ClickGui();
      assertEquals("Opai", gui.style.m225());
      gui.style.m226("Neverlose");
      assertFalse(gui.renderMode.isVisible());
      assertFalse(gui.opaiColor.isVisible());
      gui.style.m226("Modern");
      assertTrue(gui.renderMode.isVisible());
      assertFalse(gui.opaiColor.isVisible());
      gui.style.m226("Opai");
      assertFalse(gui.renderMode.isVisible());
      assertTrue(gui.opaiColor.isVisible());
   }

   @Test
   void automaticStateKeepsStyleWhileGameplayPresetsOnlyKeepKeybind() {
      ClickGui gui = new ClickGui();
      gui.style.m226("Neverlose");
      var state = ModuleConfigCodec.snapshot(List.of(gui), ModuleConfigCodec.Scope.ALL, false);
      ClickGui restored = new ClickGui();
      ModuleConfigCodec.prepare(List.of(restored), state, ModuleConfigCodec.Scope.ALL).run();
      assertEquals("Neverlose", restored.style.m224());
      assertEquals(state, ModuleConfigCodec.snapshot(List.of(restored), ModuleConfigCodec.Scope.ALL, false));
      var gameplay = ModuleConfigCodec.snapshot(List.of(gui), ModuleConfigCodec.Scope.GAMEPLAY, false);
      assertFalse(gameplay.getAsJsonObject(gui.getName()).has("settings"));
      assertEquals(gui.getKey(), gameplay.getAsJsonObject(gui.getName()).get("key").getAsInt());
      var defaults = ModuleConfigCodec.snapshot(List.of(new ClickGui()), ModuleConfigCodec.Scope.GAMEPLAY, true);
      ModuleConfigCodec.prepare(List.of(restored), defaults, ModuleConfigCodec.Scope.GAMEPLAY).run();
      assertEquals("Neverlose", restored.style.m224());
   }
}
