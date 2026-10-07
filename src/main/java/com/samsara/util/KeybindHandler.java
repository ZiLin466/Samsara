package com.samsara.util;

import com.samsara.module.Feature;
import com.samsara.module.FeatureManager;
import org.lwjgl.sdl.SDLKeyboard;
import org.lwjgl.sdl.SDLScancode;
import org.lwjgl.glfw.GLFW;

public class KeybindHandler implements Wrapper {
   private static final boolean[] pressedKeys = new boolean[GLFW.GLFW_KEY_LAST + 1];

   public static void updateKeybinds() {
      if (Wrapper.mc.gui.screen() == null) {
         java.nio.ByteBuffer state = SDLKeyboard.SDL_GetKeyboardState();

         for (Feature feature : FeatureManager.getModules()) {
            int keyCode = feature.getKey();
            if (keyCode != -1 && keyCode != 0) {
               int scancode = keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT ? SDLScancode.SDL_SCANCODE_RSHIFT : keyCode;
               boolean pressed = scancode >= 0 && scancode < state.limit() && state.get(scancode) != 0;
               if (!pressed) {
                  pressedKeys[keyCode] = false;
               } else if (!pressedKeys[keyCode]) {
                  for (Feature boundFeature : FeatureManager.getModules()) {
                     if (boundFeature.getKey() == keyCode) {
                        boundFeature.toggle();
                     }
                  }

                  pressedKeys[keyCode] = true;
               }
            }
         }
      }
   }
}
