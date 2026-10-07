package com.samsara.ui.clickgui.opai;

public final class OpaiStyle {
   public record Palette(int header, int body, int enabled, int enabledHover, int accent,
                         int enabledText, int text, int hover, int field, int fieldLine,
                         int track, int scrollbar, int selected, int toggleOutline, int tick,
                         int hudProgress, int hudKnob) { }

   static final float HEADER_TEXT_SIZE = 9.2f;
   static final float ROW_TEXT_SIZE = 8.3f;
   static final float TEXT_WEIGHT_OFFSET = 0.10f;

   public static final Palette LAVENDER = new Palette(
      0xFF1C1F29, 0xCC151315, 0xFFA8B0E7, 0xFFB0B8EF, 0xFFBBC3FF,
      0xFF172778, 0xFFE4E1E6, 0x0CE4E1E6, 0xFF2D2D38, 0xFFC4C0CC,
      0xFF484858, 0xCC636472, 0xFF414253, 0xFF938F99, 0xFF626786, 0xFF9CA5DA, 0xFF5B5F7C);
   public static final Palette LIGHT_PINK = new Palette(
      0xFF161110, 0xCC1A1413, 0xFFDF9D95, 0xFFE5A59D, 0xFFFFB4AA,
      0xFF5F150F, 0xFFEDE0DE, 0x0CEDE0DE, 0xFF2E2625, 0xFFCDB7B4,
      0xFF56413E, 0xCC69534D, 0xFF483A37, 0xFF9E8D8B, 0xFF85645D, 0xFFD19997, 0xFF7C5752);

   private OpaiStyle() {
   }

   public static Palette palette(String name) {
      return "Light Pink".equals(name) ? LIGHT_PINK : LAVENDER;
   }
}
