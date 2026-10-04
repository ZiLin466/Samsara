package com.samsara.module.visual;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.event.impl.EventRender2D;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.module.FeatureManager;
import com.samsara.module.combat.KillAura;
import com.samsara.setting.BooleanSetting;
import com.samsara.setting.ModeSetting;
import com.samsara.ui.clickgui.opai.OpaiStyle;
import com.samsara.ui.hud.HudLayouts;
import com.samsara.ui.hud.editor.HudEditorScreen;
import com.samsara.util.ColorUtil;
import com.samsara.util.ModTextures;
import com.samsara.util.render.HudBackdrop;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;

import static com.samsara.module.visual.TargetHud.OpaiTargetHudPainter.*;

public class TargetHud extends Feature {
   public final ModeSetting mode = new ModeSetting("Mode", this, "Classic", new String[]{"Classic", "Opai"});
   private final BooleanSetting showArmor = new BooleanSetting("Show Armor", this, true);
   private final OpaiTargetHudHealth opaiHealth = new OpaiTargetHudHealth();
   private Player opaiTarget;
   private OpaiTargetHudPainter.Bounds opaiBounds;
   private static final String f676 = "HP: 4";
   private float f698;
   private String f706;
   private static final String f665 = "Show Win or Loss";
   private Player f703;
   private final BooleanSetting f693;
   private static final String f666 = "Health Animation";
   private static final String f688 = "HP: 16";
   private static final String f685 = "HP: 13";
   private float f700;
   private static final String f671 = "HP: 20";
   private static final String f691 = "HP: 19";
   private final BooleanSetting f696;
   private static final String f682 = "HP: 10";
   private Player f705;
   private final BooleanSetting f692;
   private int f701;
   private static final String f677 = "HP: 5";
   private static final String f674 = "HP: 2";
   private static final String f683 = "HP: 11";
   private static final String f681 = "HP: 9";
   private int f702;
   private float f697;
   private static final String f687 = "HP: 15";
   private final BooleanSetting f695;
   private static final String f690 = "HP: 18";
   private static final String f668 = "\u00a7aW";
   private static final String f663 = "Outline";
   private static final String f675 = "HP: 3";
   private static final String f689 = "HP: 17";
   private Identifier f704;
   private static final String[] f713 = new String[]{
      TargetHud.f672,
      TargetHud.f673,
      f674,
      f675,
      f676,
      f677,
      TargetHud.f678,
      TargetHud.f679,
      TargetHud.f680,
      f681,
      f682,
      f683,
      TargetHud.f684,
      f685,
      TargetHud.f686,
      f687,
      f688,
      f689,
      f690,
      f691,
      f671
   };
   private static final String f667 = new String(new byte[0], StandardCharsets.UTF_8);
   private static final String f662 = "Theme Color";
   private static final String f680 = "HP: 8";
   private static final String f672 = "HP: 0";
   private static final String f679 = "HP: 7";
   private static final String f684 = "HP: 12";
   private static final String f669 = "\u00a7cL";
   private static final String f686 = "HP: 14";
   private int f707;
   private static final String f678 = "HP: 6";
   private int f711;
   private static final String f673 = "HP: 1";
   private int f710;
   private float f699;
   private static final String f670 = "Player";
   private static final String f661 = "TargetHUD";

   @Override
   public void onEvent(Event var1) {
      if (var1 == Events.f5) {
         if (HudEditorScreen.active()) {
            return;
         }

         this.render(Events.f5);
      }

      if (var1 == Events.f3) {
         if (this.mode.m228("Opai")) return;

         KillAura var2 = FeatureManager.f26;
         boolean var3 = var2 != null && var2.f17 instanceof Player;
         float var4 = var3 ? 255.0F : 0.0F;
         this.f702 = this.f701;
         this.f701 = (int)Mth.lerp(0.5F, (float)this.f701, var4);
         if (!var3) {
            return;
         }

         Player var5 = (Player)var2.f17;
         this.f699 = this.f698;
         this.f698 = this.f697;
         this.f697 = this.f700;
         this.f700 = var5.getHealth();
      }
   }

