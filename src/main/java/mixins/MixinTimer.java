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
   public int advanceGameTime(long var1) {
      this.deltaTicks = (float)(var1 - this.lastMs)
         / (TimerController.isDefault() ? this.targetMsptProvider.apply(this.msPerTick) : this.msPerTick / TimerController.getMultiplier());
      this.lastMs = var1;
      this.deltaTickResidual = this.deltaTickResidual + this.deltaTicks;
      int var3 = (int)this.deltaTickResidual;
      this.deltaTickResidual -= (float)var3;
      return var3;
   }
}
