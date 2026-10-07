package com.samsara.module.visual;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.module.FeatureManager;
import com.samsara.setting.BooleanSetting;
import com.samsara.setting.NumberSetting;
import com.samsara.ui.hud.HudLayouts;
import com.samsara.util.Friends;
import com.samsara.util.ModTextures;
import com.samsara.util.MutableVector3d;
import com.samsara.util.WorldToScreenProjector;
import com.samsara.util.render.GuiItemOpacity;
import com.samsara.util.render.HudBackdrop;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.phys.Vec3;

public final class NameTags extends Feature {
   private final BooleanSetting players = new BooleanSetting("Players", this, true);
   private final BooleanSetting items = new BooleanSetting("Items", this, true);
   private final BooleanSetting playerArmor = new BooleanSetting("Player Armor", this, true);
   private final BooleanSetting armorDifference = new BooleanSetting("Armor Difference", this, true);
   private final BooleanSetting floatingText = new BooleanSetting("Floating Text", this, true);
   private final BooleanSetting shader = new BooleanSetting("Apply HUD shader", this, true);
   private final NumberSetting scale = new NumberSetting("Scale", this, .6, .1, 2, .1);

   public NameTags() { super("NameTags", Category.VISUAL); }

   public interface ReplacementState {
      boolean samsara$replaceNameTag();
      void samsara$replaceNameTag(boolean replace);
   }

   public static boolean replaces(Entity entity) {
      NameTags module = FeatureManager.nameTags;
      return module != null && module.isEnabled() && module.accepts(entity);
   }

   private boolean accepts(Entity entity) {
      if (mc.level == null || mc.player == null || entity.isRemoved() || mc.gui.hud.isHidden()) return false;
      if (entity instanceof Player) return players.getValue() && (FeatureManager.targets == null
         ? entity != mc.player : FeatureManager.targets.shouldShow(entity));
      if (entity instanceof ItemEntity item) return items.getValue() && !item.getItem().isEmpty();
      return floatingText.getValue() && (entity instanceof Display.TextDisplay display
         ? display.textRenderState() != null && !display.textRenderState().text().getString().isBlank()
         : entity instanceof ArmorStand && entity.hasCustomName() && entity.isCustomNameVisible());
   }

   @Override public void onEvent(Event event) {
      if (event != Events.RENDER_2D || mc.level == null || mc.player == null || mc.gui.hud.isHidden()) return;
      GuiGraphicsExtractor graphics = Events.RENDER_2D.getGraphics();
      float partial = Events.RENDER_2D.getPartialTick();
      Vec3 camera = mc.gameRenderer.mainCamera().position();
      List<Projected> labels = new ArrayList<>();
      MutableVector3d point = new MutableVector3d();
      for (Entity entity : mc.level.entitiesForRendering()) {
         if (!accepts(entity)) continue;
         Vec3 position = entity.getPosition(partial);
         double rise = entity instanceof Display.TextDisplay ? 0
            : entity instanceof ItemEntity ? entity.getBbHeight() + .35 : entity.getEyeHeight() + .55;
         Vec3 anchor = position.add(0, rise, 0);
         if (WorldToScreenProjector.project(anchor.x, anchor.y, anchor.z, point) == null
            || !Double.isFinite(point.x) || !Double.isFinite(point.y) || !Double.isFinite(point.z)
            || point.z < -1 || point.z > 1) continue;
         float opacity = Geometry.opacity((float)point.x, graphics.guiWidth());
         if (opacity < 4f / 255) continue;
         labels.add(new Projected(entity, (float)point.x, (float)point.y, position.distanceToSqr(camera), opacity));
      }
      labels.sort(Comparator.comparingDouble(Projected::distanceSquared).reversed()
         .thenComparingInt(label -> label.entity().getId()));
      for (Projected label : labels) {
         View view = view(label.entity());
         NativeSurface surface = new NativeSurface(graphics, label.entity(), label.opacity(), shader.getValue(),
            view.horizontal() ? Geometry.FONT_SIZE : Geometry.FLOATING_FONT_SIZE);
         Layout layout = Geometry.layout(view.parts(), surface::measure,
            view.horizontal() ? Geometry.PADDING : Geometry.FLOATING_PADDING);
         float size = (float)scale.getValue();
         if (shader.getValue()) for (int i = 0; i < layout.capsules().size(); i++) {
            Capsule capsule = layout.capsules().get(i);
            float x = view.horizontal() ? capsule.x() - layout.width() / 2 : -capsule.width() / 2;
            float height = view.horizontal() ? Geometry.HEIGHT : Geometry.FLOATING_HEIGHT;
            float y = -height / 2 + (view.horizontal() ? 0 : i * (height + Geometry.GAP));
            HudBackdrop.widget(new HudLayouts.Box(label.x() + x * size, label.y() + y * size,
               capsule.width() * size, height * size), Geometry.RADIUS * size, label.opacity());
         }
         graphics.nextStratum();
         graphics.pose().pushMatrix();
         try {
            graphics.pose().translate(label.x(), label.y());
            graphics.pose().scale(size, size);
            Painter.paint(surface, view, layout);
         } finally { graphics.pose().popMatrix(); }
      }
      graphics.nextStratum();
   }

