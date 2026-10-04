package mixins;

import com.samsara.ui.dynamicIsland.DynamicIslandManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class MixinChestStealerScreen {
   @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("HEAD"), cancellable = true)
   private void samsara$extractChestIsland(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                         float partialTick, CallbackInfo callback) {
      if (DynamicIslandManager.extractChestItems((Screen)(Object)this, graphics)) callback.cancel();
   }
}
