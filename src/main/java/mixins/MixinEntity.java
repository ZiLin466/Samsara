package mixins;

import com.samsara.event.Events;
import com.samsara.module.FeatureManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Entity.class})
public abstract class MixinEntity {
   @Overwrite
   public void moveRelative(float speed, Vec3 input) {
      Entity entity = (Entity)(Object)this;
      Vec3 movement = getInputVector(input, speed, entity.getYRot());
      if (entity == Minecraft.getInstance().player && Events.ROTATION.hasMovementCorrection()) {
         movement = getInputVector(input, speed, Events.ROTATION.getYaw());
      }

      entity.setDeltaMovement(entity.getDeltaMovement().add(movement));
   }

   @Shadow
   protected static Vec3 getInputVector(Vec3 position, float speed, float yaw) {
      throw new AssertionError();
   }

   @Shadow
   public abstract void setSwimming(boolean swimming);

   @Inject(
      method = {"updateSwimming"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void samsara$preventSwimming(CallbackInfo callback) {
      if (FeatureManager.antiSwim.isEnabled()) {
         this.setSwimming(false);
         callback.cancel();
      }
   }
}
