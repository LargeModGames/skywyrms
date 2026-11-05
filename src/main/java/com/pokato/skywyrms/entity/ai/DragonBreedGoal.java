package com.pokato.skywyrms.entity.ai;

import com.pokato.skywyrms.entity.DragonEntity;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;

public class DragonBreedGoal extends Goal {
   private static final int LOVE_TIME = 60;
   private final DragonEntity dragon;
   private DragonEntity partner;
   private int loveTime;

   public DragonBreedGoal(DragonEntity dragon) {
      this.dragon = dragon;
      this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
   }

   @Override
   public boolean canUse() {
      if (this.dragon.isInLove() && !this.dragon.isVehicle()) {
         this.partner = this.findPartner();
         return this.partner != null;
      } else {
         return false;
      }
   }

   @Override
   public boolean canContinueToUse() {
      return this.partner != null && this.partner.isAlive() && this.partner.isInLove() && this.dragon.isInLove() && this.loveTime < 400;
   }

   @Override
   public void stop() {
      this.partner = null;
      this.loveTime = 0;
      this.dragon.getNavigation().stop();
   }

   @Override
   public boolean requiresUpdateEveryTick() {
      return true;
   }

   private double breedingDistance(DragonEntity other) {
      double reach = (this.dragon.getBbWidth() + other.getBbWidth()) * 0.5 + 2.5;
      return reach * reach;
   }

   @Override
   public void tick() {
      this.dragon.getLookControl().setLookAt(this.partner, 10.0F, this.dragon.getMaxHeadXRot());
      double distance = this.dragon.distanceToSqr(this.partner);
      boolean close = distance < this.breedingDistance(this.partner);
      if (!close && !this.dragon.isOrderedToSit()) {
         this.dragon.getNavigation().moveTo(this.partner, 1.0);
      } else if (close) {
         this.dragon.getNavigation().stop();
      }

      this.loveTime++;
      if (close && this.loveTime >= this.adjustedTickDelay(60) && this.dragon.level() instanceof ServerLevel level && this.dragon.canMate(this.partner)) {
         this.dragon.spawnChildFromBreeding(level, this.partner);
         this.partner = null;
      }
   }

   private DragonEntity findPartner() {
      List<DragonEntity> nearby = this.dragon
         .level()
         .getEntitiesOfClass(DragonEntity.class, this.dragon.getBoundingBox().inflate(24.0), otherx -> otherx != this.dragon && this.dragon.canMate(otherx));
      DragonEntity best = null;
      double bestDistance = Double.MAX_VALUE;

      for (DragonEntity other : nearby) {
         double distance = this.dragon.distanceToSqr(other);
         if (distance < bestDistance) {
            bestDistance = distance;
            best = other;
         }
      }

      return best;
   }
}
