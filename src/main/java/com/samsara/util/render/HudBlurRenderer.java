package com.samsara.util.render;

import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL33.*;

/**
 * Same-frame backdrop blur. Captures the world once, before GUI items, and only
 * composites rounded panel regions. Never changes the game's world framebuffer
 * outside those regions and never reads pixels back to the CPU.
 */
public final class HudBlurRenderer implements AutoCloseable {
   private static final int DOWNSAMPLE = 4;
   private static final String VERTEX = """
      #version 330 core
      out vec2 uv;
      void main() {
         vec2 p = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2);
         uv = p;
         gl_Position = vec4(p * 2.0 - 1.0, 0.0, 1.0);
      }
      """;
   private static final String BLUR = """
      #version 330 core
      uniform sampler2D scene;
      uniform vec2 direction;
      uniform int taps;
      uniform float weights[33];
      uniform float offsets[33];
      in vec2 uv;
      out vec4 color;
      void main() {
         vec3 c = texture(scene, uv).rgb * weights[0];
         for (int i = 1; i <= taps; i++) {
            c += texture(scene, uv + direction * offsets[i]).rgb * weights[i];
            c += texture(scene, uv - direction * offsets[i]).rgb * weights[i];
         }
         color = vec4(c, 1.0);
      }
      """;
   private static final String PANEL = """
      #version 330 core
      uniform sampler2D scene;
      uniform vec2 viewport;
      uniform vec4 bounds;
      uniform float radius;
      uniform float opacity;
      in vec2 uv;
      out vec4 color;
      void main() {
         // gl_FragCoord and the captured texture are bottom-up; panel coordinates are top-down.
         vec2 p = vec2(gl_FragCoord.x, viewport.y - gl_FragCoord.y);
         vec2 q = abs(p - bounds.xy - bounds.zw * 0.5) - bounds.zw * 0.5 + radius;
         float d = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
         float coverage = 1.0 - smoothstep(-0.5, 0.5, d);
         if (coverage <= 0.0) discard;
         color = vec4(texture(scene, uv).rgb, coverage * opacity);
      }
      """;
   private final int[] textures = new int[3];
   private final int[] framebuffers = new int[3];
   private int blurProgram;
   private int panelProgram;
   private int vertexArray;
   private int width;
   private int height;
   private int sampleWidth;
   private int sampleHeight;
   private boolean captured;

   public record Region(float x, float y, float width, float height) { }

   public void invalidate() {
      this.captured = false;
   }

   /** Call with the current world color target bound, before any native GUI draws. */
   public void capture(int width, int height, float guiWidth) {
      capture(width, height, guiWidth, null);
   }

   public void capture(int width, int height, float guiWidth, List<Region> regions) {
      capture(width, height, guiWidth, regions, HudGlassStyle.BLUR_SIGMA);
   }

