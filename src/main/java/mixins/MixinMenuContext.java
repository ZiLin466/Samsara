package mixins;

import com.samsara.ui.mainmenu.background.MenuBackground;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class MixinMenuContext {
   @Inject(method = "setScreen", at = @At("HEAD"))
   private void samsara$context(Screen screen, CallbackInfo ci) { MenuBackground.transition(screen); }
}
