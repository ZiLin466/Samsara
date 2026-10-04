package com.samsara.config;

import com.google.gson.JsonObject;
import com.samsara.module.Feature;
import com.samsara.ui.hud.ClickGuiLayouts;
import com.samsara.ui.hud.HudLayouts;
import java.util.List;
import java.util.function.Consumer;

/** Full local state; invalid old values do not prevent independent sections from restoring. */
public final class ClientStateCodec {
   private ClientStateCodec() { }

   public static JsonObject snapshot(List<Feature> modules,HudLayouts hud) {
      var data=new JsonObject();data.addProperty("version",1);
      data.add("modules",ModuleConfigCodec.snapshot(modules,ModuleConfigCodec.Scope.ALL,false));
      data.add("hud",hud.snapshot());data.add("clickGui",ClickGuiLayouts.snapshot());
      var friends = new com.google.gson.JsonArray();
      com.samsara.util.Friends.names().forEach(friends::add); data.add("friends", friends);
      return data;
   }

   public static void restore(List<Feature> modules,HudLayouts hud,JsonObject data,Consumer<String> warning) {
      if (data.has("version") && data.get("version").getAsInt()!=1)
         throw new IllegalArgumentException("Unsupported automatic state version");
      var actions=new java.util.ArrayList<Runnable>();
      if (data.has("friends")) {
         try {
            var friends = new java.util.ArrayList<String>();
            for (var value : data.getAsJsonArray("friends")) {
               if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) throw new IllegalArgumentException("Invalid friend name");
               friends.add(value.getAsString());
            }
            actions.add(() -> com.samsara.util.Friends.restore(friends));
         } catch (RuntimeException error) { warning.accept("friends: " + error.getMessage()); }
      }
      if (data.has("modules")) {
         try { actions.add(ModuleConfigCodec.prepare(modules,data.getAsJsonObject("modules"),ModuleConfigCodec.Scope.ALL,true,warning)); }
         catch (RuntimeException error) { warning.accept("modules: "+error.getMessage()); }
      }
      if (data.has("hud")) {
         try {
            var source=data.getAsJsonObject("hud");
            for (HudLayouts.Element element:HudLayouts.Element.values()) {
               if (!source.has(element.name())) continue;
               try {
                  var row=new JsonObject();row.add(element.name(),source.get(element.name()));
                  new HudLayouts().load(row);
                  actions.add(()->hud.load(row));
               } catch (RuntimeException error) { warning.accept("hud / "+element+": "+error.getMessage()); }
            }
         } catch (RuntimeException error) { warning.accept("hud: "+error.getMessage()); }
      }
      if (data.has("clickGui")) {
         try {
            var gui=ClickGuiLayouts.snapshot();
            for (var entry:data.getAsJsonObject("clickGui").entrySet()) {
               try {
                  var row=new JsonObject();row.add(entry.getKey(),entry.getValue());
                  gui.add(entry.getKey(),ClickGuiLayouts.validate(row).get(entry.getKey()));
               } catch (RuntimeException error) { warning.accept("clickGui / "+entry.getKey()+": "+error.getMessage()); }
            }
            actions.add(()->ClickGuiLayouts.load(gui));
         } catch (RuntimeException error) { warning.accept("clickGui: "+error.getMessage()); }
      }
      actions.forEach(Runnable::run);
   }
}
