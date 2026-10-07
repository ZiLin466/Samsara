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
   private static final String TOKEN_LOGIN_LABEL = "Token 登录";

   @Inject(
      method = {"init"},
      at = {@At("TAIL")}
   )
   private void samsara$addTokenLoginButton(CallbackInfo callback) {
      JoinMultiplayerScreen parent = (JoinMultiplayerScreen)(Object)this;
      ((ScreenAccessor)parent)
         .invokeAddRenderableWidget(
            Button.builder(Component.literal(TOKEN_LOGIN_LABEL), button -> ((ScreenAccessor)parent).getMinecraft().gui.setScreen(
               new AccountLoginScreen(new AccountManagerScreen(parent), true)))
               .bounds(5, 5, 105, 20)
               .build()
         );
   }
}
