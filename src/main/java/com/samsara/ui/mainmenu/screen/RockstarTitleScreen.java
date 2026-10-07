package com.samsara.ui.mainmenu.screen;

import com.samsara.util.render.ColorUtility;
import com.samsara.util.animation.Animation;
import com.samsara.util.animation.Easing;
import com.samsara.util.render.FontRepository;
import com.samsara.util.render.NVGRenderer;
import com.samsara.util.render.NVGTextRenderer;
import com.mojang.blaze3d.platform.InputConstants;
import com.sun.jna.Memory;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.sdl.SDLProperties;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.nanovg.NanoVG.*;

public class RockstarTitleScreen extends Screen {
   private static final int GRADIENT_TOP = 0xFF161A28;
   private static final int GRADIENT_BOTTOM = 0xFF05030C;
   private static final int WHITE = 0xFFFFFFFF;
   private static final int BUTTON_BG = 0xFF3A3A3A;
   private static final float BUTTON_SIZE = 30.0f;
   private static final float BUTTON_GAP = 6.0f;
   private static final float CLOCK_FONT = 65.0f;
   private static final float DATE_FONT = 16.0f;
   private static final float HINT_FONT = 10.0f;
   private static final float MSDF_PX_PER_EM = 64.0f;

   private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("samsara-menu");
   private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);
   private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH);

   private enum Glyph {
      SINGLE,
      MULTI,
      SETTINGS,
      QUIT
   }

   private static final class MenuButton {
      final Glyph glyph;
      final Runnable action;
      final Animation appear = new Animation(Easing.EASE_OUT_QUART, 400);
      final Animation hover = new Animation(Easing.EASE_OUT_QUART, 300);
      float x;
      float y;

      MenuButton(Glyph glyph, Runnable action) {
         this.glyph = glyph;
         this.action = action;
      }
   }

   private static final java.util.Map<Integer, double[]> msdfGlyphs = new java.util.HashMap<>();

   private static boolean wallpaperLoaded;
   private static volatile int wallpaperImage;
   private static boolean windowChromeApplied;
   private static boolean msdfLoaded;
   private static volatile int msdfImage;
   private static int msdfAtlasWidth;
   private static int msdfAtlasHeight;
   private static double mouseX;
   private static double mouseY;

   private final List<MenuButton> buttons = new ArrayList<>();
   private final Animation activeAnimation = new Animation(Easing.EASE_IN_OUT_QUART, 1000);

   private boolean active;

   public RockstarTitleScreen() {
      super(Component.literal("Main Menu"));
      this.buttons.add(new MenuButton(Glyph.SINGLE, () -> this.minecraft.gui.setScreen(new SelectWorldScreen(this))));
      this.buttons.add(new MenuButton(Glyph.MULTI, () -> this.minecraft.gui.setScreen(new JoinMultiplayerScreen(this))));
      this.buttons.add(new MenuButton(Glyph.SETTINGS, () -> this.minecraft.gui.setScreen(new OptionsScreen(this, this.minecraft.options))));
      this.buttons.add(new MenuButton(Glyph.QUIT, () -> this.minecraft.stop()));
   }

   public void renderNano() {
      this.applyWindowChrome();
      this.loadWallpaper();

      this.activeAnimation.run(this.active ? 1.0f : 0.0f);
      float progress = this.activeAnimation.getValue();
      float width = this.width;
      float height = this.height;

      NVGRenderer.roundedRectGradient(0.0f, 0.0f, width, height, 0.0f, GRADIENT_TOP, GRADIENT_BOTTOM, 90.0f);
      this.drawWallpaper(width, height, progress);

      float dim = 0.12f * (1.0f - progress);
      if (dim > 0.01f) {
         NVGRenderer.rect(0.0f, 0.0f, width, height, ColorUtility.applyOpacity(0xFF000000, dim));
      }

      NVGTextRenderer clockFont = fontWithFallback("rockstar-bold", "productsans-medium");
      NVGTextRenderer textFont = fontWithFallback("rockstar-medium", "productsans-medium");
      String date = LocalDate.now().format(DATE_FORMAT);
      String time = LocalTime.now().format(TIME_FORMAT);
      float textAlpha = 0.6f + 0.2f * progress;
      float timeOffset = lerp(height / 2.0f - 20.0f, 80.0f, progress);
      loadMsdf();

      drawCentered(textFont, date, width / 2.0f, timeOffset - 23.0f + DATE_FONT * 0.72f, DATE_FONT, ColorUtility.applyOpacity(WHITE, textAlpha * 0.92f));
      boolean clockDrawn = drawMsdfCentered(time, width / 2.0f, timeOffset, CLOCK_FONT, ColorUtility.applyOpacity(WHITE, textAlpha));
      if (!clockDrawn) {
         drawCentered(clockFont, time, width / 2.0f, timeOffset + CLOCK_FONT * 0.72f, CLOCK_FONT, ColorUtility.applyOpacity(WHITE, textAlpha));
      }

      float buttonRowY = (height > 500.0f ? height / 2.0f : height / 1.25f) - 5.0f;
      float buttonX = width / 2.0f - 69.0f;
      for (int i = 0; i < this.buttons.size(); i++) {
         MenuButton button = this.buttons.get(i);
         boolean revealed = (this.buttons.size() - i) > (1.0f - progress) * this.buttons.size() + 0.5f;
         button.appear.run(revealed ? 1.0f : 0.0f);
         button.x = buttonX;
         button.y = buttonRowY - 10.0f * button.appear.getValue();
         buttonX += BUTTON_SIZE + BUTTON_GAP;
         this.drawButton(button);
      }

      NVGRenderer.roundedRect(width / 2.0f - 36.0f, height - 5.0f - 3.0f * progress, 72.0f, 3.0f, 1.0f, ColorUtility.applyOpacity(WHITE, progress));

      String hint = "Click to continue";
      drawCentered(textFont, hint, width / 2.0f, height - 15.0f + 3.0f * progress + HINT_FONT * 0.72f, HINT_FONT, ColorUtility.applyOpacity(WHITE, 155.0f / 255.0f * (1.0f - progress)));
   }

   private void handleLeftClick() {
      if (!this.active) {
         this.active = true;
         return;
      }
      for (MenuButton button : this.buttons) {
         if (button.appear.getValue() >= 0.99f && isHovered(button.x, button.y, BUTTON_SIZE, BUTTON_SIZE)) {
            button.action.run();
            return;
         }
      }
   }

   private static void drawButton(MenuButton button) {
      float appear = button.appear.getValue();
      if (appear <= 0.01f) {
         return;
      }
      float cx = button.x + BUTTON_SIZE / 2.0f;
      float cy = button.y + BUTTON_SIZE / 2.0f;
      boolean hovered = isHovered(button.x, button.y, BUTTON_SIZE, BUTTON_SIZE) && appear >= 1.0f;
      button.hover.run(hovered ? 1.0f : 0.0f);

      float bgAlpha = 0.5f * appear + 0.2f * button.hover.getValue();
      NVGRenderer.roundedRect(button.x, button.y, BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE / 2.0f, ColorUtility.applyOpacity(BUTTON_BG, bgAlpha));
      float iconSize = button.glyph == Glyph.QUIT ? 14.0f : 12.0f;
      drawGlyph(button.glyph, cx, cy, iconSize, ColorUtility.applyOpacity(WHITE, 0.95f * appear));
   }

   private static void drawGlyph(Glyph glyph, float cx, float cy, float size, int color) {
      long vg = NVGRenderer.getContext();
      float half = size / 2.0f;
      NVGRenderer.applyColor(color, NVGRenderer.NVG_COLOR_1);
      nvgStrokeWidth(vg, Math.max(1.0f, size / 9.0f));
      nvgStrokeColor(vg, NVGRenderer.NVG_COLOR_1);
      nvgLineCap(vg, NVG_ROUND);
      switch (glyph) {
         case SINGLE -> {
            nvgBeginPath(vg);
            nvgCircle(vg, cx, cy - half * 0.55f, half * 0.55f);
            nvgStroke(vg);
            nvgBeginPath(vg);
            nvgArc(vg, cx, cy + half - 0.5f, half * 1.05f, 3.5308f, 5.8936f, NVG_CW);
            nvgStroke(vg);
         }
         case MULTI -> {
            nvgBeginPath(vg);
            nvgCircle(vg, cx, cy, half);
            nvgStroke(vg);
            nvgBeginPath(vg);
            nvgEllipse(vg, cx, cy, half * 0.45f, half);
            nvgStroke(vg);
            nvgBeginPath(vg);
            nvgMoveTo(vg, cx - half, cy);
            nvgLineTo(vg, cx + half, cy);
            nvgStroke(vg);
         }
         case SETTINGS -> {
            float toothRadius = half * 0.62f;
            for (int i = 0; i < 8; i++) {
               float angle = (float)(Math.PI / 4.0 * i);
               float dx = (float)Math.sin(angle);
               float dy = (float)-Math.cos(angle);
               nvgBeginPath(vg);
               nvgMoveTo(vg, cx + dx * toothRadius, cy + dy * toothRadius);
               nvgLineTo(vg, cx + dx * half, cy + dy * half);
               nvgStroke(vg);
            }
            nvgBeginPath(vg);
            nvgCircle(vg, cx, cy, toothRadius);
            nvgStroke(vg);
         }
         default -> {
            nvgBeginPath(vg);
            nvgArc(vg, cx, cy + 0.3f, half - 0.4f, 5.236f, 4.189f, NVG_CW);
            nvgStroke(vg);
            nvgBeginPath(vg);
            nvgMoveTo(vg, cx, cy - half);
            nvgLineTo(vg, cx, cy - 0.8f);
            nvgStroke(vg);
         }
      }
   }

   private static void drawCentered(NVGTextRenderer font, String text, float x, float baselineY, float size, int color) {
      font.drawString(text, x, baselineY, size, color, false, NVG_ALIGN_CENTER | NVG_ALIGN_BASELINE);
   }

   private static void loadMsdf() {
      if (msdfLoaded) {
         return;
      }
      msdfLoaded = true;
      try {
         var container = FabricLoader.getInstance().getModContainer("samsara");
         Path atlasPath = container.flatMap(c -> c.findPath("assets/samsara/textures/gui/roundbold.png")).orElse(null);
         Path jsonPath = container.flatMap(c -> c.findPath("assets/samsara/ui/roundbold.json")).orElse(null);
         if (atlasPath == null || jsonPath == null) {
            return;
         }
         BufferedImage atlas = ImageIO.read(Files.newInputStream(atlasPath));
         if (atlas == null) {
            return;
         }
         msdfAtlasWidth = atlas.getWidth();
         msdfAtlasHeight = atlas.getHeight();
         int[] pixels = atlas.getRGB(0, 0, msdfAtlasWidth, msdfAtlasHeight, null, 0, msdfAtlasWidth);
         int[] coverage = new int[pixels.length];
         for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            int r = pixel >> 16 & 0xFF;
            int g = pixel >> 8 & 0xFF;
            int blue = pixel & 0xFF;
            int median = Math.max(Math.min(r, g), Math.min(Math.max(r, g), blue));
            int alpha = Math.max(0, Math.min(255, (median - 128) * 8 + 128));
            coverage[i] = alpha << 24 | 0x00FFFFFF;
         }
         ByteBuffer imageBuffer = MemoryUtil.memAlloc(msdfAtlasWidth * msdfAtlasHeight * 4);
         for (int pixel : coverage) {
            imageBuffer.putInt((pixel << 16) | (pixel >>> 16));
         }
         imageBuffer.flip();
         msdfImage = nvgCreateImageMem(NVGRenderer.getContext(), 0, imageBuffer);
         MemoryUtil.memFree(imageBuffer);

         String json = Files.readString(jsonPath);
         String[] parts = json.split("\"unicode\":");
         for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            int comma = part.indexOf(',');
            if (comma <= 0) {
               continue;
            }
            try {
               int unicode = Integer.parseInt(part.substring(0, comma).trim());
               double[] glyph = new double[9];
               glyph[0] = parseJsonNumber(part, "\"advance\":", 0.0);
               double[] planeBounds = parseJsonQuad(part, "\"planeBounds\":");
               double[] atlasBounds = parseJsonQuad(part, "\"atlasBounds\":");
               if (planeBounds != null) {
                  glyph[1] = planeBounds[0];
                  glyph[2] = planeBounds[1];
                  glyph[3] = planeBounds[2];
                  glyph[4] = planeBounds[3];
               }
               if (atlasBounds != null) {
                  glyph[5] = atlasBounds[0];
                  glyph[6] = atlasBounds[1];
                  glyph[7] = atlasBounds[2];
                  glyph[8] = atlasBounds[3];
               }
               msdfGlyphs.put(unicode, glyph);
            } catch (NumberFormatException ignored) {
            }
         }
      } catch (Throwable throwable) {
         LOG.warn("Unable to load menu font atlas", throwable);
      }
   }

   private static double parseJsonNumber(String source, String key, double fallback) {
      int index = source.indexOf(key);
      if (index < 0) {
         return fallback;
      }
      int position = index + key.length();
      int start = position;
      while (position < source.length() && (Character.isDigit(source.charAt(position)) || source.charAt(position) == '-' || source.charAt(position) == '.' || source.charAt(position) == 'e')) {
         position++;
      }
      if (position == start) {
         return fallback;
      }
      return Double.parseDouble(source.substring(start, position));
   }

   private static double[] parseJsonQuad(String source, String key) {
      int index = source.indexOf(key);
      if (index < 0) {
         return null;
      }
      double[] quad = new double[4];
      int position = index + key.length();
      for (int i = 0; i < 4; i++) {
         while (position < source.length() && source.charAt(position) != '-' && source.charAt(position) != '.' && !Character.isDigit(source.charAt(position))) {
            position++;
         }
         int start = position;
         while (position < source.length() && (Character.isDigit(source.charAt(position)) || source.charAt(position) == '-' || source.charAt(position) == '.')) {
            position++;
         }
         if (position == start) {
            return null;
         }
         quad[i] = Double.parseDouble(source.substring(start, position));
      }
      return quad;
   }

   private static boolean drawMsdfCentered(String text, float centerX, float topY, float size, int color) {
      loadMsdf();
      if (msdfImage <= 0 || msdfGlyphs.isEmpty()) {
         return false;
      }
      long vg = NVGRenderer.getContext();
      float baseline = topY + 0.952f * size;
      float totalWidth = 0.0f;
      for (int i = 0; i < text.length(); i++) {
         double[] glyph = msdfGlyphs.get((int)text.charAt(i));
         if (glyph == null) {
            return false;
         }
         totalWidth += glyph[0] * size;
      }
      float penX = centerX - totalWidth / 2.0f;
      float scale = size / MSDF_PX_PER_EM;
      NVGRenderer.applyColor(color, NVGRenderer.NVG_COLOR_1);
      for (int i = 0; i < text.length(); i++) {
         double[] glyph = msdfGlyphs.get((int)text.charAt(i));
         if (glyph[5] != 0.0 || glyph[6] != 0.0 || glyph[7] != 0.0 || glyph[8] != 0.0) {
            float quadX = penX + (float)glyph[1] * size;
            float quadY = baseline - (float)glyph[4] * size;
            float quadW = (float)(glyph[3] - glyph[1]) * size;
            float quadH = (float)(glyph[4] - glyph[2]) * size;
            float atlasX = (float)glyph[5];
            float atlasY = msdfAtlasHeight - (float)glyph[8];
            float atlasW = (float)(glyph[7] - glyph[5]);
            float atlasH = (float)(glyph[8] - glyph[6]);
            try (MemoryStack stack = MemoryStack.stackPush()) {
               NVGPaint paint = NVGPaint.malloc(stack);
               nvgImagePattern(vg, quadX - atlasX * scale, quadY - atlasY * scale, msdfAtlasWidth * scale, msdfAtlasHeight * scale, 0.0f, msdfImage, 1.0f, paint);
               nvgBeginPath(vg);
               nvgRect(vg, quadX, quadY, quadW, quadH);
               nvgFillPaint(vg, paint);
               nvgFill(vg);
            }
         }
         penX += glyph[0] * size;
      }
      return true;
   }

   private static NVGTextRenderer fontWithFallback(String name, String fallback) {
      try {
         NVGTextRenderer font = FontRepository.getFont(name);
         if (font.getFontId() >= 0) {
            return font;
         }
      } catch (Throwable ignored) {
      }
      return FontRepository.getFont(fallback);
   }

   private static float lerp(float from, float to, float t) {
      return from + (to - from) * t;
   }

   private static boolean isHovered(float x, float y, float w, float h) {
      return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
   }

   private static void drawWallpaper(float width, float height, float progress) {
      if (wallpaperImage <= 0) {
         return;
      }
      long vg = NVGRenderer.getContext();
      float zoom = 1.06f - 0.06f * progress;
      float drawWidth = width * zoom;
      float drawHeight = height * zoom;
      float drawX = (width - drawWidth) / 2.0f;
      float drawY = (height - drawHeight) / 2.0f;
      try (MemoryStack stack = MemoryStack.stackPush()) {
         NVGPaint paint = NVGPaint.malloc(stack);
         nvgImagePattern(vg, drawX, drawY, drawWidth, drawHeight, 0.0f, wallpaperImage, 1.0f, paint);
         nvgBeginPath(vg);
         nvgRect(vg, drawX, drawY, drawWidth, drawHeight);
         nvgFillPaint(vg, paint);
         nvgFill(vg);
      }
   }

   private static void loadWallpaper() {
      if (wallpaperLoaded) {
         return;
      }
      wallpaperLoaded = true;
      try {
         Path image = FabricLoader.getInstance().getModContainer("samsara")
            .flatMap(container -> container.findPath("assets/samsara/textures/gui/mainmenu_background.png"))
            .orElse(null);
         if (image == null) {
            return;
         }
         byte[] bytes = Files.readAllBytes(image);
         ByteBuffer imageBuffer = MemoryUtil.memAlloc(bytes.length);
         imageBuffer.put(bytes);
         imageBuffer.flip();
         wallpaperImage = nvgCreateImageMem(NVGRenderer.getContext(), 0, imageBuffer);
         MemoryUtil.memFree(imageBuffer);
      } catch (Throwable throwable) {
         LOG.warn("Unable to load menu wallpaper", throwable);
      }
   }

   static void applyWindowChrome() {
      if (windowChromeApplied) {
         return;
      }
      windowChromeApplied = true;
      try {
         var window = net.minecraft.client.Minecraft.getInstance().getWindow();
         window.setTitle(com.samsara.ClientBranding.WINDOW_TITLE);
         if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
            return;
         }
         // Window.handle() is an SDL_Window*, never a GLFWwindow* in 26.3.
         int properties = SDLVideo.SDL_GetWindowProperties(window.handle());
         long hwnd = SDLProperties.SDL_GetPointerProperty(properties, SDLVideo.SDL_PROP_WINDOW_WIN32_HWND_POINTER, 0L);
         if (hwnd == 0L) {
            return;
         }
         try (NativeLibrary library = NativeLibrary.getInstance("dwmapi"); Memory value = new Memory(4)) {
            value.setInt(0, 1);
            for (int attribute : new int[]{20, 19}) {
               if (library.getFunction("DwmSetWindowAttribute").invokeInt(new Object[]{new Pointer(hwnd), attribute, value, 4}) == 0) {
                  break;
               }
            }
         }
      } catch (Throwable throwable) {
         LOG.warn("Unable to load menu icons", throwable);
      }
   }

   @Override
   public void mouseMoved(double mouseX, double mouseY) {
      this.mouseX = mouseX;
      this.mouseY = mouseY;
      super.mouseMoved(mouseX, mouseY);
   }

   @Override
   public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
      this.mouseX = event.x();
      this.mouseY = event.y();
      if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
         this.handleLeftClick();
         return true;
      }
      return super.mouseClicked(event, doubleClick);
   }

   @Override
   public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
      this.mouseX = mouseX;
      this.mouseY = mouseY;
      if (NVGRenderer.isAvailable()) {
         return;
      }
      renderVanillaFallback(graphics);
   }

   private void renderVanillaFallback(GuiGraphicsExtractor graphics) {
      this.activeAnimation.run(this.active ? 1.0f : 0.0f);
      graphics.fillGradient(0, 0, this.width, this.height, GRADIENT_TOP, GRADIENT_BOTTOM);
      var font = this.minecraft.font;
      float progress = this.activeAnimation.getValue();
      int timeTop = Math.round(lerp(this.height / 2.0f - 10.0f, this.height * 0.19f, progress));

      String date = LocalDate.now().format(DATE_FORMAT);
      String time = LocalTime.now().format(TIME_FORMAT);
      graphics.centeredText(font, date, this.width / 2 - font.width(date) / 2, timeTop - 12, WHITE);
      graphics.centeredText(font, time, this.width / 2 - font.width(time) / 2, timeTop, WHITE);

      int buttonX = this.width / 2 - Math.round((this.buttons.size() * (BUTTON_SIZE + BUTTON_GAP) - BUTTON_GAP) / 2.0f);
      for (int i = 0; i < this.buttons.size(); i++) {
         MenuButton button = this.buttons.get(i);
         boolean revealed = (this.buttons.size() - i) > (1.0f - progress) * this.buttons.size() + 0.5f;
         button.appear.run(revealed ? 1.0f : 0.0f);
         button.x = buttonX;
         button.y = this.height * 0.805f - BUTTON_SIZE / 2.0f;
         buttonX += Math.round(BUTTON_SIZE + BUTTON_GAP);
         int bg = ColorUtility.applyOpacity(BUTTON_BG, 0.4f * button.appear.getValue() + (isHovered(button.x, button.y, BUTTON_SIZE, BUTTON_SIZE) ? 0.2f : 0.0f));
         graphics.fill(Math.round(button.x), Math.round(button.y), Math.round(button.x + BUTTON_SIZE), Math.round(button.y + BUTTON_SIZE), bg);
         String label = switch (button.glyph) {
            case SINGLE -> "S";
            case MULTI -> "M";
            case SETTINGS -> "O";
            default -> "Q";
         };
         graphics.centeredText(font, label, Math.round(button.x + BUTTON_SIZE / 2.0f - font.width(label) / 2.0f), Math.round(button.y + BUTTON_SIZE / 2.0f - 4), WHITE);
      }

      String hint = "Click to continue";
      if (progress < 0.99f) {
         graphics.centeredText(font, hint, this.width / 2 - font.width(hint) / 2, this.height - 14, ColorUtility.applyOpacity(WHITE, 0.6f * (1.0f - progress)));
      }
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return false;
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }
}
