package com.samsara.ui.clickgui.opai;

import com.samsara.config.ConfigManager;
import com.samsara.module.Feature;
import com.samsara.module.FeatureManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Locale;

/** Same module/setting JSON schema as ConfigManager, with explicit UI errors and safe local names. */
final class OpaiConfigRepository implements OpaiConfigPanel.Backend {
   private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
   private final java.util.function.Supplier<Path> location;
   private final java.util.function.Supplier<List<Feature>> modules;

   OpaiConfigRepository() {
      this(() -> ConfigManager.getConfigDir().toPath(), FeatureManager::getModules);
   }

   OpaiConfigRepository(java.util.function.Supplier<Path> location, java.util.function.Supplier<List<Feature>> modules) {
      this.location = location;
      this.modules = modules;
   }

   static String validName(String raw) {
      return ConfigManager.validName(raw);
   }

   private Path directory() throws IOException {
      Path directory = this.location.get().toAbsolutePath().normalize();
      Files.createDirectories(directory);
      return directory;
   }
   private Path file(String name) throws IOException {
      Path directory = directory(), file = directory.resolve(validName(name) + ".json").normalize();
      if (!file.getParent().equals(directory)) throw new IOException("Invalid configuration path");
      if (Files.isSymbolicLink(file)) throw new IOException("Linked configuration files are not supported");
      return file;
   }
   @Override public List<String> names() throws IOException {
      try (var files = Files.list(directory())) {
         return files.filter(path -> Files.isRegularFile(path) && !Files.isSymbolicLink(path)
                            && path.getFileName().toString().endsWith(".json"))
            .map(path -> path.getFileName().toString())
            .map(name -> name.substring(0, name.length() - 5)).sorted(String.CASE_INSENSITIVE_ORDER).toList();
      }
   }
   private JsonObject snapshot(boolean blank) {
      return com.samsara.config.ModuleConfigCodec.snapshot(this.modules.get(), com.samsara.config.ModuleConfigCodec.Scope.GAMEPLAY, blank);
   }
   @Override public void create(String name, boolean blank) throws IOException {
      Files.writeString(file(name), JSON.toJson(snapshot(blank)), StandardCharsets.UTF_8,
         StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
   }
   @Override public void update(String name) throws IOException {
      Path target = file(name);
      if (!Files.isRegularFile(target)) throw new IOException("Configuration no longer exists");
      ConfigManager.atomicWrite(target, JSON.toJson(snapshot(false)));
   }
   @Override public void load(String name) throws IOException {
      JsonObject config;
      try { config = JsonParser.parseString(Files.readString(file(name), StandardCharsets.UTF_8)).getAsJsonObject(); }
      catch (RuntimeException error) { throw new IOException("Invalid configuration JSON", error); }
      try { com.samsara.config.ModuleConfigCodec.prepare(this.modules.get(), config, com.samsara.config.ModuleConfigCodec.Scope.GAMEPLAY).run(); }
      catch (RuntimeException error) { throw new IOException("Invalid configuration values", error); }
      ConfigManager.saveState();
   }
   @Override public void delete(String name) throws IOException { Files.delete(file(name)); }
   @Override public void openFolder() throws IOException {
      ConfigManager.openConfigDirectory(directory());
   }
}
