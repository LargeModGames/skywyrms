package com.pokato.skywyrms.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pokato.skywyrms.Skywyrms;
import com.pokato.skywyrms.entity.DragonEntity;
import com.pokato.skywyrms.entity.DragonKind;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;

public class DragonRenderer extends MobRenderer<DragonEntity, DragonRenderState, DragonModel> {
   private final DragonKind kind;
   private final Identifier[] textures = new Identifier[DragonKind.values().length];

   public DragonRenderer(Context context, DragonKind kind) {
      super(context, new DragonModel(context.bakeLayer(ModModelLayers.dragon(kind)), kind.geo()), 1.5F * kind.scale());
      this.kind = kind;

      for (DragonKind blood : DragonKind.values()) {
         this.textures[blood.ordinal()] = texture(kind, blood, false);
      }

      this.addLayer(new DragonGlowLayer(this, context, kind));
   }

   public static Identifier texture(DragonKind body, DragonKind blood, boolean glow) {
      String suffix = glow ? "_glow.png" : ".png";
      return body == blood
         ? Skywyrms.id("textures/entity/" + body.id() + suffix)
         : Skywyrms.id("textures/entity/hybrid/" + body.element() + "_" + blood.element() + suffix);
   }

   public DragonRenderState createRenderState() {
      return new DragonRenderState();
   }

   public void extractRenderState(DragonEntity entity, DragonRenderState state, float partialTick) {
      super.extractRenderState(entity, state, partialTick);
      state.kind = this.kind;
      state.blood = entity.blood();
      entity.anim.fill(state.anim, entity, partialTick);
   }

   public Identifier getTextureLocation(DragonRenderState state) {
      return this.textures[state.blood.ordinal()];
   }

   protected void scale(DragonRenderState state, PoseStack poseStack) {
      float scale = this.kind.scale() * state.ageScale;
      poseStack.scale(scale, scale, scale);
   }

   protected AABB getBoundingBoxForCulling(DragonEntity entity, float partialTick) {
      return entity.renderBounds();
   }
}
