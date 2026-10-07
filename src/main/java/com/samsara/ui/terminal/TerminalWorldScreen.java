package com.samsara.ui.terminal;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;

public final class TerminalWorldScreen extends SelectWorldScreen implements TerminalPage {
   public TerminalWorldScreen(Screen parent) { super(parent); }
   @Override protected void init() { super.init(); TerminalTheme.arrange(this); }
   @Override protected void repositionElements() { super.repositionElements(); TerminalTheme.arrange(this); }
   @Override public void extractBackground(GuiGraphicsExtractor graphics, int x, int y, float delta) { }
   @Override public String terminalTitle() { return "单人游戏"; }
   @Override public String terminalCode() { return "01 / WORLD ARCHIVE"; }
   @Override public String terminalDescription() { return "探索与记录\n从一片方块开始。"; }
   @Override public String terminalStatus() {
      int count = children().stream().filter(AbstractSelectionList.class::isInstance)
         .map(AbstractSelectionList.class::cast).mapToInt(list -> list.children().size()).sum();
      return count + " / LOCAL RECORDS";
   }
}
