package com.samsara.util.render;

import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.pipeline.*;
import com.mojang.renderpearl.api.textures.*;
import java.nio.ByteBuffer;
import java.util.*;
import net.minecraft.resources.Identifier;
import org.lwjgl.system.MemoryUtil;

/** Same-frame world blur using the active GPU device, before GUI items enter the target. */
public final class GpuHudBlurRenderer implements AutoCloseable {
   private static final int DOWNSAMPLE = 4;
   private static final int UNIFORM_FLOATS = 148;
   private static final String VERTEX = """
      #version 450
      layout(location=0) out vec2 uv;
      void main() {
         vec2 p=vec2((gl_VertexIndex << 1) & 2, gl_VertexIndex & 2);
         uv=p;
         gl_Position=vec4(p*2.0-1.0,0,1);
      }
      """;
   private static final String FRAGMENT = """
      #version 450
      layout(std140) uniform Blur { vec4 data[37]; };
      uniform sampler2D Scene;
      layout(location=0) in vec2 uv;
      layout(location=0) out vec4 color;
      void main() {
         if (data[0].w < 0.5) {
            vec3 c=texture(Scene,uv).rgb*data[4].x;
            for (int i=1;i<=int(data[0].z);i++) {
               vec2 offset=data[0].xy*data[4+i].y;
               c+=(texture(Scene,uv+offset).rgb+texture(Scene,uv-offset).rgb)*data[4+i].x;
            }
            color=vec4(c,1);
         } else {
            vec2 p=uv*data[1].xy;
            p.y=data[1].y-p.y;
            vec2 q=abs(p-data[2].xy-data[2].zw*0.5)-data[2].zw*0.5+data[3].x;
            float distance=length(max(q,0))+min(max(q.x,q.y),0)-data[3].x;
            float coverage=1.0-smoothstep(-0.5,0.5,distance);
            color=vec4(texture(Scene,uv).rgb,coverage*data[3].y);
         }
      }
      """;
   private final GpuDevice device;
   private final GpuTexture[] textures = new GpuTexture[3];
   private final GpuTextureView[] views = new GpuTextureView[3];
   private final Map<PipelineKey,CompiledRenderPipeline> pipelines = new HashMap<>();
   private GpuSampler sampler;
   private int width, height, sampleWidth, sampleHeight;
   private boolean captured;

   public GpuHudBlurRenderer(GpuDevice device) { this.device=Objects.requireNonNull(device); }
   public void invalidate() { captured=false; }

   public void capture(GpuTextureView source, float guiWidth, List<HudBlurRenderer.Region> regions) {
      invalidate();
      int w=source.getWidth(0),h=source.getHeight(0);
      if (w<=0 || h<=0 || guiWidth<=0) return;
      resize(w,h);
      var encoder=device.createCommandEncoder();
      float[] copy=new float[UNIFORM_FLOATS]; copy[16]=1;
      draw(encoder,source,views[0],copy,false,null,guiWidth,0);
      float[] kernel=kernel(HudGlassStyle.BLUR_SIGMA*sampleWidth/guiWidth);
      kernel[0]=1f/sampleWidth;
      draw(encoder,views[0],views[1],kernel,false,regions,guiWidth,HudGlassStyle.BLUR_PADDING);
      kernel[0]=0; kernel[1]=1f/sampleHeight;
      draw(encoder,views[1],views[2],kernel,false,regions,guiWidth,1);
      captured=true;
   }

   public void panel(GpuTextureView target,float x,float y,float w,float h,float radius,
                     float guiWidth,float guiHeight,float opacity) {
      if (!captured || w<=0 || h<=0 || guiWidth<=0 || guiHeight<=0) return;
      float sx=target.getWidth(0)/guiWidth,sy=target.getHeight(0)/guiHeight;
      float[] data=new float[UNIFORM_FLOATS]; data[3]=1;
      data[4]=target.getWidth(0); data[5]=target.getHeight(0);
      data[8]=x*sx; data[9]=y*sy; data[10]=w*sx; data[11]=h*sy;
      data[12]=Math.min(radius,Math.min(w,h)/2)*Math.min(sx,sy); data[13]=Math.clamp(opacity,0,1);
      draw(device.createCommandEncoder(),views[2],target,data,true,
         List.of(new HudBlurRenderer.Region(x,y*sy/sx,w,h*sy/sx)),guiWidth,1/sx);
   }

   private void resize(int w,int h) {
      if (w==width && h==height && views[0]!=null) return;
      closeTextures(); width=w; height=h;
      sampleWidth=Math.max(1,(w+DOWNSAMPLE-1)/DOWNSAMPLE);
      sampleHeight=Math.max(1,(h+DOWNSAMPLE-1)/DOWNSAMPLE);
      if (sampler==null) sampler=device.createSampler(AddressMode.CLAMP_TO_EDGE,AddressMode.CLAMP_TO_EDGE,
         FilterMode.LINEAR,FilterMode.LINEAR,1,OptionalDouble.empty());
      for (int i=0;i<3;i++) {
         textures[i]=device.createTexture("samsara/hud-blur-"+i,GpuTexture.USAGE_TEXTURE_BINDING|GpuTexture.USAGE_RENDER_ATTACHMENT,
            GpuFormat.RGBA8_UNORM,sampleWidth,sampleHeight,1,1);
         views[i]=device.createTextureView(textures[i]);
      }
   }

