package mixins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({Screen.class})
public interface ScreenAccessor {
   @Invoker("addRenderableWidget")
   GuiEventListener invokeAddRenderableWidget(GuiEventListener var1);

   @Accessor("minecraft")
   Minecraft getMinecraft();
}
