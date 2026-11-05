package com.pokato.skywyrms.entity;

import com.pokato.skywyrms.anim.AnimState;
import com.pokato.skywyrms.anim.Bone;
import com.pokato.skywyrms.anim.DragonAnimator;
import com.pokato.skywyrms.anim.Skeleton;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class DragonRig {
   private final GeoData geo;
   private final Skeleton skeleton = new Skeleton();
   private final List<Bone> mouthChain = new ArrayList<>();
   private final AnimState state = new AnimState();
   private final Matrix4f matrix = new Matrix4f();
   private final Vector3f point = new Vector3f();

   public DragonRig(GeoData geo) {
      this.geo = geo;

      for (GeoData.Part part : geo.parts) {
         this.skeleton.add(new Bone(part.name, part.px, part.py, part.pz, part.rx, part.ry, part.rz));
      }

      List<Bone> reversed = new ArrayList<>();

      for (GeoData.Part part = geo.byName.get(geo.mouthBone); part != null; part = part.parent == null ? null : geo.byName.get(part.parent)) {
         reversed.add(this.skeleton.get(part.name));
      }

      for (int i = reversed.size() - 1; i >= 0; i--) {
         this.mouthChain.add(reversed.get(i));
      }
   }

   public Vec3 mouth(DragonEntity dragon) {
      dragon.anim.fill(this.state, dragon, 1.0F);
      DragonAnimator.animate(this.skeleton, this.state);
      Matrix4f m = this.matrix.identity();

      for (Bone bone : this.mouthChain) {
         m.translate(bone.x / 16.0F, bone.y / 16.0F, bone.z / 16.0F);
         if (bone.rx != 0.0F || bone.ry != 0.0F || bone.rz != 0.0F) {
            m.rotateZYX(bone.rz, bone.ry, bone.rx);
         }

         if (bone.sx != 1.0F || bone.sy != 1.0F || bone.sz != 1.0F) {
            m.scale(bone.sx, bone.sy, bone.sz);
         }
      }

      Vector3f p = m.transformPosition(this.point.set(this.geo.mouthX / 16.0F, this.geo.mouthY / 16.0F, this.geo.mouthZ / 16.0F));
      float s = this.geo.scale;
      double x = -p.x * s;
      double y = -(p.y - 1.501F) * s;
      double z = p.z * s;
      double angle = Math.toRadians(180.0F - dragon.yBodyRot);
      double cos = Math.cos(angle);
      double sin = Math.sin(angle);
      return new Vec3(dragon.getX() + x * cos + z * sin, dragon.getY() + y, dragon.getZ() - x * sin + z * cos);
   }
}
