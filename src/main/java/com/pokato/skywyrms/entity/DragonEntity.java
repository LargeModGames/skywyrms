package com.pokato.skywyrms.entity;

import com.pokato.skywyrms.ability.DragonAbilities;
import com.pokato.skywyrms.entity.ai.AbilityAttackGoal;
import com.pokato.skywyrms.entity.ai.DragonBreedGoal;
import com.pokato.skywyrms.entity.ai.DragonFollowGoal;
import com.pokato.skywyrms.entity.ai.WanderFlightGoal;
import com.pokato.skywyrms.registry.ModEntities;
import com.pokato.skywyrms.registry.ModParticles;
import com.pokato.skywyrms.registry.ModSounds;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.Entity.MoveFunction;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class DragonEntity extends TamableAnimal {
   public static final int MAX_ENERGY = 100;
   private static final float BREATH_COST = 1.25F;
   private static final float BREATH_MIN_START = 15.0F;
   private static final float ENERGY_REGEN = 0.55F;
   public static final byte EVENT_ROAR = 70;
   public static final byte EVENT_ABILITY_FLASH = 71;
   public static final byte EVENT_BITE = 72;
   public static final byte EVENT_SNEEZE = 73;
   public static final byte EVENT_TAKEOFF = 74;
   public static final float PURE_POWER = 1.15F;
   public static final float PURE_REGEN = 1.25F;
   private static final EntityDataAccessor<Boolean> DATA_FLYING = SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> DATA_SADDLED = SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> DATA_BREATHING = SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> DATA_ENERGY = SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_BLOOD = SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Float> DATA_GROWTH = SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.FLOAT);
   public static volatile boolean descendKeyDown;
   public static volatile boolean sprintKeyDown;
   public static volatile boolean ascendKeyDown;
   public static final float SPRINT_BOOST = 1.3F;
   public static final float DIVE_MAX = 2.4F;
   private static final float DIVE_GAIN = 0.016F;
   private static final float DIVE_DRAIN_LEVEL = 0.012F;
   private static final float DIVE_DRAIN_CLIMB = 0.03F;
   private static final float DIVE_PITCH = 25.0F;
   private static final float DIVE_FULL_PITCH = 70.0F;
   private final DragonKind kind;
   public final DragonAnim anim = new DragonAnim();
   private DragonRig rig;
   private float energy = 100.0F;
   private int regenDelay;
   private boolean abilityHeld;
   private int abilityCooldown;
   private int breathTicks;
   private int aiBreathTicks;
   private int aiAbilityCooldown = 60;
   private boolean aiFlying;
   private int airTicks;
   private int flyTicks;
   private int takeoffTimer;
   private double takeoffImpulse;
   private float diveBoost = 1.0F;
   private DragonGenes genes;
   private int growTicksTotal;
   private int sneezeTimer = 200;
   public boolean flightRequested;

   public DragonEntity(EntityType<? extends DragonEntity> type, Level level) {
      super(type, level);
      this.kind = ModEntities.kindOf(type);
      this.xpReward = 40;
   }

   @Override
   protected void positionRider(Entity passenger, MoveFunction move) {
      super.positionRider(passenger, move);
      double dip = this.anim.seatDip(1.0F) / 16.0F * this.kind.geo().scale * this.getScale();
      if (dip != 0.0) {
         move.accept(passenger, passenger.getX(), passenger.getY() - dip, passenger.getZ());
      }
   }

   public DragonKind getKind() {
      return this.kind;
   }

   public static Builder createAttributes(DragonKind kind) {
      return TamableAnimal.createAnimalAttributes()
         .add(Attributes.MAX_HEALTH, kind.maxHealth())
         .add(Attributes.MOVEMENT_SPEED, kind.walkSpeed())
         .add(Attributes.FLYING_SPEED, kind.flyingSpeed())
         .add(Attributes.ATTACK_DAMAGE, kind.attackDamage())
         .add(Attributes.FOLLOW_RANGE, 40.0)
         .add(Attributes.ARMOR, kind.armor())
         .add(Attributes.KNOCKBACK_RESISTANCE, 0.85)
         .add(Attributes.STEP_HEIGHT, 1.25)
         .add(Attributes.CAMERA_DISTANCE, 12.5);
   }

   @Override
   protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(DATA_FLYING, false);
      builder.define(DATA_SADDLED, false);
      builder.define(DATA_BREATHING, false);
      builder.define(DATA_ENERGY, 100);
      builder.define(DATA_BLOOD, this.kindForData().ordinal());
      builder.define(DATA_GROWTH, 1.0F);
   }

   private DragonKind kindForData() {
      return this.kind != null ? this.kind : ModEntities.kindOf(this.getType());
   }

   public DragonGenes genes() {
      if (this.genes == null) {
         this.setGenes(DragonGenes.pure(this.kind), false);
      }

      return this.genes;
   }

   public void setGenes(DragonGenes genes, boolean heal) {
      this.genes = genes.body() == this.kind ? genes : genes.withBody(this.kind);
      this.entityData.set(DATA_BLOOD, this.genes.blood().ordinal());
      this.applyGeneStats(heal);
   }

   private void applyGeneStats(boolean heal) {
      DragonGenes g = this.genes;
      setBase(this, Attributes.MAX_HEALTH, g.mix(DragonKind::maxHealth) * g.healthFactor());
      setBase(this, Attributes.ATTACK_DAMAGE, g.mix(DragonKind::attackDamage) * g.powerFactor());
      setBase(this, Attributes.ARMOR, g.mix(DragonKind::armor));
      setBase(this, Attributes.FLYING_SPEED, g.mix(DragonKind::flyingSpeed) * g.speedFactor());
      setBase(this, Attributes.MOVEMENT_SPEED, g.mix(DragonKind::walkSpeed) * g.speedFactor());
      if (heal) {
         this.setHealth(this.getMaxHealth());
      } else if (this.getHealth() > this.getMaxHealth()) {
         this.setHealth(this.getMaxHealth());
      }
   }

   private static void setBase(LivingEntity entity, Holder<Attribute> attribute, double value) {
      AttributeInstance instance = entity.getAttribute(attribute);
      if (instance != null) {
         instance.setBaseValue(value);
      }
   }

   public DragonKind blood() {
      return DragonKind.byOrdinal(this.entityData.get(DATA_BLOOD));
   }

   public boolean isPurebred() {
      return this.blood() == this.kind;
   }

   public boolean hasElement(DragonKind element) {
      return this.kind == element || this.blood() == element;
   }

   public float abilityPower() {
      float power = this.genes().powerFactor();
      return this.isPurebred() ? power * 1.15F : power;
   }

   @Override
   protected Component getTypeName() {
      Component base = super.getTypeName();
      return this.isPurebred()
         ? base
         : Component.translatable("entity.skywyrms.hybrid", Component.translatable("element.skywyrms." + this.blood().element() + ".adjective"), base);
   }

   public float growth() {
      return this.isBaby() ? Mth.clamp(this.entityData.get(DATA_GROWTH), 0.0F, 1.0F) : 1.0F;
   }

   @Override
   public float getAgeScale() {
      return this.isBaby() ? 0.32F + 0.68F * this.growth() : 1.0F;
   }

   public void makeHatchling(int ticks) {
      this.growTicksTotal = Math.max(1, ticks);
      this.setAge(-this.growTicksTotal);
      this.entityData.set(DATA_GROWTH, 0.0F);
      this.refreshDimensions();
   }

   private void updateGrowth() {
      if (!this.isBaby()) {
         if (this.entityData.get(DATA_GROWTH) < 1.0F) {
            this.entityData.set(DATA_GROWTH, 1.0F);
         }
      } else {
         if (this.growTicksTotal <= 0) {
            this.growTicksTotal = Math.max(-this.getAge(), 1);
         }

         float growth = Mth.clamp(1.0F - (float)(-this.getAge()) / this.growTicksTotal, 0.0F, 1.0F);
         if (Math.abs(growth - this.entityData.get(DATA_GROWTH)) >= 0.004F) {
            this.entityData.set(DATA_GROWTH, growth);
         }
      }
   }

   @Override
   public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
      super.onSyncedDataUpdated(accessor);
      if (DATA_GROWTH.equals(accessor)) {
         this.refreshDimensions();
      }
   }

   @Override
   protected void ageBoundaryReached() {
      super.ageBoundaryReached();
      if (!this.isBaby()) {
         this.entityData.set(DATA_GROWTH, 1.0F);
         this.refreshDimensions();
         if (!this.level().isClientSide() && this.growTicksTotal > 0) {
            this.growTicksTotal = 0;
            this.roar();
            if (this.getOwner() instanceof Player owner) {
               message(owner, Component.translatable("message.skywyrms.grown_up", this.getDisplayName()));
            }
         }
      }
   }

   public static boolean isLoveFood(ItemStack stack) {
      return stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE);
   }

   @Override
   public boolean canMate(Animal other) {
      return other != this
         && other instanceof DragonEntity mate
         && this.isTame()
         && mate.isTame()
         && this.isInLove()
         && mate.isInLove()
         && !this.isBaby()
         && !mate.isBaby();
   }

   @Override
   public void spawnChildFromBreeding(ServerLevel level, Animal partner) {
      if (partner instanceof DragonEntity mate) {
         DragonGenes child = DragonGenes.cross(this.genes(), mate.genes(), this.random);
         ServerPlayer cause = this.getLoveCause() != null ? this.getLoveCause() : mate.getLoveCause();
         UUID owner = cause != null ? cause.getUUID() : ownerUuid(this);
         Vec3 between = this.position().add(mate.position()).scale(0.5);
         Vec3 line = mate.position().subtract(this.position());
         Vec3 side = new Vec3(-line.z, 0.0, line.x);
         side = side.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : side.normalize();
         double out = Math.max(this.getBbWidth(), mate.getBbWidth()) * 0.5 + 1.2;
         DragonEggEntity egg = DragonEggEntity.create(level, child, owner, 0);
         if (egg != null) {
            egg.placeNear(level, between.add(side.scale(this.random.nextBoolean() ? out : -out)));
            level.addFreshEntity(egg);
            if (cause != null) {
               message(cause, Component.translatable("message.skywyrms.egg_laid", egg.getDisplayName()));
            }
         }

         this.finalizeSpawnChildFromBreeding(level, partner, null);
         this.roar();
      }
   }

   public static UUID ownerUuid(TamableAnimal animal) {
      EntityReference<LivingEntity> reference = animal.getOwnerReference();
      return reference == null ? null : reference.getUUID();
   }

   public void tameTo(UUID uuid) {
      this.setTame(true, true);
      if (uuid != null) {
         this.setOwnerReference(EntityReference.of(uuid));
      }
   }

   @Override
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, SpawnGroupData data) {
      if (this.genes == null) {
         this.setGenes(DragonGenes.wild(this.kind, this.kind, this.random), true);
      }

      return super.finalizeSpawn(level, difficulty, reason, data);
   }

   public boolean isFlying() {
      return this.entityData.get(DATA_FLYING);
   }

   public void setFlying(boolean flying) {
      if (!flying) {
         this.takeoffTimer = 0;
      }

      this.entityData.set(DATA_FLYING, flying);
   }

   @Override
   public boolean isSaddled() {
      return this.entityData.get(DATA_SADDLED);
   }

   public void setSaddled(boolean saddled) {
      this.entityData.set(DATA_SADDLED, saddled);
   }

   public boolean isBreathing() {
      return this.entityData.get(DATA_BREATHING);
   }

   public int getEnergy() {
      return this.entityData.get(DATA_ENERGY);
   }

   public boolean isAiFlying() {
      return this.aiFlying;
   }

   public void setAiFlying(boolean aiFlying) {
      this.aiFlying = aiFlying;
   }

   @Override
   public void addAdditionalSaveData(ValueOutput output) {
      super.addAdditionalSaveData(output);
      output.putBoolean("Saddled", this.isSaddled());
      output.putFloat("Energy", this.energy);
      this.genes().save(output);
      if (this.isBaby() && this.growTicksTotal > 0) {
         output.putInt("GrowTicks", this.growTicksTotal);
      }
   }

   @Override
   public void readAdditionalSaveData(ValueInput input) {
      super.readAdditionalSaveData(input);
      this.setSaddled(input.getBooleanOr("Saddled", false));
      this.energy = Mth.clamp(input.getFloatOr("Energy", 100.0F), 0.0F, 100.0F);
      this.entityData.set(DATA_ENERGY, Math.round(this.energy));
      DragonGenes loaded = DragonGenes.load(input, this.kind);
      if (loaded != null) {
         this.setGenes(loaded, false);
      }

      this.growTicksTotal = input.getIntOr("GrowTicks", 0);
      if (this.isBaby()) {
         this.updateGrowth();
      }
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
      this.goalSelector.addGoal(2, new DragonBreedGoal(this));
      this.goalSelector.addGoal(2, new AbilityAttackGoal(this));
      this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.25, true));
      this.goalSelector.addGoal(4, new DragonFollowGoal(this));
      this.goalSelector.addGoal(5, new WanderFlightGoal(this));
      this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
      this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
      this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
      this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
      this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
   }

   @Override
   public boolean isFood(ItemStack stack) {
      return stack.is(ItemTags.MEAT) || this.kind == DragonKind.TIDE && stack.is(ItemTags.FISHES);
   }

   @Override
   public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
      return null;
   }

   @Override
   public boolean fireImmune() {
      return this.kind == null ? super.fireImmune() : this.hasElement(DragonKind.EMBER);
   }

   @Override
   public boolean canFreeze() {
      return !this.hasElement(DragonKind.FROST) && super.canFreeze();
   }

   @Override
   public boolean canBreatheUnderwater() {
      return this.hasElement(DragonKind.TIDE);
   }

   @Override
   public boolean canBeAffected(MobEffectInstance effect) {
      if (this.hasElement(DragonKind.THORN) && effect.is(MobEffects.POISON)) {
         return false;
      } else {
         return !this.hasElement(DragonKind.VOID) || !effect.is(MobEffects.LEVITATION) && !effect.is(MobEffects.WEAKNESS) ? super.canBeAffected(effect) : false;
      }
   }

   @Override
   public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
      if (this.hasElement(DragonKind.STORM) && source.is(DamageTypes.LIGHTNING_BOLT)) {
         return true;
      } else {
         return this.hasElement(DragonKind.VOID) && source.is(DamageTypes.DRAGON_BREATH) ? true : super.isInvulnerableTo(level, source);
      }
   }

   @Override
   public boolean canAttack(LivingEntity target) {
      return !this.isBaby() && super.canAttack(target);
   }

   @Override
   public void tick() {
      super.tick();
      boolean wingBeat = this.anim.tick(this);
      this.tickTakeoff();
      if (this.level().isClientSide()) {
         if (wingBeat && !this.isSilent()) {
            float size = this.getAgeScale();
            this.level()
               .playLocalSound(
                  this.getX(),
                  this.getY(),
                  this.getZ(),
                  ModSounds.get("dragon.flap"),
                  this.getSoundSource(),
                  1.3F * size,
                  this.flapPitch() / Mth.sqrt(size) * (0.92F + this.random.nextFloat() * 0.16F),
                  false
               );
         }

         this.groundEffects(wingBeat);
         if (this.isBreathing() && this.isAlive()) {
            DragonAbilities.breathParticles(this);
         }

         if (this.kind == DragonKind.SOLAR && this.isAlive()) {
            DragonAbilities.solarSparks(this);
         }

         if (this.anim.stoop > 0.35F) {
            this.diveTrail();
         }
      } else if (this.level() instanceof ServerLevel level) {
         this.serverTick(level);
      }
   }

   private void serverTick(ServerLevel level) {
      if (this.genes == null) {
         this.setGenes(DragonGenes.pure(this.kind), false);
      }

      if (this.tickCount % 10 == 0) {
         this.updateGrowth();
      }

      if (this.isBaby() && this.isAlive() && --this.sneezeTimer <= 0) {
         this.sneezeTimer = 300 + this.random.nextInt(500);
         this.broadcastAnim((byte)73);
         this.playSound(ModSounds.voice(this.kind, ModSounds.Voice.BABY), 0.8F, this.getVoicePitch());
      }

      if (this.abilityCooldown > 0) {
         this.abilityCooldown--;
      }

      LivingEntity rider = this.getControllingPassenger();
      if (rider == null) {
         this.abilityHeld = false;
         if (!this.aiFlying) {
            boolean airborne = !this.isGrounded() && !this.isInWater() && !this.isPassenger();
            this.airTicks = airborne ? this.airTicks + 1 : 0;
            boolean shouldFly = this.airTicks > 6;
            if (this.isFlying() != shouldFly) {
               this.setFlying(shouldFly);
            }
         }
      } else {
         this.airTicks = 0;
      }

      if (this.isBreathing()) {
         boolean wanted = rider != null && this.abilityHeld || this.aiBreathTicks > 0;
         if (this.aiBreathTicks > 0) {
            this.aiBreathTicks--;
         }

         this.energy -= 1.25F;
         this.regenDelay = 25;
         if (wanted && !(this.energy <= 0.0F) && this.isAlive() && !this.isInSittingPose()) {
            DragonAbilities.breathTick(this, level, this.breathTicks++);
         } else {
            this.energy = Math.max(0.0F, this.energy);
            this.stopBreath();
         }
      } else if (this.regenDelay > 0) {
         this.regenDelay--;
      } else if (this.energy < 100.0F) {
         this.energy = Math.min(100.0F, this.energy + (this.isPurebred() ? 0.6875F : 0.55F));
      }

      int shown = Math.round(this.energy);
      if (shown != this.getEnergy()) {
         this.entityData.set(DATA_ENERGY, shown);
      }

      if (this.tickCount % 100 == 0
         && this.hasElement(DragonKind.SOLAR)
         && this.getHealth() < this.getMaxHealth()
         && level.isBrightOutside()
         && level.canSeeSky(this.blockPosition().above())) {
         this.heal(1.0F);
      }

      if (this.tickCount % 20 == 0) {
         for (Entity passenger : this.getPassengers()) {
            if (passenger instanceof LivingEntity living) {
               this.applyRiderBoons(living);
            }
         }
      }
   }

   private boolean isGrounded() {
      return this.onGround() ? true : this.isNoAi() && !this.level().noCollision(this, this.getBoundingBox().move(0.0, -0.2, 0.0));
   }

   private void applyRiderBoons(LivingEntity rider) {
      if (this.hasElement(DragonKind.EMBER)) {
         rider.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 70, 0, true, false, true));
      }

      if (this.hasElement(DragonKind.TIDE)) {
         rider.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 70, 0, true, false, true));
      }

      if (this.hasElement(DragonKind.FROST)) {
         rider.setTicksFrozen(0);
      }
   }

   @Override
   protected void customServerAiStep(ServerLevel level) {
      super.customServerAiStep(level);
      if (this.aiAbilityCooldown > 0) {
         this.aiAbilityCooldown--;
      }

      if (this.isVehicle() && this.getControllingPassenger() != null) {
         this.getNavigation().stop();
      }
   }

   public void onAbilityKey(ServerPlayer player, boolean pressed) {
      if (this.getControllingPassenger() == player && this.isAlive()) {
         if (this.kind.ability().held()) {
            this.abilityHeld = pressed;
            if (pressed) {
               this.startBreath();
            } else {
               this.stopBreath();
            }
         } else if (pressed) {
            this.castAbility();
         }
      }
   }

   public boolean startBreath() {
      if (this.isBreathing()) {
         return true;
      } else if (!(this.energy < 15.0F) && !this.isInSittingPose()) {
         this.breathTicks = 0;
         this.entityData.set(DATA_BREATHING, true);
         return true;
      } else {
         return false;
      }
   }

   public void stopBreath() {
      if (this.isBreathing()) {
         this.entityData.set(DATA_BREATHING, false);
      }

      this.aiBreathTicks = 0;
   }

   public boolean aiBreath(int ticks) {
      if (this.startBreath()) {
         this.aiBreathTicks = ticks;
         return true;
      } else {
         return false;
      }
   }

   public boolean castAbility() {
      if (this.abilityCooldown <= 0 && this.level() instanceof ServerLevel level) {
         int var3 = DragonAbilities.cost(this.kind.ability());
         if (this.energy < var3) {
            return false;
         }

         if (!DragonAbilities.cast(this, level)) {
            return false;
         }

         this.energy -= var3;
         this.regenDelay = 25;
         this.abilityCooldown = DragonAbilities.cooldown(this.kind.ability());
         this.broadcastAnim((byte)71);
         return true;
      } else {
         return false;
      }
   }

   public float energy() {
      return this.energy;
   }

   public void setEnergy(float energy) {
      this.energy = Mth.clamp(energy, 0.0F, 100.0F);
      this.regenDelay = 200;
      this.entityData.set(DATA_ENERGY, Math.round(this.energy));
   }

   public boolean abilityReady() {
      return this.abilityCooldown <= 0 && !this.isBreathing();
   }

   public int aiAbilityCooldown() {
      return this.aiAbilityCooldown;
   }

   public void setAiAbilityCooldown(int ticks) {
      this.aiAbilityCooldown = ticks;
   }

   public LivingEntity abilityUser() {
      LivingEntity rider = this.getControllingPassenger();
      return rider != null ? rider : this;
   }

   public DamageSource abilityDamage(ResourceKey<DamageType> type) {
      return new DamageSource(this.level().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(type), this, this.abilityUser());
   }

   public boolean canAbilityHit(Entity target) {
      if (target != this && target.isAlive() && !target.isSpectator() && !this.hasPassenger(target) && !(target instanceof DragonEggEntity)) {
         LivingEntity owner = this.getOwner();
         if (owner != null) {
            if (target == owner) {
               return false;
            }

            if (target instanceof TamableAnimal pet && pet.getOwner() == owner) {
               return false;
            }

            if (owner instanceof Player ownerPlayer && target instanceof Player other && !ownerPlayer.canHarmPlayer(other)) {
               return false;
            }

            for (Entity passenger : target.getPassengers()) {
               if (passenger == owner) {
                  return false;
               }
            }
         }

         return !this.isAlliedTo(target);
      } else {
         return false;
      }
   }

   public Vec3 mouthPosition() {
      if (this.rig == null) {
         this.rig = new DragonRig(this.kind.geo());
      }

      return this.rig.mouth(this);
   }

   public static Vec3 viewVector(float xRot, float yRot) {
      float pitch = xRot * (float) (Math.PI / 180.0);
      float yaw = -yRot * (float) (Math.PI / 180.0);
      float cosYaw = Mth.cos(yaw);
      float sinYaw = Mth.sin(yaw);
      float cosPitch = Mth.cos(pitch);
      float sinPitch = Mth.sin(pitch);
      return new Vec3(sinYaw * cosPitch, -sinPitch, cosYaw * cosPitch);
   }

   public Vec3 aimPoint(double range) {
      LivingEntity rider = this.getControllingPassenger();
      if (rider != null) {
         Vec3 eye = rider.getEyePosition();
         Vec3 end = eye.add(rider.getLookAngle().scale(range + 6.0));
         BlockHitResult hit = this.level().clip(new ClipContext(eye, end, Block.COLLIDER, Fluid.NONE, rider));
         return hit.getType() == Type.MISS ? end : hit.getLocation();
      }

      if (!this.level().isClientSide()) {
         LivingEntity target = this.getTarget();
         if (target != null) {
            return target.getBoundingBox().getCenter();
         }
      }

      Vec3 from = this.position().add(0.0, this.getEyeHeight(), 0.0);
      return from.add(viewVector(this.getXRot(), this.yHeadRot).scale(range + 6.0));
   }

   public double heightAboveGround(double max) {
      Vec3 from = this.position().add(0.0, 0.1, 0.0);
      BlockHitResult hit = this.level().clip(new ClipContext(from, from.add(0.0, -max - 0.1, 0.0), Block.COLLIDER, Fluid.ANY, this));
      return hit.getType() == Type.MISS ? max : Math.max(0.0, from.y - hit.getLocation().y - 0.1);
   }

   public void teleportWithRider(Vec3 to) {
      this.teleportTo(to.x, to.y, to.z);
      if (this.getControllingPassenger() instanceof ServerPlayer rider) {
         rider.connection.send(ClientboundMoveVehiclePacket.fromEntity(this));
         rider.connection.teleport(rider.getX(), rider.getY(), rider.getZ(), rider.getYRot(), rider.getXRot());
      }
   }

   public void broadcastAnim(byte event) {
      this.applyAnimEvent(event);
      this.level().broadcastEntityEvent(this, event);
   }

   public void roar() {
      this.broadcastAnim((byte)70);
      if (this.isBaby()) {
         this.playSound(ModSounds.voice(this.kind, ModSounds.Voice.BABY), 1.0F, this.getVoicePitch());
      } else {
         this.playSound(ModSounds.voice(this.kind, ModSounds.Voice.ROAR), 2.5F, this.getVoicePitch());
      }
   }

   private boolean applyAnimEvent(byte event) {
      switch (event) {
         case 70:
            this.anim.roarTicks = 32;
            break;
         case 71:
            this.anim.flashTicks = 9;
            break;
         case 72:
            this.anim.biteTicks = 10;
            break;
         case 73:
            this.anim.roarTicks = 12;
            if (this.level().isClientSide()) {
               this.sneezeParticles();
            }
            break;
         case 74:
            this.anim.startWindup();
            break;
         default:
            return false;
      }

      return true;
   }

   @Override
   public void handleEntityEvent(byte event) {
      if (!this.applyAnimEvent(event)) {
         super.handleEntityEvent(event);
      }
   }

   @Override
   public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
      if (target instanceof TamableAnimal pet && pet.getOwner() == owner) {
         return false;
      } else {
         return target instanceof Player other && owner instanceof Player ownerPlayer && !ownerPlayer.canHarmPlayer(other)
            ? false
            : !(target instanceof ArmorStand);
      }
   }

   @Override
   public boolean doHurtTarget(ServerLevel level, Entity target) {
      this.broadcastAnim((byte)72);
      return super.doHurtTarget(level, target);
   }

   @Override
   public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
      Entity attacker = source.getEntity();
      if (attacker != null && this.hasPassenger(attacker)) {
         return false;
      }

      boolean hurt = super.hurtServer(level, source, amount);
      if (hurt) {
         if (this.isOrderedToSit() && attacker != null) {
            this.setOrderedToSit(false);
         }

         if (!this.isTame() && attacker instanceof LivingEntity && this.random.nextInt(4) == 0 && this.anim.roarTicks == 0) {
            this.roar();
         }
      }

      return hurt;
   }

   public float basePitch() {
      return switch (this.kind) {
         case EMBER -> 0.85F;
         case FROST -> 1.2F;
         case STORM -> 1.05F;
         case THORN -> 0.72F;
         case VOID -> 0.62F;
         case TIDE -> 0.98F;
         case SOLAR -> 1.15F;
      };
   }

   private float flapPitch() {
      return switch (this.kind) {
         case EMBER, THORN -> 0.85F;
         case FROST, STORM -> 1.1F;
         case VOID -> 0.8F;
         case TIDE, SOLAR -> 1.0F;
      };
   }

   @Override
   public float getVoicePitch() {
      float pitch = 1.0F + (this.random.nextFloat() - 0.5F) * 0.1F;
      return this.isBaby() ? pitch * (1.15F - 0.25F * this.growth()) : pitch;
   }

   @Override
   protected float getSoundVolume() {
      return this.isBaby() ? 0.7F : 1.3F;
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return this.isInSittingPose() && !this.isBaby() ? null : ModSounds.voice(this.kind, this.isBaby() ? ModSounds.Voice.BABY : ModSounds.Voice.AMBIENT);
   }

   @Override
   public int getAmbientSoundInterval() {
      return this.isBaby() ? 220 : 340;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource source) {
      return ModSounds.voice(this.kind, this.isBaby() ? ModSounds.Voice.BABY : ModSounds.Voice.HURT);
   }

   @Override
   protected SoundEvent getDeathSound() {
      return ModSounds.voice(this.kind, this.isBaby() ? ModSounds.Voice.BABY : ModSounds.Voice.DEATH);
   }

   @Override
   protected void playStepSound(BlockPos pos, BlockState state) {
      float size = this.getAgeScale();
      float pitch = (0.9F + this.random.nextFloat() * 0.2F) / Mth.sqrt(size);
      this.playSound(ModSounds.get("dragon.step"), 0.55F * size * size, pitch);
      if (!state.isAir()) {
         SoundType ground = state.getSoundType();
         this.playSound(ground.getStepSound(), ground.getVolume() * 0.4F * size, ground.getPitch() * 0.62F * pitch);
      }
   }

   private void sneezeParticles() {
      Vec3 look = viewVector(0.0F, this.yHeadRot);
      double reach = this.getBbWidth() * 0.55;
      Vec3 nose = this.position().add(look.x * reach, this.getBbHeight() * 0.75, look.z * reach);
      ParticleOptions puff = elementParticle(this.kind);
      ParticleOptions extra = elementParticle(this.blood());

      for (int i = 0; i < 8; i++) {
         Vec3 v = look.scale(0.08 + this.random.nextDouble() * 0.08)
            .add((this.random.nextDouble() - 0.5) * 0.06, this.random.nextDouble() * 0.05, (this.random.nextDouble() - 0.5) * 0.06);
         ParticleOptions p = i % 3 == 2 ? extra : puff;
         this.level().addParticle(p, nose.x, nose.y, nose.z, v.x, v.y, v.z);
      }

      this.level().addParticle(ParticleTypes.POOF, nose.x, nose.y, nose.z, look.x * 0.05, 0.02, look.z * 0.05);
   }

   public static ParticleOptions elementParticle(DragonKind element) {
      return switch (element) {
         case EMBER -> ParticleTypes.FLAME;
         case FROST -> ParticleTypes.SNOWFLAKE;
         case STORM -> ParticleTypes.ELECTRIC_SPARK;
         case THORN -> ParticleTypes.SPORE_BLOSSOM_AIR;
         case VOID -> ParticleTypes.REVERSE_PORTAL;
         case TIDE -> ParticleTypes.BUBBLE_POP;
         case SOLAR -> ParticleTypes.WAX_ON;
      };
   }

   @Override
   public InteractionResult mobInteract(Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      boolean client = this.level().isClientSide();
      if (stack.is(Items.BOOK)) {
         if (!client && player instanceof ServerPlayer serverPlayer) {
            DragonInfo.send(serverPlayer, this);
         }

         return InteractionResult.SUCCESS;
      } else if (!this.isTame()) {
         if (this.isFood(stack) && !this.isBaby()) {
            if (!client) {
               if (this.getTarget() == player) {
                  this.level().broadcastEntityEvent(this, (byte)6);
                  return InteractionResult.SUCCESS;
               }

               stack.consume(1, player);
               this.playSound(SoundEvents.GENERIC_EAT.value(), 1.0F, this.basePitch());
               if (this.random.nextInt(3) == 0) {
                  this.tame(player);
                  this.getNavigation().stop();
                  this.setTarget(null);
                  this.setOrderedToSit(false);
                  this.setHealth(this.getMaxHealth());
                  this.level().broadcastEntityEvent(this, (byte)7);
                  this.roar();
               } else {
                  this.level().broadcastEntityEvent(this, (byte)6);
               }
            }

            return InteractionResult.SUCCESS;
         } else {
            return InteractionResult.PASS;
         }
      } else {
         if (!this.isOwnedBy(player)) {
            return InteractionResult.PASS;
         }

         if (isLoveFood(stack)) {
            if (!client) {
               if (this.isBaby()) {
                  message(player, Component.translatable("message.skywyrms.too_young_to_breed", this.getDisplayName()));
               } else if (this.getAge() > 0) {
                  message(player, Component.translatable("message.skywyrms.breeding_rest", this.getDisplayName(), Math.max(1, (this.getAge() + 1199) / 1200)));
               } else if (this.isInLove()) {
                  message(player, Component.translatable("message.skywyrms.already_in_love", this.getDisplayName()));
               } else {
                  stack.consume(1, player);
                  this.playSound(SoundEvents.GENERIC_EAT.value(), 1.0F, this.basePitch());
                  this.heal(10.0F);
                  this.setInLove(player);
                  this.setInLoveTime(1200);
                  message(player, Component.translatable("message.skywyrms.in_love", this.getDisplayName()));
               }
            }

            return InteractionResult.SUCCESS;
         } else if (this.isFood(stack)) {
            if (this.isBaby()) {
               if (!client && this.level() instanceof ServerLevel level) {
                  stack.consume(1, player);
                  this.ageUp(AgeableMob.getSpeedUpSecondsWhenFeeding(-this.getAge()), false);
                  this.updateGrowth();
                  this.heal(6.0F);
                  this.playSound(SoundEvents.GENERIC_EAT.value(), 0.8F, 1.5F);
                  level.sendParticles(
                     ParticleTypes.HAPPY_VILLAGER,
                     this.getX(),
                     this.getY() + this.getBbHeight() * 0.8,
                     this.getZ(),
                     7,
                     this.getBbWidth() * 0.4,
                     0.3,
                     this.getBbWidth() * 0.4,
                     0.0
                  );
                  message(player, Component.translatable("message.skywyrms.growing", this.getDisplayName(), Math.round(this.growth() * 100.0F)));
               }

               return InteractionResult.SUCCESS;
            } else if (this.getHealth() < this.getMaxHealth()) {
               if (!client) {
                  stack.consume(1, player);
                  this.heal(12.0F);
                  this.playSound(SoundEvents.GENERIC_EAT.value(), 1.0F, this.basePitch());
                  this.level().broadcastEntityEvent(this, (byte)7);
               }

               return InteractionResult.SUCCESS;
            } else {
               return InteractionResult.PASS;
            }
         } else if (!this.isSaddled() && stack.is(Items.SADDLE)) {
            if (!client) {
               if (this.isBaby()) {
                  message(player, Component.translatable("message.skywyrms.too_young_to_ride", this.getDisplayName()));
               } else {
                  stack.consume(1, player);
                  this.setSaddled(true);
                  this.playSound(SoundEvents.HORSE_SADDLE.value(), 1.0F, 0.8F);
               }
            }

            return InteractionResult.SUCCESS;
         } else if (this.isSaddled() && stack.is(Items.SHEARS) && !this.isVehicle()) {
            if (!client && this.level() instanceof ServerLevel level) {
               this.setSaddled(false);
               this.spawnAtLocation(level, new ItemStack(Items.SADDLE));
               this.playSound(SoundEvents.SHEEP_SHEAR, 1.0F, 1.0F);
            }

            return InteractionResult.SUCCESS;
         } else if (player.isSecondaryUseActive()) {
            if (!client) {
               this.setOrderedToSit(!this.isOrderedToSit());
               this.jumping = false;
               this.getNavigation().stop();
               this.setTarget(null);
               message(player, Component.translatable(this.isOrderedToSit() ? "message.skywyrms.staying" : "message.skywyrms.following", this.getDisplayName()));
            }

            return InteractionResult.SUCCESS;
         } else if (this.isBaby()) {
            if (!client) {
               message(player, Component.translatable("message.skywyrms.too_young_to_ride", this.getDisplayName()));
            }

            return InteractionResult.SUCCESS;
         } else if (this.isSaddled()) {
            if (!client && !this.isVehicle()) {
               this.setOrderedToSit(false);
               this.setInSittingPose(false);
               player.startRiding(this);
            }

            return InteractionResult.SUCCESS;
         } else {
            if (!client) {
               message(player, Component.translatable("message.skywyrms.needs_saddle", this.getDisplayName()));
            }

            return InteractionResult.SUCCESS;
         }
      }
   }

   public static void message(Player player, Component message) {
      player.sendOverlayMessage(message);
   }

   @Override
   public LivingEntity getControllingPassenger() {
      return this.isSaddled() && !this.isBaby() && this.getFirstPassenger() instanceof Player player ? player : super.getControllingPassenger();
   }

   @Override
   public boolean isFlyingVehicle() {
      return true;
   }

   private static boolean isAscending(Player rider) {
      return rider instanceof ServerPlayer serverPlayer ? serverPlayer.getLastClientInput().jump() : rider.isJumping();
   }

   public static boolean isSprintHeld(Player rider) {
      return rider instanceof ServerPlayer serverPlayer ? serverPlayer.getLastClientInput().sprint() : rider.isLocalPlayer() && sprintKeyDown;
   }

   public float flightTopSpeed() {
      return (float)(this.getAttributeValue(Attributes.FLYING_SPEED) * 0.1 / 0.09);
   }

   @Override
   protected void tickRidden(Player rider, Vec3 input) {
      super.tickRidden(rider, input);
      boolean flying = this.isFlying();
      float turn = flying ? 0.13F : 0.22F;
      float yaw = this.getYRot() + Mth.wrapDegrees(rider.getYRot() - this.getYRot()) * turn;
      this.setRot(yaw, Mth.clamp(rider.getXRot(), -60.0F, 60.0F));
      this.yBodyRot = yaw;
      this.yHeadRot = yaw + Mth.clamp(Mth.wrapDegrees(rider.getYRot() - yaw), -80.0F, 80.0F);
      if (!this.level().isClientSide() || this.canSimulateMovement()) {
         boolean ascending = isAscending(rider);
         if (!flying && ascending && this.takeoffTimer == 0) {
            boolean standing = this.level().isClientSide() ? this.onGround() && !this.isInWater() : !this.isInWater();
            if (standing) {
               this.beginTakeoff(0.62);
            } else {
               this.leap(0.62);
            }
         } else if (flying) {
            this.flyTicks++;
            if (this.onGround() && !ascending && this.flyTicks > 12) {
               this.setFlying(false);
            }
         }

         this.updateDive(rider);
      }
   }

   private void updateDive(Player rider) {
      if (this.isFlying() && !this.isInWater() && !this.onGround()) {
         float pitch = rider.getXRot();
         float steep = rider.zza > 0.0F ? Mth.clamp((pitch - 25.0F) / 45.0F, 0.0F, 1.0F) : 0.0F;
         float target = 1.0F + 1.4000001F * steep;
         if (this.diveBoost < target) {
            this.diveBoost = Math.min(target, this.diveBoost + 0.016F * (0.5F + steep));
         } else {
            this.diveBoost = Math.max(target, this.diveBoost - (pitch < -10.0F ? 0.03F : 0.012F));
         }
      } else {
         this.diveBoost = 1.0F;
      }
   }

   public float diveBoost() {
      return this.diveBoost;
   }

   public void beginTakeoff(double impulse) {
      if (!this.isFlying() && this.takeoffTimer <= 0) {
         this.takeoffTimer = 6;
         this.takeoffImpulse = impulse;
         if (this.level().isClientSide()) {
            this.anim.startWindup();
         } else {
            this.broadcastAnim((byte)74);
         }
      }
   }

   public boolean isTakingOff() {
      return this.takeoffTimer > 0;
   }

   private void tickTakeoff() {
      if (this.takeoffTimer > 0) {
         if (this.getControllingPassenger() == null && !this.aiFlying) {
            this.takeoffTimer = 0;
         } else {
            if (--this.takeoffTimer == 0 && !this.isFlying() && this.isAlive()) {
               this.leap(this.takeoffImpulse);
            }
         }
      }
   }

   private void leap(double impulse) {
      this.takeoffTimer = 0;
      this.setFlying(true);
      this.flyTicks = 0;
      if (this.canSimulateMovement()) {
         Vec3 motion = this.getDeltaMovement();
         this.setDeltaMovement(motion.x, Math.max(motion.y, impulse), motion.z);
         markMoved(this);
      }
   }

   private void groundEffects(boolean wingBeat) {
      if (this.isAlive()) {
         float size = this.getAgeScale();
         if (this.anim.leapNow) {
            BlockState ground = this.groundState();
            if (ground != null) {
               this.dustRing(ground, 18, 0.55, 0.22, 0.05);
               if (!this.isSilent()) {
                  SoundType type = ground.getSoundType();
                  this.level()
                     .playLocalSound(
                        this.getX(),
                        this.getY(),
                        this.getZ(),
                        type.getStepSound(),
                        this.getSoundSource(),
                        type.getVolume() * 0.9F * size,
                        type.getPitch() * 0.55F,
                        false
                     );
               }
            }
         }

         if (this.anim.touchdownNow) {
            BlockState ground = this.groundState();
            float pow = this.anim.touchdownPow;
            if (ground != null) {
               this.dustRing(ground, 18 + (int)(26.0F * pow), 0.5, 0.16 + 0.16 * pow, 0.03);
               this.crumbs(ground, 8 + (int)(16.0F * pow));
            }

            if (!this.isSilent()) {
               float pitch = 0.62F / Mth.sqrt(size);
               this.level()
                  .playLocalSound(
                     this.getX(),
                     this.getY(),
                     this.getZ(),
                     ModSounds.get("dragon.step"),
                     this.getSoundSource(),
                     (1.0F + 1.2F * pow) * size * size,
                     pitch,
                     false
                  );
               if (ground != null) {
                  SoundType type = ground.getSoundType();
                  this.level()
                     .playLocalSound(
                        this.getX(),
                        this.getY(),
                        this.getZ(),
                        type.getStepSound(),
                        this.getSoundSource(),
                        type.getVolume() * (0.8F + 0.6F * pow) * size,
                        type.getPitch() * 0.5F,
                        false
                     );
               }
            }
         }

         if (wingBeat && this.isFlying() && !this.isInWater()) {
            double height = this.heightAboveGround(4.0);
            if (height < 3.5) {
               BlockState ground = this.level().getBlockState(BlockPos.containing(this.getX(), this.getY() - height - 0.2, this.getZ()));
               if (!ground.isAir() && ground.getFluidState().isEmpty()) {
                  float strength = (float)(1.0 - height / 3.5);
                  this.dustRingAt(ground, this.getY() - height, 6 + (int)(12.0F * strength), 0.7, 0.1 + 0.14 * strength, 0.02);
               }
            }
         }

         int idle = this.anim.idleStarted;
         if (idle != 0 && !this.isSilent()) {
            if (idle == 3 || idle == 5) {
               this.level()
                  .playLocalSound(
                     this.getX(), this.getY(), this.getZ(), ModSounds.get("dragon.flap"), this.getSoundSource(), 0.5F * size, this.flapPitch() * 1.1F, false
                  );
            } else if (idle == 4) {
               this.level()
                  .playLocalSound(
                     this.getX(),
                     this.getY(),
                     this.getZ(),
                     ModSounds.voice(this.kind, this.isBaby() ? ModSounds.Voice.BABY : ModSounds.Voice.AMBIENT),
                     this.getSoundSource(),
                     0.45F * size,
                     this.getVoicePitch() * 0.8F,
                     false
                  );
            }
         }
      }
   }

   private BlockState groundState() {
      if (this.isInWater()) {
         return null;
      }

      BlockState state = this.level().getBlockState(BlockPos.containing(this.getX(), this.getY() - 0.2, this.getZ()));
      return !state.isAir() && state.getFluidState().isEmpty() ? state : null;
   }

   private void dustRing(BlockState ground, int count, double radius, double speed, double rise) {
      this.dustRingAt(ground, this.getY(), count, radius, speed, rise);
   }

   private void dustRingAt(BlockState ground, double y, int count, double radius, double speed, double rise) {
      double r = this.getBbWidth() * radius;
      double start = this.random.nextDouble() * Math.PI * 2.0;

      for (int i = 0; i < count; i++) {
         double angle = start + i * ((Math.PI * 2) / count) + (this.random.nextDouble() - 0.5) * 0.4;
         double cos = Math.cos(angle);
         double sin = Math.sin(angle);
         double v = speed * (0.7 + this.random.nextDouble() * 0.6);
         this.level()
            .addParticle(
               ModParticles.dust(),
               this.getX() + cos * r,
               y + 0.15 + this.random.nextDouble() * 0.2,
               this.getZ() + sin * r,
               cos * v,
               rise * (0.5 + this.random.nextDouble()),
               sin * v
            );
      }
   }

   private void crumbs(BlockState ground, int count) {
      BlockParticleOption crumb = new BlockParticleOption(ParticleTypes.BLOCK, ground);
      double r = this.getBbWidth() * 0.5;

      for (int i = 0; i < count; i++) {
         double angle = this.random.nextDouble() * Math.PI * 2.0;
         double d = r * (0.6 + this.random.nextDouble() * 0.6);
         this.level()
            .addParticle(
               crumb,
               this.getX() + Math.cos(angle) * d,
               this.getY() + 0.1,
               this.getZ() + Math.sin(angle) * d,
               Math.cos(angle) * 0.2,
               0.25 + this.random.nextDouble() * 0.2,
               Math.sin(angle) * 0.2
            );
      }
   }

   public static void markMoved(Entity entity) {
      entity.needsSync = true;
      entity.syncVelocity = true;
   }

   @Override
   protected Vec3 getRiddenInput(Player rider, Vec3 travelVector) {
      float strafe = rider.xxa * 0.5F;
      float forward = rider.zza;
      if (!this.isFlying()) {
         if (this.takeoffTimer > 0) {
            return Vec3.ZERO;
         }

         if (forward <= 0.0F) {
            forward *= 0.25F;
         }

         return new Vec3(strafe, 0.0, forward);
      } else {
         float up = 0.0F;
         float ahead = 0.0F;
         if (forward != 0.0F) {
            float pitch = rider.getXRot() * (float) (Math.PI / 180.0);
            float cos = Mth.cos(pitch);
            float sin = -Mth.sin(pitch);
            if (forward < 0.0F) {
               cos *= -0.4F;
               sin *= -0.4F;
            }

            up = sin;
            ahead = cos;
         }

         if (isAscending(rider)) {
            up += 0.55F;
         }

         if (descendKeyDown && this.level().isClientSide() && this.canSimulateMovement()) {
            up -= 0.55F;
         }

         return new Vec3(strafe, up, ahead);
      }
   }

   @Override
   protected float getRiddenSpeed(Player rider) {
      float speed = (float)this.getAttributeValue(this.isFlying() ? Attributes.FLYING_SPEED : Attributes.MOVEMENT_SPEED);
      if (this.isFlying()) {
         speed *= this.diveBoost;
      }

      return isSprintHeld(rider) ? speed * 1.3F : speed;
   }

   @Override
   public void travel(Vec3 travelVector) {
      if (this.isFlying()) {
         if (this.getControllingPassenger() != null) {
            float speed = this.getSpeed() * 0.1F;
            float water = this.hasElement(DragonKind.TIDE) ? speed * 2.4F : speed * 0.7F;
            this.travelFlying(travelVector, water, speed * 0.5F, speed);
         } else if (this.aiFlying) {
            this.move(MoverType.SELF, this.getDeltaMovement());
         } else {
            super.travel(travelVector);
            Vec3 motion = this.getDeltaMovement();
            if (motion.y < -0.22) {
               this.setDeltaMovement(motion.x, -0.22, motion.z);
            }
         }
      } else {
         super.travel(travelVector);
      }
   }

   @Override
   public boolean causeFallDamage(double fallDistance, float damageMultiplier, DamageSource source) {
      return false;
   }

   @Override
   protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
   }

   @Override
   protected void removePassenger(Entity passenger) {
      super.removePassenger(passenger);
      this.diveBoost = 1.0F;
      if (!this.level().isClientSide()) {
         this.abilityHeld = false;
         this.stopBreath();
      }
   }

   @Override
   public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
      Vec3 side = getCollisionHorizontalEscapeVector(this.getBbWidth(), passenger.getBbWidth(), this.getYRot() + 90.0F);
      BlockPos pos = BlockPos.containing(this.getX() + side.x, this.getBoundingBox().minY, this.getZ() + side.z);
      Vec3 safe = DismountHelper.findSafeDismountLocation(passenger.getType(), this.level(), pos, true);
      return safe != null ? safe : super.getDismountLocationForPassenger(passenger);
   }

   private void diveTrail() {
      float yaw = this.yBodyRot * (float) (Math.PI / 180.0);
      double rightX = -Mth.cos(yaw);
      double rightZ = -Mth.sin(yaw);
      double half = this.getBbWidth() * 0.75;
      double y = this.getY() + this.getBbHeight() * 0.75;
      if (!(this.anim.stoop < 0.75F) || (this.tickCount & 1) != 0) {
         for (int side = -1; side <= 1; side += 2) {
            double x = this.getX() + rightX * half * side + (this.random.nextDouble() - 0.5) * 0.6;
            double z = this.getZ() + rightZ * half * side + (this.random.nextDouble() - 0.5) * 0.6;
            this.level().addParticle(ParticleTypes.CLOUD, x, y + (this.random.nextDouble() - 0.5) * 0.6, z, 0.0, 0.02, 0.0);
         }
      }
   }

   public AABB renderBounds() {
      double reach = 10.5 * this.kind.scale();
      return this.getBoundingBox().inflate(reach, 3.5, reach);
   }
}
