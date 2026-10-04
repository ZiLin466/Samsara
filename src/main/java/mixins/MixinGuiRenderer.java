package mixins;

import com.samsara.module.FeatureManager;
import com.samsara.ui.dynamicIsland.DynamicIslandManager;
import com.samsara.ui.NanoGui;
import com.samsara.ui.loading.SamsaraLoadingState;
import com.samsara.ui.mainmenu.launch.LaunchResources;
import com.samsara.ui.mainmenu.screen.RockstarTitleScreen;
import com.samsara.util.render.NVGRenderer;
import com.samsara.util.render.HudBackdrop;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderer.class)
public class MixinGuiRenderer {

   private static boolean logged;
   @Unique private boolean samsara$islandBehindContainer;

   @Inject(method = "render", at = @At("HEAD"))
   private void samsara$prepareHudBackdrop(CallbackInfo callbackInfo) {
      this.samsara$islandBehindContainer = false;
      HudBackdrop.prepare();
      this.samsara$renderIslandBeforeContainer();
   }

   @Unique
   private void samsara$renderIslandBeforeContainer() {
      Screen current = Minecraft.getInstance().gui.screen();
      if (!(current instanceof AbstractContainerScreen<?>) || !NVGRenderer.isAvailable()
          || !DynamicIslandManager.shouldRender(current)) return;
      boolean started = NVGRenderer.beginFrame();
      try {
         if (started) {
            // Draw before prepare() renders the inventory avatar into its small PIP target.
            // The container gradient then covers the island without saving a PIP viewport.
            DynamicIslandManager.renderNano();
            this.samsara$islandBehindContainer = true;
         }
      } finally {
         if (started) {
            NVGRenderer.endFrame();
            NVGRenderer.clearScissors();
         }
      }
   }

   @Inject(method = "close", at = @At("HEAD"))
   private void samsara$closeHudBackdrop(CallbackInfo callbackInfo) {
      HudBackdrop.close();
   }

   @Inject(method = "endFrame", at = @At("TAIL"))
   private void samsara$renderNanoGui(CallbackInfo callbackInfo) {
      boolean hud = FeatureManager.f25 != null && (FeatureManager.f25.isEnabled() || com.samsara.ui.hud.editor.HudEditorScreen.active());
      Minecraft mc = Minecraft.getInstance();
      Screen current = mc == null ? null : mc.gui.screen();
      boolean gui = current instanceof NanoGui && NVGRenderer.isAvailable();
      boolean title = current instanceof RockstarTitleScreen && NVGRenderer.isAvailable();
      boolean loading = mc != null && mc.gui.overlay() instanceof LoadingOverlay && NVGRenderer.isAvailable();
      boolean island = !this.samsara$islandBehindContainer && NVGRenderer.isAvailable() && DynamicIslandManager.shouldRender(current);
      boolean chestOverlay = NVGRenderer.isAvailable() && DynamicIslandManager.hasChestOverlay();
      if (!hud && !gui && !title && !loading && !island && !chestOverlay) {
         if (!logged) {
            logged = true;
            org.slf4j.LoggerFactory.getLogger("samsara-nvg").info(
               "[samsara] nano hud skipped (module {})",
               FeatureManager.f25 == null ? "null" : "disabled");
         }
         return;
      }

      boolean started = false;
      try {
         started = NVGRenderer.beginFrame();
         if (started) {
            if (hud) {
               FeatureManager.f25.renderNano();
            }
            if (gui) {
               ((NanoGui) current).renderNano();
               if (mc.gui.screen() == current) {
                  if (current instanceof com.samsara.ui.clickgui.opai.OpaiClickGuiScreen opai) opai.drawHudEditButton();
                  else if (current instanceof com.samsara.ui.clickgui.RockstarClickGuiScreen modern) modern.drawHudEditButton();
               }
            }
            if (title) {
               ((RockstarTitleScreen) current).renderNano();
            }
            if (loading) {
               samsara$drawLoading(mc);
            }
            if (island) {
               DynamicIslandManager.renderNano();
            }
            if (chestOverlay) DynamicIslandManager.renderChestOverlay();
            if (current instanceof com.samsara.ui.hud.editor.HudEditorScreen editor) editor.renderOutline();
         }
         if (!logged) {
            logged = true;
            org.slf4j.LoggerFactory.getLogger("samsara-nvg").info(
               "[samsara] nano hud frame started={}, available={}",
               started, NVGRenderer.isAvailable());
         }
      } finally {
         if (started) {
            NVGRenderer.endFrame();
            NVGRenderer.clearScissors();
         }
      }
   }

   private static void samsara$drawLoading(Minecraft mc) {
      float width = mc.getWindow().getGuiScaledWidth();
      float height = mc.getWindow().getGuiScaledHeight();
      LaunchResources.renderer().loading(width, height,
         SamsaraLoadingState.done ? 1 : SamsaraLoadingState.progress, System.nanoTime() / 1_000_000_000.0);
   }
}
