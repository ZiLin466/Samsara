package com.samsara.module.visual;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.module.FeatureManager;
import com.samsara.setting.BooleanSetting;
import com.samsara.util.ClientColors;
import com.samsara.util.MutableVector3d;
import com.samsara.util.WorldToScreenProjector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class PlayerEsp extends Feature {
   private static final double HEIGHT_PADDING = 0.15;
   private static final int BOX_BORDER_THICKNESS = 1;
   private static final int BOX_OUTLINE_THICKNESS = 1;
   private static final int HEALTH_BAR_OFFSET = 4;
   private static final String HEALTH_BAR_LABEL = "Health bar";
   private static final String PLAYER_ESP_LABEL = "PlayerESP";
   private final BooleanSetting twoDimensional;
   private final BooleanSetting healthBar;
   private final BooleanSetting outline;
   private final MutableVector3d projectedPosition;
   private static final String TWO_DIMENSIONAL_LABEL = "2D";
   private static final String OUTLINE_LABEL = "Outline";

   public PlayerEsp() {
      super(PLAYER_ESP_LABEL, Category.VISUAL);
      this.twoDimensional = new BooleanSetting(TWO_DIMENSIONAL_LABEL, this, false);
      this.healthBar = new BooleanSetting(HEALTH_BAR_LABEL, this, false);
      this.outline = new BooleanSetting(OUTLINE_LABEL, this, false);
      this.projectedPosition = new MutableVector3d();
   }

   private void draw2DBox(GuiGraphicsExtractor graphics, LivingEntity target, int color, float partialTick, MutableVector3d scratchPosition) {
      double minScreenX = Double.MAX_VALUE;
      double minScreenY = Double.MAX_VALUE;
      double maxScreenX = -Double.MAX_VALUE;
      double maxScreenY = -Double.MAX_VALUE;
      double entityX = target.xOld + (target.getX() - target.xOld) * (double)partialTick;
      double entityY = target.yOld + (target.getY() - target.yOld) * (double)partialTick;
      double entityZ = target.zOld + (target.getZ() - target.zOld) * (double)partialTick;
      double halfWidth = (double)target.getBbWidth() * 0.5;
      double entityHeight = (double)target.getBbHeight();
      double minX = entityX - halfWidth;
      double maxX = entityX + halfWidth;
      double maxY = entityY + entityHeight + HEIGHT_PADDING;
      double minZ = entityZ - halfWidth;
      double maxZ = entityZ + halfWidth;
      // Preserve corner order and suppress the overlay when any corner is behind the camera.
      for (int corner = 0; corner < 8; corner++) {
         double cornerX = (corner & 2) == 0 ? minX : maxX;
         double cornerY = (corner & 4) == 0 ? entityY : maxY;
         double cornerZ = (corner & 1) == 0 ? minZ : maxZ;
         MutableVector3d screenPoint = WorldToScreenProjector.project(cornerX, cornerY, cornerZ, scratchPosition);
         if (screenPoint == null) return;
         minScreenX = Math.min(minScreenX, screenPoint.x);
         minScreenY = Math.min(minScreenY, screenPoint.y);
         maxScreenX = Math.max(maxScreenX, screenPoint.x);
         maxScreenY = Math.max(maxScreenY, screenPoint.y);
      }
      int left = (int)minScreenX;
      int top = (int)minScreenY;
      int right = (int)maxScreenX;
      int bottom = (int)maxScreenY;
      drawBox(graphics, left, top, right, bottom, color);
      drawHealthBar(graphics, target, left, top, bottom);
   }

   private void drawBox(GuiGraphicsExtractor graphics, int left, int top, int right, int bottom, int color) {
      int borderThickness = BOX_BORDER_THICKNESS;
      if (this.twoDimensional.getValue()) {
         int outlineThickness = BOX_OUTLINE_THICKNESS;
         graphics.fill(left - outlineThickness, top - outlineThickness, right + outlineThickness, top + outlineThickness + borderThickness, -16777216);
         graphics.fill(left - outlineThickness, bottom - outlineThickness - borderThickness, right + outlineThickness, bottom + outlineThickness, -16777216);
         graphics.fill(left - outlineThickness, top - outlineThickness, left + outlineThickness + borderThickness, bottom + outlineThickness, -16777216);
         graphics.fill(right - outlineThickness - borderThickness, top - outlineThickness, right + outlineThickness, bottom + outlineThickness, -16777216);
         graphics.fill(left, top, right, top + borderThickness, color);
         graphics.fill(left, bottom - borderThickness, right, bottom, color);
         graphics.fill(left, top, left + borderThickness, bottom, color);
         graphics.fill(right - borderThickness, top, right, bottom, color);
      }
   }

   private void drawHealthBar(GuiGraphicsExtractor graphics, LivingEntity target, int left, int top, int bottom) {
      if (this.healthBar.getValue()) {
         double healthRatio = Math.clamp((double)(target.getHealth() / target.getMaxHealth()), 0.0, 1.0);
         int healthColor = ClientColors.healthColor((float)healthRatio);
         int barHeight = Math.max(1, bottom - top);
         int filledBarHeight = Math.max(target.getHealth() > 0.0F ? 1 : 0, (int)Math.ceil((double)barHeight * healthRatio));
         int barOffset = HEALTH_BAR_OFFSET;
         int barLeft = left - barOffset;
         int barRight = barLeft + 1;
         graphics.fill(barLeft - 1, top - 1, barRight + 1, top + barHeight + 1, -16777216);
         graphics.fill(barLeft, top, barRight, top + barHeight, -11513776);
         if (filledBarHeight > 0) {
            graphics.fill(barLeft, top + barHeight - filledBarHeight, barRight, top + barHeight, healthColor);
         }
      }
   }

   @Override
   public void onEvent(Event event) {
      if (event == Events.RENDER_2D && (this.twoDimensional.getValue() || this.healthBar.getValue())) {
         GuiGraphicsExtractor graphics = Events.RENDER_2D.getGraphics();
         float partialTick = Events.RENDER_2D.getPartialTick();
         int themeColor = ClientColors.colorAtOffset(0);

         for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof LivingEntity target && FeatureManager.targets.shouldShow(target)) {
               this.draw2DBox(graphics, target, themeColor, partialTick, this.projectedPosition);
            }
         }
      }

      if (event == Events.ENTITY_OUTLINE && this.outline.getValue()) {
         Entity entity = Events.ENTITY_OUTLINE.getEntity();
         if (!FeatureManager.targets.shouldShow(entity)) {
            return;
         }

         Events.ENTITY_OUTLINE.setColor(ClientColors.colorAtOffset(0));
      }
   }
}
