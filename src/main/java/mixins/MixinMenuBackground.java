package mixins;

import com.samsara.ui.mainmenu.background.MenuBackground;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Screen.class)
public abstract class MixinMenuBackground {
   @WrapOperation(method = "extractRenderStateWithTooltipAndSubtitles", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/client/gui/screens/Screen;extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"))
   private void samsara$background(Screen screen, GuiGraphicsExtractor g, int x, int y, float delta, Operation<Void> original) {
      if (screen instanceof com.samsara.ui.terminal.TerminalPage page)
         com.samsara.ui.terminal.TerminalTheme.background(g, screen, page);
      else if (MenuBackground.applies(screen)) MenuBackground.render(g, screen.width, screen.height);
      else original.call(screen, g, x, y, delta);
   }
}
