package com.samsara.module.visual;

import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.module.FeatureManager;
import com.samsara.setting.ModeSetting;
import com.samsara.ui.clickgui.ClickGuiScreen;
import com.samsara.ui.clickgui.opai.OpaiClickGuiScreen;
import com.samsara.ui.clickgui.opai.OpaiStyle;
import com.samsara.ui.clickgui.RockstarClickGuiScreen;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

public class ClickGui extends Feature {
   private static final String f616 = "ClickGUI";
   private Screen f617;
   private OpaiClickGuiScreen opaiScreen;
   public final ModeSetting style = new ModeSetting("Style", this, "Opai", new String[]{"Modern", "Opai"});
   public final ModeSetting renderMode = new ModeSetting("Interface", this, "LiquidGlass", new String[]{"LiquidGlass", "Normal"});
   public final ModeSetting opaiColor = new ModeSetting("Opai Color", this, "Lavender", new String[]{"Lavender", "Light Pink"});

   /** HUDs read the saved ClickGUI choice every frame, even while the GUI is closed. */
   public static OpaiStyle.Palette currentOpaiPalette() {
      ClickGui gui = FeatureManager.clickGui;
      return gui == null ? OpaiStyle.LAVENDER : OpaiStyle.palette(gui.opaiColor.m224());
   }

   @Override
   public void onEnable() {
      if (this.style.m228("Opai")) {
         if (this.opaiScreen == null) this.opaiScreen = new OpaiClickGuiScreen(this.opaiColor);
         this.f617 = this.opaiScreen;
      } else {
         ClickGuiScreen modern = new ClickGuiScreen(RockstarClickGuiScreen.Style.MODERN);
         modern.setRenderMode(this.renderMode);
         this.f617 = modern;
      }

      mc.gui.setScreen(this.f617);
      this.setEnabled(false);
   }

   public ClickGui() {
      super(f616, GLFW.GLFW_KEY_RIGHT_SHIFT, Category.VISUAL);
      this.renderMode.setVisible(() -> !this.style.m228("Opai"));
      this.opaiColor.setVisible(() -> this.style.m228("Opai"));
   }
}
