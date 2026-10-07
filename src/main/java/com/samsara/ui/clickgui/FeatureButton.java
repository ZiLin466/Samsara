package com.samsara.ui.clickgui;

import com.samsara.module.Feature;
import com.samsara.setting.BooleanSetting;
import com.samsara.setting.ModeSetting;
import com.samsara.setting.NumberSetting;
import com.samsara.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.lwjgl.glfw.GLFW;

public class FeatureButton {
   private static final String BIND_LABEL = "Bind: ";
   private static final String EMPTY_TEXT = "";
   private static final String PRESS_KEY_LABEL = "Press key...";
   private static final String DISABLED_LABEL = "\u00a7cOFF";
   private static final String ENABLED_LABEL = "\u00a7aON";
   private static final String NONE_LABEL = "NONE";

   private NumberSetting draggedSlider;

   public final Feature feature;

   public boolean expanded;
   public int height = 15;
   public int width;
   public int x;
   public int y;
   public boolean listeningForKey;

   public boolean containsMouse(double mouseX, double mouseY) {
      return mouseX >= (double)this.x && mouseX <= (double)(this.x + this.width) && mouseY >= (double)this.y && mouseY <= (double)(this.y + this.height);
   }

   public void mouseDragged(double mouseX, double mouseY) {
      if (this.draggedSlider != null) {
         this.updateSlider(this.draggedSlider, mouseX);
      }
   }

   public FeatureButton(Feature feature) {
      this.feature = feature;
   }

   public void keyPressed(int keyCode) {
      if (this.listeningForKey) {
         if (keyCode == 256) {
            this.feature.setKey(0);
         } else {
            this.feature.setKey(keyCode);
         }

         this.listeningForKey = false;
      }
   }

   private void updateSlider(NumberSetting numberSetting, double mouseX) {
      double fraction = (mouseX - (double)this.x) / (double)this.width;
      fraction = Math.max(0.0, Math.min(1.0, fraction));
      double value = numberSetting.getMinimum() + fraction * (numberSetting.getMaximum() - numberSetting.getMinimum());
      numberSetting.setValue(value);
   }

   public void mouseReleased() {
      this.draggedSlider = null;
   }

