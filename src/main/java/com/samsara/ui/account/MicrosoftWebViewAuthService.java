package com.samsara.ui.account;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import net.lenni0451.commons.httpclient.HttpClient;
import net.raphimc.minecraftauth.msa.model.MsaApplicationConfig;
import net.raphimc.minecraftauth.msa.model.MsaToken;
import net.raphimc.minecraftauth.msa.service.MsaAuthService;
import net.raphimc.minecraftauth.msa.service.impl.ExternalBrowserMsaAuthService;

/** Minecraft sets java.awt.headless=true; a native FX Stage avoids JFrame's HeadlessException. */
final class MicrosoftWebViewAuthService extends MsaAuthService {
   private static final int STARTUP_TIMEOUT_SECONDS = 15;
   private final int timeoutMs;

   MicrosoftWebViewAuthService(HttpClient client, MsaApplicationConfig config, int timeoutMs) {
      super(client, config);
      this.timeoutMs = timeoutMs;
   }

   @Override public MsaToken acquireToken() throws IOException, InterruptedException, TimeoutException {
      var window = new LoginWindow();
      var browser = new ExternalBrowserMsaAuthService(httpClient, applicationConfig, window::open, ignored -> window.close(), timeoutMs);
      // The library invokes its open callback before its own try/finally. Also clean up failed startup.
      try { return browser.acquireToken(); }
      finally { window.close(); }
   }

   static final class WindowInitializationException extends RuntimeException {
      WindowInitializationException(Throwable cause) { super("Microsoft login window could not start", cause); }
   }

   private static final class LoginWindow {
      private final AtomicBoolean closed = new AtomicBoolean();
      private final CompletableFuture<Void> ready = new CompletableFuture<>();
      private volatile Stage stage;
      private WebView webView; // Only accessed on the FX thread.

      void open(ExternalBrowserMsaAuthService browser) {
         final String url;
         try { url = browser.getAuthenticationUrl().toString(); }
         catch (IOException error) { throw new WindowInitializationException(error); }

         Runnable create = () -> {
            if (closed.get()) return;
            try {
               Platform.setImplicitExit(false); // Closing a login must not shut down the reusable toolkit.
               stage = new Stage();
               stage.setTitle("Samsara - Microsoft Login");
               webView = new WebView();
               webView.setContextMenuEnabled(false);
               // Keep WebKit's real browser user agent, rather than the auth HTTP client's product name.
               webView.getEngine().locationProperty().addListener((observable, previous, current) -> {
                  if (browser.handleNavigation(current)) close();
               });
               stage.setScene(new Scene(webView, 800, 600));
               stage.setOnCloseRequest(event -> { browser.cancel(); close(); });
               if (closed.get()) { dispose(); return; }
               stage.show();
               stage.centerOnScreen();
               stage.toFront();
               stage.requestFocus();
               webView.getEngine().load(url);
               ready.complete(null);
            } catch (Exception | LinkageError error) {
               ready.completeExceptionally(error);
               close();
            }
         };

         // Toolkit startup can block while loading natives. Keep that work off Minecraft and bound the wait.
         Thread.ofPlatform().daemon(true).name("samsara-login-window-startup").start(() -> {
            try {
               try { Platform.startup(create); }
               catch (IllegalStateException alreadyStarted) { Platform.runLater(create); }
            } catch (Exception | LinkageError error) { ready.completeExceptionally(error); }
         });
         try { ready.get(STARTUP_TIMEOUT_SECONDS, TimeUnit.SECONDS); }
         catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new WindowInitializationException(error);
         } catch (ExecutionException | TimeoutException error) {
            throw new WindowInitializationException(error instanceof ExecutionException ? error.getCause() : error);
         }
      }

      void close() {
         if (!closed.compareAndSet(false, true)) return;
         if (stage != null) {
            if (Platform.isFxApplicationThread()) dispose();
            else Platform.runLater(this::dispose);
         }
      }

      private void dispose() {
         if (webView != null) {
            webView.getEngine().getLoadWorker().cancel();
            webView.getEngine().load(null);
            webView = null;
         }
         if (stage != null) {
            stage.setOnCloseRequest(null);
            stage.hide();
            stage.setScene(null);
            stage = null;
         }
      }
   }
}
