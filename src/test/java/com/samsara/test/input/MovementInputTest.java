package com.samsara.test.input;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Feature;
import com.samsara.module.FeatureManager;
import com.samsara.module.Category;
import java.lang.reflect.Method;
import mixins.MixinKeyboardInput;
import net.minecraft.world.entity.player.Input;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class MovementInputTest {
   @Test void inputHandlerUsesCurrentKeysAndReturnsForcedJumpBeforeVanillaComputesMovement() throws Exception {
      if (FeatureManager.getModules() == null) FeatureManager.registerModules();
      var modules = FeatureManager.getModules();
      Feature fixture = new Feature("Input fixture", Category.MOVEMENT) {
         @Override public void onEvent(Event event) {
            if (event == Events.MOVE_INPUT) {
               assertTrue(Events.MOVE_INPUT.isForward()); assertFalse(Events.MOVE_INPUT.isBackward());
               assertFalse(Events.MOVE_INPUT.isJump()); assertTrue(Events.MOVE_INPUT.isSprint());
               Events.MOVE_INPUT.setJump(true);
            }
         }
      };
      var enabled = Feature.class.getDeclaredField("enabled"); enabled.setAccessible(true); enabled.set(fixture, true);
      modules.add(fixture); Events.initializeListeners();
      try {
         Method method = MixinKeyboardInput.class.getDeclaredMethod("samsara$movementInput", Input.class);
         method.setAccessible(true);
         Input original = new Input(true, false, false, false, false, false, true);
         Input changed = (Input)method.invoke(new MixinKeyboardInput(), original);
         assertFalse(original.jump()); assertTrue(changed.jump());
         assertTrue(changed.forward()); assertTrue(changed.sprint());
         assertFalse(changed.backward()); assertFalse(changed.left()); assertFalse(changed.right());
      } finally { modules.remove(fixture); Events.initializeListeners(); }
   }
}
