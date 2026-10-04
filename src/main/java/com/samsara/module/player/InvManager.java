package com.samsara.module.player;

import com.samsara.event.Event;
import com.samsara.event.Events;
import com.samsara.module.Category;
import com.samsara.module.Feature;
import com.samsara.module.FeatureManager;
import com.samsara.setting.BooleanSetting;
import com.samsara.setting.NumberSetting;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PlayerHeadItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.core.BlockPos;

public class InvManager extends Feature {
   private final NumberSetting delay = new NumberSetting("Delay", this, 2, 0, 20, 1);
   private final NumberSetting delayMax = new NumberSetting("Delay Max", this, 2, 0, 20, 1);
   private final NumberSetting openDelay = new NumberSetting("Open Delay", this, 1, 0, 20, 1);
   private final BooleanSetting autoArmor = new BooleanSetting("Auto Armor", this, true);
   private final BooleanSetting checkDurability = new BooleanSetting("Check Durability", this, true);
   private final NumberSetting swordSlot = slot("Sword Slot", 1);
   private final NumberSetting blockSlot = slot("Block Slot", 2);
   private final NumberSetting gappleSlot = slot("Gapple Slot", 3);
   private final NumberSetting pickaxeSlot = slot("Pickaxe Slot", 4);
   private final NumberSetting shovelSlot = slot("Shovel Slot", 5);
   private final NumberSetting axeSlot = slot("Axe Slot", 6);
   private final NumberSetting projectileSlot = slot("Projectile Slot", 7);
   private final NumberSetting bowSlot = slot("Bow Slot", 8);
   private final NumberSetting foodSlot = slot("Food Slot", 9);
   private final BooleanSetting dropTools = new BooleanSetting("Drop Tools", this, false);
   private final BooleanSetting dropFood = new BooleanSetting("Drop Food", this, false);
   private final BooleanSetting dropItems = new BooleanSetting("Drop Items", this, true);
   private final NumberSetting maxBlocks = new NumberSetting("Max Blocks", this, 128, 0, 2304, 1);
   private final NumberSetting maxProjectiles = new NumberSetting("Max Projectiles", this, 64, 0, 2304, 1);
   private final NumberSetting maxArrows = new NumberSetting("Max Arrows", this, 256, 0, 2304, 1);
   private final NumberSetting maxFood = new NumberSetting("Max Food", this, 64, 0, 2304, 1);
   private final BooleanSetting hypixel = new BooleanSetting("Hypixel", this, false);
   private Object world, player, menu;
   private long lastTick = Long.MIN_VALUE;
   private int elapsed, waitTicks;
   private MergeSequence merging;

   public InvManager() {
      super("InvManager", Category.PLAYER);
      this.delay.setDisplayName("Min Delay");
      this.delayMax.setDisplayName("Max Delay");
      this.pickaxeSlot.setVisible(() -> !this.dropTools.m215());
      this.shovelSlot.setVisible(() -> !this.dropTools.m215());
      this.axeSlot.setVisible(() -> !this.dropTools.m215());
      this.foodSlot.setVisible(() -> !this.dropFood.m215());
      this.maxFood.setVisible(() -> !this.dropFood.m215());
   }

   private NumberSetting slot(String name, int value) { return new NumberSetting(name, this, value, 0, 9, 1); }
   @Override public void onEnable() { reset(); }
   @Override public void onDisable() { reset(); }
   private void reset() {
      this.world = this.player = this.menu = null;
      this.lastTick = Long.MIN_VALUE;
      this.elapsed = this.waitTicks = 0;
      this.merging = null;
   }

