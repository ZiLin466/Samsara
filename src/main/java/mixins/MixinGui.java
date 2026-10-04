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
   private void pm$22(DeltaTracker var1, boolean var2, boolean var3, CallbackInfo var4, @Local GuiGraphicsExtractor var5) {
      com.samsara.ui.hud.HudLayouts.INSTANCE.beginFrame();
      com.samsara.ui.dynamicIsland.DynamicIslandManager.beginExtraction();
      Events.f5.m88(var5, var1).call();
   }
}
