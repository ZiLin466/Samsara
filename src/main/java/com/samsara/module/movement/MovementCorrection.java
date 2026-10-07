package com.samsara.module.movement;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.BooleanSetting;
import com.samsara.setting.ModeSetting;
import mixins.ClientInputAccessor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;

public class MovementCorrection extends Feature {
   private static final String MOVE_FIX_LABEL = "MoveFix";
   private final BooleanSetting usePrevYaw;
   private static final String USE_PREV_YAW_LABEL = "Use PrevYaw";
   private static final String SILENT_LABEL = "Silent";
   private final ModeSetting mode;
   private static final String MODE_LABEL = "Mode";
   private static final String STRICT_LABEL = "Strict";

   private float movementDirectionRadians(float yaw, float forward, float strafe) {
      if (forward == 0.0F && strafe == 0.0F) {
         return (float)Math.toRadians((double)yaw);
      } else {
         double yawRadians = Math.toRadians((double)yaw);
         double yawSin = Math.sin(yawRadians);
         double yawCos = Math.cos(yawRadians);
         double directionX = (double)forward * yawCos - (double)strafe * yawSin;
         double directionZ = (double)forward * yawSin + (double)strafe * yawCos;
         return (float)Math.atan2(directionZ, directionX);
      }
   }

   @Override
   public void onEvent(Event event) {
      if (event == Events.ROTATION) {
         this.setSuffix(this.mode.getValue());
         Events.ROTATION.setMovementCorrection(true);
         if (!this.usePrevYaw.getValue()) {
            Events.ROTATION.setUseClientRotation(false);
         }
      }

      if (event == Events.POST_MOVE_INPUT && this.mode.is(SILENT_LABEL)) {
         ClientInputAccessor inputAccessor = (ClientInputAccessor)mc.player.input;
         float forward = inputAccessor.getMoveVector().y;
         float strafe = inputAccessor.getMoveVector().x;
         if (forward == 0.0F && strafe == 0.0F) {
            return;
         }

         double desiredDirection = Mth.wrapDegrees(Math.toDegrees((double)this.movementDirectionRadians(Events.ROTATION.getYaw(), forward, strafe)));
         float correctedForward = 0.0F;
         float correctedStrafe = 0.0F;
         float smallestAngleDifference = Float.MAX_VALUE;

         for (float candidateForward = -1.0F; candidateForward <= 1.0F; candidateForward++) {
            for (float candidateStrafe = -1.0F; candidateStrafe <= 1.0F; candidateStrafe++) {
               if (candidateStrafe != 0.0F || candidateForward != 0.0F) {
                  double candidateDirection = Mth.wrapDegrees(Math.toDegrees((double)this.movementDirectionRadians(mc.player.getYRot(), candidateForward, candidateStrafe)));
                  double angleDifference = Math.abs(desiredDirection - candidateDirection);
                  if (angleDifference < (double)smallestAngleDifference) {
                     smallestAngleDifference = (float)angleDifference;
                     correctedForward = candidateForward;
                     correctedStrafe = candidateStrafe;
                  }
               }
            }
         }

         Vec2 movement = new Vec2(correctedStrafe, correctedForward);
         if (movement.length() > 1.0F) {
            movement = movement.normalized();
         }

         inputAccessor.setMoveVector(movement);
         mc.player.input.keyPresses = new Input(
            correctedForward > 0.0F,
            correctedForward < 0.0F,
            correctedStrafe > 0.0F,
            correctedStrafe < 0.0F,
            mc.player.input.keyPresses.jump(),
            mc.player.input.keyPresses.shift(),
            mc.player.input.keyPresses.sprint()
         );
      }
   }

   public MovementCorrection() {
      super(MOVE_FIX_LABEL, Category.MOVEMENT);
      this.mode = new ModeSetting(MODE_LABEL, this, SILENT_LABEL, new String[]{SILENT_LABEL, STRICT_LABEL});
      this.usePrevYaw = new BooleanSetting(USE_PREV_YAW_LABEL, this, true);
   }
}
