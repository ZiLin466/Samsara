package com.samsara.ui.clickgui.opai;

import com.samsara.config.ConfigManager;
import com.samsara.config.ConfigRepository;
import java.io.IOException;
import java.util.List;

/** Adapts shared preset operations to the panel's checked-error contract. */
final class OpaiConfigRepository implements OpaiConfigPanel.Backend {
   private final ConfigRepository presets;

   OpaiConfigRepository() { this(ConfigManager.presets()); }
   OpaiConfigRepository(ConfigRepository presets) { this.presets = presets; }

   @Override public List<String> names() throws IOException { return this.presets.names(); }
   @Override public void create(String name, boolean blank) throws IOException { this.presets.create(name, blank); }
   @Override public void update(String name) throws IOException { this.presets.update(name); }
   @Override public void load(String name) throws IOException { this.presets.load(name); }
   @Override public void delete(String name) throws IOException {
      if (!this.presets.delete(name)) throw new IOException("Configuration no longer exists");
   }
   @Override public void openFolder() throws IOException {
      ConfigManager.openConfigDirectory(this.presets.directory());
   }
}
