package com.samsara.module.player;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.ModeSetting;
import java.nio.charset.StandardCharsets;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class NoFall extends Feature {
   private static final String f536 = "NoGround";
   private static final String f535 = "Ground";
   private static final String f534 = "Universal";
   private static final String f532 = "NoFall";
   private static final String f533 = "Mode";
   private final ModeSetting f537 = new ModeSetting(f533, this, f534, new String[]{f534, f535, f536});

   @Override
   public void onEvent(Event var1) {
      if (var1 == Events.f1) {
         String var2 = this.f537.m224();
         byte var3 = -1;
         int var10000 = var2.hashCode();
         if (var10000 == 370287304) {
            if (var2.equals(f536)) {
               var3 = 1;
            }
         } else if (var10000 == 2141373863) {
            if (var2.equals(f535)) {
               var3 = 0;
            }
         }

         switch (var3) {
            case 0:
               Events.f1.m72(true);
               break;
            case 1:
               Events.f1.m72(false);
         }
      }

      if (var1 == Events.f3 && mc.player.fallDistance >= 3.0 && this.f537.m228(f534)) {
         mc.player.fallDistance = 0.0;
         mc.getConnection().send(new Pos(mc.player.getX(), mc.player.getY(), mc.player.getZ(), true, mc.player.horizontalCollision));
      }
   }

   public NoFall() {
      super(f532, Category.PLAYER);
   }

   private boolean pm$99(double var1) {
      AABB var3 = mc.player.getBoundingBox();
      double var4 = var3.minY;
      int var6 = Mth.floor(var3.minX);
      int var7 = Mth.floor(var3.maxX);
      int var8 = Mth.floor(var3.minZ);
      int var9 = Mth.floor(var3.maxZ);

      for (double var10 = var4 - 0.01; var10 >= var4 - var1; var10 -= 0.1) {
         int var12 = Mth.floor(var10);

         for (int var13 = var6; var13 <= var7; var13++) {
            for (int var14 = var8; var14 <= var9; var14++) {
               BlockState var15 = mc.level.getBlockState(new BlockPos(var13, var12, var14));
               if (!var15.isAir() && !var15.getCollisionShape(mc.level, new BlockPos(var13, var12, var14)).isEmpty()) {
                  return true;
               }
            }
         }
      }

      return false;
   }
}
