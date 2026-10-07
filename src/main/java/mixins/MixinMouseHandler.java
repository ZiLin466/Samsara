package mixins;

import com.samsara.event.Events;
import com.samsara.event.impl.EventMouseButton;
import com.samsara.module.FeatureManager;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.util.SmoothDouble;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({MouseHandler.class})
public class MixinMouseHandler {
   @Shadow @Final private Minecraft minecraft;
   @Shadow @Final private SmoothDouble smoothTurnX;
   @Shadow @Final private SmoothDouble smoothTurnY;
   @Unique private CameraType samsara$cameraType;
   @Unique private Object samsara$player;
   @Unique private boolean samsara$scaffold;

   @Inject(method = "handleAccumulatedMovement", at = @At("HEAD"))
   private void samsara$clearCameraFilter(CallbackInfo callback) {
      CameraType camera = this.minecraft.options.getCameraType();
      boolean scaffold = FeatureManager.scaffold != null && FeatureManager.scaffold.isEnabled();
      boolean changed = this.samsara$cameraType != camera || this.samsara$player != this.minecraft.player
         || this.samsara$scaffold != scaffold;
      if (changed) {
         this.smoothTurnX.reset();
         this.smoothTurnY.reset();
         if (scaffold || this.samsara$scaffold) {
            this.minecraft.options.smoothCamera = false;
            if (this.minecraft.player != null) {
               this.minecraft.player.yRotO = this.minecraft.player.getYRot();
               this.minecraft.player.xRotO = this.minecraft.player.getXRot();
            }
         }
      }
      this.samsara$cameraType = camera;
      this.samsara$player = this.minecraft.player;
      this.samsara$scaffold = scaffold;
   }
   @Inject(
      method = {"onButton"},
      at = {@At("HEAD")}
   )
   private void samsara$dispatchMouseButton(long windowHandle, MouseButtonInfo buttonInfo, int action, CallbackInfo callback) {
      EventMouseButton mouseButtonEvent = Events.MOUSE_BUTTON.reset(buttonInfo.button(), action, buttonInfo.modifiers());
      mouseButtonEvent.call();
      // Let MouseHandler continue into Minecraft's Screen dispatch. Cancelling
      // here prevents NanoVG screens from receiving clicks and drag events.
   }
}
