package com.pokato.skywyrms.entity.ai;

import com.pokato.skywyrms.entity.DragonEntity;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.Vec3;

public class WanderFlightGoal extends Goal {
   private final DragonEntity dragon;
   private Vec3 home = Vec3.ZERO;
   private Vec3 waypoint = Vec3.ZERO;
   private int legs;
   private int ticks;
   private int legTicks;
   private boolean landing;

   public WanderFlightGoal(DragonEntity dragon) {
      this.dragon = dragon;
      this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
   }

   @Override
   public boolean canUse() {
      DragonEntity d = this.dragon;
      if (d.isTame() || d.isVehicle() || d.isPassenger() || d.isLeashed() || d.getTarget() != null || !d.onGround() || d.isInWater()) {
         return false;
      } else if (d.flightRequested) {
         d.flightRequested = false;
         return true;
      } else {
         return d.getRandom().nextInt(reducedTickDelay(800)) != 0 ? false : d.level().canSeeSky(d.blockPosition().above(3));
      }
   }

   @Override
   public boolean canContinueToUse() {
      DragonEntity d = this.dragon;
      return d.isAiFlying() && d.isAlive() && !d.isVehicle() && !d.isTame() && d.getTarget() == null;
   }

   @Override
   public boolean requiresUpdateEveryTick() {
      return true;
   }

   @Override
   public void start() {
      DragonEntity d = this.dragon;
      this.home = d.position();
      this.legs = 2 + d.getRandom().nextInt(4);
      this.ticks = 0;
      this.landing = false;
      d.getNavigation().stop();
      d.setAiFlying(true);
      d.beginTakeoff(0.6);
      this.nextWaypoint();
   }

   @Override
   public void stop() {
      this.dragon.setAiFlying(false);
   }

   @Override
   public void tick() {
      DragonEntity d = this.dragon;
      if (!d.isTakingOff()) {
         this.ticks++;
         this.legTicks++;
         if (!this.landing || this.legTicks <= 12 || !d.onGround() && !d.isInWater()) {
            Vec3 to = this.waypoint.subtract(d.position());
            double distance = to.length();
            if (this.landing) {
               if (this.legTicks > 320) {
                  this.beginLanding();
                  return;
               }
            } else {
               boolean blocked = d.horizontalCollision && this.legTicks > 20;
               if (distance < 3.5 || this.legTicks > 260 || blocked) {
                  if (--this.legs > 0 && this.ticks <= 1500) {
                     this.nextWaypoint();
                  } else {
                     this.beginLanding();
                  }

                  return;
               }
            }

            double cruise = d.flightTopSpeed() * (this.landing ? 0.4 : 0.55);
            if (this.landing && distance < 7.0) {
               cruise *= Math.max(0.35, distance / 7.0);
            }

            Vec3 desired = to.scale(cruise / Math.max(distance, 0.001));
            desired = new Vec3(desired.x, Mth.clamp(desired.y, this.landing ? -0.16 : -0.11, 0.2), desired.z);
            Vec3 velocity = d.getDeltaMovement();
            velocity = velocity.add(desired.subtract(velocity).scale(0.07));
            d.setDeltaMovement(velocity);
            if (velocity.horizontalDistanceSqr() > 1.0E-4) {
               float yaw = (float)(Mth.atan2(velocity.z, velocity.x) * (180.0 / Math.PI)) - 90.0F;
               d.setYRot(Mth.approachDegrees(d.getYRot(), yaw, 5.0F));
               d.yBodyRot = d.getYRot();
               d.yHeadRot = d.getYRot();
            }

            d.setXRot(0.0F);
         } else {
            d.setAiFlying(false);
            d.setFlying(false);
            d.setDeltaMovement(Vec3.ZERO);
         }
      }
   }

   private int groundAt(int x, int z) {
      return this.dragon.level().getHeight(Types.MOTION_BLOCKING, x, z);
   }

   private void nextWaypoint() {
      DragonEntity d = this.dragon;
      Level level = d.level();
      RandomSource random = d.getRandom();
      this.legTicks = 0;

      for (int attempt = 0; attempt < 8; attempt++) {
         double angle = random.nextDouble() * Math.PI * 2.0;
         double radius = 14.0 + random.nextDouble() * 22.0;
         int x = Mth.floor(this.home.x + Math.cos(angle) * radius);
         int z = Mth.floor(this.home.z + Math.sin(angle) * radius);
         if (level.hasChunkAt(new BlockPos(x, 0, z))) {
            int ground = this.groundAt(x, z);
            if (ground > level.getMinY() + 1) {
               double y = Math.min(level.getMaxY() - 12.0, Math.max(ground, this.home.y) + 9.0 + random.nextInt(14));
               this.waypoint = new Vec3(x + 0.5, y, z + 0.5);
               return;
            }
         }
      }

      this.beginLanding();
   }

   private void beginLanding() {
      DragonEntity d = this.dragon;
      Level level = d.level();
      RandomSource random = d.getRandom();
      this.landing = true;
      this.legTicks = 0;
      Vec3 spot = this.home;

      for (int attempt = 0; attempt < 10; attempt++) {
         int x = Mth.floor(this.home.x + (random.nextDouble() - 0.5) * 24.0);
         int z = Mth.floor(this.home.z + (random.nextDouble() - 0.5) * 24.0);
         if (level.hasChunkAt(new BlockPos(x, 0, z))) {
            int ground = this.groundAt(x, z);
            if (ground > level.getMinY() + 1) {
               BlockPos surface = new BlockPos(x, ground - 1, z);
               if (level.getFluidState(surface).isEmpty()) {
                  spot = new Vec3(x + 0.5, ground, z + 0.5);
                  break;
               }
            }
         }
      }

      this.waypoint = spot;
   }
}
