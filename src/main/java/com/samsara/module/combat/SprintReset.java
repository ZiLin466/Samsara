package com.samsara.module.combat;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.ModeSetting;
import com.samsara.setting.NumberSetting;
import java.nio.charset.StandardCharsets;
import mixins.ClientInputAccessor;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;

public class SprintReset extends Feature {
   private static final String f307 = "WTap";
   private static final String f305 = "Mode";
   private final ModeSetting f311;
   private static final String f306 = "WTap";
   private static final String f304 = "Reset Delay";
   private final NumberSetting f309;
   private static final String f302 = "SprintReset";
   private static final String f303 = "Release Delay";
   private int f313;
   private int f312;
   private final NumberSetting f310;
   private static final String f308 = "STap";

   @Override
   public void onEvent(Event var1) {
      if (var1 == Events.f3) {
         this.setSuffix(this.f311.m224());
      }

      if (var1 == Events.f10 && Events.f10.m44() instanceof ServerboundAttackPacket var2) {
         Entity var7 = mc.level.getEntity(var2.entityId());
         if (var7 != null && mc.player.isSprinting() && this.f313 <= 0) {
            this.f312 = (int)this.f309.m220();
            this.f313 = (int)this.f310.m220();
         }
      }

      if (var1 == Events.f8) {
         if (this.f313 > 0) {
            this.f313--;
         }

         if (this.f312 > 0) {
            this.f312--;
            Input var6 = mc.player.input.keyPresses;
            ClientInputAccessor var8 = (ClientInputAccessor)mc.player.input;
            boolean var4 = this.f311.m228(f308);
            Vec2 var5 = new Vec2(0.0F, var4 ? -1.0F : 0.0F);
            if (var5.length() > 1.0F) {
               var5 = var5.normalized();
            }

            var8.setMoveVector(var5);
            mc.player.input.keyPresses = new Input(false, var4, false, false, var6.jump(), var6.shift(), var6.sprint());
         } else if (this.f312 == 0) {
            this.f312--;
         }
      }
   }

   public SprintReset() {
      super(f302, Category.COMBAT);
      this.f309 = new NumberSetting(f303, this, 2.0, 1.0, 5.0, 1.0);
      this.f310 = new NumberSetting(f304, this, 0.0, 0.0, 10.0, 1.0);
      this.f311 = new ModeSetting(f305, this, f306, new String[]{f307, f308});
   }
}
