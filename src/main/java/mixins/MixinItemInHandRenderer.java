package mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.samsara.module.FeatureManager;
import com.samsara.module.visual.Animations.BlockingAnimation;
import com.samsara.module.visual.Animations;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class MixinItemInHandRenderer {
   @Shadow private void applyItemArmTransform(PoseStack pose, HumanoidArm arm, float equip) { }

   @Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true)
   private void samsara$blocking(PlayerRenderState player, FirstPersonHandsAndItemsRenderState hands,
      float partialTick, float pitch, InteractionHand hand, float swing, ItemStack item, float equip,
      PoseStack pose, SubmitNodeCollector collector, int light, CallbackInfo callback) {
      Animations animations = FeatureManager.animations;
      if (animations == null || !animations.shouldBlock(hand, item) || hands.isScoping || player.avatarRenderState == null) return;
      HumanoidArm arm = player.avatarRenderState.mainArm;
      pose.pushPose();
      try {
         applyItemArmTransform(pose, arm, equip);
         samsara$viewModel(pose, animations);
         BlockingAnimation.transform(pose, arm, equip, swing, animations.blockingAnimation.getValue(),
            (float)animations.blockY.getValue(), (float)animations.swingScale.getValue());
         hands.mainHandRenderState.submit(pose, collector, light, OverlayTexture.NO_OVERLAY, 0);
      } finally { pose.popPose(); }
      callback.cancel();
   }

   @WrapOperation(method = "submitArmWithItem", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"))
   private void samsara$viewModel(ItemStackRenderState item, PoseStack pose, SubmitNodeCollector collector,
      int light, int overlay, int outline, Operation<Void> original,
      PlayerRenderState player, FirstPersonHandsAndItemsRenderState hands, float partialTick, float pitch,
      InteractionHand hand) {
      Animations animations = FeatureManager.animations;
      pose.pushPose();
      try {
         if (animations != null && animations.isEnabled() && hand == InteractionHand.MAIN_HAND) samsara$viewModel(pose, animations);
         original.call(item, pose, collector, light, overlay, outline);
      } finally { pose.popPose(); }
   }

   @Unique
   private static void samsara$viewModel(PoseStack pose, Animations animations) {
      pose.translate((float)animations.x.getValue(), (float)animations.y.getValue(), (float)animations.z.getValue());
      float scale = (float)animations.scale.getValue();
      pose.scale(scale, scale, scale);
   }
}
