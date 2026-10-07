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
   private void samsara$dispatchSound(SoundInstance sound, CallbackInfoReturnable callback) {
      EventSound soundEvent = Events.SOUND.reset(sound.getIdentifier());
      soundEvent.call();
      if (soundEvent.isCancelled()) {
         callback.setReturnValue(PlayResult.NOT_STARTED);
      }
   }
}
