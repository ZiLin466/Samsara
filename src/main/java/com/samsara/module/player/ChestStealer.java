package com.samsara.module.player;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.setting.BooleanSetting;
import com.samsara.setting.ModeSetting;
import com.samsara.setting.NumberSetting;
import com.samsara.ui.dynamicIsland.DynamicIslandPainter.Surface;
import com.samsara.ui.dynamicIsland.DynamicIslandState;
import com.samsara.util.ModTextures;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiPredicate;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public class ChestStealer extends Feature {
   private final NumberSetting delay = new NumberSetting("Delay", this, 2, 0, 20, 1);
   private final NumberSetting delayMax = new NumberSetting("Delay Max", this, 2, 0, 20, 1);
   private final NumberSetting openDelay = new NumberSetting("Open Delay", this, 1, 0, 20, 1);
   private final BooleanSetting hypixelChest = new BooleanSetting("Hypixel Chest", this, false);
   private final BooleanSetting nameCheck = new BooleanSetting("Name Check", this, true);
   private final BooleanSetting skipTrash = new BooleanSetting("Skip Trash", this, true);
   private final BooleanSetting quickSwaps = new BooleanSetting("Quick Swaps", this, true);
   private final BooleanSetting autoClose = new BooleanSetting("Auto Close", this, true);
   private final ModeSetting selection = new ModeSetting("Selection Mode", this, "Index", new String[]{"Index", "Distance", "Random"});
   private final ModeSetting onFull = new ModeSetting("On Full", this, "Wait", new String[]{"Wait", "Close", "Drop Trash"});
   private final LootAnimation animation = new LootAnimation();
   private final TransferTracker transfers = new TransferTracker();
   private Object world, player;
   private ChestMenu currentMenu;
   private long lastTick = Long.MIN_VALUE;
   private int elapsedTicks, waitTicks, lastSlot;
   private boolean working;

   public ChestStealer() {
      super("ChestStealer", Category.PLAYER);
      this.delay.setDisplayName("Min Delay");
      this.delayMax.setDisplayName("Max Delay");
   }

   @Override public void onEnable() { onDisable(); }
   @Override
   public void onDisable() {
      this.elapsedTicks = this.waitTicks = this.lastSlot = 0;
      this.world = this.player = this.currentMenu = null;
      this.lastTick = Long.MIN_VALUE;
      this.working = false;
      this.transfers.clear();
      this.animation.clear();
   }

   private ChestMenu workingMenu(Screen screen) {
      if (!isEnabled() || mc.player == null || mc.level == null || mc.gameMode == null
          || mc.player.isSpectator()
          || !(screen instanceof ContainerScreen container)
          || !(mc.player.containerMenu instanceof ChestMenu menu) || container.getMenu() != menu
          || !acceptsTitle(screen.getTitle().getString(), this.hypixelChest.getValue())
          || this.nameCheck.getValue() && !acceptsChestName(screen.getTitle().getString(),
             I18n.get("container.chest"), I18n.get("container.chestDouble"))) return null;
      return menu;
   }

   static boolean acceptsTitle(String title, boolean hypixelOnly) {
      return !hypixelOnly || title.equals("Chest") || title.isEmpty();
   }

   static boolean acceptsChestName(String title, String localChest, String localDoubleChest) {
      return title.isEmpty() || title.equals("Chest") || title.equals("Large Chest")
         || title.equals(localChest) || title.equals(localDoubleChest);
   }

   public boolean replacesContainer(Screen screen) {
      ChestMenu menu = workingMenu(screen);
      if (menu == null || !menu.getCarried().isEmpty()) return false;
      if (this.currentMenu == menu) return this.working;
      return lootPlan(menu).next("Index", 0, slot -> menu.getSlot(slot).mayPickup(mc.player),
         slot -> capacity(menu, menu.getSlot(slot).getItem()) > 0,
         (slot, target) -> canSwap(menu, slot, target), slot -> false) != null;
   }

   @Override
   public void onEvent(Event event) {
      if (event != Events.ROTATION) return;
      ChestMenu menu = workingMenu(mc.gui.screen());
      if (menu == null) {
         onDisable();
         return;
      }
      if (this.currentMenu != menu || this.world != mc.level || this.player != mc.player) {
         onDisable();
         this.currentMenu = menu;
         this.world = mc.level;
         this.player = mc.player;
         this.waitTicks = (int)this.openDelay.getValue();
      }
      if (this.lastTick == mc.player.tickCount) return;
      this.lastTick = mc.player.tickCount;
      if (!menu.getCarried().isEmpty()) { this.working = false; this.elapsedTicks = 0; return; }
      this.animation.attach(menu, menu.getRowCount());
      LootPlan plan = lootPlan(menu);
      this.transfers.refresh(plan.items, plan.chestSize);
      LootAction action = plan.next(this.selection.getValue(), this.lastSlot,
         slot -> menu.getSlot(slot).mayPickup(mc.player),
         slot -> capacity(menu, menu.getSlot(slot).getItem()) > 0,
         (slot, target) -> canSwap(menu, slot, target), this.transfers::rejected);
      boolean full = plan.hasWanted() && !plan.hasRoom(
         slot -> capacity(menu, menu.getSlot(slot).getItem()) > 0, (slot, target) -> canSwap(menu, slot, target));
      boolean closing = full ? this.onFull.is("Close") : this.autoClose.getValue();
      // Preserve the final island frame through the close delay; the vanilla chest must not flash in between.
      this.working = action != null || this.working && closing;
      if (++this.elapsedTicks <= this.waitTicks) return;
      if (action != null) {
         ItemStack before = menu.getSlot(action.slot()).getItem().copy();
         mc.gameMode.handleContainerInput(menu.containerId, action.slot(), action.button(), action.input(), mc.player);
         ItemStack after = menu.getSlot(action.slot()).getItem();
         boolean moved = action.input() == ContainerInput.SWAP
            ? !ItemStack.matches(before, after) : after.getCount() < before.getCount();
         if (moved) {
            this.animation.transferred(action.slot(), before.getCount(),
               action.input() == ContainerInput.SWAP ? 0 : after.getCount(), now());
            this.lastSlot = action.slot();
         } else this.transfers.reject(action.slot());
         this.elapsedTicks = 0;
         this.waitTicks = InventoryManager.randomDelay((int)this.delay.getValue(), (int)this.delayMax.getValue());
         return;
      }
      if (full && this.onFull.is("Drop Trash")) {
         int junk = plan.disposableInventorySlot(index -> !this.transfers.rejected(plan.chestSize + index)
            && menu.getSlot(inventoryMenuSlot(plan.chestSize, index)).mayPickup(mc.player));
         if (junk >= 0) {
            int slot = inventoryMenuSlot(plan.chestSize, junk);
            ItemStack before = menu.getSlot(slot).getItem().copy();
            mc.gameMode.handleContainerInput(menu.containerId, slot, 1, ContainerInput.THROW, mc.player);
            this.working = !ItemStack.matches(before, menu.getSlot(slot).getItem());
            if (!this.working) this.transfers.reject(plan.chestSize + junk);
            this.elapsedTicks = 0;
            this.waitTicks = InventoryManager.randomDelay((int)this.delay.getValue(), (int)this.delayMax.getValue());
            return;
         }
      }
      if (closing) {
         mc.player.closeContainer();
         onDisable();
      }
   }

   private LootPlan lootPlan(ChestMenu menu) {
      int size = menu.getContainer().getContainerSize();
      ItemStack[] chest = new ItemStack[size];
      for (int slot = 0; slot < size; slot++) chest[slot] = menu.getSlot(slot).getItem();
      return new LootPlan(InventoryManager.inventoryItems(), InventoryManager.wornArmor(), chest,
         InventoryManager.sharedRules(), this.skipTrash.getValue(), this.quickSwaps.getValue());
   }

   static int inventoryMenuSlot(int chestSize, int inventoryIndex) {
      return chestSize + (inventoryIndex < 9 ? 27 + inventoryIndex : inventoryIndex - 9);
   }

   private boolean canSwap(ChestMenu menu, int from, int to) {
      var source = menu.getSlot(from);
      var target = menu.getSlot(inventoryMenuSlot(menu.getContainer().getContainerSize(), to));
      return source.mayPickup(mc.player) && target.mayPickup(mc.player)
         && source.mayPlace(target.getItem()) && target.mayPlace(source.getItem());
   }

   static int capacity(ChestMenu menu, ItemStack stack) {
      int capacity = 0, chestSize = menu.getContainer().getContainerSize();
      for (int index = 0; index < 36; index++) {
         var slot = menu.getSlot(inventoryMenuSlot(chestSize, index));
         if (!slot.mayPlace(stack)) continue;
         ItemStack existing = slot.getItem();
         int max = slot.getMaxStackSize(stack);
         if (existing.isEmpty()) capacity += max;
         else if (ItemStack.isSameItemSameComponents(existing, stack)) capacity += Math.max(0, max - existing.getCount());
      }
      return capacity;
   }

   record LootAction(int slot, int button, ContainerInput input) { }

   static final class LootPlan {
      final ItemStack[] items;
      final int chestSize;
      private final InventoryManager.Plan cleanup;
      private final InventoryManager.Plan inventoryCleanup;
      private final boolean skipTrash, quickSwaps;
      LootPlan(ItemStack[] inventory, EnumMap<EquipmentSlot, ItemStack> worn, ItemStack[] chest,
               InventoryManager.Rules rules, boolean skipTrash, boolean quickSwaps) {
         this.chestSize = chest.length;
         this.items = Arrays.copyOf(inventory, 36 + chest.length);
         System.arraycopy(chest, 0, this.items, 36, chest.length);
         this.cleanup = new InventoryManager.Plan(this.items, worn, rules);
         // Full-inventory disposal must never discard the current best item before its replacement is taken.
         this.inventoryCleanup = new InventoryManager.Plan(inventory, worn, rules);
         this.skipTrash = skipTrash;
         this.quickSwaps = quickSwaps;
      }
      boolean wanted(int slot) { return !this.items[36 + slot].isEmpty() && (!this.skipTrash || this.cleanup.keeps(36 + slot)); }
      boolean hasWanted() { for (int slot = 0; slot < this.chestSize; slot++) if (wanted(slot)) return true; return false; }
      boolean hasRoom(IntPredicate canMove, BiPredicate<Integer, Integer> canSwap) {
         for (int slot = 0; slot < this.chestSize; slot++) {
            if (!wanted(slot)) continue;
            int target = this.cleanup.destination(36 + slot);
            if (canMove.test(slot) || this.quickSwaps && target >= 0 && !InventoryManager.Plan.serverItem(this.items[target]) && canSwap.test(slot, target)) return true;
         }
         return false;
      }
      LootAction next(String mode, int lastSlot, Predicate<Integer> mayPickup, IntPredicate canMove,
                      BiPredicate<Integer, Integer> canSwap, IntPredicate rejected) {
         var candidates = new ArrayList<Integer>();
         for (int slot = 0; slot < this.chestSize; slot++) if (wanted(slot) && mayPickup.test(slot) && !rejected.test(slot)) candidates.add(slot);
         Comparator<Integer> order = Comparator.comparingInt(slot -> this.cleanup.priority(36 + slot));
         if (mode.equals("Distance")) order = order.thenComparingInt(slot -> Math.abs(slot % 9 - lastSlot % 9) + Math.abs(slot / 9 - lastSlot / 9));
         if (mode.equals("Random")) {
            // Randomize only the tie order; equipment still takes priority.
            java.util.Collections.shuffle(candidates, ThreadLocalRandom.current());
            candidates.sort(order);
         } else candidates.sort(order.thenComparingInt(slot -> slot));
         for (int slot : candidates) {
            int target = this.cleanup.destination(36 + slot);
            if (this.quickSwaps && target >= 0 && !InventoryManager.Plan.serverItem(this.items[target]) && canSwap.test(slot, target))
               return new LootAction(slot, target, ContainerInput.SWAP);
            if (canMove.test(slot)) return new LootAction(slot, 0, ContainerInput.QUICK_MOVE);
         }
         return null;
      }
      int disposableInventorySlot(IntPredicate mayPickup) {
         for (int index = 0; index < 36; index++) if (this.inventoryCleanup.disposable(index) && mayPickup.test(index)) return index;
         return -1;
      }
   }

   static final class TransferTracker {
      private ItemStack[] snapshot = new ItemStack[0];
      private boolean[] rejected = new boolean[0];
      void refresh(ItemStack[] items, int chestSize) {
         boolean changed = this.snapshot.length != items.length || this.rejected.length != chestSize + 36;
         if (!changed) for (int i = 0; i < items.length; i++) if (!ItemStack.matches(this.snapshot[i], items[i])) { changed = true; break; }
         if (!changed) return;
         this.snapshot = Arrays.stream(items).map(ItemStack::copy).toArray(ItemStack[]::new);
         this.rejected = new boolean[chestSize + 36];
      }
      void reject(int slot) { this.rejected[slot] = true; }
      boolean rejected(int slot) { return slot >= 0 && slot < this.rejected.length && this.rejected[slot]; }
      void clear() { this.snapshot = new ItemStack[0]; this.rejected = new boolean[0]; }
   }

   private static long now() { return System.nanoTime() / 1_000_000L; }

   public IslandView islandView(Screen screen, long now, boolean reducedMotion) {
      if (!replacesContainer(screen)) {
         this.animation.clear();
         return null;
      }
      ChestMenu menu = workingMenu(screen);
      this.animation.attach(menu, menu.getRowCount());
      List<ItemStack> items = new ArrayList<>(menu.getContainer().getContainerSize());
      for (int slot = 0; slot < menu.getContainer().getContainerSize(); slot++) {
         items.add(menu.getSlot(slot).getItem().copy());
      }
      return new IslandView(menu.getRowCount(), List.copyOf(items),
         reducedMotion ? List.of() : this.animation.feedback(now));
   }

   public record IslandView(int rows, List<ItemStack> items, List<Feedback> feedback) {
      public DynamicIslandState.Panel panel() {
         return new DynamicIslandState.Panel(190, rows * 20 + 8, DynamicIslandState.IDLE_TOP, 8.5f);
      }
   }

   public record IslandGeometry(DynamicIslandState.Frame frame, float viewportWidth, float scale) {
      public float left() { return (viewportWidth - frame.width()) / 2; }
      public float contentLeft() { return (viewportWidth - 190) / 2; }
      public static int slotX(int slot) { return 7 + slot % 9 * 20; }
      public static int slotY(int slot) { return 6 + slot / 9 * 20; }

      public static List<ShellSlice> shellSlices(float width, float height) {
         float totalWidth = width + 16, totalHeight = height + 16;
         float edgeX = Math.min(17, totalWidth / 2), edgeY = Math.min(17, totalHeight / 2);
         float[] x = {-8, -8 + edgeX, width + 8 - edgeX, width + 8};
         float[] y = {-8, -8 + edgeY, height + 8 - edgeY, height + 8};
         int[] source = {0, 17, 39, 56};
         List<ShellSlice> slices = new ArrayList<>(9);
         for (int row = 0; row < 3; row++) for (int column = 0; column < 3; column++) {
            float w = x[column + 1] - x[column], h = y[row + 1] - y[row];
            if (w > 0 && h > 0) slices.add(new ShellSlice(x[column], y[row], w, h,
               source[column], source[row], source[column + 1] - source[column], source[row + 1] - source[row]));
         }
         return List.copyOf(slices);
      }
   }

   public record ShellSlice(float x, float y, float width, float height,
                            int sourceX, int sourceY, int sourceWidth, int sourceHeight) { }

   public void extractIslandItems(GuiGraphicsExtractor graphics, IslandView view, IslandGeometry geometry) {
      var frame = geometry.frame();
      if (frame.width() <= 2 || frame.height() <= 2) return;
      float left = geometry.left() * geometry.scale(), top = frame.top();
      graphics.pose().pushMatrix();
      try {
         graphics.pose().translate(left, top);
         graphics.pose().scale(geometry.scale(), geometry.scale());
         extractIslandShell(graphics, frame.width(), frame.height());
      } finally {
         graphics.pose().popMatrix();
      }
      graphics.enableScissor((int)Math.ceil(left + geometry.scale()), (int)Math.ceil(top + geometry.scale()),
         (int)Math.floor(left + (frame.width() - 1) * geometry.scale()),
         (int)Math.floor(top + (frame.height() - 1) * geometry.scale()));
      graphics.pose().pushMatrix();
      try {
         graphics.pose().translate(geometry.contentLeft() * geometry.scale(), top);
         graphics.pose().scale(geometry.scale(), geometry.scale());
         graphics.nextStratum();
         for (int slot = 0; slot < view.items().size(); slot++) {
            ItemStack stack = view.items().get(slot);
            if (stack.isEmpty()) continue;
            int x = IslandGeometry.slotX(slot), y = IslandGeometry.slotY(slot);
            graphics.item(stack, x, y);
            graphics.itemDecorations(mc.font, stack, x, y);
         }
      } finally {
         graphics.pose().popMatrix();
         graphics.disableScissor();
      }
   }

   private static void extractIslandShell(GuiGraphicsExtractor graphics, float width, float height) {
      var texture = ModTextures.register("textures/hud/chest-island.png");
      // Nine slices preserve corner/shadow radii while the spring changes both dimensions.
      for (ShellSlice slice : IslandGeometry.shellSlices(width, height)) {
         graphics.pose().pushMatrix();
         try {
            graphics.pose().translate(slice.x(), slice.y());
            graphics.pose().scale(slice.width() / slice.sourceWidth(), slice.height() / slice.sourceHeight());
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0,
               slice.sourceX() * 4, slice.sourceY() * 4, slice.sourceWidth(), slice.sourceHeight(),
               slice.sourceWidth() * 4, slice.sourceHeight() * 4, 224, 224);
         } finally {
            graphics.pose().popMatrix();
         }
      }
   }

   public static void paintIslandOverlay(Surface surface, IslandView view, IslandGeometry geometry) {
      float x = geometry.contentLeft(), y = geometry.frame().top();
      surface.clip(geometry.left() + 1, y + 1, Math.max(0, geometry.frame().width() - 2),
         Math.max(0, geometry.frame().height() - 2), () -> {
         for (Feedback feedback : view.feedback()) {
            float cx = x + IslandGeometry.slotX(feedback.slot()) + 8;
            float cy = y + IslandGeometry.slotY(feedback.slot()) + 8;
            surface.rounded(cx - feedback.size() / 2, cy - feedback.size() / 2,
               feedback.size(), feedback.size(), feedback.radius(),
               (Math.round(feedback.opacity() * 255) << 24) | 0xDCDCDC);
         }
      });
   }

   public record Feedback(int slot, float size, float radius, float opacity) { }

   public static final class LootAnimation {
      private Object menu;
      private long[] transferredAt = new long[0];

      public void attach(Object menu, int rows) {
         if (this.menu == menu && this.transferredAt.length == rows * 9) return;
         this.menu = menu;
         this.transferredAt = new long[rows * 9];
         java.util.Arrays.fill(this.transferredAt, -1);
      }

      public void transferred(int slot, int before, int after, long now) {
         if (slot >= 0 && slot < this.transferredAt.length && before > after) this.transferredAt[slot] = now;
      }

      public List<Feedback> feedback(long now) {
         List<Feedback> result = new ArrayList<>();
         for (int slot = 0; slot < this.transferredAt.length; slot++) {
            long age = now - this.transferredAt[slot];
            if (this.transferredAt[slot] < 0 || age < 0 || age >= 380) continue;
            float growth = Math.clamp(age / 120f, 0, 1);
            float size = 20 * growth;
            float corners = Math.clamp((growth - .75f) / .25f, 0, 1);
            float radius = size / 2 + (4 - size / 2) * corners;
            float opacity = .34f * (float)Math.exp(-Math.max(0, age - 75) / 80.0);
            result.add(new Feedback(slot, size, radius, opacity));
         }
         return List.copyOf(result);
      }

      public void clear() {
         this.menu = null;
         this.transferredAt = new long[0];
      }
   }
}
