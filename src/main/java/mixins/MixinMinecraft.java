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
   private boolean modulesInitialized = false;

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
   private void samsara$initializeModules(CallbackInfo callback) {
      if (!this.modulesInitialized) {
         FeatureManager.loadEnabled();
         com.samsara.config.ConfigManager.loadState();
         this.modulesInitialized = true;
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
   private void samsara$dispatchTick(CallbackInfo callback) {
      Events.TICK.call();
   }

   @Inject(method = "tick", at = @At("TAIL"))
   private void samsara$trackSessionAndSave(CallbackInfo callback) {
      FeatureManager.clientTickEnd();
      com.samsara.config.ConfigManager.tick();
   }

   @Inject(method = "tick", at = @At("HEAD"))
   private void samsara$initializeRestoredWorldModules(CallbackInfo callback) {
      if (this.modulesInitialized) FeatureManager.clientTick();
   }

   @Inject(method = "close", at = @At("HEAD"))
   private void samsara$flushState(CallbackInfo callback) {
      com.samsara.config.ConfigManager.flush();
      com.samsara.util.render.NVGRenderer.close();
   }
}
