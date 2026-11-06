package com.pokato.skywyrms.client;

import com.pokato.skywyrms.ability.DragonAbilities;
import com.pokato.skywyrms.entity.DragonEntity;
import com.pokato.skywyrms.entity.DragonKind;
import java.util.Optional;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

/**
 * Draws a small golden ring where the rider's beam would land, so they can aim even when the dragon
 * covers the crosshair. Client side only; uses the same range and hit width as the server's sun beam.
 */
public final class AimMarker {
   private static final int CORE_COLOR = 0xFFF4C0;
   private static final int RING_COLOR = 0xFFC83C;
   private static final int RING_POINTS = 10;
   private static final double RING_RADIUS = 0.5;

   private AimMarker() {
   }

   public static void tick(LocalPlayer player, DragonEntity dragon, int ridingTicks) {
      if (dragon.getKind().ability() != DragonKind.Ability.SUN_BEAM || ridingTicks % 2 != 0) {
         return;
      }

      ClientLevel level = (ClientLevel)player.level();
      Vec3 eye = player.getEyePosition();
      Vec3 look = player.getLookAngle();
      Vec3 end = eye.add(look.scale(DragonAbilities.SUN_BEAM_RANGE));
      BlockHitResult block = level.clip(new ClipContext(eye, end, Block.COLLIDER, Fluid.NONE, player));
      if (block.getType() != Type.MISS) {
         end = block.getLocation();
      }

      Vec3 target = end;
      double best = eye.distanceToSqr(end);
      AABB search = new AABB(eye, end).inflate(DragonAbilities.SUN_BEAM_WIDTH + 1.0);

      for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, search, e -> e != player && e != dragon && e.isAlive())) {
         Optional<Vec3> hit = entity.getBoundingBox().inflate(DragonAbilities.SUN_BEAM_WIDTH).clip(eye, end);
         if (hit.isPresent() && eye.distanceToSqr(hit.get()) < best) {
            best = eye.distanceToSqr(hit.get());
            target = entity.getBoundingBox().getCenter();
         }
      }

      // ring in the plane facing the rider; grows with distance so it looks about the same size on screen
      double distance = Math.sqrt(eye.distanceToSqr(target));
      double grow = Math.max(1.0, distance / 10.0);
      DustParticleOptions core = new DustParticleOptions(CORE_COLOR, (float)Math.min(4.0, 1.4 * grow));
      DustParticleOptions ring = new DustParticleOptions(RING_COLOR, (float)Math.min(4.0, 1.0 * grow));
      double radius = RING_RADIUS * grow;
      Vec3 side = look.cross(new Vec3(0.0, 1.0, 0.0));
      side = side.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : side.normalize();
      Vec3 up = side.cross(look).normalize();
      level.addAlwaysVisibleParticle(core, true, target.x, target.y, target.z, 0.0, 0.0, 0.0);

      for (int i = 0; i < RING_POINTS; i++) {
         double a = Math.PI * 2.0 * i / RING_POINTS;
         Vec3 p = target.add(side.scale(Math.cos(a) * radius)).add(up.scale(Math.sin(a) * radius));
         level.addAlwaysVisibleParticle(ring, true, p.x, p.y, p.z, 0.0, 0.0, 0.0);
      }
   }
}