   @Override public void onEvent(Event event) {
      if (event != Events.f3) return;
      if (mc.player == null || mc.level == null || mc.gameMode == null || mc.player.isSpectator()
          || !(mc.gui.screen() instanceof InventoryScreen) || mc.player.containerMenu != mc.player.inventoryMenu) {
         reset();
         return;
      }
      var inventoryMenu = mc.player.inventoryMenu;
      if (this.world != mc.level || this.player != mc.player || this.menu != inventoryMenu) {
         reset();
         this.world = mc.level;
         this.player = mc.player;
         this.menu = inventoryMenu;
         this.waitTicks = (int)this.openDelay.m220();
      }
      if (this.lastTick == mc.player.tickCount) return;
      this.lastTick = mc.player.tickCount;
      // Complete an owned cursor operation even if movement starts between its clicks.
      if (this.merging == null && (isMoving() || !inventoryMenu.getCarried().isEmpty())) {
         this.elapsed = 0;
         return;
      }
      if (++this.elapsed <= this.waitTicks) return;
      if (this.merging != null) {
         Action action = this.merging.next(inventoryMenu.getCarried(),
            inventoryMenu.getSlot(this.merging.from).getItem(), inventoryMenu.getSlot(this.merging.to).getItem());
         if (action == null) this.merging = null;
         else { dispatch(action); return; }
         if (isMoving() || !inventoryMenu.getCarried().isEmpty()) return;
      }
      ItemStack[] items = inventoryItems();
      Plan plan = new Plan(items, wornArmor(), rules());
      Predicate<Integer> pickup = index -> inventoryMenu.getSlot(index).mayPickup(mc.player);
      Action action = plan.next(pickup);
      Merge merge = plan.merge(pickup);
      if (merge != null && (action == null || action.input() == ContainerInput.THROW)
          && inventoryMenu.getSlot(menuSlot(merge.to())).mayPlace(items[merge.from()])) {
         this.merging = new MergeSequence(menuSlot(merge.from()), menuSlot(merge.to()), items[merge.from()], items[merge.to()]);
         action = this.merging.next(inventoryMenu.getCarried(), items[merge.from()], items[merge.to()]);
      }
      if (action != null) dispatch(action);
   }

   private void dispatch(Action action) {
      var activeMenu = mc.player.inventoryMenu;
      var source = activeMenu.getSlot(action.slot());
      if (!source.mayPickup(mc.player)) { this.merging = null; return; }
      if (action.input() == ContainerInput.SWAP) {
         var target = activeMenu.getSlot(menuSlot(action.button()));
         if (!target.mayPickup(mc.player) || !target.mayPlace(source.getItem()) || !source.mayPlace(target.getItem())) return;
      }
      mc.gameMode.handleContainerInput(activeMenu.containerId, action.slot(), action.button(), action.input(), mc.player);
      this.elapsed = 0;
      this.waitTicks = randomDelay((int)this.delay.m220(), (int)this.delayMax.m220());
   }

   private boolean isMoving() {
      return mc.player.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6
         || mc.player.input != null && mc.player.input.getMoveVector().lengthSquared() > 0
         || mc.options.keyUp.isDown() || mc.options.keyDown.isDown()
         || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown();
   }

   Rules rules() {
      var slots = new EnumMap<Kind, Integer>(Kind.class);
      slots.put(Kind.SWORD, (int)this.swordSlot.m220() - 1);
      slots.put(Kind.BLOCK, (int)this.blockSlot.m220() - 1);
      slots.put(Kind.GAPPLE, (int)this.gappleSlot.m220() - 1);
      slots.put(Kind.PICKAXE, (int)this.pickaxeSlot.m220() - 1);
      slots.put(Kind.SHOVEL, (int)this.shovelSlot.m220() - 1);
      slots.put(Kind.AXE, (int)this.axeSlot.m220() - 1);
      slots.put(Kind.PROJECTILE, (int)this.projectileSlot.m220() - 1);
      slots.put(Kind.BOW, (int)this.bowSlot.m220() - 1);
      slots.put(Kind.FOOD, (int)this.foodSlot.m220() - 1);
      return new Rules(this.hypixel.m215(), this.dropItems.m215(), this.dropTools.m215(), this.dropFood.m215(),
         this.autoArmor.m215(), this.checkDurability.m215(), slots,
         (int)this.maxBlocks.m220(), (int)this.maxProjectiles.m220(), (int)this.maxArrows.m220(), (int)this.maxFood.m220());
   }

   static Rules sharedRules() {
      var modules = FeatureManager.getModules();
      if (modules != null) for (Feature feature : modules) if (feature instanceof InvManager manager) return manager.rules();
      return Rules.defaults();
   }

