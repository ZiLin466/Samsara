package com.samsara.ui.clickgui;

import com.samsara.module.Feature;
import com.samsara.setting.BooleanSetting;
import com.samsara.setting.ModeSetting;
import com.samsara.setting.NumberSetting;
import com.samsara.setting.Setting;
import java.nio.charset.StandardCharsets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.lwjgl.glfw.GLFW;

public class KeybindButton {
   private static final String f170 = "Bind: ";
   public boolean f12;
   private static final String f167 = new String(new byte[0], StandardCharsets.UTF_8);
   private static final String f171 = "Press key...";
   public int f11 = 15;
   private NumberSetting f173;
   private static final String f169 = "\u00a7cOFF";
   public final Feature f7;
   public int f10;
   private static final String f168 = "\u00a7aON";
   public int f8;
   public int f9;
   private static final String f172 = "NONE";
   public boolean f13;

   public boolean m112(double var1, double var3) {
      return var1 >= (double)this.f8 && var1 <= (double)(this.f8 + this.f10) && var3 >= (double)this.f9 && var3 <= (double)(this.f9 + this.f11);
   }

   public void m115(double var1, double var3) {
      if (this.f173 != null) {
         this.updateSlider(this.f173, var1);
      }
   }

   public KeybindButton(Feature var1) {
      this.f7 = var1;
   }

   public void m114(int var1) {
      if (this.f13) {
         if (var1 == 256) {
            this.f7.setKey(0);
         } else {
            this.f7.setKey(var1);
         }

         this.f13 = false;
      }
   }

   private void updateSlider(NumberSetting var1, double var2) {
      double var4 = (var2 - (double)this.f8) / (double)this.f10;
      var4 = Math.max(0.0, Math.min(1.0, var4));
      double var6 = var1.m218() + var4 * (var1.m219() - var1.m218());
      var1.m223(var6);
   }

   public void m116() {
      this.f173 = null;
   }

   public void m111(double var1, double var3, int var5) {
      if (var5 == 0 && this.m112(var1, var3)) {
         this.f7.toggle();
      } else if (var5 == 1 && this.m112(var1, var3)) {
         this.f12 = !this.f12;
      } else {
         if (this.f12) {
            int var6 = this.f11;

            for (Setting var8 : this.f7.settings) {
               if (var1 >= (double)(this.f8 + 5)
                  && var1 <= (double)(this.f8 + this.f10)
                  && var3 >= (double)(this.f9 + var6)
                  && var3 <= (double)(this.f9 + var6 + 12)) {
                  if (var8 instanceof ModeSetting && var5 == 0) {
                     ModeSetting var9 = (ModeSetting)var8;
                     String[] var10 = var9.m227();
                     int var11 = 0;

                     for (int var12 = 0; var12 < var10.length; var12++) {
                        if (var10[var12].equals(var9.m224())) {
                           var11 = var12;
                           break;
                        }
                     }

                     if (++var11 >= var10.length) {
                        var11 = 0;
                     }

                     var9.m226(var10[var11]);
                  }

                  if (var8 instanceof BooleanSetting && var5 == 0) {
                     BooleanSetting var14 = (BooleanSetting)var8;
                     var14.m217(!var14.m215());
                  }

                  if (var8 instanceof NumberSetting && var5 == 0) {
                     NumberSetting var15 = (NumberSetting)var8;
                     if (var1 >= (double)this.f8
                        && var1 <= (double)(this.f8 + this.f10)
                        && var3 >= (double)(this.f9 + var6)
                        && var3 <= (double)(this.f9 + var6 + 12)) {
                        this.f173 = var15;
                        this.updateSlider(var15, var1);
                     }
                  }
               }

               var6 += 12;
            }

            int var13 = this.f9 + this.f11 + this.f7.settings.size() * 12;
            if (var1 >= (double)this.f8 && var1 <= (double)(this.f8 + this.f10) && var3 >= (double)var13 && var3 <= (double)(var13 + this.f11)) {
               if (var5 == 0) {
                  this.f13 = true;
               } else if (var5 == 1) {
                  this.f7.setHidden(!this.f7.isHidden());
               }
            }
         }
      }
   }

   public void m110(GuiGraphicsExtractor var1, int var2) {
      var1.fill(this.f8, this.f9, this.f8 + this.f10, this.f9 + this.f11, -2146430960);
      var1.text(Minecraft.getInstance().font, this.f7.getName(), this.f8 + 4, this.f9 + 4, this.f7.isEnabled() ? var2 : -1426063361);
      if (this.f12) {
         int var3 = this.f11;
         byte var4 = 12;

         for (Setting var6 : this.f7.settings) {
            var1.fill(this.f8, this.f9 + var3, this.f8 + this.f10, this.f9 + var3 + var4, -2145378272);
            String var7 = f167;
            if (var6 instanceof ModeSetting var8) {
               var7 = var8.m224();
            } else if (var6 instanceof BooleanSetting var15) {
               var7 = var15.m215() ? f168 : f169;
            } else if (var6 instanceof NumberSetting var16) {
               double var9 = (var16.m220() - var16.m218()) / (var16.m219() - var16.m218());
               int var11 = (int)((double)this.f10 * var9);
               var1.fill(this.f8, this.f9 + var3 + var4 - 1, this.f8 + this.f10, this.f9 + var3 + var4, 1342177280);
               var1.fill(this.f8, this.f9 + var3 + var4 - 1, this.f8 + var11, this.f9 + var3 + var4, var2);
               var7 = var16.m220() + "";
            }

            var1.pose().pushMatrix();
            var1.pose().scale(0.8F, 0.8F);
            float var17 = (float)(this.f8 + 4) * 1.25F;
            float var18 = ((float)(this.f9 + var3) + 3.5F) * 1.25F;
            String var10 = var6.getName() + ": ";
            var1.text(Minecraft.getInstance().font, var10, (int)var17, (int)var18, -5592406);
            var1.text(Minecraft.getInstance().font, var7, (int)(var17 + (float)Minecraft.getInstance().font.width(var10)), (int)var18, var2);
            var1.pose().popMatrix();
            var3 += var4;
         }

         var1.fill(this.f8, this.f9 + var3, this.f8 + this.f10, this.f9 + var3 + this.f11, -2145378272);
         String var12 = f170;
         String var13;
         if (this.f13) {
            var13 = f171;
         } else if (this.f7.getKey() == 0) {
            var13 = f172;
         } else {
            String var14 = GLFW.glfwGetKeyName(this.f7.getKey(), 0);
            var13 = (var14 != null ? var14.toUpperCase() : this.f7.getKey()) + "";
         }

         if (this.f7.isHidden()) {
            var13 = var13 + " §c(Hidden)";
         }

         int var10003 = this.f8 + 4;
         var1.text(Minecraft.getInstance().font, var12, var10003, this.f9 + var3 + 4, -5592406);
         var1.text(Minecraft.getInstance().font, var13, this.f8 + 4 + Minecraft.getInstance().font.width(var12), this.f9 + var3 + 4, var2);
      }
   }

   public int m113() {
      int var1 = this.f11;
      if (this.f12) {
         var1 += this.f7.settings.size() * 12;
         var1 += this.f11;
      }

      return var1;
   }
}
