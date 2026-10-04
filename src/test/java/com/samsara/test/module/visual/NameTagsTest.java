package com.samsara.test.module.visual;

import com.samsara.module.visual.NameTags.Geometry;
import com.samsara.module.visual.NameTags.Painter;
import com.samsara.module.visual.NameTags.Part;
import com.samsara.module.visual.NameTags.View;
import com.samsara.util.render.GuiItemOpacity;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.TeamColor;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class NameTagsTest {
   @Test void teamPrefixNameAndSuffixKeepIndependentColors() {
      var team = new PlayerTeam(new Scoreboard(), "blue");
      team.setColor(Optional.of(TeamColor.BLUE));
      team.setPlayerPrefix(Component.literal("[B] ").withStyle(ChatFormatting.AQUA));
      team.setPlayerSuffix(Component.literal(" Vanilla").withStyle(ChatFormatting.GREEN));
      Component result = Geometry.teamName(Component.literal("A2tk"), team);
      assertEquals("[B] A2tk Vanilla", result.getString());
      List<String> runs = new ArrayList<>();
      result.visit((style, text) -> {
         if (!text.isEmpty()) runs.add(text + ":" + style.getColor().getValue());
         return Optional.empty();
      }, net.minecraft.network.chat.Style.EMPTY);
      assertTrue(runs.contains("[B] :" + net.minecraft.network.chat.TextColor.fromLegacyFormat(ChatFormatting.AQUA).getValue()));
      assertTrue(runs.contains("A2tk:" + net.minecraft.network.chat.TextColor.fromLegacyFormat(ChatFormatting.BLUE).getValue()));
      assertTrue(runs.contains(" Vanilla:" + net.minecraft.network.chat.TextColor.fromLegacyFormat(ChatFormatting.GREEN).getValue()));
   }

   @Test void hologramLineBreaksKeepTheirFormatting() {
      Component text = Component.literal("FastBreak\n").withStyle(ChatFormatting.GOLD)
         .append(Component.literal("CivBreak").withStyle(ChatFormatting.RED));
      List<Component> lines = Geometry.lines(text);
      assertEquals(List.of("FastBreak", "CivBreak"), lines.stream().map(Component::getString).toList());
      int[] color = {0};
      lines.get(1).visit((style, fragment) -> {
         if (!fragment.isEmpty()) color[0] = style.getColor().getValue();
         return Optional.empty();
      }, net.minecraft.network.chat.Style.EMPTY);
      assertEquals(net.minecraft.network.chat.TextColor.fromLegacyFormat(ChatFormatting.RED).getValue(), color[0]);
   }

   @Test void armorComparisonAndHealthObjectivesUseServerSemantics() {
      assertEquals("-20", Geometry.armorPart(0, 20).text().getString());
      assertEquals(Geometry.RED, Geometry.armorPart(0, 20).color());
      assertEquals("=", Geometry.armorPart(0, 0).text().getString());
      assertEquals("=", Geometry.armorPart(20, 20).text().getString());
      assertEquals("12", Geometry.armorPart(20, 8).text().getString());
      assertEquals(Geometry.GREEN, Geometry.armorPart(20, 8).color());
      for (String name : List.of("❤", "HP", "Health", "Здоровья", "Здоровье")) assertTrue(Geometry.isHealthObjective(name));
      assertFalse(Geometry.isHealthObjective("Kills"));
      assertEquals(24, Geometry.health(20, 4, null));
      assertEquals(31, Geometry.health(20, 4, 31));
      assertEquals(0, Geometry.health(20, 4, 0));
      assertEquals(0, Geometry.health(-1, 0, null));
   }

   @Test void requiredMixinTargetsExistInTheCurrentMinecraftVersion() throws Exception {
      Class<?> pose = com.mojang.blaze3d.vertex.PoseStack.class;
      Class<?> collector = net.minecraft.client.renderer.SubmitNodeCollector.class;
      Class<?> entityState = net.minecraft.client.renderer.entity.state.EntityRenderState.class;
      var renderer = net.minecraft.client.renderer.entity.EntityRenderer.class;
      assertNotNull(renderer.getDeclaredMethod("submitNameDisplay", entityState, pose, collector,
         net.minecraft.client.renderer.state.level.CameraRenderState.class, int.class));
      assertNotNull(renderer.getDeclaredMethod("extractRenderState", net.minecraft.world.entity.Entity.class, entityState, float.class));
      assertNotNull(net.minecraft.client.renderer.entity.DisplayRenderer.TextDisplayRenderer.class.getDeclaredMethod("submitInner",
         net.minecraft.client.renderer.entity.state.TextDisplayEntityRenderState.class, pose, collector, int.class, float.class));
      assertNotNull(net.minecraft.client.gui.render.GuiRenderer.class.getDeclaredMethod("submitBlitFromItemAtlas",
         net.minecraft.client.renderer.state.gui.GuiItemRenderState.class, net.minecraft.client.gui.render.GuiItemAtlas.SlotView.class));
      assertNotNull(net.minecraft.client.gui.render.pip.PictureInPictureRenderer.class.getDeclaredMethod("blitTexture",
         net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState.class, net.minecraft.client.renderer.state.gui.GuiRenderState.class));
      var constructor = net.minecraft.client.renderer.state.gui.BlitRenderState.class.getConstructor(
         com.mojang.renderpearl.api.pipeline.RenderPipeline.class, net.minecraft.client.gui.render.TextureSetup.class,
         org.joml.Matrix3x2fc.class, int.class, int.class, int.class, int.class, float.class, float.class, float.class,
         float.class, int.class, net.minecraft.client.gui.navigation.ScreenRectangle.class, net.minecraft.client.gui.navigation.ScreenRectangle.class);
      assertEquals(int.class, constructor.getParameterTypes()[11]);
   }

   @Test void fadingIsContinuousSymmetricAndIndependentOfResolution() {
      assertEquals(1, Geometry.opacity(960, 1920));
      assertEquals(1, Geometry.opacity(240, 1920));
      assertEquals(1, Geometry.opacity(1680, 1920));
      assertEquals(0, Geometry.opacity(1920, 1920));
      assertEquals(0, Geometry.opacity(Float.NaN, 1920));
      assertEquals(0, Geometry.opacity(960, 0));
      float previous = 1;
      for (int x = 960; x <= 1920; x++) {
         float alpha = Geometry.opacity(x, 1920);
         assertTrue(alpha <= previous + .000001f);
         assertTrue(previous - alpha < .0065);
         assertEquals(alpha, Geometry.opacity(1920 - x, 1920), .000001);
         assertEquals(alpha, Geometry.opacity(x / 2f, 960), .000001);
         previous = alpha;
      }
   }

   @Test void removingArmorCapsuleClosesItsGapAndKeepsTheRowCentered() {
      Part health = new Part(Component.literal("20"), Geometry.GREEN, true);
      Part name = new Part(Component.literal("A2tk Vanilla"), -1, false);
      Part distance = new Part(Component.literal("6m"), Geometry.DISTANCE, false);
      var complete = Geometry.layout(List.of(health, name, Geometry.armorPart(0, 20), distance), text -> text.getString().length() * 7);
      var reduced = Geometry.layout(List.of(health, name, distance), text -> text.getString().length() * 7);
      assertEquals(complete.width() - complete.capsules().get(2).width() - Geometry.GAP, reduced.width());
      Recording surface = new Recording();
      Painter.paint(surface, new View(List.of(health, name, distance), List.of(), null, true), reduced);
      assertEquals(-reduced.width() / 2, surface.panels.getFirst()[0]);
      float[] last = surface.panels.getLast();
      assertEquals(reduced.width() / 2, last[0] + last[2]);
      assertEquals(3, surface.panels.size());
   }

   @Test void itemOpacityScopesRestoreAndPremultiplyAllChannels() {
      assertEquals(1, GuiItemOpacity.current());
      try (var outer = GuiItemOpacity.extract(.5f)) {
         assertEquals(.5f, GuiItemOpacity.current());
         try (var inner = GuiItemOpacity.extract(.1f)) { assertEquals(.1f, GuiItemOpacity.current()); }
         assertEquals(.5f, GuiItemOpacity.current());
         assertEquals(0x80808080, GuiItemOpacity.premultipliedTint(GuiItemOpacity.current()));
      }
      assertEquals(1, GuiItemOpacity.current());
      assertEquals(0xFFFFFFFF, GuiItemOpacity.premultipliedTint(1));
      assertEquals(0, GuiItemOpacity.premultipliedTint(0));
   }

   private static final class Recording implements Painter.Surface {
      final List<float[]> panels = new ArrayList<>();
      public float measure(Component text) { return text.getString().length() * 7; }
      public void panel(float x, float y, float width, float height) { panels.add(new float[]{x, y, width, height}); }
      public void text(Component text, float x, float y, int color) { }
      public void heart(float x, float y, int color) { }
      public void item(ItemStack item, float x, float y, int index) { }
      public void score(Component text, float x, float y) { }
   }
}
