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
   private void samsara$dispatchHurtCamera(CameraRenderState cameraRenderState, PoseStack poseStack, CallbackInfo callback) {
      Events.HURT_CAMERA.call();
      if (Events.HURT_CAMERA.isCancelled()) {
         callback.cancel();
      }
   }
}
