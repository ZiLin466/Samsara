package mixins;

import com.samsara.event.Events;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Gui.class})
public class MixinGui {
   @Inject(
      method = {"extractRenderState"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/Hud;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
         shift = Shift.AFTER
      )}
   )
   private void samsara$dispatchRender2D(DeltaTracker deltaTracker, boolean renderBlockOutline, boolean renderDebug, CallbackInfo callback, @Local GuiGraphicsExtractor graphics) {
      com.samsara.ui.hud.HudLayouts.INSTANCE.beginFrame();
      com.samsara.ui.dynamicIsland.DynamicIslandManager.beginExtraction();
      Events.RENDER_2D.reset(graphics, deltaTracker).call();
   }
}
