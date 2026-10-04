package com.samsara.module.visual;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.module.visual.TargetHud.OpaiTargetHudSurface;
import com.samsara.ui.hud.HudLayouts;
import com.samsara.ui.hud.editor.HudEditorScreen;
import com.samsara.util.ModTextures;
import com.samsara.util.render.HudBackdrop;
import java.util.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.ToDoubleFunction;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffects;

/** Real active effects form one editable group; editor demos never change the player's effects. */
public final class PotionStatus extends Feature {
   private final PotionStatusMotion motion=new PotionStatusMotion();
   private boolean preview;
   public PotionStatus() { super("Potion Status",Category.VISUAL); }
   @Override public void onDisable() { motion.clear(); }
   @Override public void onEvent(Event event) {
      if (event==Events.f5 && !HudEditorScreen.active()) renderStatus(Events.f5.m89());
   }
   public void renderStatus(GuiGraphicsExtractor graphics) {
      if (mc.player==null || mc.level==null || mc.gui.overlay()!=null) { motion.clear(); return; }
      var effects=new ArrayList<PotionStatusData.Effect>();
      for (var instance:mc.player.getActiveEffects()) {
         if (!instance.showIcon()) continue;
         var effect=instance.getEffect().value();var id=BuiltInRegistries.MOB_EFFECT.getKey(effect);
         String key=id.getNamespace().equals("minecraft")?id.getPath():id.toString();
         effects.add(new PotionStatusData.Effect(key,PotionStatusData.englishName(key),effect.getColor(),
            instance.getDuration(),instance.getAmplifier(),instance.isInfiniteDuration()));
      }
      boolean demo=effects.stream().noneMatch(effect->PotionStatusData.ICONS.contains(effect.key())) && HudEditorScreen.active();
      boolean initializeDemo=demo!=preview;
      if (initializeDemo) { motion.clear();preview=demo; }
      if (demo) effects.addAll(List.of(
         new PotionStatusData.Effect("night_vision","Night Vision",MobEffects.NIGHT_VISION.value().getColor(),9580,0,false),
         new PotionStatusData.Effect("speed","Speed",MobEffects.SPEED.value().getColor(),1780,1,false)));
      long now=System.nanoTime()/1_000_000L;
      var surface=new PotionStatusSurface(graphics,HudEditorScreen.previewOpacity());
      if (demo && initializeDemo) motion.update(effects,now-2000,surface::measure);
      var frame=motion.update(effects,now,surface::measure);
      if (frame.rows().isEmpty() || frame.height()<.01 || frame.width()<1) return;
      var box=HudLayouts.INSTANCE.fit(HudLayouts.Element.POTION,frame.width()*PotionStatusPainter.DEFAULT_SCALE,frame.height()*PotionStatusPainter.DEFAULT_SCALE,
         mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight(),false);
      HudLayouts.INSTANCE.drawn(HudLayouts.Element.POTION,box);
      float scale=box.width()/frame.width();
      for (var row:frame.rows()) {
         float x=box.x()+row.x()*scale,y=box.y()+(row.y()+frame.height()/2)*scale;
         HudBackdrop.widget(new HudLayouts.Box(x,y,row.width()*scale,PotionStatusMotion.HEIGHT*scale),PotionStatusPainter.RADIUS*scale);
         graphics.pose().pushMatrix();
         try {
            graphics.pose().translate(x,y);graphics.pose().scale(scale,scale);
            PotionStatusPainter.paint(surface,row);
         } finally { graphics.pose().popMatrix(); }
      }
   }