   public void capture(int width, int height, float guiWidth, List<Region> regions, float blurSigma) {
      invalidate();
      if (width <= 0 || height <= 0 || guiWidth <= 0 || !Float.isFinite(blurSigma) || blurSigma <= 0) return;
      State state = new State();
      try {
         initialize();
         glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);
         resize(width, height);
         configure();
         glBindFramebuffer(GL_READ_FRAMEBUFFER, state.drawFramebuffer);
         glBindFramebuffer(GL_DRAW_FRAMEBUFFER, this.framebuffers[0]);
         glBlitFramebuffer(0, 0, width, height, 0, 0, this.sampleWidth, this.sampleHeight,
            GL_COLOR_BUFFER_BIT, GL_LINEAR);
         glViewport(0, 0, this.sampleWidth, this.sampleHeight);
         glUseProgram(this.blurProgram);
         glUniform1i(glGetUniformLocation(this.blurProgram, "scene"), 0);
         // Sample contiguous texels. Spreading a fixed nine-tap kernel aliases cloud/block detail.
         kernel(blurSigma * (this.sampleWidth / guiWidth));
         blur(0, 1, 1f / this.sampleWidth, 0, regions, guiWidth, blurSigma * 3 + 1);
         blur(1, 2, 0, 1f / this.sampleHeight, regions, guiWidth, 1);
         this.captured = true;
      } finally {
         state.restore();
      }
   }

   /** Uses the captured world, so text, items and counts can never enter the blur. */
   public void panel(float x, float y, float w, float h, float radius, float guiWidth, float guiHeight) {
      panel(x, y, w, h, radius, guiWidth, guiHeight, 1);
   }

   public void panel(float x, float y, float w, float h, float radius, float guiWidth, float guiHeight, float opacity) {
      if (!this.captured || w <= 0 || h <= 0) return;
      State state = new State();
      try {
         configure();
         glViewport(0, 0, this.width, this.height);
         glEnable(GL_BLEND);
         glBlendEquationSeparate(GL_FUNC_ADD, GL_FUNC_ADD);
         glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
         glUseProgram(this.panelProgram);
         glBindTexture(GL_TEXTURE_2D, this.textures[2]);
         glUniform1i(glGetUniformLocation(this.panelProgram, "scene"), 0);
         glUniform1f(glGetUniformLocation(this.panelProgram, "opacity"), Math.clamp(opacity, 0, 1));
         glUniform2f(glGetUniformLocation(this.panelProgram, "viewport"), this.width, this.height);
         float sx = this.width / guiWidth;
         float sy = this.height / guiHeight;
         glUniform4f(glGetUniformLocation(this.panelProgram, "bounds"), x * sx, y * sy, w * sx, h * sy);
         glUniform1f(glGetUniformLocation(this.panelProgram, "radius"),
            Math.min(radius, Math.min(w, h) / 2) * Math.min(sx, sy));
         glEnable(GL_SCISSOR_TEST);
         int left = Math.max(0, (int)Math.floor(x * sx - 1));
         int bottom = Math.max(0, (int)Math.floor(this.height - (y + h) * sy - 1));
         int right = Math.min(this.width, (int)Math.ceil((x + w) * sx + 1));
         int top = Math.min(this.height, (int)Math.ceil(this.height - y * sy + 1));
         glScissor(left, bottom, Math.max(0, right - left), Math.max(0, top - bottom));
         glDrawArrays(GL_TRIANGLES, 0, 3);
      } finally {
         state.restore();
      }
   }

   private void initialize() {
      if (this.vertexArray != 0) return;
      this.blurProgram = program(BLUR);
      this.panelProgram = program(PANEL);
      this.vertexArray = glGenVertexArrays();
      for (int i = 0; i < 3; i++) {
         this.textures[i] = glGenTextures();
         this.framebuffers[i] = glGenFramebuffers();
      }
   }

   private void resize(int width, int height) {
      if (this.width == width && this.height == height) return;
      this.width = width;
      this.height = height;
      this.sampleWidth = Math.max(1, (width + DOWNSAMPLE - 1) / DOWNSAMPLE);
      this.sampleHeight = Math.max(1, (height + DOWNSAMPLE - 1) / DOWNSAMPLE);
      glActiveTexture(GL_TEXTURE0);
      for (int i = 0; i < 3; i++) {
         glBindTexture(GL_TEXTURE_2D, this.textures[i]);
         glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, this.sampleWidth, this.sampleHeight,
            0, GL_RGBA, GL_UNSIGNED_BYTE, 0L);
         glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
         glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
         glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
         glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
         glBindFramebuffer(GL_DRAW_FRAMEBUFFER, this.framebuffers[i]);
         glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, this.textures[i], 0);
         if (glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("HUD blur framebuffer is incomplete");
         }
      }
   }

   private void configure() {
      glActiveTexture(GL_TEXTURE0);
      glBindSampler(0, 0);
      glBindVertexArray(this.vertexArray);
      glDisable(GL_DEPTH_TEST);
      glDisable(GL_STENCIL_TEST);
      glDisable(GL_SCISSOR_TEST);
      glDisable(GL_CULL_FACE);
      glDisable(GL_BLEND);
      glDepthMask(false);
      glColorMask(true, true, true, true);
      glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
   }

   private void blur(int from, int to, float dx, float dy, List<Region> regions, float guiWidth, float margin) {
      glBindFramebuffer(GL_DRAW_FRAMEBUFFER, this.framebuffers[to]);
      glBindTexture(GL_TEXTURE_2D, this.textures[from]);
      glUniform2f(glGetUniformLocation(this.blurProgram, "direction"), dx, dy);
      if (regions == null) {
         glDisable(GL_SCISSOR_TEST);
         glDrawArrays(GL_TRIANGLES, 0, 3);
      } else {
         glEnable(GL_SCISSOR_TEST);
         float scale = this.sampleWidth / guiWidth;
         for (Region region : regions) {
            int left = Math.max(0, (int)Math.floor((region.x - margin) * scale));
            int right = Math.min(this.sampleWidth, (int)Math.ceil((region.x + region.width + margin) * scale));
            int bottom = Math.max(0, (int)Math.floor(this.sampleHeight - (region.y + region.height + margin) * scale));
            int top = Math.min(this.sampleHeight, (int)Math.ceil(this.sampleHeight - (region.y - margin) * scale));
            glScissor(left, bottom, Math.max(0, right - left), Math.max(0, top - bottom));
            glDrawArrays(GL_TRIANGLES, 0, 3);
         }
      }
   }

   /** Pair adjacent Gaussian weights using linear filtering, with at most 65 texture samples. */
   private void kernel(float sigma) {
      sigma = Math.max(.5f, sigma);
      int radius = Math.min(64, (int)Math.ceil(sigma * 3));
      float[] weights = new float[33];
      float[] offsets = new float[33];
      weights[0] = 1;
      float total = 1;
      int taps = 0;
      for (int i = 1; i <= radius; i += 2) {
         float first = (float)Math.exp(-i * i / (2f * sigma * sigma));
         float second = i + 1 <= radius ? (float)Math.exp(-(i + 1) * (i + 1) / (2f * sigma * sigma)) : 0;
         float weight = first + second;
         weights[++taps] = weight;
         offsets[taps] = i + second / weight;
         total += 2 * weight;
      }
      for (int i = 0; i <= taps; i++) weights[i] /= total;
      glUniform1i(glGetUniformLocation(this.blurProgram, "taps"), taps);
      glUniform1fv(glGetUniformLocation(this.blurProgram, "weights"), weights);
      glUniform1fv(glGetUniformLocation(this.blurProgram, "offsets"), offsets);
   }

   private static int program(String fragment) {
      int vertex = shader(GL_VERTEX_SHADER, VERTEX);
      int pixel = 0;
      int program = 0;
      try {
         pixel = shader(GL_FRAGMENT_SHADER, fragment);
         program = glCreateProgram();
         glAttachShader(program, vertex);
         glAttachShader(program, pixel);
         glLinkProgram(program);
         if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
            throw new IllegalStateException("HUD blur link: " + glGetProgramInfoLog(program));
         }
         return program;
      } catch (RuntimeException failure) {
         if (program != 0) glDeleteProgram(program);
         throw failure;
      } finally {
         glDeleteShader(vertex);
         if (pixel != 0) glDeleteShader(pixel);
      }
   }

   private static int shader(int type, String source) {
      int shader = glCreateShader(type);
      glShaderSource(shader, source);
      glCompileShader(shader);
      if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
         String log = glGetShaderInfoLog(shader);
         glDeleteShader(shader);
         throw new IllegalStateException("HUD blur shader: " + log);
      }
      return shader;
   }

   @Override
   public void close() {
      for (int i = 0; i < 3; i++) {
         if (this.textures[i] != 0) glDeleteTextures(this.textures[i]);
         if (this.framebuffers[i] != 0) glDeleteFramebuffers(this.framebuffers[i]);
         this.textures[i] = this.framebuffers[i] = 0;
      }
      if (this.blurProgram != 0) glDeleteProgram(this.blurProgram);
      if (this.panelProgram != 0) glDeleteProgram(this.panelProgram);
      if (this.vertexArray != 0) glDeleteVertexArrays(this.vertexArray);
      this.blurProgram = this.panelProgram = this.vertexArray = this.width = this.height = 0;
      invalidate();
   }

   /** Instance snapshot supports calls nested inside NanoVG's own saved GL state. */
   private static final class State {
      final int drawFramebuffer = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
      final int readFramebuffer = glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
      final int program = glGetInteger(GL_CURRENT_PROGRAM);
      final int vertexArray = glGetInteger(GL_VERTEX_ARRAY_BINDING);
      final int activeTexture = glGetInteger(GL_ACTIVE_TEXTURE);
      final int unpackBuffer = glGetInteger(GL_PIXEL_UNPACK_BUFFER_BINDING);
      final int texture;
      final int sampler;
      final int[] viewport = new int[4];
      final int[] scissor = new int[4];
      final int[] polygonMode = new int[2];
      final int[] blendFunction = {glGetInteger(GL_BLEND_SRC_RGB), glGetInteger(GL_BLEND_DST_RGB),
         glGetInteger(GL_BLEND_SRC_ALPHA), glGetInteger(GL_BLEND_DST_ALPHA)};
      final int[] blendEquation = {glGetInteger(GL_BLEND_EQUATION_RGB), glGetInteger(GL_BLEND_EQUATION_ALPHA)};
      final int[] colorMask = new int[4];
      final boolean depthMask = glGetBoolean(GL_DEPTH_WRITEMASK);
      final int[] capabilities = {GL_BLEND, GL_DEPTH_TEST, GL_STENCIL_TEST, GL_SCISSOR_TEST, GL_CULL_FACE};
      final boolean[] enabled = new boolean[5];

      State() {
         glActiveTexture(GL_TEXTURE0);
         this.texture = glGetInteger(GL_TEXTURE_BINDING_2D);
         this.sampler = glGetInteger(GL_SAMPLER_BINDING);
         glGetIntegerv(GL_VIEWPORT, this.viewport);
         glGetIntegerv(GL_SCISSOR_BOX, this.scissor);
         glGetIntegerv(GL_POLYGON_MODE, this.polygonMode);
         glGetIntegerv(GL_COLOR_WRITEMASK, this.colorMask);
         for (int i = 0; i < this.capabilities.length; i++) this.enabled[i] = glIsEnabled(this.capabilities[i]);
      }

      void restore() {
         glBindFramebuffer(GL_DRAW_FRAMEBUFFER, this.drawFramebuffer);
         glBindFramebuffer(GL_READ_FRAMEBUFFER, this.readFramebuffer);
         glUseProgram(this.program);
         glBindVertexArray(this.vertexArray);
         glBindBuffer(GL_PIXEL_UNPACK_BUFFER, this.unpackBuffer);
         glActiveTexture(GL_TEXTURE0);
         glBindTexture(GL_TEXTURE_2D, this.texture);
         glBindSampler(0, this.sampler);
         glActiveTexture(this.activeTexture);
         glViewport(this.viewport[0], this.viewport[1], this.viewport[2], this.viewport[3]);
         glScissor(this.scissor[0], this.scissor[1], this.scissor[2], this.scissor[3]);
         glPolygonMode(GL_FRONT_AND_BACK, this.polygonMode[0]);
         glBlendFuncSeparate(this.blendFunction[0], this.blendFunction[1], this.blendFunction[2], this.blendFunction[3]);
         glBlendEquationSeparate(this.blendEquation[0], this.blendEquation[1]);
         glDepthMask(this.depthMask);
         glColorMask(this.colorMask[0] != 0, this.colorMask[1] != 0, this.colorMask[2] != 0, this.colorMask[3] != 0);
         for (int i = 0; i < this.capabilities.length; i++) {
            if (this.enabled[i]) glEnable(this.capabilities[i]); else glDisable(this.capabilities[i]);
         }
      }
   }
}
