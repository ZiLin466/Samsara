package mixins;

import com.samsara.util.animation.HoverMotion;
import com.samsara.ui.terminal.TerminalTheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractButton.class)
public abstract class MixinTerminalButton {
   @Unique private final HoverMotion samsara$hover = new HoverMotion();
   @Inject(method = "extractWidgetRenderState", at = @At("HEAD"), cancellable = true)
   private void samsara$button(GuiGraphicsExtractor graphics, int x, int y, float delta, CallbackInfo ci) {
      if (!TerminalTheme.active()) return;
      TerminalTheme.button(graphics, (AbstractWidget)(Object)this, samsara$hover); ci.cancel();
   }
}
