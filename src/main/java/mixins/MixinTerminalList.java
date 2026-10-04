package mixins;

import com.samsara.ui.terminal.TerminalTheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSelectionList.class)
public abstract class MixinTerminalList {
   @Inject(method = {"extractListBackground", "extractListSeparators"}, at = @At("HEAD"), cancellable = true)
   private void samsara$background(GuiGraphicsExtractor g, CallbackInfo ci) {
      if (TerminalTheme.active()) ci.cancel();
   }
   @Inject(method = "extractSelection", at = @At("HEAD"), cancellable = true)
   private void samsara$selection(GuiGraphicsExtractor g, @Coerce Object entry, int color, CallbackInfo ci) {
      if (TerminalTheme.active()) ci.cancel();
   }
   @Inject(method = "extractItem", at = @At("HEAD"))
   private void samsara$row(GuiGraphicsExtractor g, int mx, int my, float delta, @Coerce Object entry, CallbackInfo ci) {
      if (!TerminalTheme.active()) return;
      AbstractSelectionList<?> list = (AbstractSelectionList<?>)(Object)this;
      var e = (LayoutElement)entry;
      boolean selected = list.getSelected() == entry;
      boolean hover = ((GuiEventListener)entry).isMouseOver(mx, my);
      int x = e.getX(), y = e.getY(), right = x + e.getWidth(), bottom = y + e.getHeight() - 3;
      g.fill(x, y, right, bottom, selected ? 0xEB214856 : hover ? 0xEB3D444F : 0xDD292D35);
      g.fill(x, y, x + 2, bottom, selected ? 0xFF00B8D9 : hover ? 0xFF929DA9 : 0xFF4E5864);
   }
}
