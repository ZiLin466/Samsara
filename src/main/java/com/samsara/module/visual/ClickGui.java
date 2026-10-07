package com.samsara.module.visual;

import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.module.FeatureManager;
import com.samsara.setting.ModeSetting;
import com.samsara.ui.clickgui.ClickGuiScreen;
import com.samsara.ui.clickgui.neverlose.NeverloseClickGuiScreen;
import com.samsara.ui.clickgui.opai.OpaiClickGuiScreen;
import com.samsara.ui.clickgui.opai.OpaiStyle;
import com.samsara.ui.clickgui.RockstarClickGuiScreen;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

public class ClickGui extends Feature {
   private static final String CLICK_GUI_LABEL = "ClickGUI";

   public final ModeSetting style = new ModeSetting("Style", this, "Opai", new String[]{"Modern", "Opai", "Neverlose"});
   public final ModeSetting renderMode = new ModeSetting("Interface", this, "LiquidGlass", new String[]{"LiquidGlass", "Normal"});
   public final ModeSetting opaiColor = new ModeSetting("Opai Color", this, "Lavender", new String[]{"Lavender", "Light Pink"});

   private Screen activeScreen;
   private OpaiClickGuiScreen opaiScreen;
   private NeverloseClickGuiScreen neverloseScreen;

   /** HUDs read the saved ClickGUI choice every frame, even while the GUI is closed. */
   public static OpaiStyle.Palette currentOpaiPalette() {
      ClickGui gui = FeatureManager.clickGui;
      return gui == null ? OpaiStyle.LAVENDER : OpaiStyle.palette(gui.opaiColor.getValue());
   }

   @Override
   public void onEnable() {
      if (this.style.is("Opai")) {
         if (this.opaiScreen == null) this.opaiScreen = new OpaiClickGuiScreen(this.opaiColor);
         this.activeScreen = this.opaiScreen;
      } else if (this.style.is("Neverlose")) {
         if (this.neverloseScreen == null) this.neverloseScreen = new NeverloseClickGuiScreen();
         this.activeScreen = this.neverloseScreen;
      } else {
         ClickGuiScreen modern = new ClickGuiScreen(RockstarClickGuiScreen.Style.MODERN);
         modern.setRenderMode(this.renderMode);
         this.activeScreen = modern;
      }

      mc.gui.setScreen(this.activeScreen);
      this.setEnabled(false);
   }

   public ClickGui() {
      super(CLICK_GUI_LABEL, GLFW.GLFW_KEY_RIGHT_SHIFT, Category.VISUAL);
      this.renderMode.setVisible(() -> this.style.is("Modern"));
      this.opaiColor.setVisible(() -> this.style.is("Opai"));
   }
}