   public void m207(GuiGraphicsExtractor var1, float var2, Player var3) {
      if (this.mode.m228("Opai")) {
         renderOpai(var1, var3);
         return;
      }
      if (var3 == null) return;
      String var8 = var3.getName().getString();
      int var9 = mc.font.width(var8);
      int var10 = Math.max(80, 38 + var9);
      this.f710 = var10;
      this.f711 = 42;
      var box = classicBox(var10);
      var1.pose().pushMatrix();
      try {
         var1.pose().translate(box.x(), box.y()); var1.pose().scale(box.width()/var10, box.width()/var10);
         int alpha = (int)(255 * HudEditorScreen.previewOpacity()), white = alpha << 24 | 0xFFFFFF;
         var1.fill(0,0,var10,42,((int)(170*HudEditorScreen.previewOpacity()))<<24);
         var1.text(mc.font,var8,34,5,white,false);
         var1.text(mc.font,"HP: " + Math.round(var3.getHealth()),var10-35,22,white,false);
         var1.fill(4,34,var10-4,38,white);
         new OpaiTargetHudSurface(var1,var3,8,OpaiTargetHudPainter.PANEL_RESOURCE,122,40,6,HudEditorScreen.previewOpacity()).face(4,4,26,0);
      } finally { var1.pose().popMatrix(); }
   }

   private HudLayouts.Box classicBox(int width) {
      var box = HudLayouts.INSTANCE.fit(HudLayouts.Element.TARGET,width,42,
         mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight(),true);
      HudLayouts.INSTANCE.drawn(HudLayouts.Element.TARGET,box); return box;
   }

