package com.samsara.command;

import com.samsara.SamsaraClient;
import com.samsara.module.Feature;
import com.samsara.module.FeatureManager;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.platform.InputConstants.Type;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class BindCommand extends Command {
   private static final String f56 = "none";
   private static final String f57 = "unbind";
   private static final List f60 = pm$4();
   private static final String f59 = "NONE";
   private static final String f58 = "KEY_";
   private static final String f52 = "Bind";
   private static final String f54 = "bind";
   private static final String f53 = "bind <name> <key>";
   private static final String f55 = "b";

   private static List pm$4() {
      ArrayList var0 = new ArrayList();

      for (Field var4 : InputConstants.class.getDeclaredFields()) {
         if (var4.getName().startsWith(f58) && var4.getType() == int.class) {
            String var5 = var4.getName().substring(4);
            var0.add(var5);
         }
      }

      var0.add(f59);
      return var0;
   }

   @Override
   public Collection complete(String[] var1) {
      if (var1.length <= 1) {
         return FeatureManager.getModules().stream().map(Feature::getName).toList();
      } else {
         return var1.length == 2 ? f60 : List.of();
      }
   }

   private static Key pm$5(String var0) {
      String var1 = var0.toUpperCase();

      try {
         Field var2 = InputConstants.class.getDeclaredField("KEY_" + var1);
         if (var2.getType() != int.class) {
            return InputConstants.UNKNOWN;
         } else {
            int var3 = var2.getInt(null);
            return Type.KEYBOARD.getOrCreate(var3);
         }
      } catch (ReflectiveOperationException var4) {
         return InputConstants.UNKNOWN;
      }
   }

   public BindCommand() {
      super(f52, f53, new String[]{f54, f55});
   }

   @Override
   public void execute(String[] var1, String var2) {
      if (var1.length <= 2) {
         SamsaraClient.sendPrefixedMessage("Usage: ." + this.syntax);
      } else {
         String var3 = var1[1];
         String var4 = var1[2];
         Feature var5 = null;
         Iterator var6 = FeatureManager.getModules().iterator();

         while (true) {
            if (var6.hasNext()) {
               Feature var7 = (Feature)var6.next();
               if (!var7.getName().equalsIgnoreCase(var3)) {
                  continue;
               }

               var5 = var7;
            }

            if (var5 == null) {
               SamsaraClient.sendPrefixedMessage("Invalid module: " + var3);
               return;
            }

            if (var4.equalsIgnoreCase(f56) || var4.equalsIgnoreCase(f57)) {
               var5.setKey(InputConstants.UNKNOWN.getValue());
               SamsaraClient.sendPrefixedMessage("Unbound " + var5.getName());
               return;
            }

            Key var8 = pm$5(var4);
            if (var8 == InputConstants.UNKNOWN) {
               SamsaraClient.sendPrefixedMessage("Invalid key: " + var4);
               return;
            }

            var5.setKey(var8.getValue());
            SamsaraClient.sendPrefixedMessage("Bound " + var5.getName() + " to " + var4.toUpperCase());
            break;
         }
      }
   }
}
