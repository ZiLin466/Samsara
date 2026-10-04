package com.samsara.ui.config;

import com.samsara.config.ConfigManager;
import com.samsara.util.ColorUtil;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class ConfigScreen extends ConfigScreenBase {
   private static final String f159 = "os.name";
   private static final String f163 = "open";
   private static final String f160 = "win";
   private static final String f156 = "Reset";
   private final Map<String, Boolean> f166;
   private int f165 = 1;
   private static final String f164 = "xdg-open";
   private static final String f153 = "Load";
   private static final String f162 = "mac";
   private static final String f155 = "Remove";
   private static final String f154 = "Save";
   private static final String f158 = "Open Folder";
   private static final String f161 = "explorer.exe";
   private static final String f157 = "Create Config";

   @Override
   public void m103(double var1, double var3, int var5) {
      int var6 = this.f5 + 17;
      String[] var7 = ConfigManager.getConfigNames();

      for (String var11 : var7) {
         if (var1 >= (double)this.f4 && var1 <= (double)(this.f4 + 100) && var3 >= (double)(var6 - 2) && var3 <= (double)(var6 + 10)) {
            if (var5 == 1) {
               this.f166.put(var11, !this.f166.getOrDefault(var11, false));
            } else if (var5 == 0) {
               ConfigManager.m33(var11);
            }

            return;
         }

         var6 += 12;
         if (this.f166.getOrDefault(var11, false)) {
            if (var1 >= (double)this.f4 && var1 <= (double)(this.f4 + 100) && var3 >= (double)(var6 - 2) && var3 <= (double)(var6 + 10)) {
               if (var5 == 0) {
                  ConfigManager.m33(var11);
               }

               return;
            }

            var6 += 12;
            if (var1 >= (double)this.f4 && var1 <= (double)(this.f4 + 100) && var3 >= (double)(var6 - 2) && var3 <= (double)(var6 + 10)) {
               if (var5 == 0) {
                  ConfigManager.m32(var11);
               }

               return;
            }

            var6 += 12;
            if (var1 >= (double)this.f4 && var1 <= (double)(this.f4 + 100) && var3 >= (double)(var6 - 2) && var3 <= (double)(var6 + 10)) {
               if (var5 == 0) {
                  ConfigManager.m36(var11);
                  this.f166.remove(var11);
               }

               return;
            }

            var6 += 12;
            if (var1 >= (double)this.f4 && var1 <= (double)(this.f4 + 100) && var3 >= (double)(var6 - 2) && var3 <= (double)(var6 + 10)) {
               if (var5 == 0) {
                  ConfigManager.m31(var11);
               }

               return;
            }

            var6 += 12;
         }
      }

      if (var1 >= (double)this.f4 && var1 <= (double)(this.f4 + 100) && var3 >= (double)(var6 - 2) && var3 <= (double)(var6 + 10)) {
         if (var5 == 0) {
            String var18 = "Config" + this.f165++;
            ConfigManager.m32(var18);
         }
      } else {
         var6 += 12;
         if (var1 >= (double)this.f4 && var1 <= (double)(this.f4 + 100) && var3 >= (double)(var6 - 2) && var3 <= (double)(var6 + 10)) {
            if (var5 == 0) {
               try {
                  File var17 = ConfigManager.getConfigDir();
                  if (!var17.exists()) {
                     var17.mkdirs();
                  }

                  String var19 = System.getProperty(f159).toLowerCase();
                  if (var19.contains(f160)) {
                     new ProcessBuilder(f161, var17.getAbsolutePath()).start();
                  } else if (var19.contains(f162)) {
                     new ProcessBuilder(f163, var17.getAbsolutePath()).start();
                  } else {
                     new ProcessBuilder(f164, var17.getAbsolutePath()).start();
                  }
               } catch (IOException var12) {
                  var12.printStackTrace();
               }
            }
         }
      }
   }

   @Override
   public void m100(GuiGraphicsExtractor var1) {
      int var2 = ColorUtil.m27();
      Minecraft var3 = Minecraft.getInstance();
      var1.fill(this.f4, this.f5, this.f4 + 100, this.f5 + 15, var2);
      var1.text(var3.font, this.f3, this.f4 + 4, this.f5 + 3, -1);
      int var4 = this.f5 + 17;

      for (String var8 : ConfigManager.getConfigNames()) {
         var1.fill(this.f4, var4 - 2, this.f4 + 100, var4 + 10, -2146430960);
         var1.text(var3.font, var8, this.f4 + 4, var4, -1426063361);
         var4 += 12;
         if (this.f166.getOrDefault(var8, false)) {
            var1.fill(this.f4, var4 - 2, this.f4 + 100, var4 + 46, -2145378272);
            var1.text(var3.font, f153, this.f4 + 4, var4, -11141291);
            var4 += 12;
            var1.text(var3.font, f154, this.f4 + 4, var4, -171);
            var4 += 12;
            var1.text(var3.font, f155, this.f4 + 4, var4, -43691);
            var4 += 12;
            var1.text(var3.font, f156, this.f4 + 4, var4, -11162881);
            var4 += 12;
         }
      }

      var1.fill(this.f4, var4 - 2, this.f4 + 100, var4 + 10, -1440590558);
      var1.text(var3.font, f157, this.f4 + 4, var4, -11141291);
      var4 += 12;
      var1.fill(this.f4, var4 - 2, this.f4 + 100, var4 + 10, -1440590558);
      var1.text(var3.font, f158, this.f4 + 4, var4, -11141291);
   }

   public ConfigScreen(String var1, int var2, int var3) {
      super(var1, var2, var3, null);
      this.f166 = new HashMap<>();
   }
}
