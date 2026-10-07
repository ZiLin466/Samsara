package com.samsara.module.combat;

import com.samsara.util.RotationUtil;
import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.NumberSetting;
import com.samsara.util.TargetFinder;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

public class AimAssist extends Feature {
   private static final String SPEED_LABEL = "Speed";
   private static final String RANGE_LABEL = "Range";
   private static final String AIM_ASSIST_LABEL = "AimAssist";
   private NumberSetting range = new NumberSetting(RANGE_LABEL, this, 6.0, 3.0, 8.0, 0.5);
   private NumberSetting speed = new NumberSetting(SPEED_LABEL, this, 5.0, 1.0, 10.0, 0.5);

   @Override
   public void onEvent(Event event) {
      if (event == Events.PRE_MOTION) {
         LivingEntity target = TargetFinder.nearestTarget(this.range.getValue(), true);
         if (target != null) {
            AABB bounds = target.getBoundingBox();
            double deltaX = Mth.clamp(mc.player.getX(), bounds.minX, bounds.maxX) - mc.player.getX();
            double deltaY = Mth.clamp(mc.player.getEyeY(), bounds.minY, bounds.maxY) - mc.player.getEyeY();
            double deltaZ = Mth.clamp(mc.player.getZ(), bounds.minZ, bounds.maxZ) - mc.player.getZ();
            double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
            float targetYaw = (float)(Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0);
            float targetPitch = (float)(-Math.toDegrees(Math.atan2(deltaY, horizontalDistance)));
            float yawDelta = Mth.wrapDegrees(targetYaw - mc.player.getYRot());
            float pitchDelta = targetPitch - mc.player.getXRot();
            float yawStep = (float)Math.min((double)Math.abs(yawDelta), this.speed.getValue());
            float pitchStep = (float)Math.min((double)Math.abs(pitchDelta), this.speed.getValue());
            float sensitivityStep = RotationUtil.mouseSensitivityStep(mc.options.sensitivity().get().floatValue());
            float quantizedYaw = RotationUtil.quantize(Math.copySign(yawStep, yawDelta), mc.player.getYRot(), sensitivityStep);
            float quantizedPitch = RotationUtil.quantize(Math.copySign(pitchStep, pitchDelta), mc.player.getXRot(), sensitivityStep);
            mc.player.setYRot(mc.player.getYRot() + quantizedYaw);
            mc.player.setXRot(mc.player.getXRot() + quantizedPitch);
         }
      }
   }

   public AimAssist() {
      super(AIM_ASSIST_LABEL, Category.COMBAT);
   }
}
