package com.samsara.ui.clickgui.opai;

import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.nanovg.NanoVG.*;

final class OpaiIcons {
   enum Icon { PLUS, GEAR, BACK, REFRESH, SAVE, LOAD, DELETE, FOLDER, UPLOAD, CHECK }

   static void draw(long vg, Icon icon, float x, float y, float size, int color) {
      nvgSave(vg);
      try (MemoryStack stack = MemoryStack.stackPush()) {
         NVGColor ink = NVGColor.malloc(stack).r(((color >>> 16) & 255) / 255f)
            .g(((color >>> 8) & 255) / 255f).b((color & 255) / 255f).a((color >>> 24) / 255f);
         nvgTranslate(vg, x, y);
         nvgScale(vg, size / 24, size / 24);
         nvgFillColor(vg, ink);
         nvgStrokeColor(vg, ink);
         nvgStrokeWidth(vg, 2.6f);
         nvgLineCap(vg, NVG_ROUND);
         nvgLineJoin(vg, NVG_ROUND);
         nvgBeginPath(vg);
         switch (icon) {
            case CHECK -> { nvgMoveTo(vg, 2, 13); nvgLineTo(vg, 9, 21); nvgLineTo(vg, 23, 2); nvgStrokeWidth(vg, 1.3f); nvgStroke(vg); }
            case PLUS -> { line(vg, 12, 2, 12, 22); line(vg, 2, 12, 22, 12); nvgStroke(vg); }
            case BACK -> { nvgMoveTo(vg, 15, 3); nvgLineTo(vg, 6, 12); nvgLineTo(vg, 15, 21); nvgStroke(vg); }
            case GEAR -> {
               for (int i = 0; i < 32; i++) {
                  double angle = i * Math.PI / 16;
                  float radius = (i % 4 == 0 || i % 4 == 3) ? 11 : 8.7f;
                  float px = 12 + (float)Math.cos(angle) * radius, py = 12 + (float)Math.sin(angle) * radius;
                  if (i == 0) nvgMoveTo(vg, px, py); else nvgLineTo(vg, px, py);
               }
               nvgClosePath(vg);
               nvgCircle(vg, 12, 12, 4);
               nvgPathWinding(vg, NVG_HOLE);
               nvgFill(vg);
            }
            case REFRESH -> {
               nvgArc(vg, 12, 12, 8, -2.75f, -.35f, NVG_CW); nvgStroke(vg);
               nvgBeginPath(vg); nvgArc(vg, 12, 12, 8, .39f, 2.79f, NVG_CW); nvgStroke(vg);
               nvgBeginPath(vg); nvgMoveTo(vg, 20, 10); nvgLineTo(vg, 14.5f, 10); nvgLineTo(vg, 20, 4.5f); nvgClosePath(vg);
               nvgMoveTo(vg, 4, 14); nvgLineTo(vg, 9.5f, 14); nvgLineTo(vg, 4, 19.5f); nvgClosePath(vg); nvgFill(vg);
            }
            case SAVE -> {
               nvgMoveTo(vg, 3, 2); nvgLineTo(vg, 18, 2); nvgLineTo(vg, 22, 6);
               nvgLineTo(vg, 22, 22); nvgLineTo(vg, 3, 22); nvgClosePath(vg);
               nvgRect(vg, 7, 3, 9, 6); nvgPathWinding(vg, NVG_HOLE);
               nvgRoundedRect(vg, 7, 13, 11, 7, 1); nvgPathWinding(vg, NVG_HOLE); nvgFill(vg);
            }
            case LOAD -> {
               nvgRoundedRect(vg, 4, 3, 16, 19, 2); nvgFill(vg);
               nvgBeginPath(vg); nvgRoundedRect(vg, 7, 1, 10, 4, 1); nvgFill(vg);
               nvgBeginPath(vg); line(vg, 12, 8, 12, 17);
               nvgMoveTo(vg, 8, 13); nvgLineTo(vg, 12, 17); nvgLineTo(vg, 16, 13);
               nvgStrokeWidth(vg, 1.8f);
               ink.r(.15f).g(.15f).b(.2f); nvgStrokeColor(vg, ink); nvgStroke(vg);
            }
            case DELETE -> {
               nvgMoveTo(vg, 6, 7); nvgLineTo(vg, 18, 7); nvgLineTo(vg, 17, 22);
               nvgLineTo(vg, 7, 22); nvgClosePath(vg); nvgFill(vg);
               nvgBeginPath(vg); line(vg, 4, 4, 20, 4); line(vg, 9, 1, 15, 1); nvgStroke(vg);
            }
            case FOLDER -> {
               nvgMoveTo(vg, 2, 5); nvgLineTo(vg, 10, 5); nvgLineTo(vg, 12, 8);
               nvgLineTo(vg, 22, 8); nvgLineTo(vg, 22, 21); nvgLineTo(vg, 2, 21);
               nvgClosePath(vg); nvgFill(vg);
            }
            case UPLOAD -> {
               line(vg, 6, 12, 18, 5); line(vg, 6, 12, 18, 19); nvgStroke(vg);
               nvgBeginPath(vg); nvgCircle(vg, 5, 12, 3.5f); nvgCircle(vg, 19, 4, 3.5f);
               nvgCircle(vg, 19, 20, 3.5f); nvgFill(vg);
            }
         }
      } finally { nvgRestore(vg); }
   }

   private static void line(long vg, float x1, float y1, float x2, float y2) {
      nvgMoveTo(vg, x1, y1); nvgLineTo(vg, x2, y2);
   }
}