   private View view(Entity entity) {
      List<Part> parts = new ArrayList<>();
      List<ItemStack> equipment = List.of();
      Component score = null;
      if (entity instanceof Player player) {
         int health = actualHealth(player);
         parts.add(new Part(Component.literal(Integer.toString(health)), Geometry.healthColor(health), true));
         parts.add(new Part(formattedName(player), 0xFFFFFFFF, false));
         if (armorDifference.getValue()) parts.add(Geometry.armorPart(
            Armor.value(mc.player::getItemBySlot), Armor.value(player::getItemBySlot)));
         parts.add(new Part(Component.literal(Math.round(mc.player.distanceTo(player)) + "m"), Geometry.DISTANCE, false));
         if (playerArmor.getValue()) equipment = equipment(player);
         var objective = mc.level.getScoreboard().getDisplayObjective(DisplaySlot.BELOW_NAME);
         if (objective != null && !Geometry.isHealthObjective(objective.getDisplayName().getString())) {
            var value = mc.level.getScoreboard().getPlayerScoreInfo(player, objective);
            if (value != null) score = Component.literal(value.value() + " ").append(objective.getDisplayName());
         }
      } else if (entity instanceof ItemEntity item) {
         parts.add(new Part(item.getItem().getHoverName(), 0xFFFFFFFF, false));
         parts.add(new Part(Component.literal("x" + item.getItem().getCount()), Geometry.COUNT, false));
      } else {
         Component text = entity instanceof Display.TextDisplay display
            ? display.textRenderState().text() : entity.getCustomName();
         for (Component line : Geometry.lines(text)) parts.add(new Part(line, 0xFFFFFFFF, false));
      }
      return new View(List.copyOf(parts), equipment, score, entity instanceof Player || entity instanceof ItemEntity);
   }

   public static Component formattedName(Player player) {
      Component name = Geometry.teamName(player.getName(), player.getTeam());
      if (FeatureManager.antiBot != null && FeatureManager.antiBot.isBot(player)) return name.copy().withStyle(ChatFormatting.DARK_AQUA);
      if (player.isInvisible()) return name.copy().withStyle(ChatFormatting.GOLD);
      if (player.isShiftKeyDown()) return name.copy().withStyle(ChatFormatting.DARK_RED);
      if (Friends.contains(player.getGameProfile().name())) return name.copy().withStyle(ChatFormatting.AQUA);
      return name;
   }

