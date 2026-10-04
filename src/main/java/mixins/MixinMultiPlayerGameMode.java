package mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.samsara.module.FeatureManager;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public class MixinMultiPlayerGameMode {
   @ModifyExpressionValue(method = "ensureHasSentCarriedItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;getSelectedSlot()I"))
   private int samsara$rodServerSlot(int original) {
      return FeatureManager.autoRod == null ? original : FeatureManager.autoRod.serverSlot(original);
   }

   @Inject(method = "attack", at = @At("HEAD"))
   private void samsara$recordAttack(Player player, Entity target, CallbackInfo callback) {
      com.samsara.module.visual.SessionHud.SessionTracker.attack(target);
   }
}
