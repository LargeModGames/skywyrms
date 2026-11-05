package com.pokato.skywyrms.client.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public class EggModel extends EntityModel<EggRenderState> {
   public static final int[][] RINGS = new int[][]{
      {0, 1, 6, 36, 14}, {1, 2, 8, 32, 26}, {2, 6, 10, 0, 0}, {6, 9, 9, 0, 14}, {9, 11, 8, 0, 26}, {11, 12, 7, 0, 36}, {12, 13, 5, 40, 4}, {13, 14, 3, 40, 0}
   };
   private final ModelPart egg;

   public EggModel(ModelPart root) {
      super(root);
      this.egg = root.getChild("egg");
   }

   public static LayerDefinition createLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      CubeListBuilder cubes = CubeListBuilder.create();

      for (int[] ring : RINGS) {
         float w = ring[2];
         cubes.texOffs(ring[3], ring[4]).addBox(-w / 2.0F, -ring[1], -w / 2.0F, w, ring[1] - ring[0], w);
      }

      root.addOrReplaceChild("egg", cubes, PartPose.offset(0.0F, 24.0F, 0.0F));
      return LayerDefinition.create(mesh, 64, 64);
   }

   public void setupAnim(EggRenderState state) {
      super.setupAnim(state);
      this.pose(state);
   }

   private void pose(EggRenderState state) {
      this.egg.zRot = state.wobble * (float)Math.cos(state.wobblePhase * 0.37F);
      this.egg.xRot = state.wobble * (float)Math.sin(state.wobblePhase * 0.37F) * 0.6F;
   }
}
