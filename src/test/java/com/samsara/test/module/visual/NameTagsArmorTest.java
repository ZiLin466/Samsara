package com.samsara.test.module.visual;

import com.samsara.module.visual.NameTags.Armor;
import com.samsara.module.visual.NameTags.Geometry;
import java.util.EnumMap;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class NameTagsArmorTest {
   @BeforeAll static void bootstrap() {
      SharedConstants.tryDetectVersion();
      Bootstrap.bootStrap();
      var registries = VanillaRegistries.createWorldLookup();
      net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(registries)
         .forEach(net.minecraft.core.component.DataComponentInitializers.PendingComponents::apply);
   }

   private static int value(EnumMap<EquipmentSlot, ItemStack> equipment) {
      return Armor.value(slot -> equipment.getOrDefault(slot, ItemStack.EMPTY));
   }

   @Test void visibleDiamondArmorProducesMinusTwentyWithoutEntityAttributeUpdates() {
      var equipment = new EnumMap<EquipmentSlot, ItemStack>(EquipmentSlot.class);
      assertEquals(0, value(equipment));
      equipment.put(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
      equipment.put(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
      equipment.put(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
      equipment.put(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
      int targetArmor = value(equipment);
      assertEquals(20, targetArmor);
      assertEquals("-20", Geometry.armorPart(0, targetArmor).text().getString());
      assertEquals(Geometry.RED, Geometry.armorPart(0, targetArmor).color());
      assertEquals("20", Geometry.armorPart(targetArmor, 0).text().getString());
      assertEquals(Geometry.GREEN, Geometry.armorPart(targetArmor, 0).color());
      equipment.remove(EquipmentSlot.CHEST);
      assertEquals(12, value(equipment));
      equipment.clear();
      assertEquals("=", Geometry.armorPart(0, value(equipment)).text().getString());
   }

   @Test void armorUsesTheEquippedSlotAndIgnoresSpareArmorInHand() {
      var equipment = new EnumMap<EquipmentSlot, ItemStack>(EquipmentSlot.class);
      equipment.put(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
      assertEquals(6, value(equipment));
      equipment.put(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_CHESTPLATE));
      equipment.put(EquipmentSlot.HEAD, new ItemStack(Items.IRON_CHESTPLATE));
      assertEquals(6, value(equipment));
      equipment.put(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
      assertEquals(3, value(equipment));
   }

   @Test void customArmorComponentsHonorModifierOperationsAndAttributeType() {
      var chest = new ItemStack(Items.IRON_CHESTPLATE);
      chest.set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY
         .withModifierAdded(Attributes.ARMOR, modifier("base", 4, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.CHEST)
         .withModifierAdded(Attributes.ARMOR, modifier("bonus", .5, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.CHEST)
         .withModifierAdded(Attributes.ARMOR, modifier("total", .5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), EquipmentSlotGroup.CHEST)
         .withModifierAdded(Attributes.ARMOR_TOUGHNESS, modifier("toughness", 8, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.CHEST));
      var equipment = new EnumMap<EquipmentSlot, ItemStack>(EquipmentSlot.class);
      equipment.put(EquipmentSlot.CHEST, chest);
      assertEquals(9, value(equipment));
      equipment.remove(EquipmentSlot.CHEST);
      equipment.put(EquipmentSlot.MAINHAND, chest);
      assertEquals(0, value(equipment));
   }

   private static AttributeModifier modifier(String id, double amount, AttributeModifier.Operation operation) {
      return new AttributeModifier(Identifier.fromNamespaceAndPath("samsara_test", id), amount, operation);
   }
}