   static ItemStack[] inventoryItems() {
      var items = new ItemStack[36];
      for (int index = 0; index < items.length; index++) items[index] = mc.player.getInventory().getItem(index);
      return items;
   }

   static EnumMap<EquipmentSlot, ItemStack> wornArmor() {
      var worn = new EnumMap<EquipmentSlot, ItemStack>(EquipmentSlot.class);
      for (EquipmentSlot slot : Plan.ARMOR) worn.put(slot, mc.player.getItemBySlot(slot));
      return worn;
   }

   static int randomDelay(int min, int max) {
      return ThreadLocalRandom.current().nextInt(Math.min(min, max), Math.max(min, max) + 1);
   }
   static int menuSlot(int inventoryIndex) { return inventoryIndex < 9 ? inventoryIndex + 36 : inventoryIndex; }
   record Action(int slot, int button, ContainerInput input) { }
   record Merge(int from, int to) { }

   enum Kind { SWORD, PICKAXE, SHOVEL, AXE, BLOCK, PROJECTILE, ROD, GAPPLE, BOW, FOOD,
      HOE, CROSSBOW, ARROW, POTION, UTILITY, TRASH }

   record Rules(boolean hypixel, boolean dropItems, boolean dropTools, boolean dropFood, boolean autoArmor,
                boolean checkDurability, Map<Kind, Integer> slots, int maxBlocks, int maxProjectiles, int maxArrows, int maxFood) {
      Rules { slots = Map.copyOf(slots); }
      int slot(Kind kind) { return slots.getOrDefault(kind, -1); }
      static Rules defaults() {
         return new Rules(false, true, false, false, true, true,
            Map.of(Kind.SWORD, 0, Kind.BLOCK, 1, Kind.GAPPLE, 2, Kind.PICKAXE, 3, Kind.SHOVEL, 4,
               Kind.AXE, 5, Kind.PROJECTILE, 6, Kind.BOW, 7, Kind.FOOD, 8), 128, 64, 256, 64);
      }
      static Rules legacy(boolean hypixel, boolean dropItems, boolean dropTools, int sword, int block, int gapple) {
         return new Rules(hypixel, dropItems, dropTools, false, true, false,
            Map.of(Kind.SWORD, sword, Kind.BLOCK, block, Kind.GAPPLE, gapple), 2304, 2304, 2304, 2304);
      }
   }

   static final class MergeSequence {
      final int from, to;
      private final ItemStack source, target, remainder, combined;
      private int phase;
      MergeSequence(int from, int to, ItemStack source, ItemStack target) {
         this.from = from; this.to = to;
         this.source = source.copy(); this.target = target.copy();
         int moved = Math.min(source.getCount(), target.getMaxStackSize() - target.getCount());
         this.remainder = source.copyWithCount(source.getCount() - moved);
         this.combined = target.copyWithCount(target.getCount() + moved);
      }
      Action next(ItemStack carried, ItemStack fromStack, ItemStack toStack) {
         boolean valid = switch (this.phase) {
            case 0 -> carried.isEmpty() && ItemStack.matches(fromStack, this.source) && ItemStack.matches(toStack, this.target);
            case 1 -> ItemStack.matches(carried, this.source) && fromStack.isEmpty() && ItemStack.matches(toStack, this.target);
            case 2 -> ItemStack.matches(carried, this.remainder) && fromStack.isEmpty() && ItemStack.matches(toStack, this.combined);
            default -> false;
         };
         if (!valid || this.phase == 2 && carried.isEmpty()) return null;
         return new Action(this.phase++ == 1 ? this.to : this.from, 0, ContainerInput.PICKUP);
      }
   }

   /** Inventory indices 0..35, followed by optional chest slots. Every click rebuilds this plan. */
   static final class Plan {
      static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
      private final ItemStack[] items;
      private final EnumMap<EquipmentSlot, ItemStack> worn;
      private final EnumMap<EquipmentSlot, Integer> bestArmor = new EnumMap<>(EquipmentSlot.class);
      private final EnumMap<Kind, Integer> best = new EnumMap<>(Kind.class);
      private final EnumMap<Kind, Integer> destinations = new EnumMap<>(Kind.class);
      private final boolean[] keep;
      private final Rules rules;

