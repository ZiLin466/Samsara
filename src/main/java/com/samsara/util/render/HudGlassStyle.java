package com.samsara.util.render;

/** Shared neutral glass for the island and inventory, over the same blurred world capture. */
public final class HudGlassStyle {
   public static final int BODY = 0x98000000;
   public static final int HEADER = 0x98202020;
   public static final float BLUR_SIGMA = 2;
   public static final float BLUR_PADDING = BLUR_SIGMA * 3 + 1;

   private HudGlassStyle() { }
}
