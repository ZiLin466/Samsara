package com.samsara.ui.mainmenu.launch;

import com.samsara.ClientBranding;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;
import static org.lwjgl.nanovg.NanoVG.*;
import static com.samsara.ui.mainmenu.launch.IntroMotion.*;

final class LaunchIntro {
   private static final int INK = 0xFF19191B, WHITE = 0xFFF4F4F2, LIME = 0xFFE1EA31;
   private final LaunchRenderer r;
   private final long vg;
   private final LaunchSphere sphere = new LaunchSphere();
   private final NVGColor a = NVGColor.create(), b = NVGColor.create();
   private final NVGPaint paint = NVGPaint.create();

   LaunchIntro(LaunchRenderer renderer, long vg) { r = renderer; this.vg = vg; }

   void prepare() { sphere.load(r.resources()); }

   void draw(float width, float height, double t, boolean reduced) {
      r.rect(0, 0, width, height, 0xFF000000);
      var layout = LaunchLayout.of(width, height);
      nvgSave(vg);
      try {
         nvgTranslate(vg, layout.left(), layout.top());
         nvgScale(vg, layout.scale(), layout.scale());
         float bar = t < LaunchTimeline.EMBLEM_START ? 90 : 45;
         nvgIntersectScissor(vg, 0, bar, 1600, 900 - bar * 2);
         if (t < LaunchTimeline.LOGIN_START) boot(t, reduced);
         else if (t < LaunchTimeline.CONNECTION_START) {
            paleBackground(t);
            login(t, reduced);
         } else if (t < LaunchTimeline.EMBLEM_START) {
            connection(t, reduced);
            if (t < 10.9) {
               nvgGlobalAlpha(vg, 1 - ramp(t, LaunchTimeline.CONNECTION_START, 10.9));
               login(t, reduced);
            }
         } else emblemScene(t, reduced);
      } finally { nvgRestore(vg); }
   }

   void menuMask(float width, float height, double t) {
      float bar = height * .05f * (1 - out(t, 13.1, LaunchTimeline.DURATION));
      if (bar > .01f) {
         r.rect(0, 0, width, bar, 0xFF000000);
         r.rect(0, height - bar, width, bar, 0xFF000000);
      }
   }

