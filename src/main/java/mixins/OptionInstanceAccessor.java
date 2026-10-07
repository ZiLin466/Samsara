package mixins;

import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({OptionInstance.class})
public interface OptionInstanceAccessor<T> {
   @Accessor("value")
   void setValue(Object value);

   @Accessor("value")
   Object getValue();
}
