package mixins;

import com.samsara.module.FeatureManager;
import com.samsara.util.ModTextures;
import java.nio.charset.StandardCharsets;
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
   private static final Texture f204 = new ResourceTexture(
      Identifier.fromNamespaceAndPath(MixinAbstractClientPlayer.f192, MixinAbstractClientPlayer.f199),
      Identifier.fromNamespaceAndPath(MixinAbstractClientPlayer.f192, MixinAbstractClientPlayer.f200)
   );
   private static final String f199 = "cape4";
   private static final String f198 = "cape3.png";
   private static final String f195 = "cape2";
   private static final String f189 = "Cat";
   private static final String f196 = "cape2.png";
   private static final String f193 = "cape";
   @Unique
   private static final Texture f201 = new ResourceTexture(
      Identifier.fromNamespaceAndPath(MixinAbstractClientPlayer.f192, f193),
      Identifier.fromNamespaceAndPath(MixinAbstractClientPlayer.f192, MixinAbstractClientPlayer.f194)
   );
   private static final String f191 = "Sky";
   private static final String f188 = "Cryptix";
   private static final String f200 = "cape4.png";
   private static final String f192 = "samsara";
   @Unique
   private static final Texture f202 = new ResourceTexture(Identifier.fromNamespaceAndPath(f192, f195), Identifier.fromNamespaceAndPath(f192, f196));
   private static final String f194 = "cape.png";
   private static final String f197 = "cape3";
   @Unique
   private static final Texture f203 = new ResourceTexture(Identifier.fromNamespaceAndPath(f192, f197), Identifier.fromNamespaceAndPath(f192, f198));
   private static final String f190 = "Pushy";

   @Inject(
      method = {"getSkin"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void pm$14(CallbackInfoReturnable var1) {
      AbstractClientPlayer var2 = (AbstractClientPlayer)(Object)this;
      if (var2 == Minecraft.getInstance().player) {
         if (FeatureManager.f38.isEnabled()) {
            // The mod's assets are not in any resource pack, so upload the capes ourselves.
            ModTextures.register(f194);
            ModTextures.register(f196);
            ModTextures.register(f198);
            ModTextures.register(f200);
            PlayerSkin var3 = (PlayerSkin)var1.getReturnValue();
            String var4 = FeatureManager.f38.f31.m224();
            byte var5 = -1;
            int var10000 = var4.hashCode();
            if (var10000 == -1582842115) {
               if (var4.equals(f188)) {
                  var5 = 0;
               }
            } else if (var10000 == 67510) {
               if (var4.equals(f189)) {
                  var5 = 1;
               }
            } else if (var10000 == 83201) {
               if (var4.equals(f191)) {
                  var5 = 3;
               }
            } else if (var10000 == 77481087) {
               if (var4.equals(f190)) {
                  var5 = 2;
               }
            }

            switch (var5) {
               case 0:
                  var1.setReturnValue(PlayerSkin.insecure(var3.body(), f201, var3.elytra(), var3.model()));
                  break;
               case 1:
                  var1.setReturnValue(PlayerSkin.insecure(var3.body(), f202, var3.elytra(), var3.model()));
                  break;
               case 2:
                  var1.setReturnValue(PlayerSkin.insecure(var3.body(), f203, var3.elytra(), var3.model()));
                  break;
               case 3:
                  var1.setReturnValue(PlayerSkin.insecure(var3.body(), f204, var3.elytra(), var3.model()));
            }
         }
      }
   }
}
