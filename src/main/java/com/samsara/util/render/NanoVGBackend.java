package com.samsara.util.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.RenderPass.RenderArea;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.pipeline.*;
import com.mojang.renderpearl.api.textures.*;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.util.*;
import net.minecraft.resources.Identifier;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.nanovg.NanoVG.*;

/** Keeps NanoVG's path and glyph generation while submitting through the game's GPU API. */
public final class NanoVGBackend implements AutoCloseable {
   private static final ValueLayout.OfInt INT = ValueLayout.JAVA_INT;
   private static final ValueLayout.OfFloat FLOAT = ValueLayout.JAVA_FLOAT;
   private static final AddressLayout POINTER = ValueLayout.ADDRESS;
   private static final MemoryLayout COMPOSITE = MemoryLayout.structLayout(INT, INT, INT, INT);
   private static final int PATH_SIZE = 56;
   private static final String VERTEX = """
      #version 450
      layout(location=0) in vec2 Position;
      layout(location=1) in vec2 TexCoord;
      layout(std140) uniform Paint { vec4 data[12]; };
      layout(location=0) out vec2 position;
      layout(location=1) out vec2 texCoord;
      void main() {
         position = Position;
         texCoord = TexCoord;
         gl_Position = vec4(Position.x / data[11].x * 2.0 - 1.0,
                            1.0 - Position.y / data[11].y * 2.0, 0.0, 1.0);
      }
      """;
   private static final String FRAGMENT = """
      #version 450
      layout(std140) uniform Paint { vec4 data[12]; };
      uniform sampler2D Image;
      uniform sampler2D Coverage;
      layout(location=0) in vec2 position;
      layout(location=1) in vec2 texCoord;
      layout(location=0) out vec4 color;
      void main() {
         mat3 scissor = mat3(data[0].xyz, data[1].xyz, data[2].xyz);
         mat3 paint = mat3(data[3].xyz, data[4].xyz, data[5].xyz);
         vec2 sc = vec2(0.5) - (abs((scissor * vec3(position,1)).xy) - data[8].xy) * data[8].zw;
         float clip = clamp(sc.x,0,1) * clamp(sc.y,0,1);
         float edge = min(1.0,(1.0-abs(texCoord.x*2.0-1.0))*data[10].x)*min(1.0,texCoord.y);
         int type = int(data[10].w);
         if (type == 4) { color=vec4(edge); return; }
         if (data[11].z > 0.5) {
            vec2 uv = position / data[11].xy;
            uv.y=1.0-uv.y;
            edge = texture(Coverage,uv).r;
         }
         vec4 result;
         if (type == 0) {
            vec2 p = abs((paint * vec3(position,1)).xy) - data[9].xy + data[9].z;
            float distance = min(max(p.x,p.y),0.0)+length(max(p,0.0))-data[9].z;
            result=mix(data[6],data[7],clamp((distance+data[9].w*0.5)/max(data[9].w,0.0001),0,1));
         } else {
            vec2 uv = type==3 ? texCoord : (paint * vec3(position,1)).xy / data[9].xy;
            if (data[11].w > 0.5) uv.y=1.0-uv.y;
            result=texture(Image,uv);
            if (int(data[10].z)==1) result.rgb*=result.a;
            if (int(data[10].z)==2) result=vec4(result.r);
            result*=data[6];
         }
         color=result*clip*(type==3 ? 1.0 : edge);
      }
      """;
   private final GpuDevice device;
   private final Arena arena = Arena.ofShared();
   private final ArrayList<Draw> draws = new ArrayList<>();
   private final Map<Integer, Image> images = new HashMap<>();
   private final Map<PipelineKey, CompiledRenderPipeline> pipelines = new HashMap<>();
   private final ArrayList<Image> retiredImages = new ArrayList<>();
   private final MemorySegment params;
   private final java.lang.invoke.MethodHandle create;
   private final java.lang.invoke.MethodHandle delete;
   private long context;
   private int imageId;
   private float width, height;
   private RuntimeException failure;
   private boolean closed;
   private Image white;
   private GpuTexture coverage;
   private GpuTextureView coverageView;

