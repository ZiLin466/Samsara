package com.samsara.ui.clickgui.opai;

interface OpaiSurface {
   void rect(float x, float y, float width, float height, int color);
   void rounded(float x, float y, float width, float height, float radius, int color);
   void panel(float x, float y, float width, float height, boolean header, int color);
   void text(String text, float x, float centerY, float size, float maximum, int color);
   default void plainText(String text, float x, float centerY, float size, float maximum, int color) {
      text(text, x, centerY, size, maximum, color);
   }
   float textWidth(String text, float size);
   default float plainTextWidth(String text, float size) { return textWidth(text, size); }
   void clip(float x, float y, float width, float height, Runnable content);
   void opacity(float factor, Runnable content);
   void icon(OpaiIcons.Icon icon, float x, float y, float size, int color);
   void scale(float factor, float centerX, float centerY, Runnable content);
   void ripple(float x, float y, float width, float height, float corner,
               float centerX, float centerY, float radius, int color);

   static int mix(int from, int to, float amount) {
      int result = 0;
      for (int shift = 0; shift <= 24; shift += 8) {
         int a = (from >>> shift) & 255, b = (to >>> shift) & 255;
         result |= Math.round(a + (b - a) * Math.clamp(amount, 0, 1)) << shift;
      }
      return result;
   }
}
