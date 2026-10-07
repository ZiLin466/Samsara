package com.samsara.ui.clickgui.opai;

import com.samsara.config.ConfigRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import static com.samsara.ui.clickgui.opai.OpaiLayout.*;
import static com.samsara.ui.clickgui.opai.OpaiContentLayout.*;
import static com.samsara.ui.clickgui.opai.OpaiIcons.Icon.*;

final class OpaiConfigPanel {
   interface Backend {
      List<String> names() throws Exception;
      void create(String name, boolean blank) throws Exception;
      void update(String name) throws Exception;
      void load(String name) throws Exception;
      void delete(String name) throws Exception;
      void openFolder() throws Exception;
   }
   enum Page { CONFIGS, ADD, SETTINGS, TARGETS }
   private static final float INSET = 8, LIST_ROW = 24, BUTTON_H = 18;
   private static final float CHIP_H = 11.75f;
   private static final float CHIP_TEXT_SIZE = 7.25f;
   private static final float CHIP_RADIUS = 3.875f;
   private static final float CHIP_RIGHT_OVERHANG = 1.125f;
   private static final float CHIP_PADDING = 3.75f;
   private static final int CHIP_BACKGROUND = 0xFF131316;

   private final OpaiTargetsPanel targets = new OpaiTargetsPanel();
   private final Backend backend;
   private final OpaiMotion height = new OpaiMotion(0, 40);
   private final OpaiMotion pageFade = new OpaiMotion(1, 25);
   private final OpaiMotion scrollMotion = new OpaiMotion(0, 20);
   private final OpaiMotion popup = new OpaiMotion(0, 18);
   private final Map<String, OpaiFeedback> feedback = new HashMap<>();
   private final Map<String, OpaiMotion> selectionAnims = new HashMap<>();
   private final List<String> names = new ArrayList<>();

   private Page page = Page.CONFIGS, outgoing;
   private String selected, input = "", message = "";
   private int cursor, selection;
   private double messageUntil;
   private float x, y, width = COL_W, scrollTarget, viewportHeight = Float.MAX_VALUE;
   private float dockRight, dockY, chipWidth = 76;
   private boolean expanded, focused;
   private float dragX, dragY, scrollbarGrab;
   private boolean dragging, draggingScrollbar, movedHeader;

   OpaiConfigPanel(Backend backend) { this.backend = backend; }

