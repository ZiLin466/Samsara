package com.samsara.util.render;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_SAMPLES;
import static org.lwjgl.opengl.GL20.GL_STENCIL_BACK_WRITEMASK;
import static org.lwjgl.opengl.GL20.glStencilMaskSeparate;
import static org.lwjgl.opengl.GL30.*;

/** Supplies NanoVG's fill mask while drawing into a color-only render pass. */
public final class NanoVGStencilBuffer implements AutoCloseable {
   private int buffer;
   private int width;
   private int height;
   private int samples;

   public void render(int width, int height, Runnable draw) {
      int framebuffer = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
      int attachment = framebuffer == 0 ? GL_STENCIL : GL_STENCIL_ATTACHMENT;
      if (glGetFramebufferAttachmentParameteri(GL_DRAW_FRAMEBUFFER, attachment, GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE) != GL_NONE
          && glGetFramebufferAttachmentParameteri(GL_DRAW_FRAMEBUFFER, attachment, GL_FRAMEBUFFER_ATTACHMENT_STENCIL_SIZE) > 0) {
         draw.run();
         return;
      }
      if (framebuffer == 0) {
         throw new IllegalStateException("NanoVG needs a stencil-enabled window or an explicit framebuffer");
      }
      int previousBuffer = glGetInteger(GL_RENDERBUFFER_BINDING);
      try {
         int samples = glGetInteger(GL_SAMPLES);
         if (this.buffer == 0) {
            this.buffer = glGenRenderbuffers();
         }
         glBindRenderbuffer(GL_RENDERBUFFER, this.buffer);
         if (this.width != width || this.height != height || this.samples != samples) {
            if (samples > 0) {
               glRenderbufferStorageMultisample(GL_RENDERBUFFER, samples, GL_STENCIL_INDEX8, width, height);
            } else {
               glRenderbufferStorage(GL_RENDERBUFFER, GL_STENCIL_INDEX8, width, height);
            }
            this.width = width;
            this.height = height;
            this.samples = samples;
         }
      } finally {
         glBindRenderbuffer(GL_RENDERBUFFER, previousBuffer);
      }
      glFramebufferRenderbuffer(GL_DRAW_FRAMEBUFFER, GL_STENCIL_ATTACHMENT, GL_RENDERBUFFER, this.buffer);
      try {
         if (glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("NanoVG stencil attachment is incompatible with the render target");
         }
         clearStencil();
         draw.run();
      } finally {
         // The render engine owns this FBO. Never retain our attachment on its cached target.
         glBindFramebuffer(GL_DRAW_FRAMEBUFFER, framebuffer);
         glFramebufferRenderbuffer(GL_DRAW_FRAMEBUFFER, GL_STENCIL_ATTACHMENT, GL_RENDERBUFFER, 0);
      }
   }

   private static void clearStencil() {
      int clear = glGetInteger(GL_STENCIL_CLEAR_VALUE);
      int frontMask = glGetInteger(GL_STENCIL_WRITEMASK);
      int backMask = glGetInteger(GL_STENCIL_BACK_WRITEMASK);
      boolean scissor = glIsEnabled(GL_SCISSOR_TEST);
      try {
         glDisable(GL_SCISSOR_TEST);
         glStencilMask(0xFF);
         glClearStencil(0);
         glClear(GL_STENCIL_BUFFER_BIT);
      } finally {
         glClearStencil(clear);
         glStencilMaskSeparate(GL_FRONT, frontMask);
         glStencilMaskSeparate(GL_BACK, backMask);
         if (scissor) glEnable(GL_SCISSOR_TEST);
      }
   }

   @Override
   public void close() {
      if (this.buffer != 0) {
         glDeleteRenderbuffers(this.buffer);
         this.buffer = 0;
         this.width = this.height = this.samples = 0;
      }
   }
}
