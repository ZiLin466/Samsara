package com.samsara.command;

import com.samsara.SamsaraClient;
import com.samsara.module.Feature;
import com.samsara.module.FeatureManager;
import java.util.Collection;
import java.util.List;

public class ToggleCommand extends Command {
   private static final String DISABLED_LABEL = "Disabled";
   private static final String PRIMARY_ALIAS = "toggle";
   private static final String INVALID_MODULE_LABEL = "Invalid module";
   private static final String ENABLED_LABEL = "Enabled";
   private static final String TOGGLE_NAME_LABEL = "toggle <name>";
   private static final String T_LABEL = "t";
   private static final String COMMAND_NAME = "Toggle";

   public ToggleCommand() {
      super(COMMAND_NAME, TOGGLE_NAME_LABEL, new String[]{PRIMARY_ALIAS, T_LABEL});
   }

   @Override
   public void execute(String[] arguments, String commandLine) {
      if (arguments.length > 1) {
         String moduleName = arguments[1];
         boolean found = false;

         for (Feature feature : FeatureManager.getModules()) {
            if (feature.getName().equalsIgnoreCase(moduleName)) {
               feature.toggle();
               SamsaraClient.sendPrefixedMessage((feature.isEnabled() ? ENABLED_LABEL : DISABLED_LABEL) + " " + moduleName);
               found = true;
               break;
            }
         }

         if (!found) {
            SamsaraClient.sendPrefixedMessage(INVALID_MODULE_LABEL);
         }
      } else {
         SamsaraClient.sendPrefixedMessage("Usage: ." + this.syntax);
      }
   }

   @Override
   public Collection complete(String[] arguments) {
      return arguments.length <= 1 ? FeatureManager.getModules().stream().map(Feature::getName).toList() : List.of();
   }
}
