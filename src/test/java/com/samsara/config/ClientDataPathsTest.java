package com.samsara.config;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

final class ClientDataPathsTest {
   @TempDir Path temp;

   @Test void importsPresetsWithoutReplacingExistingFilesOrResurrectingDeletedOnes() throws Exception {
      Path old = temp.resolve("cryptix/configs"), current = temp.resolve("samsara/configs");
      Files.createDirectories(old); Files.createDirectories(current);
      Files.writeString(old.resolve("same.json"), "legacy");
      Files.writeString(current.resolve("same.json"), "samsara");
      Files.writeString(old.resolve("imported.json"), "preserved");
      Files.writeString(old.resolve("unrelated.txt"), "leave alone");
      Files.createDirectory(old.resolve("directory.json"));
      assertEquals(current, ClientDataPaths.configurations(temp));
      assertEquals("samsara", Files.readString(current.resolve("same.json")));
      assertEquals("preserved", Files.readString(current.resolve("imported.json")));
      assertEquals("legacy", Files.readString(old.resolve("same.json")));
      assertFalse(Files.exists(current.resolve("unrelated.txt")));
      assertFalse(Files.exists(current.resolve("directory.json")));
      Files.delete(current.resolve("imported.json"));
      ClientDataPaths.configurations(temp);
      assertFalse(Files.exists(current.resolve("imported.json")));
      assertEquals("preserved", Files.readString(old.resolve("imported.json")));
   }

   @Test void copiesAccountsByteForByteAndAlwaysPrefersSamsara() throws Exception {
      Path old = temp.resolve("cryptix/accounts.json");
      Files.createDirectories(old.getParent());
      byte[] content = "{\"version\":1,\"offlineAccounts\":[\"Dev\"]}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
      Files.write(old, content);
      Path current = ClientDataPaths.accounts(temp);
      assertEquals(temp.resolve("samsara/accounts.json"), current);
      assertArrayEquals(content, Files.readAllBytes(current));
      assertArrayEquals(content, Files.readAllBytes(old));
      Files.writeString(current, "newer data");
      ClientDataPaths.accounts(temp);
      assertEquals("newer data", Files.readString(current));
      assertArrayEquals(content, Files.readAllBytes(old));
   }

   @Test void missingLegacyDataNeedsNoAccountFileAndFailedImportPreservesSource() throws Exception {
      assertFalse(Files.exists(ClientDataPaths.accounts(temp)));
      assertTrue(Files.isDirectory(ClientDataPaths.configurations(temp)));
      Path config = temp.resolve("config"), old = config.resolve("cryptix/accounts.json");
      Files.createDirectories(old.getParent()); Files.writeString(old, "original");
      Files.writeString(config.resolve("samsara"), "blocking file");
      assertThrows(java.io.IOException.class, () -> ClientDataPaths.accounts(config));
      assertEquals("original", Files.readString(old));
      assertEquals("blocking file", Files.readString(config.resolve("samsara")));
   }
}