   /** English labels ignore the game language; unsupported icons are omitted. */
   public static final class PotionStatusData {
      public record Effect(String key, String name, int color, int ticks, int amplifier, boolean infinite) {
         public String title() { return name + (amplifier > 0 ? " " + roman(amplifier + 1) : ""); }
         public String timer() { return duration(ticks, infinite); }
         public int timerColor() { return !infinite && ticks <= 600 ? 0xFFFF5555 : 0xFFAAAAAA; }
         public String icon() { return ICONS.contains(key) ? "textures/hud/potion/" + key + ".png" : null; }
         public String titleTexture() {
            if (!ICONS.contains(key) || (amplifier!=0 && amplifier!=referenceAmplifier(key))) return null;
            return "textures/hud/potion/"+key+(amplifier>0?"-"+roman(amplifier+1).toLowerCase(Locale.ROOT):"")+"-title.png";
         }
         public float titleTextureX() { return key.equals("absorption") ? 126/6.4f : 20; }
      }
      public static final Set<String> ICONS = Set.of("speed", "slowness", "strength", "jump_boost", "regeneration",
         "fire_resistance", "water_breathing", "invisibility", "night_vision", "weakness", "poison",
         "absorption", "saturation", "haste", "mining_fatigue", "nausea", "resistance");
      public static int referenceAmplifier(String key) {
         return switch (key) {
            case "speed", "regeneration", "haste" -> 1;
            case "mining_fatigue" -> 2;
            case "absorption" -> 3;
            default -> 0;
         };
      }
      private static final Map<String, String> SPECIAL = Map.of("dolphins_grace", "Dolphin's Grace", "unluck", "Bad Luck");
      private PotionStatusData() { }
      public static String englishName(String key) {
         if (SPECIAL.containsKey(key)) return SPECIAL.get(key);
         StringBuilder result = new StringBuilder();
         for (String word : key.substring(key.indexOf(':') + 1).split("_")) {
            if (word.isEmpty()) continue;
            if (!result.isEmpty()) result.append(' ');
            result.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
         }
         return result.toString();
      }
      public static String duration(int ticks, boolean infinite) {
         if (infinite) return "∞";
         int seconds = Math.max(0, ticks) / 20;
         return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
      }
      private static String roman(int value) {
         if (value > 20) return Integer.toString(value);
         int[] values = {10,9,5,4,1}; String[] numerals = {"X","IX","V","IV","I"};
         StringBuilder result = new StringBuilder();
         for (int i=0;i<values.length;i++) while (value>=values[i]) { result.append(numerals[i]); value-=values[i]; }
         return result.toString();
      }
      public static int order(String key) {
         return switch (key) {
            case "night_vision" -> 0; case "speed" -> 1; case "slowness" -> 2;
            case "haste" -> 3; case "mining_fatigue" -> 4; case "strength" -> 5;
            case "jump_boost" -> 8; case "nausea" -> 9; case "regeneration" -> 10;
            case "resistance" -> 11; case "fire_resistance" -> 12;
            case "water_breathing" -> 13; case "invisibility" -> 14; case "weakness" -> 18; case "poison" -> 19;
            case "absorption" -> 22; case "saturation" -> 23;
            default -> 32;
         };
      }
   }

   /** Separate single/group exits with delayed list reflow. */
   public static final class PotionStatusMotion {
      public static final float HEIGHT = 20, GAP = 2.25f;
      private static final long REFLOW_DELAY = 300;
      private static final double REFLOW_DURATION = .200;

      private static final class Spring {
         private double value, velocity;
         private long last;
         Spring(double value, double velocity, long now) {
            this.value=value;this.velocity=velocity;this.last=now;
         }
         void advance(long now) {
            double dt=Math.max(0,now-last)/1000.0, offset=value-1, rate=18;
            double decay=Math.exp(-rate*dt),linear=velocity+rate*offset;
            value=1+(offset+linear*dt)*decay;
            velocity=(velocity-rate*linear*dt)*decay;
            last=now;
         }
      }

