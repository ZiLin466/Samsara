package com.samsara.ui.terminal;

import com.samsara.util.animation.HoverMotion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class TerminalTheme {
   public record Body(int x, int y, int width, int height, int footer) { }
   private TerminalTheme() { }
   public static boolean active() { return Minecraft.getInstance().gui.screen() instanceof TerminalPage; }
   public static boolean reduced() { return Minecraft.getInstance().options.screenEffectScale().get() == 0; }
   public static Component label(String value) {
      return label(value, "terminal");
   }
   private static Component label(String value, String face) {
      return Component.literal(value).withStyle(net.minecraft.network.chat.Style.EMPTY.withFont(
         new net.minecraft.network.chat.FontDescription.Resource(net.minecraft.resources.Identifier.fromNamespaceAndPath("samsara", face))));
   }

   public static Body body(int width, int height) {
      int x = width >= 420 ? Math.round(width * .32f) : 12;
      int y = Math.max(48, Math.round(height * .22f));
      int footer = height - 62;
      return new Body(x, y, width - x - 16, Math.max(24, footer - y - 14), footer);
   }

   public static void arrange(Screen screen) {
      Body bodyLayout = body(screen.width, screen.height);
      var buttons = screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).toList();
      int columns = Math.max(1, (buttons.size() + 1) / 2);
      int bw = (bodyLayout.width - (columns - 1) * 5) / columns;
      for (int i = 0; i < buttons.size(); i++) {
         buttons.get(i).setRectangle(bodyLayout.x + i % columns * (bw + 5), bodyLayout.footer + i / columns * 26, bw, 21);
      }
      for (var child : screen.children()) {
         if (child instanceof StringWidget text) text.visible = false;
         if (child instanceof AbstractSelectionList<?> list) list.updateSizeAndPosition(bodyLayout.width, bodyLayout.height, bodyLayout.x, bodyLayout.y);
         if (child instanceof EditBox edit) {
            edit.setRectangle(bodyLayout.x, bodyLayout.y - 27, bodyLayout.width, 21);
            input(edit);
         }
      }
   }

   public static void input(EditBox edit) {
      edit.setBordered(false); edit.setTextShadow(false);
      edit.setTextColor(0xFFE5E8ED); edit.setTextColorUneditable(0xFF737985);
   }

   public static void background(GuiGraphicsExtractor graphics, Screen screen, TerminalPage page) {
      com.samsara.ui.mainmenu.background.MenuBackground.render(graphics, screen.width, screen.height);
      int w = screen.width, h = screen.height;
      Body bodyLayout = body(w, h); var font = Minecraft.getInstance().font;
      graphics.fill(0, 0, w, h, 0xA5101720);
      var code = label(page.terminalCode(), "terminal-display");
      int codeWidth = Math.min(font.width(code), Math.max(1, (w - 36) * 2 / 5));
      displayText(graphics, label("SAMSARA / TERMINAL SERVICE", "terminal-display"), 12, 12,
         Math.max(1, w - codeWidth - 36), 1, 0xFFE5E9ED);
      displayText(graphics, code, w - codeWidth - 12, 12, codeWidth, 1, 0xFF00BCD4);
      graphics.horizontalLine(12, w - 12, 30, 0xFF52616D);
      graphics.pose().pushMatrix(); graphics.pose().translate(bodyLayout.x(), Math.max(34, bodyLayout.y() - 26)); graphics.pose().scale(1.6f, 1.6f);
      graphics.text(font, label(page.terminalTitle().split(" / ")[0]), 0, 0, 0xFFFFFFFF, false); graphics.pose().popMatrix();
      graphics.fill(bodyLayout.x() - 6, bodyLayout.y() - 5, bodyLayout.x() + bodyLayout.width() + 5, bodyLayout.footer() + 25, 0xD0161D26);
      if (w >= 420) {
         displayText(graphics, label("S / R", "terminal-display"), 14, Math.max(58, (int)(h * .27f)), bodyLayout.x() - 28, 2, 0xFFE5EBEF);
         displayText(graphics, label("PERSONAL TERMINAL", "terminal-display"), 14,
            Math.max(83, (int)(h * .27f) + 25), bodyLayout.x() - 28, 1, 0xFF00BCD4);
         int descriptionY = Math.max(111, h - 95);
         for (var line : font.split(label(page.terminalDescription()), Math.max(30, bodyLayout.x() - 27))) {
            graphics.text(font, line, 14, descriptionY, 0xFFCED8E1, false);
            descriptionY += 14;
         }
      }
      var status = label(page.terminalStatus(), "terminal-google");
      var fittedStatus = label(font.substrByWidth(status, w - 24).getString(), "terminal-google");
      graphics.text(font, fittedStatus, 12, h - 14, 0xFF91A2B1, false);
   }

   /** Agibot's wide, low capitals need a larger face; fit its original proportions to each region. */
   private static void displayText(GuiGraphicsExtractor graphics, Component text, int x, int y, int width, float scale, int color) {
      var font = Minecraft.getInstance().font;
      float fittedScale = Math.min(scale, Math.max(1, width) / (float)Math.max(1, font.width(text)));
      graphics.pose().pushMatrix();
      graphics.pose().translate(x, y); graphics.pose().scale(fittedScale, fittedScale);
      graphics.text(font, text, 0, 0, color, false);
      graphics.pose().popMatrix();
   }

   public static void button(GuiGraphicsExtractor graphics, AbstractWidget button, HoverMotion motion) {
      var mc = Minecraft.getInstance();
      boolean hover = button.active && button.isHovered();
      float p = motion.update(hover, System.nanoTime(), reduced());
      float cx = button.getX() + button.getWidth() / 2f, cy = button.getY() + button.getHeight() / 2f;
      graphics.pose().pushMatrix();
      graphics.pose().translate(cx, cy - (reduced() ? 0 : 1.5f * p));
      float scale = 1 + (reduced() ? 0 : .025f * p);
      graphics.pose().scale(scale, scale); graphics.pose().translate(-cx, -cy);
      int x = button.getX(), y = button.getY(), w = button.getWidth(), h = button.getHeight();
      for (int i = 5; i >= 1; i--) {
         int glowAlpha = Math.round((5 + p * 9) * (6 - i) / 5);
         graphics.fill(x - i, y + 2, x + w + i, y + h + i + 2, glowAlpha << 24);
      }
      graphics.fill(x, y, x + w, y + h, button.active ? 0xFFF0F0EB : 0xC9363941);
      graphics.fill(x, y, x + 2, y + h, hover || button.isFocused() ? 0xFF00B8D9 : 0xFF808A95);
      if (button.isFocused()) graphics.outline(x, y, w, h, 0xFF00B8D9);
      String value = button.getMessage().getString();
      Component label = label(value);
      while (value.length() > 1 && mc.font.width(label) > w - 9) { value = value.substring(0, value.offsetByCodePoints(value.length(), -1)); label = label(value); }
      graphics.text(mc.font, label, x + (w - mc.font.width(label)) / 2, y + (h - 11) / 2,
         button.active ? 0xFF17191E : 0xFF858D99, false);
      graphics.pose().popMatrix();
   }
}
