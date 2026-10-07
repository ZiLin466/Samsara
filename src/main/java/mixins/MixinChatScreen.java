package mixins;

import com.samsara.command.CommandManager;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ChatScreen.class})
public class MixinChatScreen {
   private static final String COMMAND_PREFIX = ".";

   @Inject(
      method = {"handleChatInput"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/components/ChatComponent;addRecentChat(Ljava/lang/String;)V",
         shift = Shift.AFTER
      )},
      cancellable = true
   )
   private void samsara$handleClientCommand(String message, boolean addToHistory, CallbackInfo callback) {
      CommandManager.dispatch(message);
      if (message.startsWith(COMMAND_PREFIX)) {
         callback.cancel();
      }
   }
}