   public NanoVGBackend(GpuDevice device) {
      this.device = Objects.requireNonNull(device);
      if (POINTER.byteSize()!=8) throw new IllegalStateException("NanoVG GPU backend requires a 64-bit runtime");
      try {
         create=nativeFunction("nvgCreateInternal", FunctionDescriptor.of(POINTER, POINTER));
         delete=nativeFunction("nvgDeleteInternal", FunctionDescriptor.ofVoid(POINTER));
         params=arena.allocate(112,8);
         params.set(INT,8,1);
         bind(16,"createRenderer",INT,POINTER);
         bind(24,"createTexture",INT,POINTER,INT,INT,INT,INT,POINTER);
         bind(32,"deleteTexture",INT,POINTER,INT);
         bind(40,"updateTexture",INT,POINTER,INT,INT,INT,INT,INT,POINTER);
         bind(48,"textureSize",INT,POINTER,INT,POINTER,POINTER);
         bind(56,"viewport",null,POINTER,FLOAT,FLOAT,FLOAT);
         bind(64,"cancel",null,POINTER);
         bind(72,"flush",null,POINTER);
         bind(80,"fill",null,POINTER,POINTER,COMPOSITE,POINTER,FLOAT,POINTER,POINTER,INT);
         bind(88,"stroke",null,POINTER,POINTER,COMPOSITE,POINTER,FLOAT,FLOAT,POINTER,INT);
         bind(96,"triangles",null,POINTER,POINTER,COMPOSITE,POINTER,POINTER,INT,FLOAT);
         bind(104,"deleteRenderer",null,POINTER);
         context=((MemorySegment)create.invokeExact(params)).address();
         check();
         if (context==0) throw new IllegalStateException("Cannot create NanoVG GPU context");
      } catch (Throwable error) {
         if (context!=0) {
            try { nativeFunction("nvgDeleteInternal",FunctionDescriptor.ofVoid(POINTER)).invokeExact(MemorySegment.ofAddress(context)); }
            catch (Throwable cleanup) { error.addSuppressed(cleanup); }
            context=0;
         }
         images.values().forEach(Image::close); retiredImages.forEach(Image::close);
         arena.close();
         throw new IllegalStateException("Cannot initialize NanoVG GPU callbacks",error);
      }
   }

   private static java.lang.invoke.MethodHandle nativeFunction(String name, FunctionDescriptor descriptor) throws Exception {
      // The bundled binding exposes the internal renderer ABI as native function pointers.
      var field=org.lwjgl.nanovg.NanoVG.class.getDeclaredField(name); field.setAccessible(true);
      return Linker.nativeLinker().downcallHandle(MemorySegment.ofAddress(field.getLong(null)),descriptor);
   }

   private void bind(long offset,String name,MemoryLayout result,MemoryLayout... args) throws ReflectiveOperationException {
      var descriptor=result==null ? FunctionDescriptor.ofVoid(args) : FunctionDescriptor.of(result,args);
      var type=descriptor.toMethodType();
      var target=MethodHandles.lookup().findVirtual(NanoVGBackend.class,name,type).bindTo(this);
      params.set(POINTER,offset,Linker.nativeLinker().upcallStub(target,descriptor,arena));
   }

   public long context() { check(); return context; }
   public void check() { if (failure!=null) throw failure; }
   private void failed(Throwable error) { if (failure==null) failure=new IllegalStateException("NanoVG GPU callback failed",error); }
   private int createRenderer(MemorySegment unused) { return 1; }
   private void deleteRenderer(MemorySegment unused) { }
   private void viewport(MemorySegment unused,float width,float height,float ratio) { this.width=width; this.height=height; }
   private void cancel(MemorySegment unused) { draws.clear(); }
   private void flush(MemorySegment unused) { }

