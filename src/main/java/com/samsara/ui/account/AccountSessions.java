package com.samsara.ui.account;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.services.MinecraftServicesDiscoveryService;
import com.mojang.authlib.services.ProfileResult;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import mixins.MinecraftSessionAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.server.Services;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.java.JavaAuthManager;
import net.raphimc.minecraftauth.msa.data.MsaConstants;
import net.raphimc.minecraftauth.msa.model.MsaApplicationConfig;
import net.raphimc.minecraftauth.msa.model.MsaCredentials;
import net.raphimc.minecraftauth.msa.service.impl.CredentialsMsaAuthService;
import net.raphimc.minecraftauth.msa.service.impl.DeviceCodeMsaAuthService;
import net.raphimc.minecraftauth.msa.service.impl.ExternalBrowserMsaAuthService;

public final class AccountSessions {
   public enum MicrosoftMethod { WEB_VIEW, DEVICE_CODE, CREDENTIALS }
   public record Request(SavedAccount.Type type, String input, String password, boolean onlineId,
                         boolean generate, MicrosoftMethod method, SavedAccount saved) {
      @Override public String toString() { return "Account request / " + type; }
   }
   public record Progress(String message, String code, String url) { }
   private record Session(User user, UserApiService api, CompletableFuture<ProfileResult> profile,
                          CompletableFuture<UserApiService.UserProperties> properties, ProfileKeyPairManager keys, Services services) { }
   public record Result(SavedAccount account, Session session, String message) {
      public boolean success() { return account != null && session != null; }
      @Override public String toString() { return message; }
   }
   private static final class Operation { volatile boolean cancelled; Thread thread; }
   private static volatile Operation current;
   private static Session launcher;
   private static MinecraftServicesDiscoveryService officialDiscovery;
   private static final AccountAuthClient AUTH = new AccountAuthClient(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
   private AccountSessions() { }
   public static void captureLauncher() {
      if (launcher != null) return;
      var mc = Minecraft.getInstance(); var access = (MinecraftSessionAccessor) mc;
      launcher = new Session(mc.getUser(), access.samsara$getUserApiService(), access.samsara$getProfile(),
         access.samsara$getProperties(), mc.getProfileKeyPairManager(), mc.services());
   }
   public static boolean busy() { return current != null; }
   public static void cancel() {
      var operation = current;
      if (operation != null) { operation.cancelled = true; if (operation.thread != null) operation.thread.interrupt(); }
   }
   public static void restore() { if (busy()) return; captureLauncher(); apply(launcher); }
   public static void offline(String name) {
      captureLauncher();
      try { apply(session(new AccountAuthClient.Identity(AccountStore.validate(name), AccountStore.offlineId(name), "0", new JsonObject()), SavedAccount.Type.CRACKED)); }
      catch (com.mojang.authlib.exceptions.AuthenticationException e) { throw new IllegalStateException("无法切换账号", e); }
   }
   public static void token(String token, Consumer<String> completed) {
      authenticate(new Request(SavedAccount.Type.SESSION, token, "", false, false, MicrosoftMethod.WEB_VIEW, null), p -> {}, result -> {
         if (result.success()) activate(result); completed.accept(result.message());
      });
   }
   public static void activate(Result result) { if (result.success()) apply(result.session()); }
   public static void login(SavedAccount account, Consumer<Progress> progress, Consumer<Result> completed) {
      authenticate(new Request(account.type(), "", "", account.onlineId(), false, MicrosoftMethod.WEB_VIEW, account), progress, completed);
   }
   public static synchronized void authenticate(Request request, Consumer<Progress> progress, Consumer<Result> completed) {
      if (busy()) { completed.accept(new Result(null, null, "登录正在进行")); return; }
      captureLauncher(); var mc = Minecraft.getInstance();
      if (mc.level != null) { completed.accept(new Result(null, null, "请先退出世界再切换账号")); return; }
      var operation = new Operation(); current = operation;
      operation.thread = Thread.ofVirtual().name("samsara-account-login").unstarted(() -> {
         Result result;
         try {
            var identity = resolve(request, p -> mc.execute(() -> { if (!operation.cancelled && current == operation) progress.accept(p); }));
            if (operation.cancelled || Thread.currentThread().isInterrupted()) throw new InterruptedException();
            var saved = request.saved();
            var account = saved == null
               ? new SavedAccount(UUID.randomUUID(), request.type(), identity.name(), identity.id(), false, request.onlineId(), identity.credentials(), List.of())
               : saved.refreshed(identity.name(), identity.id(), identity.credentials());
            result = new Result(account, session(identity, request.type()), "已登录 " + identity.name());
         } catch (InterruptedException e) { Thread.currentThread().interrupt(); result = new Result(null, null, "登录已取消"); }
         catch (Exception | LinkageError e) {
            String message;
            if (e instanceof java.util.concurrent.TimeoutException) message = "登录超时，请重试";
            else if (operation.cancelled || Thread.currentThread().isInterrupted()) message = "登录已取消";
            else if (e instanceof ExternalBrowserMsaAuthService.UserClosedBrowserException) message = "登录窗口已关闭";
            else if (e instanceof MicrosoftWebViewAuthService.WindowInitializationException) {
               message = "无法启动登录窗口，请重试或使用设备代码";
               org.slf4j.LoggerFactory.getLogger("samsara").warn("Microsoft login window initialization failed", e);
            }
            else if (e instanceof java.io.IOException && request.type() != SavedAccount.Type.MICROSOFT) message = "登录失败：" + safeProtocolMessage(e.getMessage());
            else if (e instanceof IllegalArgumentException && request.type() != SavedAccount.Type.MICROSOFT)
               message = request.type() == SavedAccount.Type.SESSION && e.getMessage() != null && e.getMessage().startsWith("令牌")
                  ? e.getMessage() : "登录信息无效，请检查输入";
            else message = "登录失败，请检查网络、账号游戏许可或改用设备代码";
            result = new Result(null, null, message);
         }
         Result ready = result;
         mc.execute(() -> {
            try {
               completed.accept(operation.cancelled ? new Result(null, null, "登录已取消")
                  : mc.level != null ? new Result(null, null, "已进入世界，会话未切换") : ready);
            } finally { if (current == operation) current = null; }
         });
      });
      operation.thread.start();
   }
   private static String safeProtocolMessage(String message) {
      return message != null && (message.startsWith("登录服务") || message.startsWith("令牌") || message.startsWith("找不到账号")
         || message.startsWith("登录请求") || message.startsWith("账号没有")) ? message : "无法连接登录服务";
   }
   private static AccountAuthClient.Identity resolve(Request r, Consumer<Progress> progress) throws Exception {
      var auth = r.saved() == null ? new JsonObject() : r.saved().credentials();
      return switch (r.type()) {
         case CRACKED -> AUTH.cracked(r.saved() == null ? r.input() : r.saved().name(), r.onlineId());
         case SESSION -> AUTH.session(r.saved() == null ? r.input() : auth.get("token").getAsString());
         case THEALTENING -> AUTH.altening(r.saved() == null ? r.input() : auth.get("accountToken").getAsString(), r.saved() == null && r.generate());
         case MICROSOFT -> {
            var client = MinecraftAuth.createHttpClient(com.samsara.ClientBranding.NAME + "/" + com.samsara.ClientBranding.VERSION);
            JavaAuthManager manager;
            if (r.saved() != null) manager = JavaAuthManager.fromJson(client, auth.getAsJsonObject("authManager"));
            else {
               var builder = JavaAuthManager.create(client).msaApplicationConfig(new MsaApplicationConfig(MsaConstants.JAVA_TITLE_ID, MsaConstants.SCOPE_TITLE_AUTH));
               manager = switch (r.method()) {
                  case WEB_VIEW -> builder.login((h,c) -> new MicrosoftWebViewAuthService(h,c,300_000));
                  case DEVICE_CODE -> builder.<Consumer<net.raphimc.minecraftauth.msa.model.MsaDeviceCode>>login((h,c,callback) -> new DeviceCodeMsaAuthService(h,c,callback,300_000),
                     code -> progress.accept(new Progress("在微软页面输入设备代码以继续", code.getUserCode(), code.getDirectVerificationUri())));
                  case CREDENTIALS -> {
                     if(r.password()==null||r.password().isEmpty())throw new IllegalArgumentException("请输入密码");
                     yield builder.login(CredentialsMsaAuthService::new, new MsaCredentials(AccountAuthClient.required(r.input()), r.password()));
                  }
               };
            }
            var token = manager.getMinecraftToken().getUpToDate();
            var profile = manager.getMinecraftProfile().refresh();
            var data = new JsonObject(); data.add("authManager", JavaAuthManager.toJson(manager));
            yield new AccountAuthClient.Identity(profile.getName(), profile.getId(), token.getToken(), data);
         }
      };
   }
   private static Session session(AccountAuthClient.Identity identity, SavedAccount.Type type) throws com.mojang.authlib.exceptions.AuthenticationException {
      var mc = Minecraft.getInstance();
      var user = new User(identity.name(), identity.id(), identity.token(), Optional.empty(), Optional.empty());
      var profile = CompletableFuture.completedFuture(new ProfileResult(new GameProfile(identity.id(), identity.name())));
      var services = launcher.services();
      if (type == SavedAccount.Type.THEALTENING) {
         services = new Services(new AlteningSessionService(services.sessionService(), mc.getProxy()), services.servicesKeySet(),
            services.profileRepository(), services.nameToIdCache(), services.profileResolver());
      }
      if (type == SavedAccount.Type.CRACKED || type == SavedAccount.Type.THEALTENING)
         return new Session(user, UserApiService.OFFLINE, profile, CompletableFuture.completedFuture(UserApiService.OFFLINE_PROPERTIES), ProfileKeyPairManager.EMPTY_KEY_MANAGER, services);
      UserApiService api = discovery().createUserApiService(identity.token());
      return new Session(user, api, profile, CompletableFuture.completedFuture(api.fetchProperties()),
         ProfileKeyPairManager.create(api, user, mc.gameDirectory.toPath()), services);
   }
   private static synchronized MinecraftServicesDiscoveryService discovery() {
      if(officialDiscovery==null) officialDiscovery=MinecraftServicesDiscoveryService.create(Minecraft.getInstance().getProxy());
      return officialDiscovery;
   }
   private static void apply(Session session) {
      var mc = Minecraft.getInstance();
      if (mc.level != null) throw new IllegalStateException("请先退出世界再切换账号");
      var access = (MinecraftSessionAccessor)mc;
      access.samsara$setUser(session.user()); access.samsara$setUserApiService(session.api());
      access.samsara$setProfile(session.profile()); access.samsara$setProperties(session.properties());
      access.samsara$setKeys(session.keys()); access.samsara$setServices(session.services());
      mixins.RealmsSessionAccessor.samsara$reset(null);
      mixins.RealmsAvailabilityAccessor.samsara$reset(null);
   }
}
