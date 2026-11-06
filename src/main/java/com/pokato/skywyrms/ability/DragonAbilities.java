package com.pokato.skywyrms.ability;

import com.pokato.skywyrms.config.ModConfig;
import com.pokato.skywyrms.entity.DragonEntity;
import com.pokato.skywyrms.entity.DragonKind;
import com.pokato.skywyrms.registry.ModParticles;
import com.pokato.skywyrms.registry.ModSounds;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class DragonAbilities {
   public static final double BREATH_RANGE = 20.0;
   public static final double SUN_BEAM_RANGE = 96.0;
   /** How far (in blocks) from the beam line a target can be and still be hit. */
   public static final double SUN_BEAM_WIDTH = 2.0;
   /** Ticks between sun beam hits, and the tick of each period on which the hit lands. */
   private static final int SUN_BEAM_PERIOD = 10;
   private static final int SUN_BEAM_HIT = 3;
   private static final double CONE_SLOPE = 0.26;
   private static final double FX = 1.33;

   private DragonAbilities() {
   }

   public static int cost(DragonKind.Ability ability) {
      return switch (ability) {
         case LIGHTNING -> 30;
         case VENOM_SPIT -> 25;
         case VOID_BLINK -> 40;
         default -> 0;
      };
   }

   public static int cooldown(DragonKind.Ability ability) {
      return switch (ability) {
         case LIGHTNING -> 14;
         case VENOM_SPIT -> 12;
         case VOID_BLINK -> 24;
         default -> 0;
      };
   }

   public static double aiRange(DragonKind.Ability ability) {
      return switch (ability) {
         case LIGHTNING -> 34.0;
         case VENOM_SPIT -> 29.0;
         case VOID_BLINK -> 26.0;
         case SUN_BEAM -> 40.0;
         default -> 14.5;
      };
   }

   private static Vec3 jitter(Vec3 direction, RandomSource random, double spread) {
      return new Vec3(direction.x + random.nextGaussian() * spread, direction.y + random.nextGaussian() * spread, direction.z + random.nextGaussian() * spread);
   }

   private static Vec3 direction(Vec3 from, Vec3 to, DragonEntity dragon) {
      Vec3 delta = to.subtract(from);
      return delta.lengthSqr() < 1.0E-4 ? DragonEntity.viewVector(dragon.getXRot(), dragon.yHeadRot) : delta.normalize();
   }

   private static boolean mobGriefing(ServerLevel level) {
      return !ModConfig.get().breathAffectsBlocks ? false : level.getGameRules().get(GameRules.MOB_GRIEFING);
   }

   private static void sound(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
      level.playSound(null, at.x, at.y, at.z, sound, SoundSource.NEUTRAL, volume, pitch);
   }

   private static BlockHitResult clipBlocks(DragonEntity dragon, Vec3 from, Vec3 to, Fluid fluid) {
      return dragon.level().clip(new ClipContext(from, to, Block.COLLIDER, fluid, dragon));
   }

   private static boolean clearPath(DragonEntity dragon, Vec3 from, Vec3 to) {
      return clipBlocks(dragon, from, to, Fluid.NONE).getType() == Type.MISS;
   }

   private static Vec3 impactPoint(DragonEntity dragon, Vec3 mouth, double range) {
      LivingEntity user = dragon.abilityUser();
      Vec3 from = user != dragon ? user.getEyePosition() : mouth;
      Vec3 aim = dragon.aimPoint(range);
      Vec3 dir = direction(from, aim, dragon);
      Vec3 end = from.add(dir.scale(range));
      BlockHitResult block = clipBlocks(dragon, from, end, Fluid.NONE);
      if (block.getType() != Type.MISS) {
         end = block.getLocation();
      }

      Vec3 best = end;
      double bestDistance = from.distanceToSqr(end);
      AABB search = new AABB(from, end).inflate(1.5);

      for (LivingEntity entity : dragon.level().getEntitiesOfClass(LivingEntity.class, search, dragon::canAbilityHit)) {
         if (entity != user) {
            Optional<Vec3> hit = entity.getBoundingBox().inflate(0.45).clip(from, end);
            if (hit.isPresent()) {
               double distance = from.distanceToSqr(hit.get());
               if (distance < bestDistance) {
                  bestDistance = distance;
                  best = entity.getBoundingBox().getCenter();
               }
            }
         }
      }

      return best;
   }

   private static void line(ServerLevel level, ParticleOptions particle, Vec3 from, Vec3 to, double step, int count, double spread, double speed) {
      Vec3 delta = to.subtract(from);
      double length = delta.length();
      int points = (int)Math.min(150.0, Math.max(1.0, length / step));

      for (int i = 0; i <= points; i++) {
         Vec3 p = from.add(delta.scale((double)i / points));
         level.sendParticles(particle, p.x, p.y, p.z, count, spread, spread, spread, speed);
      }
   }

   /** Client side: golden sparks rising from the solar wyrm's corona and back spines, like solar flares. */
   public static void solarSparks(DragonEntity dragon) {
      if (dragon.tickCount % 2 != 0) {
         return;
      }

      Level level = dragon.level();
      RandomSource random = dragon.getRandom();
      float size = dragon.getAgeScale() * dragon.getKind().scale();
      Vec3 forward = DragonEntity.viewVector(0.0F, dragon.yBodyRot);
      Vec3 mouth = dragon.mouthPosition();
      // corona sits on top of the head, a little behind the mouth
      Vec3 crown = mouth.add(forward.scale(-1.3 * size)).add(0.0, 1.1 * size, 0.0);
      level.addParticle(ParticleTypes.WAX_ON, crown.x + (random.nextDouble() - 0.5) * 1.2 * size, crown.y + random.nextDouble() * 0.6 * size,
         crown.z + (random.nextDouble() - 0.5) * 1.2 * size, 0.0, 0.03, 0.0);
      // along the back spines, from shoulders to tail
      double along = (random.nextDouble() - 0.35) * 3.2 * size;
      Vec3 back = dragon.position().add(forward.scale(-along)).add(0.0, dragon.getBbHeight() * 0.95, 0.0);
      level.addParticle(ParticleTypes.WAX_ON, back.x, back.y + random.nextDouble() * 0.5 * size, back.z, 0.0, 0.04, 0.0);
      if (random.nextInt(6) == 0) {
         level.addParticle(ParticleTypes.SMALL_FLAME, crown.x, crown.y + 0.3 * size, crown.z, 0.0, 0.02, 0.0);
      }
   }

   public static void breathParticles(DragonEntity dragon) {
      Level level = dragon.level();
      RandomSource random = dragon.getRandom();
      if (dragon.getKind().ability() == DragonKind.Ability.SUN_BEAM) {
         sunBeamParticles(dragon, level, random);
         return;
      }

      Vec3 mouth = dragon.mouthPosition();
      Vec3 dir = direction(mouth, dragon.aimPoint(20.0), dragon);
      Vec3 carry = new Vec3(dragon.getX() - dragon.xo, dragon.getY() - dragon.yo, dragon.getZ() - dragon.zo);
      switch (dragon.getKind().ability()) {
         case FIRE_BREATH:
            stream(level, random, ModParticles.ember(), mouth, dir, carry, 7, 0.08645000000000001, 1.2635, 0.46549999999999997);

            for (int i = 0; i < 4; i++) {
               Vec3 v = jitter(dir, random, 0.1463).scale((0.7 + random.nextDouble() * 0.5) * 1.33).add(carry);
               level.addParticle(ParticleTypes.FLAME, true, false, mouth.x, mouth.y, mouth.z, v.x, v.y, v.z);
            }

            if (random.nextInt(3) == 0) {
               Vec3 p = mouth.add(dir.scale(1.0 + random.nextDouble() * 5.5));
               level.addParticle(ParticleTypes.LAVA, p.x, p.y, p.z, 0.0, 0.0, 0.0);
            }
            break;
         case FROST_BREATH:
            stream(level, random, ModParticles.frost(), mouth, dir, carry, 6, 0.09310000000000002, 1.2635, 0.46549999999999997);

            for (int i = 0; i < 3; i++) {
               Vec3 v = jitter(dir, random, 0.1197).scale((0.8 + random.nextDouble() * 0.4) * 1.33).add(carry);
               level.addParticle(ParticleTypes.SNOWFLAKE, true, false, mouth.x, mouth.y, mouth.z, v.x, v.y, v.z);
            }

            if (random.nextInt(2) == 0) {
               Vec3 v = jitter(dir, random, 0.0798).scale(1.33).add(carry);
               level.addParticle(ParticleTypes.END_ROD, mouth.x, mouth.y, mouth.z, v.x, v.y, v.z);
            }
            break;
         case TIDAL_JET:
            stream(level, random, ModParticles.tide(), mouth, dir, carry, 8, 0.053200000000000004, 1.3965, 0.399);

            for (int i = 0; i < 3; i++) {
               Vec3 v = jitter(dir, random, 0.0665).scale((0.9 + random.nextDouble() * 0.4) * 1.33).add(carry);
               level.addParticle(ParticleTypes.FISHING, true, false, mouth.x, mouth.y, mouth.z, v.x, v.y, v.z);
            }

            if (random.nextInt(2) == 0) {
               Vec3 v = jitter(dir, random, 0.10640000000000001).scale(1.064).add(carry);
               level.addParticle(ParticleTypes.CLOUD, mouth.x, mouth.y, mouth.z, v.x, v.y, v.z);
            }
      }

      DragonKind blood = dragon.blood();
      if (blood != dragon.getKind()) {
         ParticleOptions accent = accentParticle(blood);

         for (int i = 0; i < 2; i++) {
            Vec3 v = jitter(dir, random, 0.10640000000000001).scale((0.75 + random.nextDouble() * 0.45) * 1.33).add(carry);
            Vec3 p = mouth.add(v.scale(random.nextDouble()));
            level.addParticle(accent, true, false, p.x, p.y, p.z, v.x, v.y, v.z);
         }
      }
   }

   public static ParticleOptions accentParticle(DragonKind blood) {
      return switch (blood) {
         case EMBER -> ModParticles.ember();
         case FROST -> ModParticles.frost();
         case STORM -> ModParticles.spark();
         case THORN -> ModParticles.venom();
         case VOID -> ModParticles.voidWisp();
         case TIDE -> ModParticles.tide();
         case SOLAR -> ParticleTypes.END_ROD;
      };
   }

   private static void accentBurst(DragonEntity dragon, ServerLevel level, Vec3 at, int count) {
      DragonKind blood = dragon.blood();
      if (blood != dragon.getKind()) {
         level.sendParticles(accentParticle(blood), at.x, at.y + 0.4, at.z, count, 1.33, 0.8, 1.33, 0.12);
         level.sendParticles(DragonEntity.elementParticle(blood), at.x, at.y + 0.4, at.z, count, 1.33, 0.8, 1.33, 0.05);
      }
   }

   public static void infuse(DragonEntity dragon, ServerLevel level, LivingEntity target) {
      DragonKind blood = dragon.blood();
      if (blood != dragon.getKind() && target.isAlive()) {
         float power = dragon.abilityPower();
         switch (blood) {
            case EMBER:
               if (!target.fireImmune()) {
                  target.igniteForSeconds(3.0F);
               }
               break;
            case FROST:
               if (target.canFreeze()) {
                  target.setTicksFrozen(Math.min(target.getTicksRequiredToFreeze() + 60, target.getTicksFrozen() + 40));
               }

               target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1), dragon);
               break;
            case STORM:
               Vec3 from = target.getBoundingBox().getCenter();
               LivingEntity next = level.getEntitiesOfClass(
                     LivingEntity.class, target.getBoundingBox().inflate(6.6), entity -> entity != target && dragon.canAbilityHit(entity)
                  )
                  .stream()
                  .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(from)))
                  .orElse(null);
               LivingEntity zapped = next != null ? next : target;
               Vec3 to = zapped.getBoundingBox().getCenter();
               line(level, ModParticles.spark(), from, to, 0.7, 1, 0.08, 0.0);
               zapped.hurtServer(level, dragon.abilityDamage(DamageTypes.LIGHTNING_BOLT), 3.0F * power);
               break;
            case THORN:
               target.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0), dragon);
               break;
            case VOID:
               target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0), dragon);
               Vec3 at = target.getBoundingBox().getCenter();
               level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y, at.z, 8, 0.3, 0.4, 0.3, 0.05);
               break;
            case TIDE:
               target.clearFire();
               Vec3 away = target.position().subtract(dragon.position());
               Vec3 push = new Vec3(away.x, 0.0, away.z);
               if (push.lengthSqr() > 1.0E-4) {
                  double resist = 1.0 - Math.min(0.8, target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
                  push = push.normalize().scale(0.45 * resist);
                  target.push(push.x, 0.15 * resist, push.z);
                  DragonEntity.markMoved(target);
               }

               if (target.isSensitiveToWater() || target.fireImmune()) {
                  target.hurtServer(level, dragon.abilityDamage(DamageTypes.MOB_ATTACK), 3.0F * power);
               }
               break;
            case SOLAR:
               target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0), dragon);
               if (target.typeHolder().is(EntityTypeTags.UNDEAD)) {
                  target.igniteForSeconds(4.0F);
                  target.hurtServer(level, dragon.abilityDamage(DamageTypes.INDIRECT_MAGIC), 3.0F * power);
               }
         }
      }
   }

   private static void stream(
      Level level,
      RandomSource random,
      ParticleOptions particle,
      Vec3 mouth,
      Vec3 dir,
      Vec3 carry,
      int count,
      double spread,
      double speed,
      double speedVariation
   ) {
      for (int i = 0; i < count; i++) {
         Vec3 v = jitter(dir, random, spread).scale(speed + random.nextDouble() * speedVariation).add(carry);
         Vec3 p = mouth.add(v.scale(random.nextDouble()));
         level.addParticle(particle, true, false, p.x, p.y, p.z, v.x, v.y, v.z);
      }
   }

   public static void breathTick(DragonEntity dragon, ServerLevel level, int tick) {
      DragonKind.Ability ability = dragon.getKind().ability();
      if (ability == DragonKind.Ability.SUN_BEAM) {
         sunBeamTick(dragon, level, tick);
         return;
      }

      Vec3 mouth = dragon.mouthPosition();
      Vec3 dir = direction(mouth, dragon.aimPoint(20.0), dragon);
      RandomSource random = dragon.getRandom();
      if (tick == 0) {
         sound(level, mouth, ModSounds.voice(dragon.getKind(), ModSounds.Voice.ROAR), 1.6F, 1.05F + random.nextFloat() * 0.1F);
      }

      if (tick % 7 == 0) {
         String chunk = switch (ability) {
            case FIRE_BREATH -> "ability.fire_breath";
            case FROST_BREATH -> "ability.frost_breath";
            case TIDAL_JET -> "ability.tidal_jet";
            default -> null;
         };
         if (chunk != null) {
            sound(level, mouth, ModSounds.get(chunk), 1.8F, 0.92F + random.nextFloat() * 0.16F);
         }
      }

      if (tick % 10 == 3) {
         hitCone(dragon, level, mouth, dir, ability);
      }

      if (tick % 3 == 0) {
         touchBlocks(dragon, level, mouth, dir, ability, random);
      }
   }

   private static void hitCone(DragonEntity dragon, ServerLevel level, Vec3 mouth, Vec3 dir, DragonKind.Ability ability) {
      Vec3 end = mouth.add(dir.scale(20.0));
      AABB box = new AABB(mouth, end).inflate(7.2);

      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, dragon::canAbilityHit)) {
         Vec3 center = target.getBoundingBox().getCenter();
         Vec3 offset = center.subtract(mouth);
         double along = offset.dot(dir);
         if (!(along < -0.5) && !(along > 20.0)) {
            double beside = offset.subtract(dir.scale(along)).length();
            if (!(beside > 1.2 + target.getBbWidth() * 0.5 + Math.max(0.0, along) * 0.26) && clearPath(dragon, mouth, center)) {
               float power = dragon.abilityPower();
               switch (ability) {
                  case FIRE_BREATH:
                     if (!target.fireImmune()) {
                        target.igniteForSeconds(6.0F);
                     }

                     target.hurtServer(level, dragon.abilityDamage(DamageTypes.FIREBALL), 6.0F * power);
                     break;
                  case FROST_BREATH:
                     target.clearFire();
                     if (target.canFreeze()) {
                        target.setTicksFrozen(Math.min(target.getTicksRequiredToFreeze() + 140, target.getTicksFrozen() + 90));
                     }

                     target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 3), dragon);
                     target.hurtServer(level, dragon.abilityDamage(DamageTypes.FREEZE), 4.0F * power);
                     break;
                  case TIDAL_JET:
                     target.clearFire();
                     float damage = (!target.isSensitiveToWater() && !target.fireImmune() ? 4.0F : 9.0F) * power;
                     target.hurtServer(level, dragon.abilityDamage(DamageTypes.MOB_ATTACK), damage);
                     double resist = 1.0 - Math.min(0.8, target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
                     target.push(dir.x * 1.1 * resist, 0.3 * resist, dir.z * 1.1 * resist);
                     DragonEntity.markMoved(target);
               }

               infuse(dragon, level, target);
            }
         }
      }
   }

   private static void touchBlocks(DragonEntity dragon, ServerLevel level, Vec3 mouth, Vec3 dir, DragonKind.Ability ability, RandomSource random) {
      boolean griefing = mobGriefing(level) && (dragon.getControllingPassenger() != null || !dragon.isTame());

      for (int i = 0; i < 3; i++) {
         Vec3 ray = jitter(dir, random, 0.156).normalize();
         Vec3 end = mouth.add(ray.scale(20.0 * (0.6 + random.nextDouble() * 0.4)));
         Fluid fluid = ability == DragonKind.Ability.FROST_BREATH ? Fluid.SOURCE_ONLY : Fluid.NONE;
         BlockHitResult hit = clipBlocks(dragon, mouth, end, fluid);
         if (hit.getType() != Type.MISS) {
            BlockPos pos = hit.getBlockPos();
            BlockPos front = pos.relative(hit.getDirection());
            Vec3 at = hit.getLocation();
            switch (ability) {
               case FIRE_BREATH:
                  level.sendParticles(ParticleTypes.LAVA, at.x, at.y, at.z, 1, 0.2, 0.1, 0.2, 0.0);
                  level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y, at.z, 2, 0.3, 0.2, 0.3, 0.01);
                  if (griefing && random.nextInt(3) == 0 && level.getBlockState(front).isAir() && BaseFireBlock.canBePlacedAt(level, front, Direction.UP)) {
                     level.setBlockAndUpdate(front, BaseFireBlock.getState(level, front));
                  }
                  break;
               case FROST_BREATH:
                  level.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y, at.z, 4, 0.4, 0.2, 0.4, 0.02);
                  level.sendParticles(ParticleTypes.CLOUD, at.x, at.y, at.z, 1, 0.3, 0.1, 0.3, 0.01);
                  extinguish(level, front);
                  if (griefing
                     && level.getFluidState(pos).is(FluidTags.WATER)
                     && level.getFluidState(pos).isSource()
                     && level.getBlockState(pos.above()).isAir()) {
                     for (BlockPos near : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 0, 1))) {
                        BlockState state = level.getBlockState(near);
                        if (state.is(Blocks.WATER) && level.getFluidState(near).isSource() && level.getBlockState(near.above()).isAir()) {
                           level.setBlockAndUpdate(near.immutable(), Blocks.FROSTED_ICE.defaultBlockState());
                        }
                     }
                  }
                  break;
               case TIDAL_JET:
                  level.sendParticles(ParticleTypes.SPLASH, at.x, at.y, at.z, 10, 0.5, 0.2, 0.5, 0.1);
                  level.sendParticles(ParticleTypes.CLOUD, at.x, at.y, at.z, 1, 0.3, 0.1, 0.3, 0.02);
                  extinguish(level, front);
                  extinguish(level, pos);
            }
         }
      }
   }

   private static void extinguish(ServerLevel level, BlockPos pos) {
      if (level.getBlockState(pos).getBlock() instanceof BaseFireBlock) {
         level.removeBlock(pos, false);
         level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 1.6F);
      }
   }

   public static void blinkArrival(DragonEntity dragon, ServerLevel level) {
      LivingEntity user = dragon.abilityUser();
      Vec3 to = dragon.position();
      double width = dragon.getBbWidth() * 0.6;
      double height = dragon.getBbHeight() * 0.5;
      Vec3 arrived = to.add(0.0, height, 0.0);
      level.sendParticles(ModParticles.voidWisp(), arrived.x, arrived.y, arrived.z, 90, width, height, width, 0.47);
      level.sendParticles(ParticleTypes.REVERSE_PORTAL, arrived.x, arrived.y, arrived.z, 200, width, height, width, 0.33);
      sound(level, to, ModSounds.get("ability.void_arrive"), 2.2F, 0.95F + level.getRandom().nextFloat() * 0.1F);
      accentBurst(dragon, level, to, 24);

      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(arrived, arrived).inflate(7.3), dragon::canAbilityHit)) {
         if (target != user) {
            target.hurtServer(level, dragon.abilityDamage(DamageTypes.DRAGON_BREATH), 8.0F * dragon.abilityPower());
            infuse(dragon, level, target);
            Vec3 away = target.position().subtract(to);
            Vec3 push = new Vec3(away.x, 0.0, away.z);
            if (push.lengthSqr() > 1.0E-4) {
               push = push.normalize().scale(0.9);
               target.push(push.x, 0.35, push.z);
               DragonEntity.markMoved(target);
            }
         }
      }
   }

   public static boolean cast(DragonEntity dragon, ServerLevel level) {
      Vec3 mouth = dragon.mouthPosition();

      return switch (dragon.getKind().ability()) {
         case LIGHTNING -> lightning(dragon, level, mouth);
         case VENOM_SPIT -> venom(dragon, level, mouth);
         case VOID_BLINK -> blink(dragon, level);
         default -> false;
      };
   }

   /** The sun beam aims from `from` (the rider's eyes, or the mouth without a rider) to `end`, and is drawn from `mouth`. */
   public record SunBeam(Vec3 from, Vec3 end, Vec3 mouth) {
   }

   public static SunBeam sunBeamLine(DragonEntity dragon) {
      Vec3 mouth = dragon.mouthPosition();
      LivingEntity user = dragon.abilityUser();
      Vec3 from = user != dragon ? user.getEyePosition() : mouth;
      Vec3 aim = dragon.aimPoint(SUN_BEAM_RANGE);
      Vec3 end = from.add(direction(from, aim, dragon).scale(SUN_BEAM_RANGE));
      if (user != dragon) {
         // aimPoint already stopped the rider's look ray at the first solid block
         if (from.distanceToSqr(aim) < from.distanceToSqr(end)) {
            end = aim;
         }
      } else {
         BlockHitResult block = clipBlocks(dragon, from, end, Fluid.NONE);
         if (block.getType() != Type.MISS) {
            end = block.getLocation();
         }
      }

      return new SunBeam(from, end, mouth);
   }

   /** A unit vector at right angles to the beam direction, horizontal unless the beam is vertical. */
   public static Vec3 beamSide(Vec3 dir) {
      Vec3 u = dir.cross(new Vec3(0.0, 1.0, 0.0));
      return u.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : u.normalize();
   }

   /** Flare strength 0..1 of the sun beam: 1 on each hit, fading out over the next three ticks. */
   public static float sunBeamFlare(float breathAge) {
      float phase = Mth.positiveModulo(breathAge - SUN_BEAM_HIT, SUN_BEAM_PERIOD) / SUN_BEAM_PERIOD;
      return phase < 0.3F ? 1.0F - phase / 0.3F : 0.0F;
   }

   /** Every half second, hits each visible enemy within SUN_BEAM_WIDTH of the beam; undead take double and burn. */
   private static void sunBeamTick(DragonEntity dragon, ServerLevel level, int tick) {
      RandomSource random = dragon.getRandom();
      if (tick == 0) {
         sound(level, dragon.mouthPosition(), ModSounds.get("ability.sun_beam"), 2.5F, 0.95F + random.nextFloat() * 0.1F);
      } else if (tick % 20 == 0) {
         sound(level, dragon.mouthPosition(), ModSounds.get("ability.sun_beam"), 1.4F, 1.1F + random.nextFloat() * 0.1F);
      }

      if (tick % SUN_BEAM_PERIOD != SUN_BEAM_HIT) {
         return;
      }

      SunBeam beam = sunBeamLine(dragon);
      Vec3 from = beam.from();
      Vec3 end = beam.end();
      float power = dragon.abilityPower();
      LivingEntity user = dragon.abilityUser();
      AABB search = new AABB(from, end).inflate(SUN_BEAM_WIDTH + 1.0);

      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, search, dragon::canAbilityHit)) {
         AABB reach = target.getBoundingBox().inflate(SUN_BEAM_WIDTH);
         if (target != user
            && (reach.contains(from) || reach.clip(from, end).isPresent())
            && clearPath(dragon, from, target.getBoundingBox().getCenter())) {
            boolean undead = target.typeHolder().is(EntityTypeTags.UNDEAD);
            target.hurtServer(level, dragon.abilityDamage(DamageTypes.MOB_ATTACK), (undead ? 16.0F : 8.0F) * power);
            if (undead) {
               target.igniteForSeconds(4.0F);
            }

            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0), dragon);
            infuse(dragon, level, target);
         }
      }
   }

   /** Client side particles around the sun beam, which SunBeamRenderer draws. */
   private static void sunBeamParticles(DragonEntity dragon, Level level, RandomSource random) {
      SunBeam beam = sunBeamLine(dragon);
      Vec3 mouth = beam.mouth();
      Vec3 end = beam.end();
      Vec3 delta = end.subtract(mouth);
      double length = delta.length();
      if (length < 0.5) {
         return;
      }

      Vec3 dir = delta.scale(1.0 / length);
      Vec3 u = beamSide(dir);
      Vec3 v = u.cross(dir).normalize();
      float t = dragon.tickCount;
      double pulse = 0.5 + 0.5 * Math.sin(t * 0.55);
      boolean flare = dragon.breathAge() % SUN_BEAM_PERIOD == SUN_BEAM_HIT;

      // two spiral strands twisting along the beam (every other tick keeps the particle count sane)
      if (dragon.tickCount % 2 == 0) {
         double spiralR = 1.4 + 0.5 * pulse;
         int points = (int)Math.min(80.0, Math.max(2.0, length / 1.2));
         for (int i = 0; i <= points; i++) {
            double d = 2.5 + (length - 2.5) * i / points;
            Vec3 p = mouth.add(dir.scale(d));
            double a = d * 0.8 + t * 0.6;
            for (int s = 0; s < 2; s++) {
               double aa = a + Math.PI * s;
               Vec3 q = p.add(u.scale(Math.cos(aa) * spiralR)).add(v.scale(Math.sin(aa) * spiralR));
               level.addParticle(ParticleTypes.WAX_ON, true, false, q.x, q.y, q.z, 0.0, 0.0, 0.0);
            }
         }
      }

      // a burst of sparks along the whole beam on every flare
      if (flare) {
         for (double d = 2.5; d < length; d += 1.5) {
            Vec3 p = mouth.add(dir.scale(d));
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, true, false, p.x, p.y, p.z,
               (random.nextDouble() - 0.5) * 0.5, (random.nextDouble() - 0.5) * 0.5, (random.nextDouble() - 0.5) * 0.5);
         }
      }

      // starburst where the beam lands
      for (int i = 0; i < 8; i++) {
         level.addParticle(ParticleTypes.FIREWORK, true, false, end.x, end.y, end.z,
            (random.nextDouble() - 0.5) * 0.7, random.nextDouble() * 0.5, (random.nextDouble() - 0.5) * 0.7);
      }

      for (int i = 0; i < 4; i++) {
         level.addParticle(ParticleTypes.WAX_ON, true, false, end.x, end.y, end.z,
            (random.nextDouble() - 0.5) * 1.2, random.nextDouble() * 0.8, (random.nextDouble() - 0.5) * 1.2);
      }

      if (dragon.tickCount % 5 == 0) {
         // expanding ring of light around the impact, facing back along the beam
         for (int i = 0; i < 24; i++) {
            double a = Math.PI * 2.0 * i / 24;
            Vec3 out = u.scale(Math.cos(a)).add(v.scale(Math.sin(a))).scale(0.45);
            level.addParticle(ParticleTypes.END_ROD, true, false, end.x, end.y, end.z, out.x, out.y, out.z);
         }
      }

      // ball of light in the mouth: sparks orbiting on two tilted circles around a bright core
      level.addParticle(ParticleTypes.END_ROD, true, false, mouth.x, mouth.y, mouth.z, 0.0, 0.0, 0.0);
      for (int i = 0; i < 8; i++) {
         double a = t * 0.8 + Math.PI * 2.0 * i / 8;
         Vec3 ring = i % 2 == 0 ? u.scale(Math.cos(a)).add(v.scale(Math.sin(a))) : u.scale(Math.cos(a)).add(dir.scale(Math.sin(a)));
         Vec3 q = mouth.add(ring.scale(0.8 + 0.2 * pulse));
         level.addParticle(ParticleTypes.WAX_ON, true, false, q.x, q.y, q.z, 0.0, 0.0, 0.0);
      }
   }
   private static boolean lightning(DragonEntity dragon, ServerLevel level, Vec3 mouth) {
      Vec3 impact = impactPoint(dragon, mouth, 85.0);
      LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
      if (bolt != null) {
         bolt.snapTo(impact.x, impact.y, impact.z);
         bolt.setVisualOnly(true);
         if (dragon.abilityUser() instanceof ServerPlayer player) {
            bolt.setCause(player);
         }

         level.addFreshEntity(bolt);
      }

      line(level, ModParticles.spark(), mouth, impact, 0.45, 2, 0.14, 0.0);
      line(level, ParticleTypes.ELECTRIC_SPARK, mouth, impact, 1.1, 2, 0.15, 0.05);
      level.sendParticles(ModParticles.spark(), impact.x, impact.y + 0.8, impact.z, 20, 1.75, 1.1, 1.75, 0.0);
      level.sendParticles(ParticleTypes.ELECTRIC_SPARK, impact.x, impact.y + 0.5, impact.z, 70, 1.6, 1.0, 1.6, 0.5);
      level.sendParticles(ModParticles.spark(), mouth.x, mouth.y, mouth.z, 6, 0.35, 0.35, 0.35, 0.0);
      sound(level, mouth, ModSounds.get("ability.lightning"), 3.0F, 0.95F + level.getRandom().nextFloat() * 0.1F);
      accentBurst(dragon, level, impact, 22);
      float power = dragon.abilityPower();
      List<LivingEntity> struck = new ArrayList<>();

      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(impact, impact).inflate(4.7), dragon::canAbilityHit)) {
         target.hurtServer(level, dragon.abilityDamage(DamageTypes.LIGHTNING_BOLT), 12.0F * power);
         infuse(dragon, level, target);
         struck.add(target);
      }

      Vec3 from = impact;

      for (int jump = 0; jump < 3; jump++) {
         Vec3 origin = from;
         LivingEntity next = level.getEntitiesOfClass(
               LivingEntity.class, new AABB(origin, origin).inflate(9.3), entity -> dragon.canAbilityHit(entity) && !struck.contains(entity)
            )
            .stream()
            .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(origin)))
            .orElse(null);
         if (next == null) {
            break;
         }

         Vec3 to = next.getBoundingBox().getCenter();
         line(level, ModParticles.spark(), from, to, 0.8, 1, 0.12, 0.0);
         line(level, ParticleTypes.ELECTRIC_SPARK, from, to, 0.8, 2, 0.08, 0.03);
         next.hurtServer(level, dragon.abilityDamage(DamageTypes.LIGHTNING_BOLT), 7.0F * power);
         infuse(dragon, level, next);
         struck.add(next);
         from = to;
      }

      return true;
   }

   private static boolean venom(DragonEntity dragon, ServerLevel level, Vec3 mouth) {
      Vec3 impact = impactPoint(dragon, mouth, 53.0);
      line(level, ModParticles.venom(), mouth, impact, 0.5, 1, 0.08, 0.01);
      line(level, ParticleTypes.ITEM_SLIME, mouth, impact, 1.2, 1, 0.1, 0.02);
      level.sendParticles(ModParticles.venom(), impact.x, impact.y + 0.4, impact.z, 36, 1.45, 0.65, 1.45, 0.29);
      level.sendParticles(ParticleTypes.SNEEZE, impact.x, impact.y + 0.3, impact.z, 28, 1.6, 0.65, 1.6, 0.05);
      level.sendParticles(ParticleTypes.ITEM_SLIME, impact.x, impact.y + 0.3, impact.z, 40, 1.05, 0.5, 1.05, 0.26);
      sound(level, mouth, ModSounds.get("ability.venom_spit"), 2.0F, 0.95F + level.getRandom().nextFloat() * 0.1F);
      sound(level, impact, ModSounds.get("ability.venom_splat"), 1.8F, 0.95F + level.getRandom().nextFloat() * 0.1F);
      accentBurst(dragon, level, impact, 20);

      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(impact, impact).inflate(3.3), dragon::canAbilityHit)) {
         target.hurtServer(level, dragon.abilityDamage(DamageTypes.INDIRECT_MAGIC), 6.0F * dragon.abilityPower());
         target.addEffect(new MobEffectInstance(MobEffects.POISON, 160, 1), dragon);
         infuse(dragon, level, target);
      }

      AreaEffectCloud cloud = new AreaEffectCloud(level, impact.x, impact.y, impact.z);
      cloud.setOwner(dragon.abilityUser());
      cloud.setRadius(4.7F);
      cloud.setRadiusOnUse(-0.25F);
      cloud.setWaitTime(8);
      cloud.setDuration(140);
      cloud.setRadiusPerTick(-0.016785713F);
      cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 1));
      cloud.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 0));
      level.addFreshEntity(cloud);
      return true;
   }

   private static boolean blink(DragonEntity dragon, ServerLevel level) {
      LivingEntity user = dragon.abilityUser();
      Vec3 from = dragon.position();
      double reach = 37.0;
      Vec3 dir;
      if (user != dragon) {
         dir = user.getLookAngle();
      } else {
         if (dragon.getTarget() == null) {
            return false;
         }

         Vec3 toTarget = dragon.getTarget().position().subtract(from);
         reach = Math.max(4.0, toTarget.length() - 3.5);
         dir = toTarget.normalize();
      }

      Vec3 centre = dragon.getBoundingBox().getCenter();
      BlockHitResult wall = clipBlocks(dragon, centre, centre.add(dir.scale(reach)), Fluid.NONE);
      if (wall.getType() != Type.MISS) {
         reach = Math.max(0.0, wall.getLocation().distanceTo(centre) - 2.0);
      }

      Vec3 to = null;

      for (double distance = reach; distance >= 4.0; distance -= 1.5) {
         Vec3 offset = dir.scale(distance);
         if (level.noCollision(dragon, dragon.getBoundingBox().move(offset.x, offset.y, offset.z))) {
            to = from.add(offset);
            break;
         }
      }

      if (to == null) {
         return false;
      }

      double width = dragon.getBbWidth() * 0.6;
      double height = dragon.getBbHeight() * 0.5;
      level.sendParticles(ModParticles.voidWisp(), centre.x, centre.y, centre.z, 45, width, height, width, 0.12);
      level.sendParticles(ParticleTypes.PORTAL, centre.x, centre.y, centre.z, 140, width, height, width, 0.6);
      level.sendParticles(ParticleTypes.WITCH, centre.x, centre.y, centre.z, 25, width, height, width, 0.05);
      line(level, ModParticles.voidWisp(), centre, to.add(0.0, dragon.getBbHeight() * 0.5, 0.0), 1.2, 2, width * 0.4, 0.02);
      sound(level, from, ModSounds.get("ability.void_blink"), 2.0F, 0.95F + level.getRandom().nextFloat() * 0.1F);
      dragon.teleportWithRider(to);
      blinkArrival(dragon, level);
      return true;
   }
}
