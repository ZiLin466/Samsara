package com.samsara.module.misc;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.BooleanSetting;
import java.nio.charset.StandardCharsets;
import mixins.ClientInputAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec2;

public class Disabler extends Feature {
   private static final String f350 = "Watchdog InvMove";
   private static final String f348 = "KeepAlive Packet";
   private final BooleanSetting f353;
   private final BooleanSetting f352;
   private static final String f347 = "Disabler";
   int ticks;
   private final BooleanSetting f351 = new BooleanSetting(f348, this, false);
   private static final String f349 = "Sprint Packet";

   @Override
   public void onEvent(Event var1) {
      if (var1 == Events.f8 && this.ticks > 0) {
         mc.player.input.keyPresses = new Input(false, false, false, false, false, false, false);
         ClientInputAccessor var2 = (ClientInputAccessor)mc.player.input;
         var2.setMoveVector(new Vec2(0.0F, 0.0F));
         this.ticks--;
      }

      if (var1 == Events.f10) {
         Packet var5 = Events.f10.m44();
         if (this.f351.m215() && var5 instanceof ServerboundKeepAlivePacket) {
            var1.setCancelled(true);
         }

         if (this.f352.m215()
            && var5 instanceof ServerboundPlayerCommandPacket var3
            && (var3.getAction() == Action.START_SPRINTING || var3.getAction() == Action.STOP_SPRINTING)) {
            var1.setCancelled(true);
         }

         if (this.f353.m215() && Events.f10.m44() instanceof ServerboundPlayerCommandPacket var6 && var6.getAction() == Action.OPEN_INVENTORY) {
            var1.setCancelled(true);
         }

         if (this.f353.m215() && var5 instanceof ServerboundContainerClickPacket var7) {
            Minecraft var8 = Minecraft.getInstance();
            if (var7.containerInput() == ContainerInput.PICKUP && var8.getConnection() != null && var8.player.containerMenu instanceof InventoryMenu) {
               this.ticks = 5;
            }
         }
      }
   }

   public Disabler() {
      super(f347, Category.MISC);
      this.f352 = new BooleanSetting(f349, this, false);
      this.f353 = new BooleanSetting(f350, this, false);
      this.ticks = -1;
   }
}
