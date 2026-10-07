package com.samsara.ui.mainmenu.launch;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.Function;
import org.lwjgl.nanovg.NVGColor;
import static org.lwjgl.nanovg.NanoVG.*;

final class LaunchSphere {
   private final List<float[]> vertices = new ArrayList<>();
   private final List<int[]> faces = new ArrayList<>();
   private final List<int[]> edges = new ArrayList<>();
   private final NVGColor color = NVGColor.create();

   private float[][] projected;

   void load(Function<String, byte[]> resources) {
      if (vertices.isEmpty()) {
         String obj = new String(resources.apply("textures/launch/sphere.obj"), StandardCharsets.UTF_8);
         for (String line : obj.lines().toList()) {
            String[] parts = line.trim().split("\\s+");
            if (parts[0].equals("v")) vertices.add(new float[]{Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3])});
            if (parts[0].equals("f")) faces.add(new int[]{index(parts[1]), index(parts[2]), index(parts[3])});
         }
         // The OBJ repeats positions at UV seams. Weld them before drawing so strokes don't stack.
         var welded = new ArrayList<float[]>();
         var lookup = new HashMap<String, Integer>();
         int[] indices = new int[vertices.size()];
         for (int i = 0; i < vertices.size(); i++) {
            float[] vertex = vertices.get(i);
            String key = Math.round(vertex[0] * 1000) + ":" + Math.round(vertex[1] * 1000) + ":" + Math.round(vertex[2] * 1000);
            Integer existing = lookup.get(key);
            if (existing == null) { existing = welded.size(); lookup.put(key, existing); welded.add(vertex); }
            indices[i] = existing;
         }
         var unique = new HashSet<Long>();
         for (int[] face : faces) for (int i = 0; i < 3; i++) {
            int firstVertex = indices[face[i]], secondVertex = indices[face[(i + 1) % 3]];
            int lo = Math.min(firstVertex, secondVertex), hi = Math.max(firstVertex, secondVertex);
            if (unique.add((long)lo << 32 | hi)) edges.add(new int[]{lo, hi});
         }
         vertices.clear(); vertices.addAll(welded);
         projected = new float[vertices.size()][3];
      }
   }

   void draw(long vg, Function<String, byte[]> resources, double angle, int tint) {
      load(resources);
      double c = Math.cos(angle), s = Math.sin(angle);
      for (int i = 0; i < vertices.size(); i++) {
         float[] vertex = vertices.get(i);
         double x = vertex[0] * c + vertex[2] * s, z = -vertex[0] * s + vertex[2] * c;
         double y = vertex[1] * .96 + z * .28;
         float perspective = (float)(800 / (800 - z));
         projected[i][0] = (float)x * 2.45f * perspective;
         projected[i][1] = (float)y * 2.45f * perspective;
         projected[i][2] = (float)z;
      }
      color.r((tint >> 16 & 255) / 255f).g((tint >> 8 & 255) / 255f).b((tint & 255) / 255f);
      for (int[] edge : edges) {
         float[] edgeStart = projected[edge[0]], edgeEnd = projected[edge[1]];
         float glint = .72f + .28f * (float)Math.cos(edge[0] * 2.17 + edge[1] * .91 + angle * 17);
         color.a(glint * Math.clamp(.56f + (edgeStart[2] + edgeEnd[2]) / 420, .035f, 1));
         nvgBeginPath(vg); nvgMoveTo(vg, edgeStart[0], edgeStart[1]); nvgLineTo(vg, edgeEnd[0], edgeEnd[1]);
         nvgStrokeWidth(vg, 1.8f); nvgStrokeColor(vg, color); nvgStroke(vg);
      }
      for (float[] vertex : projected) if (vertex[2] > 25) {
         color.a(.55f);
         nvgBeginPath(vg); nvgCircle(vg, vertex[0], vertex[1], 2.4f); nvgFillColor(vg, color); nvgFill(vg);
      }
   }

   private static int index(String value) { return Integer.parseInt(value.split("/", 2)[0]) - 1; }
}
