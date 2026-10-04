package com.samsara.ui.clickgui.opai;

import java.util.ArrayDeque;

/** Click-origin ink and short press feedback, independent of the setting's value transition. */
final class OpaiFeedback {
   private static final double DURATION = 420;
   private final ArrayDeque<Pulse> pulses = new ArrayDeque<>();
   private final OpaiMotion hover = new OpaiMotion(0, 20);
   private double pressedAt = Double.NEGATIVE_INFINITY;

   void click(float localX, float localY, float width, float height, double now) {
      this.pressedAt = now;
      while (this.pulses.size() >= 3) this.pulses.removeFirst();
      this.pulses.addLast(new Pulse(Math.clamp(localX / width, 0, 1), Math.clamp(localY / height, 0, 1), now));
   }

   float hover(boolean hovered, double now) { return this.hover.approach(hovered ? 1 : 0, now); }

   float press(double now) {
      double age = now - this.pressedAt;
      return age < 0 || age >= 220 ? 0 : (float)(Math.sin(Math.PI * age / 220) * Math.exp(-age / 220));
   }

   void paint(OpaiSurface surface, float x, float y, float w, float h, float corner, int color, double now) {
      this.pulses.removeIf(pulse -> now - pulse.started >= DURATION);
      for (Pulse pulse : this.pulses) {
         float t = (float)Math.clamp((now - pulse.started) / DURATION, 0, 1);
         float cx = pulse.x * w, cy = pulse.y * h;
         float maximum = (float)Math.hypot(Math.max(cx, w - cx), Math.max(cy, h - cy));
         float growth = 1 - (float)Math.pow(1 - Math.min(1, t / .75f), 3);
         int alpha = Math.round(((color >>> 24) & 255) * .22f * (1 - t) * (1 - t));
         surface.ripple(x, y, w, h, corner, x + cx, y + cy, Math.max(.5f, maximum * growth),
            color & 0xFFFFFF | alpha << 24);
      }
   }

   private record Pulse(float x, float y, double started) { }
}
