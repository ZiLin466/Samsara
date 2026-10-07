package com.samsara.ui.mainmenu.launch;

import com.samsara.util.render.ColorUtility;
import com.samsara.util.animation.HoverMotion;
import com.samsara.ClientBranding;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.system.MemoryUtil;
import static org.lwjgl.nanovg.NanoVG.*;

public final class LaunchRenderer implements AutoCloseable {
   private static final int INK = 0xFF101114, WHITE = 0xFFF6F6F5;
   private final long vg;
   private final Function<String, byte[]> resources;
   private final Map<String, Integer> images = new HashMap<>();
   private final Map<String, Integer> fonts = new HashMap<>();
   private final List<ByteBuffer> fontData = new ArrayList<>();
   private final NVGColor color = NVGColor.create();
   private final NVGColor secondColor = NVGColor.create();
   private final NVGPaint paint = NVGPaint.create();
   private final LaunchIntro intro;
   private final Map<String, HoverMotion> lifts = new HashMap<>();
   private boolean reducedMotion;
   private long frameTime;

   public LaunchRenderer(long vg, Function<String, byte[]> resources) {
      this.vg = vg;
      this.resources = resources;
      this.intro = new LaunchIntro(this, vg);
   }

   private void preloadMenu() {
      for (String name : List.of("scene", "operator", "event", "stormwatch", "boot-background", "menu-transition", "identity", "rhodes")) imageId(name);
      font("serif"); font("bold"); font("regular");
      font("intro-regular"); font("intro-bold"); font("intro-italic");
      imageId("intro-corridor"); imageId("intro-grain"); intro.prepare();
   }

   public void loading(float width, float height, float progress, double seconds) {
      if (progress >= 1) preloadMenu();
      nvgSave(vg);
      try {
         rect(0, 0, width, height, 0xFF1F1F1F);
         float unit = Math.min(width / 1600, height / 900);
         float logoW = 475 * unit;
         image("bocchi-logo", (width - logoW) / 2, height * .19f, logoW, logoW * 231 / 728, 1);
         float x = width * .083f, w = width * .77f, y = height * .73f, h = 45 * unit;
         float filled = Math.clamp(progress, 0, 1) * w;
         if (filled > .1f) rounded(x, y, filled, h, Math.min(h, filled) / 2, 0xFF919191);
         float sprite = h * 2.5f;
         float sx = x + filled - h * 1.5f;
         int frame = Math.floorMod((int)(seconds * 20), 20);
         nvgSave(vg);
         nvgIntersectScissor(vg, sx, y - h * 1.5f, sprite, sprite);
         image("bocchi-loading", sx - frame * sprite, y - h * 1.5f, sprite * 20, sprite, 1);
         nvgRestore(vg);
      } finally { nvgRestore(vg); }
   }

   public void render(float width, float height, double seconds, String user, String date,
                      String hovered, String focused, boolean operator, boolean reduceMotion) {
      reducedMotion = reduceMotion;
      frameTime = System.nanoTime();
      nvgSave(vg);
      try {
         if (seconds >= LaunchTimeline.MENU_START) {
            float entry = IntroMotion.out(seconds, LaunchTimeline.MENU_START, LaunchTimeline.DURATION);
            nvgSave(vg);
            if (!reduceMotion) {
               float zoom = 1 + 1.05f * (1 - entry);
               nvgTranslate(vg, width * .34f, 0); nvgScale(vg, zoom, zoom); nvgTranslate(vg, -width * .34f, 0);
            }
            menu(width, height, entry, user, date, hovered, focused, operator);
            nvgRestore(vg);
            intro.menuMask(width, height, seconds);
         } else {
            intro.draw(width, height, seconds, reduceMotion);
         }
      } finally { nvgRestore(vg); }
   }

