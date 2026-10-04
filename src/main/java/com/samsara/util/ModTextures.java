package com.samsara.util;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

public final class ModTextures {
   private static final Set<Identifier> REGISTERED = new HashSet<>();

   private ModTextures() {
   }

   /** Registers assets/samsara/<path> under samsara:<path>, once. */
   public static Identifier register(String path) {
      Identifier id = Identifier.fromNamespaceAndPath("samsara", path);
      if (REGISTERED.add(id)) {
         NativeImage image = read(id);
         if (image != null) {
            DynamicTexture texture = path.startsWith("textures/hud/potion/") || path.startsWith("textures/hud/nametags/")
               || path.equals("textures/hud/chest-island.png")
               ? new DynamicTexture(id::toString, image) {
               @Override public com.mojang.renderpearl.api.textures.GpuSampler getSampler() {
                  // Preserve mask antialiasing when downscaling.
                  return com.mojang.blaze3d.systems.RenderSystem.getSamplerCache()
                     .getClampToEdge(com.mojang.renderpearl.api.textures.FilterMode.LINEAR);
               }
            } : new DynamicTexture(id::toString, image);
            Minecraft.getInstance().getTextureManager().register(id, texture);
         }
      }

      return id;
   }

   /** Decodes assets/<namespace>/<path> from the mod jar, or null when missing/broken. */
   public static NativeImage read(Identifier id) {
      String resource = "/assets/" + id.getNamespace() + "/" + id.getPath();

      try (InputStream in = ModTextures.class.getResourceAsStream(resource)) {
         if (in == null) {
            System.out.println("[Samsara] texture: resource missing " + resource);
            return null;
         }

         return NativeImage.read(in);
      } catch (Exception e) {
         System.out.println("[Samsara] texture: failed to read " + resource + " (" + e + ")");
         return null;
      }
   }
}
