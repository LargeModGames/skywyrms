package com.pokato.skywyrms.client;

import com.pokato.skywyrms.Skywyrms;
import com.pokato.skywyrms.client.render.DragonRenderer;
import com.pokato.skywyrms.client.render.EggRenderer;
import com.pokato.skywyrms.entity.DragonKind;
import com.pokato.skywyrms.registry.ModEntities;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public final class SkywyrmsClient {
   private SkywyrmsClient() {
   }

   public static void init() {
      Skywyrms.LOGGER.info("Skywyrms client init");
   }

   public static void registerRenderers(SkywyrmsClient.RendererRegistrar registrar) {
      for (DragonKind kind : DragonKind.values()) {
         registrar.register(ModEntities.type(kind), context -> new DragonRenderer(context, kind));
      }

      registrar.register(ModEntities.egg(), EggRenderer::new);
   }

   @FunctionalInterface
   public interface RendererRegistrar {
      <T extends Entity> void register(EntityType<? extends T> type, EntityRendererProvider<T> provider);
   }
}
