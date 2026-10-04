package mixins;

import com.samsara.module.visual.NameTags;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.DisplayRenderer;
import net.minecraft.client.renderer.entity.state.TextDisplayEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DisplayRenderer.TextDisplayRenderer.class)
public class MixinTextDisplayRenderer {
   @Inject(method = "submitInner(Lnet/minecraft/client/renderer/entity/state/TextDisplayEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IF)V",
      at = @At("HEAD"), cancellable = true)
   private void samsara$replaceFloatingText(TextDisplayEntityRenderState state, PoseStack pose,
      SubmitNodeCollector collector, int light, float partial, CallbackInfo ci) {
      if (((NameTags.ReplacementState)state).samsara$replaceNameTag()) ci.cancel();
   }
}
