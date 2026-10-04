package mixins;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GuiGraphicsExtractor.class)
public interface GuiGraphicsExtractorAccessor {
   @Invoker("innerBlit")
   // The Identifier overload uses x0,x1,y0,y1; the GPU-view overload uses x0,y0,x1,y1.
   void samsara$blitTinted(RenderPipeline pipeline, Identifier texture, int x0, int x1, int y0, int y1,
      float u0, float u1, float v0, float v1, int color);
}
