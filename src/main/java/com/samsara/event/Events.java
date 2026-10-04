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
   public static final EventPreMotion f1 = new EventPreMotion();
   public static final EventSprint f12 = new EventSprint();
   public static final EventMouseButton f15 = new EventMouseButton();
   public static final EventTick f4 = new EventTick();
   public static final EventPostMoveInput f8 = new EventPostMoveInput();
   public static final EventSound f16 = new EventSound();
   public static final EventPostMotion f2 = new EventPostMotion();
   public static final EventHurtCamera f6 = new EventHurtCamera();
   public static final EventPacketReceive f11 = new EventPacketReceive();
   public static final EventRender2D f5 = new EventRender2D();
   public static final EventSlowdown f9 = new EventSlowdown();
   public static final EventMoveInput f7 = new EventMoveInput();
   public static final EventRotation f3 = new EventRotation();
   public static final EventRenderNameTag f13 = new EventRenderNameTag();
   public static final EventEntityOutline f14 = new EventEntityOutline();
   public static final EventPacketSend f10 = new EventPacketSend();
   // Initialize the list after every event field; List.of rejects nulls.
   public static final List<Event> f17 = List.of(f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, f14, f15, f16);

   public static void m11() {
      if (Minecraft.getInstance().player != null) {
         for (Event var1 : f17) {
            var1.sortModules();
         }
      }
   }
}
