package com.samsara.ui.account;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

final class AccountManagementTest {
   @TempDir java.nio.file.Path temp;
   @Test void migrationRetainsAuthFavoritesOrderingAndBanMetadata() throws Exception {
      var file=temp.resolve("accounts.json");
      String legacy="{\"version\":1,\"offlineAccounts\":[\"Dev\",\"Player_2\"]}";
      Files.writeString(file,legacy); var store=new AccountStore(file);
      assertEquals(legacy,Files.readString(file)); assertEquals(List.of("Dev","Player_2"),store.names());
      var data=new JsonObject(); data.addProperty("token","test-session-secret");
      var online=new SavedAccount(UUID.randomUUID(),SavedAccount.Type.SESSION,"Online",UUID.randomUUID(),true,false,data,
         List.of(new SavedAccount.Ban("test.example","fixture",-1),new SavedAccount.Ban("expired.example","expired",1)));
      store.add(online); store.move(online.key(),store.accounts().getFirst().key());
      var restored=new AccountStore(file); assertEquals(online,restored.accounts().getFirst());
      assertEquals(List.of(online),restored.filtered("ONLINE",true,true,EnumSet.allOf(SavedAccount.Type.class)));
      assertTrue(restored.filtered("",false,false,EnumSet.noneOf(SavedAccount.Type.class)).isEmpty());
      assertEquals(List.of(online),restored.filtered(online.profileId().toString(),false,false,EnumSet.of(SavedAccount.Type.SESSION)));
      assertTrue(online.bans().getFirst().active(System.currentTimeMillis())); assertFalse(online.bans().getLast().active(System.currentTimeMillis()));
      assertFalse(online.toString().contains("secret"));
      var exposed=online.credentials(); exposed.addProperty("token","mutated"); assertEquals("test-session-secret",online.credentials().get("token").getAsString());
      var updated=store.add(new SavedAccount(UUID.randomUUID(),online.type(),"online",online.profileId(),false,false,data,List.of()));
      assertEquals(online.key(),updated.key()); assertTrue(updated.favorite()); assertEquals(3,store.accounts().size());
      store.update(updated.favorite(false)); assertFalse(new AccountStore(file).accounts().getFirst().favorite());
      store.remove(updated.key()); assertEquals(2,new AccountStore(file).accounts().size());
   }
   @Test void failedWritesAndMalformedFilesPreservePreviousData() throws Exception {
      var directory = Files.createDirectory(temp.resolve("storage"));
      var file=directory.resolve("accounts.json"); var store=new AccountStore(file); store.add("Dev");
      var backup = temp.resolve("previous-storage");
      Files.move(directory, backup);
      Files.writeString(directory, "Blocking path");
      assertThrows(java.io.IOException.class,()->store.add("Other")); assertEquals(List.of("Dev"),store.names());
      assertEquals(List.of("Dev"),new AccountStore(backup.resolve("accounts.json")).names());
      Files.delete(directory);
      Files.move(backup, directory);
      assertEquals(List.of("Dev"),new AccountStore(file).names());
      for(String invalid:List.of("not-json","{\"version\":7}","{\"version\":2,\"accounts\":[{}]}")) {
         Files.writeString(file,invalid); assertThrows(java.io.IOException.class,()->new AccountStore(file)); assertEquals(invalid,Files.readString(file));
      }
   }
   @Test void savedTokenRemainsVisibleAfterImportAndRestartDespitePreviousFilters() throws Exception {
      var file=temp.resolve("token-accounts.json"); var store=new AccountStore(file);
      store.add("Offline");
      var filters=new AccountManagerScreen.Filters();
      filters.query="unrelated"; filters.premium=true; filters.favorites=true;
      filters.types.clear(); filters.types.add(SavedAccount.Type.CRACKED);
      var credentials=new JsonObject(); credentials.addProperty("token","session-fixture");
      var account=store.add(new SavedAccount(UUID.randomUUID(),SavedAccount.Type.SESSION,"Verified",UUID.randomUUID(),false,false,credentials,List.of()));
      assertTrue(filters.apply(store).isEmpty());
      filters.reveal(account);
      assertEquals(List.of(account),filters.apply(store)); assertFalse(account.favorite());
      var reopened=new AccountStore(file);
      assertEquals(List.of(account),filters.apply(reopened));
      assertEquals("session-fixture",reopened.accounts().getLast().credentials().get("token").getAsString());
      var updated=store.add(new SavedAccount(UUID.randomUUID(),account.type(),account.name(),account.profileId(),false,false,credentials,List.of()));
      assertEquals(account.key(),updated.key()); assertEquals(2,new AccountStore(file).accounts().size());
   }
   @Test void revealingOfflineAccountDisablesPremiumFilterWithoutChangingFavorites() throws Exception {
      var store=new AccountStore(temp.resolve("offline-accounts.json"));
      var account=store.add(SavedAccount.cracked("Offline",false,AccountStore.offlineId("Offline")).favorite(true));
      var filters=new AccountManagerScreen.Filters(); filters.premium=true; filters.favorites=true;
      filters.types.clear(); filters.types.add(SavedAccount.Type.SESSION);
      assertTrue(filters.apply(store).isEmpty());
      filters.reveal(account);
      assertEquals(List.of(account),filters.apply(store)); assertTrue(filters.favorites);
   }
   @Test void tokenOnlineUuidAndAlteningUseCorrectWireProtocols() throws Exception {
      var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
      var authorization=new AtomicReference<String>(); var loginBody=new AtomicReference<JsonObject>(); String id="123456781234123412341234567890ab";
      server.createContext("/profile",e->{authorization.set(e.getRequestHeaders().getFirst("Authorization")); respond(e,"Bearer invalid".equals(authorization.get())?401:200,"{\"name\":\"Premium\",\"id\":\""+id+"\"}");});
      server.createContext("/lookup/Premium",e->respond(e,200,"{\"id\":\""+id+"\"}"));
      server.createContext("/generate",e->respond(e,200,"{\"token\":\"generated-fixture\"}"));
      server.createContext("/authenticate",e->{
         var body=JsonParser.parseString(new String(e.getRequestBody().readAllBytes(),StandardCharsets.UTF_8)).getAsJsonObject(); loginBody.set(body);
         respond(e,200,"{\"clientToken\":\""+body.get("clientToken").getAsString()+"\",\"accessToken\":\"access-fixture\",\"selectedProfile\":{\"name\":\"Premium\",\"id\":\""+id+"\"}}");
      }); server.start();
      try {
         var base=URI.create("http://127.0.0.1:"+server.getAddress().getPort()+"/");
         var client=new AccountAuthClient(HttpClient.newHttpClient(),base.resolve("profile"),base.resolve("lookup/"),base.resolve("authenticate"),base.resolve("generate"));
         var session=client.session("session-fixture"); assertEquals("Bearer session-fixture",authorization.get());
         assertEquals(AccountAuthClient.uuid(id),session.id()); assertFalse(session.toString().contains("session-fixture"));
         assertThrows(java.io.IOException.class,()->client.session("invalid"));
         assertEquals(session.id(),client.cracked("Premium",true).id()); assertEquals(AccountStore.offlineId("Premium"),client.cracked("Premium",false).id());
         var alt=client.altening("api-fixture",true); assertEquals("generated-fixture",loginBody.get().get("username").getAsString());
         assertEquals("Minecraft",loginBody.get().getAsJsonObject("agent").get("name").getAsString()); assertEquals("access-fixture",alt.token());
         assertEquals("generated-fixture",alt.credentials().get("accountToken").getAsString());
      } finally {server.stop(0);}
   }
   @Test void sessionImportSendsOnlyTheTokenAndUsesTheVerifiedProfile() throws Exception {
      String token="eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJhdXRoZW50aWNhdGlvbiJ9.fixture-signature";
      String details="Token: "+token+"\r\nCookie: __Host-MSAAUTH=cookie-fixture; other=value\r\nMC 用户名: Unverified\r\nMC UUID: invalid\r\n等级: 1\r\n封禁状态: unban";
      var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
      var authorization=new AtomicReference<String>(); var count=new AtomicInteger();
      String id="123456781234123412341234567890ab";
      server.createContext("/profile",e->{
         authorization.set(e.getRequestHeaders().getFirst("Authorization")); count.incrementAndGet();
         respond(e,200,"{\"name\":\"Verified\",\"id\":\""+id+"\"}");
      }); server.start();
      try {
         var base=URI.create("http://127.0.0.1:"+server.getAddress().getPort()+"/");
         var client=new AccountAuthClient(HttpClient.newHttpClient(),base.resolve("profile"),base.resolve("lookup/"),base.resolve("authenticate"),base.resolve("generate"));
         for(String input:List.of(token,"\uFEFF  "+token+"  ","Token: "+token,"Token："+token,
            details,"Token: "+token.replace(".",".\r\n")+details.substring(details.indexOf("\r\nCookie:")),
            "Bearer "+token,"bearer "+token,"{\"access_token\":\""+token+"\",\"cookie\":\"cookie-fixture\"}")) {
            var identity=client.session(input);
            assertEquals("Bearer "+token,authorization.get()); assertEquals(token,identity.token());
            assertEquals("Verified",identity.name()); assertEquals(AccountAuthClient.uuid(id),identity.id());
            assertEquals(1,identity.credentials().size()); assertEquals(token,identity.credentials().get("token").getAsString());
            assertFalse(identity.toString().contains(token));
         }
         assertEquals(9,count.get());
      } finally {server.stop(0);}
   }
   @Test void sessionImportRejectsUnrelatedCredentialsBeforeMakingARequest() throws Exception {
      var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0); var count=new AtomicInteger();
      server.createContext("/profile",e->{count.incrementAndGet();respond(e,200,"{}");}); server.start();
      try {
         var base=URI.create("http://127.0.0.1:"+server.getAddress().getPort()+"/");
         var client=new AccountAuthClient(HttpClient.newHttpClient(),base.resolve("profile"),base.resolve("lookup/"),base.resolve("authenticate"),base.resolve("generate"));
         for(String invalid:List.of("", "Token:", "Cookie: __Host-MSAAUTH=cookie-fixture; other=value", "M.C5.refresh-fixture",
            "{\"cookie\":\"cookie-fixture\"}","{\"access_token\":42}","{\"access_token\":\"\"}","{invalid}",
            "first-fixture\nsecond-fixture","Token: first-fixture\nToken: second-fixture","x".repeat(16385),"Token: "+"x".repeat(65536))) {
            var error=assertThrows(IllegalArgumentException.class,()->client.session(invalid));
            assertTrue(error.getMessage().startsWith("令牌")); assertFalse(error.getMessage().contains("fixture"));
         }
         assertEquals(0,count.get());
      } finally {server.stop(0);}
   }
   private static void respond(com.sun.net.httpserver.HttpExchange e,int status,String body) throws java.io.IOException {
      byte[] bytes=body.getBytes(StandardCharsets.UTF_8);e.sendResponseHeaders(status,bytes.length);e.getResponseBody().write(bytes);e.close();
   }
}
