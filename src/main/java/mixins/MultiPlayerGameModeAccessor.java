package mixins;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({MultiPlayerGameMode.class})
public interface MultiPlayerGameModeAccessor {
   @Accessor("destroyProgress")
   void setDestroyProgress(float destroyProgress);

   @Accessor("destroyDelay")
   void setDestroyDelay(int destroyDelay);

   @Accessor("destroyProgress")
   float getDestroyProgress();

   @Invoker("startPrediction")
   void invokeStartPrediction(ClientLevel level, PredictiveAction action);

   @Invoker("ensureHasSentCarriedItem")
   void invokeEnsureHasSentCarriedItem();
}
