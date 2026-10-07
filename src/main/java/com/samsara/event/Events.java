package com.samsara.event;

import com.samsara.event.impl.EventEntityOutline;
import com.samsara.event.impl.EventHurtCamera;
import com.samsara.event.impl.EventMouseButton;
import com.samsara.event.impl.EventMoveInput;
import com.samsara.event.impl.EventPacketReceive;
import com.samsara.event.impl.EventPacketSend;
import com.samsara.event.impl.EventPostMotion;
import com.samsara.event.impl.EventPostMoveInput;
import com.samsara.event.impl.EventPreMotion;
import com.samsara.event.impl.EventRender2D;
import com.samsara.event.impl.EventRenderNameTag;
import com.samsara.event.impl.EventRotation;
import com.samsara.event.impl.EventSlowdown;
import com.samsara.event.impl.EventSound;
import com.samsara.event.impl.EventSprint;
import com.samsara.event.impl.EventTick;
import java.util.List;
import net.minecraft.client.Minecraft;

public class Events {
   public static final EventPreMotion PRE_MOTION = new EventPreMotion();
   public static final EventSprint SPRINT = new EventSprint();
   public static final EventMouseButton MOUSE_BUTTON = new EventMouseButton();
   public static final EventTick TICK = new EventTick();
   public static final EventPostMoveInput POST_MOVE_INPUT = new EventPostMoveInput();
   public static final EventSound SOUND = new EventSound();
   public static final EventPostMotion POST_MOTION = new EventPostMotion();
   public static final EventHurtCamera HURT_CAMERA = new EventHurtCamera();
   public static final EventPacketReceive PACKET_RECEIVE = new EventPacketReceive();
   public static final EventRender2D RENDER_2D = new EventRender2D();
   public static final EventSlowdown SLOWDOWN = new EventSlowdown();
   public static final EventMoveInput MOVE_INPUT = new EventMoveInput();
   public static final EventRotation ROTATION = new EventRotation();
   public static final EventRenderNameTag RENDER_NAME_TAG = new EventRenderNameTag();
   public static final EventEntityOutline ENTITY_OUTLINE = new EventEntityOutline();
   public static final EventPacketSend PACKET_SEND = new EventPacketSend();
   // Initialize the list after every event field; List.of rejects nulls.
   public static final List<Event> ALL_EVENTS = List.of(PRE_MOTION, POST_MOTION, ROTATION, TICK, RENDER_2D, HURT_CAMERA, MOVE_INPUT, POST_MOVE_INPUT, SLOWDOWN, PACKET_SEND, PACKET_RECEIVE, SPRINT, RENDER_NAME_TAG, ENTITY_OUTLINE, MOUSE_BUTTON, SOUND);

   public static void refreshListeners() {
      if (Minecraft.getInstance().player != null) {
         for (Event event : ALL_EVENTS) {
            event.sortModules();
         }
      }
   }
}
