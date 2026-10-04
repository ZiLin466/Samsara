package mixins;

import com.samsara.module.visual.NameTags;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public class MixinNameTagRenderState implements NameTags.ReplacementState {
   @Unique private boolean samsara$replace;
   @Override public boolean samsara$replaceNameTag() { return samsara$replace; }
   @Override public void samsara$replaceNameTag(boolean replace) { samsara$replace = replace; }
}