      Plan(ItemStack[] items, EnumMap<EquipmentSlot, ItemStack> worn, boolean hypixel,
           boolean dropItems, boolean dropTools, int swordSlot, int blockSlot, int gappleSlot) {
         this(items, worn, Rules.legacy(hypixel, dropItems, dropTools, swordSlot, blockSlot, gappleSlot));
      }

      Plan(ItemStack[] items, EnumMap<EquipmentSlot, ItemStack> worn, Rules rules) {
         this.items = items.clone();
         this.worn = new EnumMap<>(worn);
         this.rules = rules;
         this.keep = new boolean[items.length];
         for (int index = 0; index < items.length; index++) if (serverItem(items[index])) this.keep[index] = true;
         for (EquipmentSlot slot : ARMOR) {
            ItemStack equipped = worn.getOrDefault(slot, ItemStack.EMPTY);
            Comparator<ItemStack> comparator = armorComparator(slot);
            int candidate = best(s -> armorSlot(s) == slot, comparator, -1);
            if (candidate >= 0 && !equipped.isEmpty() && usable(equipped) && comparator.compare(items[candidate], equipped) <= 0) candidate = -1;
            this.bestArmor.put(slot, candidate);
            retain(candidate);
         }
         boolean[] claimed = new boolean[9];
         for (Kind kind : Kind.values()) {
            if (kind == Kind.TRASH) continue;
            List<Integer> candidates = candidates(kind);
            int target = rules.slot(kind);
            if (kind == Kind.ROD && target < 0 && this.best.getOrDefault(Kind.PROJECTILE, -1) < 0) target = rules.slot(Kind.PROJECTILE);
            if (target < 0 || target > 8 || claimed[target]) target = -1;
            final int preferred = target;
            candidates.sort((a, b) -> {
               if (a.equals(b)) return 0;
               int quality = comparator(kind).compare(items[b], items[a]);
               if (quality != 0) return quality;
               if (a == preferred) return -1;
               if (b == preferred) return 1;
               return Integer.compare(a, b);
            });
            int count = 0, selected = -1;
            for (int index : candidates) {
               int amount = unique(kind) ? 1 : items[index].getCount();
               if (count + amount > limit(kind)) continue;
               this.keep[index] = true;
               count += amount;
               if (selected < 0) selected = index;
            }
            this.best.put(kind, selected);
            this.destinations.put(kind, target);
            if (selected >= 0 && target >= 0) claimed[target] = true;
         }
      }

