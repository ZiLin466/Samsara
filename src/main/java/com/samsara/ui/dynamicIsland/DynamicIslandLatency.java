package com.samsara.ui.dynamicIsland;

import java.util.HashMap;
import java.util.Map;

/** Connection-local RTT samples, using Minecraft's gameplay ping/pong packets. */
public final class DynamicIslandLatency {
   public static final long INTERVAL_MS = 500;
   public static final long TIMEOUT_MS = 3000;
   private static final long TOKEN_PREFIX = 0xC0DE000000000000L;
   private static final long TOKEN_MASK = 0xFFFF000000000000L;
   private final Map<Long, Long> pending = new HashMap<>();
   private long nextRequest;
   private long sequence;
   private long measuredAt;
   private int latency = -1;

   /** Implemented by the packet listener; each server connection owns its own samples. */
   public interface Source {
      int samsara$getLivePing(long now, int fallback);
   }

   /** Returns zero when no request is due. A monotonic timestamp avoids wall-clock adjustments. */
   public synchronized long request(long now) {
      if (now < this.nextRequest) return 0;
      this.pending.entrySet().removeIf(entry -> now - entry.getValue() >= TIMEOUT_MS);
      long token = TOKEN_PREFIX | (++this.sequence & ~TOKEN_MASK);
      this.pending.put(token, now);
      this.nextRequest = now + INTERVAL_MS;
      return token;
   }

   /** Only our replies are consumed; native debug-chart probes retain their normal handler. */
   public synchronized boolean receive(long token, long now) {
      if ((token & TOKEN_MASK) != TOKEN_PREFIX) return false;
      Long sentAt = this.pending.remove(token);
      if (sentAt != null && now >= sentAt && now - sentAt < TIMEOUT_MS && sentAt >= this.measuredAt) {
         this.latency = (int)(now - sentAt);
         this.measuredAt = sentAt;
      }
      return true;
   }

   public synchronized int latency(long now, int fallback) {
      return this.latency >= 0 && now - this.measuredAt < TIMEOUT_MS ? this.latency : fallback;
   }

   public synchronized void reset() {
      this.pending.clear();
      this.nextRequest = this.measuredAt = 0;
      this.latency = -1;
   }
}
