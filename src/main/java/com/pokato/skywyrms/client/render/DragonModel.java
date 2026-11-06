package com.pokato.skywyrms.client.render;

import com.pokato.skywyrms.anim.Bone;
import com.pokato.skywyrms.anim.DragonAnimator;
import com.pokato.skywyrms.anim.Skeleton;
import com.pokato.skywyrms.entity.DragonKind;
import com.pokato.skywyrms.entity.GeoData;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.core.Direction;

public class DragonModel extends EntityModel<DragonRenderState> {
   private final Skeleton skeleton = new Skeleton();
   private final ModelPart[] parts;
   private final Bone[] bones;
   private static final float SHEET_GAP = 0.1F;

   public DragonModel(ModelPart root, GeoData geo) {
      super(root);
      int count = geo.parts.size();
      this.parts = new ModelPart[count];
      this.bones = new Bone[count];
      Map<String, ModelPart> byName = new HashMap<>();

      for (int i = 0; i < count; i++) {
         GeoData.Part part = geo.parts.get(i);
         ModelPart parent = part.parent == null ? root : byName.get(part.parent);
         ModelPart modelPart = parent.getChild(part.name);
         byName.put(part.name, modelPart);
         Bone bone = new Bone(part.name, part.px, part.py, part.pz, part.rx, part.ry, part.rz);
         this.skeleton.add(bone);
         this.parts[i] = modelPart;
         this.bones[i] = bone;
      }

      this.neck = byName.get("neck1");
   }

   /** Root of the neck chain (neck, head, jaw and their decorations), hidden in the rider's first-person view. */
   private ModelPart neck;

   /** Every part of the kind's model, with only the cubes that pass the filter. */
   public static LayerDefinition createLayer(DragonKind kind, Predicate<GeoData.Cube> keep) {
      GeoData geo = kind.geo();
      MeshDefinition mesh = new MeshDefinition();
      Map<String, PartDefinition> definitions = new HashMap<>();

      for (GeoData.Part part : geo.parts) {
         CubeListBuilder cubes = CubeListBuilder.create();

         for (GeoData.Cube cube : part.cubes) {
            if (!keep.test(cube)) {
               continue;
            }

            cubes.texOffs(cube.u, cube.v).mirror(cube.mirror);
            if (cube.inflate != 0.0F || cube.w != 0.0F && cube.h != 0.0F && cube.d != 0.0F) {
               cubes.addBox(cube.x, cube.y, cube.z, cube.w, cube.h, cube.d, new CubeDeformation(cube.inflate));
            } else {
               addSheet(cubes, cube);
            }
         }

         PartDefinition parent = part.parent == null ? mesh.getRoot() : definitions.get(part.parent);
         definitions.put(
            part.name, parent.addOrReplaceChild(part.name, cubes, PartPose.offsetAndRotation(part.px, part.py, part.pz, part.rx, part.ry, part.rz))
         );
      }

      return LayerDefinition.create(mesh, geo.textureWidth, geo.textureHeight);
   }

   private static void addSheet(CubeListBuilder cubes, GeoData.Cube c) {
      float e = 0.05F;
      if (c.h == 0.0F) {
         cubes.addBox(c.x, c.y - e, c.z, c.w, 0.0F, c.d, EnumSet.of(Direction.DOWN));
         cubes.addBox(c.x, c.y + e, c.z, c.w, 0.0F, c.d, EnumSet.of(Direction.UP));
      } else if (c.w == 0.0F) {
         float s = c.mirror ? -e : e;
         cubes.addBox(c.x - s, c.y, c.z, 0.0F, c.h, c.d, EnumSet.of(Direction.WEST));
         cubes.addBox(c.x + s, c.y, c.z, 0.0F, c.h, c.d, EnumSet.of(Direction.EAST));
      } else {
         cubes.addBox(c.x, c.y, c.z - e, c.w, c.h, 0.0F, EnumSet.of(Direction.NORTH));
         cubes.addBox(c.x, c.y, c.z + e, c.w, c.h, 0.0F, EnumSet.of(Direction.SOUTH));
      }
   }

   public void setupAnim(DragonRenderState state) {
      super.setupAnim(state);
      this.pose(state);
   }

   private void pose(DragonRenderState state) {
      DragonAnimator.animate(this.skeleton, state.anim);

      for (int i = 0; i < this.parts.length; i++) {
         ModelPart part = this.parts[i];
         Bone bone = this.bones[i];
         part.x = bone.x;
         part.y = bone.y;
         part.z = bone.z;
         part.xRot = bone.rx;
         part.yRot = bone.ry;
         part.zRot = bone.rz;
         part.xScale = bone.sx;
         part.yScale = bone.sy;
         part.zScale = bone.sz;
         part.visible = bone.visible;
      }

      if (state.riderView && this.neck != null) {
         this.neck.visible = false;
      }
   }
}
