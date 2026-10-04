package mixins;

import com.samsara.module.FeatureManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Hud.class})
public class MixinHud {
   @Inject(method="extractEffects",at=@At("HEAD"),cancellable=true)
   private void samsara$replacePotionEffects(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo callback) {
      if (FeatureManager.potionStatus!=null && FeatureManager.potionStatus.isEnabled()) callback.cancel();
   }

   @ModifyArg(
      method = {"displayScoreboardSidebar"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"
      ),
      index = 3
   )
   private int pm$30(int var1) {
      return var1 - this.pm$24();
   }

   @ModifyArg(
      method = {"displayScoreboardSidebar"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"
      ),
      index = 1
   )
   private int pm$29(int var1) {
      return var1 - this.pm$24();
   }

   @ModifyArg(
      method = {"displayScoreboardSidebar"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"
      ),
      index = 0
   )
   private int pm$27(int var1) {
      return var1 - this.pm$23();
   }

   private int pm$23() {
      return FeatureManager.f32.isEnabled() ? (int)FeatureManager.f32.f32.m220() : 0;
   }

   private int pm$24() {
      return FeatureManager.f32.isEnabled() ? (int)FeatureManager.f32.f33.m220() : 0;
   }

   @ModifyArg(
      method = {"displayScoreboardSidebar"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"
      ),
      index = 2
   )
   private int pm$28(int var1) {
      return var1 - this.pm$23();
   }

   @ModifyArg(
      method = {"displayScoreboardSidebar"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V"
      ),
      index = 2
   )
   private int pm$25(int var1) {
      return var1 - this.pm$23();
   }

   @ModifyArg(
      method = {"displayScoreboardSidebar"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V"
      ),
      index = 3
   )
   private int pm$26(int var1) {
      return var1 - this.pm$24();
   }
}
