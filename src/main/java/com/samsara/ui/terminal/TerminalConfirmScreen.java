package com.samsara.ui.terminal;

import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class TerminalConfirmScreen extends Screen implements TerminalPage {
   private final Screen parent;
   private final String detail;
   private final Consumer<Boolean> callback;
   public TerminalConfirmScreen(Screen parent, String title, String detail, Consumer<Boolean> callback) {
      super(Component.literal(title)); this.parent = parent; this.detail = detail; this.callback = callback;
   }
   @Override protected void init() {
      var bodyLayout = TerminalTheme.body(width, height);
      addRenderableWidget(Button.builder(Component.literal("确认"), button -> finish(true)).bounds(bodyLayout.x(), bodyLayout.footer(), (bodyLayout.width() - 6) / 2, 21).build());
      addRenderableWidget(Button.builder(Component.literal("取消"), button -> finish(false)).bounds(bodyLayout.x() + (bodyLayout.width() + 6) / 2, bodyLayout.footer(), (bodyLayout.width() - 6) / 2, 21).build());
   }
   private void finish(boolean value) { minecraft.gui.setScreen(parent); callback.accept(value); }
   @Override public void onClose() { finish(false); }
   @Override public void extractBackground(GuiGraphicsExtractor graphics, int x, int y, float dt) { }
   @Override public void extractRenderState(GuiGraphicsExtractor graphics, int x, int y, float dt) {
      var bodyLayout = TerminalTheme.body(width, height);
      graphics.text(font, detail, bodyLayout.x() + 12, bodyLayout.y() + 22, 0xFFE5E8ED, false);
      super.extractRenderState(graphics, x, y, dt);
   }
   @Override public String terminalTitle() { return title.getString(); }
   @Override public String terminalCode() { return "IDENTITY ARCHIVE / CONFIRM"; }
   @Override public String terminalDescription() { return "档案操作\n确认后生效。"; }
   @Override public String terminalStatus() { return "AWAITING CONFIRMATION"; }
}
