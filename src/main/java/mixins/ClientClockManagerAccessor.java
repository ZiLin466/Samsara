package mixins;

import java.util.Map;
import net.minecraft.client.ClientClockManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({ClientClockManager.class})
public interface ClientClockManagerAccessor {
   @Accessor("clocks")
   Map getClocks();
}
