package com.pokato.skywyrms.client.particle;

import com.pokato.skywyrms.registry.ModParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.SingleQuadParticle.Layer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

public class BreathParticle extends SingleQuadParticle {
   private final ModParticles.Style style;
   private final SpriteSet sprites;
   private final float startSize;
   private final float endSize;

   protected BreathParticle(
      ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, ModParticles.Style style, SpriteSet sprites
   ) {
      super(level, x, y, z, sprites.first());
      this.style = style;
      this.sprites = sprites;
      this.xd = xSpeed;
      this.yd = ySpeed;
      this.zd = zSpeed;
      this.friction = style.friction;
      this.gravity = style.gravity;
      this.hasPhysics = style.physics;
      this.lifetime = style.life + this.random.nextInt(style.lifeVariation + 1);
      float jitter = 0.75F + this.random.nextFloat() * 0.5F;
      this.startSize = style.startSize * jitter;
      this.endSize = style.endSize * jitter;
      this.quadSize = this.startSize;
      this.roll = this.random.nextFloat() * (float) (Math.PI * 2);
      this.oRoll = this.roll;
      this.setSpriteFromAge(sprites);
      if (style == ModParticles.Style.DUST) {
         this.tintFromGround(level, x, y, z);
      }
   }

   private void tintFromGround(ClientLevel level, double x, double y, double z) {
      BlockPos pos = BlockPos.containing(x, y - 0.6, z);
      BlockState state = level.getBlockState(pos);
      MapColor map = state.isAir() ? null : state.getMapColor(level, pos);
      int col = map == null ? 12101770 : (map != MapColor.GRASS && map != MapColor.PLANT ? map.col : 9071180);
      float shade = 0.92F + this.random.nextFloat() * 0.12F;
      float r = ((col >> 16 & 0xFF) / 255.0F * 0.45F + 0.39600003F) * shade;
      float g = ((col >> 8 & 0xFF) / 255.0F * 0.45F + 0.36300004F) * shade;
      float b = ((col & 0xFF) / 255.0F * 0.45F + 0.29700002F) * shade;
      this.setColor(Math.min(1.0F, r), Math.min(1.0F, g), Math.min(1.0F, b));
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.removed) {
         this.setSpriteFromAge(this.sprites);
         float life = (float)this.age / this.lifetime;
         this.alpha = life < 0.7F ? 1.0F : Mth.clamp(1.0F - (life - 0.7F) / 0.3F, 0.0F, 1.0F);
         if (this.style == ModParticles.Style.DUST) {
            this.alpha *= 0.75F;
         }
      }
   }

   @Override
   public float getQuadSize(float partialTick) {
      float life = Mth.clamp((this.age + partialTick) / this.lifetime, 0.0F, 1.0F);
      float eased = 1.0F - (1.0F - life) * (1.0F - life);
      return Mth.lerp(eased, this.startSize, this.endSize);
   }

   @Override
   protected int getLightCoords(float partialTick) {
      return this.style.glow ? 15728880 : super.getLightCoords(partialTick);
   }

   @Override
   protected Layer getLayer() {
      return Layer.TRANSLUCENT;
   }

   public static final class Provider implements ParticleProvider<SimpleParticleType> {
      private final ModParticles.Style style;
      private final SpriteSet sprites;

      public Provider(ModParticles.Style style, SpriteSet sprites) {
         this.style = style;
         this.sprites = sprites;
      }

      public Particle createParticle(
         SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, RandomSource random
      ) {
         return new BreathParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.style, this.sprites);
      }
   }
}
