package com.samsara.ui.hud.editor;

import com.samsara.ui.hud.HudLayouts;
import com.samsara.module.visual.ClickGui;
import com.samsara.util.render.NVGRenderer;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.nanovg.NanoVG.*;

public final class HudEditButton {
   public static final float SIZE = 26.7f, INSET = 8;
   private long opened;
   private HudLayouts.Box box;
   private float alpha;
   public void open() { opened = System.nanoTime(); box = null; alpha = 0; }
   public boolean click(double x, double y, int button) {
      return button == 1 && alpha > .15f && box != null && box.contains(x, y);
   }
   public void draw(int width, int height, float opacity, boolean closing) {
      if (net.minecraft.client.Minecraft.getInstance().player == null) return;
      double seconds = Math.max(0, (System.nanoTime() - opened) / 1_000_000_000.0);
      float rise = (float)Math.exp(-seconds * 30);
      float y = height - INSET - SIZE + (SIZE + INSET) * (closing ? 1 - opacity : rise);
      box = new HudLayouts.Box(width - INSET - SIZE, y, SIZE, SIZE); alpha = opacity;
      paint(NVGRenderer.getContext(), box.x(), box.y(), opacity, ClickGui.currentOpaiPalette().accent());
   }
   public static void paint(long vg, float x, float y, float opacity, int accent) {
      nvgSave(vg);
      try (MemoryStack stack = MemoryStack.stackPush()) {
         nvgGlobalAlpha(vg, opacity); nvgTranslate(vg, x, y);
         var color = NVGColor.malloc(stack);
         nvgRGBA((byte)(accent >> 16),(byte)(accent >> 8),(byte)accent,(byte)255,color);
         nvgFillColor(vg,color); nvgBeginPath(vg); nvgRoundedRect(vg,0,0,SIZE,SIZE,5); nvgFill(vg);
         nvgRGBA((byte)16,(byte)16,(byte)20,(byte)255,color); nvgFillColor(vg,color);
         nvgBeginPath(vg);
         nvgMoveTo(vg,6.5f,16.4f); nvgLineTo(vg,15.7f,7.2f); nvgLineTo(vg,19.5f,11);
         nvgLineTo(vg,10.3f,20.2f); nvgLineTo(vg,6.2f,20.5f); nvgClosePath(vg);
         nvgMoveTo(vg,16.6f,6.3f); nvgLineTo(vg,17.4f,5.5f);
         nvgBezierTo(vg,20.1f,2.8f,23.9f,6.6f,21.2f,9.3f);
         nvgLineTo(vg,20.4f,10.1f); nvgClosePath(vg); nvgFill(vg);
         nvgBeginPath(vg); nvgRoundedRect(vg,6,22,16,1.6f,.5f); nvgFill(vg);
      } finally { nvgRestore(vg); }
   }
}