      private static final class Space {
         private Spring growth;
         private long collapseAt;
         private boolean retiring, collapsing;
         private double value, velocity, collapseFrom;
         Space(boolean firstGroup,long now) {
            value=firstGroup?1:0;growth=new Spring(value,0,now);
         }
         void advance(long now) {
            if (!collapsing) {
               growth.advance(retiring?Math.min(now,collapseAt):now);
               value=growth.value;velocity=growth.velocity;
               if (retiring && now>=collapseAt) { collapseFrom=value;collapsing=true; }
            }
            if (collapsing) {
               double t=Math.clamp((now-collapseAt)/1000.0/REFLOW_DURATION,0,1);
               value=collapseFrom*(1-t);velocity=t<1?-collapseFrom/REFLOW_DURATION:0;
            }
         }
         void present(boolean present,long now) {
            if (present && retiring) {
               growth=new Spring(value,velocity,now);retiring=false;collapsing=false;
            } else if (!present && !retiring) {
               retiring=true;collapseAt=now+REFLOW_DELAY;
            }
         }
      }

      private static final class Slide {
         private static final double ENTRY_DURATION=.430, GROUP_EXIT_DURATION=.170, BACK=1.70158;
         private static final double SINGLE_EXIT_SPEED=530;
         private double value,velocity,start,target,startVelocity,endVelocity,duration;
         private long changedAt;
         private boolean moving,groupExit;
         Slide(long now) { changedAt=now; }
         void advance(long now) {
            if (!moving) return;
            double t=Math.clamp((now-changedAt)/1000.0/duration,0,1),d=target-start;
            double a=startVelocity*duration,b=endVelocity*duration;
            double raw=start+a*t+(3*d-2*a-b)*t*t+(-2*d+a+b)*t*t*t;
            double speed=(a+2*(3*d-2*a-b)*t+3*(-2*d+a+b)*t*t)/duration;
            // Hold group cards in place during exit anticipation.
            value=groupExit?Math.min(start,raw):raw;
            velocity=groupExit && raw>start?0:speed;
            if (t>=1) { value=target;velocity=0;moving=false; }
         }
         void to(double target,boolean groupExit,float distance,long now) {
            if (target==this.target) return;
            start=value;this.groupExit=target==0 && groupExit;
            duration=target==1?ENTRY_DURATION:this.groupExit?GROUP_EXIT_DURATION:distance/SINGLE_EXIT_SPEED;
            double d=target-start;
            startVelocity=moving?velocity:target==1?(3+BACK)*d/duration:this.groupExit?0:3*d/duration;
            endVelocity=this.groupExit?(3+BACK)*d/duration:0;
            this.target=target;changedAt=now;moving=true;
         }
      }

