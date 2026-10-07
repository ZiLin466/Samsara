package com.samsara.ui.mainmenu.launch;

import com.samsara.util.render.ColorUtility;
import com.samsara.ClientBranding;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;
import static org.lwjgl.nanovg.NanoVG.*;
import static com.samsara.ui.mainmenu.launch.IntroMotion.*;

final class LaunchIntro {
   private static final int INK = 0xFF19191B, WHITE = 0xFFF4F4F2, LIME = 0xFFE1EA31;
   private final LaunchRenderer launchRenderer;
   private final long vg;
   private final LaunchSphere sphere = new LaunchSphere();
   private final NVGColor innerGlowColor = NVGColor.create(), outerGlowColor = NVGColor.create();
   private final NVGPaint paint = NVGPaint.create();

   LaunchIntro(LaunchRenderer renderer, long vg) { launchRenderer = renderer; this.vg = vg; }

   void prepare() { sphere.load(launchRenderer.resources()); }

   void draw(float width, float height, double timeSeconds, boolean reduced) {
      launchRenderer.rect(0, 0, width, height, 0xFF000000);
      var layout = LaunchLayout.of(width, height);
      nvgSave(vg);
      try {
         nvgTranslate(vg, layout.left(), layout.top());
         nvgScale(vg, layout.scale(), layout.scale());
         float bar = timeSeconds < LaunchTimeline.EMBLEM_START ? 90 : 45;
         nvgIntersectScissor(vg, 0, bar, 1600, 900 - bar * 2);
         if (timeSeconds < LaunchTimeline.LOGIN_START) boot(timeSeconds, reduced);
         else if (timeSeconds < LaunchTimeline.CONNECTION_START) {
            paleBackground(timeSeconds);
            login(timeSeconds, reduced);
         } else if (timeSeconds < LaunchTimeline.EMBLEM_START) {
            connection(timeSeconds, reduced);
            if (timeSeconds < 10.9) {
               nvgGlobalAlpha(vg, 1 - ramp(timeSeconds, LaunchTimeline.CONNECTION_START, 10.9));
               login(timeSeconds, reduced);
            }
         } else emblemScene(timeSeconds, reduced);
      } finally { nvgRestore(vg); }
   }

   void menuMask(float width, float height, double timeSeconds) {
      float bar = height * .05f * (1 - out(timeSeconds, 13.1, LaunchTimeline.DURATION));
      if (bar > .01f) {
         launchRenderer.rect(0, 0, width, bar, 0xFF000000);
         launchRenderer.rect(0, height - bar, width, bar, 0xFF000000);
      }
   }