   void position(float x, float y, float width) {
      this.x = x; this.y = y; this.width = width; this.dockRight = x + width; this.dockY = y;
   }
   void dock(float viewportWidth, float top) { this.dockRight = viewportWidth; this.dockY = Math.max(4, top); }
   void collapse() {
      this.expanded = this.focused = this.dragging = this.draggingScrollbar = false;
      this.targets.close();
      this.popup.approach(0, OpaiMotion.now());
   }
   private void open() {
      this.x = Math.max(4, com.samsara.ui.hud.ClickGuiLayouts.x("opai:configs", this.dockRight - this.width - 4));
      this.y = Math.clamp(com.samsara.ui.hud.ClickGuiLayouts.y("opai:configs", this.dockY), 4, Math.max(4, this.viewportHeight - HEADER_H - 76));
      this.expanded = true;
      this.popup.approach(1, OpaiMotion.now());
      this.height.approach(pageHeight(this.page), OpaiMotion.now());
   }
   void fitViewport(int viewportWidth, int viewportHeight) {
      this.viewportHeight = viewportHeight;
      this.width = Math.min(COL_W, Math.max(80, viewportWidth - 8));
      this.x = Math.clamp(this.x, 4, Math.max(4, viewportWidth - this.width - 4));
      this.y = Math.clamp(this.y, 4, Math.max(4, viewportHeight - HEADER_H - 76));
   }
   void refresh() {
      try {
         List<String> refreshed = this.backend.names().stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
         this.names.clear();
         this.names.addAll(refreshed);
         if (!this.names.contains(this.selected)) this.selected = this.names.isEmpty() ? null : this.names.getFirst();
         this.selectionAnims.keySet().retainAll(this.names);
         for (String name : this.names) this.selectionAnims.computeIfAbsent(name, ignored -> new OpaiMotion(name.equals(this.selected) ? 1 : 0, 25));
         this.scrollTarget = Math.clamp(this.scrollTarget, 0, maximumScroll());
      } catch (Exception error) { fail(error); }
   }
   void editor(String input, int cursor, int selection) {
      this.input = input;
      this.cursor = Math.clamp(cursor, 0, input.length());
      this.selection = Math.clamp(selection, 0, input.length());
   }
   boolean focused() { return this.focused && this.page == Page.ADD && this.expanded; }
   void blur() { this.focused = false; }
   boolean adding() { return this.page == Page.ADD; }
   String input() { return this.input; }
   int cursorAt(OpaiSurface surface, double mouseX) {
      mouseX = inputX(mouseX);
      float inner = this.width - INSET * 2;
      float offset = Math.max(0, surface.textWidth(this.input.substring(0, this.cursor), 8.3f) - inner + 14);
      float desired = (float)mouseX - this.x - INSET - 6 + offset;
      int best = 0;
      float distance = Float.MAX_VALUE;
      for (int index = 0; index <= this.input.length(); index++) {
         if (index > 0 && index < this.input.length() && Character.isLowSurrogate(this.input.charAt(index))) continue;
         float delta = Math.abs(surface.textWidth(this.input.substring(0, index), 8.3f) - desired);
         if (delta < distance) { distance = delta; best = index; }
      }
      return best;
   }
   String selected() { return this.selected; }
   List<String> names() { return List.copyOf(this.names); }
   boolean expanded() { return this.expanded; }
   boolean captured() { return this.dragging || this.draggingScrollbar; }
   float bodyHeight() { return this.height.value(); }

   void update(double now) {
      this.targets.update(Math.max(86, this.viewportHeight - this.y - HEADER_H - 8), now);
      this.popup.approach(this.expanded ? 1 : 0, now);
      this.height.approach(this.expanded ? pageHeight(this.page) : 0, now);
      this.pageFade.approach(1, now);
      this.scrollTarget = Math.clamp(this.scrollTarget, 0, maximumScroll());
      this.scrollMotion.approach(this.scrollTarget, now);
      if (this.pageFade.value() == 1) this.outgoing = null;
      for (var entry : this.selectionAnims.entrySet()) entry.getValue().approach(entry.getKey().equals(this.selected) ? 1 : 0, now);
   }
   private float listHeight() {
      float available = Math.max(24, this.viewportHeight - this.y - HEADER_H - 62);
      return Math.min(Math.min(144, available), Math.max(24, this.names.size() * LIST_ROW));
   }
   private float maximumScroll() { return Math.max(0, this.names.size() * LIST_ROW - listHeight()); }
   private float pageHeight(Page page) {
      return switch (page) {
         case CONFIGS -> 3 + listHeight() + 5 + BUTTON_H * 2 + 2 + 8;
         case ADD -> 3 + 21 + 7 + BUTTON_H * 2 + 2 + 8;
         case SETTINGS -> 5 * ROW_H + 4;
         case TARGETS -> this.targets.height();
      };
   }
   private void switchPage(Page page) {
      if (this.page == page) return;
      this.outgoing = this.page;
      this.page = page;
      if (page != Page.TARGETS) this.targets.close();
      this.pageFade.snap(0, OpaiMotion.now());
      this.focused = false;
      this.message = "";
   }

