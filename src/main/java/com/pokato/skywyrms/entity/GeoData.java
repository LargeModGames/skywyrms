package com.pokato.skywyrms.entity;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GeoData {
   private static final Map<DragonKind, GeoData> CACHE = new EnumMap<>(DragonKind.class);
   public final DragonKind kind;
   public final int textureWidth;
   public final int textureHeight;
   public final float scale;
   public final float seatX;
   public final float seatY;
   public final float seatZ;
   public final float bboxWidth;
   public final float bboxHeight;
   public final String mouthBone;
   public final float mouthX;
   public final float mouthY;
   public final float mouthZ;
   public final float sitDrop;
   public final List<GeoData.Part> parts = new ArrayList<>();
   public final Map<String, GeoData.Part> byName = new LinkedHashMap<>();

   private GeoData(DragonKind kind, JsonObject json) {
      this.kind = kind;
      JsonArray tex = json.getAsJsonArray("texture_size");
      this.textureWidth = tex.get(0).getAsInt();
      this.textureHeight = tex.get(1).getAsInt();
      this.scale = json.get("scale").getAsFloat();
      float[] seat = floats(json.getAsJsonArray("seat"));
      this.seatX = seat[0];
      this.seatY = seat[1];
      this.seatZ = seat[2];
      float[] bbox = floats(json.getAsJsonArray("bbox"));
      this.bboxWidth = bbox[0];
      this.bboxHeight = bbox[1];
      this.mouthBone = json.get("mouth_bone").getAsString();
      float[] mouth = floats(json.getAsJsonArray("mouth_local"));
      this.mouthX = mouth[0];
      this.mouthY = mouth[1];
      this.mouthZ = mouth[2];
      this.sitDrop = json.has("sit_drop") ? json.get("sit_drop").getAsFloat() : 14.0F;

      for (JsonElement element : json.getAsJsonArray("parts")) {
         JsonObject p = element.getAsJsonObject();
         GeoData.Part part = new GeoData.Part();
         part.name = p.get("name").getAsString();
         part.parent = p.get("parent").isJsonNull() ? null : p.get("parent").getAsString();
         float[] pivot = floats(p.getAsJsonArray("pivot"));
         part.px = pivot[0];
         part.py = pivot[1];
         part.pz = pivot[2];
         float[] rot = floats(p.getAsJsonArray("rotation"));
         part.rx = (float)Math.toRadians(rot[0]);
         part.ry = (float)Math.toRadians(rot[1]);
         part.rz = (float)Math.toRadians(rot[2]);

         for (JsonElement ce : p.getAsJsonArray("cubes")) {
            JsonObject c = ce.getAsJsonObject();
            GeoData.Cube cube = new GeoData.Cube();
            float[] origin = floats(c.getAsJsonArray("origin"));
            float[] size = floats(c.getAsJsonArray("size"));
            JsonArray uv = c.getAsJsonArray("uv");
            cube.x = origin[0];
            cube.y = origin[1];
            cube.z = origin[2];
            cube.w = size[0];
            cube.h = size[1];
            cube.d = size[2];
            cube.u = uv.get(0).getAsInt();
            cube.v = uv.get(1).getAsInt();
            cube.inflate = c.has("inflate") ? c.get("inflate").getAsFloat() : 0.0F;
            cube.mirror = c.has("mirror") && c.get("mirror").getAsBoolean();
            part.cubes.add(cube);
         }

         if (part.parent != null && !this.byName.containsKey(part.parent)) {
            throw new IllegalStateException("geo " + kind.id() + ": part " + part.name + " appears before its parent " + part.parent);
         }

         this.parts.add(part);
         this.byName.put(part.name, part);
      }
   }

   private static float[] floats(JsonArray array) {
      float[] out = new float[array.size()];

      for (int i = 0; i < out.length; i++) {
         out[i] = array.get(i).getAsFloat();
      }

      return out;
   }

   public static synchronized GeoData get(DragonKind kind) {
      GeoData data = CACHE.get(kind);
      if (data == null) {
         String path = "/assets/skywyrms/dragon_geo/" + kind.id() + ".json";

         try (InputStream in = GeoData.class.getResourceAsStream(path)) {
            if (in == null) {
               throw new IllegalStateException("missing " + path);
            }

            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
               data = new GeoData(kind, JsonParser.parseReader(reader).getAsJsonObject());
            }
         } catch (IOException e) {
            throw new IllegalStateException("cannot read " + path, e);
         }

         CACHE.put(kind, data);
      }

      return data;
   }

   public float seatHeight() {
      return (24.0F - this.seatY) / 16.0F * this.scale;
   }

   public float hitboxWidth() {
      return this.bboxWidth * this.scale;
   }

   public float hitboxHeight() {
      return this.bboxHeight * this.scale;
   }

   public static final class Cube {
      public float x;
      public float y;
      public float z;
      public float w;
      public float h;
      public float d;
      public float inflate;
      public int u;
      public int v;
      public boolean mirror;
   }

   public static final class Part {
      public String name;
      public String parent;
      public float px;
      public float py;
      public float pz;
      public float rx;
      public float ry;
      public float rz;
      public final List<GeoData.Cube> cubes = new ArrayList<>();
   }
}
