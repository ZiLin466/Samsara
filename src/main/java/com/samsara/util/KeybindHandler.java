package com.samsara.util;

import com.samsara.module.Feature;
import com.samsara.module.FeatureManager;
import org.lwjgl.sdl.SDLKeyboard;
import org.lwjgl.sdl.SDLScancode;
import org.lwjgl.glfw.GLFW;

public class KeybindHandler implements Wrapper {
   private static final boolean[] f48 = new boolean[349];

   public static void m5() {
      if (Wrapper.mc.gui.screen() == null) {
         java.nio.ByteBuffer state = SDLKeyboard.SDL_GetKeyboardState();

         for (Feature var4 : FeatureManager.getModules()) {
            int var5 = var4.getKey();
            if (var5 != -1 && var5 != 0) {
               int scancode = var5 == GLFW.GLFW_KEY_RIGHT_SHIFT ? SDLScancode.SDL_SCANCODE_RSHIFT : var5;
               boolean var6 = scancode >= 0 && scancode < state.limit() && state.get(scancode) != 0;
               if (!var6) {
                  f48[var5] = false;
               } else if (!f48[var5]) {
                  for (Feature var8 : FeatureManager.getModules()) {
                     if (var8.getKey() == var5) {
                        var8.toggle();
                     }
                  }

                  f48[var5] = true;
               }
            }
         }
      }
   }
}
