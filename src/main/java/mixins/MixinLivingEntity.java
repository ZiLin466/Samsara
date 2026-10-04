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
   private float pm$44(float var1) {
      if (Events.f3.m84()) {
         return Events.f3.m86() ? Events.f3.m76() : Events.f1.m69();
      } else {
         return var1;
      }
   }
}