   private void menu(float width, float height, float entry, String user, String date,
                     String hovered, String focused, boolean operator) {
      cover("scene", width, height, 1820, 1024, 1.035f - .035f * entry, 1);
      if (entry < 1) cover("menu-transition", width, height, 1600, 900, 1.035f - .035f * entry, 1 - entry);
      var layout = LaunchLayout.of(width, height);
      nvgTranslate(vg, layout.left(), layout.top());
      nvgScale(vg, layout.scale(), layout.scale());
      if (operator) image("operator", reducedMotion ? 40 : 40 - 130 * (1 - entry), reducedMotion ? 30 : 30 - 160 * (1 - entry), 980, 980, reducedMotion ? entry : 1);
      nvgGlobalAlpha(vg, entry);
      text("SAMSARA", 395, 65, 42, WHITE, "bold", 0);
      text("CLIENT / " + ClientBranding.DISPLAY_VERSION, 401, 85, 12, WHITE, "regular", 0);
      menuIcon("settings", 54, 48, 23, hovered, focused);
      glyph("warning", 140, 48, 24, WHITE);
      glyph("mail", 222, 48, 26, WHITE);
      glyph("calendar", 308, 48, 25, WHITE);
      menuIcon("quit", 1550, 48, 19, hovered, focused);
      text(date, 972, 41, 19, WHITE, "regular", 0);
      badge(950, 87, 0xFF00B3DA, "2595381");
      badge(1193, 87, 0xFFD95C72, "9376");
      badge(1400, 87, 0xFFEBC545, "2");
      nvgSave(vg);
      nvgTranslate(vg, reducedMotion ? 0 : -265 * (1 - entry), 0);
      nvgGlobalAlpha(vg, reducedMotion ? entry : 1);
      nvgSave(vg);
      if (!reducedMotion && entry < 1) nvgIntersectScissor(vg, 940, 118, 650, 230 * (.2f + .8f * entry));
      rect(948, 125, 628, 210, 0xF5FFFFFF);
      rect(970, 149, 181, 112, 0xFFB8B8BA);
      text("744", 986, 246, 95, INK, "regular", 0);
      rect(970, 269, 181, 45, 0xFF333337);
      text("理智 /130", 1061, 302, 27, WHITE, "serif", NVG_ALIGN_CENTER);
      text("终端", 1172, 217, 58, INK, "serif", 0);
      rect(1178, 243, 61, 29, 0xFF333337);
      text("当前", 1208, 266, 20, WHITE, "serif", NVG_ALIGN_CENTER);
      text("9-1 风暴突击", 1176, 305, 21, INK, "serif", 0);
      image("event", 1359, 142, 211, 170, 1);
      rect(1178, 331, 385, 6, 0xFFEF641D);
      nvgRestore(vg);
      nvgSave(vg);
      nvgGlobalAlpha(vg, reducedMotion ? entry : Math.clamp((entry - .22f) / .78f, 0, 1));
      for (var tile : LaunchLayout.ACTIONS.subList(0, 5)) {
         boolean selected = tile.id().equals(hovered) || tile.id().equals(focused);
         float lift = lifts.computeIfAbsent(tile.id(), key -> new HoverMotion()).update(selected, frameTime, reducedMotion);
         nvgSave(vg);
         float cx = tile.x() + tile.width() / 2, cy = tile.y() + tile.height() / 2;
         nvgTranslate(vg, cx, cy - (reducedMotion ? 0 : 7 * lift));
         float scale = 1 + (reducedMotion ? 0 : .035f * lift);
         nvgScale(vg, scale, scale); nvgTranslate(vg, -cx, -cy);
         boolean dark = tile.id().equals("mods");
         shadow(tile.x() + 3, tile.y() + 5 + 8 * lift, tile.width(), tile.height(), 9 + 23 * lift, .22f + .22f * lift);
         rect(tile.x(), tile.y(), tile.width(), tile.height(), dark ? 0xF548484B : 0xF4FFFFFF);
         String cardGlyph = switch (tile.id()) { case "accounts" -> "multi"; case "multi" -> "accounts"; default -> tile.id(); };
         glyph(cardGlyph, tile.x() + tile.width() - 52, tile.y() + tile.height() * .63f, 42,
            dark ? 0x225F6062 : 0x1A55555B);
         rect(tile.x(), tile.y(), tile.width(), 4, ColorUtility.multiplyOpacityRounded(0xFF00B5DC, lift));
         float size = dark ? 36 : tile.id().equals("multi") ? 39 : tile.id().equals("single") ? 43 : 48;
         fitText(tile.label(), tile.x() + 20, tile.y() + 79, size, tile.width() - 36,
            dark ? WHITE : INK, "serif");
         if (tile.id().equals("accounts")) text("角色管理", tile.x() + 22, tile.y() + 113, 25, 0xFF949497, "serif", 0);
         if (tile.id().equals("options")) text(ClientBranding.DISPLAY_VERSION, tile.x() + 22, tile.y() + 105, 18, 0xFFDE6A37, "bold", 0);
         nvgRestore(vg);
      }
      nvgRestore(vg);
      nvgSave(vg);
      if (!reducedMotion && entry < 1) {
         nvgTranslate(vg, -20 * (1 - entry), -150 * (1 - entry));
         nvgIntersectScissor(vg, 988, 514, 582 * (.2f + .8f * entry), 140 * (.22f + .78f * entry));
      }
      rect(994, 520, 570, 128, 0xF000ABD1);
      glyph("cart", 1101, 569, 44, 0x55B1F5FF);
      text("采购中心", 1011, 611, 36, WHITE, "serif", 0);
      rect(1226, 526, 331, 39, 0xFF45454B);
      text("招募", 1242, 556, 27, WHITE, "serif", 0);
      text("公开招募", 1235, 617, 29, WHITE, "serif", 0);
      text("干员寻访", 1410, 617, 29, WHITE, "serif", 0);
      line(1396, 568, 1396, 644, 2, 0xAA218FAA);
      rect(1381, 553, 31, 33, 0xFFEA6224);
      text("4", 1396, 580, 25, WHITE, "regular", NVG_ALIGN_CENTER);
      nvgRestore(vg);
      nvgRestore(vg);
      rect(0, 390, 182, 150, 0xBE303034);
      circle(101, 426, 64, 3, 0xFF919498);
      text("91", 102, 444, 77, WHITE, "regular", NVG_ALIGN_CENTER);
      text("LV", 102, 475, 22, WHITE, "regular", NVG_ALIGN_CENTER);
      fitText(user, 32, 570, 30, 260, WHITE, "regular");
      text("ID:139330622", 30, 594, 16, WHITE, "regular", 0);
      menuIcon("visibility", 304, 510, 17, hovered, focused);
      menuIcon("replay", 369, 510, 17, hovered, focused);
      rect(26, 615, 481, 111, 0xCA242529);
      rect(29, 609, 143, 13, 0xC8BBBBBA);
      text("博士，今天过得好吗？", 45, 673, 25, WHITE, "serif", 0);
      triangle(478, 699, 494, 699, 486, 709, WHITE);
      image("stormwatch", 30, 736, 305, 158, 1);
      rect(347, 736, 162, 75, 0xED414147);
      glyph("accounts", 374, 771, 18, WHITE);
      text("好友", 412, 787, 33, WHITE, "serif", 0);
      rect(347, 815, 162, 79, 0xF6FFFFFF);
      glyph("mods", 374, 854, 18, 0xFF9B9CA0);
      text("档案", 412, 869, 33, INK, "serif", 0);
      rounded(573, 882, 450, 5, 2.5f, 0x668D9191);
      for (var tile : LaunchLayout.ACTIONS.subList(5, LaunchLayout.ACTIONS.size())) {
         if (tile.id().equals(hovered) || tile.id().equals(focused)) {
            float labelX = Math.min(tile.x(), 1440);
            rect(labelX, tile.y() + tile.height() + 7, 120, 30, 0xE5202226);
            text(tile.label(), labelX + 10, tile.y() + tile.height() + 29, 17, WHITE, "serif", 0);
         }
      }
   }

