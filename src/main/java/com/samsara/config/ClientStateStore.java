package com.samsara.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Main-thread snapshots, ordered atomic writes, and an explicit startup/shutdown boundary. */
public final class ClientStateStore implements AutoCloseable {
   private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
   private final Path file;
   private final Supplier<JsonObject> snapshot;
   private final Consumer<JsonObject> restore;
   private final BiConsumer<String,Exception> failure;
   private final ExecutorService writer;
   private boolean loaded;
   private long lastCheck;
   private volatile String saved;
   private String requested;
   private Future<?> pending;

   public ClientStateStore(Path file,Supplier<JsonObject> snapshot,Consumer<JsonObject> restore,BiConsumer<String,Exception> failure) {
      this(file,snapshot,restore,failure,Executors.newSingleThreadExecutor(r->{
         Thread thread=new Thread(r,"Samsara state writer");thread.setDaemon(true);return thread;
      }));
   }

   ClientStateStore(Path file,Supplier<JsonObject> snapshot,Consumer<JsonObject> restore,BiConsumer<String,Exception> failure,ExecutorService writer) {
      this.file=file;this.snapshot=snapshot;this.restore=restore;this.failure=failure;
      this.writer=writer;
   }

   public void load() {
      if (loaded) return;
      try {
         if (Files.exists(file)) {
            String content=Files.readString(file,StandardCharsets.UTF_8);
            try { restore.accept(JsonParser.parseString(content).getAsJsonObject()); }
            catch (RuntimeException error) {
               // Preserve damaged data, and never overwrite it if preservation fails.
               Files.copy(file,file.resolveSibling("state-invalid-"+System.currentTimeMillis()+".json"));
               failure.accept("Unable to restore automatic state; original preserved",error);
            }
         }
         loaded=true;
      } catch (IOException error) { failure.accept("Unable to read/preserve automatic state; saving disabled",error); }
   }

   public void tick(long now) {
      if (!loaded) return;
      if (now-lastCheck>=1000) { lastCheck=now;save(); }
   }

   public synchronized void save() {
      if (!loaded) return;
      String next=JSON.toJson(snapshot.get());
      boolean writing=pending!=null && !pending.isDone();
      if (writing?next.equals(requested):next.equals(saved)) return;
      // Do not drop a newer snapshot just because the previous write is still running.
      requested=next;
      pending=writer.submit(()->{
         try { ConfigManager.atomicWrite(file,next);saved=next; }
         catch (IOException error) { failure.accept("Unable to save automatic state",error); }
      });
   }

   public void flush() {
      if (!loaded) return;
      save();
      try { if (pending!=null) pending.get(5,TimeUnit.SECONDS); }
      catch (InterruptedException error) { Thread.currentThread().interrupt();failure.accept("Interrupted while saving automatic state",error); }
      catch (ExecutionException | TimeoutException error) { failure.accept("Unable to flush automatic state",error); }
   }

   @Override public void close() { flush();writer.shutdown(); }
}
