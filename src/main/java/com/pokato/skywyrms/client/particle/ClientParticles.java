package com.pokato.skywyrms.client.particle;

import com.pokato.skywyrms.registry.ModParticles;
import java.util.function.Function;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;

public final class ClientParticles {
   private ClientParticles() {
   }

   public static void register(ClientParticles.Registrar registrar) {
      for (ModParticles.Style style : ModParticles.Style.values()) {
         registrar.register(ModParticles.type(style), sprites -> new BreathParticle.Provider(style, sprites));
      }
   }

   @FunctionalInterface
   public interface Registrar {
      void register(SimpleParticleType type, Function<SpriteSet, ParticleProvider<SimpleParticleType>> factory);
   }
}
