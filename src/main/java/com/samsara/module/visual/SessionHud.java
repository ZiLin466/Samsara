package com.samsara.module.visual;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.module.visual.TargetHud.OpaiTargetHudPainter.Surface;
import com.samsara.module.visual.TargetHud.OpaiTargetHudSurface;
import com.samsara.ui.hud.HudLayouts;
import com.samsara.ui.hud.editor.HudEditorScreen;
import com.samsara.util.ModTextures;
import com.samsara.util.render.HudBackdrop;
import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class SessionHud extends Feature {
   public SessionHud() { super("SessionHUD", Category.VISUAL); }
   @Override public void onEvent(Event event) {
      if (event == Events.f5 && !HudEditorScreen.active()) renderSession(Events.f5.m89());
   }
   public void renderSession(GuiGraphicsExtractor graphics) {
      if (mc.player == null || mc.level == null || mc.gui.overlay() != null) return;
      var stats = SessionTracker.STATS;
      var surface = new OpaiTargetHudSurface(graphics, mc.player, SessionHudPainter.FONT_SIZE, "textures/hud/session.png",
         144, SessionHudPainter.HEIGHT, SessionHudPainter.RADIUS, HudEditorScreen.previewOpacity());
      SessionHudPainter.Canvas canvas = new SessionHudPainter.Canvas() {
         public float measure(String text) { return surface.measure(text); }
         public void panel(com.samsara.module.visual.TargetHud.OpaiTargetHudPainter.Bounds bounds) { surface.panel(bounds); }
         public void face(float x, float y, float size, float radius) { surface.face(x,y,size,radius); }
         public void armor(int slot, float x, float y) { }
         public void text(String text, float x, float y, int color) { surface.text(text,x,y,color); }
         public void bar(float x, float y, float w, float h, int color) { }
         public void icon(boolean skull, float x, float y, float size) {
            var texture = ModTextures.register("textures/hud/session-" + (skull ? "skull" : "wins") + ".png");
            graphics.pose().pushMatrix();
            try {
               graphics.pose().translate(x,y); graphics.pose().scale(size/32, size/32);
               graphics.blit(RenderPipelines.GUI_TEXTURED,texture,0,0,0,0,32,32,32,32,32,32,
                  ((int)(255 * HudEditorScreen.previewOpacity()) << 24) | 0xFFFFFF);
            } finally { graphics.pose().popMatrix(); }
         }
      };
      int width = SessionHudPainter.width(canvas, stats.time(), stats.kills(), stats.wins());
      var box = HudLayouts.INSTANCE.fit(HudLayouts.Element.SESSION,width,SessionHudPainter.HEIGHT,
         mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight(),false);
      HudLayouts.INSTANCE.drawn(HudLayouts.Element.SESSION,box);
      float scale = box.width()/width;
      HudBackdrop.widget(box,SessionHudPainter.RADIUS*scale);
      graphics.pose().pushMatrix();
      try {
         graphics.pose().translate(box.x(),box.y()); graphics.pose().scale(scale,scale);
         SessionHudPainter.paint(canvas,width,stats.time(),stats.kills(),stats.wins());
      } finally { graphics.pose().popMatrix(); }
   }

   /** Session timing and deduplicated combat/title facts, independent of HUD enablement. */
   public static final class SessionStats {
      private long lastTick = -1;
      private long playedMillis;
      private boolean wasPlaying;
      private int kills, wins;
      private UUID attacked;
      private int attackedLife;
      private long attackedAt, lastVictory = Long.MIN_VALUE / 2;
      private record Death(int life, long time) { }
      private final HashMap<UUID, Death> counted = new HashMap<>();
      public void tick(long now, boolean playing) {
         if (this.lastTick >= 0 && playing && this.wasPlaying) this.playedMillis += Math.clamp(now - this.lastTick, 0, 1000);
         this.lastTick = now;
         this.wasPlaying = playing;
         this.counted.entrySet().removeIf(entry -> now - entry.getValue().time() > 60000);
         if (!playing || now - this.attackedAt > 15000) this.attacked = null;
      }
      public void attack(UUID victim, long now) { attack(victim, 0, now); }
      public void attack(UUID victim, int life, long now) { this.attacked = victim; this.attackedLife = life; this.attackedAt = now; }
      public boolean death(UUID victim, long now) {
         return death(victim, 0, now);
      }
      public boolean death(UUID victim, int life, long now) {
         var previous = this.counted.get(victim);
         if (!victim.equals(this.attacked) || life != this.attackedLife || now - this.attackedAt > 15000
            || (previous != null && previous.life() == life)) return false;
         this.counted.put(victim, new Death(life, now)); this.attacked = null; this.kills++; return true;
      }
      public UUID attacked() { return this.attacked; }
      public void victory(String title, long now) {
         String value = title.replaceAll("§.", "").strip().toUpperCase(Locale.ROOT);
         if ((value.equals("VICTORY!") || value.equals("VICTORY") || value.equals("YOU WIN!")
              || value.equals("胜利!") || value.equals("胜利！")) && now - this.lastVictory > 15000) {
            this.wins++; this.lastVictory = now;
         }
      }
      public void clearCombat() { this.attacked = null; this.counted.clear(); }
      public int kills() { return this.kills; }
      public int wins() { return this.wins; }
      public String time() { return formatTime(this.playedMillis / 1000); }
      public static String formatTime(long seconds) {
         long hours = seconds / 3600, minutes = seconds / 60 % 60, secs = seconds % 60;
         return (hours > 0 ? hours + "h " : "") + (hours > 0 || minutes > 0 ? minutes + "m " : "") + secs + "s";
      }
   }

   /** Receives actual local attacks/death events rather than treating entity unloading as a kill. */
   public static final class SessionTracker {
      public static final SessionStats STATS = new SessionStats();
      private static Object world;
      private SessionTracker() { }
      private static long now() { return System.nanoTime() / 1_000_000L; }
      public static void tick() {
         var mc = Minecraft.getInstance();
         if (world != mc.level) { world = mc.level; STATS.clearCombat(); }
         STATS.tick(now(), mc.player != null && mc.level != null && !mc.isPaused());
         if (mc.level != null && STATS.attacked() != null) {
            Entity entity = mc.level.getEntity(STATS.attacked());
            if (entity instanceof LivingEntity living && living.isDeadOrDying()) death(entity);
         }
      }
      public static void attack(Entity entity) {
         if (entity instanceof LivingEntity living && !living.isDeadOrDying() && entity != Minecraft.getInstance().player)
            STATS.attack(entity.getUUID(), entity.getId(), now());
      }
      public static void death(Entity entity) { if (entity != null) STATS.death(entity.getUUID(), entity.getId(), now()); }
      public static void title(String title) { STATS.victory(title, now()); }
   }

   public static final class SessionHudPainter {
      public static final int HEIGHT = 49;
      public static final float RADIUS = 7.5f;
      public static final float FONT_SIZE = 9.2f;
      public interface Canvas extends Surface { void icon(boolean skull, float x, float y, float size); }
      private SessionHudPainter() { }
      public static int width(Canvas surface, String time, int kills, int wins) {
         // Reserve the time bucket's full width to prevent resizing on each second.
         if (!time.contains("h ")) return 150;
         String reserved = "8".repeat(time.indexOf("h ")) + "h 59m 59s";
         return Math.max(150, (int)Math.ceil(58 + surface.measure("Time elapsed " + reserved)));
      }
      public static void paint(Canvas surface, int width, String time, int kills, int wins) {
         surface.panel(new com.samsara.module.visual.TargetHud.OpaiTargetHudPainter.Bounds(0, 0, width, HEIGHT));
         surface.face(6, 6, 34.5f, 5.5f);
         surface.text("Time elapsed " + time, 50, 13, 0xFFAAAAAA);
         surface.icon(true, 50, 28, 8);
         String killLabel = kills + " kills";
         surface.text(killLabel, 62, 28.5f, 0xFFFFFFFF);
         float winX = 62 + surface.measure(killLabel) + 10;
         surface.icon(false, winX, 28.5f, 6);
         surface.text(wins + " wins", winX + 11, 28.5f, 0xFFFFFFFF);
      }
   }
}
