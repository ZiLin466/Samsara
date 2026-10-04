package com.samsara.ui.mainmenu.launch;

import java.util.List;

/** Drawing and hit testing share the same letterboxed 1600 x 900 composition. */
public record LaunchLayout(float scale, float left, float top) {
   public record Tile(String id, String label, float x, float y, float width, float height) {
      public boolean contains(float px, float py) {
         return px >= x && py >= y && px < x + width && py < y + height;
      }
   }

   public static final List<Tile> ACTIONS = List.of(
      new Tile("single", "单人游戏", 915, 355, 280, 140),
      new Tile("accounts", "账号设置", 1214, 355, 348, 140),
      new Tile("multi", "多人游戏", 945, 677, 242, 153),
      new Tile("options", "选项", 1203, 695, 240, 147),
      new Tile("mods", "模组", 1455, 713, 122, 147),
      new Tile("settings", "选项", 28, 22, 52, 52),
      new Tile("quit", "退出游戏", 1522, 20, 56, 56),
      new Tile("visibility", "立绘", 278, 484, 52, 52),
      new Tile("replay", "重播", 343, 484, 52, 52)
   );

   public static LaunchLayout of(float width, float height) {
      float scale = Math.max(.001f, Math.min(width / 1600, height / 900));
      return new LaunchLayout(scale, (width - 1600 * scale) / 2, (height - 900 * scale) / 2);
   }

   public float x(double screenX) { return (float)(screenX - left) / scale; }
   public float y(double screenY) { return (float)(screenY - top) / scale; }
   public Tile hit(double screenX, double screenY) {
      return ACTIONS.stream().filter(t -> t.contains(x(screenX), y(screenY))).findFirst().orElse(null);
   }
}
