package mixins;

import com.samsara.event.Events;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LivingEntityRenderer.class})
public class MixinLivingEntityRenderer<T extends LivingEntity, S extends LivingEntityRenderState> {
   @Inject(
      method = {"extractRenderState"},
      at = {@At(
         value = "FIELD",
         target = "Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;xRot:F",
         shift = Shift.AFTER
      )}
   )
   private void samsara$extractManagedRotation(LivingEntity target, LivingEntityRenderState renderState, float partialTick, CallbackInfo callback) {
      if (target == Minecraft.getInstance().player) {
         renderState.xRot = Mth.rotLerp(partialTick, Events.ROTATION.getPreviousPitch(), Events.ROTATION.getPitch());
      }
   }
}
