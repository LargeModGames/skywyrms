package com.pokato.skywyrms.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pokato.skywyrms.Skywyrms;
import com.pokato.skywyrms.ability.DragonAbilities;
import com.pokato.skywyrms.entity.DragonEntity;
import com.pokato.skywyrms.entity.DragonKind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

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
      Minecraft minecraft = Minecraft.getInstance();
      state.riderView = minecraft.player != null && entity.hasPassenger(minecraft.player) && minecraft.options.getCameraType().isFirstPerson();
      entity.anim.fill(state.anim, entity, partialTick);
      state.beam = this.kind.ability() == DragonKind.Ability.SUN_BEAM && entity.isBreathing() && entity.isAlive();
      if (state.beam) {
         // relative to the entity's current position, so the beam follows the smoothly interpolated model
         Vec3[] line = DragonAbilities.sunBeamLine(entity);
         Vec3 mouth = line[2];
         Vec3 end = line[1];
         Vec3 start = mouth;
         if (state.riderView) {
            Vec3 delta = end.subtract(mouth);
            double length = delta.length();
            start = length > 3.0 ? mouth.add(delta.scale(2.5 / length)) : end;
         }

         Vec3 origin = entity.position();
         state.beamStart = start.subtract(origin);
         state.beamEnd = end.subtract(origin);
         state.beamTime = entity.tickCount + partialTick;
      }
   }

   public Identifier getTextureLocation(DragonRenderState state) {
      return this.textures[state.blood.ordinal()];
   }

   protected void scale(DragonRenderState state, PoseStack poseStack) {
      float scale = this.kind.scale() * state.ageScale;
      poseStack.scale(scale, scale, scale);
   }

   @Override
   public void submit(DragonRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
      super.submit(state, poseStack, collector, camera);
      if (state.beam) {
         SunBeamRenderer.submit(state, poseStack, collector);
      }
   }

   protected AABB getBoundingBoxForCulling(DragonEntity entity, float partialTick) {
      AABB box = entity.renderBounds();
      if (this.kind.ability() == DragonKind.Ability.SUN_BEAM && entity.isBreathing()) {
         // keep drawing the dragon (and its beam) while only the beam is on screen
         box = box.minmax(new AABB(DragonAbilities.sunBeamLine(entity)[1], DragonAbilities.sunBeamLine(entity)[1]).inflate(2.0));
      }

      return box;
   }
}
