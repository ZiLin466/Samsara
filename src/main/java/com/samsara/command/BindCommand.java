package com.samsara.command;

import com.samsara.SamsaraClient;
import com.samsara.module.Feature;
import com.samsara.module.FeatureManager;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.platform.InputConstants.Type;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class BindCommand extends Command {
   private static final String NO_KEY_ALIAS = "none";
   private static final String UNBIND_LABEL = "unbind";
   private static final List bindableKeys = collectBindableKeys();
   private static final String UNBOUND_KEY_LABEL = "NONE";
   private static final String KEY_LABEL = "KEY_";
   private static final String COMMAND_NAME = "Bind";
   private static final String PRIMARY_ALIAS = "bind";
   private static final String BIND_NAME_KEY_LABEL = "bind <name> <key>";
   private static final String B_LABEL = "b";

   private static List collectBindableKeys() {
      ArrayList keyNames = new ArrayList();

      for (Field keyField : InputConstants.class.getDeclaredFields()) {
         if (keyField.getName().startsWith(KEY_LABEL) && keyField.getType() == int.class) {
            String keyName = keyField.getName().substring(4);
            keyNames.add(keyName);
         }
      }

      keyNames.add(UNBOUND_KEY_LABEL);
      return keyNames;
   }

   @Override
   public Collection complete(String[] arguments) {
      if (arguments.length <= 1) {
         return FeatureManager.getModules().stream().map(Feature::getName).toList();
      } else {
         return arguments.length == 2 ? bindableKeys : List.of();
      }
   }

   private static Key resolveKey(String keyName) {
      String uppercaseName = keyName.toUpperCase();

      try {
         Field keyField = InputConstants.class.getDeclaredField("KEY_" + uppercaseName);
         if (keyField.getType() != int.class) {
            return InputConstants.UNKNOWN;
         } else {
            int keyCode = keyField.getInt(null);
            return Type.KEYBOARD.getOrCreate(keyCode);
         }
      } catch (ReflectiveOperationException error) {
         return InputConstants.UNKNOWN;
      }
   }

   public BindCommand() {
      super(COMMAND_NAME, BIND_NAME_KEY_LABEL, new String[]{PRIMARY_ALIAS, B_LABEL});
   }

   @Override
   public void execute(String[] arguments, String commandLine) {
      if (arguments.length <= 2) {
         SamsaraClient.sendPrefixedMessage("Usage: ." + this.syntax);
      } else {
         String moduleName = arguments[1];
         String keyName = arguments[2];
         Feature feature = null;
         Iterator resolvedKey = FeatureManager.getModules().iterator();

         while (true) {
            if (resolvedKey.hasNext()) {
               Feature boundFeature = (Feature)resolvedKey.next();
               if (!boundFeature.getName().equalsIgnoreCase(moduleName)) {
                  continue;
               }

               feature = boundFeature;
            }

            if (feature == null) {
               SamsaraClient.sendPrefixedMessage("Invalid module: " + moduleName);
               return;
            }

            if (keyName.equalsIgnoreCase(NO_KEY_ALIAS) || keyName.equalsIgnoreCase(UNBIND_LABEL)) {
               feature.setKey(InputConstants.UNKNOWN.getValue());
               SamsaraClient.sendPrefixedMessage("Unbound " + feature.getName());
               return;
            }

            Key attackKey = resolveKey(keyName);
            if (attackKey == InputConstants.UNKNOWN) {
               SamsaraClient.sendPrefixedMessage("Invalid key: " + keyName);
               return;
            }

            feature.setKey(attackKey.getValue());
            SamsaraClient.sendPrefixedMessage("Bound " + feature.getName() + " to " + keyName.toUpperCase());
            break;
         }
      }
   }
}
