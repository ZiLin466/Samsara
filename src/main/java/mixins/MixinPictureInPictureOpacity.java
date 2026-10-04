package mixins;

import com.samsara.util.render.GuiItemOpacity;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.pip.OversizedItemRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(PictureInPictureRenderer.class)
public class MixinPictureInPictureOpacity {
   @ModifyArgs(method = "blitTexture", at = @At(value = "INVOKE", target =
      "Lnet/minecraft/client/renderer/state/gui/BlitRenderState;<init>(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/client/gui/render/TextureSetup;Lorg/joml/Matrix3x2fc;IIIIFFFFILnet/minecraft/client/gui/navigation/ScreenRectangle;Lnet/minecraft/client/gui/navigation/ScreenRectangle;)V"))
   private void samsara$fadeOversizedItem(Args args, PictureInPictureRenderState state, GuiRenderState gui) {
      if (state instanceof OversizedItemRenderState item) args.set(11,
         GuiItemOpacity.premultipliedTint(((GuiItemOpacity.State)(Object)item.guiItemRenderState()).samsara$itemOpacity()));
   }
}
