package mixins;

import com.samsara.event.Events;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({LivingEntity.class})
public class MixinLivingEntity {
   @ModifyExpressionValue(
      method = {"jumpFromGround"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/LivingEntity;getYRot()F"
      )}
   )
   private float samsara$jumpYaw(float originalYaw) {
      if (Events.ROTATION.hasMovementCorrection()) {
         return Events.ROTATION.usesClientRotation() ? Events.ROTATION.getYaw() : Events.PRE_MOTION.getYaw();
      } else {
         return originalYaw;
      }
   }
}
