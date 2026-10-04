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
   void setDestroyProgress(float var1);

   @Accessor("destroyDelay")
   void setDestroyDelay(int var1);

   @Accessor("destroyProgress")
   float getDestroyProgress();

   @Invoker("startPrediction")
   void invokeStartPrediction(ClientLevel var1, PredictiveAction var2);

   @Invoker("ensureHasSentCarriedItem")
   void invokeEnsureHasSentCarriedItem();
}
