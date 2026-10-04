package mixins;

import com.samsara.event.Events;
import com.samsara.event.impl.EventSound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundEngine.PlayResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({SoundEngine.class})
public class MixinSoundEngine {
   @Inject(
      method = {"play"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pm$42(SoundInstance var1, CallbackInfoReturnable var2) {
      EventSound var3 = Events.f16.m45(var1.getIdentifier());
      var3.call();
      if (var3.isCancelled()) {
         var2.setReturnValue(PlayResult.NOT_STARTED);
      }
   }
}