   private void render(EventRender2D var1) {
      if (mc.player == null || mc.level == null || mc.gui.overlay() != null) {
         clearTarget();
         return;
      }
      if (this.mode.m228("Opai")) {
         KillAura aura = FeatureManager.f26;
         Player target = aura != null && aura.isEnabled() && aura.f17 instanceof Player player ? player : null;
         renderOpai(var1.m89(), target);
         return;
      }
      this.opaiTarget = null;
      this.opaiBounds = null;
      KillAura var2 = FeatureManager.f26;
      if (var2 != null) {
         boolean var3 = var2.f17 instanceof Player || this.f703 != null && this.f701 > 10;
         if (var3) {
            Player var4 = var2.f17 instanceof Player ? (Player)var2.f17 : this.f703;
            if (this.f705 != var4) {
               this.f705 = var4;
               this.f704 = null;
               this.f706 = var4.getDisplayName().getString();
               this.f707 = mc.font.width(this.f706);
               mc.getSkinManager().get(var4.getGameProfile()).thenAccept(var1x -> var1x.ifPresent(var1xx -> this.f704 = var1xx.body().texturePath()));
            }

            int var5 = mc.getWindow().getGuiScaledWidth();
            int var6 = mc.getWindow().getGuiScaledHeight();
            int var7 = 0, var8 = 0;
            int var9 = Math.max(80, 48 + this.f707);
            float var10 = Mth.lerp(var1.m91(), this.f699, this.f698);
            float var11 = Mth.lerp(var1.m91(), this.f697, this.f700);
            float var12 = var4.getMaxHealth();
            float var13 = var11 / var12;
            float var14 = var10 / var12;
            GuiGraphicsExtractor var15 = var1.m89();
            int var16 = ColorUtil.m27();
            int var17 = this.f692.m215() ? var16 : ColorUtil.m26(var13);
            int var18 = this.f692.m215() ? var16 : ColorUtil.m26(var13);
            var18 = var18 & 16777215 | 1677721600;
            int var19 = (int)((float)this.f702 + (float)(this.f701 - this.f702) * var1.m91());
            var17 = var19 << 24 | var17 & 16777215;
            int var20 = var19 * 170 / 255 << 24;
            int var21 = Math.round(var11);
            if (var21 < 0) {
               var21 = 0;
            } else if (var21 > 20) {
               var21 = 20;
            }

            String var22 = f713[var21];
            var box = classicBox(var9);
            var15.pose().pushMatrix();
            try {
            var15.pose().translate(box.x(),box.y()); var15.pose().scale(box.width()/var9,box.width()/var9);
            var15.fill(var7, var8, var7 + var9, var8 + 42, var20);
            if (this.f693.m215()) {
               var15.fill(var7, var8, var7 + 1, var8 + 42, var19 << 24 | var16 & 16777215);
               var15.fill(var7 + var9 - 1, var8, var7 + var9, var8 + 42, var19 << 24 | var16 & 16777215);
               var15.fill(var7, var8, var7 + var9, var8 + 1, var19 << 24 | var16 & 16777215);
               var15.fill(var7, var8 + 41, var7 + var9, var8 + 42, var19 << 24 | var16 & 16777215);
            }

            if (this.f696.m215() && this.f701 > 200) {
               var15.fill(var7 + 4, var8 + 34, var7 + 8 + (int)((float)(var9 - 12) * var14), var8 + 38, var18);
            }

            var15.fill(var7 + 4, var8 + 34, var7 + 8 + (int)((float)(var9 - 12) * var13), var8 + 38, var17);
            this.f710 = var9;
            this.f711 = 42;
            if (this.f695.m215()) {
               String var23 = var4.getHealth() <= mc.player.getHealth() ? f668 : f669;
               var15.text(mc.font, var23, var7 + var9 - 10, var8 + 5, var19 << 24 | 16777215, false);
            }

            var15.text(mc.font, this.f706, var7 + 34, var8 + 5, var19 << 24 | 16777215, false);
            var15.text(mc.font, var22, var7 + var9 - 35, var8 + 22, var17, false);
            if (this.f704 != null) {
               var15.blit(RenderPipelines.GUI_TEXTURED, this.f704, var7 + 4, var8 + 4, 8.0F, 8.0F, 26, 26, 8, 8, 64, 64, var19 << 24 | 16777215);
            }

            this.f703 = var4;
            } finally { var15.pose().popMatrix(); }
         }
      }
   }

   public TargetHud() {
      super(f661, Category.VISUAL);
      this.f692 = new BooleanSetting(f662, this, false);
      this.f693 = new BooleanSetting(f663, this, false);
      this.f695 = new BooleanSetting(f665, this, false);
      this.f696 = new BooleanSetting(f666, this, false);
      this.f706 = f667;
      this.showArmor.setVisible(() -> this.mode.m228("Opai"));
      for (BooleanSetting setting : new BooleanSetting[]{this.f692, this.f693, this.f695, this.f696})
         setting.setVisible(() -> !this.mode.m228("Opai"));
   }

