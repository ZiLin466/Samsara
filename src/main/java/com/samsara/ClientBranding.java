package com.samsara;

import java.io.IOException;
import java.util.Locale;
import java.util.Properties;

/** The displayed version and Fabric metadata come from the same Gradle version. */
public final class ClientBranding {
   public static final String MOD_ID = "samsara";
   public static final String NAME = "Samsara";
   public static final String VERSION = readVersion();
   public static final String DISPLAY_VERSION = VERSION.toUpperCase(Locale.ROOT);
   public static final String WINDOW_TITLE = NAME + " Client " + DISPLAY_VERSION;
   public static final String TERMINAL_LABEL = "SAMSARA / CLIENT " + DISPLAY_VERSION;

   private ClientBranding() { }

   private static String readVersion() {
      try (var input = ClientBranding.class.getResourceAsStream("/samsara-version.properties")) {
         if (input == null) throw new IOException("Missing Samsara build version");
         var properties = new Properties();
         properties.load(input);
         var version = properties.getProperty("version");
         if (version == null || version.isBlank() || version.contains("${"))
            throw new IOException("Invalid Samsara build version");
         return version.strip();
      } catch (IOException error) {
         throw new ExceptionInInitializerError(error);
      }
   }
}
