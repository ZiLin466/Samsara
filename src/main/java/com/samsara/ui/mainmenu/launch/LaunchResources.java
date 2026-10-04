package com.samsara.ui.mainmenu.launch;

import com.samsara.util.render.NVGRenderer;
import java.io.IOException;
import java.nio.file.Files;
import net.fabricmc.loader.api.FabricLoader;

public final class LaunchResources {
   private static LaunchRenderer renderer;
   private LaunchResources() { }

   public static LaunchRenderer renderer() {
      if (renderer == null) renderer = new LaunchRenderer(NVGRenderer.getContext(), name -> {
         var path = FabricLoader.getInstance().getModContainer("samsara")
            .flatMap(c -> c.findPath("assets/samsara/" + name))
            .orElseThrow(() -> new IllegalStateException("Missing launch resource: " + name));
         try { return Files.readAllBytes(path); }
         catch (IOException e) { throw new IllegalStateException("Cannot read launch resource: " + name, e); }
      });
      return renderer;
   }

   public static void close() {
      if (renderer != null) { renderer.close(); renderer = null; }
   }
}
