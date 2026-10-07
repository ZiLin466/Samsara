package com.samsara.ui.mainmenu.launch;

import static org.lwjgl.nanovg.NanoVG.*;

final class IntroCamera {
   private final float top, bottom, topScale, bottomScale, zoom;

   IntroCamera(double timeSeconds, boolean reduced) {
      top = reduced ? 290 : IntroMotion.sample(timeSeconds, 5.5,290, 6,291, 7,288, 8,280, 9,264, 9.5,262, 10,278, 10.5,277, 10.77,305);
      bottom = reduced ? 620 : IntroMotion.sample(timeSeconds, 5.5,580, 6,590, 7,606, 8,612, 9,606, 9.5,610, 10,655, 10.5,628, 10.77,607);
      float leftTop = IntroMotion.sample(timeSeconds, 5.5,207, 6,209, 7,213, 8,217, 9,220, 9.5,215, 10,226, 10.5,279, 10.77,385);
      float leftBottom = IntroMotion.sample(timeSeconds, 5.5,150, 6,155, 7,166, 8,176, 9,185, 9.5,189, 10,212, 10.5,276, 10.77,383);
      topScale = reduced ? 1 : (800 - leftTop) / 580;
      bottomScale = reduced ? 1 : (800 - leftBottom) / 580;
      zoom = reduced ? 1 : 1 - .48f * IntroMotion.ramp(timeSeconds, 10.73, 10.88);
   }

   void begin(long vg, float x, float y) {
      float p = (y - 290) / 330;
      float sx = IntroMotion.mix(topScale, bottomScale, p);
      float shear = (bottomScale - topScale) / 330 * (x - 800);
      nvgSave(vg);
      nvgTranslate(vg, 800, 450); nvgScale(vg, zoom, zoom); nvgTranslate(vg, -800, -450);
      nvgTransform(vg, sx, 0, shear, (bottom - top) / 330, 800 + (x - 800) * sx, IntroMotion.mix(top, bottom, p));
   }

   static void end(long vg) { nvgRestore(vg); }
}
