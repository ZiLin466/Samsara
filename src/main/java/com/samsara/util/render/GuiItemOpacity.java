package com.samsara.util.render;

/** Carries extraction opacity to the final item texture, including oversized models. */
public final class GuiItemOpacity implements AutoCloseable {
   private static final ThreadLocal<Float> CURRENT = ThreadLocal.withInitial(() -> 1f);
   private final float previous;

   public interface State { float samsara$itemOpacity(); }

   private GuiItemOpacity(float opacity) {
      previous = CURRENT.get();
      CURRENT.set(Math.clamp(opacity, 0, 1));
   }
   public static GuiItemOpacity extract(float opacity) { return new GuiItemOpacity(opacity); }
   public static float current() { return CURRENT.get(); }
   public static int premultipliedTint(float opacity) {
      int alpha = Math.round(Math.clamp(opacity, 0, 1) * 255);
      return alpha * 0x01010101;
   }
   @Override public void close() { CURRENT.set(previous); }
}
