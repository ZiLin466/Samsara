package com.samsara.ui.config;

import com.samsara.module.Feature;
import com.samsara.ui.clickgui.FeatureButton;
import com.samsara.util.ClientColors;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class CategoryPanel {
   protected final int width = 100;

   private final List<FeatureButton> featureButtons;
   public final String title;

   public int x;
   public boolean expanded;
   public int y;

   public boolean containsPanel(double mouseX, double mouseY) {
      return mouseX >= (double)this.x && mouseX <= (double)(this.x + 100) && mouseY >= (double)this.y && mouseY <= (double)(this.y + 15);
   }

   public void render(GuiGraphicsExtractor graphics) {
      int themeColor = ClientColors.colorAtOffset(0);
      graphics.fill(this.x, this.y, this.x + 100, this.y + 15, themeColor);
      int labelX = this.x + 4;
      int labelY = this.y + 3;
      graphics.text(Minecraft.getInstance().font, this.title, labelX, labelY, -1);
      if (this.expanded) {
         int rowOffset = 15;

         for (FeatureButton featureButton : this.featureButtons) {
            featureButton.x = this.x;
            featureButton.y = this.y + rowOffset;
            featureButton.width = 100;
            featureButton.render(graphics, themeColor);
            rowOffset += featureButton.getExpandedHeight();
         }
      }
   }

   public boolean containsHeader(double mouseX, double mouseY) {
      return mouseX >= (double)this.x && mouseX <= (double)(this.x + 100) && mouseY >= (double)this.y && mouseY <= (double)(this.y + 15);
   }

   public CategoryPanel(String title, int x, int y, List<Feature> features) {
      this.featureButtons = new ArrayList<>();
      this.title = title;
      this.x = x;
      this.y = y;
      if (features != null) {
         for (Feature feature : features) {
            this.featureButtons.add(new FeatureButton(feature));
         }
      }
   }

   public void mouseDragged(double mouseX, double mouseY) {
      if (this.expanded) {
         for (FeatureButton featureButton : this.featureButtons) {
            featureButton.mouseDragged(mouseX, mouseY);
         }
      }
   }

   public void mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 1 && this.containsHeader(mouseX, mouseY)) {
         this.expanded = !this.expanded;
      } else if (this.expanded) {
         for (FeatureButton featureButton : this.featureButtons) {
            featureButton.mouseClicked(mouseX, mouseY, button);
         }
      }
   }

   public void mouseReleased() {
      if (this.expanded) {
         for (FeatureButton featureButton : this.featureButtons) {
            featureButton.mouseReleased();
         }
      }
   }

   public List<FeatureButton> getFeatureButtons() {
      return this.featureButtons;
   }

   public void keyPressed(int keyCode) {
      for (FeatureButton featureButton : this.featureButtons) {
         featureButton.keyPressed(keyCode);
      }
   }
}