   private void menuIcon(String id, float x, float y, float r, String hovered, String focused) {
      float p = lifts.computeIfAbsent(id, key -> new HoverMotion())
         .update(id.equals(hovered) || id.equals(focused), frameTime, reducedMotion);
      nvgSave(vg);
      nvgTranslate(vg, x, y - (reducedMotion ? 0 : p * 3));
      float s = 1 + (reducedMotion ? 0 : .1f * p); nvgScale(vg, s, s);
      if (p > .001f) {
         shadow(-28, -28, 56, 56, 17, .4f * p);
         rect(-28, -28, 56, 56, ColorUtility.multiplyOpacityRounded(0xB8202226, p));
      }
      if (id.equals("visibility") || id.equals("replay")) circle(0, 0, 25, 3, WHITE);
      glyph(id, 0, 0, r, WHITE);
      nvgRestore(vg);
   }

   public void terminalPage(float width, float height, String title, String code, String description, String status,
                            float bx, float by, float bw, float bh) {
      nvgSave(vg);
      try {
         cover("menu-transition", width, height, 1600, 900, 1, 1);
         rect(0, 0, width, height, 0xD5141820);
         float s = Math.min(width / 1600, height / 900);
         float margin = Math.max(12, 48 * s);
         text("SAMSARA", margin, 53 * s, 32 * s, WHITE, "bold", 0);
         text("TERMINAL SERVICE", margin, 77 * s, 15 * s, 0xFF969FAB, "regular", 0);
         text(code, width - margin, 56 * s, 20 * s, 0xFF00B8D9, "regular", NVG_ALIGN_RIGHT);
         line(margin, 99 * s, width - margin, 99 * s, Math.max(.5f, s), 0xFF59616D);
         text(title, bx, by - 93 * s, 53 * s, WHITE, "serif", 0);
         text(status, bx + bw, by - 90 * s, 17 * s, 0xFFB7C2CF, "regular", NVG_ALIGN_RIGHT);
         shadow(bx - 6 * s, by - 6 * s, bw + 12 * s, bh + 12 * s, 25 * s, .32f);
         rect(bx - 6 * s, by - 6 * s, bw + 12 * s, bh + 12 * s, 0x9C14181F);
         line(bx - 6 * s, by - 6 * s, bx + bw + 6 * s, by - 6 * s, Math.max(.6f, s), 0xFF6A7786);
         if (width >= 420) {
            nvgSave(vg); nvgIntersectScissor(vg, 0, 110 * s, bx - 14 * s, height - 190 * s);
            image("operator", -220 * s, 144 * s, 790 * s, 790 * s, .34f);
            rect(0, 550 * s, bx, 370 * s, 0x97141820);
            text("S / R", margin, 262 * s, 88 * s, 0xFFDFE8ED, "serif", 0);
            text("PERSONAL TERMINAL", margin, 302 * s, 17 * s, 0xFF00B8D9, "regular", 0);
            String[] lines = description.split("\n");
            for (int i = 0; i < lines.length; i++) text(lines[i], margin, (644 + i * 43) * s, 28 * s, WHITE, "serif", 0);
            line(margin, 752 * s, bx - 42 * s, 752 * s, s, 0xFF75808B);
            text(ClientBranding.TERMINAL_LABEL, margin, 792 * s, 16 * s, 0xFF8E9CAA, "regular", 0);
            nvgRestore(vg);
         }
         text("TERMINAL ONLINE", margin, height - 20 * s, 15 * s, 0xFF939DA8, "regular", 0);
      } finally { nvgRestore(vg); }
   }

