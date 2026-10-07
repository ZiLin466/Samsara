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
      if (com.samsara.module.visual.Hud.enabled(com.samsara.module.visual.Hud.Widget.POTION_STATUS)) callback.cancel();
   }

   @ModifyArg(
      method = {"displayScoreboardSidebar"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"
      ),
      index = 3
   )
   private int samsara$offsetScoreboardBottom(int coordinate) {
      return coordinate - this.samsara$scoreboardOffsetY();
   }

   @ModifyArg(
      method = {"displayScoreboardSidebar"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"
      ),
      index = 1
   )
   private int samsara$offsetScoreboardTop(int coordinate) {
      return coordinate - this.samsara$scoreboardOffsetY();
   }

   @ModifyArg(
      method = {"displayScoreboardSidebar"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"
      ),
      index = 0
   )
   private int samsara$offsetScoreboardLeft(int coordinate) {
      return coordinate - this.samsara$scoreboardOffsetX();
   }

   private int samsara$scoreboardOffsetX() {
      return FeatureManager.scoreboard.isEnabled() ? (int)FeatureManager.scoreboard.x.getValue() : 0;
   }

   private int samsara$scoreboardOffsetY() {
      return FeatureManager.scoreboard.isEnabled() ? (int)FeatureManager.scoreboard.y.getValue() : 0;
   }

   @ModifyArg(
      method = {"displayScoreboardSidebar"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"
      ),
      index = 2
   )
   private int samsara$offsetScoreboardRight(int coordinate) {
      return coordinate - this.samsara$scoreboardOffsetX();
   }

   @ModifyArg(
      method = {"displayScoreboardSidebar"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V"
      ),
      index = 2
   )
   private int samsara$offsetScoreboardTextX(int coordinate) {
      return coordinate - this.samsara$scoreboardOffsetX();
   }

   @ModifyArg(
      method = {"displayScoreboardSidebar"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V"
      ),
      index = 3
   )
   private int samsara$offsetScoreboardTextY(int coordinate) {
      return coordinate - this.samsara$scoreboardOffsetY();
   }
}
