package com.samsara.ui.mainmenu.launch;

/** Frame-independent animation sampling; time values are in seconds. */
final class IntroMotion {
   private IntroMotion() { }

   static float mix(float from, float to, float p) { return from + (to - from) * p; }

   static float ramp(double time, double start, double end) {
      return LaunchTimeline.phase(time, start, end - start);
   }

   /** Evaluate the temporal cubic by its X coordinate. */
   static float bezier(float progress, float x1, float y1, float x2, float y2) {
      if (progress <= 0) return 0;
      if (progress >= 1) return 1;
      float lo = 0, hi = 1;
      for (int i = 0; i < 18; i++) {
         float u = (lo + hi) / 2;
         if (cubic(u, x1, x2) < progress) lo = u; else hi = u;
      }
      return cubic((lo + hi) / 2, y1, y2);
   }

   static float out(double time, double start, double end) {
      return bezier(ramp(time, start, end), .23f, 1, .32f, 1);
   }

   static float card(double time, double start, double end) {
      return bezier(ramp(time, start, end), .11f, .89f, .02f, 1);
   }

   static float sample(double time, double... pairs) {
      if (time <= pairs[0]) return (float)pairs[1];
      for (int i = 2; i < pairs.length; i += 2) {
         if (time <= pairs[i]) {
            float p = (float)((time - pairs[i - 2]) / (pairs[i] - pairs[i - 2]));
            return mix((float)pairs[i - 1], (float)pairs[i + 1], p);
         }
      }
      return (float)pairs[pairs.length - 1];
   }

   private static float cubic(float u, float firstControlPoint, float secondControlPoint) {
      float v = 1 - u;
      return 3 * v * v * u * firstControlPoint + 3 * v * u * u * secondControlPoint + u * u * u;
   }
}