      private static final class Entry {
         PotionStatusData.Effect effect;
         final Slide slide;
         final Space space;
         boolean present,justRemoved;
         float exitY,width;
         Entry(PotionStatusData.Effect effect,long now,boolean firstGroup) {
            this.effect=effect;slide=new Slide(now);space=new Space(firstGroup,now);
         }
      }
      public record Row(PotionStatusData.Effect effect, float x, float y, float width, float opacity) { }
      public record Frame(List<Row> rows, float width, float height) { }
      private final Map<String,Entry> entries=new HashMap<>();
      public Frame update(List<PotionStatusData.Effect> effects, long now, ToDoubleFunction<String> measure) {
         boolean firstGroup=entries.isEmpty();
         long previousCount=entries.values().stream().filter(entry->entry.present).count();
         var present=new HashSet<String>();
         for (var effect : effects) {
            if (!PotionStatusData.ICONS.contains(effect.key())) continue;
            var entry=entries.computeIfAbsent(effect.key(), key -> new Entry(effect,now,firstGroup));
            entry.effect=effect;present.add(effect.key());
         }
         for (var entry:entries.values()) {
            entry.slide.advance(now);entry.space.advance(now);
            boolean next=present.contains(entry.effect.key());
            entry.justRemoved=entry.present && !next;
            entry.width=PotionStatusPainter.width(entry.effect,measure);
            entry.slide.to(next?1:0,Math.max(previousCount,present.size())>1,entry.width+8,now);
            entry.space.present(next,now);entry.present=next;
         }
         // A final card exits at its existing vertical anchor; there is no shrinking empty group.
         if (present.isEmpty() && entries.values().stream().noneMatch(entry->entry.slide.moving)) {
            clear();return new Frame(List.of(),0,0);
         }
         entries.values().removeIf(entry->!entry.present && !entry.slide.moving && entry.space.value<=.001);
         var sorted=new ArrayList<>(entries.values());
         sorted.sort(Comparator.comparingInt((Entry entry)->PotionStatusData.order(entry.effect.key()))
            .thenComparing(entry->entry.effect.key()));
         float height=0,width=0;
         for (var entry:sorted) height+=(HEIGHT+GAP)*(float)Math.clamp(entry.space.value,0,1);
         height=Math.max(0,height-GAP);
         var rows=new ArrayList<Row>();float y=-height/2;
         for (var entry:sorted) {
            if (entry.justRemoved) entry.exitY=y;
            if (entry.present || entry.slide.moving) {
               float w=entry.width;
               rows.add(new Row(entry.effect,-(w+8)*(1-(float)entry.slide.value),entry.present?y:entry.exitY,w,1));width=Math.max(width,w);
            }
            y+=(HEIGHT+GAP)*(float)Math.clamp(entry.space.value,0,1);
         }
         return new Frame(List.copyOf(rows),width,height);
      }
      public void clear() { entries.clear(); }
   }

   /** Apply the default scale before the user-selected HUD scale. */
   public static final class PotionStatusPainter {
      public static final float DEFAULT_SCALE=1.6f;
      public static final float FONT_SIZE=5.75f, RADIUS=3.75f;
      public interface Surface {
         float measure(String text);
         void panel(float width,int color);
         void icon(String resource,int color);
         void text(String text,float x,float y,int color);
         default void title(PotionStatusData.Effect effect,int color) { text(effect.title(),19.75f,4.75f,color); }
         default void timer(PotionStatusData.Effect effect) { text(effect.timer(),19.75f,11.75f,effect.timerColor()); }
      }
      private PotionStatusPainter() { }
      public static float width(PotionStatusData.Effect effect,java.util.function.ToDoubleFunction<String> measure) {
         // Fixed base widths prevent panel jitter from font advance rounding.
         int pixels=switch (effect.key()) {
            case "jump_boost" -> 365; case "speed" -> 268; case "strength" -> 312;
            case "weakness" -> 334; case "night_vision" -> 370; case "water_breathing" -> 443;
            case "slowness" -> 320; case "fire_resistance" -> 423; case "invisibility" -> 339;
            case "poison" -> 275; case "regeneration" -> 423;
            case "absorption" -> 398; case "saturation" -> 341; case "haste" -> 289;
            case "mining_fatigue" -> 456; case "nausea" -> 286; case "resistance" -> 347;
            default -> 0;
         };
         if (pixels==0) return (float)(23.75+Math.max(measure.applyAsDouble(effect.title()),measure.applyAsDouble(effect.timer())));
         int amplifier=effect.key().equals("speed")?0:PotionStatusData.referenceAmplifier(effect.key());
         String referenceTitle=new PotionStatusData.Effect(effect.key(),effect.name(),effect.color(),0,amplifier,false).title();
         return (float)Math.max(23.75+measure.applyAsDouble(effect.timer()),pixels/6.4
            +measure.applyAsDouble(effect.title())-measure.applyAsDouble(referenceTitle));
      }
      public static void paint(Surface surface, PotionStatusMotion.Row row) {
         var effect=row.effect();int color=0xFF000000 | effect.color() & 0xFFFFFF;
         surface.panel(row.width(),color);
         if (effect.icon()!=null) surface.icon(effect.icon(),color);
         surface.title(effect,color);
         surface.timer(effect);
      }
   }

