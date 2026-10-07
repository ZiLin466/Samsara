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
   private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("samsara-auth");
   private static final HttpClient HTTP = HttpClient.newHttpClient();
   private record Profile(String name, String id) { }
   private static final String TOKEN_LOGIN_LABEL = "Token Login";
   private static final String PROFILE_ID_KEY = "id";
   private static final String TOKEN_LABEL = "Token";
   private static final String UUID_GROUP_REPLACEMENT = "$1-$2-$3-$4-$5";
   private static final String BACK_LABEL = "Back";
   private EditBox tokenInput;
   private static final String AUTHORIZATION_HEADER = "Authorization";
   private static final String PROFILE_NAME_KEY = "name";
   private static final String PROFILE_ENDPOINT = "https://api.minecraftservices.com/minecraft/profile";
   private static final String UUID_GROUP_PATTERN = "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})";
   private static final String LOGIN_LABEL = "Login";

   protected void init() {
      this.tokenInput = new EditBox(this.font, this.width / 2 - 100, this.height / 2 - 20, 200, 20, Component.literal(TOKEN_LABEL));
      this.tokenInput.setMaxLength(32767);
      this.addRenderableWidget(this.tokenInput);
      this.addRenderableWidget(Button.builder(Component.literal(LOGIN_LABEL), button -> this.submitToken()).bounds(this.width / 2 - 50, this.height / 2 + 20, 100, 20).build());
      this.addRenderableWidget(
         Button.builder(Component.literal(BACK_LABEL), button -> this.onClose()).bounds(this.width / 2 - 50, this.height / 2 + 50, 100, 20).build()
      );
      this.setInitialFocus(this.tokenInput);
   }

   public TokenLoginScreen() {
      super(Component.literal(TOKEN_LOGIN_LABEL));
   }

   public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      super.extractRenderState(graphics, mouseX, mouseY, partialTick);
      String loginStatus = "§aLogged in as: " + ((MinecraftAccessor)Minecraft.getInstance()).getUser().getName();
      graphics.text(this.font, Component.literal(loginStatus), this.width / 2 - this.font.width(loginStatus) / 2, this.height / 2 - 50, -1);
   }

   public void loginWithToken(String token) {
      new Thread(() -> {
         try {
            Profile profile = this.fetchProfile(token);
            UUID profileId = UUID.fromString(profile.id().replaceFirst(UUID_GROUP_PATTERN, UUID_GROUP_REPLACEMENT));
            User sessionUser = new User(profile.name(), profileId, token, Optional.empty(), Optional.empty());
            ((MinecraftAccessor)Minecraft.getInstance()).setUser(sessionUser);
            LOG.info("Token login completed");
         } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            LOG.debug("Token login interrupted");
         } catch (Exception error) {
            LOG.warn("Token login failed", error);
         }
      }, "Samsara token login").start();
   }

   private void submitToken() {
      this.loginWithToken(this.tokenInput.getValue());
   }

   private Profile fetchProfile(String token) throws IOException, InterruptedException {
      HttpRequest request = HttpRequest.newBuilder().uri(URI.create(PROFILE_ENDPOINT)).header(AUTHORIZATION_HEADER, "Bearer " + token).GET().build();
      HttpResponse<String> response = HTTP.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
      JsonObject profile = JsonParser.parseString(response.body()).getAsJsonObject();
      return new Profile(profile.get(PROFILE_NAME_KEY).getAsString(), profile.get(PROFILE_ID_KEY).getAsString());
   }
}