   public void mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0 && this.containsMouse(mouseX, mouseY)) {
         this.feature.toggle();
      } else if (button == 1 && this.containsMouse(mouseX, mouseY)) {
         this.expanded = !this.expanded;
      } else {
         if (this.expanded) {
            int rowOffset = this.height;

            for (Setting setting : this.feature.settings) {
               if (mouseX >= (double)(this.x + 5)
                  && mouseX <= (double)(this.x + this.width)
                  && mouseY >= (double)(this.y + rowOffset)
                  && mouseY <= (double)(this.y + rowOffset + 12)) {
                  if (setting instanceof ModeSetting && button == 0) {
                     ModeSetting modeSetting = (ModeSetting)setting;
                     String[] options = modeSetting.getOptions();
                     int selectedIndex = 0;

                     for (int optionIndex = 0; optionIndex < options.length; optionIndex++) {
                        if (options[optionIndex].equals(modeSetting.getValue())) {
                           selectedIndex = optionIndex;
                           break;
                        }
                     }

                     if (++selectedIndex >= options.length) {
                        selectedIndex = 0;
                     }

                     modeSetting.setValue(options[selectedIndex]);
                  }

                  if (setting instanceof BooleanSetting && button == 0) {
                     BooleanSetting booleanSetting = (BooleanSetting)setting;
                     booleanSetting.setValue(!booleanSetting.getValue());
                  }

                  if (setting instanceof NumberSetting && button == 0) {
                     NumberSetting numberSetting = (NumberSetting)setting;
                     if (mouseX >= (double)this.x
                        && mouseX <= (double)(this.x + this.width)
                        && mouseY >= (double)(this.y + rowOffset)
                        && mouseY <= (double)(this.y + rowOffset + 12)) {
                        this.draggedSlider = numberSetting;
                        this.updateSlider(numberSetting, mouseX);
                     }
                  }
               }

               rowOffset += 12;
            }

            int size = this.y + this.height + this.feature.settings.size() * 12;
            if (mouseX >= (double)this.x && mouseX <= (double)(this.x + this.width) && mouseY >= (double)size && mouseY <= (double)(size + this.height)) {
               if (button == 0) {
                  this.listeningForKey = true;
               } else if (button == 1) {
                  this.feature.setHidden(!this.feature.isHidden());
               }
            }
         }
      }
   }

   public void render(GuiGraphicsExtractor graphics, int textColor) {
      graphics.fill(this.x, this.y, this.x + this.width, this.y + this.height, -2146430960);
      graphics.text(Minecraft.getInstance().font, this.feature.getName(), this.x + 4, this.y + 4, this.feature.isEnabled() ? textColor : -1426063361);
      if (this.expanded) {
         int rowOffset = this.height;
         byte rowHeight = 12;

         for (Setting setting : this.feature.settings) {
            graphics.fill(this.x, this.y + rowOffset, this.x + this.width, this.y + rowOffset + rowHeight, -2145378272);
            String settingText = EMPTY_TEXT;
            if (setting instanceof ModeSetting modeSetting) {
               settingText = modeSetting.getValue();
            } else if (setting instanceof BooleanSetting booleanSetting) {
               settingText = booleanSetting.getValue() ? ENABLED_LABEL : DISABLED_LABEL;
            } else if (setting instanceof NumberSetting numberSetting) {
               double numberSettingValue = (numberSetting.getValue() - numberSetting.getMinimum()) / (numberSetting.getMaximum() - numberSetting.getMinimum());
               int fillWidth = (int)((double)this.width * numberSettingValue);
               graphics.fill(this.x, this.y + rowOffset + rowHeight - 1, this.x + this.width, this.y + rowOffset + rowHeight, 1342177280);
               graphics.fill(this.x, this.y + rowOffset + rowHeight - 1, this.x + fillWidth, this.y + rowOffset + rowHeight, textColor);
               settingText = numberSetting.getValue() + "";
            }

            graphics.pose().pushMatrix();
            graphics.pose().scale(0.8F, 0.8F);
            float textX = (float)(this.x + 4) * 1.25F;
            float textY = ((float)(this.y + rowOffset) + 3.5F) * 1.25F;
            String settingLabel = setting.getName() + ": ";
            graphics.text(Minecraft.getInstance().font, settingLabel, (int)textX, (int)textY, -5592406);
            graphics.text(Minecraft.getInstance().font, settingText, (int)(textX + (float)Minecraft.getInstance().font.width(settingLabel)), (int)textY, textColor);
            graphics.pose().popMatrix();
            rowOffset += rowHeight;
         }

         graphics.fill(this.x, this.y + rowOffset, this.x + this.width, this.y + rowOffset + this.height, -2145378272);
         String bindLabel = BIND_LABEL;
         String keyLabel;
         if (this.listeningForKey) {
            keyLabel = PRESS_KEY_LABEL;
         } else if (this.feature.getKey() == 0) {
            keyLabel = NONE_LABEL;
         } else {
            String keyName = GLFW.glfwGetKeyName(this.feature.getKey(), 0);
            keyLabel = (keyName != null ? keyName.toUpperCase() : this.feature.getKey()) + "";
         }

         if (this.feature.isHidden()) {
            keyLabel = keyLabel + " §c(Hidden)";
         }

         int labelX = this.x + 4;
         graphics.text(Minecraft.getInstance().font, bindLabel, labelX, this.y + rowOffset + 4, -5592406);
         graphics.text(Minecraft.getInstance().font, keyLabel, this.x + 4 + Minecraft.getInstance().font.width(bindLabel), this.y + rowOffset + 4, textColor);
      }
   }

   public int getExpandedHeight() {
      int height = this.height;
      if (this.expanded) {
         height += this.feature.settings.size() * 12;
         height += this.height;
      }

      return height;
   }
}
