package com.samsara.ui.mainmenu.screen;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class InstalledModsScreen extends Screen {
   private final Screen parent;
   private final List<ModContainer> mods = FabricLoader.getInstance().getAllMods().stream()
      .sorted(Comparator.comparing(mod -> mod.getMetadata().getName(), String.CASE_INSENSITIVE_ORDER)).toList();
   private EditBox search;
   private int page;
   private Button previous, next;

   public InstalledModsScreen(Screen parent) { super(Component.literal("模组")); this.parent = parent; }

   @Override protected void init() {
      String query = search == null ? "" : search.getValue();
      int w = Math.min(420, width - 32), x = (width - w) / 2;
      search = new EditBox(font, x, 38, w, 20, Component.literal("搜索模组"));
      search.setHint(Component.literal("搜索模组")); search.setValue(query);
      search.setResponder(ignored -> page = 0); addRenderableWidget(search);
      previous = addRenderableWidget(Button.builder(Component.literal("<"), button -> page--).bounds(x, height - 30, 35, 20).build());
      next = addRenderableWidget(Button.builder(Component.literal(">"), button -> page++).bounds(x + w - 35, height - 30, 35, 20).build());
      addRenderableWidget(Button.builder(Component.literal("返回"), button -> onClose()).bounds(width / 2 - 50, height - 30, 100, 20).build());
   }

   @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mx, int my, float dt) {
      graphics.fill(0, 0, width, height, 0xFF202225);
      var filtered = mods.stream().filter(mod -> (mod.getMetadata().getName() + " " + mod.getMetadata().getId())
         .toLowerCase(Locale.ROOT).contains(search.getValue().toLowerCase(Locale.ROOT))).toList();
      int rows = Math.max(1, (height - 110) / 32), pages = Math.max(1, (filtered.size() + rows - 1) / rows);
      page = Math.clamp(page, 0, pages - 1);
      previous.active = page > 0; next.active = page < pages - 1;
      graphics.centeredText(font, "模组  /  " + filtered.size(), width / 2, 17, 0xFFFFFFFF);
      int x = (width - Math.min(420, width - 32)) / 2;
      for (int i = 0; i < rows && page * rows + i < filtered.size(); i++) {
         var metadata = filtered.get(page * rows + i).getMetadata();
         int y = 70 + i * 32;
         graphics.text(font, font.plainSubstrByWidth(metadata.getName(), width - x * 2), x, y, 0xFFFFFFFF);
         graphics.text(font, font.plainSubstrByWidth(metadata.getId() + "  " + metadata.getVersion().getFriendlyString(), width - x * 2), x, y + 12, 0xFFAAB4B8);
      }
      super.extractRenderState(graphics, mx, my, dt);
   }

   @Override public void onClose() { minecraft.gui.setScreen(parent); }
}
