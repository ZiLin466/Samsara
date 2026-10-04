package com.samsara.protection;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import mixins.MinecraftAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class TokenLoginScreen extends Screen {
   private static final String f129 = "Token Login";
   private static final String f136 = "id";
   private static final String f130 = "Token";
   private static final String f138 = "$1-$2-$3-$4-$5";
   private static final String f132 = "Back";
   private EditBox f139;
   private static final String f134 = "Authorization";
   private static final String f135 = "name";
   private static final String f133 = "https://api.minecraftservices.com/minecraft/profile";
   private static final String f137 = "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})";
   private static final String f131 = "Login";

   protected void init() {
      this.f139 = new EditBox(this.font, this.width / 2 - 100, this.height / 2 - 20, 200, 20, Component.literal(f130));
      this.f139.setMaxLength(32767);
      this.addRenderableWidget(this.f139);
      this.addRenderableWidget(Button.builder(Component.literal(f131), var1 -> this.pm$8()).bounds(this.width / 2 - 50, this.height / 2 + 20, 100, 20).build());
      this.addRenderableWidget(
         Button.builder(Component.literal(f132), var1 -> this.onClose()).bounds(this.width / 2 - 50, this.height / 2 + 50, 100, 20).build()
      );
      this.setInitialFocus(this.f139);
   }

   public TokenLoginScreen() {
      super(Component.literal(f129));
   }

   public void extractRenderState(GuiGraphicsExtractor var1, int var2, int var3, float var4) {
      super.extractRenderState(var1, var2, var3, var4);
      String var5 = "§aLogged in as: " + ((MinecraftAccessor)Minecraft.getInstance()).getUser().getName();
      var1.text(this.font, Component.literal(var5), this.width / 2 - this.font.width(var5) / 2, this.height / 2 - 50, -1);
   }

   public void loginWithToken(String var1) {
      new Thread(() -> {
         try {
            String[] var2 = this.pm$9(var1);
            Minecraft var3 = Minecraft.getInstance();
            UUID var4 = UUID.fromString(var2[1].replaceFirst(f137, f138));
            User var5 = new User(var2[0], var4, var1, Optional.empty(), Optional.empty());
            ((MinecraftAccessor)Minecraft.getInstance()).setUser(var5);
            System.out.println("Logged in as " + var5.getName());
         } catch (Exception var6) {
            var6.printStackTrace();
         }
      }).start();
   }

   private void pm$8() {
      String var1 = this.f139.getValue();
      this.loginWithToken(var1);
   }

   private String[] pm$9(String var1) throws IOException, InterruptedException {
      HttpClient var2 = HttpClient.newHttpClient();
      HttpRequest var3 = HttpRequest.newBuilder().uri(URI.create(f133)).header(f134, "Bearer " + var1).GET().build();
      HttpResponse var4 = var2.send(var3, BodyHandlers.ofString(StandardCharsets.UTF_8));
      JsonObject var5 = JsonParser.parseString((String)var4.body()).getAsJsonObject();
      return new String[]{var5.get(f135).getAsString(), var5.get(f136).getAsString()};
   }
}
