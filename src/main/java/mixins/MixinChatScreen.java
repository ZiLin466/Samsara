package mixins;

import com.samsara.command.CommandManager;
import java.nio.charset.StandardCharsets;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ChatScreen.class})
public class MixinChatScreen {
   private static final String f205 = ".";

   @Inject(
      method = {"handleChatInput"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/components/ChatComponent;addRecentChat(Ljava/lang/String;)V",
         shift = Shift.AFTER
      )},
      cancellable = true
   )
   private void pm$15(String var1, boolean var2, CallbackInfo var3) {
      CommandManager.dispatch(var1);
      if (var1.startsWith(f205)) {
         var3.cancel();
      }
   }
}
