package com.pokato.skywyrms.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pokato.skywyrms.Skywyrms;
import com.pokato.skywyrms.entity.DragonKind;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class EggGlowLayer extends EyesLayer<EggRenderState, EggModel> {
   private final RenderType[] renderTypes = new RenderType[DragonKind.values().length * 3];
   private int current;

   public EggGlowLayer(RenderLayerParent<EggRenderState, EggModel> parent) {
      super(parent);

      for (DragonKind blood : DragonKind.values()) {
         for (int stage = 0; stage < 3; stage++) {
            Identifier texture = Skywyrms.id("textures/entity/egg/glow_" + blood.element() + "_" + stage + ".png");
            this.renderTypes[blood.ordinal() * 3 + stage] = RenderTypes.eyes(texture);
         }
      }
   }

   @Override
   public RenderType renderType() {
      return this.renderTypes[this.current];
   }

   public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, EggRenderState state, float yRot, float xRot) {
      this.current = state.blood.ordinal() * 3 + state.stage;
      super.submit(poseStack, collector, light, state, yRot, xRot);
   }
}
