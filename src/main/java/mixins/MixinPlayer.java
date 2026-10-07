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
   private Vec3 samsara$preserveSprintMotion(Vec3 position, double xMultiplier, double yMultiplier, double zMultiplier) {
      KeepSprint keepSprint = FeatureManager.keepSprint;
      double sprintMultiplier = keepSprint.getSprintMotionMultiplier();
      return position.multiply(sprintMultiplier, yMultiplier, sprintMultiplier);
   }
}