   boolean contains(double mouseX, double mouseY) {
      if (!this.expanded && chip().contains(mouseX, mouseY)) return true;
      float scale = popupScale();
      return this.popup.value() > .005f && new Rect(this.x + this.width * (1 - scale), this.y,
         this.width * scale, (HEADER_H + this.height.value()) * scale).contains(mouseX, mouseY);
   }
   private Rect chip() { return new Rect(this.dockRight + CHIP_RIGHT_OVERHANG - this.chipWidth, this.dockY + .125f, this.chipWidth, CHIP_H); }
   private float popupScale() {
      float t = this.popup.value();
      return .86f + .14f * t + .07f * (float)Math.sin(Math.PI * t);
   }
   private double inputX(double mouseX) { return this.x + this.width + (mouseX - this.x - this.width) / popupScale(); }
   private double inputY(double mouseY) { return this.y + (mouseY - this.y) / popupScale(); }
   void release() {
      boolean close = this.dragging && !this.movedHeader;
      this.dragging = this.draggingScrollbar = false;
      if (close) collapse();
   }
   void drag(double mouseX, double mouseY, int viewportWidth, int viewportHeight) {
      if (this.dragging) {
         if (Math.abs(mouseX - this.x - this.dragX) + Math.abs(mouseY - this.y - this.dragY) <= 3 && !this.movedHeader) return;
         this.movedHeader = true;
         this.x = Math.clamp((float)mouseX - this.dragX, 4, Math.max(4, viewportWidth - this.width - 4));
         this.y = Math.clamp((float)mouseY - this.dragY, 4, Math.max(4, viewportHeight - HEADER_H - (this.expanded ? 76 : 8)));
         com.samsara.ui.hud.ClickGuiLayouts.put("opai:configs", this.x, this.y);
      } else if (this.draggingScrollbar) {
         mouseY = inputY(mouseY);
         float visible = listHeight(), track = visible - 4;
         float thumb = Math.min(track, Math.max(12, track * visible / Math.max(1, this.names.size() * LIST_ROW)));
         float travel = track - thumb;
         float top = (float)mouseY - this.y - HEADER_H - 3 - this.scrollbarGrab - 2;
         this.scrollTarget = travel <= 0 ? 0 : Math.clamp(top / travel, 0, 1) * maximumScroll();
         this.scrollMotion.snap(this.scrollTarget, OpaiMotion.now());
      }
   }
   boolean wheel(double amount) {
      if (this.expanded && this.page == Page.TARGETS) return this.targets.wheel(amount);
      if (!this.expanded || this.page != Page.CONFIGS) return true;
      this.scrollTarget = Math.clamp(this.scrollTarget - (float)amount * LIST_ROW * 2, 0, maximumScroll());
      return true;
   }
   boolean escape() {
      if (this.page == Page.CONFIGS) {
         if (!this.expanded) return false;
         collapse(); return true;
      }
      switchPage(this.page == Page.TARGETS ? Page.SETTINGS : Page.CONFIGS);
      return true;
   }

