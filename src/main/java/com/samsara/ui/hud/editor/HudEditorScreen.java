package com.samsara.ui.hud.editor;

import com.samsara.ui.hud.HudLayouts;
import com.samsara.config.ConfigManager;
import com.samsara.module.FeatureManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.sdl.SDLScancode;

/** A transparent unified editor. Fixed widgets participate in scaling but never in dragging. */
public final class HudEditorScreen extends Screen {
   private final HudEditorGesture gesture = new HudEditorGesture(HudLayouts.INSTANCE);
   private long entered;
   private int mouseX, mouseY;
   public HudEditorScreen() { super(Component.literal("HUD Editor")); }
   private static long now() { return System.nanoTime() / 1_000_000L; }
   public static boolean active() { return Minecraft.getInstance().gui.screen() instanceof HudEditorScreen; }
   public static float previewOpacity() {
      var screen = Minecraft.getInstance().gui.screen();
      return screen instanceof HudEditorScreen editor ? 1 - (float)Math.exp(-(now() - editor.entered) / 60.0) : 1;
   }
   @Override protected void init() { this.entered = now(); }
   @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) { }
   @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      this.mouseX = mouseX; this.mouseY = mouseY;
      // Preview once through the same native rendering code, even without an Aura target.
      if (FeatureManager.inventoryHud != null) FeatureManager.inventoryHud.renderInventory(graphics);
      if (FeatureManager.targetHud != null) FeatureManager.targetHud.m207(graphics, partialTick, Minecraft.getInstance().player);
      if (FeatureManager.sessionHud != null) FeatureManager.sessionHud.renderSession(graphics);
      if (FeatureManager.potionStatus != null) FeatureManager.potionStatus.renderStatus(graphics);
      graphics.centeredText(Minecraft.getInstance().font,
         "Drag HUDs • Hold left + scroll to resize • Right-click resets size • Esc to finish",
         this.width / 2, this.height - 22, 0xFFFFFFFF);
   }
   @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      return this.gesture.press(event.x(), event.y(), event.button(), now());
   }
   @Override public boolean mouseDragged(MouseButtonEvent event, double x, double y) {
      return this.gesture.drag(event.x(), event.y(), event.button(), this.width, this.height);
   }
   @Override public boolean mouseReleased(MouseButtonEvent event) { return this.gesture.release(event.button()); }
   @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
      this.gesture.wheel(vertical, now()); return true;
   }
   @Override public boolean keyPressed(KeyEvent event) {
      if (event.key() == SDLScancode.SDL_SCANCODE_ESCAPE) { onClose(); return true; }
      return super.keyPressed(event);
   }
   @Override public void onClose() { ConfigManager.saveState(); this.minecraft.gui.setScreen(null); }
   public void renderOutline() {
      var element = this.gesture.captured() != null ? this.gesture.captured() : HudLayouts.INSTANCE.hit(mouseX, mouseY);
      if (element == null) return;
      var box = HudLayouts.INSTANCE.bounds(element); if (box == null) return;
      long vg = com.samsara.util.render.NVGRenderer.getContext();
      org.lwjgl.nanovg.NanoVG.nvgSave(vg);
      try (var stack = org.lwjgl.system.MemoryStack.stackPush()) {
         var color = org.lwjgl.nanovg.NVGColor.malloc(stack);
         int accent = com.samsara.module.visual.ClickGui.currentOpaiPalette().accent();
         org.lwjgl.nanovg.NanoVG.nvgRGBA((byte)(accent >> 16),(byte)(accent >> 8),(byte)accent,(byte)160,color);
         org.lwjgl.nanovg.NanoVG.nvgStrokeColor(vg,color); org.lwjgl.nanovg.NanoVG.nvgStrokeWidth(vg,.6f);
         org.lwjgl.nanovg.NanoVG.nvgBeginPath(vg);
         org.lwjgl.nanovg.NanoVG.nvgRoundedRect(vg,box.x()-1,box.y()-1,box.width()+2,box.height()+2,4);
         org.lwjgl.nanovg.NanoVG.nvgStroke(vg);
      } finally { org.lwjgl.nanovg.NanoVG.nvgRestore(vg); }
   }
   @Override public boolean isPauseScreen() { return false; }
}
