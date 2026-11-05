package com.pokato.skywyrms.client.render;

import com.pokato.skywyrms.Skywyrms;
import com.pokato.skywyrms.entity.DragonEggEntity;
import com.pokato.skywyrms.entity.DragonKind;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.Identifier;

public class EggRenderer extends MobRenderer<DragonEggEntity, EggRenderState, EggModel> {
   private static final int KINDS = DragonKind.values().length;
   private final Identifier[] textures = new Identifier[KINDS * KINDS];

   public EggRenderer(Context context) {
      super(context, new EggModel(context.bakeLayer(ModModelLayers.EGG)), 0.32F);

      for (DragonKind body : DragonKind.values()) {
         for (DragonKind blood : DragonKind.values()) {
            this.textures[body.ordinal() * KINDS + blood.ordinal()] = Skywyrms.id("textures/entity/egg/" + body.element() + "_" + blood.element() + ".png");
         }
      }

      this.addLayer(new EggGlowLayer(this));
   }

   public EggRenderState createRenderState() {
      return new EggRenderState();
   }

   public void extractRenderState(DragonEggEntity egg, EggRenderState state, float partialTick) {
      super.extractRenderState(egg, state, partialTick);
      fill(egg, state, partialTick);
   }

   public Identifier getTextureLocation(EggRenderState state) {
      return this.textures[state.body.ordinal() * KINDS + state.blood.ordinal()];
   }

   private static void fill(DragonEggEntity egg, EggRenderState state, float partialTick) {
      state.body = egg.body();
      state.blood = egg.blood();
      float progress = egg.progressFraction();
      state.stage = progress >= 0.9F ? 2 : (progress >= 0.75F ? 1 : 0);
      float t = egg.wobbleTicks > 0 ? Math.max(0.0F, egg.wobbleTicks - partialTick) : 0.0F;
      state.wobble = t <= 0.0F ? 0.0F : egg.wobbleStrength * 0.28F * (t / 24.0F) * (float)Math.sin(t * 1.25F);
      state.wobblePhase = egg.getId() * 3.1F;
   }
}
