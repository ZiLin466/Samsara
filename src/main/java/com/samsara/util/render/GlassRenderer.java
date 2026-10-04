package com.samsara.util.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.nanovg.NanoVGGL3;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/**
 * Copies the blurred main framebuffer into a NanoVG texture using glBlitFramebuffer,
 * preserving NanoVG's texture-binding cache. GL objects are allocated manually
 * because NVGLUFramebuffer accessors fail under the game's class loader.
 */
public final class GlassRenderer {
   private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("samsara-glass");
   private static int texId;
   private static int fboId;
   private static int nvgImage;
   private static boolean requested;
   private static boolean failed;
   private static boolean loggedCapture;
   private static int fbWidth;
   private static int fbHeight;

   private GlassRenderer() {}

   public static void ensure(long vg) {
      if (failed) {
         return;
      }
      try {
         RenderTarget target = Minecraft.getInstance().gameRenderer.mainRenderTarget();
         if (target == null || target.width <= 0 || target.height <= 0) {
            return;
         }
         if (texId != 0 && fbWidth == target.width && fbHeight == target.height) {
            return;
         }
         destroy(vg);
         fbWidth = target.width;
         fbHeight = target.height;

         texId = GL11.glGenTextures();
         GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);
         GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, fbWidth, fbHeight, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (java.nio.ByteBuffer) null);
         GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
         GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
         GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL30.GL_CLAMP_TO_EDGE);
         GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL30.GL_CLAMP_TO_EDGE);
         GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

         fboId = GL30.glGenFramebuffers();
         GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboId);
         GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, texId, 0);
         GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);

         nvgImage = NanoVGGL3.nvglCreateImageFromHandle(vg, texId, fbWidth, fbHeight, NanoVG.NVG_IMAGE_FLIPY);
         if (nvgImage == 0) {
            LOG.error("[glass] nvglCreateImageFromHandle failed");
            destroy(vg);
            failed = true;
            return;
         }
         LOG.info("[glass] capture framebuffer ready {}x{} fbo={} tex={} img={}", fbWidth, fbHeight, fboId, texId, nvgImage);
      } catch (Throwable t) {
         LOG.error("[glass] ensure failed", t);
         destroy(vg);
         failed = true;
      }
   }

   public static void request() {
      requested = true;
   }

   /**
    * Blits the main render target into the NanoVG framebuffer. Must run while a render
    * pass over the main target is active (GL_DRAW_FRAMEBUFFER is bound to it) and before
    * NanoVG flushes, so panels sample this frame's blurred background.
    */
   public static void capture() {
      if (!requested || fboId == 0) {
         return;
      }
      requested = false;
      try {
         int mainFbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
         GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, mainFbo);
         GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, fboId);
         GL30.glBlitFramebuffer(0, 0, fbWidth, fbHeight, 0, 0, fbWidth, fbHeight, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
         GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, mainFbo);
         GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, mainFbo);
         if (!loggedCapture) {
            loggedCapture = true;
            LOG.info("[glass] blit captured {}x{} from fbo {}", fbWidth, fbHeight, mainFbo);
         }
      } catch (Throwable t) {
         LOG.error("[glass] capture failed", t);
         failed = true;
      }
   }

   public static boolean isReady() {
      return !failed && nvgImage != 0;
   }

   public static int image() {
      return nvgImage;
   }

   public static void destroy(long vg) {
      if (nvgImage != 0) {
         try {
            NanoVG.nvgDeleteImage(vg, nvgImage);
         } catch (Throwable ignored) {
         }
         nvgImage = 0;
      }
      if (fboId != 0) {
         GL30.glDeleteFramebuffers(fboId);
         fboId = 0;
      }
      if (texId != 0) {
         GL11.glDeleteTextures(texId);
         texId = 0;
      }
   }
}