   private void renderOpai(GuiGraphicsExtractor graphics, Player target) {
      this.f703 = null;
      this.f701 = this.f702 = 0;
      if (mc.player == null || mc.level == null || mc.gui.overlay() != null || target == null
          || target.level() != mc.level || target.isRemoved()) {
         this.opaiTarget = null;
         this.opaiBounds = null;
         return;
      }
      long now = System.nanoTime() / 1_000_000L;
      if (target != this.opaiTarget) {
         this.opaiTarget = target;
         this.opaiHealth.reset(target.getHealth(), target.getMaxHealth(), now);
      }
      var health = this.opaiHealth.update(target.getHealth(), target.getMaxHealth(), now);
      var surface = new OpaiTargetHudSurface(graphics, target, OpaiTargetHudPainter.TEXT_SIZE,
         OpaiTargetHudPainter.PANEL_RESOURCE, OpaiTargetHudPainter.WIDTH, OpaiTargetHudPainter.HEIGHT,
         OpaiTargetHudPainter.RADIUS, HudEditorScreen.previewOpacity());
      String name = target.getName().getString();
      int viewportWidth = mc.getWindow().getGuiScaledWidth(), viewportHeight = mc.getWindow().getGuiScaledHeight();
      var content = OpaiTargetHudPainter.bounds(surface, name, health.maximum(), viewportWidth, viewportHeight, 0, 0);
      var box = HudLayouts.INSTANCE.fit(HudLayouts.Element.TARGET, content.width(), content.height(), viewportWidth, viewportHeight, true);
      HudLayouts.INSTANCE.drawn(HudLayouts.Element.TARGET, box);
      float scale = box.width() / content.width();
      this.opaiBounds = new OpaiTargetHudPainter.Bounds(0, 0, content.width(), content.height());
      this.f710 = this.opaiBounds.width();
      this.f711 = this.opaiBounds.height();
      HudBackdrop.widget(box, OpaiTargetHudPainter.RADIUS * scale);
      graphics.pose().pushMatrix();
      try {
         graphics.pose().translate(box.x(), box.y()); graphics.pose().scale(scale, scale);
         OpaiTargetHudPainter.paint(surface, this.opaiBounds, name, health,
            this.showArmor.m215(), ClickGui.currentOpaiPalette());
      } finally { graphics.pose().popMatrix(); }
   }

   private void clearTarget() {
      this.opaiTarget = this.f703 = this.f705 = null;
      this.opaiBounds = null;
      this.f704 = null;
      this.f701 = this.f702 = 0;
   }

   @Override public void onDisable() { clearTarget(); }

   /** Two wall-time health responses: a quick fill and a lighter, delayed damage trail. */
   public static final class OpaiTargetHudHealth {
      private float goal;
      private float maximum = 20;
      private float fillFrom;
      private float trailFrom;
      private long changedAt;
      private boolean damage;
      private boolean initialized;

      public record Sample(float health, float trail, float maximum) {
         public float fraction() { return health / maximum; }
         public float trailFraction() { return trail / maximum; }
         public String label() { return format(health); }
      }

      public void reset(float health, float maximum, long now) {
         this.maximum = maximum(maximum);
         this.goal = health(health, this.maximum);
         this.fillFrom = this.trailFrom = this.goal;
         this.changedAt = now;
         this.damage = false;
         this.initialized = true;
      }

      public Sample update(float health, float maximum, long now) {
         float max = maximum(maximum);
         float value = health(health, max);
         if (!this.initialized) reset(value, max, now);
         if (value != this.goal || max != this.maximum) {
            Sample current = sample(now);
            this.damage = value < current.health();
            this.fillFrom = current.health();
            this.trailFrom = Math.max(current.health(), current.trail());
            this.goal = value;
            this.maximum = max;
            this.changedAt = now;
         }
         return sample(now);
      }

      public Sample sample(long now) {
         double elapsed = Math.max(0, now - this.changedAt);
         float fill = response(this.fillFrom, this.goal, elapsed, 65);
         float trail = this.damage
            ? Math.max(fill, response(this.trailFrom, this.goal, Math.max(0, elapsed - 100), 220)) : fill;
         return new Sample(health(fill, this.maximum), health(trail, this.maximum), this.maximum);
      }

      private static float response(float from, float to, double elapsed, double decay) {
         float value = (float)(to + (from - to) * Math.exp(-elapsed / decay));
         return Math.abs(value - to) < .025f ? to : value;
      }

      private static float maximum(float value) {
         return Float.isFinite(value) ? Math.max(1, value) : 20;
      }

      private static float health(float value, float maximum) {
         return Float.isFinite(value) ? Math.clamp(value, 0, maximum) : 0;
      }

      public static String format(float health) {
         float rounded = Math.round(health * 10) / 10f;
         return rounded == Math.round(rounded) ? Integer.toString(Math.round(rounded))
            : String.format(Locale.ROOT, "%.1f", rounded);
      }
   }

