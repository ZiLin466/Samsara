package mixins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({Minecraft.class})
public interface MinecraftAccessor {
   @Accessor("rightClickDelay")
   int getRightClickDelay();

   @Accessor("user")
   User getUser();

   @Accessor("rightClickDelay")
   void setRightClickDelay(int var1);

   @Accessor("user")
   void setUser(User var1);
}