   private void draw(CommandEncoder encoder,GpuTextureView source,GpuTextureView target,float[] data,boolean panel,
                     List<HudBlurRenderer.Region> regions,float guiWidth,float margin) {
      var pipeline=pipeline(panel,target.texture().getFormat());
      ByteBuffer buffer=MemoryUtil.memAlloc(UNIFORM_FLOATS*4);
      try {
         buffer.asFloatBuffer().put(data);
         var uniform=encoder.transientMemory().uploadGpu(buffer,device.getDeviceInfo().limits().minUniformOffsetAlignment(),GpuBuffer.USAGE_UNIFORM);
         try (var pass=encoder.createRenderPass(()->"samsara/hud-blur",target,Optional.empty())) {
            pass.setPipeline(pipeline); pass.setUniform("Scene",source,sampler); pass.setUniform("Blur",uniform);
            if (regions==null) pass.draw(3,1,0,0);
            else {
               float scale=target.getWidth(0)/guiWidth;
               for (var region:regions) {
                  int left=Math.max(0,(int)Math.floor((region.x()-margin)*scale));
                  int top=Math.max(0,(int)Math.floor((region.y()-margin)*scale));
                  int right=Math.min(target.getWidth(0),(int)Math.ceil((region.x()+region.width()+margin)*scale));
                  int bottom=Math.min(target.getHeight(0),(int)Math.ceil((region.y()+region.height()+margin)*scale));
                  if (right>left && bottom>top) {
                     pass.enableScissor(left,target.getHeight(0)-bottom,right-left,bottom-top);
                     pass.draw(3,1,0,0);
                  }
               }
            }
         }
      } finally { MemoryUtil.memFree(buffer); }
   }

   private CompiledRenderPipeline pipeline(boolean panel,GpuFormat format) {
      return pipelines.computeIfAbsent(new PipelineKey(panel,format),key->{
         var description=RenderPipeline.builder().withLocation("samsara/hud-blur-"+pipelines.size())
            .withVertexShader("samsara/hud-blur").withFragmentShader("samsara/hud-blur")
            .withBindGroupLayout(BindGroupLayout.builder().withUniform("Blur",UniformType.UNIFORM_BUFFER)
               .withUniform("Scene",UniformType.COMBINED_IMAGE_SAMPLER).build())
            .withCull(false).withDepthStencilState(Optional.empty()).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withColorTargetState(new ColorTargetState(panel ? Optional.of(new BlendFunction(BlendFactor.SRC_ALPHA,
               BlendFactor.ONE_MINUS_SRC_ALPHA,BlendFactor.ONE,BlendFactor.ONE_MINUS_SRC_ALPHA)) : Optional.empty(),format,ColorTargetState.WRITE_ALL)).build();
         return device.compilePipeline(description,new ShaderSource() {
            public String getShader(Identifier id,ShaderType type) { return type==ShaderType.VERTEX ? VERTEX : FRAGMENT; }
            public CachedIncludeSource getInclude(Identifier id) { return CachedIncludeSource.createError("No includes"); }
            public void close() { }
         },Runnable::run).join().finishCompile();
      });
   }

   private static float[] kernel(float sigma) {
      sigma=Math.max(.5f,sigma);
      int radius=Math.min(64,(int)Math.ceil(sigma*3));
      float[] data=new float[UNIFORM_FLOATS]; data[16]=1;
      float total=1; int taps=0;
      for (int i=1;i<=radius;i+=2) {
         float first=(float)Math.exp(-i*i/(2f*sigma*sigma));
         float second=i+1<=radius ? (float)Math.exp(-(i+1)*(i+1)/(2f*sigma*sigma)) : 0;
         float weight=first+second; ++taps;
         data[16+taps*4]=weight; data[17+taps*4]=i+second/weight; total+=2*weight;
      }
      for (int i=0;i<=taps;i++) data[16+i*4]/=total;
      data[2]=taps; return data;
   }

   private void closeTextures() {
      for (int i=0;i<3;i++) {
         if (views[i]!=null) { views[i].close(); views[i]=null; }
         if (textures[i]!=null) { textures[i].close(); textures[i]=null; }
      }
   }
   @Override public void close() {
      invalidate(); closeTextures();
      pipelines.values().forEach(CompiledRenderPipeline::close); pipelines.clear();
      if (sampler!=null) { sampler.close(); sampler=null; }
   }
   private record PipelineKey(boolean panel,GpuFormat format) { }
}
