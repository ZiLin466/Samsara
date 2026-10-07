package com.samsara.config;

import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.BooleanSetting;
import com.samsara.setting.ModeSetting;
import com.samsara.util.AtomicFiles;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

final class ConfigRepositoryTest {
   @TempDir Path directory;
   private static final class Fixture extends Feature {
      final BooleanSetting flag = new BooleanSetting("Flag", this, false);
      final ModeSetting mode = new ModeSetting("Mode", this, "A", new String[]{"A", "B"});
      Fixture(String name, Category category) { super(name, category); }
   }

   @Test void createNeverOverwritesButSaveAndUpdateUseTheSamePortableSnapshot() throws Exception {
      var gameplay = new Fixture("Gameplay", Category.COMBAT);
      var visual = new Fixture("Visual", Category.VISUAL);
      var repository = new ConfigRepository(() -> directory, () -> List.of(gameplay, visual), () -> { });
      gameplay.flag.setValue(true); visual.flag.setValue(true); visual.setKey(31);
      repository.create(" PVP.json ", false);
      var saved = repository.read("PVP");
      assertEquals(java.util.Set.of("key"), saved.getAsJsonObject("Visual").keySet());
      assertThrows(FileAlreadyExistsException.class, () -> repository.create("PVP", true));
      assertEquals(saved, repository.read("PVP"));
      gameplay.mode.setValue("B");
      repository.update("PVP");
      assertEquals(ModuleConfigCodec.snapshot(List.of(gameplay, visual), ModuleConfigCodec.Scope.GAMEPLAY, false), repository.read("PVP"));
      repository.save("PVP", true);
      assertEquals(ModuleConfigCodec.snapshot(List.of(gameplay, visual), ModuleConfigCodec.Scope.GAMEPLAY, true), repository.read("PVP"));
      assertThrows(IOException.class, () -> repository.update("missing"));
      assertFalse(Files.exists(directory.resolve("missing.json")));
   }

   @Test void invalidLoadCannotApplyEarlierValuesOrRunTheSuccessCallback() throws Exception {
      var feature = new Fixture("Gameplay", Category.COMBAT);
      var completed = new AtomicInteger();
      var repository = new ConfigRepository(() -> directory, () -> List.of(feature), completed::incrementAndGet);
      repository.create("PVP", false);
      var broken = repository.read("PVP");
      var values = broken.getAsJsonObject("Gameplay").getAsJsonObject("settings");
      values.addProperty("Flag", true); values.addProperty("Mode", "unknown");
      repository.write("PVP", broken);
      assertThrows(IOException.class, () -> repository.load("PVP"));
      assertFalse(feature.flag.getValue()); assertEquals("A", feature.mode.getValue());
      assertEquals(0, completed.get());
      values.addProperty("Mode", "B"); repository.write("PVP", broken); repository.load("PVP");
      assertTrue(feature.flag.getValue()); assertEquals("B", feature.mode.getValue());
      assertEquals(1, completed.get());
   }

   @Test void listingDeletionAndMalformedJsonHaveOneFileContract() throws Exception {
      var repository = new ConfigRepository(() -> directory, List::of, () -> { });
      repository.create("z", false); repository.create("A", true);
      Files.createDirectories(directory.resolve("folder.json"));
      Files.writeString(directory.resolve("ignore.tmp"), "temporary");
      assertEquals(List.of("A", "z"), repository.names());
      assertTrue(repository.delete("A")); assertFalse(repository.delete("A"));
      Files.writeString(directory.resolve("z.json"), "{broken");
      assertThrows(IOException.class, () -> repository.load("z"));
      assertEquals("{broken", Files.readString(directory.resolve("z.json")));
      assertThrows(IllegalArgumentException.class, () -> repository.file("../state"));
   }

   @Test void failedAtomicReplacementPreservesTheTargetAndRemovesItsTemporaryFile() throws Exception {
      Path target = directory.resolve("occupied.json");
      Files.createDirectories(target); Files.writeString(target.resolve("keep"), "original");
      assertThrows(IOException.class, () -> AtomicFiles.writeUtf8(target, "replacement"));
      assertEquals("original", Files.readString(target.resolve("keep")));
      try (var files = Files.list(directory)) { assertEquals(List.of(target), files.toList()); }
   }
}
