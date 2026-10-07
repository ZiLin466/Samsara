package com.samsara.config;

import com.samsara.module.FeatureManager;
import com.samsara.ui.hud.HudLayouts;
import com.samsara.event.Events;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import net.minecraft.client.Minecraft;

/** Complete automatic state is separate from portable gameplay configurations. */
public final class ConfigManager {
   private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("samsara-config");
   private static final ConfigRepository PRESETS = new ConfigRepository(
      () -> getConfigDir().toPath(), FeatureManager::getModules, () -> { Events.refreshListeners(); saveState(); });
   private static ClientStateStore stateStore;
   private static Path root() { return Minecraft.getInstance().gameDirectory.toPath().resolve("samsara"); }
   public static void init() {
      try { ClientDataPaths.configurations(Minecraft.getInstance().gameDirectory.toPath()); }
      catch (IOException error) { LOG.warn("Unable to import legacy configurations; originals are preserved", error); }
   }
   public static String validName(String raw) {
      return ConfigRepository.validName(raw);
   }
   public static ConfigRepository presets() { return PRESETS; }
   public static File getConfigDir() { return root().resolve("configs").toFile(); }
   public static void openConfigDirectory(Path directory) throws IOException {
      String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
      String opener = os.contains("win") ? "explorer.exe" : os.contains("mac") ? "open" : "xdg-open";
      new ProcessBuilder(opener, directory.toAbsolutePath().toString()).start();
   }
   public static boolean configExists(String name) {
      try { return Files.isRegularFile(PRESETS.file(name)); }
      catch (IOException error) { return false; }
   }
   public static void loadConfig(String name) {
      try { PRESETS.load(name); }
      catch (IOException error) { throw new IllegalArgumentException(error.getMessage(), error); }
   }
   public static void saveConfig(String name) { saveConfig(name, false); }
   public static void createDefaultConfig(String name) { saveConfig(name, true); }
   private static void saveConfig(String name, boolean defaults) {
      try { PRESETS.save(name, defaults); }
      catch (IOException error) { throw new IllegalStateException("Could not save configuration", error); }
   }
   public static boolean deleteConfig(String name) {
      try { return PRESETS.delete(name); }
      catch (IOException error) { return false; }
   }
   public static String[] getConfigNames() {
      try { return PRESETS.names().toArray(String[]::new); }
      catch (IOException error) { return new String[0]; }
   }
   public static void loadState() {
      if (stateStore==null) {
         stateStore=new ClientStateStore(root().resolve("state.json"),
            ()->ClientStateCodec.snapshot(FeatureManager.getModules(),HudLayouts.INSTANCE),
            state->ClientStateCodec.restore(FeatureManager.getModules(),HudLayouts.INSTANCE,state,
               message->LOG.warn("Skipping outdated automatic state value: {}",message)),
            (message,error)->LOG.warn(message,error));
      }
      stateStore.load();
      // Startup happens before a player exists; the regular toggle sorter is world-gated.
      Events.initializeListeners();
   }
   public static void tick() {
      if (stateStore!=null) stateStore.tick(System.nanoTime()/1_000_000);
   }
   public static void saveState() {
      if (stateStore!=null) stateStore.save();
   }
   public static void flush() {
      if (stateStore!=null) stateStore.flush();
   }
}