   private void boot(double t, boolean reduced) {
      float camera = reduced ? .6f : bezier(ramp(t, 0, 4.6), .61f, 0, .41f, 1);
      nvgSave(vg);
      nvgTranslate(vg, 800, 450);
      nvgRotate(vg, reduced ? -.04f : mix(.018f, -.065f, camera));
      float scale = reduced ? 1.05f : mix(1.26f, 1.12f, camera);
      nvgScale(vg, scale, scale);
      r.image("intro-corridor", -800, -450, 1600, 900, 1);
      nvgRestore(vg);
      r.rect(0, 0, 1600, 900, 0x181D2639);
      glow(800, 425, 700, 0x1034517A);
      grain(.45f);
      float light = sample(t, 0,.015, .5,.07, 1,.23, 1.5,.44, 2,.88, 2.5,1, 4.42,1);
      nvgSave(vg);
      nvgGlobalAlpha(vg, light);
      nvgTranslate(vg, 800, 450);
      nvgRotate(vg, reduced ? -.18f : mix(-.12f, -.19f, camera));
      nvgScale(vg, reduced ? 1 : mix(.78f, 1.08f, camera), .92f);
      float line = out(t, 1.32, 2.05);
      glowLine(-1050 * line, 43, -167, 43, 2.6f, LIME);
      glowLine(167, 43, 1050 * line, 43, 2.6f, LIME);
      float ticks = ramp(t, 1.7, 2.55);
      for (int i = 0; i < 74 * ticks; i++)
         glowLine(-1020 + i * 10, -6, -1008 + i * 10, 28, 2, LIME);
      float dots = ramp(t, 2.08, 3.32);
      for (int i = 0; i < 43 * dots; i++) r.rect(186 + i * 14, 10, 8, 8, LIME);
      for (double start : new double[]{1.62, 2.65, 3.8}) {
         float p = ramp(t, start, start + .6);
         if (p > 0 && p < 1) {
            nvgSave(vg); nvgRotate(vg, .64f);
            float size = reduced ? 225 : mix(170, 415, out(t, start, start + .58));
            float opacity = 1 - (float)Math.pow(p, 5);
            r.outline(-size / 2 + 6, -size / 2 + 18, size, size, 3, alpha(LIME, opacity * .16f));
            r.outline(-size / 2, -size / 2, size, size, 3, alpha(LIME, opacity));
            nvgRestore(vg);
         }
      }
      float insignia = out(t, .45, 1.16);
      nvgSave(vg); nvgRotate(vg, .65f);
      float logoScale = reduced ? 1 : .91f + .09f * insignia;
      nvgScale(vg, logoScale, logoScale);
      r.shadow(-90, -78, 180, 156, 26, .7f);
      r.outline(-90, -67, 180, 134, 12, alpha(LIME, out(t, 1.54, 2.1)));
      r.outline(-72, -49, 144, 98, 1.2f, WHITE);
      nvgRestore(vg);
      shadowText("轮回", 0, 5, 39, WHITE, "serif", NVG_ALIGN_CENTER, 5);
      shadowText("SAMSARA", 0, 31, 19, WHITE, "intro-regular", NVG_ALIGN_CENTER, 5);
      nvgSave(vg);
      nvgGlobalAlpha(vg, light * out(t, 1.2, 2.2));
      shadowText("SAMSARA CLIENT " + ClientBranding.DISPLAY_VERSION, -610, 76, 26, WHITE, "intro-bold", NVG_ALIGN_CENTER, 5);
      shadowText("TERMINAL SERVICE", -500, 103, 25, WHITE, "intro-bold", NVG_ALIGN_CENTER, 5);
      for (int i = 0; i < 26 * ramp(t, 1.8, 2.7); i++) r.rect(-690 + i * 17, 116, 11, 7, LIME);
      nvgRestore(vg);
      float badge = out(t, 1.95, 2.7);
      nvgSave(vg);
      nvgTranslate(vg, 357 + (reduced ? 0 : 50 * (1 - badge)), 134);
      nvgGlobalAlpha(vg, light * badge);
      r.outline(-109, -104, 218, 208, 2.5f, WHITE);
      r.outline(-102, -97, 204, 194, 1, 0x44787C86);
      shadowText("S A", 0, -40, 48, WHITE, "intro-regular", NVG_ALIGN_CENTER, 3);
      shadowText("M S", 0, 22, 48, WHITE, "intro-regular", NVG_ALIGN_CENTER, 3);
      shadowText("A R A", 0, 80, 40, WHITE, "intro-bold", NVG_ALIGN_CENTER, 3);
      nvgRestore(vg);
      for (int i = 0; i < 16 * ramp(t, 2.04, 2.9); i++)
         r.line(480 + i * 11, 54, 501 + i * 11, -14, 6, WHITE);
      glow(345, 15, 130, alpha(WHITE, .85f * out(t, 1.7, 2.6)));
      glow(345, 15, 62, alpha(WHITE, out(t, 1.7, 2.6)));
      glow(-440, 28, 62, alpha(WHITE, .48f * out(t, 1.7, 2.6)));
      nvgRestore(vg);
      r.rect(0, 0, 1600, 900, alpha(0xFF000000, 1 - light));
      vignette();
   }

   private void paleBackground(double t) {
      r.rect(0, 0, 1600, 900, 0xFFCFD7FB);
      float drift = (float)(t - 4.4) * 18;
      glow(280 + drift, 560, 860, 0xD9FFFFFF);
      glow(1430 - drift, 180, 820, 0x85EFF3FF);
      glow(895 + drift, 880, 810, 0x518C9DEB);
      grain(.10f);
   }