   private void badge(float x, float y, int c, String value) {
      nvgSave(vg);
      nvgTranslate(vg, x, y - 14);
      nvgRotate(vg, .7853982f);
      rect(-13, -13, 26, 26, c);
      outline(-17, -17, 34, 34, 2, WHITE);
      nvgRestore(vg);
      text(value, x + 27, y, 32, WHITE, "regular", 0);
   }

   private void glyph(String kind, float x, float y, float r, int c) {
      switch (kind) {
         case "single", "accounts" -> {
            disk(x, y - r * .48f, r * .33f, c);
            rounded(x - r * .62f, y, r * 1.24f, r * .9f, r * .25f, c);
            if (kind.equals("accounts")) {
               disk(x + r * .69f, y - r * .35f, r * .23f, c);
               rounded(x + r * .48f, y + r * .06f, r * .55f, r * .7f, r * .1f, c);
            }
         }
         case "multi" -> {
            outline(x - r * .75f, y - r * .9f, r * 1.5f, r * 1.85f, r * .12f, c);
            line(x - r * .44f, y + r * .03f, x - r * .04f, y + r * .43f, r * .15f, c);
            line(x - r * .04f, y + r * .43f, x + r * .53f, y - r * .34f, r * .15f, c);
         }
         case "settings", "options" -> {
            for (int i = 0; i < 8; i++) {
               nvgSave(vg); nvgTranslate(vg, x, y); nvgRotate(vg, i * .7853982f);
               rect(-r * .22f, -r, r * .44f, r * .46f, c); nvgRestore(vg);
            }
            circle(x, y, r * .54f, r * .34f, c);
         }
         case "mods", "cart" -> {
            outline(x - r * .78f, y - r * .61f, r * 1.56f, r * 1.35f, r * .16f, c);
            line(x - r * .7f, y - r * .55f, x, y - r, r * .14f, c);
            line(x, y - r, x + r * .7f, y - r * .55f, r * .14f, c);
            line(x, y - r * .55f, x, y + r * .61f, r * .12f, c);
         }
         case "warning" -> { triangle(x, y - r, x + r, y + r, x - r, y + r, c); rect(x - 2, y - 9, 4, 15, INK); rect(x - 2, y + 11, 4, 4, INK); }
         case "mail" -> {
            rect(x - r, y - r * .7f, r * 2, r * 1.4f, c);
            line(x - r * .8f, y - r * .5f, x, y + r * .1f, 2, 0xFF77797A);
            line(x, y + r * .1f, x + r * .8f, y - r * .5f, 2, 0xFF77797A);
         }
         case "calendar" -> {
            outline(x - r, y - r * .78f, r * 2, r * 1.56f, 4, c);
            line(x - r, y - r * .35f, x + r, y - r * .35f, 4, c);
            for (int i = 0; i < 6; i++) rect(x - r * .65f + (i % 3) * r * .5f, y - r * .1f + (i / 3) * r * .45f, 5, 5, c);
         }
         case "visibility" -> { circle(x, y, r * .75f, 2, c); disk(x, y, r * .25f, c); line(x - r, y + r, x + r, y - r, 2, c); }
         case "replay" -> { circle(x, y, r * .75f, 3, c); triangle(x + r, y - r, x + r, y, x, y - r, c); }
         default -> { circle(x, y + 2, r * .75f, 3, c); line(x, y - r, x, y + 1, 4, c); }
      }
   }

