package mixins;

import com.samsara.util.TimerController;
import it.unimi.dsi.fastutil.floats.FloatUnaryOperator;
import net.minecraft.client.DeltaTracker.Timer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({Timer.class})
public class MixinTimer {
   @Shadow
   private float deltaTickResidual;
   @Shadow
   private long lastMs;
   @Shadow
   private FloatUnaryOperator targetMsptProvider;
   @Shadow
   private float msPerTick;
   @Shadow
   private float deltaTicks;

   @Overwrite
   public int advanceGameTime(long lastMs) {
      this.deltaTicks = (float)(lastMs - this.lastMs)
         / (TimerController.isDefault() ? this.targetMsptProvider.apply(this.msPerTick) : this.msPerTick / TimerController.getMultiplier());
      this.lastMs = lastMs;
      this.deltaTickResidual = this.deltaTickResidual + this.deltaTicks;
      int deltaTickResidual = (int)this.deltaTickResidual;
      this.deltaTickResidual -= (float)deltaTickResidual;
      return deltaTickResidual;
   }
}