   private void login(double t, boolean reduced) {
      IntroCamera camera = new IntroCamera(t, reduced);
      fiducials(camera, t, reduced);
      float vanish = 1 - out(t, 9.14, 9.39);
      if (vanish > 0) {
         nvgSave(vg); nvgGlobalAlpha(vg, vanish);
         terminalMark(camera, t, reduced);
         inputs(camera, t, reduced);
         nvgRestore(vg);
      }
      if (t >= 8.78) identityCard(camera, t, reduced);
      if (t >= 9.28) identification(camera, t, reduced);
      if (t >= 9.24 && t < 10.47) verificationBand(t, reduced);
   }

   private void fiducials(IntroCamera camera, double t, boolean reduced) {
      float p = reduced ? out(t, 4.42, 5.15) : out(t, 4.95, 5.65);
      for (int i = 0; i < 4; i++) {
         float x = i % 2 == 0 ? 220 : 1380;
         float y = i < 2 ? 290 : 620;
         if (!reduced) { x = mix(1380, x, p); y = mix(620, y, p); }
         camera.begin(vg, x, y);
         nvgGlobalAlpha(vg, out(t, 4.42, 4.55));
         r.shadow(-20, -15, 40, 32, 12, .45f);
         r.rect(-20, -15, 40, 32, INK);
         IntroCamera.end(vg);
      }
   }

   private void terminalMark(IntroCamera camera, double t, boolean reduced) {
      camera.begin(vg, 295, 450);
      float reveal = out(t, 5.15, 5.8);
      shadowLine(0, -135, 500 * reveal, -135, 2);
      shadowLine(0, 122, 500 * out(t, 5.24, 5.9), 122, 2);
      if (t >= 5.91 || reduced) {
         nvgSave(vg); nvgGlobalAlpha(vg, reveal * (1 - out(t, 9.14, 9.39)));
         terminalTitle(); nvgRestore(vg);
      } else for (int band = 0; band < 10; band++) {
         float p = out(t, 5.23 + band * .013, 5.77 + band * .013);
         if (p == 0) continue;
         nvgSave(vg); nvgIntersectScissor(vg, -12, -146 + band * 16, 524 * p, 16.5f);
         nvgTranslate(vg, 45 * (1 - p), 0);
         terminalTitle();
         nvgRestore(vg);
      }
      float underline = 1 - out(t, 6.15, 6.53);
      if (underline > 0) r.rect(0, 0, 500 * out(t, 5.68, 6.12), 13 * underline, INK);
      float subtitle = out(t, 6.22, 6.64);
      nvgSave(vg); nvgGlobalAlpha(vg, subtitle * (1 - out(t, 9.15, 9.4)));
      shadowText(ClientBranding.WINDOW_TITLE, 4, 44 + (reduced ? 0 : 8 * (1 - subtitle)), 32, INK, "intro-bold", 0, 5);
      shadowText("TERMINAL SERVICE", 0, 96, 47, INK, "intro-bold", 0, 5);
      nvgRestore(vg);
      IntroCamera.end(vg);
   }

   private void terminalTitle() {
      r.fontBlur(6); stretchText("SAMSARA", 0, 3, 154, 500, 0x7047474C, "intro-italic"); r.fontBlur(0);
      stretchText("SAMSARA", 0, -9, 154, 500, INK, "intro-italic");
   }

