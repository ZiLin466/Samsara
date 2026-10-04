package mixins;

import com.samsara.util.render.GuiItemOpacity;
import net.minecraft.client.gui.render.GuiItemAtlas;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(GuiRenderer.class)
public class MixinItemAtlasOpacity {
   @ModifyArgs(method = "submitBlitFromItemAtlas", at = @At(value = "INVOKE", target =
      "Lnet/minecraft/client/renderer/state/gui/BlitRenderState;<init>(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/client/gui/render/TextureSetup;Lorg/joml/Matrix3x2fc;IIIIFFFFILnet/minecraft/client/gui/navigation/ScreenRectangle;Lnet/minecraft/client/gui/navigation/ScreenRectangle;)V"))
   private void samsara$fadeItem(Args args, GuiItemRenderState item, GuiItemAtlas.SlotView slot) {
      args.set(11, GuiItemOpacity.premultipliedTint(((GuiItemOpacity.State)(Object)item).samsara$itemOpacity()));
   }
}