   boolean click(double mouseX, double mouseY, int button) {
      if (!contains(mouseX, mouseY)) { this.focused = false; return false; }
      if (!this.expanded) {
         if (chip().contains(mouseX, mouseY) && (button == 0 || button == 1)) {
            Rect chip = chip();
            feedback("chip").click((float)mouseX - chip.x(), (float)mouseY - chip.y(), chip.width(), CHIP_H, OpaiMotion.now());
            open();
         }
         return true;
      }
      double rawX = mouseX, rawY = mouseY;
      mouseX = inputX(mouseX); mouseY = inputY(mouseY);
      float localX = (float)mouseX - this.x, localY = (float)mouseY - this.y;
      if (localY < HEADER_H) {
         if (button == 1) {
            collapse();
         } else if (button == 0) {
            if (this.page != Page.CONFIGS && localX < 20) { headerClick(BACK); switchPage(this.page == Page.TARGETS ? Page.SETTINGS : Page.CONFIGS); }
            else if (this.page == Page.CONFIGS && localX >= this.width - 39 && localX < this.width - 23) {
               headerClick(PLUS); switchPage(Page.ADD);
            } else if (this.page == Page.CONFIGS && localX >= this.width - 22) {
               headerClick(GEAR); switchPage(Page.SETTINGS);
            } else { this.dragging = true; this.movedHeader = false; this.dragX = (float)rawX - this.x; this.dragY = (float)rawY - this.y; }
         }
         return true;
      }
      if (!this.expanded || button != 0) return true;
      float by = localY - HEADER_H;
      if (this.page == Page.CONFIGS) {
         if (by >= 3 && by < 3 + listHeight() && localX >= INSET && localX < this.width - INSET) {
            if (maximumScroll() > 0 && localX > this.width - INSET - 6) {
               float track = listHeight() - 4;
               float thumb = Math.min(track, Math.max(12, track * listHeight() / (this.names.size() * LIST_ROW)));
               float top = 2 + (track - thumb) * this.scrollMotion.value() / maximumScroll();
               float clicked = by - 3;
               this.scrollbarGrab = clicked >= top && clicked < top + thumb ? clicked - top : thumb / 2;
               this.draggingScrollbar = true;
            } else {
               int index = (int)((by - 3 + this.scrollMotion.value()) / LIST_ROW);
               if (index >= 0 && index < this.names.size()) {
                  this.selected = this.names.get(index);
                  feedback("row:" + this.selected).click(localX - INSET, (by - 3 + this.scrollMotion.value()) % LIST_ROW,
                     this.width - INSET * 2, LIST_ROW, OpaiMotion.now());
                  update(OpaiMotion.now());
               }
            }
         } else {
            float controlsY = 3 + listHeight() + 5;
            if (by >= controlsY && by < controlsY + BUTTON_H) {
               float gap = 6, cell = (this.width - INSET * 2 - gap * 3) / 4;
               for (int i = 0; i < 4; i++) if (localX >= INSET + i * (cell + gap) && localX < INSET + i * (cell + gap) + cell) {
                  clickButton(new String[]{"REFRESH", "SAVE", "LOAD", "DELETE"}[i], localX - INSET - i * (cell + gap), by - controlsY, cell);
                  action(i); break;
               }
            } else if (by >= controlsY + BUTTON_H + 2 && by < controlsY + BUTTON_H * 2 + 2
                       && localX >= INSET && localX < this.width / 2 - 3) {
               clickButton("Folder", localX - INSET, by - controlsY - BUTTON_H - 2, (this.width - INSET * 2 - 6) / 2);
               try { this.backend.openFolder(); } catch (Exception error) { fail(error); }
            } else if (by >= controlsY + BUTTON_H + 2 && by < controlsY + BUTTON_H * 2 + 2
                       && localX >= this.width / 2 + 3 && localX < this.width - INSET) {
               clickButton("Upload", localX - this.width / 2 - 3, by - controlsY - BUTTON_H - 2, (this.width - INSET * 2 - 6) / 2);
            }
            // Upload is a visual placeholder. It never performs a network request.
         }
      } else if (this.page == Page.ADD) {
         this.focused = by >= 3 && by < 24 && localX >= INSET && localX < this.width - INSET;
         if (localX >= INSET && localX < this.width - INSET) {
            if (by >= 31 && by < 31 + BUTTON_H) { clickButton("Create", localX - INSET, by - 31, this.width - INSET * 2); create(false); }
            else if (by >= 51 && by < 51 + BUTTON_H) { clickButton("Create Blank", localX - INSET, by - 51, this.width - INSET * 2); create(true); }
         }
      } else if (this.page == Page.TARGETS) this.targets.click(localX, by, this.width);
      else if (by >= 0 && by < ROW_H) switchPage(Page.CONFIGS);
      else if (by >= ROW_H * 3 && by < ROW_H * 4) switchPage(Page.TARGETS);
      // Other Settings rows are reserved.
      return true;
   }
   private OpaiFeedback feedback(String key) { return this.feedback.computeIfAbsent(key, ignored -> new OpaiFeedback()); }
   private void clickButton(String key, float x, float y, float width) { feedback(key).click(x, y, width, BUTTON_H, OpaiMotion.now()); }
   private void headerClick(OpaiIcons.Icon icon) { feedback("header:" + icon).click(5, 5, 10, 10, OpaiMotion.now()); }
   private void action(int index) {
      try {
         if (index == 0) { refresh(); return; }
         if (this.selected == null) return;
         switch (index) {
            case 1 -> this.backend.update(this.selected);
            case 2 -> this.backend.load(this.selected);
            case 3 -> this.backend.delete(this.selected);
            default -> { }
         }
         refresh();
      } catch (Exception error) { fail(error); }
   }
   void create(boolean blank) {
      try {
         String name = ConfigRepository.validName(this.input);
         if (this.names.stream().anyMatch(item -> item.equalsIgnoreCase(name))) throw new IllegalArgumentException("Name already exists");
         this.backend.create(name, blank);
         refresh();
         this.selected = name;
         switchPage(Page.CONFIGS);
         float top = this.names.indexOf(name) * LIST_ROW;
         this.scrollTarget = Math.clamp(top + LIST_ROW - listHeight(), 0, maximumScroll());
      } catch (Exception error) { fail(error); }
   }
   private void fail(Exception error) {
      this.message = error.getMessage() == null ? "Configuration failed" : error.getMessage();
      this.messageUntil = OpaiMotion.now() + 3500;
   }

