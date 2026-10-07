package mixins;

import com.samsara.event.Events;
import com.samsara.event.impl.EventEntityOutline;
import com.samsara.module.visual.NameTags;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({EntityRenderer.class})
public abstract class MixinEntityRenderer<T extends Entity, S extends EntityRenderState> {
   @Inject(
      method = {"submitNameDisplay(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;I)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void samsara$replaceNameTag(EntityRenderState renderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, int packedLight, CallbackInfo callback) {
      if (((NameTags.ReplacementState)renderState).samsara$replaceNameTag()) callback.cancel();
   }

   @Inject(
      method = {"extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V"},
      at = {@At("TAIL")}
   )
   private void samsara$extractOutline(Entity entity, EntityRenderState renderState, float partialTick, CallbackInfo callback) {
      ((NameTags.ReplacementState)renderState).samsara$replaceNameTag(NameTags.replaces(entity));
      boolean glowing = Minecraft.getInstance().shouldEntityAppearGlowing(entity);
      EventEntityOutline entityOutlineEvent = Events.ENTITY_OUTLINE.reset(glowing ? ARGB.opaque(entity.getTeamColor()) : 0, entity);
      entityOutlineEvent.call();
      renderState.outlineColor = entityOutlineEvent.getColor();
   }
}
