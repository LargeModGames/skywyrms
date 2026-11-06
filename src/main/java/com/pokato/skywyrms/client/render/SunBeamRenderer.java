package com.pokato.skywyrms.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pokato.skywyrms.ability.DragonAbilities;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;

/** Draws the sun beam as full-bright tubes that flare wider on every damage tick. */
public final class SunBeamRenderer {
   private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/entity/beacon/beacon_beam.png");
   private static final int SIDES = 10;
   private static final RenderType CORE = RenderTypes.beaconBeam(TEXTURE, false);
   private static final RenderType GLOW = RenderTypes.beaconBeam(TEXTURE, true);

   private SunBeamRenderer() {
   }

   public static void submit(DragonRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
      Vec3 start = state.beamStart;
      Vec3 end = state.beamEnd;
      Vec3 axis = end.subtract(start);
      double length = axis.length();
      if (length < 0.3) {
         return;
      }

      Vec3 dir = axis.scale(1.0 / length);
      Vec3 u = DragonAbilities.beamSide(dir);
      Vec3 w = u.cross(dir);
      float t = state.beamTime;
      float pulse = 0.5F + 0.5F * (float)Math.sin(t * 0.55F);
      float flare = DragonAbilities.sunBeamFlare(t);
      float scroll = -t * 0.35F;
      float vLen = (float)length * 0.3F;

      tube(poseStack, collector, CORE, start, end, u, w,
         0.32F + 0.08F * pulse + 0.15F * flare, 0xFFFFF8E0, scroll, vLen, false);
      tube(poseStack, collector, GLOW, start, end, u, w,
         0.85F + 0.3F * pulse + 0.6F * flare, ARGB.color(110 + (int)(90 * flare), 0xFFD24A), scroll * 0.7F, vLen, true);
      tube(poseStack, collector, GLOW, start, end, u, w,
         1.7F + 0.5F * pulse + 1.2F * flare, ARGB.color(45 + (int)(70 * flare), 0xFFA21E), scroll * 0.4F, vLen * 0.5F, true);
   }

   private static void tube(
      PoseStack poseStack, SubmitNodeCollector collector, RenderType type, Vec3 start, Vec3 end, Vec3 u, Vec3 w,
      float radius, int color, float v0, float vLen, boolean bothSides
   ) {
      collector.submitCustomGeometry(poseStack, type, (pose, consumer) -> {
         for (int k = 0; k < SIDES; k++) {
            double a0 = Math.PI * 2.0 * k / SIDES;
            double a1 = Math.PI * 2.0 * (k + 1) / SIDES;
            Vec3 o0 = u.scale(Math.cos(a0) * radius).add(w.scale(Math.sin(a0) * radius));
            Vec3 o1 = u.scale(Math.cos(a1) * radius).add(w.scale(Math.sin(a1) * radius));
            Vec3 p0 = start.add(o0);
            Vec3 p1 = start.add(o1);
            Vec3 p2 = end.add(o1);
            Vec3 p3 = end.add(o0);
            float uA = (float)k / SIDES;
            float uB = (float)(k + 1) / SIDES;
            vertex(pose, consumer, p0, color, uA, v0);
            vertex(pose, consumer, p1, color, uB, v0);
            vertex(pose, consumer, p2, color, uB, v0 + vLen);
            vertex(pose, consumer, p3, color, uA, v0 + vLen);
            if (bothSides) {
               vertex(pose, consumer, p3, color, uA, v0 + vLen);
               vertex(pose, consumer, p2, color, uB, v0 + vLen);
               vertex(pose, consumer, p1, color, uB, v0);
               vertex(pose, consumer, p0, color, uA, v0);
            }
         }
      });
   }

   private static void vertex(PoseStack.Pose pose, VertexConsumer consumer, Vec3 p, int color, float u, float v) {
      consumer.addVertex(pose, (float)p.x, (float)p.y, (float)p.z)
         .setColor(color)
         .setUv(u, v)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(LightCoordsUtil.FULL_BRIGHT)
         .setNormal(pose, 0.0F, 1.0F, 0.0F);
   }
}
