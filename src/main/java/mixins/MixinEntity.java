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
   public void moveRelative(float var1, Vec3 var2) {
      Entity var3 = (Entity)(Object)this;
      Vec3 var4 = getInputVector(var2, var1, var3.getYRot());
      if (var3 == Minecraft.getInstance().player && Events.f3.m84()) {
         var4 = getInputVector(var2, var1, Events.f3.m76());
      }

      var3.setDeltaMovement(var3.getDeltaMovement().add(var4));
   }

   @Shadow
   protected static Vec3 getInputVector(Vec3 var0, float var1, float var2) {
      throw new AssertionError();
   }

   @Shadow
   public abstract void setSwimming(boolean var1);

   @Inject(
      method = {"updateSwimming"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pm$43(CallbackInfo var1) {
      if (FeatureManager.f37.isEnabled()) {
         this.setSwimming(false);
         var1.cancel();
      }
   }
}
