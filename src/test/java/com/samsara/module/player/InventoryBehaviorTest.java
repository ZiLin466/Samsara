package com.samsara.module.player;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import com.samsara.config.ModuleConfigCodec;
import com.samsara.module.Feature;
import com.samsara.setting.BooleanSetting;
import com.samsara.setting.NumberSetting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class InventoryBehaviorTest {
   private static net.minecraft.core.HolderLookup.Provider registries;
   @BeforeAll static void bootstrap() {
      SharedConstants.tryDetectVersion();
      Bootstrap.bootStrap();
      registries = VanillaRegistries.createWorldLookup();
      net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(registries)
         .forEach(net.minecraft.core.component.DataComponentInitializers.PendingComponents::apply);
   }

   private static ItemStack[] items() {
      var result = new ItemStack[36]; Arrays.fill(result, ItemStack.EMPTY); return result;
   }
   private static EnumMap<EquipmentSlot, ItemStack> armor() { return new EnumMap<>(EquipmentSlot.class); }
   private static InvManager.Plan plan(ItemStack[] items, EnumMap<EquipmentSlot, ItemStack> armor, boolean hypixel, boolean drop) {
      return new InvManager.Plan(items, armor, hypixel, drop, false, 0, 1, 2);
   }
   private static InvManager.Action next(InvManager.Plan plan) { return plan.next(slot -> true); }

   @Test void hypixelAloneMakesSharpnessOneStoneBeatPlainIron() {
      var stone = new ItemStack(Items.STONE_SWORD);
      stone.enchant(registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS), 1);
      var iron = new ItemStack(Items.IRON_SWORD);
      assertTrue(InvManager.Plan.swordDamage(stone, true) > InvManager.Plan.swordDamage(iron, true));
      assertEquals(InvManager.Plan.swordDamage(stone, false), InvManager.Plan.swordDamage(iron, false));
      var inventory = items(); inventory[0] = iron; inventory[20] = stone;
      assertEquals(new InvManager.Action(20, 0, ContainerInput.SWAP), next(plan(inventory, armor(), true, true)));
      assertEquals(new InvManager.Action(20, 1, ContainerInput.THROW), next(plan(inventory, armor(), false, true)));
   }

   @Test void equalWeaponsAndApplesKeepTheirConfiguredSlots() {
      var inventory = items(); inventory[0] = new ItemStack(Items.IRON_SWORD);
      inventory[9] = new ItemStack(Items.IRON_SWORD); inventory[10] = new ItemStack(Items.IRON_SWORD);
      inventory[1] = new ItemStack(Items.STONE, 64); inventory[11] = new ItemStack(Items.STONE, 64);
      inventory[2] = new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 64); inventory[12] = new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 64);
      assertNull(next(plan(inventory, armor(), false, false)));
      var drop = next(plan(inventory, armor(), false, true));
      assertEquals(new InvManager.Action(9, 1, ContainerInput.THROW), drop);
   }

   @Test void fullInventorySortsThenDisposesWholeJunkStacks() {
      var inventory = items(); Arrays.fill(inventory, new ItemStack(Items.STICK, 64));
      inventory[20] = new ItemStack(Items.DIAMOND_SWORD);
      inventory[21] = new ItemStack(Items.STONE, 64); inventory[22] = new ItemStack(Items.GOLDEN_APPLE, 32);
      var worn = armor(); int clicks = 0;
      for (InvManager.Action action; (action = next(plan(inventory, worn, false, true))) != null;) {
         assertTrue(++clicks < 45, "Cleanup must converge");
         apply(action, inventory, worn);
      }
      assertEquals(3 + 33, clicks);
      assertTrue(inventory[0].is(Items.DIAMOND_SWORD));
      assertTrue(inventory[1].is(Items.STONE)); assertTrue(inventory[2].is(Items.GOLDEN_APPLE));
      for (int slot = 3; slot < 36; slot++) assertTrue(inventory[slot].isEmpty());
   }

   @Test void fullInventoryReplacesArmorAndDropsEqualSpareWithoutOscillating() {
      var inventory = items(); Arrays.fill(inventory, new ItemStack(Items.STICK, 64));
      inventory[20] = new ItemStack(Items.DIAMOND_CHESTPLATE); inventory[21] = new ItemStack(Items.DIAMOND_CHESTPLATE);
      var worn = armor(); worn.put(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
      assertEquals(new InvManager.Action(6, 1, ContainerInput.THROW), next(plan(inventory, worn, false, true)));
      int clicks = 0;
      for (InvManager.Action action; (action = next(plan(inventory, worn, false, true))) != null;) {
         assertTrue(++clicks < 45, "Armor and cleanup must converge"); apply(action, inventory, worn);
      }
      assertTrue(worn.get(EquipmentSlot.CHEST).is(Items.DIAMOND_CHESTPLATE));
      for (ItemStack stack : inventory) assertTrue(stack.isEmpty());
   }

   @Test void occupiedArmorMovesToFreeInventoryBeforeUpgrade() {
      var inventory = items(); inventory[20] = new ItemStack(Items.DIAMOND_HELMET);
      var worn = armor(); worn.put(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
      assertEquals(new InvManager.Action(5, 0, ContainerInput.QUICK_MOVE), next(plan(inventory, worn, false, false)));
      inventory[0] = new ItemStack(Items.DIAMOND_HELMET); inventory[20] = ItemStack.EMPTY;
      assertEquals(new InvManager.Action(5, 0, ContainerInput.SWAP), next(plan(inventory, worn, false, false)));
   }

   @Test void disabledDroppingDoesNotDestroyItemsToReplaceFullArmor() {
      var inventory = items(); Arrays.fill(inventory, new ItemStack(Items.STICK, 64)); inventory[20] = new ItemStack(Items.DIAMOND_HELMET);
      var worn = armor(); worn.put(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
      assertNull(next(plan(inventory, worn, false, false)));
      assertEquals(new InvManager.Action(36, 1, ContainerInput.THROW), plan(inventory, worn, false, true).next(slot -> slot != 5));
   }

   @Test void toolsRetainBestAndOtherWearablesCannotBlockArmorCleanup() {
      var inventory = items(); inventory[9] = new ItemStack(Items.IRON_PICKAXE); inventory[10] = new ItemStack(Items.DIAMOND_PICKAXE);
      inventory[1] = new ItemStack(Items.STONE, 64);
      inventory[11] = new ItemStack(Items.CARVED_PUMPKIN);
      assertNull(InvManager.Plan.armorSlot(inventory[11]));
      assertEquals(new InvManager.Action(9, 1, ContainerInput.THROW), next(plan(inventory, armor(), false, true)));
      inventory[9] = ItemStack.EMPTY;
      assertNull(next(plan(inventory, armor(), false, true)));
   }

   @Test void conflictingHotbarSettingsHaveStablePriority() {
      var inventory = items(); inventory[0] = new ItemStack(Items.STONE, 64); inventory[20] = new ItemStack(Items.DIAMOND_SWORD);
      inventory[21] = new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 64);
      var worn = armor();
      var action = next(new InvManager.Plan(inventory, worn, false, false, false, 0, 0, 0));
      assertEquals(new InvManager.Action(20, 0, ContainerInput.SWAP), action);
      apply(action, inventory, worn);
      assertNull(next(new InvManager.Plan(inventory, worn, false, false, false, 0, 0, 0)));
   }

   @Test void closedInventoryCannotSortOrDropAndOpeningItStillCleansUp() {
      var inventory = items();
      inventory[9] = new ItemStack(Items.STICK, 64);
      inventory[10] = new ItemStack(Items.IRON_PICKAXE);
      inventory[11] = new ItemStack(Items.WOODEN_SWORD);
      inventory[20] = new ItemStack(Items.DIAMOND_SWORD);
      inventory[21] = new ItemStack(Items.STONE, 64);
      inventory[22] = new ItemStack(Items.GOLDEN_APPLE, 32);
      inventory[23] = new ItemStack(Items.DIAMOND_HELMET);
      var worn = armor();
      assertNull(new InvManager.Plan(inventory, worn, true, true, true, 0, 1, 2).next(slot -> true, false));
      assertTrue(inventory[20].is(Items.DIAMOND_SWORD));
      assertTrue(worn.isEmpty());
      int clicks = 0;
      for (InvManager.Action action; (action = new InvManager.Plan(inventory, worn, true, true, true, 0, 1, 2)
         .next(slot -> true, true)) != null;) {
         assertTrue(++clicks < 12, "Inventory cleanup must converge");
         apply(action, inventory, worn);
      }
      assertTrue(inventory[0].is(Items.DIAMOND_SWORD));
      assertTrue(inventory[1].is(Items.STONE));
      assertTrue(inventory[2].is(Items.GOLDEN_APPLE));
      assertTrue(worn.get(EquipmentSlot.HEAD).is(Items.DIAMOND_HELMET));
      assertTrue(inventory[9].isEmpty());
      assertTrue(inventory[10].isEmpty());
      assertTrue(inventory[11].isEmpty());
   }

   @Test void closedFullInventoryCannotThrowWornArmorToMakeRoom() {
      var inventory = items(); Arrays.fill(inventory, new ItemStack(Items.STICK, 64));
      inventory[20] = new ItemStack(Items.DIAMOND_HELMET);
      var worn = armor(); worn.put(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
      assertNull(plan(inventory, worn, true, true).next(slot -> true, false));
      assertEquals(new InvManager.Action(5, 1, ContainerInput.THROW), next(plan(inventory, worn, true, true)));
   }

   private static InvManager.Rules rules(Map<InvManager.Kind, Integer> slots, boolean drop, boolean tools, boolean food,
                                        boolean equip, boolean durability, int blocks, int projectiles, int arrows, int foods) {
      return new InvManager.Rules(false, drop, tools, food, equip, durability, slots, blocks, projectiles, arrows, foods);
   }
   private static InvManager.Rules unsorted() { return rules(Map.of(), true, false, false, true, true, 128, 64, 256, 64); }

   @Test void allConfiguredSlotsSortAndConvergeWithoutSwappingCategoriesBack() {
      var inventory = items();
      inventory[20] = new ItemStack(Items.DIAMOND_SWORD);
      inventory[21] = new ItemStack(Items.STONE, 64);
      inventory[22] = new ItemStack(Items.GOLDEN_APPLE, 8);
      inventory[23] = new ItemStack(Items.DIAMOND_PICKAXE);
      inventory[24] = new ItemStack(Items.DIAMOND_SHOVEL);
      inventory[25] = new ItemStack(Items.DIAMOND_AXE);
      inventory[26] = new ItemStack(Items.SNOWBALL, 16);
      inventory[27] = new ItemStack(Items.BOW);
      inventory[28] = new ItemStack(Items.COOKED_BEEF, 16);
      int clicks = 0;
      for (InvManager.Action action; (action = next(new InvManager.Plan(inventory, armor(), InvManager.Rules.defaults()))) != null;) {
         assertTrue(++clicks <= 9); apply(action, inventory, armor());
      }
      assertEquals(9, clicks);
      var expected = new net.minecraft.world.item.Item[]{Items.DIAMOND_SWORD, Items.STONE, Items.GOLDEN_APPLE,
         Items.DIAMOND_PICKAXE, Items.DIAMOND_SHOVEL, Items.DIAMOND_AXE, Items.SNOWBALL, Items.BOW, Items.COOKED_BEEF};
      for (int i = 0; i < expected.length; i++) assertTrue(inventory[i].is(expected[i]));
   }

   @Test void disabledToolSlotStillRetainsBestAndDropsInferiorTools() {
      var inventory = items();
      inventory[20] = new ItemStack(Items.IRON_PICKAXE);
      inventory[21] = new ItemStack(Items.DIAMOND_PICKAXE);
      var plan = new InvManager.Plan(inventory, armor(), unsorted());
      assertEquals(new InvManager.Action(20, 1, ContainerInput.THROW), next(plan));
      assertTrue(plan.keeps(21));
      assertEquals(-1, plan.destination(21));
      inventory[20] = ItemStack.EMPTY;
      assertNull(next(new InvManager.Plan(inventory, armor(), unsorted())));
   }

   @Test void toolAndFoodDisposalAreIndependentAndNeverDiscardGoldenApples() {
      var inventory = items();
      inventory[20] = new ItemStack(Items.DIAMOND_PICKAXE);
      inventory[21] = new ItemStack(Items.COOKED_BEEF, 32);
      inventory[22] = new ItemStack(Items.GOLDEN_APPLE, 16);
      inventory[23] = new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 8);
      var options = rules(Map.of(), false, true, true, true, true, 128, 64, 256, 64);
      var plan = new InvManager.Plan(inventory, armor(), options);
      assertTrue(plan.disposable(20)); assertTrue(plan.disposable(21));
      assertFalse(plan.disposable(22)); assertFalse(plan.disposable(23));
      assertTrue(plan.keeps(22)); assertTrue(plan.keeps(23));
      var chest = new ChestStealer.LootPlan(items(), armor(), new ItemStack[]{inventory[20], inventory[21], inventory[22]}, options, true, true);
      assertFalse(chest.wanted(0)); assertFalse(chest.wanted(1)); assertTrue(chest.wanted(2));
   }

   @Test void betterFoodIsKeptBeforeWorseFoodWithinTheCountLimit() {
      var inventory = items();
      inventory[8] = new ItemStack(Items.BREAD, 64);
      inventory[20] = new ItemStack(Items.COOKED_BEEF, 64);
      inventory[21] = new ItemStack(Items.ROTTEN_FLESH, 64);
      var plan = new InvManager.Plan(inventory, armor(), InvManager.Rules.defaults());
      assertEquals(new InvManager.Action(20, 8, ContainerInput.SWAP), next(plan));
      assertFalse(plan.keeps(8)); assertTrue(plan.keeps(20)); assertFalse(plan.keeps(21));
      apply(next(plan), inventory, armor());
      var sorted = new InvManager.Plan(inventory, armor(), InvManager.Rules.defaults());
      assertTrue(sorted.keeps(8)); assertTrue(sorted.disposable(20));
   }

   @Test void jointChestPlanRespectsQuantitiesAndDoesNotGrabEqualOrWorseEquipment() {
      var inventory = items();
      inventory[0] = new ItemStack(Items.DIAMOND_SWORD);
      inventory[1] = new ItemStack(Items.STONE, 64);
      inventory[10] = new ItemStack(Items.STONE, 64);
      inventory[11] = new ItemStack(Items.SNOWBALL, 16);
      inventory[12] = new ItemStack(Items.EGG, 16);
      inventory[13] = new ItemStack(Items.SNOWBALL, 16);
      inventory[14] = new ItemStack(Items.EGG, 16);
      for (int i = 15; i < 19; i++) inventory[i] = new ItemStack(Items.ARROW, 64);
      var worn = armor(); worn.put(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
      var chest = new ItemStack[]{new ItemStack(Items.STONE, 64), new ItemStack(Items.EGG, 16),
         new ItemStack(Items.ARROW, 64), new ItemStack(Items.IRON_SWORD), new ItemStack(Items.DIAMOND_SWORD),
         new ItemStack(Items.IRON_HELMET), new ItemStack(Items.DIAMOND_HELMET), new ItemStack(Items.GOLDEN_APPLE, 8)};
      var loot = new ChestStealer.LootPlan(inventory, worn, chest, InvManager.Rules.defaults(), true, true);
      for (int i = 0; i < 7; i++) assertFalse(loot.wanted(i), "Should skip chest slot " + i);
      assertTrue(loot.wanted(7));
   }

   @Test void enchantedUpgradesAreTakenAndLowDurabilityCheckCanBeDisabled() {
      var inventory = items(); inventory[0] = new ItemStack(Items.IRON_SWORD);
      var sword = new ItemStack(Items.IRON_SWORD);
      sword.enchant(registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS), 2);
      var pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
      pickaxe.setDamageValue(pickaxe.getMaxDamage() - 15);
      var chest = new ItemStack[]{new ItemStack(Items.IRON_SWORD), sword, pickaxe};
      var loot = new ChestStealer.LootPlan(inventory, armor(), chest, unsorted(), true, true);
      assertFalse(loot.wanted(0)); assertTrue(loot.wanted(1)); assertFalse(loot.wanted(2));
      var unchecked = rules(Map.of(), true, false, false, true, false, 128, 64, 256, 64);
      assertTrue(new ChestStealer.LootPlan(inventory, armor(), chest, unchecked, true, true).wanted(2));
   }

   @Test void disabledAutoArmorRetainsTheUpgradeWithoutRemovingWornArmor() {
      var inventory = items(); inventory[20] = new ItemStack(Items.DIAMOND_HELMET);
      var worn = armor(); worn.put(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
      var options = rules(Map.of(), true, false, false, false, true, 128, 64, 256, 64);
      var plan = new InvManager.Plan(inventory, worn, options);
      assertNull(next(plan)); assertTrue(plan.keeps(20));
   }

   @Test void harmfulPotionsAndInteractableBlocksAreSkippedWhileBuffsAreRetained() {
      var heal = new ItemStack(Items.POTION);
      heal.set(DataComponents.POTION_CONTENTS, PotionContents.EMPTY.withEffectAdded(new MobEffectInstance(MobEffects.REGENERATION, 200)));
      var poison = new ItemStack(Items.SPLASH_POTION);
      poison.set(DataComponents.POTION_CONTENTS, PotionContents.EMPTY.withEffectAdded(new MobEffectInstance(MobEffects.POISON, 200)));
      var chest = new ItemStack[]{new ItemStack(Items.CHEST), new ItemStack(Items.TNT), poison, heal,
         new ItemStack(Items.STONE, 64), new ItemStack(Items.ENDER_PEARL, 16), new ItemStack(Items.GLASS, 32),
         new ItemStack(Items.CRAFTING_TABLE), new ItemStack(Items.MAGMA_BLOCK), new ItemStack(Items.SAND, 64)};
      var plan = new ChestStealer.LootPlan(items(), armor(), chest, unsorted(), true, false);
      assertFalse(plan.wanted(0)); assertFalse(plan.wanted(1)); assertFalse(plan.wanted(2));
      assertTrue(plan.wanted(3)); assertTrue(plan.wanted(4)); assertTrue(plan.wanted(5));
      assertTrue(plan.wanted(6)); assertFalse(plan.wanted(7)); assertFalse(plan.wanted(8)); assertFalse(plan.wanted(9));
   }

   @Test void serverItemsAreProtectedEvenWhenTheyOccupyTheConfiguredSlot() {
      var inventory = items();
      inventory[0] = new ItemStack(Items.WOODEN_SWORD); inventory[0].set(DataComponents.CUSTOM_NAME, Component.literal("Right click to select"));
      inventory[20] = new ItemStack(Items.DIAMOND_SWORD);
      var plan = new InvManager.Plan(inventory, armor(), InvManager.Rules.defaults());
      assertNull(next(plan)); assertFalse(plan.disposable(0)); assertTrue(plan.keeps(20));
      var loot = new ChestStealer.LootPlan(inventory, armor(), new ItemStack[]{new ItemStack(Items.NETHERITE_SWORD)}, InvManager.Rules.defaults(), true, true);
      assertEquals(ContainerInput.QUICK_MOVE, loot.next("Index", 0, i -> true, i -> true, (a, b) -> true, i -> false).input());
   }

   @Test void stackMergingPreservesCountAndAbortsWhenCursorOwnershipChanges() {
      var inventory = items(); inventory[10] = new ItemStack(Items.STONE, 40); inventory[11] = new ItemStack(Items.STONE, 40);
      var merge = new InvManager.Plan(inventory, armor(), unsorted()).merge(i -> true);
      assertEquals(new InvManager.Merge(11, 10), merge);
      var sequence = new InvManager.MergeSequence(11, 10, inventory[11], inventory[10]);
      assertEquals(new InvManager.Action(11, 0, ContainerInput.PICKUP), sequence.next(ItemStack.EMPTY, inventory[11], inventory[10]));
      assertEquals(new InvManager.Action(10, 0, ContainerInput.PICKUP), sequence.next(inventory[11], ItemStack.EMPTY, inventory[10]));
      assertEquals(new InvManager.Action(11, 0, ContainerInput.PICKUP),
         sequence.next(new ItemStack(Items.STONE, 16), ItemStack.EMPTY, new ItemStack(Items.STONE, 64)));
      assertNull(sequence.next(ItemStack.EMPTY, new ItemStack(Items.STONE, 16), new ItemStack(Items.STONE, 64)));
      var interrupted = new InvManager.MergeSequence(11, 10, inventory[11], inventory[10]);
      interrupted.next(ItemStack.EMPTY, inventory[11], inventory[10]);
      assertNull(interrupted.next(new ItemStack(Items.STICK, 40), ItemStack.EMPTY, inventory[10]));
   }

   @Test void aFullInventoryCanMergeMatchingStacksButNotDifferentComponents() {
      var inventory = new Inventory(null, new EntityEquipment());
      var menu = ChestMenu.threeRows(1, inventory);
      for (int index = 0; index < 36; index++) inventory.setItem(index, new ItemStack(Items.STICK, 64));
      inventory.setItem(10, new ItemStack(Items.STONE, 50));
      assertEquals(14, ChestStealer.capacity(menu, new ItemStack(Items.STONE, 64)));
      var renamed = new ItemStack(Items.STONE, 64); renamed.set(DataComponents.CUSTOM_NAME, Component.literal("Other"));
      assertEquals(0, ChestStealer.capacity(menu, renamed));
      inventory.setItem(11, ItemStack.EMPTY);
      assertEquals(64, ChestStealer.capacity(menu, renamed));
   }

   @Test void fullInventoryCanSwapAnUpgradeWithoutDisposingTheCurrentWeaponFirst() {
      var inventory = items(); Arrays.fill(inventory, new ItemStack(Items.ENDER_PEARL, 16));
      inventory[0] = new ItemStack(Items.IRON_SWORD);
      var chest = new ItemStack[]{new ItemStack(Items.DIAMOND_SWORD)};
      var plan = new ChestStealer.LootPlan(inventory, armor(), chest, InvManager.Rules.defaults(), true, true);
      assertEquals(new ChestStealer.LootAction(0, 0, ContainerInput.SWAP),
         plan.next("Index", 0, i -> true, i -> false, (a, b) -> true, i -> false));
      assertEquals(-1, plan.disposableInventorySlot(i -> true));
      var disabled = new ChestStealer.LootPlan(inventory, armor(), chest, InvManager.Rules.defaults(), true, false);
      assertNull(disabled.next("Index", 0, i -> true, i -> false, (a, b) -> true, i -> false));
      assertTrue(disabled.hasWanted()); assertFalse(disabled.hasRoom(i -> false, (a, b) -> true));
   }

   @Test void failedTransfersSkipTheSlotUntilActualInventoryContentsChange() {
      var inventory = items();
      var plan = new ChestStealer.LootPlan(inventory, armor(),
         new ItemStack[]{new ItemStack(Items.ENDER_PEARL, 16), new ItemStack(Items.GOLDEN_APPLE, 8)}, unsorted(), true, false);
      var tracker = new ChestStealer.TransferTracker(); tracker.refresh(plan.items, plan.chestSize);
      assertEquals(0, plan.next("Index", 0, i -> true, i -> true, (a, b) -> false, tracker::rejected).slot());
      tracker.reject(0); tracker.refresh(plan.items, plan.chestSize);
      assertEquals(1, plan.next("Index", 0, i -> true, i -> true, (a, b) -> false, tracker::rejected).slot());
      tracker.reject(1);
      tracker.reject(plan.chestSize + 10);
      tracker.refresh(plan.items, plan.chestSize);
      assertTrue(tracker.rejected(plan.chestSize + 10));
      assertNull(plan.next("Index", 0, i -> true, i -> true, (a, b) -> false, tracker::rejected));
      plan.items[0] = new ItemStack(Items.STONE, 64); tracker.refresh(plan.items, plan.chestSize);
      assertFalse(tracker.rejected(0)); assertFalse(tracker.rejected(1));
      assertFalse(tracker.rejected(plan.chestSize + 10));
   }

   @Test void skipTrashOffAllowsManualLootRulesAndLockedSlotsAreNeverClicked() {
      var chest = new ItemStack[]{new ItemStack(Items.STICK, 64), new ItemStack(Items.COAL, 64)};
      var filtered = new ChestStealer.LootPlan(items(), armor(), chest, unsorted(), true, false);
      assertFalse(filtered.hasWanted());
      var all = new ChestStealer.LootPlan(items(), armor(), chest, unsorted(), false, false);
      assertEquals(1, all.next("Index", 0, i -> i != 0, i -> true, (a, b) -> false, i -> false).slot());
      assertNull(all.next("Index", 0, i -> false, i -> true, (a, b) -> false, i -> false));
   }

   @Test void menuMappingsMatchRealSingleAndDoubleChestSlotsAndQuickMoveMerges() {
      var inventory = new Inventory(null, new EntityEquipment());
      for (ChestMenu menu : List.of(ChestMenu.threeRows(1, inventory), ChestMenu.sixRows(2, inventory))) {
         int size = menu.getContainer().getContainerSize();
         for (int index = 0; index < 36; index++) {
            var slot = menu.getSlot(ChestStealer.inventoryMenuSlot(size, index));
            assertSame(inventory, slot.container); assertEquals(index, slot.getContainerSlot());
         }
         for (int index = 0; index < 36; index++) inventory.setItem(index, new ItemStack(Items.STICK, 64));
         inventory.setItem(10, new ItemStack(Items.STONE, 50));
         menu.getSlot(0).set(new ItemStack(Items.STONE, 64));
         menu.quickMoveStack(null, 0);
         assertEquals(64, inventory.getItem(10).getCount());
         assertEquals(50, menu.getSlot(0).getItem().getCount());
      }
   }

   private static NumberSetting number(Feature feature, String name) {
      return (NumberSetting)feature.settings.stream().filter(s -> s.getName().equals(name)).findFirst().orElseThrow();
   }

   @Test void durabilityPrefersAHealthyReplacementButKeepsTheLastOwnedTool() {
      var inventory = items();
      var worn = new ItemStack(Items.DIAMOND_PICKAXE); worn.setDamageValue(worn.getMaxDamage() - 10);
      inventory[20] = worn;
      assertTrue(new InvManager.Plan(inventory, armor(), unsorted()).keeps(20));
      inventory[21] = new ItemStack(Items.IRON_PICKAXE);
      var replacement = new InvManager.Plan(inventory, armor(), unsorted());
      assertFalse(replacement.keeps(20)); assertTrue(replacement.keeps(21));
      var elytra = new ItemStack(Items.ELYTRA); elytra.setDamageValue(elytra.getMaxDamage() - 1); inventory[22] = elytra;
      assertTrue(new InvManager.Plan(inventory, armor(), unsorted()).keeps(22));
   }

   @Test void projectileSlotFallsBackToTheBestRodAndConflictsStayStable() {
      var inventory = items(); inventory[20] = new ItemStack(Items.FISHING_ROD);
      assertEquals(new InvManager.Action(20, 6, ContainerInput.SWAP),
         next(new InvManager.Plan(inventory, armor(), InvManager.Rules.defaults())));
      inventory[21] = new ItemStack(Items.SNOWBALL, 16);
      assertEquals(new InvManager.Action(21, 6, ContainerInput.SWAP),
         next(new InvManager.Plan(inventory, armor(), InvManager.Rules.defaults())));
      var slots = Map.of(InvManager.Kind.SWORD, 0, InvManager.Kind.PICKAXE, 0, InvManager.Kind.BLOCK, 0, InvManager.Kind.GAPPLE, 0);
      inventory = items(); inventory[20] = new ItemStack(Items.DIAMOND_SWORD); inventory[21] = new ItemStack(Items.DIAMOND_PICKAXE);
      inventory[22] = new ItemStack(Items.STONE, 64); inventory[23] = new ItemStack(Items.GOLDEN_APPLE, 8);
      var options = rules(slots, false, false, false, false, true, 128, 64, 256, 64);
      var action = next(new InvManager.Plan(inventory, armor(), options));
      assertEquals(new InvManager.Action(20, 0, ContainerInput.SWAP), action); apply(action, inventory, armor());
      assertNull(next(new InvManager.Plan(inventory, armor(), options)));
   }

   @Test void chestNameCheckAcceptsLocalizedAndDoubleChestsButRejectsShops() {
      assertTrue(ChestStealer.acceptsChestName("Chest", "箱子", "大型箱子"));
      assertTrue(ChestStealer.acceptsChestName("Large Chest", "箱子", "大型箱子"));
      assertTrue(ChestStealer.acceptsChestName("箱子", "箱子", "大型箱子"));
      assertTrue(ChestStealer.acceptsChestName("大型箱子", "箱子", "大型箱子"));
      assertTrue(ChestStealer.acceptsChestName("", "箱子", "大型箱子"));
      assertFalse(ChestStealer.acceptsChestName("Item Shop", "箱子", "大型箱子"));
      assertFalse(ChestStealer.acceptsChestName("Upgrades", "箱子", "大型箱子"));
   }
   @Test void oldDelayAndSlotSettingsLoadAndExpandedSettingsRoundTrip() {
      var manager = new InvManager(); var stealer = new ChestStealer();
      var modules = List.<Feature>of(manager, stealer);
      var old = ModuleConfigCodec.snapshot(modules, ModuleConfigCodec.Scope.GAMEPLAY, false);
      for (String module : List.of("InvManager", "ChestStealer")) {
         var values = old.getAsJsonObject(module).getAsJsonObject("settings");
         values.addProperty("Delay", 4); values.remove("Delay Max");
      }
      old.getAsJsonObject("InvManager").getAsJsonObject("settings").addProperty("Gapple Slot", 3);
      ModuleConfigCodec.prepare(modules, old, ModuleConfigCodec.Scope.GAMEPLAY, true).run();
      assertEquals(4, number(manager, "Delay Max").m220()); assertEquals(4, number(stealer, "Delay Max").m220());
      assertFalse(old.getAsJsonObject("InvManager").getAsJsonObject("settings").has("Delay Max"));
      number(manager, "Pickaxe Slot").m223(0);
      number(manager, "Max Food").m223(32);
      ((BooleanSetting)manager.settings.stream().filter(s -> s.getName().equals("Drop Food")).findFirst().orElseThrow()).m217(true);
      number(manager, "Delay Max").m223(8);
      var saved = ModuleConfigCodec.snapshot(modules, ModuleConfigCodec.Scope.GAMEPLAY, false);
      var loaded = new InvManager();
      ModuleConfigCodec.prepare(List.of(loaded), saved, ModuleConfigCodec.Scope.GAMEPLAY, true).run();
      assertEquals(-1, loaded.rules().slot(InvManager.Kind.PICKAXE));
      assertTrue(loaded.rules().dropFood()); assertEquals(32, loaded.rules().maxFood());
      assertEquals(3, number(loaded, "Gapple Slot").m220());
      assertEquals(8, number(loaded, "Delay Max").m220());
   }

   private static void apply(InvManager.Action action, ItemStack[] inventory, EnumMap<EquipmentSlot, ItemStack> worn) {
      int slot = action.slot();
      EquipmentSlot armorSlot = slot >= 5 && slot <= 8 ? InvManager.Plan.ARMOR[slot - 5] : null;
      int index = slot >= 36 ? slot - 36 : slot;
      ItemStack stack = armorSlot == null ? inventory[index] : worn.getOrDefault(armorSlot, ItemStack.EMPTY);
      switch (action.input()) {
         case THROW -> { assertEquals(1, action.button()); if (armorSlot == null) inventory[index] = ItemStack.EMPTY; else worn.put(armorSlot, ItemStack.EMPTY); }
         case SWAP -> {
            ItemStack target = inventory[action.button()]; inventory[action.button()] = stack;
            if (armorSlot == null) inventory[index] = target; else worn.put(armorSlot, target);
         }
         case QUICK_MOVE -> {
            if (armorSlot != null) {
               int free = 0; while (!inventory[free].isEmpty()) free++;
               inventory[free] = stack; worn.put(armorSlot, ItemStack.EMPTY);
            } else {
               worn.put(InvManager.Plan.armorSlot(stack), stack); inventory[index] = ItemStack.EMPTY;
            }
         }
         default -> fail("Unexpected click " + action);
      }
   }
}
