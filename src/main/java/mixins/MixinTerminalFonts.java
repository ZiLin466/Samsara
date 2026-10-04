package mixins;

import com.samsara.ui.terminal.TerminalFontResources;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.VanillaPackResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(VanillaPackResources.class)
public abstract class MixinTerminalFonts {
   @ModifyVariable(method="<init>",at=@At("HEAD"),argsOnly=true)
   private static List<PackResources> samsara$fonts(List<PackResources> packs) {
      var result=new ArrayList<>(packs);result.add(new TerminalFontResources());return result;
   }
}
