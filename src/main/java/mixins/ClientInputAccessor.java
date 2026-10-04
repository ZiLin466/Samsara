package mixins;

import net.minecraft.client.player.ClientInput;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({ClientInput.class})
public interface ClientInputAccessor {
   @Accessor("moveVector")
   Vec2 getMoveVector();

   @Accessor("moveVector")
   void setMoveVector(Vec2 var1);
}