      boolean keeps(int index) { return index >= 0 && index < this.keep.length && this.keep[index]; }
      int destination(int index) {
         if (!keeps(index) || armorSlot(this.items[index]) != null || serverItem(this.items[index])) return -1;
         Kind kind = kind(this.items[index]);
         return this.best.getOrDefault(kind, -1) == index ? this.destinations.getOrDefault(kind, -1) : -1;
      }
      int priority(int index) {
         return armorSlot(this.items[index]) != null ? 0 : switch (kind(this.items[index])) {
            case SWORD, PICKAXE, SHOVEL, AXE, BOW, CROSSBOW -> 1;
            case GAPPLE, POTION, UTILITY -> 2;
            default -> 3;
         };
      }
      Action next(Predicate<Integer> mayPickup) { return next(mayPickup, true); }
      Action next(Predicate<Integer> mayPickup, boolean inventoryOpen) {
         if (!inventoryOpen) return null;
         if (this.rules.autoArmor()) for (EquipmentSlot slot : ARMOR) {
            int candidate = this.bestArmor.get(slot);
            if (candidate < 0 || candidate >= 36 || !mayPickup.test(menuSlot(candidate))) continue;
            ItemStack equipped = this.worn.getOrDefault(slot, ItemStack.EMPTY);
            int armorMenuSlot = armorMenuSlot(slot);
            if (!mayPickup.test(armorMenuSlot) || equipped.is(Items.ELYTRA) || serverItem(equipped)) continue;
            if (equipped.isEmpty()) return click(candidate, 0, ContainerInput.QUICK_MOVE);
            if (candidate < 9) return new Action(armorMenuSlot, candidate, ContainerInput.SWAP);
            if (hasSpace()) return new Action(armorMenuSlot, 0, ContainerInput.QUICK_MOVE);
            if (this.rules.dropItems()) return new Action(armorMenuSlot, 1, ContainerInput.THROW);
         }
         for (Kind kind : Kind.values()) {
            int from = this.best.getOrDefault(kind, -1), to = this.destinations.getOrDefault(kind, -1);
            if (from < 0 || from >= 36 || to < 0 || from == to || serverItem(this.items[to])
                || !mayPickup.test(menuSlot(from)) || !mayPickup.test(menuSlot(to))) continue;
            return click(from, to, ContainerInput.SWAP);
         }
         for (int index = 0; index < 36; index++) {
            if (disposable(index) && mayPickup.test(menuSlot(index))) return click(index, 1, ContainerInput.THROW);
         }
         return null;
      }
      boolean disposable(int index) {
         if (index < 0 || index >= 36 || this.items[index].isEmpty() || serverItem(this.items[index])) return false;
         return this.rules.dropTools() && isTool(this.items[index])
            || this.rules.dropFood() && ordinaryFood(this.items[index])
            || this.rules.dropItems() && !keeps(index);
      }
      Merge merge(Predicate<Integer> mayPickup) {
         var targets = new ArrayList<Integer>();
         for (int index = 0; index < 36; index++) {
            ItemStack stack = this.items[index];
            if (keeps(index) && !serverItem(stack) && stack.isStackable() && stack.getCount() < stack.getMaxStackSize()
                && mayPickup.test(menuSlot(index))) targets.add(index);
         }
         targets.sort(Comparator.<Integer, Boolean>comparing(i -> destination(i) == i).reversed()
            .thenComparing(Comparator.<Integer>comparingInt(i -> this.items[i].getCount()).reversed()).thenComparingInt(i -> i));
         for (int target : targets) for (int source : targets) {
            if (source != target && ItemStack.isSameItemSameComponents(this.items[source], this.items[target])) return new Merge(source, target);
         }
         return null;
      }
      private boolean hasSpace() { for (int i = 0; i < 36; i++) if (this.items[i].isEmpty()) return true; return false; }
      private void retain(int index) { if (index >= 0) this.keep[index] = true; }
      private List<Integer> candidates(Kind kind) {
         var result = new ArrayList<Integer>();
         if (this.rules.dropTools() && (kind == Kind.PICKAXE || kind == Kind.SHOVEL || kind == Kind.AXE || kind == Kind.HOE)
             || this.rules.dropFood() && kind == Kind.FOOD) return result;
         for (int index = 0; index < this.items.length; index++) {
            ItemStack stack = this.items[index];
            if (!stack.isEmpty() && !serverItem(stack) && armorSlot(stack) == null
                && (!unique(kind) || usable(stack)) && kind(stack) == kind) result.add(index);
         }
         // Keep the last owned weapon/tool, but never collect an almost-broken replacement from a chest.
         if (result.isEmpty() && unique(kind)) for (int index = 0; index < 36; index++) {
            ItemStack stack = this.items[index];
            if (!stack.isEmpty() && !serverItem(stack) && kind(stack) == kind) result.add(index);
         }
         return result;
      }
      private boolean usable(ItemStack stack) {
         return !this.rules.checkDurability() || !stack.isDamageableItem() || durability(stack) >= 30;
      }
      private int best(Predicate<ItemStack> filter, Comparator<ItemStack> comparator, int preferred) {
         int best = -1;
         for (int index = 0; index < this.items.length; index++) {
            ItemStack stack = this.items[index];
            if (stack.isEmpty() || !filter.test(stack) || serverItem(stack) || !usable(stack)) continue;
            if (best < 0 || comparator.compare(stack, this.items[best]) > 0
                || comparator.compare(stack, this.items[best]) == 0 && index == preferred) best = index;
         }
         return best;
      }
      private static boolean unique(Kind kind) {
         return switch (kind) { case SWORD, PICKAXE, SHOVEL, AXE, HOE, BOW, CROSSBOW, ROD -> true; default -> false; };
      }
      private int limit(Kind kind) {
         if (unique(kind)) return 1;
         return switch (kind) {
            case BLOCK -> this.rules.maxBlocks();
            case PROJECTILE -> this.rules.maxProjectiles();
            case ARROW -> this.rules.maxArrows();
            case FOOD -> this.rules.maxFood();
            default -> Integer.MAX_VALUE;
         };
      }
      private Comparator<ItemStack> comparator(Kind kind) {
         Comparator<ItemStack> quality = switch (kind) {
            case SWORD -> Comparator.comparingDouble(s -> swordDamage(s, this.rules.hypixel()));
            case PICKAXE, SHOVEL, AXE, HOE -> Comparator.comparingDouble(Plan::toolQuality);
            case BOW -> Comparator.comparingDouble(s -> enchantment(s, Enchantments.POWER) * 5
               + enchantment(s, Enchantments.FLAME) + enchantment(s, Enchantments.INFINITY) * .1);
            case CROSSBOW -> Comparator.comparingInt(s -> enchantment(s, Enchantments.QUICK_CHARGE) * 5 + enchantment(s, Enchantments.MULTISHOT));
            case ROD -> Comparator.comparingInt(s -> enchantment(s, Enchantments.UNBREAKING));
            case FOOD -> Comparator.<ItemStack>comparingDouble(s -> s.get(DataComponents.FOOD).saturation()
               / Math.max(1, s.get(DataComponents.FOOD).nutrition())).thenComparingInt(s -> s.get(DataComponents.FOOD).nutrition());
            case GAPPLE -> Comparator.comparing(s -> s.is(Items.ENCHANTED_GOLDEN_APPLE));
            case BLOCK -> Comparator.comparing(s -> ((BlockItem)s.getItem()).getBlock().defaultBlockState().isSolidRender());
            default -> (a, b) -> 0;
         };
         return unique(kind) ? quality.thenComparingInt(Plan::durability) : quality.thenComparingInt(ItemStack::getCount);
      }

