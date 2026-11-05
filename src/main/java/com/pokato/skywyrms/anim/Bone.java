package com.pokato.skywyrms.anim;

public final class Bone {
   public final String name;
   public float x0;
   public float y0;
   public float z0;
   public float rx0;
   public float ry0;
   public float rz0;
   public float x;
   public float y;
   public float z;
   public float rx;
   public float ry;
   public float rz;
   public float sx = 1.0F;
   public float sy = 1.0F;
   public float sz = 1.0F;
   public boolean visible = true;

   public Bone(String name) {
      this.name = name;
   }

   public Bone(String name, float x0, float y0, float z0, float rx0, float ry0, float rz0) {
      this.name = name;
      this.x0 = x0;
      this.y0 = y0;
      this.z0 = z0;
      this.rx0 = rx0;
      this.ry0 = ry0;
      this.rz0 = rz0;
      this.reset();
   }

   public void reset() {
      this.x = this.x0;
      this.y = this.y0;
      this.z = this.z0;
      this.rx = this.rx0;
      this.ry = this.ry0;
      this.rz = this.rz0;
      this.sx = 1.0F;
      this.sy = 1.0F;
      this.sz = 1.0F;
      this.visible = true;
   }
}