   private void boot(double timeSeconds, boolean reduced) {
      float camera = reduced ? .6f : bezier(ramp(timeSeconds, 0, 4.6), .61f, 0, .41f, 1);
      nvgSave(vg);
      nvgTranslate(vg, 800, 450);
      nvgRotate(vg, reduced ? -.04f : mix(.018f, -.065f, camera));
      float scale = reduced ? 1.05f : mix(1.26f, 1.12f, camera);
      nvgScale(vg, scale, scale);
      launchRenderer.image("intro-corridor", -800, -450, 1600, 900, 1);
      nvgRestore(vg);
      launchRenderer.rect(0, 0, 1600, 900, 0x181D2639);
      glow(800, 425, 700, 0x1034517A);
      grain(.45f);
      float light = sample(timeSeconds, 0,.015, .5,.07, 1,.23, 1.5,.44, 2,.88, 2.5,1, 4.42,1);
      nvgSave(vg);
      nvgGlobalAlpha(vg, light);
      nvgTranslate(vg, 800, 450);
      nvgRotate(vg, reduced ? -.18f : mix(-.12f, -.19f, camera));
      nvgScale(vg, reduced ? 1 : mix(.78f, 1.08f, camera), .92f);
      float line = out(timeSeconds, 1.32, 2.05);
      glowLine(-1050 * line, 43, -167, 43, 2.6f, LIME);
      glowLine(167, 43, 1050 * line, 43, 2.6f, LIME);
      float ticks = ramp(timeSeconds, 1.7, 2.55);
      for (int i = 0; i < 74 * ticks; i++)
         glowLine(-1020 + i * 10, -6, -1008 + i * 10, 28, 2, LIME);
      float dots = ramp(timeSeconds, 2.08, 3.32);
      for (int i = 0; i < 43 * dots; i++) launchRenderer.rect(186 + i * 14, 10, 8, 8, LIME);
      for (double start : new double[]{1.62, 2.65, 3.8}) {
         float p = ramp(timeSeconds, start, start + .6);
         if (p > 0 && p < 1) {
            nvgSave(vg); nvgRotate(vg, .64f);
            float size = reduced ? 225 : mix(170, 415, out(timeSeconds, start, start + .58));
            float opacity = 1 - (float)Math.pow(p, 5);
            launchRenderer.outline(-size / 2 + 6, -size / 2 + 18, size, size, 3, ColorUtility.multiplyOpacityRounded(LIME, opacity * .16f));
            launchRenderer.outline(-size / 2, -size / 2, size, size, 3, ColorUtility.multiplyOpacityRounded(LIME, opacity));
            nvgRestore(vg);
         }
      }
      float insignia = out(timeSeconds, .45, 1.16);
      nvgSave(vg); nvgRotate(vg, .65f);
      float logoScale = reduced ? 1 : .91f + .09f * insignia;
      nvgScale(vg, logoScale, logoScale);
      launchRenderer.shadow(-90, -78, 180, 156, 26, .7f);
      launchRenderer.outline(-90, -67, 180, 134, 12, ColorUtility.multiplyOpacityRounded(LIME, out(timeSeconds, 1.54, 2.1)));
      launchRenderer.outline(-72, -49, 144, 98, 1.2f, WHITE);
      nvgRestore(vg);
      shadowText("轮回", 0, 5, 39, WHITE, "serif", NVG_ALIGN_CENTER, 5);
      shadowText("SAMSARA", 0, 31, 19, WHITE, "intro-regular", NVG_ALIGN_CENTER, 5);
      nvgSave(vg);
      nvgGlobalAlpha(vg, light * out(timeSeconds, 1.2, 2.2));
      shadowText("SAMSARA CLIENT " + ClientBranding.DISPLAY_VERSION, -610, 76, 26, WHITE, "intro-bold", NVG_ALIGN_CENTER, 5);
      shadowText("TERMINAL SERVICE", -500, 103, 25, WHITE, "intro-bold", NVG_ALIGN_CENTER, 5);
      for (int i = 0; i < 26 * ramp(timeSeconds, 1.8, 2.7); i++) launchRenderer.rect(-690 + i * 17, 116, 11, 7, LIME);
      nvgRestore(vg);
      float badge = out(timeSeconds, 1.95, 2.7);
      nvgSave(vg);
      nvgTranslate(vg, 357 + (reduced ? 0 : 50 * (1 - badge)), 134);
      nvgGlobalAlpha(vg, light * badge);
      launchRenderer.outline(-109, -104, 218, 208, 2.5f, WHITE);
      launchRenderer.outline(-102, -97, 204, 194, 1, 0x44787C86);
      shadowText("S A", 0, -40, 48, WHITE, "intro-regular", NVG_ALIGN_CENTER, 3);
      shadowText("M S", 0, 22, 48, WHITE, "intro-regular", NVG_ALIGN_CENTER, 3);
      shadowText("A R A", 0, 80, 40, WHITE, "intro-bold", NVG_ALIGN_CENTER, 3);
      nvgRestore(vg);
      for (int i = 0; i < 16 * ramp(timeSeconds, 2.04, 2.9); i++)
         launchRenderer.line(480 + i * 11, 54, 501 + i * 11, -14, 6, WHITE);
      glow(345, 15, 130, ColorUtility.multiplyOpacityRounded(WHITE, .85f * out(timeSeconds, 1.7, 2.6)));
      glow(345, 15, 62, ColorUtility.multiplyOpacityRounded(WHITE, out(timeSeconds, 1.7, 2.6)));
      glow(-440, 28, 62, ColorUtility.multiplyOpacityRounded(WHITE, .48f * out(timeSeconds, 1.7, 2.6)));
      nvgRestore(vg);
      launchRenderer.rect(0, 0, 1600, 900, ColorUtility.multiplyOpacityRounded(0xFF000000, 1 - light));
      vignette();
   }

   private void paleBackground(double timeSeconds) {
      launchRenderer.rect(0, 0, 1600, 900, 0xFFCFD7FB);
      float drift = (float)(timeSeconds - 4.4) * 18;
      glow(280 + drift, 560, 860, 0xD9FFFFFF);
      glow(1430 - drift, 180, 820, 0x85EFF3FF);
      glow(895 + drift, 880, 810, 0x518C9DEB);
      grain(.10f);
   }

