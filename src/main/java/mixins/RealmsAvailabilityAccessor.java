package mixins;

import com.mojang.realmsclient.RealmsAvailability;
import java.util.concurrent.CompletableFuture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RealmsAvailability.class)
public interface RealmsAvailabilityAccessor {
   @Accessor("future") static void samsara$reset(CompletableFuture<RealmsAvailability.Result> future) { throw new AssertionError(); }
}