   public static final class PotionStatusSurface implements PotionStatusPainter.Surface {
      private final GuiGraphicsExtractor graphics;
      private final OpaiTargetHudSurface text;
      private final float opacity;
      private static final java.util.Map<String,int[]> TEXTURE_SIZES=new java.util.HashMap<>();
      public PotionStatusSurface(GuiGraphicsExtractor graphics,float opacity) {
         this.graphics=graphics;this.opacity=opacity;
         this.text=new OpaiTargetHudSurface(graphics,null,PotionStatusPainter.FONT_SIZE,"textures/hud/potion/glass.png",80,20,3.75f,opacity);
      }
      @Override public float measure(String value) { return text.measure(value); }
      @Override public void text(String value,float x,float y,int color) { text.text(value,x,y,color); }
      @Override public void title(PotionStatusData.Effect effect,int color) {
         String resource=effect.titleTexture();
         if (resource==null) { PotionStatusPainter.Surface.super.title(effect,color);return; }
         sprite(resource,effect.titleTextureX(),3.125f,color);
      }
      @Override public void timer(PotionStatusData.Effect effect) {
         if (effect.infinite()) { PotionStatusPainter.Surface.super.timer(effect);return; }
         float x=20;
         for (char digit:effect.timer().toCharArray()) {
            sprite("textures/hud/potion/digit-"+(digit==':'?"colon":digit)+".png",x,10.15625f,effect.timerColor());
            x+=(digit==':'?10:22)/6.4f;
         }
      }
      private void sprite(String resource,float x,float y,int color) {
         int[] size=TEXTURE_SIZES.computeIfAbsent(resource,path->{
            try(var image=ModTextures.read(net.minecraft.resources.Identifier.fromNamespaceAndPath("samsara",path))) {
               if (image==null) throw new IllegalStateException("Missing referenced potion glyph: "+path);
               return new int[]{image.getWidth(),image.getHeight()};
            }
         });
         graphics.pose().pushMatrix();
         try {
            graphics.pose().translate(x,y);graphics.pose().scale(1f/6.4f,1f/6.4f);
            graphics.blit(RenderPipelines.GUI_TEXTURED,ModTextures.register(resource),0,0,0,0,size[0],size[1],size[0],size[1],size[0],size[1],tint(color));
         } finally { graphics.pose().popMatrix(); }
      }
      @Override public void icon(String resource,int color) {
         graphics.pose().pushMatrix();
         try {
            graphics.pose().scale(20f/128,20f/128);
            graphics.blit(RenderPipelines.GUI_TEXTURED,ModTextures.register(resource),0,0,0,0,128,128,128,128,128,128,tint(color));
         } finally { graphics.pose().popMatrix(); }
      }
      private int tint(int color) { return ((int)((color>>>24)*opacity)<<24)|(color&0xFFFFFF); }
      @Override public void panel(float width,int color) {
         graphics.nextStratum();
         panelTexture("shadow",width,0xFFFFFFFF);
         panelTexture("glass",width,color);
         graphics.nextStratum();
      }
      private void panelTexture(String name,float width,int color) {
         var texture=ModTextures.register("textures/hud/potion/"+name+".png");
         int sourceWidth=720,sourceHeight=240,edge=70,destinationWidth=Math.round((width+10)*8);
         int[] src={0,edge,sourceWidth-edge,sourceWidth},dst={0,edge,destinationWidth-edge,destinationWidth};
         graphics.pose().pushMatrix();
         try {
            graphics.pose().translate(-5,-5);graphics.pose().scale(1f/8,1f/8);
            for (int i=0;i<3;i++) graphics.blit(RenderPipelines.GUI_TEXTURED,texture,dst[i],0,src[i],0,
               dst[i+1]-dst[i],sourceHeight,src[i+1]-src[i],sourceHeight,sourceWidth,sourceHeight,tint(color));
         } finally { graphics.pose().popMatrix(); }
      }
   }
}