   private void login(double timeSeconds, boolean reduced) {
      IntroCamera camera = new IntroCamera(timeSeconds, reduced);
      fiducials(camera, timeSeconds, reduced);
      float vanish = 1 - out(timeSeconds, 9.14, 9.39);
      if (vanish > 0) {
         nvgSave(vg); nvgGlobalAlpha(vg, vanish);
         terminalMark(camera, timeSeconds, reduced);
         inputs(camera, timeSeconds, reduced);
         nvgRestore(vg);
      }
      if (timeSeconds >= 8.78) identityCard(camera, timeSeconds, reduced);
      if (timeSeconds >= 9.28) identification(camera, timeSeconds, reduced);
      if (timeSeconds >= 9.24 && timeSeconds < 10.47) verificationBand(timeSeconds, reduced);
   }

   private void fiducials(IntroCamera camera, double timeSeconds, boolean reduced) {
      float p = reduced ? out(timeSeconds, 4.42, 5.15) : out(timeSeconds, 4.95, 5.65);
      for (int i = 0; i < 4; i++) {
         float x = i % 2 == 0 ? 220 : 1380;
         float y = i < 2 ? 290 : 620;
         if (!reduced) { x = mix(1380, x, p); y = mix(620, y, p); }
         camera.begin(vg, x, y);
         nvgGlobalAlpha(vg, out(timeSeconds, 4.42, 4.55));
         launchRenderer.shadow(-20, -15, 40, 32, 12, .45f);
         launchRenderer.rect(-20, -15, 40, 32, INK);
         IntroCamera.end(vg);
      }
   }

   private void terminalMark(IntroCamera camera, double timeSeconds, boolean reduced) {
      camera.begin(vg, 295, 450);
      float reveal = out(timeSeconds, 5.15, 5.8);
      shadowLine(0, -135, 500 * reveal, -135, 2);
      shadowLine(0, 122, 500 * out(timeSeconds, 5.24, 5.9), 122, 2);
      if (timeSeconds >= 5.91 || reduced) {
         nvgSave(vg); nvgGlobalAlpha(vg, reveal * (1 - out(timeSeconds, 9.14, 9.39)));
         terminalTitle(); nvgRestore(vg);
      } else for (int band = 0; band < 10; band++) {
         float p = out(timeSeconds, 5.23 + band * .013, 5.77 + band * .013);
         if (p == 0) continue;
         nvgSave(vg); nvgIntersectScissor(vg, -12, -146 + band * 16, 524 * p, 16.5f);
         nvgTranslate(vg, 45 * (1 - p), 0);
         terminalTitle();
         nvgRestore(vg);
      }
      float underline = 1 - out(timeSeconds, 6.15, 6.53);
      if (underline > 0) launchRenderer.rect(0, 0, 500 * out(timeSeconds, 5.68, 6.12), 13 * underline, INK);
      float subtitle = out(timeSeconds, 6.22, 6.64);
      nvgSave(vg); nvgGlobalAlpha(vg, subtitle * (1 - out(timeSeconds, 9.15, 9.4)));
      shadowText(ClientBranding.WINDOW_TITLE, 4, 44 + (reduced ? 0 : 8 * (1 - subtitle)), 32, INK, "intro-bold", 0, 5);
      shadowText("TERMINAL SERVICE", 0, 96, 47, INK, "intro-bold", 0, 5);
      nvgRestore(vg);
      IntroCamera.end(vg);
   }

   private void terminalTitle() {
      launchRenderer.fontBlur(6); stretchText("SAMSARA", 0, 3, 154, 500, 0x7047474C, "intro-italic"); launchRenderer.fontBlur(0);
      stretchText("SAMSARA", 0, -9, 154, 500, INK, "intro-italic");
   }