   private int createTexture(MemorySegment unused,int type,int w,int h,int flags,MemorySegment data) {
      GpuTexture texture=null; GpuTextureView view=null; GpuSampler sampler=null;
      try {
         int id=++imageId;
         texture=device.createTexture("samsara/nvg-image",GpuTexture.USAGE_COPY_DST|GpuTexture.USAGE_TEXTURE_BINDING,
            type==1 ? GpuFormat.R8_UNORM : GpuFormat.RGBA8_UNORM,w,h,1,1);
         var filter=(flags&NVG_IMAGE_NEAREST)!=0 ? FilterMode.NEAREST : FilterMode.LINEAR;
         sampler=device.createSampler((flags&NVG_IMAGE_REPEATX)!=0 ? AddressMode.REPEAT : AddressMode.CLAMP_TO_EDGE,
            (flags&NVG_IMAGE_REPEATY)!=0 ? AddressMode.REPEAT : AddressMode.CLAMP_TO_EDGE,filter,filter,1,OptionalDouble.empty());
         view=device.createTextureView(texture);
         int size=w*h*(type==1 ? 1 : 4);
         ByteBuffer pixels=data.address()==0 ? MemoryUtil.memCalloc(size) : data.reinterpret(size).asByteBuffer();
         try { device.createCommandEncoder().writeToTexture(texture,pixels,0,0,0,0,w,h); }
         finally { if (data.address()==0) MemoryUtil.memFree(pixels); }
         images.put(id,new Image(texture,view,sampler,type,flags));
         return id;
      } catch (Throwable error) {
         if (view!=null) view.close(); if (texture!=null) texture.close(); if (sampler!=null) sampler.close();
         failed(error); return 0;
      }
   }

   private int deleteTexture(MemorySegment unused,int id) {
      Image image=images.remove(id);
      if (image!=null) retiredImages.add(image);
      return image==null ? 0 : 1;
   }

   private int updateTexture(MemorySegment unused,int id,int x,int y,int w,int h,MemorySegment data) {
      try {
         Image image=images.get(id); if (image==null) return 0;
         int stride=image.texture.getWidth(0)*(image.type==1 ? 1 : 4),row=w*(image.type==1 ? 1 : 4);
         ByteBuffer pixels=MemoryUtil.memAlloc(row*h);
         try {
            for (int i=0;i<h;i++) pixels.put(data.reinterpret((long)stride*image.texture.getHeight(0))
               .asSlice((long)(y+i)*stride+(long)x*(image.type==1 ? 1 : 4),row).asByteBuffer());
            pixels.flip();
            device.createCommandEncoder().writeToTexture(image.texture,pixels,0,0,x,y,w,h);
         } finally { MemoryUtil.memFree(pixels); }
         return 1;
      } catch (Throwable error) { failed(error); return 0; }
   }

   private int textureSize(MemorySegment unused,int id,MemorySegment w,MemorySegment h) {
      Image image=images.get(id); if (image==null) return 0;
      w.reinterpret(4).set(INT,0,image.texture.getWidth(0)); h.reinterpret(4).set(INT,0,image.texture.getHeight(0));
      return 1;
   }

   private void fill(MemorySegment unused,MemorySegment paint,MemorySegment blend,MemorySegment scissor,float fringe,
                     MemorySegment bounds,MemorySegment paths,int count) {
      try {
         var mesh=new Mesh(); var contours=new ArrayList<float[]>();
         MemorySegment list=paths.reinterpret((long)count*PATH_SIZE);
         boolean convex=count==1 && list.get(INT,48)!=0;
         for (int i=0;i<count;i++) {
            var path=list.asSlice((long)i*PATH_SIZE,PATH_SIZE);
            int n=path.get(INT,24); var vertices=path.get(POINTER,16).reinterpret((long)n*16);
            if (convex) mesh.fan(vertices,n);
            else {
               float[] contour=new float[n*2];
               for (int j=0;j<n;j++) { contour[j*2]=vertices.get(FLOAT,j*16L); contour[j*2+1]=vertices.get(FLOAT,j*16L+4); }
               contours.add(contour);
            }
         }
         if (!convex) triangulate(contours,mesh);
         for (int i=0;i<count;i++) {
            var path=list.asSlice((long)i*PATH_SIZE,PATH_SIZE);
            mesh.strip(path.get(POINTER,32).reinterpret((long)path.get(INT,40)*16),path.get(INT,40));
         }
         add(paint,blend,scissor,fringe,fringe,false,!convex,mesh);
      } catch (Throwable error) { failed(error); }
   }

