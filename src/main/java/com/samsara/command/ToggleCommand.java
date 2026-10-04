package com.samsara.command;

import com.samsara.SamsaraClient;
import com.samsara.module.Feature;
import com.samsara.module.FeatureManager;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

public class ToggleCommand extends Command {
   private static final String f76 = "Disabled";
   private static final String f73 = "toggle";
   private static final String f77 = "Invalid module";
   private static final String f75 = "Enabled";
   private static final String f72 = "toggle <name>";
   private static final String f74 = "t";
   private static final String f71 = "Toggle";

   public ToggleCommand() {
      super(f71, f72, new String[]{f73, f74});
   }

   @Override
   public void execute(String[] var1, String var2) {
      if (var1.length > 1) {
         String var3 = var1[1];
         boolean var4 = false;

         for (Feature var6 : FeatureManager.getModules()) {
            if (var6.getName().equalsIgnoreCase(var3)) {
               var6.toggle();
               SamsaraClient.sendPrefixedMessage((var6.isEnabled() ? f75 : f76) + " " + var3);
               var4 = true;
               break;
            }
         }

         if (!var4) {
            SamsaraClient.sendPrefixedMessage(f77);
         }
      } else {
         SamsaraClient.sendPrefixedMessage("Usage: ." + this.syntax);
      }
   }

   @Override
   public Collection complete(String[] var1) {
      return var1.length <= 1 ? FeatureManager.getModules().stream().map(Feature::getName).toList() : List.of();
   }
}