   private void inputs(IntroCamera camera, double timeSeconds, boolean reduced) {
      camera.begin(vg, 830, 280);
      float line = out(timeSeconds, 5.13, 5.7);
      shadowLine(0, 0, 0, 390 * line, 3);
      IntroCamera.end(vg);
      for (int row = 0; row < 2; row++) {
         float y = row == 0 ? 348 : 482;
         float p = out(timeSeconds, 5.53 + row * .10, 6.14 + row * .10);
         camera.begin(vg, 885 + (reduced ? 0 : 300 * (1 - p)), y);
         nvgScale(vg, reduced ? 1 : .4f + .6f * p, 1);
         nvgGlobalAlpha(vg, p * (1 - out(timeSeconds, 9.14, 9.39)));
         launchRenderer.hollowShadow(0, 0, 468, 75, 12, .4f);
         launchRenderer.outline(0, 0, 468, 75, 2.5f, INK);
         launchRenderer.outline(6, 7, 460, 69, 1, 0x444A506A);
         IntroCamera.end(vg);
         camera.begin(vg, 864, y - 12);
         String title = row == 0 ? "USERNAME" : "PASSWORD";
         shadowText(type(title, timeSeconds, 5.6 + row * .12, .043), 0, 0, 32, INK, "intro-regular", 0, 5);
         IntroCamera.end(vg);
         camera.begin(vg, 905, y + 53);
         shadowText(row == 0 ? type("Dev", timeSeconds, 6.57, .22) : type("*************", timeSeconds, 7.75, .077),
            0, 0, 33, INK, "intro-bold", 0, 5);
         IntroCamera.end(vg);
      }
      camera.begin(vg, 1356, 585);
      nvgGlobalAlpha(vg, out(timeSeconds, 6.02, 6.32) * (1 - out(timeSeconds, 9.1, 9.32)));
      shadowText("SAMSARA AUTHENTICATION", 0, 0, 16, INK, "intro-regular", NVG_ALIGN_RIGHT, 3);
      IntroCamera.end(vg);
   }

   private void identityCard(IntroCamera camera, double timeSeconds, boolean reduced) {
      float entry = card(timeSeconds, 8.78, 9.02);
      float relocate = card(timeSeconds, 9.2, 9.86);
      float x = reduced ? 1126 : mix(1036, 1126, relocate);
      camera.begin(vg, x, 430);
      nvgScale(vg, 1, .86f);
      nvgGlobalAlpha(vg, entry * (1 - ramp(timeSeconds, 10.77, 10.9)));
      if (!reduced) {
         float size = .93f + .07f * entry;
         nvgRotate(vg, -.035f * (1 - relocate)); nvgScale(vg, size, size);
         float flip = ramp(timeSeconds, 9.02, 9.23);
         nvgScale(vg, Math.max(.025f, Math.abs((float)Math.cos(Math.PI * flip))), 1);
      }
      launchRenderer.shadow(-132, -130, 264, 260, 15, .52f);
      boolean light = timeSeconds >= 9.94 && timeSeconds < 10.25;
      launchRenderer.rect(-132, -130, 264, 260, INK);
      if (light) launchRenderer.rect(-124, -122, 248, 244, 0xFFE1E6F9);
      launchRenderer.outline(-125, -123, 250, 246, 3, light ? INK : WHITE);
      launchRenderer.outline(-119, -117, 238, 234, 1, light ? 0xFF777B87 : 0xFF75777E);
      if (timeSeconds < 9.15) {
         nvgGlobalAlpha(vg, entry * out(timeSeconds, 8.92, 9.04));
         shadowText("* *", 0, 24, 65, WHITE, "intro-regular", NVG_ALIGN_CENTER, 2);
      } else {
         if (timeSeconds >= 10.25) {
            float scan = out(timeSeconds, 10.25, 10.51);
            nvgSave(vg); nvgIntersectScissor(vg, -119, -117, 238, 234 * scan);
            launchRenderer.image("identity", -119, -129, 238, 354, 1); nvgRestore(vg);
            launchRenderer.line(-119, -117 + 234 * scan, 119, -117 + 234 * scan, 2, 0xFFE0E8F7);
         } else {
            float opacity = out(timeSeconds, 9.16, 9.33);
            for (int i = 0; i < 9; i++) {
               float xx = -86 + i % 3 * 69, yy = -84 + i / 3 * 69;
               launchRenderer.line(xx + 14, yy, xx, yy + 50, 2, ColorUtility.multiplyOpacityRounded(light ? INK : 0xFFA3A3A8, opacity));
            }
         }
      }
      nvgGlobalAlpha(vg, out(timeSeconds, 9.65, 10.05) * (1 - ramp(timeSeconds, 10.77, 10.9)));
      shadowText("Dev", 0, 180, 42, INK, "intro-bold", NVG_ALIGN_CENTER, 3);
      IntroCamera.end(vg);
   }

