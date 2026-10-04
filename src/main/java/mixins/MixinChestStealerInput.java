package mixins;

import com.samsara.ui.dynamicIsland.DynamicIslandManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class MixinChestStealerInput {
   // Hidden vanilla slots must not receive clicks or drop carried items behind the replacement.
   @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
   private void samsara$blockHiddenClick(MouseButtonEvent event, boolean doubleClick,
                                        CallbackInfoReturnable<Boolean> callback) {
      if (DynamicIslandManager.replacesContainer((Screen)(Object)this)) callback.setReturnValue(true);
   }

   @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
   private void samsara$blockHiddenDrag(MouseButtonEvent event, double deltaX, double deltaY,
                                       CallbackInfoReturnable<Boolean> callback) {
      if (DynamicIslandManager.replacesContainer((Screen)(Object)this)) callback.setReturnValue(true);
   }

   @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
   private void samsara$blockHiddenRelease(MouseButtonEvent event, CallbackInfoReturnable<Boolean> callback) {
      if (DynamicIslandManager.replacesContainer((Screen)(Object)this)) callback.setReturnValue(true);
   }

   @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
   private void samsara$blockHiddenSlotKeys(KeyEvent event, CallbackInfoReturnable<Boolean> callback) {
      Screen screen = (Screen)(Object)this;
      if (!DynamicIslandManager.replacesContainer(screen)) return;
      if (event.isEscape() || Minecraft.getInstance().options.keyInventory.matches(event)) screen.onClose();
      callback.setReturnValue(true);
   }
}