   private void inputs(IntroCamera camera, double t, boolean reduced) {
      camera.begin(vg, 830, 280);
      float line = out(t, 5.13, 5.7);
      shadowLine(0, 0, 0, 390 * line, 3);
      IntroCamera.end(vg);
      for (int row = 0; row < 2; row++) {
         float y = row == 0 ? 348 : 482;
         float p = out(t, 5.53 + row * .10, 6.14 + row * .10);
         camera.begin(vg, 885 + (reduced ? 0 : 300 * (1 - p)), y);
         nvgScale(vg, reduced ? 1 : .4f + .6f * p, 1);
         nvgGlobalAlpha(vg, p * (1 - out(t, 9.14, 9.39)));
         r.hollowShadow(0, 0, 468, 75, 12, .4f);
         r.outline(0, 0, 468, 75, 2.5f, INK);
         r.outline(6, 7, 460, 69, 1, 0x444A506A);
         IntroCamera.end(vg);
         camera.begin(vg, 864, y - 12);
         String title = row == 0 ? "USERNAME" : "PASSWORD";
         shadowText(type(title, t, 5.6 + row * .12, .043), 0, 0, 32, INK, "intro-regular", 0, 5);
         IntroCamera.end(vg);
         camera.begin(vg, 905, y + 53);
         shadowText(row == 0 ? type("Dev", t, 6.57, .22) : type("*************", t, 7.75, .077),
            0, 0, 33, INK, "intro-bold", 0, 5);
         IntroCamera.end(vg);
      }
      camera.begin(vg, 1356, 585);
      nvgGlobalAlpha(vg, out(t, 6.02, 6.32) * (1 - out(t, 9.1, 9.32)));
      shadowText("SAMSARA AUTHENTICATION", 0, 0, 16, INK, "intro-regular", NVG_ALIGN_RIGHT, 3);
      IntroCamera.end(vg);
   }

   private void identityCard(IntroCamera camera, double t, boolean reduced) {
      float entry = card(t, 8.78, 9.02);
      float relocate = card(t, 9.2, 9.86);
      float x = reduced ? 1126 : mix(1036, 1126, relocate);
      camera.begin(vg, x, 430);
      nvgScale(vg, 1, .86f);
      nvgGlobalAlpha(vg, entry * (1 - ramp(t, 10.77, 10.9)));
      if (!reduced) {
         float size = .93f + .07f * entry;
         nvgRotate(vg, -.035f * (1 - relocate)); nvgScale(vg, size, size);
         float flip = ramp(t, 9.02, 9.23);
         nvgScale(vg, Math.max(.025f, Math.abs((float)Math.cos(Math.PI * flip))), 1);
      }
      r.shadow(-132, -130, 264, 260, 15, .52f);
      boolean light = t >= 9.94 && t < 10.25;
      r.rect(-132, -130, 264, 260, INK);
      if (light) r.rect(-124, -122, 248, 244, 0xFFE1E6F9);
      r.outline(-125, -123, 250, 246, 3, light ? INK : WHITE);
      r.outline(-119, -117, 238, 234, 1, light ? 0xFF777B87 : 0xFF75777E);
      if (t < 9.15) {
         nvgGlobalAlpha(vg, entry * out(t, 8.92, 9.04));
         shadowText("* *", 0, 24, 65, WHITE, "intro-regular", NVG_ALIGN_CENTER, 2);
      } else {
         if (t >= 10.25) {
            float scan = out(t, 10.25, 10.51);
            nvgSave(vg); nvgIntersectScissor(vg, -119, -117, 238, 234 * scan);
            r.image("identity", -119, -129, 238, 354, 1); nvgRestore(vg);
            r.line(-119, -117 + 234 * scan, 119, -117 + 234 * scan, 2, 0xFFE0E8F7);
         } else {
            float opacity = out(t, 9.16, 9.33);
            for (int i = 0; i < 9; i++) {
               float xx = -86 + i % 3 * 69, yy = -84 + i / 3 * 69;
               r.line(xx + 14, yy, xx, yy + 50, 2, alpha(light ? INK : 0xFFA3A3A8, opacity));
            }
         }
      }
      nvgGlobalAlpha(vg, out(t, 9.65, 10.05) * (1 - ramp(t, 10.77, 10.9)));
      shadowText("Dev", 0, 180, 42, INK, "intro-bold", NVG_ALIGN_CENTER, 3);
      IntroCamera.end(vg);
   }

