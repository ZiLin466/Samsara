package com.samsara.config;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/** Imports the previous brand's data once, retaining originals and newer Samsara files. */
public final class ClientDataPaths {
   private ClientDataPaths() { }

   public static Path configurations(Path gameDirectory) throws IOException {
      Path target = gameDirectory.resolve("samsara/configs");
      Path source = gameDirectory.resolve("cryptix/configs");
      Path marker = target.resolve(".legacy-imported");
      Files.createDirectories(target);
      if (Files.exists(marker, LinkOption.NOFOLLOW_LINKS) || !Files.isDirectory(source, LinkOption.NOFOLLOW_LINKS))
         return target;
      try (var files = Files.list(source)) {
         for (Path file : files.toList()) {
            if (file.getFileName().toString().endsWith(".json") && Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS))
               copyIfMissing(file, target.resolve(file.getFileName()));
         }
      }
      // A deleted imported preset must not reappear at the next client launch.
      try { Files.writeString(marker, "Imported legacy configurations\n", StandardOpenOption.CREATE_NEW); }
      catch (FileAlreadyExistsException ignored) { }
      return target;
   }

   public static Path accounts(Path configDirectory) throws IOException {
      Path target = configDirectory.resolve("samsara/accounts.json");
      copyIfMissing(configDirectory.resolve("cryptix/accounts.json"), target);
      return target;
   }

   private static void copyIfMissing(Path source, Path target) throws IOException {
      if (Files.exists(target, LinkOption.NOFOLLOW_LINKS) || !Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS)) return;
      Files.createDirectories(target.getParent());
      Path temporary = Files.createTempFile(target.getParent(), ".legacy-import-", ".tmp");
      try {
         Files.copy(source, temporary, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
         // Do not use REPLACE_EXISTING or ATOMIC_MOVE: neither may replace a concurrently created destination.
         try { Files.move(temporary, target); }
         catch (FileAlreadyExistsException ignored) { }
      } finally { Files.deleteIfExists(temporary); }
   }
}
