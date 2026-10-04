package mixins;

import com.mojang.realmsclient.client.RealmsClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RealmsClient.class)
public interface RealmsSessionAccessor {
   @Accessor("realmsClientInstance") static void samsara$reset(RealmsClient client) { throw new AssertionError(); }
}
