package com.samsara.ui.mainmenu.launch;

/** Wall-clock choreography: resources finish before the first frame starts. */
public final class LaunchTimeline {
   public static final double LOGIN_START = 4.416666667;
   public static final double CONNECTION_START = 10.766666667;
   public static final double EMBLEM_START = 12.133333333;
   public static final double MENU_START = 12.483333333;
   public static final double DURATION = 13.4;
   private long started = -1;
   private final boolean alreadySeen;

   public LaunchTimeline() { this(false); }
   public LaunchTimeline(boolean alreadySeen) { this.alreadySeen = alreadySeen; }

   public double time(long nowNanos, boolean loading) {
      if (alreadySeen) return DURATION;
      if (loading) { started = -1; return 0; }
      if (started < 0) started = nowNanos;
      return Math.min(DURATION, (nowNanos - started) / 1_000_000_000.0);
   }

   public static boolean interactive(double time) { return time >= DURATION; }

   public static float phase(double seconds, double start, double duration) {
      return (float)Math.clamp((seconds - start) / duration, 0, 1);
   }

   public static float out(double seconds, double start, double duration) {
      float t = phase(seconds, start, duration);
      return 1 - (float)Math.pow(1 - t, 4);
   }
}