   public static final class OpaiTargetHudPainter {
      public static final int WIDTH = 122;
      public static final int HEIGHT = 40;
      public static final int RADIUS = 6;
      public static final int SHADOW_MARGIN = 8;
      public static final int TEXTURE_SCALE = 4;
      public static final float FACE_SIZE = 26;
      public static final float FACE_RADIUS = 4.5f;
      public static final float TEXT_SIZE = 8;
      public static final String PANEL_RESOURCE = "textures/hud/target-opai.png";
      public static final String BAR_RESOURCE = "textures/hud/target-opai-bar.png";

      public record Bounds(int x, int y, int width, int height) {
         public boolean contains(double x, double y) {
            return x >= this.x && x < this.x + this.width && y >= this.y && y < this.y + this.height;
         }
      }

      public interface Surface {
         float measure(String text);
         void panel(Bounds bounds);
         void face(float x, float y, float size, float radius);
         void armor(int slot, float x, float y);
         void text(String text, float x, float y, int color);
         void bar(float x, float y, float width, float height, int color);
      }

      private OpaiTargetHudPainter() { }

      public static Bounds bounds(Surface surface, String name, float maximum, int viewportWidth,
                                  int viewportHeight, int offsetX, int offsetY) {
         // Reserve a decimal so changes in the health label do not resize the shell.
         float labelWidth = labelWidth(surface, maximum);
         int width = Math.min(Math.max(WIDTH, (int)Math.ceil(35 + surface.measure(name) + labelWidth + 4)),
            Math.max(1, viewportWidth - 4));
         int x = Math.clamp(viewportWidth / 2 + offsetX, 2, Math.max(2, viewportWidth - width - 2));
         int y = Math.clamp(viewportHeight / 2 + offsetY, 2, Math.max(2, viewportHeight - HEIGHT - 2));
         return new Bounds(x, y, width, HEIGHT);
      }

      public static void paint(Surface surface, Bounds bounds, String name, OpaiTargetHudHealth.Sample health,
                               boolean armor, OpaiStyle.Palette palette) {
         float x = bounds.x(), y = bounds.y();
         surface.panel(bounds);
         surface.face(x + 3, y + 3, FACE_SIZE, FACE_RADIUS);
         String label = health.label();
         String fittedName = fit(surface, name, bounds.width() - 35 - labelWidth(surface, health.maximum()) - 3);
         surface.text(fittedName, x + 32, y + 5, 0xFFFFFFFF);
         surface.text(label, x + 32 + surface.measure(fittedName) + 1.5f, y + 5, palette.accent());
         if (armor) {
            for (int slot = 0; slot < 4; slot++) surface.armor(slot, x + 31.5f + slot * 15.5f, y + 14.5f);
         }
         float barWidth = Math.max(0, bounds.width() - 7);
         surface.bar(x + 3, y + 32, barWidth, 5, 0x66000000);
         // Draw only the remaining damage interval: the theme fill covers the front of it.
         surface.bar(x + 3, y + 31.25f, barWidth * health.trailFraction(), 4.5f, 0x66FFFFFF);
         surface.bar(x + 3, y + 31.25f, barWidth * health.fraction(), 4.5f, palette.accent());
      }

      private static String fit(Surface surface, String text, float width) {
         if (surface.measure(text) <= width) return text;
         int end = text.length();
         while (end > 0 && surface.measure(text.substring(0, end) + "…") > width)
            end = text.offsetByCodePoints(end, -1);
         return end == 0 ? "" : text.substring(0, end) + "…";
      }

      private static float labelWidth(Surface surface, float maximum) {
         String label = OpaiTargetHudHealth.format(maximum);
         return Math.max(surface.measure(label.contains(".") ? label : label + ".0"), surface.measure("00.0"));
      }
   }

