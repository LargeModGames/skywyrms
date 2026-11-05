package com.pokato.skywyrms.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pokato.skywyrms.entity.DragonKind;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;

public class DragonGlowLayer extends EyesLayer<DragonRenderState, DragonModel> {
   private final RenderType[] renderTypes = new RenderType[DragonKind.values().length];
   private DragonKind current;

   public DragonGlowLayer(RenderLayerParent<DragonRenderState, DragonModel> parent, DragonKind kind) {
      super(parent);
      this.current = kind;

      for (DragonKind blood : DragonKind.values()) {
         this.renderTypes[blood.ordinal()] = RenderTypes.eyes(DragonRenderer.texture(kind, blood, true));
      }
   }

   @Override
   public RenderType renderType() {
      return this.renderTypes[this.current.ordinal()];
   }

   public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, DragonRenderState state, float yRot, float xRot) {
      this.current = state.blood;
      super.submit(poseStack, collector, light, state, yRot, xRot);
   }
}
