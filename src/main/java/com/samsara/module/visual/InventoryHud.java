package com.samsara.module.visual;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.ui.hud.HudLayouts;
import com.samsara.ui.hud.editor.HudEditorScreen;
import com.samsara.util.ModTextures;
import com.samsara.util.render.HudBackdrop;
import com.samsara.util.render.HudGlassStyle;
import java.util.List;
import java.util.stream.IntStream;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import static com.samsara.module.visual.InventoryHud.InventoryHudLayout.*;

public final class InventoryHud extends Feature {

   public InventoryHud() {
      super("InventoryHUD", Category.VISUAL);
   }

   @Override
   public void onEvent(Event event) {
      if (event == Events.f5 && !HudEditorScreen.active()) {
         renderInventory(Events.f5.m89());
      }
   }

   public Bounds bounds(int viewportWidth, int viewportHeight) {
      var placement = HudLayouts.INSTANCE.get(HudLayouts.Element.INVENTORY);
      return InventoryHudLayout.bounds(viewportWidth, viewportHeight,
         placement.x(), placement.y(), placement.scale());
   }

   public void renderInventory(GuiGraphicsExtractor graphics) {
      if (mc.player == null || mc.level == null || mc.gui.overlay() != null) {
         return;
      }
      // Fabric Loader alone does not expose mod assets through Minecraft's resource packs.
      // Upload from the jar on first rendering, when the texture manager is ready.
      Identifier panel = ModTextures.register("textures/hud/inventory.png");
      Bounds bounds = bounds(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
      HudLayouts.INSTANCE.drawn(HudLayouts.Element.INVENTORY,
         new HudLayouts.Box(bounds.x(), bounds.y(), bounds.width(), bounds.height()));
      HudBackdrop.inventory(bounds);
      graphics.pose().pushMatrix();
      try {
         graphics.pose().translate(bounds.x(), bounds.y());
         graphics.pose().scale(bounds.scale(), bounds.scale());
         // Separate strata keep the panel behind native item models and their decorations.
         graphics.nextStratum();
         graphics.blit(RenderPipelines.GUI_TEXTURED, panel, -SHADOW_MARGIN, -SHADOW_MARGIN,
            0, 0, WIDTH + SHADOW_MARGIN * 2, HEIGHT + SHADOW_MARGIN * 2,
            TEXTURE_WIDTH, TEXTURE_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
         graphics.nextStratum();
         for (Slot slot : SLOTS) {
            ItemStack stack = mc.player.getInventory().getItem(slot.inventoryIndex());
            if (!stack.isEmpty()) {
               graphics.item(stack, slot.x(), slot.y());
               graphics.itemDecorations(mc.font, stack, slot.x(), slot.y());
            }
         }
      } finally {
         graphics.pose().popMatrix();
      }
   }

   public static final class InventoryHudLayout {
      public static final int WIDTH = 182;
      public static final int HEIGHT = 80;
      public static final int HEADER_HEIGHT = 18;
      public static final int RADIUS = 7;
      public static final int SHADOW_MARGIN = 8;
      public static final int TEXTURE_SCALE = 4;
      public static final int TEXTURE_WIDTH = (WIDTH + SHADOW_MARGIN * 2) * TEXTURE_SCALE;
      public static final int TEXTURE_HEIGHT = (HEIGHT + SHADOW_MARGIN * 2) * TEXTURE_SCALE;
      public static final int DEFAULT_X = 10;
      public static final int DEFAULT_Y = 83;
      public static final int BODY_COLOR = HudGlassStyle.BODY;
      public static final int HEADER_COLOR = HudGlassStyle.HEADER;
      public static final String PANEL_RESOURCE = "assets/samsara/textures/hud/inventory.png";
      public static final List<Slot> SLOTS = IntStream.range(0, 27)
         .mapToObj(index -> new Slot(index + 9, 3 + index % 9 * 20, 20 + index / 9 * 20))
         .toList();

      public record Slot(int inventoryIndex, int x, int y) { }

      public record Bounds(float x, float y, float scale) {
         public float width() { return WIDTH * this.scale; }
         public float height() { return HEIGHT * this.scale; }
         public boolean contains(double mouseX, double mouseY) {
            return mouseX >= this.x && mouseX < this.x + width()
               && mouseY >= this.y && mouseY < this.y + height();
         }
      }

      private InventoryHudLayout() { }

      /** Fits smaller windows without changing the saved position or chosen scale. */
      public static Bounds bounds(int viewportWidth, int viewportHeight, double x, double y, double scale) {
         float availableWidth = Math.max(1, viewportWidth - 4);
         float availableHeight = Math.max(1, viewportHeight - 4);
         float fittedScale = (float)Math.min(Math.clamp(scale, .5, 2),
            Math.min(availableWidth / WIDTH, availableHeight / HEIGHT));
         float left = (float)Math.clamp(x, 2, Math.max(2, viewportWidth - 2 - WIDTH * fittedScale));
         float top = (float)Math.clamp(y, 2, Math.max(2, viewportHeight - 2 - HEIGHT * fittedScale));
         return new Bounds(left, top, fittedScale);
      }
   }
}
