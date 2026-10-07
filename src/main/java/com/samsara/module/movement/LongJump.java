package com.samsara.module.movement;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.event.impl.EventPacketReceive;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.module.FeatureManager;
import com.samsara.setting.BooleanSetting;
import com.samsara.setting.ModeSetting;
import com.samsara.setting.NumberSetting;
import com.samsara.util.ClientColors;
import com.samsara.util.WorldToScreenProjector;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import mixins.MultiPlayerGameModeAccessor;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

public class LongJump extends Feature {
   private static final String MODE_LABEL = "Mode";
   private static final String FIREBALL_LABEL = "Fireball";
   private static final String FIREBALL2_LABEL = "Fireball2";
   private static final String LONG_JUMP_LABEL = "LongJump";
   private static final String FIREBALL_DELAY_LABEL = "Fireball Delay";
   private static final String SHOW_PROGRESS_LABEL = "Show Progress";

   private final ModeSetting mode;
   private final NumberSetting fireballDelay;
   private final BooleanSetting showProgress;

   public final List<TimedPacket> delayedPackets;

   private double previousProgress;
   private int jumpTicks;
   private int progressAlpha;
   private int delayTicksElapsed;
   private boolean delayingPackets;
   private double progress;
   private int previousProgressAlpha;
   private int previousSlot;

   private void updatePacketDelay() {
      this.previousProgress = this.progress;
      if (this.delayingPackets) {
         this.delayTicksElapsed++;
         this.progress = (double)((float)this.delayTicksElapsed * 50.0F / (float)this.fireballDelay.getValue());
      }

      if ((double)(this.delayTicksElapsed * 50) > this.fireballDelay.getValue() || !this.delayingPackets) {
         this.delayTicksElapsed = 0;
         this.progress = 0.0;
         this.delayingPackets = false;
      }

      int targetAlpha = this.delayingPackets ? 255 : 0;
      this.previousProgressAlpha = this.progressAlpha;
      this.progressAlpha = (int)Mth.lerp(0.5F, (float)this.progressAlpha, (float)targetAlpha);
      synchronized (this.delayedPackets) {
         Iterator<TimedPacket> iterator = this.delayedPackets.iterator();

         while (iterator.hasNext()) {
            TimedPacket timedPacket = iterator.next();
            if (timedPacket.isReady() || !this.delayingPackets) {
               iterator.remove();
               mc.execute(() -> timedPacket.packet.handle(mc.getConnection()));
            }
         }
      }
   }

   @Override
   public void onEnable() {
      this.jumpTicks = 0;
      this.previousSlot = -1;
      this.delayTicksElapsed = 0;
      this.delayingPackets = false;
      this.previousProgress = 0.0;
      this.progress = 0.0;
      this.previousProgressAlpha = 0;
      this.progressAlpha = 0;
   }

   @Override
   public void onDisable() {
      synchronized (this.delayedPackets) {
         for (TimedPacket timedPacket : this.delayedPackets) {
            mc.execute(() -> timedPacket.packet.handle(mc.getConnection()));
         }

         this.delayedPackets.clear();
         this.delayingPackets = false;
      }

      this.jumpTicks = 0;
      this.previousSlot = -1;
      this.delayTicksElapsed = 0;
   }

   private int findFireChargeSlot() {
      for (int slot = 0; slot < 9; slot++) {
         if (mc.player.getInventory().getItem(slot).is(Items.FIRE_CHARGE)) {
            return slot;
         }
      }

      return -1;
   }

   @Override
   public void onEvent(Event event) {
      if (event == Events.RENDER_2D && this.showProgress.getValue()) this.renderProgress();
      else if (event == Events.ROTATION) this.updateFireballJump();
      else if (event == Events.POST_MOVE_INPUT && this.jumpTicks == (this.mode.is(FIREBALL_LABEL) ? 4 : 20)) {
         mc.player.input.makeJump();
      } else if (event instanceof EventPacketReceive receiving) this.handlePacketReceive(receiving);
   }

