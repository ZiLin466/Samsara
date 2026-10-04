package com.samsara.util.animation;

import java.util.function.Function;
import net.minecraft.util.Mth;

public enum Easing {
   LINEAR(x -> x),
   EASE_IN_QUAD(x -> x * x),
   EASE_OUT_QUAD(x -> x * (2 - x)),
   EASE_IN_OUT_QUAD(x -> x < 0.5 ? 2 * x * x : -1 + (4 - 2 * x) * x),
   EASE_IN_CUBIC(x -> x * x * x),
   EASE_OUT_CUBIC(x -> 1 - (float)Math.pow(1 - x, 3)),
   EASE_IN_OUT_CUBIC(x -> x < 0.5 ? 4 * x * x * x : 1 - (float)Math.pow(-2 * x + 2, 3) / 2),
   EASE_IN_QUART(x -> x * x * x * x),
   EASE_OUT_QUART(x -> 1 - (float)Math.pow(1 - x, 4)),
   EASE_IN_OUT_QUART(x -> x < 0.5 ? 8 * x * x * x * x : 1 - (float)Math.pow(-2 * x + 2, 4) / 2),
   EASE_IN_QUINT(x -> x * x * x * x * x),
   EASE_OUT_QUINT(x -> 1 - (float)Math.pow(1 - x, 5)),
   EASE_IN_OUT_QUINT(x -> x < 0.5 ? 16 * x * x * x * x * x : 1 - (float)Math.pow(-2 * x + 2, 5) / 2),
   EASE_IN_SINE(x -> 1 - Mth.cos((float)(x * Math.PI * 0.5))),
   EASE_OUT_SINE(x -> Mth.sin((float)(x * Math.PI * 0.5))),
   EASE_IN_OUT_SINE(x -> 1 - Mth.cos((float)(Math.PI * x * 0.5))),
   EASE_IN_EXPO(x -> x == 0 ? 0 : (float)Math.pow(2, 10 * x - 10)),
   EASE_OUT_EXPO(x -> x == 1 ? 1 : 1 - (float)Math.pow(2, -10 * x)),
   EASE_IN_OUT_EXPO(x -> x == 0 ? 0 : x == 1 ? 1 : x < 0.5 ? (float)Math.pow(2, 20 * x - 10) / 2 : (2 - (float)Math.pow(2, -20 * x + 10)) / 2),
   EASE_IN_CIRC(x -> 1 - (float)Math.sqrt(1 - x * x)),
   EASE_OUT_CIRC(x -> (float)Math.sqrt(1 - (x - 1) * (x - 1))),
   EASE_IN_OUT_CIRC(x -> x < 0.5 ? (1 - (float)Math.sqrt(1 - 4 * x * x)) / 2 : ((float)Math.sqrt(1 - 4 * (x - 1) * (x - 1)) + 1) / 2),
   EASE_IN_BACK(x -> 2.70158F * x * x * x - 1.70158F * x * x),
   EASE_OUT_ELASTIC(x -> x == 0 ? 0 : x == 1 ? 1 : (float)(Math.pow(2, -10 * x) * Math.sin((x * 10 - 0.75) * (2 * Math.PI / 3)) * 0.5 + 1)),
   SIGMOID(x -> 1 / (1 + (float)Math.exp(-x)));

   private final Function<Float, Float> function;

   Easing(Function<Float, Float> function) {
      this.function = function;
   }

   public Function<Float, Float> getFunction() {
      return this.function;
   }
}
