package com.samsara.module.visual;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.BooleanSetting;
import com.samsara.setting.NumberSetting;
import mixins.ClientClockManagerAccessor;
import mixins.ClockInstanceAccessor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.world.clock.WorldClocks;

public class Ambience extends Feature {
   private static final String MINECRAFT_ENTITY_PLAYER_ATTACK_CRIT_LABEL = "minecraft:entity.player.attack.crit";
   public final NumberSetting time;
   private final BooleanSetting cancelHitSound;
   private static final String MINECRAFT_ENTITY_PLAYER_ATTACK_SWEEP_LABEL = "minecraft:entity.player.attack.sweep";
   private static final String TIME_LABEL = "Time";
   private static final String MINECRAFT_ENTITY_PLAYER_ATTACK_WEAK_LABEL = "minecraft:entity.player.attack.weak";
   private static final String MINECRAFT_ENTITY_PLAYER_ATTACK_NODAMAGE_LABEL = "minecraft:entity.player.attack.nodamage";
   private static final String CANCEL_HIT_SOUND_LABEL = "Cancel Hit Sound";
   private int appliedTime;
   private static final String MINECRAFT_ENTITY_PLAYER_ATTACK_KNOCKBACK_LABEL = "minecraft:entity.player.attack.knockback";
   private static final String AMBIENCE_LABEL = "Ambience";
   private ClientLevel appliedLevel;
   private static final String MINECRAFT_ENTITY_PLAYER_ATTACK_STRONG_LABEL = "minecraft:entity.player.attack.strong";

   @Override
   public void onEvent(Event event) {
      if (event == Events.ROTATION) {
         int timeTicks = (int)this.time.getValue();
         ClientLevel level = mc.level;
         if (timeTicks != this.appliedTime || level != this.appliedLevel) {
            ClientClockManagerAccessor clockManager = (ClientClockManagerAccessor)level.clockManager();
            level.registryAccess().get(WorldClocks.OVERWORLD).ifPresent(clockId -> {
               Object clock = clockManager.getClocks().get(clockId);
               if (clock != null) {
                  ((ClockInstanceAccessor)clock).setTotalTicks((long)timeTicks);
               }
            });
         }

         this.appliedTime = timeTicks;
         this.appliedLevel = mc.level;
      }

      if (event == Events.PACKET_RECEIVE && Events.PACKET_RECEIVE.getPacket() instanceof ClientboundSetTimePacket) {
         event.setCancelled(true);
      }

      if (event == Events.SOUND && this.cancelHitSound.getValue()) {
         String soundId = Events.SOUND.getSoundId().toString();
         if (soundId.equals(MINECRAFT_ENTITY_PLAYER_ATTACK_NODAMAGE_LABEL) || soundId.equals(MINECRAFT_ENTITY_PLAYER_ATTACK_WEAK_LABEL) || soundId.equals(MINECRAFT_ENTITY_PLAYER_ATTACK_CRIT_LABEL) || soundId.equals(MINECRAFT_ENTITY_PLAYER_ATTACK_STRONG_LABEL) || soundId.equals(MINECRAFT_ENTITY_PLAYER_ATTACK_KNOCKBACK_LABEL) || soundId.equals(MINECRAFT_ENTITY_PLAYER_ATTACK_SWEEP_LABEL)) {
            event.setCancelled(true);
         }
      }
   }

   public Ambience() {
      super(AMBIENCE_LABEL, Category.VISUAL);
      this.cancelHitSound = new BooleanSetting(CANCEL_HIT_SOUND_LABEL, this, false);
      this.time = new NumberSetting(TIME_LABEL, this, 6000.0, 0.0, 24000.0, 100.0);
   }
}