   private void identification(IntroCamera camera, double timeSeconds, boolean reduced) {
      float title = card(timeSeconds, 9.28, 10.03);
      camera.begin(vg, 310 + (reduced ? 0 : 158 * (1 - title)), 345);
      nvgGlobalAlpha(vg, out(timeSeconds, 9.28, 9.53) * (1 - ramp(timeSeconds, 10.77, 10.9)));
      launchRenderer.fontBlur(6); stretchText("ADMINISTRATOR", 2, 12, 73, 581, 0x68414144, "intro-regular");
      launchRenderer.fontBlur(0); stretchText("ADMINISTRATOR", 0, 0, 73, 581, INK, "intro-regular");
      IntroCamera.end(vg);
      float bar = card(timeSeconds, 9.45, 10.04);
      camera.begin(vg, 283 - (reduced ? 0 : 180 * (1 - bar)), 365);
      nvgGlobalAlpha(vg, out(timeSeconds, 9.45, 9.62) * (1 - ramp(timeSeconds, 10.77, 10.9)));
      launchRenderer.shadow(0, 0, 614, 96, 12, .36f);
      launchRenderer.rect(0, 0, 614, 96, timeSeconds < 9.77 ? 0xFF36C449 : INK);
      launchRenderer.fitText("IDENTIFIED", 15, 82, 91, 580, WHITE, "intro-bold");
      IntroCamera.end(vg);
      float welcome = card(timeSeconds, 9.49, 10.25);
      camera.begin(vg, 295 - (reduced ? 0 : 148 * (1 - welcome)), 577);
      nvgGlobalAlpha(vg, ramp(timeSeconds, 9.49, 9.9) * (1 - ramp(timeSeconds, 10.77, 10.9)));
      shadowText("WELCOME", 7, -8, 110, INK, "intro-regular", 0, 6);
      float path = out(timeSeconds, 9.87, 10.15);
      launchRenderer.hollowShadow(0, -98, 590, 115, 9, .23f);
      launchRenderer.line(0, -98, 590 * path, -98, 3, INK);
      launchRenderer.line(0, 17, 590 * path, 17, 3, INK);
      launchRenderer.line(0, -98, 0, 17, 3, INK);
      if (path > .95) launchRenderer.line(590, -98, 590, 17, 3, INK);
      IntroCamera.end(vg);
      camera.begin(vg, 933, 333);
      nvgGlobalAlpha(vg, out(timeSeconds, 9.71, 10.1) * (1 - ramp(timeSeconds, 10.77, 10.9)));
      launchRenderer.shadow(0, 0, 54, 126, 8, .4f);
      launchRenderer.rect(0, 0, 54, 126, INK);
      nvgTranslate(vg, 9, 9); nvgRotate(vg, (float)Math.PI / 2);
      launchRenderer.text("SA01", 0, -2, 41, WHITE, "intro-regular", 0);
      IntroCamera.end(vg);
   }

   private void verificationBand(double timeSeconds, boolean reduced) {
      float opacity = ramp(timeSeconds, 9.24, 9.42) * (1 - out(timeSeconds, 10.1, 10.47));
      nvgSave(vg); nvgGlobalAlpha(vg, opacity);
      float retract = out(timeSeconds, 9.58, 10.22);
      float bandY = mix(691, 778, retract), bandH = 810 - bandY;
      launchRenderer.rect(0, bandY, 1600, bandH, 0x99707788);
      float pulse = reduced ? 1 : sample(timeSeconds, 9.24,.9, 9.5,1.12, 9.8,.9, 10.05,1.12, 10.3,.9);
      nvgSave(vg); nvgTranslate(vg, 800, bandY + bandH / 2); nvgRotate(vg, (float)Math.PI / 4);
      nvgScale(vg, pulse * mix(1, .28f, retract), pulse * mix(1, .28f, retract));
      launchRenderer.outline(-35, -35, 70, 70, 3, WHITE); launchRenderer.outline(-27, -27, 54, 54, 9, WHITE); nvgRestore(vg);
      launchRenderer.text("正在确认终端身份……", mix(911,850,retract), bandY + bandH * .65f, mix(25,16,retract), WHITE, "serif", 0);
      nvgRestore(vg);
   }

