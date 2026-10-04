package com.samsara.ui.mainmenu.launch;

import com.samsara.ui.account.AccountStore;
import java.nio.file.Files;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public final class LaunchFlowTest {
   @Test void loadingAndMandatoryPlaybackUseWallTime() {
      var timeline = new LaunchTimeline();
      require(timeline.time(1_000_000_000L, true) == 0, "Loading holds the introduction");
      require(timeline.time(8_000_000_000L, false) == 0, "Loading time is excluded");
      require(timeline.time(10_000_000_000L, false) == 2, "Uses elapsed wall time");
      require(!LaunchTimeline.interactive(timeline.time(11_000_000_000L, false)), "Introduction stays noninteractive");
      require(!LaunchTimeline.interactive(LaunchTimeline.DURATION - .001), "Entrance must finish before interaction");
      require(timeline.time(30_000_000_000L, false) == LaunchTimeline.DURATION, "Elapsed playback reaches menu");
      require(new LaunchTimeline(true).time(1L, false) == LaunchTimeline.DURATION, "Returning to menu does not replay intro");
   }

   @Test void menuActionsRemainReachableAcrossAspectRatios() {
      for (int[] size : new int[][]{{320,240},{854,480},{1024,768},{1920,1080},{2560,1080}}) {
         var layout = LaunchLayout.of(size[0], size[1]);
         for (var tile : LaunchLayout.ACTIONS) {
            double x = layout.left() + (tile.x() + tile.width() / 2) * layout.scale();
            double y = layout.top() + (tile.y() + tile.height() / 2) * layout.scale();
            require(x >= 0 && y >= 0 && x < size[0] && y < size[1], "Every action remains in bounds");
            require(tile.equals(layout.hit(x, y)), "Visible center activates its own action: " + tile.id());
         }
         require(layout.hit(-1, -1) == null, "Outside coordinates do not activate controls");
      }
   }

   @Test void offlineAccountsRoundTripWithoutOverwritingMalformedFiles() throws Exception {
      var path = Files.createTempDirectory("samsara-account-test-").resolve("accounts.json");
      var store = new AccountStore(path);
      store.add("Dev"); store.add("dev"); store.add("Player_2");
      require(store.names().size() == 2, "Account identity is case-insensitive");
      require(new AccountStore(path).names().equals(store.names()), "Saved identities survive restart");
      require(AccountStore.offlineId("Dev").equals(UUID.nameUUIDFromBytes("OfflinePlayer:Dev".getBytes(java.nio.charset.StandardCharsets.UTF_8))), "Vanilla-compatible offline UUID");
      for (String invalid : new String[]{"", "a b", "../escape", "abcdefghijklmnopq"}) {
         boolean rejected = false;
         try { store.add(invalid); } catch (IllegalArgumentException expected) { rejected = true; }
         require(rejected, "Reject invalid identity: " + invalid);
      }
      store.remove("Dev");
      require(new AccountStore(path).names().equals(java.util.List.of("Player_2")), "Removal is persisted");
      Files.writeString(path, "{not valid json");
      boolean rejected = false;
      try { new AccountStore(path); } catch (java.io.IOException expected) { rejected = true; }
      require(rejected && Files.readString(path).equals("{not valid json"), "Malformed files remain untouched");
   }
   private static void require(boolean value, String message) {
      if (!value) throw new AssertionError(message);
   }
}