   private void identification(IntroCamera camera, double t, boolean reduced) {
      float title = card(t, 9.28, 10.03);
      camera.begin(vg, 310 + (reduced ? 0 : 158 * (1 - title)), 345);
      nvgGlobalAlpha(vg, out(t, 9.28, 9.53) * (1 - ramp(t, 10.77, 10.9)));
      r.fontBlur(6); stretchText("ADMINISTRATOR", 2, 12, 73, 581, 0x68414144, "intro-regular");
      r.fontBlur(0); stretchText("ADMINISTRATOR", 0, 0, 73, 581, INK, "intro-regular");
      IntroCamera.end(vg);
      float bar = card(t, 9.45, 10.04);
      camera.begin(vg, 283 - (reduced ? 0 : 180 * (1 - bar)), 365);
      nvgGlobalAlpha(vg, out(t, 9.45, 9.62) * (1 - ramp(t, 10.77, 10.9)));
      r.shadow(0, 0, 614, 96, 12, .36f);
      r.rect(0, 0, 614, 96, t < 9.77 ? 0xFF36C449 : INK);
      r.fitText("IDENTIFIED", 15, 82, 91, 580, WHITE, "intro-bold");
      IntroCamera.end(vg);
      float welcome = card(t, 9.49, 10.25);
      camera.begin(vg, 295 - (reduced ? 0 : 148 * (1 - welcome)), 577);
      nvgGlobalAlpha(vg, ramp(t, 9.49, 9.9) * (1 - ramp(t, 10.77, 10.9)));
      shadowText("WELCOME", 7, -8, 110, INK, "intro-regular", 0, 6);
      float path = out(t, 9.87, 10.15);
      r.hollowShadow(0, -98, 590, 115, 9, .23f);
      r.line(0, -98, 590 * path, -98, 3, INK);
      r.line(0, 17, 590 * path, 17, 3, INK);
      r.line(0, -98, 0, 17, 3, INK);
      if (path > .95) r.line(590, -98, 590, 17, 3, INK);
      IntroCamera.end(vg);
      camera.begin(vg, 933, 333);
      nvgGlobalAlpha(vg, out(t, 9.71, 10.1) * (1 - ramp(t, 10.77, 10.9)));
      r.shadow(0, 0, 54, 126, 8, .4f);
      r.rect(0, 0, 54, 126, INK);
      nvgTranslate(vg, 9, 9); nvgRotate(vg, (float)Math.PI / 2);
      r.text("SA01", 0, -2, 41, WHITE, "intro-regular", 0);
      IntroCamera.end(vg);
   }

   private void verificationBand(double t, boolean reduced) {
      float opacity = ramp(t, 9.24, 9.42) * (1 - out(t, 10.1, 10.47));
      nvgSave(vg); nvgGlobalAlpha(vg, opacity);
      float retract = out(t, 9.58, 10.22);
      float bandY = mix(691, 778, retract), bandH = 810 - bandY;
      r.rect(0, bandY, 1600, bandH, 0x99707788);
      float pulse = reduced ? 1 : sample(t, 9.24,.9, 9.5,1.12, 9.8,.9, 10.05,1.12, 10.3,.9);
      nvgSave(vg); nvgTranslate(vg, 800, bandY + bandH / 2); nvgRotate(vg, (float)Math.PI / 4);
      nvgScale(vg, pulse * mix(1, .28f, retract), pulse * mix(1, .28f, retract));
      r.outline(-35, -35, 70, 70, 3, WHITE); r.outline(-27, -27, 54, 54, 9, WHITE); nvgRestore(vg);
      r.text("正在确认终端身份……", mix(911,850,retract), bandY + bandH * .65f, mix(25,16,retract), WHITE, "serif", 0);
      nvgRestore(vg);
   }