   private void stroke(MemorySegment unused,MemorySegment paint,MemorySegment blend,MemorySegment scissor,float fringe,
                       float strokeWidth,MemorySegment paths,int count) {
      try {
         var mesh=new Mesh(); var list=paths.reinterpret((long)count*PATH_SIZE);
         for (int i=0;i<count;i++) {
            var path=list.asSlice((long)i*PATH_SIZE,PATH_SIZE);
            mesh.strip(path.get(POINTER,32).reinterpret((long)path.get(INT,40)*16),path.get(INT,40));
         }
         add(paint,blend,scissor,fringe,strokeWidth,false,true,mesh);
      } catch (Throwable error) { failed(error); }
   }

   private void triangles(MemorySegment unused,MemorySegment paint,MemorySegment blend,MemorySegment scissor,
                          MemorySegment vertices,int count,float fringe) {
      try {
         var mesh=new Mesh(); vertices=vertices.reinterpret((long)count*16);
         for (int i=0;i<count;i++) mesh.vertex(vertices,i);
         add(paint,blend,scissor,fringe,1,true,false,mesh);
      } catch (Throwable error) { failed(error); }
   }

   private void add(MemorySegment paint,MemorySegment blend,MemorySegment scissor,float fringe,float strokeWidth,
                    boolean text,boolean mask,Mesh mesh) {
      if (mesh.size==0) return;
      NVGPaint p=NVGPaint.create(paint.address());
      Image image=images.get(p.image());
      float[] uniforms=new float[48];
      var clip=scissor.reinterpret(32);
      if (clip.get(FLOAT,24)<-0.5 || clip.get(FLOAT,28)<-0.5) {
         uniforms[32]=uniforms[33]=uniforms[34]=uniforms[35]=1;
      } else {
         matrix(uniforms,0,inverse(clip));
         uniforms[32]=clip.get(FLOAT,24); uniforms[33]=clip.get(FLOAT,28);
         uniforms[34]=(float)Math.hypot(clip.get(FLOAT,0),clip.get(FLOAT,8))/fringe;
         uniforms[35]=(float)Math.hypot(clip.get(FLOAT,4),clip.get(FLOAT,12))/fringe;
      }
      matrix(uniforms,12,inverse(paint.reinterpret(NVGPaint.SIZEOF)));
      premultiply(uniforms,24,p.innerColor()); premultiply(uniforms,28,p.outerColor());
      uniforms[36]=p.extent(0); uniforms[37]=p.extent(1); uniforms[38]=p.radius(); uniforms[39]=p.feather();
      uniforms[40]=(strokeWidth+fringe)*0.5F/fringe;
      uniforms[42]=image==null ? 0 : image.type==1 ? 2 : (image.flags&NVG_IMAGE_PREMULTIPLIED)!=0 ? 0 : 1;
      uniforms[43]=text ? 3 : image!=null ? 1 : 0;
      uniforms[44]=width; uniforms[45]=height; uniforms[46]=mask ? 1 : 0;
      uniforms[47]=image!=null && (image.flags&NVG_IMAGE_FLIPY)!=0 ? 1 : 0;
      draws.add(new Draw(mesh.array(),uniforms,image,new PipelineKey(blend.get(INT,0),blend.get(INT,4),
         blend.get(INT,8),blend.get(INT,12)),mask));
   }

   static float[] inverse(MemorySegment transform) {
      double a=transform.get(FLOAT,0),b=transform.get(FLOAT,4),c=transform.get(FLOAT,8),d=transform.get(FLOAT,12);
      double e=transform.get(FLOAT,16),f=transform.get(FLOAT,20),det=a*d-b*c;
      if (Math.abs(det)<1.0E-6) return new float[]{1,0,0,1,0,0};
      return new float[]{(float)(d/det),(float)(-b/det),(float)(-c/det),(float)(a/det),
         (float)((c*f-d*e)/det),(float)((b*e-a*f)/det)};
   }

   private static void matrix(float[] target,int offset,float[] m) {
      target[offset]=m[0]; target[offset+1]=m[1]; target[offset+4]=m[2]; target[offset+5]=m[3];
      target[offset+8]=m[4]; target[offset+9]=m[5]; target[offset+10]=1;
   }

   private static void premultiply(float[] target,int offset,org.lwjgl.nanovg.NVGColor c) {
      target[offset]=c.r()*c.a(); target[offset+1]=c.g()*c.a(); target[offset+2]=c.b()*c.a(); target[offset+3]=c.a();
   }

