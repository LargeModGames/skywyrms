package com.pokato.skywyrms.entity.ai;

import com.pokato.skywyrms.ability.DragonAbilities;
import com.pokato.skywyrms.entity.DragonEntity;
import com.pokato.skywyrms.entity.DragonKind;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

public class AbilityAttackGoal extends Goal {
   private final DragonEntity dragon;

   public AbilityAttackGoal(DragonEntity dragon) {
      this.dragon = dragon;
   }

   @Override
   public boolean canUse() {
      LivingEntity target = this.dragon.getTarget();
      return target != null && target.isAlive() && this.dragon.getControllingPassenger() == null && !this.dragon.isInSittingPose() && !this.dragon.isBaby();
   }

   @Override
   public boolean requiresUpdateEveryTick() {
      return true;
   }

   @Override
   public void stop() {
      this.dragon.stopBreath();
   }

   @Override
   public void tick() {
      LivingEntity target = this.dragon.getTarget();
      if (target != null) {
         DragonKind.Ability ability = this.dragon.getKind().ability();
         if (this.dragon.isBreathing()) {
            this.dragon.getLookControl().setLookAt(target, 30.0F, 30.0F);
         } else if (this.dragon.aiAbilityCooldown() <= 0 && this.dragon.abilityReady()) {
            double range = DragonAbilities.aiRange(ability);
            double distanceSqr = this.dragon.distanceToSqr(target);
            if (!(distanceSqr > range * range) && this.dragon.hasLineOfSight(target)) {
               this.dragon.getLookControl().setLookAt(target, 30.0F, 30.0F);
               boolean used;
               if (ability.held()) {
                  used = this.dragon.aiBreath(30 + this.dragon.getRandom().nextInt(25));
               } else if (ability == DragonKind.Ability.VOID_BLINK && distanceSqr < 49.0) {
                  used = false;
               } else {
                  used = this.dragon.castAbility();
               }

               if (used) {
                  this.dragon.setAiAbilityCooldown(110 + this.dragon.getRandom().nextInt(90));
               } else {
                  this.dragon.setAiAbilityCooldown(20);
               }
            }
         }
      }
   }
}
