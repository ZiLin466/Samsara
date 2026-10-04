package com.samsara.ui.hud.potion;

import com.samsara.module.visual.PotionStatus.PotionStatusData;
import com.samsara.module.visual.PotionStatus.PotionStatusMotion;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class PotionStatusTest {
   private static PotionStatusData.Effect effect(String key,int amplifier,int ticks) {
      return new PotionStatusData.Effect(key,PotionStatusData.englishName(key),0x32ABFF,ticks,amplifier,false);
   }
   @Test void labelsRemainEnglishAndUnreferencedIconsStayEmpty() {
      assertEquals("Jump Boost",effect("jump_boost",0,3200).title());
      assertEquals("Speed II",effect("speed",1,1780).title());
      assertEquals("Dolphin's Grace",PotionStatusData.englishName("dolphins_grace"));
      assertEquals("Bad Luck",PotionStatusData.englishName("unluck"));
      assertNull(effect("blindness",0,100).icon());
      assertNotNull(effect("slowness",0,100).icon());
      assertNull(effect("blindness",0,100).titleTexture());
      assertNull(effect("strength",1,100).titleTexture());
      assertNotNull(effect("speed",1,100).titleTexture());
      assertEquals("1:29",effect("speed",1,1780).timer());
      assertEquals("0:00",PotionStatusData.duration(-2,false));
      assertEquals("∞",PotionStatusData.duration(-1,true));
      assertEquals(0xFFFF5555,effect("poison",0,599).timerColor());
   }
   @Test void suppliedEffectsKeepEnglishNamesAndOnlyReferencedAmplifierMasks() {
      assertEquals("Absorption IV",effect("absorption",3,2300).title());
      assertEquals("Haste II",effect("haste",1,1160).title());
      assertEquals("Mining Fatigue III",effect("mining_fatigue",2,1160).title());
      for (String key:List.of("absorption","saturation","haste","mining_fatigue","nausea","resistance")) {
         assertNotNull(effect(key,0,1160).icon());
         assertNotNull(effect(key,0,1160).titleTexture());
         assertNotNull(effect(key,PotionStatusData.referenceAmplifier(key),1160).titleTexture());
         assertNull(effect(key,8,1160).titleTexture());
      }
   }
   @Test void unreferencedEffectsDoNotCreateRowsOrChangeGroupBounds() {
      var motion=new PotionStatusMotion();
      var known=effect("haste",1,1160);
      var unknown=List.of(effect("blindness",0,1160),effect("mod:custom_effect",0,1160));
      assertTrue(motion.update(unknown,0,s->20).rows().isEmpty());
      motion.update(List.of(known),10,s->20);
      var expected=motion.update(List.of(known),2000,s->20);
      var actual=motion.update(List.of(known,unknown.get(0),unknown.get(1)),2010,s->20);
      assertEquals(1,actual.rows().size());
      assertEquals(expected.width(),actual.width(),.001f);
      assertEquals(expected.height(),actual.height(),.001f);
   }
   @Test void everyRegisteredIconAndReferencedTitleHasAnActualTransparentMask() throws Exception {
      for (String key:PotionStatusData.ICONS) {
         for (String resource:List.of(effect(key,0,1160).icon(),effect(key,0,1160).titleTexture(),
               effect(key,PotionStatusData.referenceAmplifier(key),1160).titleTexture())) {
            try (var in=getClass().getResourceAsStream("/assets/samsara/"+resource)) {
               assertNotNull(in,resource);
               var image=javax.imageio.ImageIO.read(in);
               assertNotNull(image,resource);
               assertTrue(image.getColorModel().hasAlpha(),resource);
               boolean visible=false,transparent=false;
               for (int y=0;y<image.getHeight();y++) for (int x=0;x<image.getWidth();x++) {
                  int pixel=image.getRGB(x,y);
                  if ((pixel>>>24)==0) transparent=true;
                  else { visible=true;assertEquals(0xFFFFFF,pixel&0xFFFFFF,resource); }
               }
               assertTrue(visible && transparent,resource);
            }
         }
      }
   }
   @Test void refreshDoesNotRestartAndRemovalSettlesWithoutStaleRows() {
      var motion=new PotionStatusMotion();var effects=List.of(effect("speed",1,1780),effect("night_vision",0,9580));
      motion.update(effects,0,text->text.length()*3);
      var settled=motion.update(effects,2000,text->text.length()*3);
      assertEquals("night_vision",settled.rows().getFirst().effect().key());
      assertEquals(42.25f,settled.height(),.001f);
      assertEquals(22.25f,settled.rows().get(1).y()-settled.rows().getFirst().y(),.001f);
      var refreshed=motion.update(List.of(effect("speed",2,3600),effects.get(1)),2010,text->text.length()*3);
      assertEquals("Speed III",refreshed.rows().get(1).effect().title());
      assertEquals(0,refreshed.rows().get(1).x(),.01f);
      motion.update(List.of(),2020,text->text.length()*3);
      assertTrue(motion.update(List.of(),4020,text->text.length()*3).rows().isEmpty());
   }
   @Test void sameTimeHasTheSamePositionAtThirtySixtyAndOneFortyFourFps() {
      var effects=List.of(effect("speed",0,1200));
      var direct=new PotionStatusMotion();direct.update(effects,0,s->20);
      var expected=direct.update(effects,240,s->20);
      for (int fps:List.of(30,60,144)) {
         var sampled=new PotionStatusMotion();sampled.update(effects,0,s->20);
         for (long now=1;now<240;now+=1000/fps)sampled.update(effects,now,s->20);
         var actual=sampled.update(effects,240,s->20);
         assertEquals(expected.rows().getFirst().x(),actual.rows().getFirst().x(),.001f);
         assertEquals(expected.height(),actual.height(),.001f);
      }
   }
   @Test void backOutEntryOvershootsThenSettlesAndReversalIsContinuous() {
      var motion=new PotionStatusMotion();var effects=List.of(effect("night_vision",0,9600));
      assertEquals(20,motion.update(effects,0,s->34).height(),.001f);
      var near=motion.update(effects,250,s->34);
      assertTrue(near.rows().getFirst().x()>5 && near.rows().getFirst().x()<8);
      var startExit=motion.update(List.of(),250,s->34);
      assertEquals(near.rows().getFirst().x(),startExit.rows().getFirst().x(),.0001f);
      var exit=motion.update(List.of(),266,s->34);
      var reentered=motion.update(effects,266,s->34);
      assertEquals(exit.rows().getFirst().x(),reentered.rows().getFirst().x(),.0001f);
      assertEquals(0,motion.update(effects,1000,s->34).rows().getFirst().x(),.0001f);
   }
   @Test void finalEffectSlidesOutWithoutMovingOrShrinkingVertically() {
      var motion=new PotionStatusMotion();var effects=List.of(effect("night_vision",0,0));
      motion.update(effects,0,s->34);
      var settled=motion.update(effects,1000,s->34);
      motion.update(List.of(),1000,s->34);
      var exiting=motion.update(List.of(),1050,s->34);
      assertEquals(settled.height(),exiting.height(),.001f);
      assertEquals(settled.rows().getFirst().y(),exiting.rows().getFirst().y(),.001f);
      assertTrue(exiting.rows().getFirst().x()<-40);
      assertTrue(motion.update(List.of(),1150,s->34).rows().isEmpty());
   }
   @Test void groupExitFinishesBeforeSurvivorsRecenterAndDepartingRowsNeverOverlapThem() {
      var motion=new PotionStatusMotion();var night=effect("night_vision",0,1200);var speed=effect("speed",0,0);
      motion.update(List.of(night,speed),0,s->34);
      var settled=motion.update(List.of(night,speed),1000,s->34);
      motion.update(List.of(night),1000,s->34);
      var anticipation=motion.update(List.of(night),1080,s->34);
      assertEquals(0,anticipation.rows().get(1).x(),.001f);
      var leaving=motion.update(List.of(night),1135,s->34);
      assertTrue(leaving.rows().get(1).x()<-15);
      assertEquals(settled.rows().get(1).y(),leaving.rows().get(1).y(),.001f);
      var gone=motion.update(List.of(night),1200,s->34);
      assertEquals(1,gone.rows().size());
      assertEquals(settled.height(),gone.height(),.001f);
      assertEquals(settled.rows().getFirst().y(),gone.rows().getFirst().y(),.001f);
      var reflow=motion.update(List.of(night),1400,s->34);
      var finalFrame=motion.update(List.of(night),1600,s->34);
      assertEquals((settled.rows().getFirst().y()+finalFrame.rows().getFirst().y())/2,reflow.rows().getFirst().y(),.001f);
      assertEquals(20,finalFrame.height(),.001f);
   }
   @Test void exitsAndDelayedReflowAreIndependentOfFrameRateIncludingSkippedFrames() {
      var night=effect("night_vision",0,1200);var speed=effect("speed",0,0);
      for (boolean single:List.of(false,true)) {
         var initial=single?List.of(night):List.of(night,speed);
         var remaining=single?List.<PotionStatusData.Effect>of():List.of(night);
         for (long elapsed:List.of(60L,135L,360L,400L,700L)) {
            var direct=new PotionStatusMotion();direct.update(initial,0,s->34);direct.update(initial,1000,s->34);
            direct.update(remaining,1000,s->34);var expected=direct.update(remaining,1000+elapsed,s->34);
            for (int fps:List.of(30,60,144)) {
               var sampled=new PotionStatusMotion();sampled.update(initial,0,s->34);sampled.update(initial,1000,s->34);
               sampled.update(remaining,1000,s->34);
               for (long now=1001;now<1000+elapsed;now+=1000/fps) sampled.update(remaining,now,s->34);
               var actual=sampled.update(remaining,1000+elapsed,s->34);
               assertEquals(expected.rows().size(),actual.rows().size());
               assertEquals(expected.height(),actual.height(),.001f);
               for (int i=0;i<expected.rows().size();i++) {
                  assertEquals(expected.rows().get(i).x(),actual.rows().get(i).x(),.001f);
                  assertEquals(expected.rows().get(i).y(),actual.rows().get(i).y(),.001f);
               }
            }
         }
      }
   }
   @Test void reapplicationDuringExitOrReflowRetainsPositionAndEventuallyRestoresTheGroup() {
      var night=effect("night_vision",0,1200);var speed=effect("speed",0,0);
      for (long elapsed:List.of(70L,135L,380L)) {
         var motion=new PotionStatusMotion();motion.update(List.of(night,speed),0,s->34);
         motion.update(List.of(night,speed),1000,s->34);motion.update(List.of(night),1000,s->34);
         var before=motion.update(List.of(night),1000+elapsed,s->34);
         var after=motion.update(List.of(night,speed),1000+elapsed,s->34);
         assertEquals(before.height(),after.height(),.001f);
         assertEquals(before.rows().getFirst().y(),after.rows().getFirst().y(),.001f);
         if (before.rows().size()>1) assertEquals(before.rows().get(1).x(),after.rows().get(1).x(),.001f);
         var settled=motion.update(List.of(night,speed),4000,s->34);
         assertEquals(2,settled.rows().size());assertEquals(42.25,settled.height(),.001f);
         assertEquals(0,settled.rows().get(1).x(),.001f);
      }
   }
   @Test void finalCardKeepsItsAnchorEvenWhenAnOlderEmptySlotIsStillClosing() {
      var night=effect("night_vision",0,0);var speed=effect("speed",0,0);
      var motion=new PotionStatusMotion();motion.update(List.of(night,speed),0,s->20);
      motion.update(List.of(night,speed),1000,s->20);motion.update(List.of(night),1000,s->20);
      var before=motion.update(List.of(night),1400,s->20);
      motion.update(List.of(),1400,s->20);
      var exit=motion.update(List.of(),1460,s->20);
      assertEquals(before.rows().getFirst().y(),exit.rows().getFirst().y(),.001f);
      assertTrue(exit.height()<before.height());
      assertTrue(motion.update(List.of(),1600,s->20).rows().isEmpty());
   }
}
