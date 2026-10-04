package com.samsara.ui.config;

import com.samsara.module.Feature;
import com.samsara.ui.clickgui.KeybindButton;
import com.samsara.util.ColorUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class ConfigScreenBase {
   private final List<KeybindButton> f141;
   public int f4;
   public boolean f6;
   protected final int width = 100;
   private final int f140 = 15;
   public int f5;
   public final String f3;

   public boolean m101(double var1, double var3) {
      return var1 >= (double)this.f4 && var1 <= (double)(this.f4 + 100) && var3 >= (double)this.f5 && var3 <= (double)(this.f5 + 15);
   }

   public void m100(GuiGraphicsExtractor var1) {
      int var2 = ColorUtil.m27();
      var1.fill(this.f4, this.f5, this.f4 + 100, this.f5 + 15, var2);
      int var10003 = this.f4 + 4;
      int var10004 = this.f5 + 3;
      var1.text(Minecraft.getInstance().font, this.f3, var10003, var10004, -1);
      if (this.f6) {
         int var3 = 15;

         for (KeybindButton var5 : this.f141) {
            var5.f8 = this.f4;
            var5.f9 = this.f5 + var3;
            var5.f10 = 100;
            var5.m110(var1, var2);
            var3 += var5.m113();
         }
      }
   }

   public boolean m102(double var1, double var3) {
      return var1 >= (double)this.f4 && var1 <= (double)(this.f4 + 100) && var3 >= (double)this.f5 && var3 <= (double)(this.f5 + 15);
   }

   public ConfigScreenBase(String var1, int var2, int var3, List<Feature> var4) {
      this.f141 = new ArrayList<>();
      this.f3 = var1;
      this.f4 = var2;
      this.f5 = var3;
      if (var4 != null) {
         for (Feature var6 : var4) {
            this.f141.add(new KeybindButton(var6));
         }
      }
   }

   public void m105(double var1, double var3) {
      if (this.f6) {
         for (KeybindButton var6 : this.f141) {
            var6.m115(var1, var3);
         }
      }
   }

   public void m103(double var1, double var3, int var5) {
      if (var5 == 1 && this.m102(var1, var3)) {
         this.f6 = !this.f6;
      } else if (this.f6) {
         for (KeybindButton var7 : this.f141) {
            var7.m111(var1, var3, var5);
         }
      }
   }

   public void m106() {
      if (this.f6) {
         for (KeybindButton var2 : this.f141) {
            var2.m116();
         }
      }
   }

   public List<KeybindButton> m107() {
      return this.f141;
   }

   public void m104(int var1) {
      for (KeybindButton var3 : this.f141) {
         var3.m114(var1);
      }
   }
}
