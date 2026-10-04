package com.samsara.util.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.samsara.module.visual.InventoryHud.InventoryHudLayout;
import com.samsara.module.visual.TargetHud.OpaiTargetHudPainter;
import com.samsara.ui.dynamicIsland.DynamicIslandManager;
import com.samsara.ui.dynamicIsland.DynamicIslandState;
import com.samsara.ui.hud.HudLayouts;
import java.util.ArrayList;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import org.slf4j.LoggerFactory;

/** Bridges GUI extraction and rendering without native texture interop or a previous-frame image. */
public final class HudBackdrop {
   private static final HudBlurRenderer BLUR = new HudBlurRenderer();
   private static GpuHudBlurRenderer gpuBlur;
   private static InventoryHudLayout.Bounds inventory;
   private static OpaiTargetHudPainter.Bounds targetHud;
   private record Widget(HudLayouts.Box box, float radius, float opacity) { }
   private static final ArrayList<Widget> widgets = new ArrayList<>();
   private static boolean ready;
   private static boolean failed;

   private HudBackdrop() { }

   public static void inventory(InventoryHudLayout.Bounds bounds) {
      inventory = bounds;
   }

   public static void target(OpaiTargetHudPainter.Bounds bounds) {
      targetHud = bounds;
   }

   public static void widget(HudLayouts.Box box, float radius) { widget(box, radius, 1); }
   public static void widget(HudLayouts.Box box, float radius, float opacity) {
      if (opacity > 0) widgets.add(new Widget(box, radius, Math.clamp(opacity, 0, 1)));
   }

   /** World rendering has completed; native GUI item rendering has not started. */
   public static void prepare() {
      InventoryHudLayout.Bounds panel = inventory;
      OpaiTargetHudPainter.Bounds targetPanel = targetHud;
      var widgetPanels = new ArrayList<>(widgets);
      widgets.clear();
      inventory = null;
      targetHud = null;
      ready = false;
      BLUR.invalidate();
      if (gpuBlur != null) gpuBlur.invalidate();
      Minecraft mc = Minecraft.getInstance();
      if (failed || !NVGRenderer.isAvailable() || mc.player == null || mc.level == null
          || mc.gui.overlay() != null
          || (panel == null && targetPanel == null && widgetPanels.isEmpty() && !DynamicIslandManager.shouldRender(mc.gui.screen()))) return;
      RenderTarget target = mc.gameRenderer.mainRenderTarget();
      if (target == null || target.getColorTextureView() == null) return;
      int guiWidth = mc.getWindow().getGuiScaledWidth();
      var regions = new ArrayList<HudBlurRenderer.Region>(3);
      if (DynamicIslandManager.shouldRender(mc.gui.screen())) {
         regions.add(new HudBlurRenderer.Region(0, DynamicIslandState.TOP - 1, guiWidth,
            DynamicIslandState.IDLE_TOP - DynamicIslandState.TOP
               + Math.max(DynamicIslandState.MAX_NOTICES * DynamicIslandState.ROW_HEIGHT, 128) * 1.1f
                  * HudLayouts.INSTANCE.get(HudLayouts.Element.ISLAND).scale() + 2));
      }
      if (panel != null) {
         regions.add(new HudBlurRenderer.Region(panel.x(), panel.y(), panel.width(), panel.height()));
      }
      if (targetPanel != null) {
         regions.add(new HudBlurRenderer.Region(targetPanel.x(), targetPanel.y(), targetPanel.width(), targetPanel.height()));
      }
      for (Widget widget : widgetPanels) {
         var box = widget.box(); regions.add(new HudBlurRenderer.Region(box.x(), box.y(), box.width(), box.height()));
      }
      boolean gpu = NVGRenderer.usesGpuBackend();
      if (gpu && gpuBlur == null) gpuBlur = new GpuHudBlurRenderer(RenderSystem.getDevice());
      try (RenderPass pass = gpu ? null : RenderSystem.getDevice().createCommandEncoder()
              .createRenderPass(() -> "samsara/hud-backdrop", target.getColorTextureView(), Optional.empty())) {
         if (gpu) gpuBlur.capture(target.getColorTextureView(), guiWidth, regions);
         else BLUR.capture(target.width, target.height, guiWidth, regions);
         if (panel != null) {
            panel(target, panel.x(), panel.y(), panel.width(), panel.height(), InventoryHudLayout.RADIUS * panel.scale(),
               guiWidth, mc.getWindow().getGuiScaledHeight(), 1);
         }
         if (targetPanel != null) {
            panel(target, targetPanel.x(), targetPanel.y(), targetPanel.width(), targetPanel.height(),
               OpaiTargetHudPainter.RADIUS, guiWidth, mc.getWindow().getGuiScaledHeight(), 1);
         }
         for (Widget widget : widgetPanels) {
            var box = widget.box(); panel(target, box.x(), box.y(), box.width(), box.height(), widget.radius(),
               guiWidth, mc.getWindow().getGuiScaledHeight(), widget.opacity());
         }
         ready = true;
      } catch (RuntimeException failure) {
         fail(failure);
      }
   }

   /** Animated island geometry is known at the NanoVG stage, using the same world capture. */
   public static void island(float x, float y, float width, float height, float radius) {
      if (!ready || failed) return;
      Minecraft mc = Minecraft.getInstance();
      RenderTarget target = mc.gameRenderer.mainRenderTarget();
      if (target == null || target.getColorTextureView() == null) return;
      try (RenderPass pass = NVGRenderer.usesGpuBackend() ? null : RenderSystem.getDevice().createCommandEncoder()
              .createRenderPass(() -> "samsara/island-backdrop", target.getColorTextureView(), Optional.empty())) {
         panel(target, x, y, width, height, radius,
            mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), 1);
      } catch (RuntimeException failure) {
         fail(failure);
      }
   }

   private static void panel(RenderTarget target, float x, float y, float width, float height, float radius,
                             float guiWidth, float guiHeight, float opacity) {
      if (NVGRenderer.usesGpuBackend()) {
         gpuBlur.panel(target.getColorTextureView(), x, y, width, height, radius, guiWidth, guiHeight, opacity);
      } else BLUR.panel(x, y, width, height, radius, guiWidth, guiHeight, opacity);
   }

   private static void fail(RuntimeException failure) {
      failed = true;
      ready = false;
      LoggerFactory.getLogger("samsara-hud").warn("HUD backdrop blur unavailable; keeping translucent panels", failure);
   }

   public static void close() {
      BLUR.close();
      if (gpuBlur != null) { gpuBlur.close(); gpuBlur = null; }
      inventory = null;
      targetHud = null;
      ready = failed = false;
      widgets.clear();
   }
}
