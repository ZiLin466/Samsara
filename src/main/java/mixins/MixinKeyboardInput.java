package mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.samsara.event.Events;
import com.samsara.event.impl.EventMoveInput;
import com.samsara.event.impl.EventPostMoveInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({KeyboardInput.class})
public class MixinKeyboardInput {
   @ModifyExpressionValue(method = "tick", at = @At(value = "NEW", target = "(ZZZZZZZ)Lnet/minecraft/world/entity/player/Input;"))
   private Input samsara$movementInput(Input var3) {
      EventMoveInput var4 = Events.f7.m47(var3.forward(), var3.backward(), var3.left(), var3.right(), var3.jump(), var3.shift(), var3.sprint());
      var4.call();
      return new Input(var4.m48(), var4.m49(), var4.m50(), var4.m51(), var4.m52(), var4.m53(), var4.m54());
   }

   @Inject(
      method = {"tick"},
      at = {@At("TAIL")}
   )
   private void pm$37(CallbackInfo var1) {
      EventPostMoveInput var2 = Events.f8;
      var2.call();
   }
}
