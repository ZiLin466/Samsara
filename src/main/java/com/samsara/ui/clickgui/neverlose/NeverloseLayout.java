package com.samsara.ui.clickgui.neverlose;

import java.util.ArrayList;
import java.util.List;

public final class NeverloseLayout {
   public static final float WIDTH = 760, HEIGHT = 620, SIDEBAR = 180, HEADER = 64;
   public static final float RADIUS = 4;
   public static final Rect WINDOW = new Rect(0, 0, WIDTH, HEIGHT);
   // Hide the backdrop's inner rounded corners beneath the opaque content panel.
   public static final Rect SIDEBAR_BACKDROP = new Rect(0, 0, SIDEBAR + RADIUS, HEIGHT);
   public static final float ROW = 29, SECTION_HEADER = 28, GAP = 18, OPTION = 25;
   public static final Rect CONTENT = new Rect(202, 84, 536, 516);
   public static final Rect SEARCH = new Rect(202, 19, 330, 27);
   public static final Rect SAVE = new Rect(544, 19, 70, 27);
   public static final Rect PRESET = new Rect(626, 19, 112, 27);
   public static final Rect CONFIG_INPUT = new Rect(202, 84, 344, 28);
   public static final Rect CREATE = new Rect(558, 84, 180, 28);
   public static final String[] NAV_NAMES = {"Combat", "Targets", "Movement", "Player", "Visuals", "Misc", "HUD Editor", "Configs"};
   private static final float[] NAV_Y = {96, 132, 200, 236, 304, 372, 408, 444};

   private NeverloseLayout() { }

   public record Rect(float x, float y, float width, float height) {
      public float right() { return x + width; }
      public float bottom() { return y + height; }
      public boolean contains(double px, double py) {
         return width > 0 && height > 0 && px >= x && px < right() && py >= y && py < bottom();
      }
      public Rect intersect(Rect other) {
         float left = Math.max(x, other.x), top = Math.max(y, other.y);
         return new Rect(left, top, Math.max(0, Math.min(right(), other.right()) - left),
            Math.max(0, Math.min(bottom(), other.bottom()) - top));
      }
   }

   public record Viewport(float x, float y, float scale) {
      public Rect screen(Rect local, float openingScale) {
         float size = scale * openingScale;
         return new Rect(x + WIDTH * scale / 2 + (local.x() - WIDTH / 2) * size,
            y + HEIGHT * scale / 2 + (local.y() - HEIGHT / 2) * size,
            local.width() * size, local.height() * size);
      }
      public float localX(double screenX, float openingScale) {
         return WIDTH / 2 + ((float)screenX - x - WIDTH * scale / 2) / (scale * openingScale);
      }
      public float localY(double screenY, float openingScale) {
         return HEIGHT / 2 + ((float)screenY - y - HEIGHT * scale / 2) / (scale * openingScale);
      }
   }

   public static Viewport viewport(int width, int height, float x, float y) {
      float scale = Math.max(.05f, Math.min(1, Math.min((width - 16f) / WIDTH, (height - 16f) / HEIGHT)));
      return new Viewport(Math.clamp(x, 4, Math.max(4, width - WIDTH * scale - 4)),
         Math.clamp(y, 4, Math.max(4, height - HEIGHT * scale - 4)), scale);
   }

   public static Viewport centered(int width, int height) {
      Viewport size = viewport(width, height, 4, 4);
      return viewport(width, height, (width - WIDTH * size.scale) / 2, (height - HEIGHT * size.scale) / 2);
   }

   public static Rect navigation(int index) { return new Rect(10, NAV_Y[index], SIDEBAR - 20, 29); }
   public static Rect control(Rect row) { return new Rect(row.right() - 126, row.y + (ROW - 20) / 2, 126, 20); }
   public static Rect slider(Rect row) { return new Rect(row.right() - 126, row.y + ROW / 2 - 6, 88, 12); }
   public static Rect binding(Rect section) { return new Rect(section.right() - 67, section.y + 3, 48, 18); }

   public record Packed(List<Rect> sections, float height) { }

   public static Packed pack(List<Float> heights, float scroll) {
      List<Integer> columns = new ArrayList<>();
      float[] ends = {0, 0};
      for (float height : heights) {
         int column = ends[0] <= ends[1] ? 0 : 1;
         columns.add(column); ends[column] += height + GAP;
      }
      return pack(heights, columns, scroll);
   }

   public static Packed pack(List<Float> heights, List<Integer> columns, float scroll) {
      List<Rect> sections = new ArrayList<>();
      float[] ends = {0, 0};
      float columnWidth = (CONTENT.width - 24) / 2;
      for (int i = 0; i < heights.size(); i++) {
         float height = heights.get(i);
         int column = columns.get(i);
         sections.add(new Rect(CONTENT.x + column * (columnWidth + 24), CONTENT.y + ends[column] - scroll,
            columnWidth, height));
         ends[column] += height + GAP;
      }
      return new Packed(List.copyOf(sections), Math.max(0, Math.max(ends[0], ends[1]) - GAP));
   }

   public static Rect dropdown(Rect anchor, int count) {
      float h = Math.min(240, count * OPTION + 8);
      float y = anchor.bottom() + 4;
      if (y + h > HEIGHT - 14) y = anchor.y - h - 4;
      return new Rect(Math.clamp(anchor.x, SIDEBAR + 8, WIDTH - anchor.width - 10),
         Math.clamp(y, HEADER + 6, HEIGHT - h - 14), anchor.width, h);
   }

   public static float fraction(double x, Rect track) {
      return Math.clamp(((float)x - track.x) / track.width, 0, 1);
   }

   public static Rect thumb(float scroll, float maximum) {
      return thumb(scroll, maximum, CONTENT);
   }

   public static Rect thumb(float scroll, float maximum, Rect track) {
      float h = Math.max(24, track.height * track.height / (track.height + maximum));
      return new Rect(748, track.y + (maximum <= 0 ? 0 : scroll / maximum) * (track.height - h), 3, h);
   }

   public static int mouseButton(int sdlButton) {
      return switch (sdlButton) { case 1 -> 0; case 3 -> 1; default -> sdlButton; };
   }

   public static final class Motion {
      private final double rate;
      private double time = Double.NaN;
      private float value, target;
      private double velocity;

      public Motion(float value, double rate) { this.value = this.target = value; this.rate = rate; }
      public float to(float target, double now) {
         if (!Double.isNaN(time)) {
            double dt = Math.max(0, now - time), error = value - this.target;
            double c = velocity + rate * error, decay = Math.exp(-rate * dt);
            // Analytic critical damping keeps velocity continuous when the target changes.
            value = this.target + (float)((error + c * dt) * decay);
            velocity = (velocity - rate * c * dt) * decay;
            if (Math.abs(value - this.target) < .0001f && Math.abs(velocity) < .001) { value = this.target; velocity = 0; }
         }
         time = now;
         this.target = target;
         return value;
      }
      public void snap(float value, double now) { this.value = this.target = value; velocity = 0; time = now; }
      public float value() { return value; }
      public double velocity() { return velocity; }
   }
}