   private static int actualHealth(Player player) {
      var objective = mc.level.getScoreboard().getDisplayObjective(DisplaySlot.BELOW_NAME);
      if (player != mc.player && objective != null && Geometry.isHealthObjective(objective.getDisplayName().getString())) {
         var value = mc.level.getScoreboard().getPlayerScoreInfo(player, objective);
         if (value != null) return Geometry.health(player.getHealth(), player.getAbsorptionAmount(), value.value());
      }
      return Geometry.health(player.getHealth(), player.getAbsorptionAmount(), null);
   }

   private static List<ItemStack> equipment(LivingEntity player) {
      List<ItemStack> result = new ArrayList<>(6);
      for (EquipmentSlot slot : Geometry.EQUIPMENT) {
         ItemStack stack = player.getItemBySlot(slot);
         if (!stack.isEmpty()) result.add(stack.copy());
      }
      return List.copyOf(result);
   }

   private record Projected(Entity entity, float x, float y, double distanceSquared, float opacity) { }
   public record Part(Component text, int color, boolean heart) { }
   public record Capsule(Part part, float x, float width) { }
   public record Layout(List<Capsule> capsules, float width) { }
   public record View(List<Part> parts, List<ItemStack> equipment, Component score, boolean horizontal) { }
   public record MaskSlice(float x, float y, float width, float height, int sourceX, int sourceY,
      int sourceWidth, int sourceHeight, int sourceSize) { }

   public static final class Armor {
      public static int value(Function<EquipmentSlot, ItemStack> equipment) {
         // Equipment packets can arrive without an updated entity armor attribute.
         AttributeInstance armor = new AttributeInstance(Attributes.ARMOR, ignored -> { });
         for (EquipmentSlot slot : Geometry.EQUIPMENT) {
            ItemStack stack = equipment.apply(slot);
            if (stack.isEmpty()) continue;
            stack.forEachModifier(slot, (attribute, modifier) -> {
               if (attribute.equals(Attributes.ARMOR)) armor.addOrUpdateTransientModifier(modifier);
            });
         }
         return (int)Math.floor(armor.getValue());
      }
   }

   public static final class Geometry {
      public static final float HEIGHT = 30, FLOATING_HEIGHT = 36, RADIUS = 12,
         FONT_SIZE = 16, FLOATING_FONT_SIZE = 24.5f, PADDING = 7, FLOATING_PADDING = 10, GAP = 2,
         HEART_WIDTH = 13.5f, HEART_HEIGHT = 13;
      public static final int GREEN = 0xFF88D922, YELLOW = 0xFFFFFF55, RED = 0xFFFF3333,
         EQUAL = 0xFFFFAA00, DISTANCE = 0xFF888888, COUNT = 0xFFFFCC00;
      private static final EquipmentSlot[] EQUIPMENT = {EquipmentSlot.MAINHAND, EquipmentSlot.HEAD,
         EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.OFFHAND};

      @FunctionalInterface public interface Measure { float width(Component text); }

      public static Layout layout(List<Part> parts, Measure measure) {
         return layout(parts, measure, PADDING);
      }

      public static Layout layout(List<Part> parts, Measure measure, float padding) {
         List<Capsule> capsules = new ArrayList<>(parts.size());
         float x = 0;
         for (Part part : parts) {
            float width = padding * 2 + measure.width(part.text()) + (part.heart() ? 16 : 0);
            capsules.add(new Capsule(part, x, width));
            x += width + GAP;
         }
         return new Layout(List.copyOf(capsules), Math.max(0, x - GAP));
      }

      public static List<MaskSlice> slices(boolean shadow, float width, float height) {
         float edge = shadow ? 20 : RADIUS;
         float ex = Math.min(edge, width / 2), ey = Math.min(edge, height / 2);
         float[] x = {0, ex, width - ex, width}, y = {0, ey, height - ey, height};
         int size = shadow ? 56 : 40;
         int[] src = {0, (int)edge, size - (int)edge, size};
         List<MaskSlice> slices = new ArrayList<>(9);
         for (int row = 0; row < 3; row++) for (int col = 0; col < 3; col++) {
            float w = x[col + 1] - x[col], h = y[row + 1] - y[row];
            if (w > 0 && h > 0) slices.add(new MaskSlice(x[col], y[row], w, h, src[col], src[row],
               src[col + 1] - src[col], src[row + 1] - src[row], size));
         }
         return slices;
      }