      private Comparator<ItemStack> armorComparator(EquipmentSlot slot) {
         double defense = 0, toughness = 0;
         for (EquipmentSlot other : ARMOR) {
            if (other == slot) continue;
            ItemStack stack = this.worn.getOrDefault(other, ItemStack.EMPTY);
            defense += attribute(stack, other, Attributes.ARMOR);
            toughness += attribute(stack, other, Attributes.ARMOR_TOUGHNESS);
         }
         final double kitDefense = defense, kitToughness = toughness;
         return Comparator.<ItemStack>comparingDouble(s -> armorProtection(s, slot, kitDefense, kitToughness, this.rules.hypixel()))
            .thenComparingInt(s -> enchantment(s, Enchantments.FEATHER_FALLING) * 3
               + enchantment(s, Enchantments.THORNS) + enchantment(s, Enchantments.UNBREAKING))
            .thenComparingInt(Plan::durability);
      }
      static double swordDamage(ItemStack stack, boolean hypixel) {
         int sharpness = enchantment(stack, Enchantments.SHARPNESS);
         return attribute(stack, EquipmentSlot.MAINHAND, Attributes.ATTACK_DAMAGE)
            + (sharpness == 0 ? 0 : hypixel ? sharpness * 1.25 : sharpness * 0.5 + 0.5);
      }
      private static double armorProtection(ItemStack stack, EquipmentSlot slot, double kitDefense, double kitToughness, boolean hypixel) {
         double defense = kitDefense + attribute(stack, slot, Attributes.ARMOR);
         double toughness = kitToughness + attribute(stack, slot, Attributes.ARMOR_TOUGHNESS);
         double effective = hypixel ? Math.min(20, defense) : Math.clamp(defense - 6 / (2 + toughness / 4), defense * .2, 20);
         double protection = Math.min(20, enchantment(stack, Enchantments.PROTECTION));
         return Math.round((1 - (1 - effective / 25) * (1 - protection * .04)) * 1000) / 1000.0;
      }
      private static double attribute(ItemStack stack, EquipmentSlot slot, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute) {
         double[] value = {0};
         stack.forEachModifier(slot, (type, modifier) -> { if (type.equals(attribute)) value[0] += modifier.amount(); });
         return value[0];
      }
      static int enchantment(ItemStack stack, ResourceKey<Enchantment> type) {
         for (var enchantment : stack.getEnchantments().entrySet()) if (enchantment.getKey().is(type)) return enchantment.getIntValue();
         return 0;
      }
      static EquipmentSlot armorSlot(ItemStack stack) {
         Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
         if (equippable == null) return null;
         EquipmentSlot slot = equippable.slot();
         return slot != EquipmentSlot.BODY && slot.isArmor() && attribute(stack, slot, Attributes.ARMOR) > 0 ? slot : null;
      }
      private static int durability(ItemStack stack) { return stack.getMaxDamage() - stack.getDamageValue(); }
      private static double toolQuality(ItemStack stack) {
         var tool = stack.get(DataComponents.TOOL);
         if (tool == null) return 0;
         double speed = tool.rules().stream().mapToDouble(rule -> rule.speed().orElse(tool.defaultMiningSpeed())).max().orElse(tool.defaultMiningSpeed());
         int efficiency = enchantment(stack, Enchantments.EFFICIENCY);
         return speed + (speed > 1 && efficiency > 0 ? efficiency * efficiency + 1 : 0);
      }
      static Kind kind(ItemStack stack) {
         if (isSword(stack)) return Kind.SWORD;
         if (isPickaxe(stack)) return Kind.PICKAXE;
         if (isShovel(stack)) return Kind.SHOVEL;
         if (isAxe(stack)) return Kind.AXE;
         if (isHoe(stack)) return Kind.HOE;
         if (stack.is(Items.BOW)) return Kind.BOW;
         if (stack.is(Items.CROSSBOW)) return Kind.CROSSBOW;
         if (stack.is(Items.FISHING_ROD)) return Kind.ROD;
         if (stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE)) return Kind.GAPPLE;
         if (ordinaryFood(stack)) return healthyFood(stack) ? Kind.FOOD : Kind.TRASH;
         if (stack.is(Items.SNOWBALL) || stack.is(Items.EGG)) return Kind.PROJECTILE;
         if (stack.is(Items.ARROW) || stack.is(Items.SPECTRAL_ARROW) || stack.is(Items.TIPPED_ARROW)) return Kind.ARROW;
         if (stack.has(DataComponents.POTION_CONTENTS)) return goodPotion(stack) ? Kind.POTION : Kind.TRASH;
         if (stack.getItem() instanceof PlayerHeadItem) return Kind.UTILITY;
         if (buildingBlock(stack)) return Kind.BLOCK;
         return usefulUtility(stack) ? Kind.UTILITY : Kind.TRASH;
      }
      private static boolean buildingBlock(ItemStack stack) {
         if (!(stack.getItem() instanceof BlockItem item)) return false;
         var block = item.getBlock();
         return !(block instanceof BaseEntityBlock) && !(block instanceof FallingBlock)
            && block != Blocks.TNT && block != Blocks.CRAFTING_TABLE && block != Blocks.SMITHING_TABLE
            && block != Blocks.FLETCHING_TABLE && block != Blocks.CARTOGRAPHY_TABLE && block != Blocks.LOOM
            && block != Blocks.MAGMA_BLOCK && block.getFriction() <= .6F && block.getSpeedFactor() >= 1
            && block.getJumpFactor() >= 1
            && block.defaultBlockState().isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
      }
      private static boolean ordinaryFood(ItemStack s) {
         return s.has(DataComponents.FOOD) && !s.is(Items.GOLDEN_APPLE) && !s.is(Items.ENCHANTED_GOLDEN_APPLE);
      }
      private static boolean healthyFood(ItemStack s) {
         return !s.is(Items.ROTTEN_FLESH) && !s.is(Items.SPIDER_EYE) && !s.is(Items.POISONOUS_POTATO)
            && !s.is(Items.PUFFERFISH) && !s.is(Items.CHICKEN);
      }
      private static boolean goodPotion(ItemStack s) {
         for (var effect : s.get(DataComponents.POTION_CONTENTS).getAllEffects()) {
            if (effect.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL) return true;
         }
         return false;
      }
      private static boolean usefulUtility(ItemStack s) {
         return s.getItem() instanceof SpawnEggItem || s.is(Items.NETHER_STAR)
            || s.is(Items.ELYTRA) || s.is(Items.TOTEM_OF_UNDYING) || s.is(Items.END_CRYSTAL)
            || s.is(Items.ENDER_PEARL) || s.is(Items.ENDER_EYE) || s.is(Items.FIRE_CHARGE) || s.is(Items.WIND_CHARGE)
            || s.is(Items.WATER_BUCKET) || s.is(Items.LAVA_BUCKET) || s.is(Items.MILK_BUCKET) || s.is(Items.BUCKET)
            || s.is(Items.SHEARS) || s.is(Items.FLINT_AND_STEEL) || s.is(Items.TRIDENT) || s.is(Items.MACE)
            || s.is(Items.SHIELD) || s.is(Items.EXPERIENCE_BOTTLE) || s.is(Items.FIREWORK_ROCKET)
            || s.is(Items.COMPASS) || s.is(Items.CLOCK);
      }
      static boolean serverItem(ItemStack stack) {
         var name = stack.get(DataComponents.CUSTOM_NAME);
         if (name == null) return false;
         String text = name.getString().toLowerCase(Locale.ROOT);
         return text.contains("click") || text.contains("right") || text.contains("teleport")
            || text.contains("点击") || text.contains("传送") || text.contains("使用")
            || text.contains("再来") || text.contains("选择") || text.contains("离开游戏");
      }
      // Identity fallbacks are needed before server item tags have been bound.
      private static boolean isSword(ItemStack s) { return s.is(ItemTags.SWORDS) || s.is(Items.WOODEN_SWORD) || s.is(Items.STONE_SWORD) || s.is(Items.IRON_SWORD) || s.is(Items.GOLDEN_SWORD) || s.is(Items.DIAMOND_SWORD) || s.is(Items.NETHERITE_SWORD); }
      private static boolean isPickaxe(ItemStack s) { return s.is(ItemTags.PICKAXES) || s.is(Items.WOODEN_PICKAXE) || s.is(Items.STONE_PICKAXE) || s.is(Items.IRON_PICKAXE) || s.is(Items.GOLDEN_PICKAXE) || s.is(Items.DIAMOND_PICKAXE) || s.is(Items.NETHERITE_PICKAXE); }
      private static boolean isAxe(ItemStack s) { return s.is(ItemTags.AXES) || s.is(Items.WOODEN_AXE) || s.is(Items.STONE_AXE) || s.is(Items.IRON_AXE) || s.is(Items.GOLDEN_AXE) || s.is(Items.DIAMOND_AXE) || s.is(Items.NETHERITE_AXE); }
      private static boolean isShovel(ItemStack s) { return s.is(ItemTags.SHOVELS) || s.is(Items.WOODEN_SHOVEL) || s.is(Items.STONE_SHOVEL) || s.is(Items.IRON_SHOVEL) || s.is(Items.GOLDEN_SHOVEL) || s.is(Items.DIAMOND_SHOVEL) || s.is(Items.NETHERITE_SHOVEL); }
      private static boolean isHoe(ItemStack s) { return s.is(ItemTags.HOES) || s.is(Items.WOODEN_HOE) || s.is(Items.STONE_HOE) || s.is(Items.IRON_HOE) || s.is(Items.GOLDEN_HOE) || s.is(Items.DIAMOND_HOE) || s.is(Items.NETHERITE_HOE); }
      private static boolean isTool(ItemStack s) { return isPickaxe(s) || isAxe(s) || isShovel(s) || isHoe(s); }
      private static int armorMenuSlot(EquipmentSlot slot) {
         return switch (slot) { case HEAD -> 5; case CHEST -> 6; case LEGS -> 7; case FEET -> 8; default -> throw new IllegalArgumentException(); };
      }
      private static Action click(int index, int button, ContainerInput input) { return new Action(menuSlot(index), button, input); }
   }
}
