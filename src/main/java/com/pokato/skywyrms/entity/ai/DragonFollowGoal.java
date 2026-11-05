package com.pokato.skywyrms.entity.ai;

import com.pokato.skywyrms.entity.DragonEntity;
import com.pokato.skywyrms.entity.DragonKind;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class DragonFollowGoal extends Goal {
   private static final double START = 14.0;
   private static final double STOP = 6.0;
   private static final double FLY_DISTANCE = 22.0;
   private static final double FLY_HEIGHT = 5.0;
   private static final double AIR_START = 9.0;
   private static final double CATCH_UP = 96.0;
   private static final double CATCH_UP_HATCHLING = 40.0;
   private static final int STUCK_TICKS = 240;
   private final DragonEntity dragon;
   private LivingEntity owner;
   private DragonFollowGoal.Mode mode = DragonFollowGoal.Mode.WALK;
   private boolean done;
   private int modeTicks;
   private int recalcPath;
   private int failedPaths;
   private float oldWaterCost;
   private Vec3 landing;
   private int ownerAirTicks;
   private double bestDistance;
   private int noProgressTicks;
   private double side = 1.0;
   private Vec3 ownerVel = Vec3.ZERO;
   private Vec3 lastOwnerPos;

   public DragonFollowGoal(DragonEntity dragon) {
      this.dragon = dragon;
      this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
   }

   private LivingEntity ownerIfFollowable() {
      DragonEntity d = this.dragon;
      if (d.isTame() && !d.unableToMoveToOwner() && !d.isVehicle() && d.isAlive()) {
         LivingEntity owner = d.getOwner();
         return owner != null && owner.isAlive() && !owner.isSpectator() && owner.level() == d.level() ? owner : null;
      } else {
         return null;
      }
   }

   @Override
   public boolean canUse() {
      LivingEntity owner = this.ownerIfFollowable();
      if (owner == null) {
         return false;
      } else {
         double distance = this.dragon.distanceTo(owner);
         if (!(distance < 14.0) || this.canFly() && isAirborne(owner) && distance > 9.0) {
            this.owner = owner;
            return true;
         } else {
            return false;
         }
      }
   }

   @Override
   public boolean canContinueToUse() {
      return !this.done && this.owner != null && this.ownerIfFollowable() == this.owner;
   }

   @Override
   public boolean requiresUpdateEveryTick() {
      return true;
   }

   @Override
   public void start() {
      DragonEntity d = this.dragon;
      this.done = false;
      this.recalcPath = 0;
      this.failedPaths = 0;
      this.landing = null;
      this.ownerAirTicks = 0;
      this.bestDistance = d.distanceTo(this.owner);
      this.noProgressTicks = 0;
      this.side = d.getRandom().nextBoolean() ? 1.0 : -1.0;
      this.ownerVel = Vec3.ZERO;
      this.lastOwnerPos = null;
      this.oldWaterCost = d.getPathfindingMalus(PathType.WATER);
      d.setPathfindingMalus(PathType.WATER, 0.0F);
      if (d.isFlying() && this.canFly()) {
         this.setMode(DragonFollowGoal.Mode.FLY);
         d.setAiFlying(true);
      } else {
         this.setMode(DragonFollowGoal.Mode.WALK);
      }
   }

   @Override
   public void stop() {
      DragonEntity d = this.dragon;
      this.owner = null;
      this.landing = null;
      d.getNavigation().stop();
      d.setPathfindingMalus(PathType.WATER, this.oldWaterCost);
      d.setAiFlying(false);
   }

   @Override
   public void tick() {
      DragonEntity d = this.dragon;
      LivingEntity owner = this.owner;
      this.modeTicks++;
      double distance = d.distanceTo(owner);
      Vec3 ownerPos = owner.position();
      if (this.lastOwnerPos != null) {
         Vec3 step = ownerPos.subtract(this.lastOwnerPos);
         this.ownerVel = step.lengthSqr() > 64.0 ? Vec3.ZERO : this.ownerVel.scale(0.7).add(step.scale(0.3));
      }

      this.lastOwnerPos = ownerPos;
      this.ownerAirTicks = isAirborne(owner) ? this.ownerAirTicks + 1 : 0;
      boolean ownerMoving = this.ownerVel.lengthSqr() > 0.0064;
      if (!(distance < this.bestDistance - 1.5) && !ownerMoving) {
         this.noProgressTicks++;
      } else {
         this.bestDistance = distance;
         this.noProgressTicks = 0;
      }

      boolean stuck = this.noProgressTicks > 240 && distance > 14.0;
      if (!(distance > (this.canFly() ? 96.0 : 40.0)) && !stuck) {
         switch (this.mode) {
            case WALK:
               this.tickWalk(distance);
               break;
            case FLY:
               this.tickFly();
               break;
            case LAND:
               this.tickLand(distance);
         }
      } else {
         this.catchUp(true);
      }
   }

   private void tickWalk(double distance) {
      DragonEntity d = this.dragon;
      LivingEntity owner = this.owner;
      d.getLookControl().setLookAt(owner, 10.0F, d.getMaxHeadXRot());
      boolean noPath = this.failedPaths >= 2 && underOpenSky(owner);
      if (!this.canFly() || !(horizontalDistance(d, owner) > 22.0) && !(owner.getY() - d.getY() > 5.0) && this.ownerAirTicks <= 10 && !noPath) {
         if (distance <= 6.0 + d.getBbWidth()) {
            this.done = true;
         } else {
            if (--this.recalcPath <= 0) {
               this.recalcPath = this.adjustedTickDelay(10);
               boolean moving = d.getNavigation().moveTo(owner, distance > 18.0 ? 1.45 : 1.15);
               Path path = d.getNavigation().getPath();
               this.failedPaths = moving && path != null && path.canReach() ? 0 : this.failedPaths + 1;
            }
         }
      } else {
         this.takeOff();
      }
   }

   private void takeOff() {
      DragonEntity d = this.dragon;
      d.getNavigation().stop();
      d.setAiFlying(true);
      if (d.onGround() && !d.isInWater()) {
         d.beginTakeoff(0.6);
      } else {
         d.setFlying(true);
         Vec3 motion = d.getDeltaMovement();
         d.setDeltaMovement(motion.x, Math.max(motion.y, 0.6), motion.z);
         DragonEntity.markMoved(d);
      }

      this.landing = null;
      this.setMode(DragonFollowGoal.Mode.FLY);
   }

   private void tickFly() {
      DragonEntity d = this.dragon;
      if (!d.isTakingOff()) {
         LivingEntity owner = this.owner;
         double horizontal = horizontalDistance(d, owner);
         boolean ownerFlying = this.ownerAirTicks > 0;
         Vec3 ownerFlat = new Vec3(this.ownerVel.x, 0.0, this.ownerVel.z);
         Vec3 target;
         if (ownerFlying && ownerFlat.lengthSqr() > 0.0144) {
            Vec3 heading = ownerFlat.normalize();
            Vec3 right = new Vec3(-heading.z, 0.0, heading.x);
            target = owner.position().add(heading.scale(-9.0)).add(right.scale(this.side * 7.0)).add(0.0, 3.0, 0.0);
         } else if (ownerFlying) {
            Vec3 out = d.position().subtract(owner.position());
            out = new Vec3(out.x, 0.0, out.z);
            out = out.lengthSqr() < 0.001 ? new Vec3(this.side, 0.0, 0.0) : out.normalize();
            target = owner.position().add(out.scale(10.0)).add(0.0, 2.0, 0.0);
         } else if (horizontal > 12.0) {
            double cruiseY = Math.max(Math.max(owner.getY(), surfaceAt(owner)) + 7.0, d.getY() - d.heightAboveGround(12.0) + 8.0);
            target = new Vec3(owner.getX(), cruiseY, owner.getZ());
         } else {
            this.landing = this.findLanding();
            if (this.landing != null) {
               this.setMode(DragonFollowGoal.Mode.LAND);
               return;
            }

            if (!underOpenSky(owner) && this.modeTicks > 60) {
               this.catchUp(false);
               return;
            }

            double angle = this.modeTicks * 0.035 * this.side;
            double y = Math.max(owner.getY(), surfaceAt(owner)) + 7.0;
            target = new Vec3(owner.getX() + Math.cos(angle) * 9.0, y, owner.getZ() + Math.sin(angle) * 9.0);
         }

         double toTarget = target.distanceTo(d.position());
         double top = d.flightTopSpeed();
         double speed = top * Mth.clamp(toTarget / 20.0, 0.3, 1.0);
         if (toTarget > 32.0) {
            speed = top * Math.min(1.8, 1.0 + (toTarget - 32.0) / 40.0);
         }

         if (ownerFlying) {
            double ownerSpeed = this.ownerVel.length();
            speed = Math.max(speed, Math.min(top * 1.8, ownerSpeed * 1.05));
         }

         this.steer(target, speed, 0.1, true);
      }
   }

   private void tickLand(double distance) {
      DragonEntity d = this.dragon;
      LivingEntity owner = this.owner;
      if (this.ownerAirTicks <= 10 && this.landing != null && !(this.landing.distanceTo(owner.position()) > 12.0)) {
         if (this.modeTicks <= 12 || !d.onGround() && !d.isInWater()) {
            if (this.modeTicks > 300) {
               this.landing = null;
               this.setMode(DragonFollowGoal.Mode.FLY);
            } else {
               Vec3 to = this.landing.subtract(d.position());
               double horizontal = Math.sqrt(to.x * to.x + to.z * to.z);
               Vec3 target = horizontal > 2.0 ? this.landing.add(0.0, Math.min(6.0, horizontal * 0.5), 0.0) : this.landing;
               double speed = d.flightTopSpeed() * Mth.clamp(to.length() / 14.0, 0.15, 0.5);
               this.steer(target, speed, 0.12, false);
               if (horizontal < 4.0) {
                  d.getLookControl().setLookAt(owner, 10.0F, d.getMaxHeadXRot());
               }
            }
         } else {
            this.touchDown();
            this.done = distance <= 14.0;
         }
      } else {
         this.landing = null;
         this.setMode(DragonFollowGoal.Mode.FLY);
      }
   }

   private void touchDown() {
      DragonEntity d = this.dragon;
      d.setAiFlying(false);
      d.setFlying(false);
      Vec3 motion = d.getDeltaMovement();
      d.setDeltaMovement(motion.x * 0.2, 0.0, motion.z * 0.2);
      this.landing = null;
      this.recalcPath = 0;
      this.failedPaths = 0;
      this.setMode(DragonFollowGoal.Mode.WALK);
   }

   private Vec3 findLanding() {
      DragonEntity d = this.dragon;
      LivingEntity owner = this.owner;
      Level level = d.level();
      Vec3 from = d.position().subtract(owner.position());
      double base = from.horizontalDistanceSqr() > 1.0E-4 ? Math.atan2(from.z, from.x) : d.getRandom().nextDouble() * Math.PI * 2.0;
      double reach = d.getBbWidth() + 6.5;

      for (int pass = 0; pass < 2; pass++) {
         for (int i = 0; i < 18; i++) {
            double angle = base + (i + 1) / 2 * (i % 2 == 0 ? 1.0 : -1.0) * 0.35;
            double radius = reach + i / 6 * 2.0;
            int x = Mth.floor(owner.getX() + Math.cos(angle) * radius);
            int z = Mth.floor(owner.getZ() + Math.sin(angle) * radius);
            if (level.hasChunkAt(new BlockPos(x, 0, z))) {
               int ground = level.getHeight(Types.MOTION_BLOCKING, x, z);
               if ((pass != 0 || ground == level.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, x, z))
                  && ground > level.getMinY() + 1
                  && !(Math.abs(ground - owner.getY()) > 4.0)
                  && (level.getFluidState(new BlockPos(x, ground - 1, z)).isEmpty() || d.hasElement(DragonKind.TIDE))) {
                  Vec3 spot = new Vec3(x + 0.5, ground, z + 0.5);
                  AABB box = d.getBoundingBox().move(spot.subtract(d.position()));
                  if (level.noCollision(d, box) && level.noCollision(d, box.move(0.0, 3.0, 0.0))) {
                     return spot;
                  }
               }
            }
         }
      }

      return null;
   }

   private void steer(Vec3 target, double speed, double response, boolean keepClear) {
      DragonEntity d = this.dragon;
      Vec3 to = target.subtract(d.position());
      double length = to.length();
      Vec3 desired = length < 0.001 ? Vec3.ZERO : to.scale(Math.min(speed, length * 0.25) / length);
      if (keepClear) {
         boolean blocked = d.horizontalCollision || this.blockedAhead(desired);
         if (blocked) {
            desired = new Vec3(desired.x * 0.4, Math.max(desired.y, 0.4), desired.z * 0.4);
         } else if (d.heightAboveGround(4.0) < 3.0 && desired.y < 0.15) {
            desired = new Vec3(desired.x, 0.15, desired.z);
         }
      }

      desired = new Vec3(desired.x, Mth.clamp(desired.y, -0.35, 0.45), desired.z);
      Vec3 velocity = d.getDeltaMovement();
      velocity = velocity.add(desired.subtract(velocity).scale(response));
      d.setDeltaMovement(velocity);
      float yaw;
      if (velocity.horizontalDistanceSqr() > 0.004) {
         yaw = (float)(Mth.atan2(velocity.z, velocity.x) * (180.0 / Math.PI)) - 90.0F;
      } else {
         Vec3 look = this.owner.position().subtract(d.position());
         yaw = (float)(Mth.atan2(look.z, look.x) * (180.0 / Math.PI)) - 90.0F;
      }

      d.setYRot(Mth.approachDegrees(d.getYRot(), yaw, 6.0F));
      d.yBodyRot = d.getYRot();
      d.yHeadRot = d.getYRot();
      d.setXRot(0.0F);
   }

   private boolean blockedAhead(Vec3 desired) {
      DragonEntity d = this.dragon;
      Vec3 flat = new Vec3(desired.x, 0.0, desired.z);
      if (flat.lengthSqr() < 1.0E-4) {
         return false;
      }

      Vec3 dir = flat.normalize();
      double reach = d.getBbWidth() * 0.5 + 4.0;

      for (double y : new double[]{0.4, d.getBbHeight() * 0.6}) {
         Vec3 from = d.position().add(0.0, y, 0.0);
         if (d.level().clip(new ClipContext(from, from.add(dir.scale(reach)), Block.COLLIDER, Fluid.NONE, d)).getType() != Type.MISS) {
            return true;
         }
      }

      return false;
   }

   private void catchUp(boolean viaAir) {
      DragonEntity d = this.dragon;
      LivingEntity owner = this.owner;
      boolean moved = false;
      if (viaAir && this.canFly()) {
         Vec3 heading = this.heading(owner);

         for (double back = 24.0; back >= 12.0 && !moved; back -= 4.0) {
            Vec3 spot = owner.position().add(heading.scale(-back)).add(0.0, 5.0, 0.0);
            BlockPos column = BlockPos.containing(spot);
            if (d.level().hasChunkAt(column)
               && !(spot.y < d.level().getHeight(Types.MOTION_BLOCKING, column.getX(), column.getZ()) + 2.0)
               && d.level().noCollision(d, d.getBoundingBox().move(spot.subtract(d.position())))) {
               d.teleportTo(spot.x, spot.y, spot.z);
               d.setAiFlying(true);
               d.setFlying(true);
               d.setDeltaMovement(this.ownerVel);
               DragonEntity.markMoved(d);
               this.setMode(DragonFollowGoal.Mode.FLY);
               moved = true;
            }
         }
      }

      if (!moved) {
         d.setAiFlying(false);
         d.setFlying(false);
         d.tryToTeleportToOwner();
         this.setMode(DragonFollowGoal.Mode.WALK);
      }

      this.bestDistance = d.distanceTo(owner);
      this.noProgressTicks = 0;
      this.failedPaths = 0;
      this.recalcPath = 0;
   }

   private void setMode(DragonFollowGoal.Mode mode) {
      this.mode = mode;
      this.modeTicks = 0;
   }

   private boolean canFly() {
      return !this.dragon.isBaby();
   }

   private static double surfaceAt(Entity entity) {
      return entity.level().getHeight(Types.MOTION_BLOCKING, entity.getBlockX(), entity.getBlockZ());
   }

   private static boolean underOpenSky(Entity entity) {
      return entity.getY() >= entity.level().getHeight(Types.MOTION_BLOCKING_NO_LEAVES, entity.getBlockX(), entity.getBlockZ()) - 1.0;
   }

   private static double horizontalDistance(Entity a, Entity b) {
      double dx = a.getX() - b.getX();
      double dz = a.getZ() - b.getZ();
      return Math.sqrt(dx * dx + dz * dz);
   }

   private Vec3 heading(LivingEntity owner) {
      Vec3 flat = new Vec3(this.ownerVel.x, 0.0, this.ownerVel.z);
      if (flat.lengthSqr() < 0.01) {
         flat = new Vec3(owner.getLookAngle().x, 0.0, owner.getLookAngle().z);
      }

      return flat.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
   }

   public static boolean isAirborne(LivingEntity owner) {
      if (owner.isFallFlying()) {
         return true;
      } else if (owner instanceof Player player && player.getAbilities().flying) {
         return true;
      } else {
         Entity vehicle = owner.getVehicle();
         if (vehicle instanceof DragonEntity mount) {
            return mount.isFlying();
         } else {
            Entity body = vehicle != null ? vehicle : owner;
            if (!body.onGround() && !body.isInWater()) {
               Vec3 from = body.position();
               return body.level().clip(new ClipContext(from, from.add(0.0, -5.0, 0.0), Block.COLLIDER, Fluid.ANY, body)).getType() == Type.MISS;
            } else {
               return false;
            }
         }
      }
   }

   private enum Mode {
      WALK,
      FLY,
      LAND;
   }
}
