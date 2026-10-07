package com.samsara.config;
import com.samsara.util.AtomicFiles;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.ui.hud.ClickGuiLayouts;
import com.samsara.ui.hud.HudLayouts;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

final class ClientStateStoreTest {
   @TempDir Path directory;
   @AfterEach void clearGui() { ClickGuiLayouts.load(new JsonObject()); }
   private static final class Session {
      final ModuleConfigCodecTest.Fixture gameplay=new ModuleConfigCodecTest.Fixture("Combat",Category.COMBAT);
      final ModuleConfigCodecTest.Fixture visual=new ModuleConfigCodecTest.Fixture("Visual",Category.VISUAL);
      final List<Feature> modules=List.of(gameplay,visual);
      final HudLayouts hud=new HudLayouts();
      final List<String> warnings=new ArrayList<>();
      JsonObject snapshot() { return ClientStateCodec.snapshot(modules,hud); }
      void restore(JsonObject data) { ClientStateCodec.restore(modules,hud,data,warnings::add); }
      ClientStateStore store(Path file) {
         return new ClientStateStore(file,this::snapshot,this::restore,(message,error)->fail(message,error));
      }
   }

   @Test void automaticSaveAndNextStartupRestoreSettingsBindingsAndEveryVisualPlacement() throws Exception {
      Path file=directory.resolve("samsara/state.json");var first=new Session();
      first.gameplay.flag.setValue(true);first.gameplay.mode.setValue("B");first.gameplay.restoreEnabled(true);
      first.gameplay.setHidden(true);first.gameplay.setKey(45);
      first.visual.flag.setValue(true);first.visual.number.setValue(7.5);first.visual.mode.setValue("B");first.visual.setKey(32);
      for (var element:HudLayouts.Element.values()) {
         first.hud.get(element).move(41+element.ordinal(),-25+element.ordinal());
         first.hud.get(element).scale(1.37);
      }
      ClickGuiLayouts.put("opai:Combat",71,81);ClickGuiLayouts.put("opai:configs",310,92);
      var expected=first.snapshot();
      try(var store=first.store(file)) {
         store.save();assertFalse(Files.exists(file));
         store.load();store.tick(1000);store.flush();
         assertEquals(expected,JsonParser.parseString(Files.readString(file)));
      }
      ClickGuiLayouts.load(new JsonObject());var restarted=new Session();
      try(var store=restarted.store(file)) {
         store.load();assertEquals(expected,restarted.snapshot());assertTrue(restarted.warnings.isEmpty());
         assertTrue(restarted.gameplay.isEnabled());assertEquals(0,restarted.gameplay.enabledCalls);
      }
   }

   @Test void portablePresetIncludesOnlyVisualBindingsAndPreservesVisualParametersAndGeometry() throws Exception {
      var session=new Session();session.gameplay.flag.setValue(true);session.visual.flag.setValue(true);
      var preset=ModuleConfigCodec.snapshot(session.modules,ModuleConfigCodec.Scope.GAMEPLAY,false);
      Path file=directory.resolve("pvp.json");AtomicFiles.writeUtf8(file,preset.toString());
      assertEquals(java.util.Set.of("key"),preset.getAsJsonObject("Visual").keySet());
      assertFalse(preset.has("hud"));assertFalse(preset.has("clickGui"));
      session.gameplay.flag.setValue(false);session.visual.flag.setValue(false);
      session.hud.get(HudLayouts.Element.POTION).move(123,56);session.hud.get(HudLayouts.Element.ISLAND).scale(1.8);
      ClickGuiLayouts.put("opai:Visual",223,73);
      var before=session.snapshot();
      ModuleConfigCodec.prepare(session.modules,JsonParser.parseString(Files.readString(file)).getAsJsonObject(),ModuleConfigCodec.Scope.GAMEPLAY).run();
      var after=session.snapshot();assertTrue(session.gameplay.flag.getValue());
      assertEquals(before.getAsJsonObject("modules").get("Visual"),after.getAsJsonObject("modules").get("Visual"));
      assertEquals(before.get("hud"),after.get("hud"));assertEquals(before.get("clickGui"),after.get("clickGui"));
   }

