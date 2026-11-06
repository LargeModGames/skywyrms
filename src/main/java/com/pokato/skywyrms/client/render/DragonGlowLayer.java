package com.pokato.skywyrms.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.pokato.skywyrms.entity.DragonKind;
import com.pokato.skywyrms.entity.GeoData;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

/** Glow pass with its own model copy that holds only the cubes with lit texels in some glow texture of this body. */
public class DragonGlowLayer extends RenderLayer<DragonRenderState, DragonModel> {
   private final DragonModel model;
   private final RenderType[] renderTypes = new RenderType[DragonKind.values().length];

   public DragonGlowLayer(RenderLayerParent<DragonRenderState, DragonModel> parent, Context context, DragonKind kind) {
      super(parent);
      GeoData geo = kind.geo();
      Set<GeoData.Cube> lit = new HashSet<>();

      for (DragonKind blood : DragonKind.values()) {
         Identifier texture = DragonRenderer.texture(kind, blood, true);
         this.renderTypes[blood.ordinal()] = glow(texture);
         markLitCubes(context.getResourceManager(), texture, geo, lit);
      }

      this.model = new DragonModel(DragonModel.createLayer(kind, lit::contains).bakeRoot(), geo);
   }

   /** RenderTypes.eyes minus sortOnUpload, which changes nothing for glow drawn on the dragon's own faces. */
   private static RenderType glow(Identifier texture) {
      return RenderType.create(
         "skywyrms_glow",
         RenderSetup.builder(RenderPipelines.EYES).setOitPipelines(RenderPipelines.OIT_EYES).withTexture("Sampler0", texture).createRenderSetup()
      );
   }

   private static void markLitCubes(ResourceManager resources, Identifier texture, GeoData geo, Set<GeoData.Cube> lit) {
      try (InputStream in = resources.open(texture); NativeImage image = NativeImage.read(in)) {
         float sx = (float)image.getWidth() / geo.textureWidth;
         float sy = (float)image.getHeight() / geo.textureHeight;

         for (GeoData.Part part : geo.parts) {
            for (GeoData.Cube c : part.cubes) {
               if (!lit.contains(c) && anyLit(image, c.u * sx, c.v * sy, (c.u + 2.0F * (c.d + c.w)) * sx, (c.v + c.d + c.h) * sy)) {
                  lit.add(c);
               }
            }
         }
      } catch (IOException e) {
         geo.parts.forEach(part -> lit.addAll(part.cubes));
      }
   }

   private static boolean anyLit(NativeImage image, float u0, float v0, float u1, float v1) {
      int x1 = Math.min(image.getWidth(), Mth.ceil(u1));
      int y1 = Math.min(image.getHeight(), Mth.ceil(v1));

      for (int y = Math.max(0, Mth.floor(v0)); y < y1; y++) {
         for (int x = Math.max(0, Mth.floor(u0)); x < x1; x++) {
            if (ARGB.alpha(image.getPixel(x, y)) != 0) {
               return true;
            }
         }
      }

      return false;
   }

   @Override
   public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, DragonRenderState state, float yRot, float xRot) {
      collector.order(1)
         .submitModel(this.model, state, poseStack, this.renderTypes[state.blood.ordinal()], light, OverlayTexture.NO_OVERLAY, state.outlineColor);
   }
}
