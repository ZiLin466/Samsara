package com.samsara.ui.mainmenu.background;

import com.samsara.ui.mainmenu.launch.LaunchLayout;
import com.samsara.ui.mainmenu.screen.SamsaraTitleScreen;
import com.samsara.util.ModTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.renderer.RenderPipelines;

/** Drawn in Minecraft's background stratum, so all vanilla widgets retain their own rendering/input. */
public final class MenuBackground {
   private static boolean menuFlow;
   private MenuBackground() { }
   public static void transition(Screen next) {
      var mc = Minecraft.getInstance();
      if (next == null || mc.level != null) { menuFlow = false; return; }
      if (next instanceof SamsaraTitleScreen || mc.gui.screen() instanceof SamsaraTitleScreen) menuFlow = true;
   }
   public static boolean applies(Screen screen) {
      return Minecraft.getInstance().level == null && (menuFlow || screen instanceof SelectWorldScreen
         || screen instanceof JoinMultiplayerScreen || screen instanceof OptionsScreen)
         && !(screen instanceof SamsaraTitleScreen) && !(screen instanceof com.samsara.ui.NanoGui)
         && !(screen instanceof com.samsara.ui.terminal.TerminalPage);
   }
   public static void render(GuiGraphicsExtractor graphics, int width, int height) {
      float cover = Math.max(width / 1820f, height / 1024f);
      int w = Math.round(1820 * cover), h = Math.round(1024 * cover);
      graphics.blit(RenderPipelines.GUI_TEXTURED, ModTextures.register("textures/launch/scene.png"), (width-w)/2, (height-h)/2,
         0, 0, w, h, 1820, 1024, 1820, 1024);
      var layout = LaunchLayout.of(width, height);
      int size = Math.round(980*layout.scale());
      graphics.blit(RenderPipelines.GUI_TEXTURED, ModTextures.register("textures/launch/operator.png"),
         Math.round(layout.left()+40*layout.scale()), Math.round(layout.top()+30*layout.scale()), 0, 0, size, size, 1024, 1024, 1024, 1024);
      graphics.fill(0, 0, width, height, 0x700A111A);
   }
}
