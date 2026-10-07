package com.samsara.test.module;

import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.module.FeatureManager;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class FeatureLifecycleTest {
   @Test void deferredWorldInitializationPrecedesEveryHeadCallbackAndDisabledModulesStillCleanUp() throws Exception {
      var calls = new ArrayList<String>();
      Feature first = fixture("first", calls);
      Feature second = fixture("second", calls);
      var registry = FeatureManager.class.getDeclaredField("modules");
      registry.setAccessible(true);
      Object original = registry.get(null);
      registry.set(null, List.of(first, second));
      try {
         FeatureManager.clientTick();
         FeatureManager.clientTickEnd();
         assertEquals(List.of("first.initialize", "second.initialize", "first.head", "second.head",
            "first.tail", "second.tail"), calls);
      } finally { registry.set(null, original); }
   }

   private static Feature fixture(String name, List<String> calls) {
      return new Feature(name, Category.COMBAT) {
         @Override public void initializeWorldState() { calls.add(name + ".initialize"); }
         @Override public void clientTick() { calls.add(name + ".head"); }
         @Override public void clientTickEnd() { calls.add(name + ".tail"); }
      };
   }
}