   private void connection(double timeSeconds, boolean reduced) {
      launchRenderer.rect(0, 0, 1600, 900, 0xFF1B1B1B);
      glow(770, 120, 1030, 0x543F3F42);
      launchRenderer.rect(0, 0, 1600, 900, ColorUtility.multiplyOpacityRounded(0xFFB5B6C5, .4f * (1 - out(timeSeconds, 10.79, 11.31))));
      particles(reduced ? 0 : timeSeconds);
      float pullback = reduced ? .86f : sample(timeSeconds, 10.7667,5.7, 10.86,2.6, 11,1.35, 11.2,1.01, 11.5,.86, 12.1,.8);
      nvgSave(vg); nvgTranslate(vg, 800, 450); nvgScale(vg, pullback, pullback);
      sphere.draw(vg, launchRenderer.resources(), reduced ? .3 : (timeSeconds - 10.7667) * .51, LIME);
      nvgSave(vg); nvgGlobalAlpha(vg, .31f); tower(0, -111, 220, WHITE); nvgRestore(vg);
      launchRenderer.rect(-133, -235, 266, 470, 0xA8232329);
      launchRenderer.outline(-133, -235, 266, 470, 1.8f, 0xFFE7E7EA);
      launchRenderer.text("服务器", 0, -142, 26, WHITE, "serif", NVG_ALIGN_CENTER);
      launchRenderer.text("轮回", 0, -87, 56, WHITE, "serif", NVG_ALIGN_CENTER);
      launchRenderer.text("#", -60, -18, 59, WHITE, "intro-regular", NVG_ALIGN_CENTER);
      launchRenderer.text("0", 0, 96, 174, WHITE, "intro-regular", NVG_ALIGN_CENTER);
      nvgRestore(vg);
      float progress = 1 - (float)Math.pow(1 - ramp(timeSeconds, 10.89, 11.97), 2.4);
      float lineY = mix(694, 605, ramp(timeSeconds, 10.94, 11.62));
      float gap = 168 * pullback;
      if (timeSeconds < 11.91) {
         glowLine(0, lineY, (800 - gap) * progress, lineY, 1.8f, LIME);
         glowLine(1600, lineY, 1600 - (800 - gap) * progress, lineY, 1.8f, LIME);
         launchRenderer.text(Math.round(progress * 100) + "%", 800 - gap, lineY - 19, 23, LIME, "intro-regular", NVG_ALIGN_RIGHT);
         launchRenderer.text(Math.round(progress * 100) + "%", 800 + gap, lineY - 19, 23, LIME, "intro-regular", 0);
      } else {
         glowLine(0, lineY, 1600, lineY, 1.8f, LIME);
         launchRenderer.text("100%", 800, lineY - 19, 23, LIME, "intro-regular", NVG_ALIGN_CENTER);
      }
      nvgGlobalAlpha(vg, out(timeSeconds, 11.05, 11.45));
      launchRenderer.text("正在接入 Samsara 终端……", 800, 774, 24, 0xAA9A9A9D, "serif", NVG_ALIGN_CENTER);
      nvgGlobalAlpha(vg, 1);
      grain(.15f);
      launchRenderer.rect(0, 0, 1600, 900, ColorUtility.multiplyOpacityRounded(0xFF000000, out(timeSeconds, 11.93, 12.16)));
   }

   private void emblemScene(double timeSeconds, boolean reduced) {
      launchRenderer.image("menu-transition", 0, 0, 1600, 900, 1);
      launchRenderer.rect(0, 0, 1600, 900, 0x51303438);
      float show = out(timeSeconds, 12.14, 12.28);
      nvgSave(vg); nvgGlobalAlpha(vg, show);
      float s = reduced ? 1 : 1.08f - .08f * out(timeSeconds, 12.18, 12.48);
      nvgTranslate(vg, 800, 450); nvgScale(vg, s, s); tower(0, 0, 300, INK); nvgRestore(vg);
      for (int i = 0; i < 5; i++) {
         float yy = 230 + i * 90;
         launchRenderer.line(200, yy, 1380, yy, .7f, 0x3AFFFFFF);
         launchRenderer.line(970 + i * 70, 180, 970 + i * 70, 754, .7f, 0x3AFFFFFF);
      }
      particles(reduced ? 0 : timeSeconds);
      launchRenderer.rect(0, 0, 1600, 900, ColorUtility.multiplyOpacityRounded(0xFF000000, 1 - show));
   }

