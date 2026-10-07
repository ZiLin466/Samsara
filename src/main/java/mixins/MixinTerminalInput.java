package mixins;

import com.samsara.ui.terminal.TerminalTheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EditBox.class)
public abstract class MixinTerminalInput {
   @Inject(method = "extractWidgetRenderState", at = @At("HEAD"))
   private void samsara$input(GuiGraphicsExtractor graphics, int mx, int my, float delta, CallbackInfo ci) {
      if (!TerminalTheme.active()) return;
      EditBox box = (EditBox)(Object)this;
      TerminalTheme.input(box);
      graphics.fill(box.getX() - 4, box.getY() - 4, box.getRight() + 4, box.getBottom() + 4, 0xF0292D35);
      graphics.fill(box.getX() - 4, box.getBottom() + 3, box.getRight() + 4, box.getBottom() + 4,
         box.isFocused() ? 0xFF00B8D9 : 0xFF67717D);
   }
}
