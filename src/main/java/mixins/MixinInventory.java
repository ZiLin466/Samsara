package mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.samsara.module.FeatureManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Inventory.class)
public class MixinInventory {
   @ModifyExpressionValue(method = {"removeFromSelected", "tick", "getSelectedItem", "setSelectedItem"},
      at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Inventory;selected:I", opcode = Opcodes.GETFIELD))
   private int samsara$rodInteractionSlot(int original) {
      var inventory = (Inventory)(Object)this;
      return inventory.player == Minecraft.getInstance().player && FeatureManager.autoRod != null
         ? FeatureManager.autoRod.serverSlot(original) : original;
   }
}
