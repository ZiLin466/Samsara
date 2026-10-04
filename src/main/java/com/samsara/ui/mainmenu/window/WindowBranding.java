package com.samsara.ui.mainmenu.window;

import java.io.IOException;
import javax.imageio.ImageIO;
import org.lwjgl.sdl.SDL_Surface;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.LoggerFactory;
import static org.lwjgl.sdl.SDLError.SDL_GetError;
import static org.lwjgl.sdl.SDLPixels.SDL_PIXELFORMAT_RGBA32;
import static org.lwjgl.sdl.SDLSurface.*;
import static org.lwjgl.sdl.SDLVideo.SDL_SetWindowIcon;

/** Minecraft 26.3 uses an SDL window; the icon must be uploaded through SDL. */
public final class WindowBranding {
   private WindowBranding() { }

   public static void applyIcon(long window) {
      try (var input = WindowBranding.class.getResourceAsStream("/assets/samsara/icon.png")) {
         if (input == null) throw new IOException("Missing Samsara window icon");
         var image = ImageIO.read(input);
         if (image == null) throw new IOException("Invalid Samsara window icon");
         int width = image.getWidth(), height = image.getHeight();
         var pixels = MemoryUtil.memAlloc(width * height * 4);
         SDL_Surface surface = null;
         try {
            for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
               int argb = image.getRGB(x, y);
               pixels.put((byte)(argb >> 16)).put((byte)(argb >> 8)).put((byte)argb).put((byte)(argb >> 24));
            }
            pixels.flip();
            surface = SDL_CreateSurfaceFrom(width, height, SDL_PIXELFORMAT_RGBA32, pixels, width * 4);
            if (surface == null || !SDL_SetWindowIcon(window, surface)) throw new IOException(SDL_GetError());
         } finally {
            if (surface != null) SDL_DestroySurface(surface);
            MemoryUtil.memFree(pixels);
         }
      } catch (Exception error) {
         LoggerFactory.getLogger("samsara").warn("Unable to apply Samsara window icon", error);
      }
   }
}
