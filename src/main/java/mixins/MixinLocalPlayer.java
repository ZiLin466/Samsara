package mixins;

import com.samsara.event.Events;
import com.samsara.event.impl.EventPreMotion;
import com.samsara.event.impl.EventSlowdown;
import com.samsara.event.impl.EventSprint;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LocalPlayer.class})
public abstract class MixinLocalPlayer extends AbstractClientPlayer {
   @Shadow
   private int sprintTriggerTime;
   @Unique
   private EventPreMotion samsara$motion;
   @Unique
   private float samsara$motionYaw;
   @Unique
   private float samsara$motionPitch;

   public MixinLocalPlayer(ClientLevel var1, GameProfile var2) {
      super(var1, var2);
   }

   @ModifyExpressionValue(
      method = {"modifyInput"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/LocalPlayer;itemUseSpeedMultiplier()F"
      )
   )
   private float pm$46(float original) {
      EventSlowdown var2 = Events.f9.m92(original);
      var2.call();
      return var2.m93();
   }

   @Inject(
      method = {"tick"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pm$45(CallbackInfo var1) {
      Events.f3.m75(this.getYRot(), this.getXRot()).call();
      if (Events.f3.isCancelled()) {
         var1.cancel();
      }
   }

   // Keep vanilla's sendPosition body intact: ViaFabricPlus injects its movement
   // threshold, idle packets, position reminder and sneaking protocol fixes here.
   @Inject(method = "sendPosition", at = @At("HEAD"))
   private void samsara$preMotion(CallbackInfo ci) {
      this.samsara$motion = Events.f1.m62(this.getX(), this.getY(), this.getZ(), this.onGround(), this.horizontalCollision);
      this.samsara$motion.call();
      this.samsara$motionYaw = Events.f3.m76();
      this.samsara$motionPitch = Events.f3.m82();
   }

   @ModifyExpressionValue(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getX()D"))
   private double samsara$motionX(double original) {
      return this.samsara$motion.m63();
   }

   @ModifyExpressionValue(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getY()D"))
   private double samsara$motionY(double original) {
      return this.samsara$motion.m65();
   }

   @ModifyExpressionValue(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getZ()D"))
   private double samsara$motionZ(double original) {
      return this.samsara$motion.m67();
   }

   // 26.3 constructs Pos/PosRot from position(), not from getX/Y/Z. Substitute
   // that Vec3 too, so packet coordinates match the deltas and last-sent cache.
   @ModifyExpressionValue(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;position()Lnet/minecraft/world/phys/Vec3;"))
   private Vec3 samsara$motionPosition(Vec3 original) {
      return new Vec3(this.samsara$motion.m63(), this.samsara$motion.m65(), this.samsara$motion.m67());
   }

   @ModifyExpressionValue(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getYRot()F"))
   private float samsara$motionYaw(float original) {
      return this.samsara$motionYaw;
   }

   @ModifyExpressionValue(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getXRot()F"))
   private float samsara$motionPitch(float original) {
      return this.samsara$motionPitch;
   }

   @ModifyExpressionValue(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;onGround()Z"))
   private boolean samsara$motionOnGround(boolean original) {
      return this.samsara$motion.m71();
   }

   @ModifyExpressionValue(method = "sendPosition", at = @At(value = "FIELD", target = "Lnet/minecraft/client/player/LocalPlayer;horizontalCollision:Z"))
   private boolean samsara$motionHorizontalCollision(boolean original) {
      return this.samsara$motion.m73();
   }

   @Inject(method = "sendPosition", at = @At("RETURN"))
   private void samsara$postMotion(CallbackInfo ci) {
      Events.f2.call();
   }

   @ModifyExpressionValue(
      method = {"aiStep"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/LocalPlayer;canStartSprinting()Z"
      )
   )
   private boolean pm$47(boolean original) {
      // Read the result after ViaFabricPlus's redirect instead of replacing it.
      EventSprint var2 = Events.f12.m95(this.sprintTriggerTime, original);
      var2.call();
      return var2.m98();
   }
}
