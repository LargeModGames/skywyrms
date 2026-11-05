package com.pokato.skywyrms.registry;

import com.pokato.skywyrms.Skywyrms;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.Identifier;

public final class ModParticles {
   private static final Map<ModParticles.Style, SimpleParticleType> TYPES = new EnumMap<>(ModParticles.Style.class);

   private ModParticles() {
   }

   public static synchronized SimpleParticleType type(ModParticles.Style style) {
      SimpleParticleType type = TYPES.get(style);
      if (type == null) {
         type = new ModParticles.Type();
         TYPES.put(style, type);
      }

      return type;
   }

   public static SimpleParticleType ember() {
      return type(ModParticles.Style.EMBER);
   }

   public static SimpleParticleType frost() {
      return type(ModParticles.Style.FROST);
   }

   public static SimpleParticleType tide() {
      return type(ModParticles.Style.TIDE);
   }

   public static SimpleParticleType venom() {
      return type(ModParticles.Style.VENOM);
   }

   public static SimpleParticleType voidWisp() {
      return type(ModParticles.Style.VOID);
   }

   public static SimpleParticleType spark() {
      return type(ModParticles.Style.SPARK);
   }

   public static SimpleParticleType dust() {
      return type(ModParticles.Style.DUST);
   }

   public static void register(BiConsumer<Identifier, ParticleType<?>> registrar) {
      for (ModParticles.Style style : ModParticles.Style.values()) {
         registrar.accept(Skywyrms.id(style.id), type(style));
      }
   }

   public enum Style {
      EMBER("ember", 13, 8, 0.32F, 1.2F, 0.92F, -0.035F, true, true),
      FROST("frost", 16, 9, 0.34F, 1.25F, 0.92F, 0.01F, true, true),
      TIDE("tide", 13, 6, 0.36F, 0.92F, 0.95F, 0.3F, false, true),
      VENOM("venom", 14, 8, 0.3F, 0.65F, 0.93F, 0.25F, false, true),
      VOID("void_wisp", 16, 10, 0.32F, 0.85F, 0.9F, -0.01F, true, false),
      SPARK("spark", 3, 3, 0.45F, 0.6F, 0.8F, 0.0F, true, false),
      DUST("dust", 22, 14, 0.42F, 1.45F, 0.9F, -0.004F, false, true);

      public final String id;
      public final int life;
      public final int lifeVariation;
      public final float startSize;
      public final float endSize;
      public final float friction;
      public final float gravity;
      public final boolean glow;
      public final boolean physics;

      Style(String id, int life, int lifeVariation, float startSize, float endSize, float friction, float gravity, boolean glow, boolean physics) {
         this.id = id;
         this.life = life;
         this.lifeVariation = lifeVariation;
         this.startSize = startSize;
         this.endSize = endSize;
         this.friction = friction;
         this.gravity = gravity;
         this.glow = glow;
         this.physics = physics;
      }
   }

   private static final class Type extends SimpleParticleType {
      Type() {
         super(true);
      }
   }
}