   private void renderProgress() {
      if (!this.delayingPackets && this.progressAlpha <= 5) {
         this.previousProgress = 0.0;
      } else {
         float barWidth = 100.0F;
         float barHeight = 4.0F;
         float barX = (float)mc.getWindow().getGuiScaledWidth() * 0.5F - barWidth * 0.5F;
         float barY = (float)mc.getWindow().getGuiScaledHeight() * 0.5F + 20.0F;
         float partialTick = Events.RENDER_2D.getPartialTick();
         double progress = this.previousProgress + (this.progress - this.previousProgress) * (double)partialTick;
         int alpha = (int)((float)this.previousProgressAlpha + (float)(this.progressAlpha - this.previousProgressAlpha) * partialTick);
         int progressColor = ClientColors.colorAtOffset(0) & 16777215 | alpha << 24;
         int borderColor = alpha << 24;
         int backgroundColor = (int)((float)alpha * 0.5F) << 24 | 6316128;
         WorldToScreenProjector.drawProgressBar(Events.RENDER_2D, (int)barX, (int)barY, (int)barWidth, (int)barHeight, progress, borderColor, backgroundColor, progressColor);
      }
   }

   private void updateFireballJump() {
      this.setSuffix(this.mode.getValue());
      if (FeatureManager.scaffold.isEnabled()) {
         FeatureManager.scaffold.toggle();
      }

      int fireChargeSlot = this.findFireChargeSlot();
      if (fireChargeSlot != -1) {
         Events.ROTATION.setPitch(90.0F);
         switch (this.jumpTicks) {
            case 0:
               this.previousSlot = mc.player.getInventory().getSelectedSlot();
               mc.player.getInventory().setSelectedSlot(fireChargeSlot);
               break;
            case 1:
               ((MultiPlayerGameModeAccessor)mc.gameMode)
                  .invokeStartPrediction(mc.level, sequence -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, sequence, Events.ROTATION.getYaw(), Events.ROTATION.getPitch()));
               break;
            case 2:
               if (this.mode.is(FIREBALL_LABEL)) {
                  mc.player.getInventory().setSelectedSlot(this.previousSlot);
               }
               break;
            case 15:
               if (this.mode.is(FIREBALL2_LABEL)) {
                  ((MultiPlayerGameModeAccessor)mc.gameMode)
                     .invokeStartPrediction(
                        mc.level, sequence -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, sequence, Events.ROTATION.getYaw(), Events.ROTATION.getPitch())
                     );
               }
               break;
            case 16:
               mc.player.getInventory().setSelectedSlot(this.previousSlot);
         }
      }

      this.jumpTicks++;
      this.updatePacketDelay();
      if (mc.player.onGround() && this.jumpTicks > 20) {
         this.toggle();
      }
   }

   private void handlePacketReceive(EventPacketReceive receiving) {
      if (receiving.getPacket() instanceof ClientboundSetEntityMotionPacket setEntityMotionPacket) {
         if (setEntityMotionPacket.id() == mc.player.getId()) {
            this.delayPacket(receiving);

            this.delayingPackets = true;
         }
      } else if (this.delayingPackets) {
         this.delayPacket(receiving);
      }
   }

   private void delayPacket(EventPacketReceive receiving) {
      synchronized (this.delayedPackets) {
         long delayMillis = this.mode.is(FIREBALL_LABEL) ? (long)this.fireballDelay.getValue() : (long)this.fireballDelay.getValue() + 800L;
         this.delayedPackets.add(new TimedPacket(receiving.getPacket(), delayMillis));
         receiving.setCancelled(true);
      }
   }

   public LongJump() {
      super(LONG_JUMP_LABEL, Category.MOVEMENT);
      this.mode = new ModeSetting(MODE_LABEL, this, FIREBALL_LABEL, new String[]{FIREBALL_LABEL, FIREBALL2_LABEL});
      this.fireballDelay = new NumberSetting(FIREBALL_DELAY_LABEL, this, 200.0, 100.0, 1000.0, 50.0);
      this.showProgress = new BooleanSetting(SHOW_PROGRESS_LABEL, this, false);
      this.delayedPackets = new ArrayList<>();
      this.previousSlot = -1;
   }

   public static class TimedPacket {
      public final Packet packet;
      private final long releaseTimeMillis;

      public TimedPacket(Packet packet, long delayMillis) {
         this.packet = packet;
         this.releaseTimeMillis = System.currentTimeMillis() + delayMillis;
      }

      public boolean isReady() {
         return System.currentTimeMillis() >= this.releaseTimeMillis;
      }
   }
}
