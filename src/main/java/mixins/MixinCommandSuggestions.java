package mixins;

import com.samsara.command.Command;
import com.samsara.command.CommandManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({CommandSuggestions.class})
public class MixinCommandSuggestions {
   private static final String f206 = ".";
   private static final String f208 = new String(new byte[0], StandardCharsets.UTF_8);
   private static final String f207 = " ";
   @Shadow
   @Final
   private EditBox input;

   @ModifyVariable(
      method = {"updateCommandInfo"},
      at = @At("STORE"),
      ordinal = 0
   )
   private StringReader pm$17(StringReader var1) {
      if (this.input.getValue().startsWith(f206) && var1.canRead() && var1.peek() == '.') {
         var1.skip();
      }

      return var1;
   }

   @Inject(
      method = {"formatChat"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pm$19(String var1, int var2, CallbackInfoReturnable var3) {
      if (this.input.getValue().startsWith(f206)) {
         var3.setReturnValue(FormattedCharSequence.forward(var1, Style.EMPTY.withColor(ChatFormatting.GRAY)));
      }
   }

   @Redirect(
      method = {"updateCommandInfo"},
      at = @At(
         value = "INVOKE",
         target = "Lcom/mojang/brigadier/CommandDispatcher;getCompletionSuggestions(Lcom/mojang/brigadier/ParseResults;I)Ljava/util/concurrent/CompletableFuture;"
      )
   )
   private CompletableFuture pm$18(CommandDispatcher var1, ParseResults var2, int var3) {
      String var4 = this.input.getValue();
      if (!var4.startsWith(f206)) {
         return var1.getCompletionSuggestions(var2, var3);
      } else {
         String var5 = var4.substring(1);
         int var6 = Math.max(0, var3 - 1);
         var6 = Math.min(var6, var5.length());
         String var7 = var5.substring(0, var6);
         if (!var7.contains(f207)) {
            SuggestionsBuilder var17 = new SuggestionsBuilder(var4, 1);
            return SharedSuggestionProvider.suggest(CommandManager.m9(), var17);
         } else {
            String[] var8 = var7.split(f207, -1);
            String var9 = var8.length > 0 ? var8[0] : f208;
            Command var10 = CommandManager.getCommand(var9);
            if (var10 == null) {
               List var18 = CommandManager.m9();
               SuggestionsBuilder var19 = new SuggestionsBuilder(var4, 1);
               return SharedSuggestionProvider.suggest(var18, var19);
            } else {
               String[] var11;
               if (var8.length <= 1) {
                  var11 = new String[0];
               } else {
                  var11 = Arrays.copyOfRange(var8, 1, var8.length);
               }

               Collection var12 = var10.complete(var11);
               int var13 = var7.lastIndexOf(32);
               int var14;
               if (var13 == -1) {
                  var14 = 1;
               } else {
                  var14 = var13 + 1 + 1;
               }

               SuggestionsBuilder var15 = new SuggestionsBuilder(var4, var14);
               return SharedSuggestionProvider.suggest(var12, var15);
            }
         }
      }
   }

   @ModifyVariable(
      method = {"updateCommandInfo"},
      at = @At("STORE"),
      ordinal = 1
   )
   private boolean pm$16(boolean var1) {
      return var1 || this.input.getValue().startsWith(f206);
   }
}
