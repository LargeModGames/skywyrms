package com.pokato.skywyrms.anim;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Skeleton {
   private final Map<String, Bone> bones = new LinkedHashMap<>();
   private final Bone dummy = new Bone("__dummy__");

   public void add(Bone b) {
      this.bones.put(b.name, b);
   }

   public Bone get(String name) {
      Bone b = this.bones.get(name);
      if (b == null) {
         this.dummy.reset();
         return this.dummy;
      } else {
         return b;
      }
   }

   public boolean has(String name) {
      return this.bones.containsKey(name);
   }

   public void reset() {
      for (Bone b : this.bones.values()) {
         b.reset();
      }
   }

   public Iterable<Bone> bones() {
      return this.bones.values();
   }
}
