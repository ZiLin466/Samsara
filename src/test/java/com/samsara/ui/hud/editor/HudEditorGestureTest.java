package com.samsara.ui.hud.editor;

import com.google.gson.JsonParser;
import com.samsara.ui.hud.HudLayouts;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class HudEditorGestureTest {
   @Test void potionRowsDragAndScaleAsOneVerticallyCenteredGroup() {
      var layouts=new HudLayouts();var gesture=new HudEditorGesture(layouts);
      float width=80*com.samsara.module.visual.PotionStatus.PotionStatusPainter.DEFAULT_SCALE;
      float height=64.5f*com.samsara.module.visual.PotionStatus.PotionStatusPainter.DEFAULT_SCALE;
      var box=layouts.fit(HudLayouts.Element.POTION,width,height,400,300,false);
      layouts.drawn(HudLayouts.Element.POTION,box);
      assertEquals(150+layouts.get(HudLayouts.Element.POTION).y(),box.y()+box.height()/2,.001f);
      assertTrue(gesture.press(box.x()+10,box.y()+35,1,0));
      gesture.drag(110,90,1,400,300);
      var moved=layouts.fit(HudLayouts.Element.POTION,width,height,400,300,false);
      assertEquals(100,moved.x(),.001f);assertEquals(55,moved.y(),.001f);
      assertTrue(gesture.wheel(2,200));assertTrue(layouts.get(HudLayouts.Element.POTION).scale()>1);
      var saved=new HudLayouts();saved.load(layouts.snapshot());
      assertEquals(layouts.snapshot(),saved.snapshot());
      gesture.release(1);gesture.press(box.x()+10,box.y()+35,3,300);
      var reset=layouts.fit(HudLayouts.Element.POTION,width,height,400,300,false);
      assertEquals(box.width(),reset.width(),.001f);assertEquals(box.height(),reset.height(),.001f);
      assertEquals(moved.x(),reset.x(),.001f);assertEquals(moved.y(),reset.y(),.001f);
   }
   @Test void dragRetainsTheGrabOffsetAndCenteredCoordinates() {
      var layout = new HudLayouts(); var gesture = new HudEditorGesture(layout);
      layout.drawn(HudLayouts.Element.TARGET,new HudLayouts.Box(208,154,122,40));
      assertTrue(gesture.press(228,164,1,0));
      gesture.drag(130,90,1,400,300);
      assertEquals(-90,layout.get(HudLayouts.Element.TARGET).x());
      assertEquals(-70,layout.get(HudLayouts.Element.TARGET).y());
      assertTrue(gesture.release(1)); assertFalse(gesture.wheel(1,500));
   }
   @Test void fixedWidgetsCaptureResizeButNeverMoveAndRightClickOnlyResetsSize() {
      for (var element : new HudLayouts.Element[]{HudLayouts.Element.ISLAND,HudLayouts.Element.ARRAYLIST}) {
         var layout = new HudLayouts(); var gesture = new HudEditorGesture(layout);
         layout.drawn(element,new HudLayouts.Box(100,20,120,40));
         gesture.press(110,30,1,100); assertFalse(gesture.wheel(1,200));
         assertTrue(gesture.wheel(3,260)); assertTrue(layout.get(element).scale()>1);
         gesture.drag(300,200,1,400,300);
         assertEquals(0,layout.get(element).x()); assertEquals(0,layout.get(element).y());
         gesture.release(1); gesture.press(110,30,3,400);
         assertEquals(1,layout.get(element).scale()); assertNull(gesture.captured());
      }
   }
   @Test void persistedLayoutRoundTripsAndMalformedLayoutDoesNotPartlyApply() {
      var original=new HudLayouts(); original.get(HudLayouts.Element.INVENTORY).move(44,99);
      original.get(HudLayouts.Element.TARGET).scale(1.7);
      var copy=new HudLayouts(); copy.load(JsonParser.parseString(original.snapshot().toString()).getAsJsonObject());
      assertEquals(original.snapshot(),copy.snapshot());
      var malformed=original.snapshot(); malformed.getAsJsonObject("TARGET").remove("scale");
      var before=copy.snapshot(); assertThrows(RuntimeException.class,()->copy.load(malformed)); assertEquals(before,copy.snapshot());
      copy.get(HudLayouts.Element.TARGET).scale(99); assertEquals(2,copy.get(HudLayouts.Element.TARGET).scale());
   }
}
