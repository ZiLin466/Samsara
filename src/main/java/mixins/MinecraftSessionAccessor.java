package mixins;

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.services.ProfileResult;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface MinecraftSessionAccessor {
   @Mutable @Accessor("services") void samsara$setServices(net.minecraft.server.Services services);
   @Mutable @Accessor("user") void samsara$setUser(User user);
   @Accessor("userApiService") UserApiService samsara$getUserApiService();
   @Mutable @Accessor("userApiService") void samsara$setUserApiService(UserApiService service);
   @Accessor("profileFuture") CompletableFuture<ProfileResult> samsara$getProfile();
   @Mutable @Accessor("profileFuture") void samsara$setProfile(CompletableFuture<ProfileResult> profile);
   @Accessor("userPropertiesFuture") CompletableFuture<UserApiService.UserProperties> samsara$getProperties();
   @Mutable @Accessor("userPropertiesFuture") void samsara$setProperties(CompletableFuture<UserApiService.UserProperties> properties);
   @Mutable @Accessor("profileKeyPairManager") void samsara$setKeys(ProfileKeyPairManager keys);
}