   public static final class OpaiTargetHudSurface implements OpaiTargetHudPainter.Surface {
      private static final Style FONT = Style.EMPTY.withFont(new FontDescription.Resource(
         Identifier.fromNamespaceAndPath("samsara", "terminal-google")));
      private final float fontScale;
      private final String panelResource;
      private final int panelWidth, panelHeight;
      private final float panelRadius;
      private final float opacity;
      private static final EquipmentSlot[] ARMOR = {
         EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
      };
      private final Minecraft mc = Minecraft.getInstance();
      private final GuiGraphicsExtractor graphics;
      private final Player player;

      public OpaiTargetHudSurface(GuiGraphicsExtractor graphics, Player player) {
         this(graphics, player, TEXT_SIZE, PANEL_RESOURCE, WIDTH, HEIGHT, RADIUS, 1);
      }

      public OpaiTargetHudSurface(GuiGraphicsExtractor graphics, Player player, float fontSize,
         String panelResource, int panelWidth, int panelHeight, float panelRadius, float opacity) {
         this.graphics = graphics;
         this.player = player;
         this.fontScale = fontSize / 10;
         this.panelResource = panelResource; this.panelWidth = panelWidth; this.panelHeight = panelHeight;
         this.panelRadius = panelRadius; this.opacity = Math.clamp(opacity, 0, 1);
      }

      private int tint(int color) { return ((int)((color >>> 24) * this.opacity) << 24) | (color & 0xFFFFFF); }

      private static Component label(String text) { return Component.literal(text).withStyle(FONT); }

      @Override public float measure(String text) { return this.mc.font.width(label(text)) * this.fontScale; }

      @Override public void panel(Bounds bounds) {
         Identifier texture = ModTextures.register(this.panelResource);
         int sourceWidth = (this.panelWidth + SHADOW_MARGIN * 2) * TEXTURE_SCALE;
         int sourceHeight = (this.panelHeight + SHADOW_MARGIN * 2) * TEXTURE_SCALE;
         // Stretch only the neutral center. Corners and the soft shadow keep their measured radius.
         int edge = Math.round((SHADOW_MARGIN + this.panelRadius) * TEXTURE_SCALE);
         int destinationWidth = (bounds.width() + SHADOW_MARGIN * 2) * TEXTURE_SCALE;
         int[] src = {0, edge, sourceWidth - edge, sourceWidth};
         int[] dst = {0, edge, destinationWidth - edge, destinationWidth};
         this.graphics.nextStratum();
         this.graphics.pose().pushMatrix();
         try {
            this.graphics.pose().translate(bounds.x() - SHADOW_MARGIN, bounds.y() - SHADOW_MARGIN);
            this.graphics.pose().scale(1f / TEXTURE_SCALE, 1f / TEXTURE_SCALE);
            for (int i = 0; i < 3; i++) {
               this.graphics.blit(RenderPipelines.GUI_TEXTURED, texture, dst[i], 0, src[i], 0,
                  dst[i + 1] - dst[i], sourceHeight, src[i + 1] - src[i], sourceHeight, sourceWidth, sourceHeight, tint(0xFFFFFFFF));
            }
         } finally { this.graphics.pose().popMatrix(); }
         this.graphics.nextStratum();
      }

      @Override public void face(float x, float y, float size, float radius) {
         if (this.player == null) return;
         Identifier skin = (this.player instanceof AbstractClientPlayer client
            ? client.getSkin() : DefaultPlayerSkin.get(this.player.getGameProfile())).body().texturePath();
         NativePlayerFace.extract(this.graphics,skin,x,y,size,radius,this.opacity);
      }

      @Override public void armor(int slot, float x, float y) {
         if (this.player == null) return;
         var stack = this.player.getItemBySlot(ARMOR[slot]);
         if (stack.isEmpty()) return;
         this.graphics.pose().pushMatrix();
         try {
            this.graphics.pose().translate(x, y);
            this.graphics.pose().scale(.95f, .95f);
            this.graphics.item(this.player, stack, 0, 0, slot);
         } finally { this.graphics.pose().popMatrix(); }
      }

