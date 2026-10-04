package com.samsara.util.animation;

/** Critically damped, reversible lift. Uses elapsed time, never render-frame counts. */
public final class HoverMotion {
   private double position, velocity;
   private long previous;

   public float update(boolean raised, long now, boolean reduced) {
      double target = raised ? 1 : 0;
      if (reduced) { position = target; velocity = 0; previous = now; return (float)position; }
      double dt = previous == 0 ? 0 : Math.clamp((now - previous) / 1e9, 0, .1);
      previous = now;
      double offset = position - target, decay = Math.exp(-32 * dt);
      double impulse = velocity + 32 * offset;
      position = target + (offset + impulse * dt) * decay;
      velocity = (velocity - 32 * impulse * dt) * decay;
      return (float)Math.clamp(position, 0, 1);
   }
}
