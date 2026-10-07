package com.samsara.ui.account;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.minecraft.InsecurePublicKeyException;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import com.mojang.authlib.minecraft.SessionService;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.services.MinecraftServicesDiscoveryService;
import com.mojang.authlib.services.ProfileResult;
import com.mojang.authlib.services.request.JoinMinecraftServerRequest;
import java.net.InetAddress;
import java.net.Proxy;
import java.util.UUID;

/** Switch only the alternate provider's join handshake; official texture validation remains intact. */
final class AlteningSessionService implements SessionService {
   private final SessionService official;
   private final MinecraftClient client;
   AlteningSessionService(SessionService official, Proxy proxy) {
      this.official = official; this.client = MinecraftClient.unauthenticated(proxy);
   }
   @Override public void joinServer(UUID id, String token, String server) throws AuthenticationException {
      try {
         client.post(MinecraftServicesDiscoveryService.constantURL("http://sessionserver.thealtening.com/session/minecraft/join"),
            new JoinMinecraftServerRequest(token, id, server), Void.class);
      } catch (MinecraftClientException error) { throw error.toAuthenticationException(); }
   }
   @Override public ProfileResult hasJoinedServer(String name, String server, InetAddress address) throws AuthenticationUnavailableException {
      return official.hasJoinedServer(name, server, address);
   }
   @Override public Property getPackedTextures(GameProfile profile) { return official.getPackedTextures(profile); }
   @Override public MinecraftProfileTextures unpackTextures(Property property) { return official.unpackTextures(property); }
   @Override public ProfileResult fetchProfile(UUID id, boolean secure) { return official.fetchProfile(id, secure); }
   @Override public String getSecurePropertyValue(Property property) throws InsecurePublicKeyException { return official.getSecurePropertyValue(property); }
}