   private void connection(double t, boolean reduced) {
      r.rect(0, 0, 1600, 900, 0xFF1B1B1B);
      glow(770, 120, 1030, 0x543F3F42);
      r.rect(0, 0, 1600, 900, alpha(0xFFB5B6C5, .4f * (1 - out(t, 10.79, 11.31))));
      particles(reduced ? 0 : t);
      float pullback = reduced ? .86f : sample(t, 10.7667,5.7, 10.86,2.6, 11,1.35, 11.2,1.01, 11.5,.86, 12.1,.8);
      nvgSave(vg); nvgTranslate(vg, 800, 450); nvgScale(vg, pullback, pullback);
      sphere.draw(vg, r.resources(), reduced ? .3 : (t - 10.7667) * .51, LIME);
      nvgSave(vg); nvgGlobalAlpha(vg, .31f); tower(0, -111, 220, WHITE); nvgRestore(vg);
      r.rect(-133, -235, 266, 470, 0xA8232329);
      r.outline(-133, -235, 266, 470, 1.8f, 0xFFE7E7EA);
      r.text("服务器", 0, -142, 26, WHITE, "serif", NVG_ALIGN_CENTER);
      r.text("轮回", 0, -87, 56, WHITE, "serif", NVG_ALIGN_CENTER);
      r.text("#", -60, -18, 59, WHITE, "intro-regular", NVG_ALIGN_CENTER);
      r.text("0", 0, 96, 174, WHITE, "intro-regular", NVG_ALIGN_CENTER);
      nvgRestore(vg);
      float progress = 1 - (float)Math.pow(1 - ramp(t, 10.89, 11.97), 2.4);
      float lineY = mix(694, 605, ramp(t, 10.94, 11.62));
      float gap = 168 * pullback;
      if (t < 11.91) {
         glowLine(0, lineY, (800 - gap) * progress, lineY, 1.8f, LIME);
         glowLine(1600, lineY, 1600 - (800 - gap) * progress, lineY, 1.8f, LIME);
         r.text(Math.round(progress * 100) + "%", 800 - gap, lineY - 19, 23, LIME, "intro-regular", NVG_ALIGN_RIGHT);
         r.text(Math.round(progress * 100) + "%", 800 + gap, lineY - 19, 23, LIME, "intro-regular", 0);
      } else {
         glowLine(0, lineY, 1600, lineY, 1.8f, LIME);
         r.text("100%", 800, lineY - 19, 23, LIME, "intro-regular", NVG_ALIGN_CENTER);
      }
      nvgGlobalAlpha(vg, out(t, 11.05, 11.45));
      r.text("正在接入 Samsara 终端……", 800, 774, 24, 0xAA9A9A9D, "serif", NVG_ALIGN_CENTER);
      nvgGlobalAlpha(vg, 1);
      grain(.15f);
      r.rect(0, 0, 1600, 900, alpha(0xFF000000, out(t, 11.93, 12.16)));
   }

   private void emblemScene(double t, boolean reduced) {
      r.image("menu-transition", 0, 0, 1600, 900, 1);
      r.rect(0, 0, 1600, 900, 0x51303438);
      float show = out(t, 12.14, 12.28);
      nvgSave(vg); nvgGlobalAlpha(vg, show);
      float s = reduced ? 1 : 1.08f - .08f * out(t, 12.18, 12.48);
      nvgTranslate(vg, 800, 450); nvgScale(vg, s, s); tower(0, 0, 300, INK); nvgRestore(vg);
      for (int i = 0; i < 5; i++) {
         float yy = 230 + i * 90;
         r.line(200, yy, 1380, yy, .7f, 0x3AFFFFFF);
         r.line(970 + i * 70, 180, 970 + i * 70, 754, .7f, 0x3AFFFFFF);
      }
      particles(reduced ? 0 : t);
      r.rect(0, 0, 1600, 900, alpha(0xFF000000, 1 - show));
   }

