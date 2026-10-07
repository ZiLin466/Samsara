package mixins;

import com.samsara.command.Command;
import com.samsara.command.CommandManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
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
   private static final String COMMAND_PREFIX = ".";
   private static final String EMPTY_INPUT = "";
   private static final String ARGUMENT_SEPARATOR = " ";
   @Shadow
   @Final
   private EditBox input;

   @ModifyVariable(
      method = {"updateCommandInfo"},
      at = @At("STORE"),
      ordinal = 0
   )
   private StringReader samsara$parseClientCommand(StringReader reader) {
      if (this.input.getValue().startsWith(COMMAND_PREFIX) && reader.canRead() && reader.peek() == '.') {
         reader.skip();
      }

      return reader;
   }

   @Inject(
      method = {"formatChat"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void samsara$formatClientCommand(String commandLine, int cursor, CallbackInfoReturnable callback) {
      if (this.input.getValue().startsWith(COMMAND_PREFIX)) {
         callback.setReturnValue(FormattedCharSequence.forward(commandLine, Style.EMPTY.withColor(ChatFormatting.GRAY)));
      }
   }

   @Redirect(
      method = {"updateCommandInfo"},
      at = @At(
         value = "INVOKE",
         target = "Lcom/mojang/brigadier/CommandDispatcher;getCompletionSuggestions(Lcom/mojang/brigadier/ParseResults;I)Ljava/util/concurrent/CompletableFuture;"
      )
   )
   private CompletableFuture samsara$suggestClientCommands(CommandDispatcher dispatcher, ParseResults parseResults, int cursor) {
      String inputName = this.input.getValue();
      if (!inputName.startsWith(COMMAND_PREFIX)) {
         return dispatcher.getCompletionSuggestions(parseResults, cursor);
      } else {
         String commandLine = inputName.substring(1);
         int commandCursor = Math.max(0, cursor - 1);
         commandCursor = Math.min(commandCursor, commandLine.length());
         String beforeCursor = commandLine.substring(0, commandCursor);
         if (!beforeCursor.contains(ARGUMENT_SEPARATOR)) {
            SuggestionsBuilder suggestionsBuilder = new SuggestionsBuilder(inputName, 1);
            return SharedSuggestionProvider.suggest(CommandManager.getAliases(), suggestionsBuilder);
         } else {
            String[] arguments = beforeCursor.split(ARGUMENT_SEPARATOR, -1);
            String commandName = arguments.length > 0 ? arguments[0] : EMPTY_INPUT;
            Command command = CommandManager.getCommand(commandName);
            if (command == null) {
               List aliases = CommandManager.getAliases();
               SuggestionsBuilder suggestionsBuilder = new SuggestionsBuilder(inputName, 1);
               return SharedSuggestionProvider.suggest(aliases, suggestionsBuilder);
            } else {
               String[] completionArguments;
               if (arguments.length <= 1) {
                  completionArguments = new String[0];
               } else {
                  completionArguments = Arrays.copyOfRange(arguments, 1, arguments.length);
               }

               Collection completions = command.complete(completionArguments);
               int lastSpace = beforeCursor.lastIndexOf(32);
               int replacementStart;
               if (lastSpace == -1) {
                  replacementStart = 1;
               } else {
                  replacementStart = lastSpace + 1 + 1;
               }

               SuggestionsBuilder suggestionsBuilder = new SuggestionsBuilder(inputName, replacementStart);
               return SharedSuggestionProvider.suggest(completions, suggestionsBuilder);
            }
         }
      }
   }

   @ModifyVariable(
      method = {"updateCommandInfo"},
      at = @At("STORE"),
      ordinal = 1
   )
   private boolean samsara$includeClientCommands(boolean original) {
      return original || this.input.getValue().startsWith(COMMAND_PREFIX);
   }
}
