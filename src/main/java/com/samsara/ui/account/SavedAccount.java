package com.samsara.ui.account;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record SavedAccount(UUID key, Type type, String name, UUID profileId, boolean favorite,
                           boolean onlineId, JsonObject credentials, List<Ban> bans) {
   public enum Type {
      MICROSOFT("Microsoft", true), THEALTENING("TheAltening", true), CRACKED("Cracked", false), SESSION("Token", true);
      public final String label;
      public final boolean premium;
      Type(String label, boolean premium) { this.label = label; this.premium = premium; }
   }
   public record Ban(String serverName, String reason, long bannedUntil) {
      public boolean active(long now) { return bannedUntil == -1 || bannedUntil > now; }
   }
   public SavedAccount {
      Objects.requireNonNull(key); Objects.requireNonNull(type); Objects.requireNonNull(profileId);
      name = AccountStore.validate(name);
      credentials = Objects.requireNonNull(credentials).deepCopy(); bans = List.copyOf(bans);
   }
   @Override public JsonObject credentials() { return credentials.deepCopy(); }
   @Override public String toString() { return type.label + " / " + name + " / " + profileId; }
   public SavedAccount favorite(boolean value) { return new SavedAccount(key, type, name, profileId, value, onlineId, credentials, bans); }
   public SavedAccount refreshed(String username, UUID id, JsonObject auth) {
      return new SavedAccount(key, type, username, id, favorite, onlineId, auth, bans);
   }
   public static SavedAccount cracked(String name, boolean onlineId, UUID id) {
      return new SavedAccount(UUID.randomUUID(), Type.CRACKED, name, id, false, onlineId, new JsonObject(), List.of());
   }
}
