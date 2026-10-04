package com.samsara.ui.terminal;

import com.samsara.ui.account.AccountManagerScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;

public final class TerminalMultiplayerScreen extends JoinMultiplayerScreen implements TerminalPage {
   public TerminalMultiplayerScreen(Screen parent) { super(parent); }
   @Override protected void init() {
      super.init();
      addRenderableWidget(Button.builder(Component.literal("账号设置"), b -> minecraft.gui.setScreen(new AccountManagerScreen(this))).bounds(0, 0, 80, 20).build());
      TerminalTheme.arrange(this);
   }
   @Override protected void repositionElements() { super.repositionElements(); TerminalTheme.arrange(this); }
   @Override public void extractBackground(GuiGraphicsExtractor g, int x, int y, float delta) { }
   @Override public String terminalTitle() { return "多人游戏"; }
   @Override public String terminalCode() { return "02 / NETWORK TERMINAL"; }
   @Override public String terminalDescription() { return "建立连接\n在另一端相遇。"; }
   @Override public String terminalStatus() { return getServers() == null ? "SCANNING NETWORK" : getServers().size() + " / SAVED SIGNALS"; }
}
