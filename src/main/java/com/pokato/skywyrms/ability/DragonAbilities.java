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
   private static final double CONE_SLOPE = 0.26;
   private static final double FX = 1.33;

   private DragonAbilities() {
   }

   public static int cost(DragonKind.Ability ability) {
      return switch (ability) {
         case LIGHTNING -> 30;
         case VENOM_SPIT -> 25;
         case VOID_BLINK -> 40;
         case SUN_BEAM -> 35;
         default -> 0;
      };
   }

   public static int cooldown(DragonKind.Ability ability) {
      return switch (ability) {
         case LIGHTNING -> 14;
         case VENOM_SPIT -> 12;
         case VOID_BLINK -> 24;
         case SUN_BEAM -> 16;
         default -> 0;
      };
   }

   public static double aiRange(DragonKind.Ability ability) {
      return switch (ability) {
         case LIGHTNING -> 34.0;
         case VENOM_SPIT -> 29.0;
         case VOID_BLINK -> 26.0;
         case SUN_BEAM -> 30.0;
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

   public static void breathParticles(DragonEntity dragon) {
      Level level = dragon.level();
      RandomSource random = dragon.getRandom();
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
         case SUN_BEAM -> sunBeam(dragon, level, mouth);
         default -> false;
      };
   }

   /**
    * Solar wyrm: a straight beam of sunlight. It passes through every enemy on the line (up to the first
    * solid block), blinds them briefly and makes them glow; undead take double damage and catch fire.
    */
   private static boolean sunBeam(DragonEntity dragon, ServerLevel level, Vec3 mouth) {
      double range = 32.0;
      LivingEntity user = dragon.abilityUser();
      Vec3 from = user != dragon ? user.getEyePosition() : mouth;
      Vec3 dir = direction(from, dragon.aimPoint(range), dragon);
      Vec3 end = from.add(dir.scale(range));
      BlockHitResult block = clipBlocks(dragon, from, end, Fluid.NONE);
      if (block.getType() != Type.MISS) {
         end = block.getLocation();
      }

      Vec3 beamEnd = end;
      line(level, ParticleTypes.END_ROD, mouth, beamEnd, 0.35, 1, 0.05, 0.0);
      line(level, ParticleTypes.WAX_ON, mouth, beamEnd, 0.6, 2, 0.18, 0.02);
      line(level, ParticleTypes.GLOW, mouth, beamEnd, 0.9, 1, 0.25, 0.0);
      level.sendParticles(ParticleTypes.END_ROD, beamEnd.x, beamEnd.y, beamEnd.z, 30, 0.6, 0.6, 0.6, 0.08);
      level.sendParticles(ParticleTypes.END_ROD, mouth.x, mouth.y, mouth.z, 12, 0.3, 0.3, 0.3, 0.04);
      sound(level, mouth, ModSounds.get("ability.sun_beam"), 2.5F, 0.95F + level.getRandom().nextFloat() * 0.1F);
      accentBurst(dragon, level, beamEnd, 18);
      float power = dragon.abilityPower();
      AABB search = new AABB(from, beamEnd).inflate(1.5);

      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, search, dragon::canAbilityHit)) {
         if (target != user && target.getBoundingBox().inflate(0.6).clip(from, beamEnd).isPresent()) {
            boolean undead = target.typeHolder().is(EntityTypeTags.UNDEAD);
            target.hurtServer(level, dragon.abilityDamage(DamageTypes.INDIRECT_MAGIC), (undead ? 20.0F : 10.0F) * power);
            if (undead) {
               target.igniteForSeconds(5.0F);
            }

            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0), dragon);
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0), dragon);
            infuse(dragon, level, target);
         }
      }

      return true;
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
