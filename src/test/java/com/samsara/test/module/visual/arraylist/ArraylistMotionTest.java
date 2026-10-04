package com.samsara.test.module.visual.arraylist;

import com.samsara.module.visual.Hud.ArraylistMotion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class ArraylistMotionTest {
   @Test void entryAndGapAnimateAndReverseContinuously() {
      var motion=new ArraylistMotion();motion.visibility(false,0);assertFalse(motion.visible());
      motion.visibility(true,100);assertEquals(0,motion.progress());
      motion.visibility(true,200);assertTrue(motion.progress()>0 && motion.progress()<1);
      float before=motion.progress(); motion.visibility(false,200);assertEquals(before,motion.progress());
      motion.visibility(false,350);float reversed=motion.progress();motion.visibility(true,350);assertEquals(reversed,motion.progress());
      motion.visibility(true,1500);assertEquals(1,motion.progress(),.001);
      motion.visibility(false,1500);for(int t=1510;t<2600;t+=10)motion.visibility(false,t);assertFalse(motion.visible());
   }
   @Test void analyticSpringIsIndependentOfSamplingFrequency() {
      for(int fps:new int[]{30,60,144}) {
         var sampled=new ArraylistMotion.Spring(0,25); sampled.to(1,0);
         for(int i=1;i<fps;i++)sampled.to(1,Math.round(i*1000.0/fps));
         var sparse=new ArraylistMotion.Spring(0,25);sparse.to(1,0);
         assertEquals(sparse.to(1,1000),sampled.to(1,1000),1e-10);
         double atReverse=sampled.to(0,1000);assertEquals(atReverse,sampled.to(0,1000));
      }
   }
}
