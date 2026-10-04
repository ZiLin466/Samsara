package com.samsara.ui.hud.editor;

import com.samsara.ui.hud.HudLayouts;
/** SDL mouse capture, shared by dragging and hold-to-scale; right-click only resets scale. */
public final class HudEditorGesture {
   private final HudLayouts layouts;
   private HudLayouts.Element captured;
   private long pressedAt;
   private double grabX, grabY;
   public HudEditorGesture(HudLayouts layouts) { this.layouts = layouts; }
   public boolean press(double x, double y, int button, long now) {
      HudLayouts.Element element = this.layouts.hit(x, y);
      if (element == null) return false;
      if (button == 3) { this.layouts.get(element).resetScale(); return true; }
      if (button != 1) return false;
      this.captured = element; this.pressedAt = now;
      var box = this.layouts.bounds(element); this.grabX = x - box.x(); this.grabY = y - box.y();
      return true;
   }
   public boolean drag(double x, double y, int button, int viewportWidth, int viewportHeight) {
      if (this.captured == null || button != 1) return false;
      if (!this.captured.draggable) return true;
      var box = this.layouts.bounds(this.captured); if (box == null) return true;
      double left = Math.clamp(x - this.grabX, 2, Math.max(2, viewportWidth - box.width() - 2));
      double top = Math.clamp(y - this.grabY, 2, Math.max(2, viewportHeight - box.height() - 2));
      boolean centered = this.captured == HudLayouts.Element.TARGET;
      double anchorY=centered ? viewportHeight/2 : this.captured==HudLayouts.Element.POTION ? (viewportHeight-box.height())/2 : 0;
      this.layouts.get(this.captured).move(left - (centered ? viewportWidth / 2 : 0), top - anchorY);
      return true;
   }
   public boolean wheel(double amount, long now) {
      if (this.captured == null || now - this.pressedAt < 150) return false;
      var placement = this.layouts.get(this.captured); float before = placement.scale();
      placement.scale(before * Math.pow(1.08, amount));
      return true;
   }
   public boolean release(int button) { if (button != 1 || this.captured == null) return false; this.captured = null; return true; }
   public HudLayouts.Element captured() { return this.captured; }
}
