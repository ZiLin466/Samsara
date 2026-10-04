package mixins;

import com.samsara.event.Events;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({GameRenderer.class})
public class MixinGameRenderer {
   @Inject(
      method = {"bobHurt"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pm$51(CameraRenderState var1, PoseStack var2, CallbackInfo var3) {
      Events.f6.call();
      if (Events.f6.isCancelled()) {
         var3.cancel();
      }
   }
}
