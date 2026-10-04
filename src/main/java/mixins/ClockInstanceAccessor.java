package mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = {"net.minecraft.client.ClientClockManager$ClientClockInstance"})
public interface ClockInstanceAccessor {
   @Accessor("totalTicks")
   long getTotalTicks();

   @Accessor("totalTicks")
   void setTotalTicks(long var1);
}
