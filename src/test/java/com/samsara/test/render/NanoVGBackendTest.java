package com.samsara.test.render;

import com.samsara.util.render.NanoVGBackend;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class NanoVGBackendTest {
   private static float[] triangles(List<float[]> contours) {
      try {
         var method=MethodHandles.privateLookupIn(NanoVGBackend.class,MethodHandles.lookup())
            .findStatic(NanoVGBackend.class,"tessellate",MethodType.methodType(float[].class,List.class));
         return (float[])method.invokeExact(contours);
      } catch (Throwable error) { throw new AssertionError(error); }
   }

   @Test void holesAndSeparateContoursKeepTheirArea() {
      var contours=List.of(new float[]{0,0,10,0,10,10,0,10},new float[]{2,2,2,8,8,8,8,2},
         new float[]{12,0,14,0,14,2,12,2});
      assertEquals(68,area(triangles(contours)),.0001);
      assertCoverage(contours);
   }

   @Test void overlappingContoursUseNonzeroWindingWithoutDoubleCoverage() {
      var contours=List.of(new float[]{0,0,4,0,4,4,0,4},new float[]{2,2,6,2,6,6,2,6});
      assertEquals(28,area(triangles(contours)),.0001);
      assertCoverage(contours);
   }

   @Test void selfIntersectionsSplitAtTheirCrossing() {
      var contours=List.of(new float[]{0,0,6,6,0,6,6,0});
      assertEquals(18,area(triangles(contours)),.0001);
      assertCoverage(contours);
   }

   @Test void concaveAndReversedContoursKeepCoverage() {
      var contours=List.of(new float[]{0,0,0,6,2,6,2,2,6,2,6,0});
      assertEquals(20,area(triangles(contours)),.0001);
      assertCoverage(contours);
      assertEquals(0,triangles(List.of(new float[]{0,0,1,0,2,0})).length);
   }

   private static double area(float[] vertices) {
      double total=0;
      for (int i=0;i<vertices.length;i+=12)
         total+=Math.abs(cross(vertices[i+4]-vertices[i],vertices[i+5]-vertices[i+1],
            vertices[i+8]-vertices[i],vertices[i+9]-vertices[i+1]))/2;
      return total;
   }

   private static void assertCoverage(List<float[]> contours) {
      float[] vertices=triangles(contours);
      for (double y=-.43;y<11;y+=.37) for (double x=-.27;x<15;x+=.41) {
         int winding=0;
         for (float[] contour:contours) for (int i=0;i<contour.length;i+=2) {
            int next=(i+2)%contour.length;
            double ax=contour[i],ay=contour[i+1],bx=contour[next],by=contour[next+1];
            double side=cross(bx-ax,by-ay,x-ax,y-ay);
            if (ay<=y && by>y && side>0) winding++;
            if (ay>y && by<=y && side<0) winding--;
         }
         int coverage=0;
         for (int i=0;i<vertices.length;i+=12) {
            double firstEdgeCross=cross(vertices[i+4]-vertices[i],vertices[i+5]-vertices[i+1],x-vertices[i],y-vertices[i+1]);
            double secondEdgeCross=cross(vertices[i+8]-vertices[i+4],vertices[i+9]-vertices[i+5],x-vertices[i+4],y-vertices[i+5]);
            double c=cross(vertices[i]-vertices[i+8],vertices[i+1]-vertices[i+9],x-vertices[i+8],y-vertices[i+9]);
            if ((firstEdgeCross>0 && secondEdgeCross>0 && c>0) || (firstEdgeCross<0 && secondEdgeCross<0 && c<0)) coverage++;
         }
         assertEquals(winding==0 ? 0 : 1,coverage,"Coverage at "+x+", "+y);
      }
   }
   private static double cross(double ax,double ay,double bx,double by) { return ax*by-ay*bx; }
}