   void paint(OpaiSurface surface, OpaiStyle.Palette palette, double mouseX, double mouseY, double now) {
      this.chipWidth = Math.min(this.dockRight, surface.plainTextWidth("Configurations", CHIP_TEXT_SIZE) + CHIP_PADDING);
      float opacity = this.popup.value();
      if (opacity < 1) surface.opacity((float)Math.pow(1 - opacity, 4), () -> {
         Rect chip = chip();
         surface.rounded(chip.x(), chip.y(), chip.width(), CHIP_H, CHIP_RADIUS, CHIP_BACKGROUND);
         feedback("chip").paint(surface, chip.x(), chip.y(), chip.width(), CHIP_H, CHIP_RADIUS, palette.accent(), now);
         surface.plainText("Configurations", chip.x() + 2, chip.y() + CHIP_H / 2, CHIP_TEXT_SIZE, chip.width() - CHIP_PADDING, palette.text());
      });
      if (opacity <= .005f) return;
      double mx = inputX(mouseX), my = inputY(mouseY);
      surface.opacity(opacity, () -> surface.scale(popupScale(), this.x + this.width, this.y, () -> paintPopup(surface, palette, mx, my, now)));
   }

   private void paintPopup(OpaiSurface surface, OpaiStyle.Palette palette, double mouseX, double mouseY, double now) {
      float body = this.height.value();
      if (body > .01f) surface.panel(this.x, this.y + HEADER_H, this.width, body, false, palette.body());
      if (body > .5f) surface.panel(this.x, this.y, this.width, HEADER_H, true, palette.header());
      else surface.rounded(this.x, this.y, this.width, HEADER_H, RADIUS, palette.header());
      String title = this.page == Page.ADD ? "Add" : this.page == Page.SETTINGS ? "Settings" : this.page == Page.TARGETS ? "Targets" : "Configurations";
      float titleX = this.x + TEXT_PAD;
      if (this.page != Page.CONFIGS) { headerIcon(surface, palette, BACK, this.x + 8, 8, mouseX, mouseY, now); titleX += 10; }
      surface.text(title, titleX, this.y + HEADER_H / 2, OpaiStyle.HEADER_TEXT_SIZE,
         this.width - (this.page == Page.CONFIGS ? 48 : 30), palette.text());
      if (this.page == Page.CONFIGS) {
         headerIcon(surface, palette, PLUS, this.x + this.width - 37, 10, mouseX, mouseY, now);
         headerIcon(surface, palette, GEAR, this.x + this.width - 21, 10, mouseX, mouseY, now);
      }
      if (body <= .01f) return;
      surface.clip(this.x, this.y + HEADER_H, this.width, body, () -> {
         if (this.outgoing != null) surface.opacity(1 - this.pageFade.value(), () -> paintPage(surface, palette, this.outgoing, mouseX, mouseY));
         surface.opacity(this.pageFade.value(), () -> paintPage(surface, palette, this.page, mouseX, mouseY));
      });
      if (!this.message.isEmpty() && now < this.messageUntil) {
         surface.rounded(this.x + 3, this.y + HEADER_H + body + 3, this.width - 6, 15, 3, palette.field());
         surface.text(this.message, this.x + 7, this.y + HEADER_H + body + 10.5f, 7, this.width - 14, 0xFFFF9999);
      }
   }
   private void headerIcon(OpaiSurface surface, OpaiStyle.Palette palette, OpaiIcons.Icon icon, float x, float size,
                           double mx, double my, double now) {
      OpaiFeedback ink = feedback("header:" + icon);
      float hover = ink.hover(new Rect(x - 3, this.y + 3, size + 6, 16).contains(mx, my), now);
      surface.scale(1 - .12f * ink.press(now), x + size / 2, this.y + 6 + size / 2, () ->
         surface.icon(icon, x, this.y + 6, size, OpaiSurface.mix(palette.fieldLine(), palette.accent(), hover)));
   }
   private void paintPage(OpaiSurface surface, OpaiStyle.Palette p, Page page, double mx, double my) {
      float left = this.x + INSET, top = this.y + HEADER_H + 3, inner = this.width - INSET * 2;
      if (page == Page.CONFIGS) {
         surface.rounded(left, top, inner, listHeight(), 8, OpaiSurface.mix(p.field(), 0xFF202020, .45f));
         surface.clip(left, top, inner, listHeight(), () -> {
            if (this.names.isEmpty()) surface.text("No configurations", left + 6, top + 12, 8.3f, inner - 12, p.fieldLine());
            else for (int i = 0; i < this.names.size(); i++) {
               String name = this.names.get(i);
               float rowY = top + LIST_ROW * i - this.scrollMotion.value();
               if (rowY + LIST_ROW <= top || rowY >= top + listHeight()) continue;
               float selected = this.selectionAnims.get(name).value();
               int highlight = (p.accent() & 0xFFFFFF) | Math.round(255 * selected) << 24;
               if (selected > 0) {
                  surface.rounded(left, rowY, inner, LIST_ROW, i == 0 || i == this.names.size() - 1 ? 8 : 0, highlight);
                  if (i == 0 && this.names.size() > 1) surface.rect(left, rowY + 8, inner, LIST_ROW - 8, highlight);
                  else if (i == this.names.size() - 1 && i > 0) surface.rect(left, rowY, inner, LIST_ROW - 8, highlight);
               } else if (new Rect(left, rowY, inner, LIST_ROW).contains(mx, my)) surface.rect(left, rowY, inner, LIST_ROW, p.hover());
               feedback("row:" + name).paint(surface, left, rowY, inner, LIST_ROW, 0, p.accent(), OpaiMotion.now());
               surface.text(name + ".json", left + 6, rowY + LIST_ROW / 2, 8.3f, inner - 12,
                  OpaiSurface.mix(p.text(), p.enabledText(), selected));
            }
            if (maximumScroll() > 0) {
               float track = listHeight() - 4, thumb = Math.min(track, Math.max(12, track * listHeight() / (this.names.size() * LIST_ROW)));
               surface.rounded(left + inner - 3, top + 2 + (track - thumb) * this.scrollMotion.value() / maximumScroll(), 2, thumb, 1, p.scrollbar());
            }
         });
         float y = top + listHeight() + 5, gap = 6, cell = (inner - gap * 3) / 4;
         OpaiIcons.Icon[] icons = {REFRESH, SAVE, LOAD, DELETE};
         for (int i = 0; i < 4; i++) button(surface, p, left + i * (cell + gap), y, cell, "", icons[i], i == 0 || this.selected != null, mx, my);
         float half = (inner - gap) / 2;
         button(surface, p, left, y + BUTTON_H + 2, half, "Folder", FOLDER, true, mx, my);
         button(surface, p, left + half + gap, y + BUTTON_H + 2, half, "Upload", UPLOAD, true, mx, my);
      } else if (page == Page.ADD) {
         surface.rounded(left, top, inner, 21, 1.5f, this.focused ? p.accent() : p.toggleOutline());
         surface.rounded(left + .8f, top + .8f, inner - 1.6f, 19.4f, 1, 0xFF111014);
         if (this.focused) {
            surface.rect(left + 3, top - 2, 19, 5, p.header());
            surface.text("Name", left + 4, top + .5f, 5.5f, 19, p.accent());
         }
         float cursorWidth = surface.textWidth(this.input.substring(0, this.cursor), 8.3f);
         float offset = Math.max(0, cursorWidth - inner + 14);
         surface.clip(left + 4, top + 2, inner - 8, 17, () -> {
            float tx = left + 6 - offset;
            if (this.focused && this.cursor != this.selection) {
               float selectionStartX = surface.textWidth(this.input.substring(0, Math.min(this.cursor, this.selection)), 8.3f);
               float selectionEndX = surface.textWidth(this.input.substring(0, Math.max(this.cursor, this.selection)), 8.3f);
               surface.rect(tx + selectionStartX, top + 3, selectionEndX - selectionStartX, 15, 0x665C6399);
            }
            surface.text(this.input.isEmpty() && !this.focused ? "Name" : this.input, tx, top + 10.5f, 8.3f,
               Math.max(inner, surface.textWidth(this.input, 8.3f) + 1), p.text());
            if (this.focused && ((long)OpaiMotion.now() / 500) % 2 == 0) surface.rect(tx + cursorWidth, top + 4, .7f, 13, p.text());
         });
         button(surface, p, left, top + 28, inner, "Create", PLUS, true, mx, my);
         button(surface, p, left, top + 48, inner, "Create Blank", PLUS, true, mx, my);
      } else if (page == Page.TARGETS) {
         this.targets.paint(surface, p, this.x, this.y + HEADER_H, this.width);
      } else {
         String[] items = {"Configurations", "Chat Translate", "Module Tweaks", "Targets", "Misc"};
         for (int i = 0; i < items.length; i++) {
            surface.text(items[i], this.x + TEXT_PAD, this.y + HEADER_H + ROW_H * (i + .5f), 8.3f, this.width - TEXT_PAD * 2, p.text());
         }
      }
   }
   private void button(OpaiSurface surface, OpaiStyle.Palette p, float x, float y, float w, String label,
                       OpaiIcons.Icon icon, boolean active, double mx, double my) {
      double now = OpaiMotion.now();
      OpaiFeedback ink = feedback(label.isEmpty() ? icon.name() : label);
      float hover = ink.hover(new Rect(x, y, w, BUTTON_H).contains(mx, my) && active, now);
      surface.scale(1 - .06f * ink.press(now), x + w / 2, y + BUTTON_H / 2,
         () -> paintButton(surface, p, x, y, w, label, icon, active, ink, hover, now));
   }
   private void paintButton(OpaiSurface surface, OpaiStyle.Palette p, float x, float y, float w, String label,
                            OpaiIcons.Icon icon, boolean active, OpaiFeedback ink, float hover, double now) {
      int fill = OpaiSurface.mix(p.field(), p.accent(), .15f + .10f * hover);
      surface.rounded(x, y, w, BUTTON_H, BUTTON_H / 2, fill);
      ink.paint(surface, x, y, w, BUTTON_H, BUTTON_H / 2, p.accent(), now);
      float textW = label.isEmpty() ? 0 : surface.textWidth(label, 8.3f), glyph = 9;
      float left = x + (w - textW - glyph - (label.isEmpty() ? 0 : 5)) / 2;
      surface.opacity(active ? 1 : .4f, () -> {
         surface.icon(icon, left, y + (BUTTON_H - glyph) / 2, glyph, p.text());
         if (!label.isEmpty()) surface.text(label, left + glyph + 5, y + BUTTON_H / 2, 8.3f, textW + 1, p.text());
      });
   }
}