      public static float opacity(float x, float width) {
         if (!Float.isFinite(x) || !Float.isFinite(width) || width <= 0) return 0;
         float edge = Math.abs(x * 2 / width - 1);
         float t = Math.clamp((edge - .75f) / .25f, 0, 1);
         return 1 - t * t * (3 - 2 * t);
      }

      public static Part armorPart(int local, int target) {
         int difference = local - target;
         return new Part(Component.literal(difference == 0 ? "=" : Integer.toString(difference)),
            difference == 0 ? EQUAL : difference > 0 ? GREEN : RED, false);
      }

      public static int healthColor(int health) { return health >= 14 ? GREEN : health >= 8 ? YELLOW : RED; }
      public static int health(float health, float absorption, Integer scoreboardHealth) {
         return Math.max(0, scoreboardHealth == null ? (int)(health + absorption) : scoreboardHealth);
      }
      public static boolean isHealthObjective(String name) {
         String lower = name.toLowerCase(Locale.ROOT);
         return lower.contains("❤") || lower.contains("hp") || lower.contains("health")
            || lower.contains("здоровья") || lower.contains("здоровье");
      }
      public static Component teamName(Component base, PlayerTeam team) {
         return team == null ? base.copy() : team.getFormattedName(base);
      }
      public static List<Component> lines(Component text) {
         List<Component> result = new ArrayList<>();
         MutableComponent[] current = {Component.empty()};
         text.visit((style, fragment) -> {
            String[] lines = fragment.split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
               if (i > 0) { result.add(current[0]); current[0] = Component.empty(); }
               current[0].append(Component.literal(lines[i]).withStyle(style));
            }
            return Optional.empty();
         }, Style.EMPTY);
         result.add(current[0]);
         return result;
      }
   }

   public static final class Painter {
      public interface Surface {
         float measure(Component text);
         void panel(float x, float y, float width, float height);
         void text(Component text, float x, float y, int color);
         void heart(float x, float y, int color);
         void item(ItemStack item, float x, float y, int index);
         void score(Component text, float x, float y);
      }
      public static void paint(Surface surface, View view, Layout layout) {
         float height = view.horizontal() ? Geometry.HEIGHT : Geometry.FLOATING_HEIGHT;
         float top = -height / 2;
         if (view.horizontal()) {
            for (Capsule capsule : layout.capsules()) paintCapsule(surface, capsule, capsule.x() - layout.width() / 2, top, height, 8);
         } else {
            for (int i = 0; i < layout.capsules().size(); i++) {
               Capsule capsule = layout.capsules().get(i);
               paintCapsule(surface, capsule, -capsule.width() / 2, top + i * (height + Geometry.GAP), height, 7);
            }
         }
         float equipmentY = top - 36;
         float equipmentX = -(view.equipment().size() * 32 - 4) / 2f;
         for (int i = 0; i < view.equipment().size(); i++) surface.item(view.equipment().get(i), equipmentX + i * 32, equipmentY, i);
         if (view.score() != null) surface.score(view.score(), 0, top - 36);
      }
      private static void paintCapsule(Surface surface, Capsule capsule, float x, float y, float height, float textY) {
         surface.panel(x, y, capsule.width(), height);
         float textX = x + (height == Geometry.FLOATING_HEIGHT ? Geometry.FLOATING_PADDING : Geometry.PADDING);
         if (capsule.part().heart()) { surface.heart(textX + 1.25f, y + 7, capsule.part().color()); textX += 16; }
         surface.text(capsule.part().text(), textX, y + textY, capsule.part().color());
      }
   }

   private static final class NativeSurface implements Painter.Surface {
      private static final Style FONT = Style.EMPTY.withFont(new FontDescription.Resource(
         Identifier.fromNamespaceAndPath("samsara", "nametags")));
      private final GuiGraphicsExtractor graphics;
      private final Entity entity;
      private final float opacity;
      private final boolean shader;
      private final float fontSize;
      NativeSurface(GuiGraphicsExtractor graphics, Entity entity, float opacity, boolean shader, float fontSize) {
         this.graphics = graphics; this.entity = entity; this.opacity = opacity; this.shader = shader;
         this.fontSize = fontSize;
      }
      private Component font(Component text) { return text.copy().withStyle(FONT); }
      private int tint(int color) { return Math.round((color >>> 24) * opacity) << 24 | color & 0xFFFFFF; }
      @Override public float measure(Component text) { return mc.font.width(font(text)) * fontSize / 10; }
      @Override public void panel(float x, float y, float width, float height) {
         if (shader) mask("shadow", x - 8, y - 8, width + 16, height + 16, 0x80000000);
         mask("capsule", x, y, width, height, shader ? 0x98000000 : 0x80000000);
      }
      private void mask(String name, float x, float y, float width, float height, int color) {
         Identifier texture = ModTextures.register("textures/hud/nametags/" + name + ".png");
         for (MaskSlice slice : Geometry.slices(name.equals("shadow"), width, height)) {
            graphics.pose().pushMatrix();
            try {
               graphics.pose().translate(x + slice.x(), y + slice.y());
               graphics.pose().scale(slice.width() / slice.sourceWidth(), slice.height() / slice.sourceHeight());
               graphics.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0, slice.sourceX() * 4, slice.sourceY() * 4,
                  slice.sourceWidth(), slice.sourceHeight(), slice.sourceWidth() * 4, slice.sourceHeight() * 4,
                  slice.sourceSize() * 4, slice.sourceSize() * 4, tint(color));
            } finally { graphics.pose().popMatrix(); }
         }
      }
      @Override public void text(Component text, float x, float y, int color) {
         if ((tint(color) >>> 24) < 4) return;
         graphics.pose().pushMatrix();
         try {
            graphics.pose().translate(x, y);
            graphics.pose().scale(fontSize / 10, fontSize / 10);
            graphics.text(mc.font, font(text), 0, 0, tint(color), false);
         } finally { graphics.pose().popMatrix(); }
      }
      @Override public void heart(float x, float y, int color) {
         Identifier texture = ModTextures.register("textures/hud/nametags/heart.png");
         graphics.pose().pushMatrix();
         try {
            graphics.pose().translate(x, y);
            graphics.pose().scale(Geometry.HEART_WIDTH / 12, Geometry.HEART_HEIGHT / 14);
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0, 0, 0, 12, 14, 48, 56, 48, 56, tint(color));
         } finally { graphics.pose().popMatrix(); }
      }
      @Override public void item(ItemStack item, float x, float y, int index) {
         graphics.pose().pushMatrix();
         try (var ignored = GuiItemOpacity.extract(opacity)) {
            graphics.pose().translate(x, y); graphics.pose().scale(1.75f, 1.75f);
            graphics.item(entity instanceof LivingEntity living ? living : null, item, 0, 0, index);
            if (item.getCount() > 1) graphics.text(mc.font, Integer.toString(item.getCount()),
               17 - mc.font.width(Integer.toString(item.getCount())), 9, tint(0xFFFFFFFF), false);
         } finally { graphics.pose().popMatrix(); }
      }
      @Override public void score(Component text, float x, float y) {
         graphics.pose().pushMatrix();
         try {
            graphics.pose().translate(x, y); graphics.pose().scale(2, 2);
            graphics.text(mc.font, text, -mc.font.width(text) / 2, 0, tint(0xFFFFFFFF), false);
         } finally { graphics.pose().popMatrix(); }
      }
   }
}
