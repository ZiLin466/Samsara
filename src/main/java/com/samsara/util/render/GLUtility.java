package com.samsara.util.render;

import com.mojang.renderpearl.backend.opengl.GlStateManager;
import org.lwjgl.opengl.GL;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_DEPTH_WRITEMASK;
import static org.lwjgl.opengl.GL11.GL_FRONT_AND_BACK;
import static org.lwjgl.opengl.GL11.GL_POLYGON_MODE;
import static org.lwjgl.opengl.GL11.GL_SCISSOR_BOX;
import static org.lwjgl.opengl.GL11.GL_SCISSOR_TEST;
import static org.lwjgl.opengl.GL11.GL_STENCIL_TEST;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_BINDING_2D;
import static org.lwjgl.opengl.GL11.GL_UNPACK_ALIGNMENT;
import static org.lwjgl.opengl.GL11.GL_UNPACK_ROW_LENGTH;
import static org.lwjgl.opengl.GL11.GL_UNPACK_SKIP_PIXELS;
import static org.lwjgl.opengl.GL11.GL_UNPACK_SKIP_ROWS;
import static org.lwjgl.opengl.GL11.GL_VIEWPORT;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDepthMask;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glGetBoolean;
import static org.lwjgl.opengl.GL11.glGetIntegerv;
import static org.lwjgl.opengl.GL11.glIsEnabled;
import static org.lwjgl.opengl.GL11.glPixelStorei;
import static org.lwjgl.opengl.GL11.glPolygonMode;
import static org.lwjgl.opengl.GL11.glScissor;
import static org.lwjgl.opengl.GL13.GL_ACTIVE_TEXTURE;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL14.GL_BLEND_DST_ALPHA;
import static org.lwjgl.opengl.GL14.GL_BLEND_DST_RGB;
import static org.lwjgl.opengl.GL14.GL_BLEND_SRC_ALPHA;
import static org.lwjgl.opengl.GL14.GL_BLEND_SRC_RGB;
import static org.lwjgl.opengl.GL14.glBlendFuncSeparate;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER_BINDING;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL20.GL_BLEND_EQUATION_ALPHA;
import static org.lwjgl.opengl.GL20.GL_BLEND_EQUATION_RGB;
import static org.lwjgl.opengl.GL20.GL_CURRENT_PROGRAM;
import static org.lwjgl.opengl.GL20.glBlendEquationSeparate;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL30.GL_MAJOR_VERSION;
import static org.lwjgl.opengl.GL30.GL_MINOR_VERSION;
import static org.lwjgl.opengl.GL30.GL_VERTEX_ARRAY_BINDING;
import static org.lwjgl.opengl.GL30.GL_DRAW_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_READ_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_DRAW_FRAMEBUFFER_BINDING;
import static org.lwjgl.opengl.GL30.GL_READ_FRAMEBUFFER_BINDING;
import static org.lwjgl.opengl.GL21.GL_PIXEL_UNPACK_BUFFER;
import static org.lwjgl.opengl.GL21.GL_PIXEL_UNPACK_BUFFER_BINDING;
import static org.lwjgl.opengl.GL11.GL_COLOR_WRITEMASK;
import static org.lwjgl.opengl.GL11.glColorMask;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL31.GL_PRIMITIVE_RESTART;
import static org.lwjgl.opengl.GL33.GL_SAMPLER_BINDING;
import static org.lwjgl.opengl.GL33.glBindSampler;

public final class GLUtility {

   private static final int[] lastActiveTexture = new int[1];
   private static final int[] lastProgram = new int[1];
   private static final int[] lastTexture = new int[1];
   private static final int[] lastSampler = new int[1];
   private static final int[] lastArrayBuffer = new int[1];
   private static final int[] lastVertexArrayObject = new int[1];
   private static final int[] lastPolygonMode = new int[2];
   private static final int[] lastViewport = new int[4];
   private static final int[] lastScissorBox = new int[4];
   private static final int[] lastBlendSrcRgb = new int[1];
   private static final int[] lastBlendDstRgb = new int[1];
   private static final int[] lastBlendSrcAlpha = new int[1];
   private static final int[] lastBlendDstAlpha = new int[1];
   private static final int[] lastBlendEquationRgb = new int[1];
   private static final int[] lastBlendEquationAlpha = new int[1];
   private static final int[] lastUnpackAlignment = new int[1];
   private static final int[] lastUnpackRowLength = new int[1];
   private static final int[] lastUnpackSkipRows = new int[1];
   private static final int[] lastUnpackSkipPixels = new int[1];
   private static final int[] lastDrawFramebuffer = new int[1];
   private static final int[] lastReadFramebuffer = new int[1];
   private static final int[] lastUnpackBuffer = new int[1];
   private static final int[] lastColorMask = new int[4];

