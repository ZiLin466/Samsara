package com.samsara.ui.config;

import com.samsara.config.ConfigManager;
import com.samsara.util.ClientColors;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class ConfigPanel extends CategoryPanel {
   private static final int HEADER_HEIGHT = 15;
   private static final int FIRST_ROW_OFFSET = 17;
   private static final int ROW_HEIGHT = 12;
   private static final int ROW_TOP_INSET = 2;
   private static final int ROW_BOTTOM_INSET = 10;
   private static final ConfigAction[] ACTIONS = ConfigAction.values();
   private final Map<String, Boolean> expandedConfigs = new HashMap<>();
   private int nextConfigNumber = 1;

   private enum ConfigAction {
      LOAD("Load", 0xFF55FF55), SAVE("Save", 0xFFFFFF55),
      REMOVE("Remove", 0xFFFF5555), RESET("Reset", 0xFF55AAFF);
      final String label;
      final int color;
      ConfigAction(String label, int color) { this.label = label; this.color = color; }
   }

   @Override
   public void mouseClicked(double mouseX, double mouseY, int button) {
      int rowY = this.y + FIRST_ROW_OFFSET;
      for (String configName : ConfigManager.getConfigNames()) {
         if (this.containsRow(mouseX, mouseY, rowY)) {
            if (button == 1) this.expandedConfigs.put(configName, !this.expandedConfigs.getOrDefault(configName, false));
            else if (button == 0) ConfigManager.loadConfig(configName);
            return;
         }
         rowY += ROW_HEIGHT;
         if (this.expandedConfigs.getOrDefault(configName, false)) {
            for (ConfigAction action : ACTIONS) {
               if (this.containsRow(mouseX, mouseY, rowY)) {
                  if (button == 0) this.performAction(action, configName);
                  return;
               }
               rowY += ROW_HEIGHT;
            }
         }
      }
      if (this.containsRow(mouseX, mouseY, rowY)) {
         if (button == 0) ConfigManager.saveConfig("Config" + this.nextConfigNumber++);
      } else if (this.containsRow(mouseX, mouseY, rowY + ROW_HEIGHT) && button == 0) {
         this.openConfigFolder();
      }
   }

   private boolean containsRow(double mouseX, double mouseY, int rowY) {
      return mouseX >= this.x && mouseX <= this.x + this.width
         && mouseY >= rowY - ROW_TOP_INSET && mouseY <= rowY + ROW_BOTTOM_INSET;
   }

   private void performAction(ConfigAction action, String configName) {
      switch (action) {
         case LOAD -> ConfigManager.loadConfig(configName);
         case SAVE -> ConfigManager.saveConfig(configName);
         case REMOVE -> {
            ConfigManager.deleteConfig(configName);
            this.expandedConfigs.remove(configName);
         }
         case RESET -> ConfigManager.createDefaultConfig(configName);
      }
   }

   private void openConfigFolder() {
      try {
         File directory = ConfigManager.getConfigDir();
         if (!directory.exists()) directory.mkdirs();
         ConfigManager.openConfigDirectory(directory.toPath());
      } catch (IOException error) {
         org.slf4j.LoggerFactory.getLogger("samsara-config").warn("Unable to open configuration directory", error);
      }
   }

   @Override
   public void render(GuiGraphicsExtractor graphics) {
      Minecraft minecraft = Minecraft.getInstance();
      graphics.fill(this.x, this.y, this.x + this.width, this.y + HEADER_HEIGHT, ClientColors.colorAtOffset(0));
      graphics.text(minecraft.font, this.title, this.x + 4, this.y + 3, -1);
      int rowY = this.y + FIRST_ROW_OFFSET;
      for (String configName : ConfigManager.getConfigNames()) {
         graphics.fill(this.x, rowY - ROW_TOP_INSET, this.x + this.width, rowY + ROW_BOTTOM_INSET, -2146430960);
         graphics.text(minecraft.font, configName, this.x + 4, rowY, -1426063361);
         rowY += ROW_HEIGHT;
         if (this.expandedConfigs.getOrDefault(configName, false)) {
            graphics.fill(this.x, rowY - ROW_TOP_INSET, this.x + this.width, rowY + ROW_HEIGHT * ACTIONS.length - ROW_TOP_INSET, -2145378272);
            for (ConfigAction action : ACTIONS) {
               graphics.text(minecraft.font, action.label, this.x + 4, rowY, action.color);
               rowY += ROW_HEIGHT;
            }
         }
      }
      graphics.fill(this.x, rowY - ROW_TOP_INSET, this.x + this.width, rowY + ROW_BOTTOM_INSET, -1440590558);
      graphics.text(minecraft.font, "Create Config", this.x + 4, rowY, -11141291);
      rowY += ROW_HEIGHT;
      graphics.fill(this.x, rowY - ROW_TOP_INSET, this.x + this.width, rowY + ROW_BOTTOM_INSET, -1440590558);
      graphics.text(minecraft.font, "Open Folder", this.x + 4, rowY, -11141291);
   }

   public ConfigPanel(String title, int x, int y) {
      super(title, x, y, null);
   }
}