   public void render(GpuTextureView target,int pixelWidth,int pixelHeight) {
      check();
      if (white==null) {
         try (Arena temporary=Arena.ofConfined()) {
            var pixel=temporary.allocate(4); pixel.fill((byte)255);
            white=images.get(createTexture(MemorySegment.NULL,2,1,1,0,pixel)); check();
         }
      }
      if (coverage==null || coverage.getWidth(0)!=pixelWidth || coverage.getHeight(0)!=pixelHeight) {
         if (coverageView!=null) { coverageView.close(); coverage.close(); }
         coverage=device.createTexture("samsara/nvg-coverage",GpuTexture.USAGE_TEXTURE_BINDING|GpuTexture.USAGE_RENDER_ATTACHMENT,
            GpuFormat.RGBA8_UNORM,pixelWidth,pixelHeight,1,1);
         coverageView=device.createTextureView(coverage);
      }
      var encoder=device.createCommandEncoder();
      try {
         for (Draw draw:draws) {
            if (draw.mask) {
               RenderArea area=area(draw.vertices,pixelWidth,pixelHeight);
               if (area.width()==0 || area.height()==0) continue;
               float[] mask=draw.uniforms.clone(); mask[43]=4; mask[46]=0;
               renderDraw(encoder,coverageView,draw.vertices,mask,white,white.view,PipelineKey.MASK,true,area);
               Mesh quad=new Mesh(); quad.quad(area.x()*width/pixelWidth,area.y()*height/pixelHeight,
                  area.width()*width/pixelWidth,area.height()*height/pixelHeight);
               renderDraw(encoder,target,quad.array(),draw.uniforms,draw.image,coverageView,draw.blend,false,area);
            } else renderDraw(encoder,target,draw.vertices,draw.uniforms,draw.image,white.view,draw.blend,false,null);
         }
      } finally {
         draws.clear();
         for (Image image:retiredImages) image.close();
         retiredImages.clear();
      }
   }

   private RenderArea area(float[] vertices,int pixelWidth,int pixelHeight) {
      float left=Float.POSITIVE_INFINITY,top=left,right=Float.NEGATIVE_INFINITY,bottom=right;
      for (int i=0;i<vertices.length;i+=4) {
         left=Math.min(left,vertices[i]); top=Math.min(top,vertices[i+1]);
         right=Math.max(right,vertices[i]); bottom=Math.max(bottom,vertices[i+1]);
      }
      int x=Math.clamp((int)Math.floor(left*pixelWidth/width)-1,0,pixelWidth);
      int y=Math.clamp((int)Math.floor(top*pixelHeight/height)-1,0,pixelHeight);
      int endX=Math.clamp((int)Math.ceil(right*pixelWidth/width)+1,0,pixelWidth);
      int endY=Math.clamp((int)Math.ceil(bottom*pixelHeight/height)+1,0,pixelHeight);
      return new RenderArea(x,y,Math.max(0,endX-x),Math.max(0,endY-y));
   }

   private void renderDraw(CommandEncoder encoder,GpuTextureView target,float[] vertices,float[] uniforms,
                           Image image,GpuTextureView mask,PipelineKey key,boolean clear,RenderArea area) {
      var pipeline=pipeline(key,target.texture().getFormat());
      ByteBuffer vertexData=MemoryUtil.memAlloc(vertices.length*4),paintData=MemoryUtil.memAlloc(192);
      try {
         vertexData.asFloatBuffer().put(vertices); paintData.asFloatBuffer().put(uniforms);
         var vertexBuffer=encoder.transientMemory().uploadGpu(vertexData,16,GpuBuffer.USAGE_VERTEX);
         var paintBuffer=encoder.transientMemory().uploadGpu(paintData,
            device.getDeviceInfo().limits().minUniformOffsetAlignment(),GpuBuffer.USAGE_UNIFORM);
         // Game render targets keep Y up; Vulkan flips the target when presenting it.
         RenderArea framebufferArea=area==null ? null : new RenderArea(area.x(),
            target.getHeight(0)-area.y()-area.height(),area.width(),area.height());
         try (var pass=encoder.createRenderPass(()->"samsara/nvg-gpu",target,
            clear ? Optional.of(new org.joml.Vector4f()) : Optional.empty(),null,OptionalDouble.empty(),framebufferArea)) {
            pass.setPipeline(pipeline); pass.setVertexBuffer(0,vertexBuffer); pass.setUniform("Paint",paintBuffer);
            var texture=image==null ? white : image;
            pass.setUniform("Image",texture.view,texture.sampler);
            pass.setUniform("Coverage",mask,white.sampler);
            pass.draw(vertices.length/4,1,0,0);
         }
      } finally { MemoryUtil.memFree(vertexData); MemoryUtil.memFree(paintData); }
   }

