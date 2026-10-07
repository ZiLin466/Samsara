package com.samsara.test.event;

import com.samsara.event.Event;
import com.samsara.event.EventBus;
import com.samsara.event.impl.EventPacketReceive;
import com.samsara.event.impl.EventPacketSend;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class EventBusTest {
   private static final class Fixture extends Feature {
      boolean active = true;
      final int priority;
      final Consumer<Event> listener;
      Fixture(String name, int priority, Consumer<Event> listener) {
         super(name, Category.COMBAT);
         this.priority = priority;
         this.listener = listener;
      }
      @Override public boolean isEnabled() { return this.active; }
      @Override public int getPriority(Event event) { return this.priority; }
      @Override public void onEvent(Event event) { this.listener.accept(event); }
   }

   @Test void prioritiesAreAscendingStableAndCancellationRemainsVisibleToLaterHandlers() {
      var order = new ArrayList<String>();
      var bus = new EventBus(List.of(new EventPacketSend()));
      var late = new Fixture("late", 1, event -> { assertTrue(event.isCancelled()); order.add("late"); });
      var first = new Fixture("first", -10, event -> { assertFalse(event.isCancelled()); event.setCancelled(true); order.add("first"); });
      var equal = new Fixture("equal", -10, event -> { assertTrue(event.isCancelled()); order.add("equal"); });
      var disabled = new Fixture("disabled", -20, event -> fail("Disabled handler")); disabled.active = false;
      bus.refresh(List.of(late, first, equal, disabled));
      var event = new EventPacketSend(new ServerboundPongPacket(1));
      bus.dispatch(event);
      bus.dispatch(event);
      assertEquals(List.of("first", "equal", "late", "first", "equal", "late"), order);
   }

   @Test void refreshingDuringDispatchChangesOnlyTheNextDispatchSnapshot() {
      var calls = new ArrayList<String>();
      var bus = new EventBus(List.of(new EventPacketSend()));
      var replaced = new Fixture("replaced", 0, event -> calls.add("replaced"));
      var replacement = new Fixture("replacement", 0, event -> calls.add("replacement"));
      var first = new Fixture("first", -1, event -> { calls.add("first"); bus.refresh(List.of(replacement)); });
      bus.refresh(List.of(first, replaced));
      bus.dispatch(new EventPacketSend(new ServerboundPongPacket(1)));
      assertEquals(List.of("first", "replaced"), calls);
      bus.dispatch(new EventPacketSend(new ServerboundPongPacket(2)));
      assertEquals(List.of("first", "replaced", "replacement"), calls);
   }

   @Test void nestedSendsCannotOverwriteTheOuterPacketOrItsCancellation() {
      var packets = new ArrayList<Integer>();
      var bus = new EventBus(List.of(new EventPacketSend()));
      var nested = new Fixture("nested", -1, event -> {
         var sending = (EventPacketSend)event;
         int id = ((ServerboundPongPacket)sending.getPacket()).getId();
         if (id == 1) {
            sending.setCancelled(true);
            bus.dispatch(new EventPacketSend(new ServerboundPongPacket(2)));
            assertEquals(1, ((ServerboundPongPacket)sending.getPacket()).getId());
            assertTrue(sending.isCancelled());
         } else assertFalse(sending.isCancelled());
      });
      var observer = new Fixture("observer", 0, event -> packets.add(((ServerboundPongPacket)((EventPacketSend)event).getPacket()).getId()));
      bus.refresh(List.of(nested, observer));
      bus.dispatch(new EventPacketSend(new ServerboundPongPacket(1)));
      assertEquals(List.of(2, 1), packets);
   }

   @Test void simultaneousPacketDispatchesKeepTheirOwnPayloadAndCancellation() throws Exception {
      var entered = new CountDownLatch(2);
      var bus = new EventBus(List.of(new EventPacketReceive()));
      bus.refresh(List.of(new Fixture("concurrent", 0, event -> {
         var receiving = (EventPacketReceive)event;
         var original = receiving.getPacket();
         receiving.setCancelled(((ServerboundPongPacket)original).getId() == 1);
         entered.countDown();
         try { assertTrue(entered.await(5, TimeUnit.SECONDS)); }
         catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new AssertionError(error); }
         assertSame(original, receiving.getPacket());
         assertEquals(((ServerboundPongPacket)original).getId() == 1, receiving.isCancelled());
      })));
      try (var executor = Executors.newFixedThreadPool(2)) {
         var first = executor.submit(() -> bus.dispatch(new EventPacketReceive(new ServerboundPongPacket(1))));
         var second = executor.submit(() -> bus.dispatch(new EventPacketReceive(new ServerboundPongPacket(2))));
         first.get(10, TimeUnit.SECONDS);
         second.get(10, TimeUnit.SECONDS);
      }
   }

   @Test void eventsCanBeConstructedBeforeTheModuleRegistryExists() throws Exception {
      var registry = com.samsara.module.FeatureManager.class.getDeclaredField("modules");
      registry.setAccessible(true);
      Object original = registry.get(null);
      registry.set(null, null);
      try {
         assertDoesNotThrow(() -> new EventPacketSend(new ServerboundPongPacket(1)));
         new EventBus(List.of(new EventPacketSend())).dispatch(new EventPacketSend(new ServerboundPongPacket(2)));
      } finally { registry.set(null, original); }
   }
}
