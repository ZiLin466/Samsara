package com.samsara.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class AtomicFiles {
   private AtomicFiles() { }

   public static void writeUtf8(Path target, String content) throws IOException {
      target = target.toAbsolutePath().normalize();
      Files.createDirectories(target.getParent());
      Path temporary = Files.createTempFile(target.getParent(), ".samsara-", ".tmp");
      try {
         Files.writeString(temporary, content, StandardCharsets.UTF_8);
         try {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
         } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
         }
      } finally {
         Files.deleteIfExists(temporary);
      }
   }
}
