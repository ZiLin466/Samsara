package com.samsara.config;

import com.google.gson.*;
import com.samsara.module.FeatureManager;
import com.samsara.ui.hud.HudLayouts;
import com.samsara.event.Events;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Locale;
import net.minecraft.client.Minecraft;

/** Complete automatic state is separate from portable gameplay configurations. */
public final class ConfigManager {
   private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
   private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("samsara-config");
   private static ClientStateStore stateStore;
   private static Path root() { return Minecraft.getInstance().gameDirectory.toPath().resolve("samsara"); }
   public static void init() {
      try { ClientDataPaths.configurations(Minecraft.getInstance().gameDirectory.toPath()); }
      catch (IOException error) { LOG.warn("Unable to import legacy configurations; originals are preserved", error); }
   }
   public static String validName(String raw) {
      String name = raw.strip();
      if (name.toLowerCase(Locale.ROOT).endsWith(".json")) name = name.substring(0, name.length() - 5);
      if (name.isBlank() || name.length() > 64 || name.endsWith(".") || name.endsWith(" ")
         || name.chars().anyMatch(c -> c < 32 || "<>:\"/\\|?*".indexOf(c) >= 0)
         || name.equals(".") || name.equals("..")
         || name.toUpperCase(Locale.ROOT).matches("(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(\\..*)?"))
         throw new IllegalArgumentException("Enter a valid configuration name");
      return name;
   }
   public static File getConfigDir() { return root().resolve("configs").toFile(); }
   public static void openConfigDirectory(Path directory) throws IOException {
      String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
      String opener = os.contains("win") ? "explorer.exe" : os.contains("mac") ? "open" : "xdg-open";
      new ProcessBuilder(opener, directory.toAbsolutePath().toString()).start();
   }
   public static File getConfigFile(String name) { return getConfigDir().toPath().resolve(validName(name) + ".json").toFile(); }
   public static boolean configExists(String name) { return getConfigFile(name).isFile(); }
   public static void atomicWrite(Path target, String content) throws IOException {
      Files.createDirectories(target.getParent());
      if (Files.isSymbolicLink(target)) throw new IOException("Linked configuration files are not supported");
      Path temporaryFile = Files.createTempFile(target.getParent(), ".samsara-", ".tmp");
      try {
         Files.writeString(temporaryFile, content, StandardCharsets.UTF_8);
         try { Files.move(temporaryFile, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
         catch (AtomicMoveNotSupportedException ignored) { Files.move(temporaryFile, target, StandardCopyOption.REPLACE_EXISTING); }
      } finally { Files.deleteIfExists(temporaryFile); }
   }
   public static void writeConfig(String name, JsonObject state) {
      try { atomicWrite(getConfigFile(name).toPath(), JSON.toJson(state)); }
      catch (IOException error) { throw new IllegalStateException("Could not save configuration", error); }
   }
   public static JsonObject readConfig(String name) {
      try { return JsonParser.parseString(Files.readString(getConfigFile(name).toPath(), StandardCharsets.UTF_8)).getAsJsonObject(); }
      catch (IOException | RuntimeException error) { throw new IllegalArgumentException("Could not read configuration", error); }
   }
   public static void loadConfig(String name) {
      ModuleConfigCodec.prepare(FeatureManager.getModules(), readConfig(name), ModuleConfigCodec.Scope.GAMEPLAY).run();
      Events.refreshListeners(); saveState();
   }
   public static void saveConfig(String name) { writeConfig(name, ModuleConfigCodec.snapshot(FeatureManager.getModules(), ModuleConfigCodec.Scope.GAMEPLAY, false)); }
   public static void createDefaultConfig(String name) { writeConfig(name, ModuleConfigCodec.snapshot(FeatureManager.getModules(), ModuleConfigCodec.Scope.GAMEPLAY, true)); }
   public static boolean deleteConfig(String name) { return getConfigFile(name).delete(); }
   public static String[] getConfigNames() {
      File[] files = getConfigDir().listFiles((dir, name) -> name.endsWith(".json"));
      return files == null ? new String[0] : java.util.Arrays.stream(files).map(file -> file.getName().substring(0, file.getName().length()-5)).sorted(String.CASE_INSENSITIVE_ORDER).toArray(String[]::new);
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
      Events.ALL_EVENTS.forEach(com.samsara.event.Event::sortModules);
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
