package com.samsara.ui.account;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Versioned atomic storage. Failed reads never rewrite the user's file. */
public final class AccountStore {
   private final Path file;
   private List<SavedAccount> accounts = List.of();
   public AccountStore(Path file) throws IOException {
      this.file = file;
      if (!Files.exists(file)) return;
      try {
         var root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
         var loaded = new ArrayList<SavedAccount>();
         int version = root.get("version").getAsInt();
         if (version == 1) {
            for (var value : root.getAsJsonArray("offlineAccounts")) {
               String name = validate(value.getAsString());
               if (loaded.stream().noneMatch(account -> account.name().equalsIgnoreCase(name)))
                  loaded.add(SavedAccount.cracked(name, false, offlineId(name)));
            }
         } else if (version == 2) {
            for (var value : root.getAsJsonArray("accounts")) {
               var accountState = value.getAsJsonObject();
               var bans = new ArrayList<SavedAccount.Ban>();
               for (var banElement : accountState.getAsJsonArray("bans")) {
                  var ban = banElement.getAsJsonObject();
                  bans.add(new SavedAccount.Ban(ban.get("serverName").getAsString(), ban.get("reason").getAsString(), ban.get("bannedUntil").getAsLong()));
               }
               var account = new SavedAccount(UUID.fromString(accountState.get("key").getAsString()), SavedAccount.Type.valueOf(accountState.get("type").getAsString()),
                  accountState.get("name").getAsString(), UUID.fromString(accountState.get("profileId").getAsString()), accountState.get("favorite").getAsBoolean(),
                  accountState.get("onlineId").getAsBoolean(), accountState.getAsJsonObject("credentials"), bans);
               if (loaded.stream().anyMatch(old -> old.key().equals(account.key()))) throw new IllegalArgumentException("Duplicate key");
               loaded.add(account);
            }
         } else throw new IllegalArgumentException("Unsupported version");
         accounts = List.copyOf(loaded);
      } catch (RuntimeException error) { throw new IOException("账号文件格式无效，原文件已保留", error); }
   }
   public List<SavedAccount> accounts() { return accounts; }
   public List<String> names() { return accounts.stream().filter(account -> account.type() == SavedAccount.Type.CRACKED).map(SavedAccount::name).toList(); }
   public List<SavedAccount> filtered(String search, boolean premium, boolean favorites, Set<SavedAccount.Type> types) {
      String query = search.strip().toLowerCase(Locale.ROOT);
      return accounts.stream().filter(account -> account.name().toLowerCase(Locale.ROOT).contains(query) || account.profileId().toString().contains(query))
         .filter(account -> !premium || account.type().premium).filter(account -> !favorites || account.favorite()).filter(account -> types.contains(account.type())).toList();
   }
   public static String validate(String name) {
      String value = name.trim();
      if (!value.matches("[A-Za-z0-9_]{1,16}")) throw new IllegalArgumentException("用户名需要 1-16 位字母、数字或下划线");
      return value;
   }
   public static UUID offlineId(String name) { return UUID.nameUUIDFromBytes(("OfflinePlayer:" + validate(name)).getBytes(StandardCharsets.UTF_8)); }
   public void add(String name) throws IOException { add(SavedAccount.cracked(validate(name), false, offlineId(name))); }
   public SavedAccount add(SavedAccount account) throws IOException {
      var next = new ArrayList<>(accounts);
      for (int i = 0; i < next.size(); i++) {
         var old = next.get(i);
         if (old.type() == account.type() && (account.type()==SavedAccount.Type.CRACKED
            ? old.name().equalsIgnoreCase(account.name()) : old.profileId().equals(account.profileId()))) {
            account = new SavedAccount(old.key(), account.type(), account.name(), account.profileId(), old.favorite(), account.onlineId(), account.credentials(), old.bans());
            next.set(i, account); commit(next); return account;
         }
      }
      next.add(account); commit(next); return account;
   }
   public void update(SavedAccount account) throws IOException {
      var next = new ArrayList<>(accounts);
      for (int i = 0; i < next.size(); i++) if (next.get(i).key().equals(account.key())) { next.set(i, account); commit(next); return; }
   }
   public void remove(String name) throws IOException {
      for (var account : accounts) if (account.name().equalsIgnoreCase(name)) { remove(account.key()); return; }
   }
   public void remove(UUID key) throws IOException { commit(accounts.stream().filter(account -> !account.key().equals(key)).toList()); }
   /** Move relative to a visible row; hidden accounts retain their relative order. */
   public void move(UUID key, UUID target) throws IOException {
      var next = new ArrayList<>(accounts); int from = index(next, key), to = index(next, target);
      if (from < 0 || to < 0 || from == to) return;
      var moved = next.remove(from); next.add(to, moved); commit(next);
   }
   private static int index(List<SavedAccount> list, UUID key) {
      for (int i = 0; i < list.size(); i++) if (list.get(i).key().equals(key)) return i;
      return -1;
   }
   private void commit(List<SavedAccount> next) throws IOException {
      var root = new JsonObject(); var array = new JsonArray(); root.addProperty("version", 2);
      for (var account : next) {
         var accountState = new JsonObject();
         accountState.addProperty("key", account.key().toString()); accountState.addProperty("type", account.type().name()); accountState.addProperty("name", account.name());
         accountState.addProperty("profileId", account.profileId().toString()); accountState.addProperty("favorite", account.favorite()); accountState.addProperty("onlineId", account.onlineId());
         accountState.add("credentials", account.credentials()); var bans = new JsonArray();
         for (var ban : account.bans()) {
            var banState = new JsonObject(); banState.addProperty("serverName", ban.serverName()); banState.addProperty("reason", ban.reason()); banState.addProperty("bannedUntil", ban.bannedUntil()); bans.add(banState);
         }
         accountState.add("bans", bans); array.add(accountState);
      }
      root.add("accounts", array); Files.createDirectories(file.toAbsolutePath().getParent());
      Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
      Files.writeString(temporary, new GsonBuilder().setPrettyPrinting().create().toJson(root));
      try { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
      catch (java.nio.file.AtomicMoveNotSupportedException error) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
      accounts = List.copyOf(next);
   }
}
