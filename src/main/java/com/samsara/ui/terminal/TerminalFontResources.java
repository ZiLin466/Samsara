package com.samsara.ui.terminal;

import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;

/** Core-only Fabric does not expose mod font assets to Minecraft's resource manager. */
public final class TerminalFontResources implements PackResources {
   private static final Map<String, String> FILES=Map.of(
      "font/terminal.json", "font/terminal.json",
      "font/terminal-serif.ttf", "font/terminal-serif.ttf",
      "font/terminal-display.json", "font/terminal-display.json",
      "font/agibot-display.ttf", "font/agibot-display.ttf",
      "font/terminal-google.json", "font/terminal-google.json",
      "font/terminal-google.ttf", "fonts/googlesans-bold.ttf",
      "font/nametags.json", "font/nametags.json",
      "font/nametags.ttf", "fonts/sourcesans3-semibold.ttf");
   @Override public IoSupplier<InputStream> getResource(PackType type,Identifier id) {
      if(type!=PackType.CLIENT_RESOURCES||!id.getNamespace().equals("samsara")||!FILES.containsKey(id.getPath()))return null;
      return () -> {
         var stream=TerminalFontResources.class.getResourceAsStream("/assets/samsara/"+FILES.get(id.getPath()));
         if(stream==null)throw new java.io.FileNotFoundException(id.toString());return stream;
      };
   }
   @Override public void listResources(PackType type,String namespace,String prefix,ResourceOutput output) {
      if(type!=PackType.CLIENT_RESOURCES||!namespace.equals("samsara"))return;
      for(var file:FILES.keySet())if(file.startsWith(prefix+"/")) {
         var id=Identifier.fromNamespaceAndPath(namespace,file);output.accept(id,getResource(type,id));
      }
   }
   @Override public Set<String> getNamespaces(PackType type) {return type==PackType.CLIENT_RESOURCES?Set.of("samsara"):Set.of();}
   @Override public IoSupplier<InputStream> getRootResource(String... path) {return null;}
   @Override public <T> T getMetadataSection(MetadataSectionType<T> type) {return null;}
   @Override public PackLocationInfo location() {return new PackLocationInfo("samsara_fonts",Component.literal("Samsara terminal fonts"),PackSource.BUILT_IN,Optional.empty());}
   @Override public void close() { }
}
