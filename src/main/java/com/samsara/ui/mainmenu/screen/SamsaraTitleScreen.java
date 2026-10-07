package com.samsara.ui.mainmenu.screen;

import com.samsara.ui.mainmenu.launch.LaunchLayout;
import com.samsara.ui.mainmenu.launch.LaunchResources;
import com.samsara.ui.mainmenu.launch.LaunchTimeline;
import com.samsara.ui.NanoGui;
import com.samsara.ui.account.AccountManagerScreen;
import com.samsara.ui.account.AccountSessions;
import com.samsara.util.render.NVGRenderer;
import com.mojang.blaze3d.platform.InputConstants;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class SamsaraTitleScreen extends Screen implements NanoGui {
   private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
   private static boolean introSeen;
   private final LaunchTimeline timeline;
   private double time;
   private double mouseX = -1, mouseY = -1;
   private boolean showOperator = true;
   private int focus = -1;

   public SamsaraTitleScreen() {
      super(Component.literal("Samsara"));
      timeline = new LaunchTimeline(introSeen);
   }

   @Override protected void init() {
      RockstarTitleScreen.applyWindowChrome();
      if (!NVGRenderer.isAvailable()) {
         for (int i = 0; i < 5; i++) {
            var tile = LaunchLayout.ACTIONS.get(i);
            addRenderableWidget(Button.builder(Component.literal(tile.label()), button -> activate(tile.id()))
               .bounds(width / 2 - 100, Math.max(30, height / 2 - 68) + i * 24, 200, 20).build());
         }
         addRenderableWidget(Button.builder(Component.literal("退出游戏"), button -> minecraft.stop())
            .bounds(width / 2 - 100, Math.max(30, height / 2 - 68) + 120, 200, 20).build());
      }
   }

   private boolean reduceMotion() { return minecraft.options.screenEffectScale().get() == 0; }

   @Override public void renderNano() {
      if (minecraft.gui.overlay() != null) return;
      AccountSessions.captureLauncher();
      time = timeline.time(System.nanoTime(), false);
      if (time >= LaunchTimeline.DURATION) introSeen = true;
      var hover = LaunchLayout.of(width, height).hit(mouseX, mouseY);
      LaunchResources.renderer().render(width, height, time, minecraft.getUser().getName(), LocalDateTime.now().format(DATE),
         hover == null ? "" : hover.id(), focus < 0 ? "" : LaunchLayout.ACTIONS.get(focus).id(), showOperator, reduceMotion());
   }

   @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mx, int my, float delta) {
      mouseX = mx; mouseY = my;
      if (!NVGRenderer.isAvailable()) {
         time = timeline.time(System.nanoTime(), minecraft.gui.overlay() != null);
         if (LaunchTimeline.interactive(time)) introSeen = true;
         graphics.fill(0, 0, width, height, 0xFF202225);
         graphics.centeredText(font, com.samsara.ClientBranding.WINDOW_TITLE, width / 2, 15, 0xFFFFFFFF);
         if (LaunchTimeline.interactive(time)) super.extractRenderState(graphics, mx, my, delta);
         else graphics.centeredText(font, "INITIALIZING TERMINAL", width / 2, height / 2, 0xFFFFFFFF);
      }
   }

   @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      if (!LaunchTimeline.interactive(time)) return true;
      if (!NVGRenderer.isAvailable()) return super.mouseClicked(event, doubleClick);
      if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || minecraft.gui.overlay() != null) return false;
      var hit = LaunchLayout.of(width, height).hit(event.x(), event.y());
      if (hit != null) { activate(hit.id()); return true; }
      return false;
   }

   @Override public boolean keyPressed(KeyEvent event) {
      if (!LaunchTimeline.interactive(time)) return true;
      if (!NVGRenderer.isAvailable()) return super.keyPressed(event);
      if (minecraft.gui.overlay() != null) return false;
      if (event.isCycleFocus()) {
         moveFocus(event.hasShiftDown() ? -1 : 1); return true;
      }
      if (event.isRight() || event.isDown()) {
         moveFocus(1); return true;
      }
      if (event.isLeft() || event.isUp()) {
         moveFocus(-1); return true;
      }
      if ((event.isConfirmation() || event.key() == InputConstants.KEY_SPACE) && focus >= 0) {
         activate(LaunchLayout.ACTIONS.get(focus).id()); return true;
      }
      return super.keyPressed(event);
   }

   private void moveFocus(int direction) {
      focus = focus < 0 ? (direction > 0 ? 0 : LaunchLayout.ACTIONS.size() - 1)
         : Math.floorMod(focus + direction, LaunchLayout.ACTIONS.size());
      minecraft.getNarrator().saySystemNow(Component.literal(LaunchLayout.ACTIONS.get(focus).label()));
   }

   private void activate(String id) {
      switch (id) {
         case "single" -> minecraft.gui.setScreen(new SelectWorldScreen(this));
         case "multi" -> minecraft.gui.setScreen(new JoinMultiplayerScreen(this));
         case "accounts" -> minecraft.gui.setScreen(new AccountManagerScreen(this));
         case "options", "settings" -> minecraft.gui.setScreen(new OptionsScreen(this, minecraft.options));
         case "mods" -> minecraft.gui.setScreen(new InstalledModsScreen(this));
         case "visibility" -> showOperator = !showOperator;
         case "replay" -> { introSeen = false; minecraft.gui.setScreen(new SamsaraTitleScreen()); }
         case "quit" -> minecraft.stop();
         default -> { }
      }
   }

   @Override public boolean shouldCloseOnEsc() { return false; }
   @Override public boolean isPauseScreen() { return false; }
}