   int imageId(String name) {
      return images.computeIfAbsent(name, key -> {
         byte[] bytes = resources.apply("textures/launch/" + key + ".png");
         ByteBuffer keyState = MemoryUtil.memAlloc(bytes.length);
         try {
            keyState.put(bytes).flip();
            int id = nvgCreateImageMem(vg, key.equals("intro-grain") ? NVG_IMAGE_REPEATX | NVG_IMAGE_REPEATY : 0, keyState);
            if (id == 0) throw new IllegalStateException("Unable to decode launch image: " + key);
            return id;
         } finally { MemoryUtil.memFree(keyState); }
      });
   }

   Function<String, byte[]> resources() { return resources; }
   void fontBlur(float blur) { nvgFontBlur(vg, blur); }
   float textWidth(String text, float size, String face) {
      nvgFontFaceId(vg, font(face)); nvgFontSize(vg, size); nvgTextLetterSpacing(vg, 0);
      nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_BASELINE);
      return nvgTextBounds(vg, 0, 0, text, (java.nio.FloatBuffer)null);
   }

   private int font(String name) {
      return fonts.computeIfAbsent(name, key -> {
         String file = switch (key) { case "intro-regular", "intro-bold", "intro-italic" -> key; case "serif" -> "launch-serif"; case "bold" -> "googlesans-bold"; default -> "googlesans-regular"; };
         byte[] bytes = resources.apply("fonts/" + file + ".ttf");
         ByteBuffer keyState = MemoryUtil.memAlloc(bytes.length).put(bytes).flip();
         fontData.add(keyState);
         int id = nvgCreateFontMem(vg, "samsara-launch-" + key, keyState, false);
         if (id < 0) throw new IllegalStateException("Unable to load launch font: " + key);
         return id;
      });
   }

   void image(String name, float x, float y, float w, float h, float opacity) {
      nvgImagePattern(vg, x, y, w, h, 0, imageId(name), opacity, paint);
      nvgBeginPath(vg); nvgRect(vg, x, y, w, h); nvgFillPaint(vg, paint); nvgFill(vg);
   }

   private void cover(String name, float w, float h, float iw, float ih, float zoom, float alpha) {
      float scale = Math.max(w / iw, h / ih) * zoom;
      image(name, (w - iw * scale) / 2, (h - ih * scale) / 2, iw * scale, ih * scale, alpha);
   }

   void text(String text, float x, float y, float size, int c, String face, int align) {
      nvgFontFaceId(vg, font(face)); nvgFontSize(vg, size); nvgTextLetterSpacing(vg, 0);
      nvgTextAlign(vg, (align == 0 ? NVG_ALIGN_LEFT : align) | NVG_ALIGN_BASELINE);
      setColor(c); nvgFillColor(vg, color); nvgText(vg, x, y, text);
   }

   void fitText(String text, float x, float y, float size, float width, int c, String face) {
      nvgFontFaceId(vg, font(face)); nvgFontSize(vg, size);
      float measured = nvgTextBounds(vg, 0, 0, text, (java.nio.FloatBuffer)null);
      text(text, x, y, measured > width ? size * width / measured : size, c, face, 0);
   }

   private void setColor(int c) { color.r((c >> 16 & 255) / 255f).g((c >> 8 & 255) / 255f).b((c & 255) / 255f).a((c >>> 24) / 255f); }
   private static NVGColor rgba(int c, NVGColor out) { return out.r((c >> 16 & 255) / 255f).g((c >> 8 & 255) / 255f).b((c & 255) / 255f).a((c >>> 24) / 255f); }
   void shadow(float x, float y, float w, float h, float feather, float opacity) {
      nvgBoxGradient(vg, x, y + feather * .4f, w, h, 1, feather,
         rgba(ColorUtility.multiplyOpacityRounded(0xFF000000, opacity), color), rgba(0, secondColor), paint);
      nvgBeginPath(vg); nvgRect(vg, x - feather, y - feather, w + feather * 2, h + feather * 3);
      nvgFillPaint(vg, paint); nvgFill(vg);
   }
   void hollowShadow(float x, float y, float w, float h, float feather, float opacity) {
      nvgBoxGradient(vg, x, y + feather * .65f, w, h, 0, feather,
         rgba(ColorUtility.multiplyOpacityRounded(0xFF000000, opacity), color), rgba(0, secondColor), paint);
      nvgBeginPath(vg); nvgRect(vg, x - feather, y - feather, w + feather * 2, h + feather * 3);
      nvgRect(vg, x, y, w, h); nvgPathWinding(vg, NVG_HOLE);
      nvgFillPaint(vg, paint); nvgFill(vg);
   }
   void rect(float x, float y, float w, float h, int c) { rounded(x, y, w, h, 0, c); }
   private void rounded(float x, float y, float w, float h, float r, int c) { setColor(c); nvgBeginPath(vg); nvgRoundedRect(vg, x, y, w, h, r); nvgFillColor(vg, color); nvgFill(vg); }
   void outline(float x, float y, float w, float h, float stroke, int c) { setColor(c); nvgBeginPath(vg); nvgRect(vg, x, y, w, h); nvgStrokeColor(vg, color); nvgStrokeWidth(vg, stroke); nvgStroke(vg); }
   void line(float x, float y, float xx, float yy, float stroke, int c) { setColor(c); nvgBeginPath(vg); nvgMoveTo(vg, x, y); nvgLineTo(vg, xx, yy); nvgStrokeWidth(vg, stroke); nvgStrokeColor(vg, color); nvgStroke(vg); }
   private void circle(float x, float y, float r, float stroke, int c) { setColor(c); nvgBeginPath(vg); nvgCircle(vg, x, y, r); nvgStrokeWidth(vg, stroke); nvgStrokeColor(vg, color); nvgStroke(vg); }
   void disk(float x, float y, float r, int c) { setColor(c); nvgBeginPath(vg); nvgCircle(vg, x, y, r); nvgFillColor(vg, color); nvgFill(vg); }
   void triangle(float x, float y, float xx, float yy, float xxx, float yyy, int c) { setColor(c); nvgBeginPath(vg); nvgMoveTo(vg, x, y); nvgLineTo(vg, xx, yy); nvgLineTo(vg, xxx, yyy); nvgClosePath(vg); nvgFillColor(vg, color); nvgFill(vg); }

   @Override public void close() {
      images.values().forEach(id -> nvgDeleteImage(vg, id));
      images.clear();
      // NanoVG fonts reference these buffers until its context is deleted.
      // close is called during renderer shutdown, immediately before process teardown.
   }
}
