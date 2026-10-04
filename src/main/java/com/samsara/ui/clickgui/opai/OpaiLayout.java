package com.samsara.ui.clickgui.opai;

/** Geometry shared by the NanoVG renderer, fallback renderer and hit testing. */
final class OpaiLayout {
   static final float COL_W = 152.0f;
   static final float COL_GAP = 8.0f;
   static final float HEADER_H = 21.0f;
   static final float ROW_H = 19.6f;
   static final float FOOTER_H = 7.0f;
   static final float RADIUS = 7.0f;
   static final float TEXT_PAD = 10.0f;

   private OpaiLayout() {
   }

   static Viewport viewport(int width, int height, int columns) {
      float margin = Math.min(30.0f, width * 0.0382f);
      float colW = Math.min(COL_W, width - margin * 2);
      float top = Math.max(8.0f, height * 0.112f);
      float maxBody = Math.max(ROW_H, height - top - HEADER_H - Math.max(8.0f, height * 0.10f));
      return new Viewport(colW, margin, top, Math.min(16.0f * ROW_H + FOOTER_H, maxBody));
   }

   static float horizontalOverflow(int width, int columns, Viewport layout) {
      return Math.max(0, layout.left() * 2 + columns * layout.columnWidth() + (columns - 1) * COL_GAP - width);
   }

   static float bodyHeight(int rows, float maxHeight) {
      return Math.min(maxHeight, rows * ROW_H + FOOTER_H);
   }

   static float maxScroll(int rows, float bodyHeight) {
      return Math.max(0.0f, rows * ROW_H - (bodyHeight - FOOTER_H));
   }

   static int rowIndex(double localY, float scroll, float bodyHeight, int rows) {
      if (localY < 0.0 || localY >= bodyHeight - FOOTER_H) {
         return -1;
      }
      int index = (int)Math.floor((localY + scroll) / ROW_H);
      return index >= 0 && index < rows ? index : -1;
   }

   static String displayName(String name) {
      return name.replaceAll("([A-Z]+)([A-Z][a-z])", "$1 $2")
         .replaceAll("([a-z0-9])([A-Z])", "$1 $2");
   }

   record Viewport(float columnWidth, float left, float top, float maxBodyHeight) {
   }
}
