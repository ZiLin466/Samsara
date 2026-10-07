package com.samsara.event;

import com.samsara.module.Feature;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** A dispatch uses one immutable listener snapshot, including during nested packet sends. */
public final class EventBus {
   private final List<Event> eventTypes;
   private volatile Map<Class<? extends Event>, List<Feature>> listeners = Map.of();

   public EventBus(List<Event> eventTypes) {
      this.eventTypes = List.copyOf(eventTypes);
   }

   public void refresh(List<Feature> modules) {
      List<Feature> enabled = modules.stream().filter(Feature::isEnabled).toList();
      Map<Class<? extends Event>, List<Feature>> next = new HashMap<>();
      for (Event type : this.eventTypes) {
         List<Feature> ordered = new ArrayList<>(enabled);
         // List.sort is stable: equal priorities retain module registration order.
         ordered.sort(Comparator.comparingInt(feature -> feature.getPriority(type)));
         next.put(type.getClass(), List.copyOf(ordered));
      }
      this.listeners = Map.copyOf(next);
   }

   public void dispatch(Event event) {
      event.setCancelled(false);
      for (Feature feature : this.listeners.getOrDefault(event.getClass(), List.of())) {
         feature.onEvent(event);
      }
   }
}
