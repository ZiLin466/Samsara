package mixins;

import com.samsara.util.render.GuiItemOpacity;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiItemRenderState.class)
public class MixinGuiItemRenderState implements GuiItemOpacity.State {
   @Unique private float samsara$opacity = 1;
   @Inject(method = "<init>", at = @At("RETURN"))
   private void samsara$captureOpacity(CallbackInfo ci) { samsara$opacity = GuiItemOpacity.current(); }
   @Override public float samsara$itemOpacity() { return samsara$opacity; }
}
