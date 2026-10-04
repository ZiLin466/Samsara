package com.samsara.module.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.module.FeatureManager;
import com.samsara.setting.BooleanSetting;
import com.samsara.setting.ModeSetting;
import com.samsara.setting.NumberSetting;
import java.nio.charset.StandardCharsets;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.lwjgl.sdl.SDLMouse;

public class Animations extends Feature {
   public final ModeSetting blockingAnimation = new ModeSetting("Blocking Animation", this, "1.7", com.samsara.module.visual.Animations.BlockingAnimation.STYLES);
   public final BooleanSetting fakeBlock = new BooleanSetting("Fake Block", this, false);
   public final NumberSetting blockY = new NumberSetting("Block Y", this, 0.1, 0.05, 0.3, 0.05);
   public final NumberSetting swingScale = new NumberSetting("Swing Scale", this, 0.9, 0.1, 1.0, 0.1);
   public NumberSetting f28;
   private static final String f597 = "Scale";
   public NumberSetting f27 = new NumberSetting(f597, this, 1.0, 0.1, 2.0, 0.1);
   private static final String f598 = "X";
   private static final String f600 = "Z";
   public NumberSetting f29;
   private static final String f599 = "Y";
   private static final String f596 = "Animations";
   public NumberSetting f30;

   public Animations() {
      super(f596, Category.VISUAL);
      this.f28 = new NumberSetting(f598, this, 0.0, -2.0, 2.0, 0.05);
      this.f29 = new NumberSetting(f599, this, 0.0, -2.0, 2.0, 0.05);
      this.f30 = new NumberSetting(f600, this, 0.0, -2.0, 2.0, 0.05);
      this.blockY.setVisible(() -> !this.blockingAnimation.m228("Pushdown"));
      this.swingScale.setVisible(() -> this.blockingAnimation.m228("1.7"));
   }

   public boolean shouldBlock(InteractionHand hand, ItemStack item) {
      if (!isEnabled() || hand != InteractionHand.MAIN_HAND || !item.is(ItemTags.SWORDS) || mc.player == null
         || mc.gui.screen() != null || !mc.isWindowActive()) return false;
      var aura = FeatureManager.f26;
      boolean target = aura != null && aura.isEnabled() && aura.f17 != null;
      if (target && !aura.isAutoBlockInputAllowed()) return false;
      boolean usingHand = mc.player.isUsingItem() && mc.player.getUsedItemHand() == hand;
      if (target && aura.hasAutoBlockMode()) return usingHand || aura.hasAutoBlockAnimation();
      return usingHand || this.fakeBlock.m215() && (target || isRightMouseDown());
   }

   private static boolean isRightMouseDown() {
      // Screen input can leave MouseHandler's gameplay button state latched.
      return (SDLMouse.SDL_GetMouseState((java.nio.FloatBuffer)null, (java.nio.FloatBuffer)null)
         & SDLMouse.SDL_BUTTON_RMASK) != 0;
   }

   /*
    * Copyright (c) 2015 - 2026 CCBlueX.
    * Licensed under GNU GPL version 3 or, at your option, any later version.
    * https://www.gnu.org/licenses/gpl-3.0.html
    */
   public static final class BlockingAnimation {
      public static final String[] STYLES = {"1.7", "Pushdown", "Sigma", "Exhibition", "Avatar", "Dortware"};
      private BlockingAnimation() { }

      public static void transform(PoseStack pose, HumanoidArm arm, float equip, float swing, String style, float y, float swingScale) {
         int side = arm == HumanoidArm.RIGHT ? 1 : -1;
         swing = Math.clamp(swing, 0, 1);
         float sine = (float)Math.sin(Math.sqrt(swing) * Math.PI);
         switch (style) {
            case "1.7" -> { pose.translate(-.1f * side, y, 0); swingOffset(pose, side, swing * swingScale); }
            case "Pushdown" -> {
               pose.translate(-.1f * side, .1f, 0);
               pose.rotateDegrees(Axis.ZP, side * sine * 10);
               pose.rotateDegrees(Axis.XP, sine * -35);
            }
            case "Sigma" -> {
               pose.rotate(axis(-sine * 27.5f * side, -8 * side, 0, 9)
                  .mul(axis(-sine * 45 * side, side, sine / 2, 0)));
               pose.translate(0, y, 0); swingOffset(pose, side, 0);
            }
            case "Exhibition" -> {
               pose.translate(0, -.1f, 0); swingOffset(pose, side, 0);
               pose.translate(.1f, .4f, -.1f);
               pose.rotate(axis(-sine * 30 * side, sine / 2, 0, 9)
                  .mul(axis(-sine * 50 * side, .8f * side, sine / 2, 0)));
               pose.translate(0, y - .2f, 0);
            }
            case "Avatar" -> {
               float sine1 = (float)Math.sin(swing * swing * Math.PI);
               pose.translate(.2f * side, y, 0);
               pose.rotate(new Quaternionf().rotateY((float)Math.toRadians(sine1 * -20 * side))
                  .rotateZ((float)Math.toRadians(sine * -20 * side))
                  .mul(axis(sine * -40 * side, side, 0, 0)));
               swingOffset(pose, side, 0);
            }
            case "Dortware" -> {
               pose.rotate(axis(-sine * 10, 0, 15, 200).mul(axis(-sine * 10, 300, sine / 2, 1)));
               pose.translate(3.4, .3, -.4); pose.translate(-2.1f, -.2f, .1f);
               pose.rotate(axis((float)Math.sin(Math.sqrt(swing) * Math.PI - 3) * 13, -10, -1.4f, -10));
               pose.translate(arm == HumanoidArm.RIGHT ? -1 : -2, y, 0);
            }
            default -> throw new IllegalArgumentException("Unknown blocking animation: " + style);
         }
         pose.translate(-.14142136f * side, .08f, .14142136f);
         pose.rotateDegrees(Axis.XP, -102.25f);
         pose.rotateDegrees(Axis.YP, side * 13.365f);
         pose.rotateDegrees(Axis.ZP, side * 78.05f);
      }

      private static Quaternionf axis(float degrees, float x, float y, float z) {
         float length = (float)Math.sqrt(x * x + y * y + z * z);
         return new Quaternionf().rotationAxis((float)Math.toRadians(degrees), x / length, y / length, z / length);
      }
      private static void swingOffset(PoseStack pose, int side, float swing) {
         float sine1 = (float)Math.sin(swing * swing * Math.PI), sine = (float)Math.sin(Math.sqrt(swing) * Math.PI);
         pose.rotateDegrees(Axis.YP, side * (45 + sine1 * -20));
         pose.rotateDegrees(Axis.ZP, side * sine * -20);
         pose.rotateDegrees(Axis.XP, sine * -80);
         pose.rotateDegrees(Axis.YP, side * -45);
      }
   }
}