   private static boolean lastEnableBlend;
   private static boolean lastEnableCullFace;
   private static boolean lastEnableDepthTest;
   private static boolean lastEnableStencilTest;
   private static boolean lastEnableScissorTest;
   private static boolean lastEnablePrimitiveRestart;

   private static boolean lastDepthMask;

   private static int glVersion = -1;

   private GLUtility() {
   }

   public static void setup() {
      int[] major = new int[1];
      int[] minor = new int[1];
      glGetIntegerv(GL_MAJOR_VERSION, major);
      glGetIntegerv(GL_MINOR_VERSION, minor);

      glVersion = major[0] * 100 + minor[0] * 10;
   }

   public static void push() {
      if (glVersion == -1) {
         throw new IllegalStateException("GlStateUtility.setup(glVersion) must be called before push/pop!");
      }

      glGetIntegerv(GL_ACTIVE_TEXTURE, lastActiveTexture);
      glActiveTexture(GL_TEXTURE0);

      glGetIntegerv(GL_CURRENT_PROGRAM, lastProgram);
      glGetIntegerv(GL_TEXTURE_BINDING_2D, lastTexture);

      if (glVersion >= 330 || GL.getCapabilities().GL_ARB_sampler_objects) {
         glGetIntegerv(GL_SAMPLER_BINDING, lastSampler);
      }

      glGetIntegerv(GL_ARRAY_BUFFER_BINDING, lastArrayBuffer);
      glGetIntegerv(GL_VERTEX_ARRAY_BINDING, lastVertexArrayObject);

      if (glVersion >= 200) {
         glGetIntegerv(GL_POLYGON_MODE, lastPolygonMode);
      }

      glGetIntegerv(GL_VIEWPORT, lastViewport);
      glGetIntegerv(GL_SCISSOR_BOX, lastScissorBox);

      glGetIntegerv(GL_BLEND_SRC_RGB, lastBlendSrcRgb);
      glGetIntegerv(GL_BLEND_DST_RGB, lastBlendDstRgb);
      glGetIntegerv(GL_BLEND_SRC_ALPHA, lastBlendSrcAlpha);
      glGetIntegerv(GL_BLEND_DST_ALPHA, lastBlendDstAlpha);
      glGetIntegerv(GL_BLEND_EQUATION_RGB, lastBlendEquationRgb);
      glGetIntegerv(GL_BLEND_EQUATION_ALPHA, lastBlendEquationAlpha);
      glGetIntegerv(GL_UNPACK_ALIGNMENT, lastUnpackAlignment);
      glGetIntegerv(GL_UNPACK_ROW_LENGTH, lastUnpackRowLength);
      glGetIntegerv(GL_UNPACK_SKIP_ROWS, lastUnpackSkipRows);
      glGetIntegerv(GL_UNPACK_SKIP_PIXELS, lastUnpackSkipPixels);
      glGetIntegerv(GL_DRAW_FRAMEBUFFER_BINDING, lastDrawFramebuffer);
      glGetIntegerv(GL_READ_FRAMEBUFFER_BINDING, lastReadFramebuffer);
      glGetIntegerv(GL_PIXEL_UNPACK_BUFFER_BINDING, lastUnpackBuffer);
      glGetIntegerv(GL_COLOR_WRITEMASK, lastColorMask);

      lastEnableBlend = glIsEnabled(GL_BLEND);
      lastEnableCullFace = glIsEnabled(GL_CULL_FACE);
      lastEnableDepthTest = glIsEnabled(GL_DEPTH_TEST);
      lastEnableStencilTest = glIsEnabled(GL_STENCIL_TEST);
      lastEnableScissorTest = glIsEnabled(GL_SCISSOR_TEST);

      if (glVersion >= 310) {
         lastEnablePrimitiveRestart = glIsEnabled(GL_PRIMITIVE_RESTART);
      }

      lastDepthMask = glGetBoolean(GL_DEPTH_WRITEMASK);
   }

