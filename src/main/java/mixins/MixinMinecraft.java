package mixins;

import com.samsara.ClientBranding;
import com.samsara.event.Events;
import com.samsara.module.FeatureManager;
import com.samsara.ui.mainmenu.window.WindowBranding;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Minecraft.class})
public class MixinMinecraft {
   private boolean f211 = false;

   @Inject(method = "createTitle", at = @At("HEAD"), cancellable = true)
   private void samsara$windowTitle(CallbackInfoReturnable<String> callback) {
      callback.setReturnValue(ClientBranding.WINDOW_TITLE);
   }

   @Inject(method = "<init>", at = @At("TAIL"))
   private void samsara$windowIcon(CallbackInfo callback) {
      WindowBranding.applyIcon(((Minecraft)(Object)this).getWindow().handle());
   }

   @Inject(
      method = {"onResourceLoadFinished"},
      at = {@At("TAIL")}
   )
   private void pm$38(CallbackInfo var1) {
      if (!this.f211) {
         FeatureManager.loadEnabled();
         com.samsara.config.ConfigManager.loadState();
         this.f211 = true;
      }
   }

   @Inject(
      method = {"handleKeybinds"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/Gui;handleKeybinds()V",
         shift = Shift.AFTER
      )}
   )
   private void pm$40(CallbackInfo var1) {
      Events.f4.call();
   }

   @Inject(method = "tick", at = @At("TAIL"))
   private void samsara$trackSessionAndSave(CallbackInfo callback) {
      com.samsara.module.visual.Hud.SessionHud.SessionTracker.tick();
      com.samsara.config.ConfigManager.tick();
   }

   @Inject(method = "tick", at = @At("HEAD"))
   private void samsara$initializeRestoredWorldModules(CallbackInfo callback) {
      if (this.f211) FeatureManager.getModules().forEach(com.samsara.module.Feature::initializeWorldState);
      if (this.f211 && FeatureManager.f28 != null) FeatureManager.f28.clientTick();
      if (this.f211 && FeatureManager.autoRod != null) FeatureManager.autoRod.clientTick();
   }

   @Inject(method = "close", at = @At("HEAD"))
   private void samsara$flushState(CallbackInfo callback) {
      com.samsara.config.ConfigManager.flush();
      com.samsara.util.render.NVGRenderer.close();
   }
}
