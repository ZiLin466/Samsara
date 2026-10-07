package com.samsara.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.samsara.module.Feature;
import com.samsara.util.AtomicFiles;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/** Portable presets share the same file and restore rules across commands and every GUI. */
public final class ConfigRepository {
   private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
   private final Supplier<Path> location;
   private final Supplier<List<Feature>> modules;
   private final Runnable afterLoad;

   public ConfigRepository(Supplier<Path> location, Supplier<List<Feature>> modules, Runnable afterLoad) {
      this.location = location;
      this.modules = modules;
      this.afterLoad = afterLoad;
   }

   public static String validName(String raw) {
      String name = raw.strip();
      if (name.toLowerCase(Locale.ROOT).endsWith(".json")) name = name.substring(0, name.length() - 5);
      if (name.isBlank() || name.length() > 64 || name.endsWith(".") || name.endsWith(" ")
         || name.chars().anyMatch(character -> character < 32 || "<>:\"/\\|?*".indexOf(character) >= 0)
         || name.equals(".") || name.equals("..")
         || name.toUpperCase(Locale.ROOT).matches("(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(\\..*)?")) {
         throw new IllegalArgumentException("Enter a valid configuration name");
      }
      return name;
   }

   public Path directory() throws IOException {
      Path directory = this.location.get().toAbsolutePath().normalize();
      Files.createDirectories(directory);
      return directory;
   }

   public Path file(String name) throws IOException {
      Path directory = directory();
      Path file = directory.resolve(validName(name) + ".json").normalize();
      if (!file.getParent().equals(directory)) throw new IOException("Invalid configuration path");
      if (Files.isSymbolicLink(file)) throw new IOException("Linked configuration files are not supported");
      return file;
   }

   public List<String> names() throws IOException {
      try (var files = Files.list(directory())) {
         return files.filter(path -> Files.isRegularFile(path) && !Files.isSymbolicLink(path)
               && path.getFileName().toString().endsWith(".json"))
            .map(path -> path.getFileName().toString())
            .map(name -> name.substring(0, name.length() - 5))
            .sorted(String.CASE_INSENSITIVE_ORDER).toList();
      }
   }

   private JsonObject snapshot(boolean defaults) {
      return ModuleConfigCodec.snapshot(this.modules.get(), ModuleConfigCodec.Scope.GAMEPLAY, defaults);
   }

   public void create(String name, boolean defaults) throws IOException {
      Files.writeString(file(name), JSON.toJson(snapshot(defaults)), StandardCharsets.UTF_8,
         StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
   }

   public void write(String name, JsonObject config) throws IOException {
      AtomicFiles.writeUtf8(file(name), JSON.toJson(config));
   }

   public void save(String name, boolean defaults) throws IOException {
      write(name, snapshot(defaults));
   }

   public void update(String name) throws IOException {
      if (!Files.isRegularFile(file(name))) throw new IOException("Configuration no longer exists");
      save(name, false);
   }

   public JsonObject read(String name) throws IOException {
      try {
         return JsonParser.parseString(Files.readString(file(name), StandardCharsets.UTF_8)).getAsJsonObject();
      } catch (RuntimeException error) {
         throw new IOException("Invalid configuration JSON", error);
      }
   }

   public void load(String name) throws IOException {
      JsonObject config = read(name);
      try {
         ModuleConfigCodec.prepare(this.modules.get(), config, ModuleConfigCodec.Scope.GAMEPLAY).run();
      } catch (RuntimeException error) {
         throw new IOException("Invalid configuration values", error);
      }
      this.afterLoad.run();
   }

   public boolean delete(String name) throws IOException {
      return Files.deleteIfExists(file(name));
   }
}
