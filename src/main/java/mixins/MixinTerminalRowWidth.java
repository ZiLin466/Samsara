package mixins;

import com.samsara.ui.terminal.TerminalTheme;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({WorldSelectionList.class, ServerSelectionList.class})
public abstract class MixinTerminalRowWidth {
   @Inject(method = "getRowWidth", at = @At("HEAD"), cancellable = true)
   private void samsara$width(CallbackInfoReturnable<Integer> ci) {
      if (TerminalTheme.active()) ci.setReturnValue(Math.max(40, ((AbstractSelectionList<?>)(Object)this).getWidth() - 18));
   }
}
