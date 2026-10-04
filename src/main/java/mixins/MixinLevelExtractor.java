package mixins;

import com.samsara.module.player.BedAura.BedAuraTargetRenderer;
import net.minecraft.client.renderer.extract.LevelExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelExtractor.class)
public class MixinLevelExtractor {
   @Inject(method = "extractGizmos", at = @At("HEAD"))
   private void samsara$extractBedAuraTarget(CallbackInfo callbackInfo) {
      BedAuraTargetRenderer.extract();
      com.samsara.module.combat.AutoRod.extractTarget();
   }
}