   @Test void obsoleteSettingAndOneBadLayoutDoNotDiscardIndependentSettingsOrOtherHudRows() throws Exception {
      Path file=directory.resolve("state.json");var original=new Session();
      original.gameplay.flag.setValue(true);original.visual.mode.setValue("B");
      original.hud.get(HudLayouts.Element.TARGET).move(64,-12);
      ClickGuiLayouts.put("opai:Combat",101,92);
      var saved=original.snapshot();
      saved.getAsJsonObject("modules").getAsJsonObject("Combat").getAsJsonObject("settings").addProperty("Mode","retired");
      saved.getAsJsonObject("hud").getAsJsonObject("INVENTORY").remove("scale");
      saved.getAsJsonObject("clickGui").addProperty("opai:Visual","invalid");
      AtomicFiles.writeUtf8(file,saved.toString());ClickGuiLayouts.load(new JsonObject());
      var restarted=new Session();
      try(var store=restarted.store(file)) {
         store.load();assertTrue(restarted.gameplay.flag.getValue());assertEquals("A",restarted.gameplay.mode.getValue());
         assertEquals("B",restarted.visual.mode.getValue());assertEquals(64,restarted.hud.get(HudLayouts.Element.TARGET).x());
         assertEquals(101,ClickGuiLayouts.x("opai:Combat",0));assertEquals(3,restarted.warnings.size());
         assertEquals(new HudLayouts().get(HudLayouts.Element.INVENTORY).x(),restarted.hud.get(HudLayouts.Element.INVENTORY).x());
      }
   }

   @Test void latestSnapshotIsNotDroppedWhileAnEarlierWriteIsBusy() throws Exception {
      Path file=directory.resolve("state.json");var session=new Session();
      var writer=Executors.newSingleThreadExecutor();var release=new CountDownLatch(1);var waiting=new CountDownLatch(1);
      writer.submit(()->{ waiting.countDown();try { release.await(); } catch (InterruptedException error) { Thread.currentThread().interrupt(); } });
      assertTrue(waiting.await(2,TimeUnit.SECONDS));
      try(var store=new ClientStateStore(file,session::snapshot,session::restore,(message,error)->fail(message,error),writer)) {
         store.load();store.save();session.gameplay.flag.setValue(true);session.hud.get(HudLayouts.Element.SESSION).move(99,88);
         store.save();var expected=session.snapshot();release.countDown();store.flush();
         assertEquals(expected,JsonParser.parseString(Files.readString(file)));
      } finally { release.countDown();writer.shutdownNow(); }
   }

   @Test void unreadableStateCannotBeOverwrittenByPeriodicOrShutdownSaving() throws Exception {
      Path file=directory.resolve("state.json");Files.createDirectory(file);Files.writeString(file.resolve("original"),"preserve");
      var session=new Session();var errors=new ArrayList<String>();
      try(var store=new ClientStateStore(file,session::snapshot,session::restore,(message,error)->errors.add(message))) {
         store.load();store.tick(1000);store.flush();
         assertEquals("preserve",Files.readString(file.resolve("original")));assertFalse(errors.isEmpty());
      }
   }
   @Test void malformedJsonIsPreservedBeforeDefaultsCanBeSaved() throws Exception {
      Path file=directory.resolve("state.json");String broken="{\"modules\": broken";Files.writeString(file,broken);
      var session=new Session();var errors=new ArrayList<String>();
      try(var store=new ClientStateStore(file,session::snapshot,session::restore,(message,error)->errors.add(message))) {
         store.load();assertEquals(1,errors.size());
         try(var files=Files.list(directory)) {
            var backups=files.filter(p->p.getFileName().toString().startsWith("state-invalid-")).toList();
            assertEquals(1,backups.size());assertEquals(broken,Files.readString(backups.getFirst()));
         }
         store.flush();assertEquals(session.snapshot(),JsonParser.parseString(Files.readString(file)));
      }
   }
}
