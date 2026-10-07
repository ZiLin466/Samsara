package com.samsara.util.render;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;

public final class FontRepository {

   private static final Map<String, NVGTextRenderer> FONTS = new HashMap<>();
   private static final String RESOURCE_PREFIX = "assets/samsara/fonts/";

   private FontRepository() {
   }

   static void clear() { FONTS.clear(); }

   public static NVGTextRenderer getFont(String name) {
      NVGTextRenderer cached = FONTS.get(name);
      if (cached != null) {
         return cached;
      }

      InputStream input = FabricLoader.getInstance().getModContainer("samsara")
         .flatMap(container -> container.findPath(RESOURCE_PREFIX + name + ".ttf"))
         .map(path -> {
            try {
               return java.nio.file.Files.newInputStream(path);
            } catch (java.io.IOException error) {
               throw new IllegalStateException("Unable to open font: " + name, error);
            }
         })
         .orElse(null);
      if (input == null) {
         throw new IllegalStateException("Font not found: " + RESOURCE_PREFIX + name + ".ttf");
      }

      try (InputStream stream = input) {
         NVGTextRenderer renderer = new NVGTextRenderer(name, stream);
         FONTS.put(name, renderer);
         org.slf4j.LoggerFactory.getLogger("samsara-nvg").info(
            "Loaded font '{}' id={} dataSize={}",
            name, renderer.getFontId(), renderer.getDataSize());
         return renderer;
      } catch (Exception error) {
         throw new IllegalStateException("Unable to load font: " + name, error);
      }
   }
}