   private CompiledRenderPipeline pipeline(PipelineKey key,GpuFormat format) {
      // Different target formats need distinct compiled pipelines.
      PipelineKey typed=key.withFormat(format);
      return pipelines.computeIfAbsent(typed,ignored->{
         var description=RenderPipeline.builder().withLocation("samsara/nvg-"+pipelines.size())
            .withVertexShader("samsara/nvg").withFragmentShader("samsara/nvg")
            .withBindGroupLayout(BindGroupLayout.builder().withUniform("Paint",UniformType.UNIFORM_BUFFER)
               .withUniform("Image",UniformType.COMBINED_IMAGE_SAMPLER).withUniform("Coverage",UniformType.COMBINED_IMAGE_SAMPLER).build())
            .withVertexBinding(0,VertexFormat.builder(0).addAttribute("Position",GpuFormat.RG32_FLOAT)
               .addAttribute("TexCoord",GpuFormat.RG32_FLOAT).build())
            .withCull(false).withDepthStencilState(Optional.empty()).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withColorTargetState(new ColorTargetState(Optional.of(key==PipelineKey.MASK ? BlendFunction.MAX : key.function()),
               format,ColorTargetState.WRITE_ALL)).build();
         return device.compilePipeline(description,new ShaderSource() {
            public String getShader(Identifier id,ShaderType type) {
               return type==ShaderType.VERTEX ? VERTEX : FRAGMENT;
            }
            public CachedIncludeSource getInclude(Identifier id) { return CachedIncludeSource.createError("No includes"); }
            public void close() { }
         },Runnable::run).join().finishCompile();
      });
   }

   static float[] tessellate(List<float[]> contours) { Mesh mesh=new Mesh(); triangulate(contours,mesh); return mesh.array(); }

   private static void triangulate(List<float[]> contours,Mesh mesh) {
      var edges=new ArrayList<Edge>(); var levels=new TreeSet<Double>();
      for (float[] contour:contours) {
         for (int i=0;i<contour.length/2;i++) {
            int next=(i+1)%(contour.length/2); double x=contour[i*2],y=contour[i*2+1],nx=contour[next*2],ny=contour[next*2+1];
            levels.add(y); if (Math.abs(y-ny)>1.0E-7) edges.add(new Edge(x,y,nx,ny));
         }
      }
      // Split crossing edges before sorting so each slab preserves nonzero winding.
      for (int i=0;i<edges.size();i++) for (int j=i+1;j<edges.size();j++) {
         var a=edges.get(i); var b=edges.get(j); double low=Math.max(a.minY(),b.minY()),high=Math.min(a.maxY(),b.maxY());
         double slope=a.slope()-b.slope();
         if (high>low && Math.abs(slope)>1.0E-9) {
            double y=(b.x(0)-a.x(0))/slope;
            if (y>low+1.0E-7 && y<high-1.0E-7) levels.add(y);
         }
      }
      Double[] ys=levels.toArray(Double[]::new);
      for (int i=0;i+1<ys.length;i++) {
         double low=ys[i],high=ys[i+1],mid=(low+high)*0.5;
         var active=new ArrayList<Edge>(); for (var edge:edges) if (mid>edge.minY() && mid<edge.maxY()) active.add(edge);
         active.sort(Comparator.comparingDouble(edge->edge.x(mid)));
         int winding=0; Edge left=null;
         for (var edge:active) {
            int before=winding; winding+=edge.y2>edge.y1 ? 1 : -1;
            if (before==0 && winding!=0) left=edge;
            if (before!=0 && winding==0 && left!=null) {
               mesh.triangle((float)left.x(low),(float)low,(float)edge.x(low),(float)low,(float)edge.x(high),(float)high);
               mesh.triangle((float)left.x(low),(float)low,(float)edge.x(high),(float)high,(float)left.x(high),(float)high);
               left=null;
            }
         }
      }
   }

