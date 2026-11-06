package com.pokato.skywyrms.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the solar wyrm's sun beam as glowing geometry: a white core tube inside two pulsing golden glow
 * tubes, using the beacon beam render type so it is full bright. Every half second the beam flares wider.
 */
public final class SunBeamRenderer {
   private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/entity/beacon/beacon_beam.png");
   private static final int SIDES = 10;
   private static final int FULL_BRIGHT = 15728880;
   private static final int NO_OVERLAY = 655360;

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
      Vec3 u = dir.cross(new Vec3(0.0, 1.0, 0.0));
      u = u.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : u.normalize();
      Vec3 w = u.cross(dir).normalize();
      float t = state.beamTime;
      float pulse = 0.5F + 0.5F * (float)Math.sin(t * 0.55F);
      float phase = (t % 10.0F) / 10.0F;
      float flare = phase < 0.3F ? 1.0F - phase / 0.3F : 0.0F;
      float scroll = -t * 0.35F;
      float vLen = (float)length * 0.3F;

      tube(poseStack, collector, RenderTypes.beaconBeam(TEXTURE, false), start, end, u, w,
         0.32F + 0.08F * pulse + 0.15F * flare, 0xFFFFF8E0, scroll, vLen, false);
      tube(poseStack, collector, RenderTypes.beaconBeam(TEXTURE, true), start, end, u, w,
         0.85F + 0.3F * pulse + 0.6F * flare, argb(110 + (int)(90 * flare), 0xFFD24A), scroll * 0.7F, vLen, true);
      tube(poseStack, collector, RenderTypes.beaconBeam(TEXTURE, true), start, end, u, w,
         1.7F + 0.5F * pulse + 1.2F * flare, argb(45 + (int)(70 * flare), 0xFFA21E), scroll * 0.4F, vLen * 0.5F, true);
   }

   private static int argb(int alpha, int rgb) {
      return Math.max(0, Math.min(255, alpha)) << 24 | rgb & 0xFFFFFF;
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
         .setOverlay(NO_OVERLAY)
         .setLight(FULL_BRIGHT)
         .setNormal(pose, 0.0F, 1.0F, 0.0F);
   }
}
