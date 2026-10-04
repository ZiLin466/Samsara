package com.samsara.ui.clickgui.opai;

import com.samsara.module.FeatureManager;
import com.samsara.setting.MultiSelectSetting;
import static com.samsara.ui.clickgui.opai.OpaiContentLayout.*;
import static com.samsara.ui.clickgui.opai.OpaiLayout.*;

final class OpaiTargetsPanel {
   private final OpaiMotion reveal = new OpaiMotion(0, 40);
   private int open = -1;
   private boolean expanded;
   private float scroll, listHeight;

   private MultiSelectSetting choices(int group) { return group == 0 ? FeatureManager.targets.combat : FeatureManager.targets.visual; }
   void close() { this.expanded = false; this.reveal.approach(0, OpaiMotion.now()); }
   void update(float available, double now) {
      this.listHeight = this.open < 0 ? 0 : Math.min(choices(this.open).options().length * OPTION_H, Math.max(OPTION_H, available - MODE_H * 2 - 6));
      this.scroll = this.open < 0 ? 0 : Math.clamp(this.scroll, 0, Math.max(0, choices(this.open).options().length * OPTION_H - this.listHeight));
      this.reveal.approach(this.expanded ? 1 : 0, now);
   }
   float height() { return MODE_H * 2 + 6 + this.listHeight * this.reveal.value(); }
   private float groupTop(int group) { return 3 + group * MODE_H + (group == 1 && this.open == 0 ? this.listHeight * this.reveal.value() : 0); }
   boolean wheel(double amount) {
      if (this.open >= 0 && this.expanded) {
         this.scroll = Math.clamp(this.scroll - (float)amount * OPTION_H * 2, 0, Math.max(0, choices(this.open).options().length * OPTION_H - this.listHeight));
      }
      return true;
   }
   void click(float x, float y, float width) {
      if (x < TEXT_PAD - 2 || x >= width - TEXT_PAD + 2) return;
      if (this.open >= 0) {
         float top = groupTop(this.open) + FIELD_Y + FIELD_H;
         if (y >= top && y < top + this.listHeight * this.reveal.value()) {
            if (this.expanded) {
               int option = (int)((y - top + this.scroll) / OPTION_H);
               if (option < choices(this.open).options().length) choices(this.open).select(option);
            }
            return;
         }
      }
      for (int group = 0; group < 2; group++) {
         float top = groupTop(group) + FIELD_Y;
         if (y < top || y >= top + FIELD_H) continue;
         if (this.open == group) this.expanded = !this.expanded;
         else { this.open = group; this.expanded = true; this.scroll = 0; this.reveal.snap(0, OpaiMotion.now()); }
         return;
      }
   }
   void paint(OpaiSurface s, OpaiStyle.Palette p, float x, float y, float width) {
      for (int group = 0; group < 2; group++) {
         MultiSelectSetting choices = choices(group);
         float top = y + groupTop(group), left = x + TEXT_PAD - 2, fieldWidth = width - TEXT_PAD * 2 + 4;
         s.text(choices.getName(), x + TEXT_PAD, top + 8, 8.5f, fieldWidth, p.text());
         s.rounded(left, top + FIELD_Y, fieldWidth, FIELD_H, 2, 0xFF2A2A2D);
         s.text(choices.selectionLabel(), left + 6, top + FIELD_Y + FIELD_H / 2, 8.5f, fieldWidth - 12, p.text());
         s.rect(left + 1, top + FIELD_Y + FIELD_H - 1, fieldWidth - 2, 1, this.open == group && this.expanded ? p.accent() : p.fieldLine());
         if (this.open != group || this.reveal.value() <= 0) continue;
         float listTop = top + FIELD_Y + FIELD_H, shown = this.listHeight * this.reveal.value();
         s.rounded(left, listTop, fieldWidth, shown, 2, 0xFF2A2A2D);
         s.clip(left, listTop, fieldWidth, shown, () -> {
            String[] options = choices.options();
            for (int i = 0; i < options.length; i++) {
               float rowY = listTop + i * OPTION_H - this.scroll;
               if (rowY + OPTION_H <= listTop || rowY >= listTop + shown) continue;
               if (choices.selected(i)) {
                  s.rect(left + 1, rowY, fieldWidth - 2, OPTION_H, OpaiSurface.mix(0xFF2A2A2D, p.accent(), .10f));
                  s.icon(OpaiIcons.Icon.CHECK, left + 5.5f, rowY + 6, 8, p.text());
               }
               s.text(options[i], left + 17, rowY + OPTION_H / 2, 8.5f, fieldWidth - 23, p.text());
            }
            float total = options.length * OPTION_H;
            if (total > this.listHeight) {
               float thumb = Math.max(8, this.listHeight * this.listHeight / total);
               s.rounded(left + fieldWidth - 3, listTop + (this.listHeight - thumb) * this.scroll / (total - this.listHeight), 2, thumb, 1, p.scrollbar());
            }
         });
      }
   }
}