   @Override public void close() {
      if (closed) return;
      closed=true;
      try {
         if (context!=0) delete.invokeExact(MemorySegment.ofAddress(context));
      } catch (Throwable error) {
         throw new IllegalStateException("Cannot close NanoVG GPU context",error);
      } finally {
         context=0;
         images.values().forEach(Image::close); images.clear(); retiredImages.forEach(Image::close); retiredImages.clear();
         pipelines.values().forEach(CompiledRenderPipeline::close); pipelines.clear();
         if (coverageView!=null) { coverageView.close(); coverage.close(); coverageView=null; coverage=null; }
         draws.clear(); arena.close();
      }
   }

   private record Image(GpuTexture texture,GpuTextureView view,GpuSampler sampler,int type,int flags) {
      void close() { view.close(); texture.close(); sampler.close(); }
   }
   private record Draw(float[] vertices,float[] uniforms,Image image,PipelineKey blend,boolean mask) { }
   private record Edge(double x1,double y1,double x2,double y2) {
      double minY() { return Math.min(y1,y2); } double maxY() { return Math.max(y1,y2); }
      double slope() { return (x2-x1)/(y2-y1); } double x(double y) { return x1+(y-y1)*slope(); }
   }
   private record PipelineKey(int srcRgb,int dstRgb,int srcAlpha,int dstAlpha,GpuFormat format) {
      static final PipelineKey MASK=new PipelineKey(-1,-1,-1,-1);
      PipelineKey(int a,int b,int c,int d) { this(a,b,c,d,null); }
      PipelineKey withFormat(GpuFormat format) { return new PipelineKey(srcRgb,dstRgb,srcAlpha,dstAlpha,format); }
      BlendFunction function() { return new BlendFunction(factor(srcRgb),factor(dstRgb),factor(srcAlpha),factor(dstAlpha)); }
      static BlendFactor factor(int factor) {
         return switch(factor) {
            case 1 -> BlendFactor.ZERO; case 2 -> BlendFactor.ONE; case 4 -> BlendFactor.SRC_COLOR;
            case 8 -> BlendFactor.ONE_MINUS_SRC_COLOR; case 16 -> BlendFactor.DST_COLOR; case 32 -> BlendFactor.ONE_MINUS_DST_COLOR;
            case 64 -> BlendFactor.SRC_ALPHA; case 128 -> BlendFactor.ONE_MINUS_SRC_ALPHA;
            case 256 -> BlendFactor.DST_ALPHA; case 512 -> BlendFactor.ONE_MINUS_DST_ALPHA;
            case 1024 -> BlendFactor.SRC_ALPHA_SATURATE; default -> throw new IllegalArgumentException("Unsupported NanoVG blend factor: "+factor);
         };
      }
   }
   private static final class Mesh {
      float[] values=new float[256]; int size;
      void add(float x,float y,float u,float v) {
         if (size+4>values.length) values=Arrays.copyOf(values,values.length*2);
         values[size++]=x; values[size++]=y; values[size++]=u; values[size++]=v;
      }
      void vertex(MemorySegment vertices,int i) { long offset=i*16L; add(vertices.get(FLOAT,offset),vertices.get(FLOAT,offset+4),vertices.get(FLOAT,offset+8),vertices.get(FLOAT,offset+12)); }
      void fan(MemorySegment vertices,int count) { for(int i=2;i<count;i++) { vertex(vertices,0); vertex(vertices,i-1); vertex(vertices,i); } }
      void strip(MemorySegment vertices,int count) { for(int i=2;i<count;i++) { vertex(vertices,i-2); vertex(vertices,i-1); vertex(vertices,i); } }
      void triangle(float x,float y,float a,float b,float c,float d) { add(x,y,0.5F,1); add(a,b,0.5F,1); add(c,d,0.5F,1); }
      void quad(float x,float y,float w,float h) { triangle(x,y,x+w,y,x+w,y+h); triangle(x,y,x+w,y+h,x,y+h); }
      float[] array() { return Arrays.copyOf(values,size); }
   }
}
