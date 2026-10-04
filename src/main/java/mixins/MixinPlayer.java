package mixins;

import com.samsara.module.FeatureManager;
import com.samsara.module.movement.KeepSprint;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({Player.class})
public class MixinPlayer {
   @Redirect(
      method = {"causeExtraKnockback"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/phys/Vec3;multiply(DDD)Lnet/minecraft/world/phys/Vec3;"
      )
   )
   private Vec3 pm$48(Vec3 var1, double var2, double var4, double var6) {
      KeepSprint var8 = FeatureManager.f36;
      double var9 = var8.m156();
      return var1.multiply(var9, var4, var9);
   }
}