   public static void pop() {
      if (glVersion == -1) {
         throw new IllegalStateException("GlStateUtility.setup(glVersion) must be called before push/pop!");
      }

      glUseProgram(lastProgram[0]);
      GlStateManager._glBindFramebuffer(GL_DRAW_FRAMEBUFFER, lastDrawFramebuffer[0]);
      GlStateManager._glBindFramebuffer(GL_READ_FRAMEBUFFER, lastReadFramebuffer[0]);
      glActiveTexture(GL_TEXTURE0);
      glBindTexture(GL_TEXTURE_2D, lastTexture[0]);

      if (glVersion >= 330 || GL.getCapabilities().GL_ARB_sampler_objects) {
         glBindSampler(0, lastSampler[0]);
      }

      glActiveTexture(lastActiveTexture[0]);
      glBindVertexArray(lastVertexArrayObject[0]);
      glBindBuffer(GL_ARRAY_BUFFER, lastArrayBuffer[0]);
      glBindBuffer(GL_PIXEL_UNPACK_BUFFER, lastUnpackBuffer[0]);

      glBlendEquationSeparate(lastBlendEquationRgb[0], lastBlendEquationAlpha[0]);
      glBlendFuncSeparate(lastBlendSrcRgb[0], lastBlendDstRgb[0], lastBlendSrcAlpha[0], lastBlendDstAlpha[0]);
      glPixelStorei(GL_UNPACK_ALIGNMENT, lastUnpackAlignment[0]);
      glPixelStorei(GL_UNPACK_ROW_LENGTH, lastUnpackRowLength[0]);
      glPixelStorei(GL_UNPACK_SKIP_ROWS, lastUnpackSkipRows[0]);
      glPixelStorei(GL_UNPACK_SKIP_PIXELS, lastUnpackSkipPixels[0]);

      setGlState(GL_BLEND, lastEnableBlend);
      setGlState(GL_CULL_FACE, lastEnableCullFace);
      setGlState(GL_DEPTH_TEST, lastEnableDepthTest);
      setGlState(GL_STENCIL_TEST, lastEnableStencilTest);
      setGlState(GL_SCISSOR_TEST, lastEnableScissorTest);

      if (glVersion >= 310) {
         setGlState(GL_PRIMITIVE_RESTART, lastEnablePrimitiveRestart);
      }

      if (glVersion >= 200) {
         glPolygonMode(GL_FRONT_AND_BACK, lastPolygonMode[0]);
      }

      // Render passes update the engine's viewport cache. Restore through that same path.
      GlStateManager._viewport(lastViewport[0], lastViewport[1], lastViewport[2], lastViewport[3]);
      glScissor(lastScissorBox[0], lastScissorBox[1], lastScissorBox[2], lastScissorBox[3]);

      glDepthMask(lastDepthMask);
      glColorMask(lastColorMask[0] != 0, lastColorMask[1] != 0, lastColorMask[2] != 0, lastColorMask[3] != 0);
   }

   /**
    * MC's GUI pipeline can leave a sampler object and pixel-store offsets bound
    * when NanoVG flushes its glyph atlas. NanoVG sets texture parameters on the
    * texture object, so an external sampler would silently override them.
    */
   public static void prepareNanoVG() {
      glActiveTexture(GL_TEXTURE0);
      glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);
      if (glVersion >= 330 || GL.getCapabilities().GL_ARB_sampler_objects) {
         glBindSampler(0, 0);
      }
      glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
      glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
      glPixelStorei(GL_UNPACK_SKIP_ROWS, 0);
      glPixelStorei(GL_UNPACK_SKIP_PIXELS, 0);
   }

   private static void setGlState(int capability, boolean enabled) {
      if (enabled) {
         glEnable(capability);
      } else {
         glDisable(capability);
      }
   }
}
