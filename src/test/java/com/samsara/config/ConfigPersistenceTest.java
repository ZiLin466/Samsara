package com.samsara.config;
import com.samsara.util.AtomicFiles;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

final class ConfigPersistenceTest {
   @TempDir Path directory;
   @Test void atomicReplacementIsUtf8AndLeavesOnlyTheCompleteTarget() throws Exception {
      Path state=directory.resolve("samsara/state.json");
      AtomicFiles.writeUtf8(state,"{\"name\":\"主题\"}");
      AtomicFiles.writeUtf8(state,"{\"name\":\"位置\",\"scale\":1.5}");
      assertEquals("{\"name\":\"位置\",\"scale\":1.5}",Files.readString(state,StandardCharsets.UTF_8));
      try(var files=Files.list(state.getParent())){assertEquals(1,files.count());}
   }
   @Test void configurationNamesCannotEscapeThePresetFolder() {
      for(String name:new String[]{"../state","..","CON","a/b","a\\b","x:"," ","a."})
         assertThrows(IllegalArgumentException.class,()->ConfigManager.validName(name));
      assertEquals("PVP",ConfigManager.validName(" PVP.json "));
   }
}
