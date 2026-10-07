package com.samsara.ui.account;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.regex.Pattern;

public final class AccountAuthClient {
   private static final Pattern TOKEN_LABEL = Pattern.compile("(?i)^(?:(?:Minecraft\\s+)?(?:Access[_ ]?)?Token|访问令牌|会话令牌)\\s*[:：]\\s*");
   private static final Pattern ACCOUNT_DETAILS = Pattern.compile("(?i)(?:\\R|\\s+)(?:Cookie|MC\\s*(?:用户名|UUID)|等级|封禁状态)\\s*[:：]");
   private static final Pattern BEARER_TOKEN = Pattern.compile("[A-Za-z0-9_.~+/-]+=*");
   public record Identity(String name, UUID id, String token, JsonObject credentials) {
      @Override public String toString() { return name + " / " + id; }
   }
   private final HttpClient http;
   private final URI profile, lookup, altening, generator;
   public AccountAuthClient(HttpClient http) {
      this(http, URI.create("https://api.minecraftservices.com/minecraft/profile"),
         URI.create("https://api.mojang.com/users/profiles/minecraft/"),
         URI.create("http://authserver.thealtening.com/authenticate"), URI.create("https://api.thealtening.com/v2/generate"));
   }
   AccountAuthClient(HttpClient http, URI profile, URI lookup, URI altening, URI generator) {
      this.http = http; this.profile = profile; this.lookup = lookup; this.altening = altening; this.generator = generator;
   }
   public Identity session(String input) throws Exception {
      String token = sessionToken(input);
      var profileResponse = request(HttpRequest.newBuilder(profile).header("Authorization", "Bearer " + token).GET());
      var auth = new JsonObject(); auth.addProperty("token", token);
      return identity(profileResponse, token, auth);
   }
   static String sessionToken(String input) {
      if (input == null || input.isBlank()) throw new IllegalArgumentException("令牌为空，请粘贴 Minecraft 访问令牌");
      if (input.length() > 65536) throw new IllegalArgumentException("令牌资料过长，请只复制 Token 部分");
      String token = input.replace("\uFEFF", "").strip();
      if (token.startsWith("{")) {
         try {
            var tokenDetails = JsonParser.parseString(token).getAsJsonObject();
            token = null;
            for (String key : new String[]{"access_token", "token", "Token"}) {
               var value = tokenDetails.get(key);
               if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                  token = value.getAsString().strip();
                  break;
               }
            }
            if (token == null) throw new IllegalArgumentException();
         } catch (RuntimeException error) {
            throw new IllegalArgumentException("令牌资料中没有有效的 Token 字段");
         }
      }
      var label = TOKEN_LABEL.matcher(token);
      if (label.find()) {
         token = token.substring(label.end());
         var details = ACCOUNT_DETAILS.matcher(token);
         if (details.find()) token = token.substring(0, details.start());
      }
      token = token.strip();
      if (token.regionMatches(true, 0, "Bearer ", 0, 7)) token = token.substring(7).strip();
      if (token.startsWith("eyJ")) token = token.replaceAll("(?U)\\s+", "");
      if (token.length() > 16384 || !BEARER_TOKEN.matcher(token).matches() || token.startsWith("M."))
         throw new IllegalArgumentException("令牌格式不正确，请粘贴 Minecraft 访问令牌或包含 Token 的账号资料");
      return token;
   }
   public Identity cracked(String input, boolean onlineId) throws Exception {
      String name = AccountStore.validate(input);
      UUID id = onlineId ? uuid(request(HttpRequest.newBuilder(lookup.resolve(name)).GET()).get("id").getAsString()) : AccountStore.offlineId(name);
      return new Identity(name, id, "0", new JsonObject());
   }
   public Identity altening(String input, boolean generate) throws Exception {
      String token = required(input);
      if (generate) {
         var generatedAccount = request(HttpRequest.newBuilder(URI.create(generator + "?key=" + URLEncoder.encode(token, StandardCharsets.UTF_8) + "&info=true")).GET());
         token = required(generatedAccount.get("token").getAsString());
      }
      String clientToken = UUID.randomUUID().toString();
      var body = new JsonObject(); var agent = new JsonObject(); agent.addProperty("name", "Minecraft"); agent.addProperty("version", 1);
      body.add("agent", agent); body.addProperty("username", token); body.addProperty("password", "Samsara");
      body.addProperty("clientToken", clientToken); body.addProperty("requestUser", true);
      var loginResponse = request(HttpRequest.newBuilder(altening).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body.toString())));
      if (!loginResponse.has("clientToken") || !clientToken.equals(loginResponse.get("clientToken").getAsString())) throw new IOException("登录服务会话校验失败");
      if (!loginResponse.has("selectedProfile") || loginResponse.get("selectedProfile").isJsonNull()) throw new IOException("账号没有 Minecraft 游戏许可");
      var auth = new JsonObject(); auth.addProperty("accountToken", token);
      return identity(loginResponse.getAsJsonObject("selectedProfile"), required(loginResponse.get("accessToken").getAsString()), auth);
   }
   private Identity identity(JsonObject profile, String token, JsonObject auth) {
      return new Identity(AccountStore.validate(profile.get("name").getAsString()), uuid(profile.get("id").getAsString()), token, auth);
   }
   private JsonObject request(HttpRequest.Builder builder) throws IOException, InterruptedException {
      var response = http.send(builder.timeout(Duration.ofSeconds(25)).build(), HttpResponse.BodyHandlers.ofString());
      int status = response.statusCode();
      if (status == 401 || status == 403) throw new IOException("令牌无效、已过期或账号没有访问权限");
      if (status == 404 || status == 204) throw new IOException("找不到账号，请检查用户名或游戏许可");
      if (status == 429) throw new IOException("登录请求过多，请稍后重试");
      if (status != 200) throw new IOException("登录服务返回 HTTP " + status);
      try { return JsonParser.parseString(response.body()).getAsJsonObject(); }
      catch (RuntimeException error) { throw new IOException("登录服务返回无效数据"); }
   }
   static String required(String input) {
      if (input == null || input.isBlank()) throw new IllegalArgumentException("请填写登录信息");
      return input.trim();
   }
   static UUID uuid(String value) {
      if (value.matches("[0-9a-fA-F]{32}")) value = value.substring(0,8) + "-" + value.substring(8,12) + "-" + value.substring(12,16) + "-" + value.substring(16,20) + "-" + value.substring(20);
      return UUID.fromString(value);
   }
}
