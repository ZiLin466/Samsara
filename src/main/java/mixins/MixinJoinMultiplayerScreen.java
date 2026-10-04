package mixins;

import com.samsara.ui.account.AccountLoginScreen;
import com.samsara.ui.account.AccountManagerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({JoinMultiplayerScreen.class})
public class MixinJoinMultiplayerScreen {
   private static final String f209 = "Token 登录";

   @Inject(
      method = {"init"},
      at = {@At("TAIL")}
   )
   private void pm$35(CallbackInfo var1) {
      JoinMultiplayerScreen var2 = (JoinMultiplayerScreen)(Object)this;
      ((ScreenAccessor)var2)
         .invokeAddRenderableWidget(
            Button.builder(Component.literal(f209), var1x -> ((ScreenAccessor)var2).getMinecraft().gui.setScreen(
               new AccountLoginScreen(new AccountManagerScreen(var2), true)))
               .bounds(5, 5, 105, 20)
               .build()
         );
   }
}