   private void tower(float x, float y, float size, int tint) {
      nvgSave(vg); nvgTranslate(vg, x, y); nvgScale(vg, size / 300, size / 300);
      launchRenderer.triangle(-144, 119, 0, -143, 144, 119, tint);
      int cutout = tint == INK ? 0xFFB4B9AE : 0xFF444449;
      launchRenderer.triangle(-126, 108, 0, -119, 126, 108, cutout);
      launchRenderer.rect(-39, -73, 78, 56, tint);
      for (int i = 0; i < 3; i++) launchRenderer.rect(-39 + i * 29, -91, 20, 29, tint);
      launchRenderer.triangle(-37, -30, 37, -30, -68, 67, tint);
      launchRenderer.triangle(37, -30, -68, 67, 68, 67, tint);
      launchRenderer.text("SAMSARA", 0, 99, 30, tint, "intro-bold", NVG_ALIGN_CENTER);
      nvgRestore(vg);
   }

   private void particles(double timeSeconds) {
      for (int i = 0; i < 33; i++) {
         float x = (i * 331 % 1580) + 10;
         float y = (float)((i * 197 + timeSeconds * (8 + i % 4 * 5)) % 900);
         float radius = i % 7 == 0 ? 4 : 1.5f;
         launchRenderer.disk(x, y, radius, i % 3 == 0 ? 0x8BDFDFDF : 0x556F7076);
      }
   }

   private void shadowText(String label, float x, float y, float size, int tint, String face, int align, float blur) {
      launchRenderer.fontBlur(blur);
      launchRenderer.text(label, x + 2, y + 12, size, 0x68414144, face, align);
      launchRenderer.fontBlur(0); launchRenderer.text(label, x, y, size, tint, face, align);
   }

   private void stretchText(String text, float x, float y, float size, float width, int tint, String face) {
      float measured = launchRenderer.textWidth(text, size, face);
      nvgSave(vg); nvgTranslate(vg, x, y); nvgScale(vg, width / Math.max(1, measured), 1);
      launchRenderer.text(text, 0, 0, size, tint, face, 0); nvgRestore(vg);
   }

   private void shadowLine(float x, float y, float xx, float yy, float width) {
      launchRenderer.shadow(x + 2, y + 15, Math.max(width, xx - x), Math.max(width, yy - y), 7, .3f);
      launchRenderer.line(x, y, xx, yy, width, INK);
   }

   private void glowLine(float x, float y, float xx, float yy, float width, int tint) {
      launchRenderer.line(x, y + 3, xx, yy + 3, width * 4, ColorUtility.multiplyOpacityRounded(tint, .055f));
      launchRenderer.line(x, y + 1, xx, yy + 1, width * 2, ColorUtility.multiplyOpacityRounded(tint, .1f));
      launchRenderer.line(x, y, xx, yy, width, tint);
   }

   private void glow(float x, float y, float radius, int tint) {
      nvgRadialGradient(vg, x, y, 0, radius, rgba(tint, innerGlowColor), rgba(tint & 0xFFFFFF, outerGlowColor), paint);
      nvgBeginPath(vg); nvgCircle(vg, x, y, radius); nvgFillPaint(vg, paint); nvgFill(vg);
   }

   private void vignette() {
      nvgRadialGradient(vg, 800, 450, 280, 920, rgba(0, innerGlowColor), rgba(0x85000000, outerGlowColor), paint);
      nvgBeginPath(vg); nvgRect(vg, 0, 0, 1600, 900); nvgFillPaint(vg, paint); nvgFill(vg);
   }

   private void grain(float opacity) {
      nvgImagePattern(vg, 0, 0, 256, 256, 0, launchRenderer.imageId("intro-grain"), opacity, paint);
      nvgBeginPath(vg); nvgRect(vg, 0, 0, 1600, 900); nvgFillPaint(vg, paint); nvgFill(vg);
   }

   private static NVGColor rgba(int c, NVGColor out) {
      return out.r((c >> 16 & 255) / 255f).g((c >> 8 & 255) / 255f).b((c & 255) / 255f).a((c >>> 24) / 255f);
   }

   private static String type(String text, double timeSeconds, double start, double step) {
      return text.substring(0, Math.clamp((int)((timeSeconds - start) / step), 0, text.length()));
   }
}