   private void tower(float x, float y, float size, int tint) {
      nvgSave(vg); nvgTranslate(vg, x, y); nvgScale(vg, size / 300, size / 300);
      r.triangle(-144, 119, 0, -143, 144, 119, tint);
      int cutout = tint == INK ? 0xFFB4B9AE : 0xFF444449;
      r.triangle(-126, 108, 0, -119, 126, 108, cutout);
      r.rect(-39, -73, 78, 56, tint);
      for (int i = 0; i < 3; i++) r.rect(-39 + i * 29, -91, 20, 29, tint);
      r.triangle(-37, -30, 37, -30, -68, 67, tint);
      r.triangle(37, -30, -68, 67, 68, 67, tint);
      r.text("SAMSARA", 0, 99, 30, tint, "intro-bold", NVG_ALIGN_CENTER);
      nvgRestore(vg);
   }

   private void particles(double t) {
      for (int i = 0; i < 33; i++) {
         float x = (i * 331 % 1580) + 10;
         float y = (float)((i * 197 + t * (8 + i % 4 * 5)) % 900);
         float radius = i % 7 == 0 ? 4 : 1.5f;
         r.disk(x, y, radius, i % 3 == 0 ? 0x8BDFDFDF : 0x556F7076);
      }
   }

   private void shadowText(String s, float x, float y, float size, int tint, String face, int align, float blur) {
      r.fontBlur(blur);
      r.text(s, x + 2, y + 12, size, 0x68414144, face, align);
      r.fontBlur(0); r.text(s, x, y, size, tint, face, align);
   }

   private void stretchText(String text, float x, float y, float size, float width, int tint, String face) {
      float measured = r.textWidth(text, size, face);
      nvgSave(vg); nvgTranslate(vg, x, y); nvgScale(vg, width / Math.max(1, measured), 1);
      r.text(text, 0, 0, size, tint, face, 0); nvgRestore(vg);
   }

   private void shadowLine(float x, float y, float xx, float yy, float width) {
      r.shadow(x + 2, y + 15, Math.max(width, xx - x), Math.max(width, yy - y), 7, .3f);
      r.line(x, y, xx, yy, width, INK);
   }

   private void glowLine(float x, float y, float xx, float yy, float width, int tint) {
      r.line(x, y + 3, xx, yy + 3, width * 4, alpha(tint, .055f));
      r.line(x, y + 1, xx, yy + 1, width * 2, alpha(tint, .1f));
      r.line(x, y, xx, yy, width, tint);
   }

   private void glow(float x, float y, float radius, int tint) {
      nvgRadialGradient(vg, x, y, 0, radius, rgba(tint, a), rgba(tint & 0xFFFFFF, b), paint);
      nvgBeginPath(vg); nvgCircle(vg, x, y, radius); nvgFillPaint(vg, paint); nvgFill(vg);
   }

   private void vignette() {
      nvgRadialGradient(vg, 800, 450, 280, 920, rgba(0, a), rgba(0x85000000, b), paint);
      nvgBeginPath(vg); nvgRect(vg, 0, 0, 1600, 900); nvgFillPaint(vg, paint); nvgFill(vg);
   }

   private void grain(float opacity) {
      nvgImagePattern(vg, 0, 0, 256, 256, 0, r.imageId("intro-grain"), opacity, paint);
      nvgBeginPath(vg); nvgRect(vg, 0, 0, 1600, 900); nvgFillPaint(vg, paint); nvgFill(vg);
   }

   private static NVGColor rgba(int c, NVGColor out) {
      return out.r((c >> 16 & 255) / 255f).g((c >> 8 & 255) / 255f).b((c & 255) / 255f).a((c >>> 24) / 255f);
   }
   private static int alpha(int c, float opacity) { return Math.round((c >>> 24) * Math.clamp(opacity, 0, 1)) << 24 | c & 0xFFFFFF; }
   private static String type(String text, double t, double start, double step) {
      return text.substring(0, Math.clamp((int)((t - start) / step), 0, text.length()));
   }
}