      @Override public void text(String text, float x, float y, int color) {
         // Vanilla treats near-zero text alpha as an unspecified opaque color.
         if (((tint(color) >>> 24) & 0xFF) < 4) return;
         this.graphics.pose().pushMatrix();
         try {
            this.graphics.pose().translate(x, y);
            this.graphics.pose().scale(this.fontScale, this.fontScale);
            this.graphics.text(this.mc.font, label(text), 0, 0, tint(color), false);
         } finally { this.graphics.pose().popMatrix(); }
      }

      @Override public void bar(float x, float y, float width, float height, int color) {
         if (width <= 0) return;
         color = tint(color);
         Identifier texture = ModTextures.register(BAR_RESOURCE);
         this.graphics.pose().pushMatrix();
         try {
            this.graphics.pose().translate(x, y);
            this.graphics.pose().scale(1f / TEXTURE_SCALE, 1f / TEXTURE_SCALE);
            int w = Math.max(1, Math.round(width * TEXTURE_SCALE));
            int h = Math.max(1, Math.round(height * TEXTURE_SCALE));
            // Below one diameter, squeeze the pill; otherwise keep both round caps unscaled.
            if (w <= h) {
               this.graphics.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0, 0, 0, w, h, 20, 20, 20, 20, color);
            } else {
               int cap = h / 2;
               this.graphics.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0, 0, 0, cap, h, 10, 20, 20, 20, color);
               this.graphics.blit(RenderPipelines.GUI_TEXTURED, texture, cap, 0, 9, 0, w - cap * 2, h, 2, 20, 20, 20, color);
               this.graphics.blit(RenderPipelines.GUI_TEXTURED, texture, w - cap, 0, 10, 0, cap, h, 10, 20, 20, 20, color);
            }
         } finally { this.graphics.pose().popMatrix(); }
      }
   }

   /** Rounded skin head extraction uses the same face/hat UVs as vanilla PlayerFaceExtractor. */
   public static final class NativePlayerFace {
      private NativePlayerFace() { }
      public static void extract(GuiGraphicsExtractor graphics, Identifier skin, float x,float y,float size,float radius,float opacity) {
         int scale=OpaiTargetHudPainter.TEXTURE_SCALE;
         graphics.pose().pushMatrix();
         try {
            graphics.pose().translate(x,y);graphics.pose().scale(1f/scale,1f/scale);
            int pixels=Math.round(size*scale),round=Math.clamp(Math.round(radius*scale),0,pixels/2);
            int color=((int)(255*Math.clamp(opacity,0,1))<<24)|0xFFFFFF;
            for (int u:new int[]{8,40}) {
               for (int row=0;row<round;row++) {
                  double dy=round-row-.5;
                  int inset=(int)Math.round(round-Math.sqrt(round*round-dy*dy));
                  strip(graphics,skin,u,pixels,inset,row,pixels-inset*2,1,color);
                  strip(graphics,skin,u,pixels,inset,pixels-row-1,pixels-inset*2,1,color);
               }
               strip(graphics,skin,u,pixels,0,round,pixels,pixels-round*2,color);
            }
         } finally { graphics.pose().popMatrix(); }
      }
      private static void strip(GuiGraphicsExtractor graphics,Identifier skin,int u,int pixels,int x,int y,int width,int height,int color) {
         if (width<=0 || height<=0) return;
         float factor=8f/pixels;
         ((mixins.GuiGraphicsExtractorAccessor)graphics).samsara$blitTinted(RenderPipelines.GUI_TEXTURED,skin,x,x+width,y,y+height,
            (u+x*factor)/64,(u+(x+width)*factor)/64,(8+y*factor)/64,(8+(y+height)*factor)/64,color);
      }
   }
}
