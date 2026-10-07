package mixins;

import com.samsara.module.FeatureManager;
import com.samsara.util.ModTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.ClientAsset.ResourceTexture;
import net.minecraft.core.ClientAsset.Texture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({AbstractClientPlayer.class})
public class MixinAbstractClientPlayer {
   @Unique
   private static final Texture skyCapeTexture = new ResourceTexture(
      Identifier.fromNamespaceAndPath(MixinAbstractClientPlayer.SAMSARA_LABEL, MixinAbstractClientPlayer.CAPE4_LABEL),
      Identifier.fromNamespaceAndPath(MixinAbstractClientPlayer.SAMSARA_LABEL, MixinAbstractClientPlayer.CAPE4_PNG_LABEL)
   );
   private static final String CAPE4_LABEL = "cape4";
   private static final String CAPE3_PNG_LABEL = "cape3.png";
   private static final String CAPE2_LABEL = "cape2";
   private static final String CAT_LABEL = "Cat";
   private static final String CAPE2_PNG_LABEL = "cape2.png";
   private static final String CAPE_LABEL = "cape";
   @Unique
   private static final Texture cryptixCapeTexture = new ResourceTexture(
      Identifier.fromNamespaceAndPath(MixinAbstractClientPlayer.SAMSARA_LABEL, CAPE_LABEL),
      Identifier.fromNamespaceAndPath(MixinAbstractClientPlayer.SAMSARA_LABEL, MixinAbstractClientPlayer.CAPE_PNG_LABEL)
   );
   private static final String SKY_LABEL = "Sky";
   private static final String CRYPTIX_LABEL = "Cryptix";
   private static final String CAPE4_PNG_LABEL = "cape4.png";
   private static final String SAMSARA_LABEL = "samsara";
   @Unique
   private static final Texture catCapeTexture = new ResourceTexture(Identifier.fromNamespaceAndPath(SAMSARA_LABEL, CAPE2_LABEL), Identifier.fromNamespaceAndPath(SAMSARA_LABEL, CAPE2_PNG_LABEL));
   private static final String CAPE_PNG_LABEL = "cape.png";
   private static final String CAPE3_LABEL = "cape3";
   @Unique
   private static final Texture pushyCapeTexture = new ResourceTexture(Identifier.fromNamespaceAndPath(SAMSARA_LABEL, CAPE3_LABEL), Identifier.fromNamespaceAndPath(SAMSARA_LABEL, CAPE3_PNG_LABEL));
   private static final String PUSHY_LABEL = "Pushy";

   @Inject(
      method = {"getSkin"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void samsara$overrideCape(CallbackInfoReturnable<PlayerSkin> callback) {
      AbstractClientPlayer player = (AbstractClientPlayer)(Object)this;
      if (player == Minecraft.getInstance().player) {
         if (FeatureManager.cape.isEnabled()) {
            // The mod's assets are not in any resource pack, so upload the capes ourselves.
            ModTextures.register(CAPE_PNG_LABEL);
            ModTextures.register(CAPE2_PNG_LABEL);
            ModTextures.register(CAPE3_PNG_LABEL);
            ModTextures.register(CAPE4_PNG_LABEL);
            PlayerSkin originalSkin = callback.getReturnValue();
            Texture selectedCape = switch (FeatureManager.cape.cape.getValue()) {
               case CRYPTIX_LABEL -> cryptixCapeTexture;
               case CAT_LABEL -> catCapeTexture;
               case PUSHY_LABEL -> pushyCapeTexture;
               case SKY_LABEL -> skyCapeTexture;
               default -> null;
            };
            if (selectedCape != null) {
               callback.setReturnValue(PlayerSkin.insecure(originalSkin.body(), selectedCape, originalSkin.elytra(), originalSkin.model()));
            }
         }
      }
   }
}
