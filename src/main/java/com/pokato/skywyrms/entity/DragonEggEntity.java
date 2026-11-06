package com.pokato.skywyrms.entity;

import com.pokato.skywyrms.config.ModConfig;
import com.pokato.skywyrms.registry.ModEntities;
import com.pokato.skywyrms.registry.ModItems;
import com.pokato.skywyrms.registry.ModSounds;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class DragonEggEntity extends Mob {
   public static final byte EVENT_WOBBLE = 80;
   public static final byte EVENT_CRACK = 81;
   private static final EntityDataAccessor<Integer> DATA_BODY = SynchedEntityData.defineId(DragonEggEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_BLOOD = SynchedEntityData.defineId(DragonEggEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_PERCENT = SynchedEntityData.defineId(DragonEggEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> DATA_WARM = SynchedEntityData.defineId(DragonEggEntity.class, EntityDataSerializers.BOOLEAN);
   private DragonGenes genes;
   private UUID owner;
   private int progress;
   public int wobbleTicks;
   public float wobbleStrength;

   public DragonEggEntity(EntityType<? extends DragonEggEntity> type, Level level) {
      super(type, level);
      this.setPersistenceRequired();
   }

   public static Builder createAttributes() {
      return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
   }

   public static DragonEggEntity create(ServerLevel level, DragonGenes genes, UUID owner, int progress) {
      DragonEggEntity egg = ModEntities.egg().create(level, EntitySpawnReason.BREEDING);
      if (egg != null) {
         egg.setGenes(genes);
         egg.owner = owner;
         egg.progress = Math.max(0, progress);
         egg.updatePercent();
      }

      return egg;
   }

   public void placeNear(ServerLevel level, Vec3 at) {
      BlockPos center = BlockPos.containing(at);

      for (int radius = 0; radius <= 3; radius++) {
         for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
               if (Math.max(Math.abs(dx), Math.abs(dz)) == radius) {
                  for (int dy = 2; dy >= -3; dy--) {
                     BlockPos pos = center.offset(dx, dy, dz);
                     if (level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                        && level.noCollision(this, this.getType().getDimensions().makeBoundingBox(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5))) {
                        this.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, this.random.nextFloat() * 360.0F, 0.0F);
                        return;
                     }
                  }
               }
            }
         }
      }

      this.snapTo(at.x, at.y, at.z, this.random.nextFloat() * 360.0F, 0.0F);
   }

   @Override
   protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(DATA_BODY, 0);
      builder.define(DATA_BLOOD, 0);
      builder.define(DATA_PERCENT, 0);
      builder.define(DATA_WARM, false);
   }

   public DragonGenes genes() {
      if (this.genes == null) {
         this.setGenes(DragonGenes.pure(this.body()));
      }

      return this.genes;
   }

   public void setGenes(DragonGenes genes) {
      this.genes = genes;
      this.entityData.set(DATA_BODY, genes.body().ordinal());
      this.entityData.set(DATA_BLOOD, genes.blood().ordinal());
   }

   public DragonKind body() {
      return DragonKind.byOrdinal(this.entityData.get(DATA_BODY));
   }

   public DragonKind blood() {
      return DragonKind.byOrdinal(this.entityData.get(DATA_BLOOD));
   }

   public float progressFraction() {
      return Mth.clamp(this.entityData.get(DATA_PERCENT).intValue() / 100.0F, 0.0F, 1.0F);
   }

   public boolean isWarm() {
      return this.entityData.get(DATA_WARM);
   }

   public UUID owner() {
      return this.owner;
   }

   public int progressTicks() {
      return this.progress;
   }

   public void setProgressTicks(int ticks) {
      this.progress = Math.max(0, ticks);
      this.updatePercent();
   }

   private void updatePercent() {
      int percent = Mth.clamp((int)(this.progress * 100L / ModConfig.get().eggHatchTicks()), 0, 100);
      if (percent != this.entityData.get(DATA_PERCENT)) {
         this.entityData.set(DATA_PERCENT, percent);
      }
   }

   @Override
   protected Component getTypeName() {
      DragonKind body = this.body();
      Component dragon = Component.translatable("entity.skywyrms." + body.id());
      if (this.blood() != body) {
         dragon = Component.translatable("entity.skywyrms.hybrid", Component.translatable("element.skywyrms." + this.blood().element() + ".adjective"), dragon);
      }

      return Component.translatable("entity.skywyrms.dragon_egg.named", dragon);
   }

   @Override
   public void tick() {
      super.tick();
      if (this.level().isClientSide()) {
         if (this.wobbleTicks > 0) {
            this.wobbleTicks--;
         }

         if (this.isWarm() && this.random.nextInt(12) == 0) {
            double r = this.getBbWidth() * 0.6;
            this.level()
               .addParticle(
                  DragonEntity.elementParticle(this.random.nextBoolean() ? this.body() : this.blood()),
                  this.getX() + (this.random.nextDouble() - 0.5) * r,
                  this.getY() + this.random.nextDouble() * this.getBbHeight(),
                  this.getZ() + (this.random.nextDouble() - 0.5) * r,
                  0.0,
                  0.02,
                  0.0
               );
         }
      } else if (this.level() instanceof ServerLevel level && this.isAlive()) {
         this.incubate(level);
      }
   }

   private void incubate(ServerLevel level) {
      if (this.tickCount % 40 == 1) {
         boolean warm = this.likes(level, this.body()) || this.blood() != this.body() && this.likes(level, this.blood());
         if (warm != this.isWarm()) {
            this.entityData.set(DATA_WARM, warm);
         }
      }

      int before = this.entityData.get(DATA_PERCENT);
      this.progress = this.progress + (this.isWarm() ? 2 : 1);
      this.updatePercent();
      int percent = this.entityData.get(DATA_PERCENT);
      if (percent >= 100) {
         this.hatch(level);
      } else {
         int chance = percent >= 90 ? 40 : (percent >= 75 ? 120 : (percent >= 60 ? 300 : 0));
         if (chance > 0 && this.random.nextInt(chance) == 0) {
            this.wobble(level, percent >= 85);
         }

         if (before < 85 && percent >= 85 || before < 95 && percent >= 95) {
            this.wobble(level, true);
         }
      }
   }

   private void wobble(ServerLevel level, boolean crack) {
      level.broadcastEntityEvent(this, (byte)(crack ? 81 : 80));
      if (crack) {
         this.playSound(ModSounds.get("egg.crack"), 0.7F, 0.9F + this.random.nextFloat() * 0.25F);
      }
   }

   @Override
   public void handleEntityEvent(byte event) {
      if (event != 80 && event != 81) {
         super.handleEntityEvent(event);
      } else {
         this.wobbleTicks = event == 81 ? 24 : 16;
         this.wobbleStrength = event == 81 ? 1.0F : 0.55F;
      }
   }

   private boolean likes(ServerLevel level, DragonKind element) {
      BlockPos pos = this.blockPosition();

      return switch (element) {
         case EMBER -> level.dimension() == Level.NETHER
            || this.near(
               level, pos, 2, state -> state.is(BlockTags.FIRE) || state.is(BlockTags.CAMPFIRES) || state.is(Blocks.LAVA) || state.is(Blocks.MAGMA_BLOCK)
            );
         case FROST -> this.near(level, pos, 1, state -> state.is(BlockTags.SNOW) || state.is(BlockTags.ICE));
         case STORM -> level.isThundering() && level.canSeeSky(pos)
            || pos.getY() >= 110 && level.canSeeSky(pos)
            || this.near(level, pos, 2, DragonEggEntity::isLightningRod);
         case THORN -> this.near(level, pos, 2, state -> state.is(BlockTags.LEAVES) || state.is(Blocks.MOSS_BLOCK) || state.is(Blocks.MOSS_CARPET));
         case VOID -> level.dimension() == Level.END
            || this.near(
               level, pos, 2, state -> state.is(Blocks.OBSIDIAN) || state.is(Blocks.CRYING_OBSIDIAN) || state.is(Blocks.END_STONE) || state.is(Blocks.END_ROD)
            );
         case TIDE -> this.isInWater();
         case SOLAR -> DragonEntity.inSunlight(level, pos);
      };
   }

   private static boolean isLightningRod(BlockState state) {
      return state.is(BlockTags.LIGHTNING_RODS);
   }

   private boolean near(ServerLevel level, BlockPos center, int radius, Predicate<BlockState> test) {
      for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -1, -radius), center.offset(radius, 1, radius))) {
         if (test.test(level.getBlockState(pos))) {
            return true;
         }
      }

      return false;
   }

   public void hatch(ServerLevel level) {
      DragonGenes g = this.genes();
      DragonEntity baby = ModEntities.type(g.body()).create(level, EntitySpawnReason.BREEDING);
      if (baby != null) {
         baby.snapTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
         baby.setGenes(g, true);
         baby.makeHatchling(ModConfig.get().babyGrowTicks());
         baby.tameTo(this.owner);
         baby.setPersistenceRequired();
         level.addFreshEntity(baby);
         double h = this.getBbHeight() * 0.5;
         level.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + h, this.getZ(), 12, 0.3, 0.3, 0.3, 0.04);
         level.sendParticles(DragonEntity.elementParticle(g.body()), this.getX(), this.getY() + h, this.getZ(), 20, 0.35, 0.35, 0.35, 0.05);
         level.sendParticles(DragonEntity.elementParticle(g.blood()), this.getX(), this.getY() + h, this.getZ(), 12, 0.35, 0.35, 0.35, 0.05);
         level.sendParticles(ParticleTypes.ITEM_SNOWBALL, this.getX(), this.getY() + h, this.getZ(), 18, 0.25, 0.25, 0.25, 0.12);
         level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.get("egg.hatch"), SoundSource.NEUTRAL, 1.0F, 1.0F);
         baby.playSound(ModSounds.voice(g.body(), ModSounds.Voice.BABY), 1.0F, 1.1F);
         ServerPlayer player = this.owner == null ? null : level.getServer().getPlayerList().getPlayer(this.owner);
         if (player != null) {
            DragonEntity.message(player, Component.translatable("message.skywyrms.hatched", baby.getDisplayName()));
         }

         this.discard();
      }
   }

   @Override
   protected InteractionResult mobInteract(Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      boolean client = this.level().isClientSide();
      if (stack.is(Items.BOOK)) {
         if (!client && player instanceof ServerPlayer serverPlayer) {
            DragonInfo.sendEgg(serverPlayer, this);
         }

         return InteractionResult.SUCCESS;
      } else if (stack.isEmpty() && hand == InteractionHand.MAIN_HAND) {
         if (!client) {
            if (this.owner != null && !this.owner.equals(player.getUUID()) && !player.isCreative()) {
               DragonEntity.message(player, Component.translatable("message.skywyrms.not_your_egg"));
               return InteractionResult.SUCCESS;
            }

            ItemStack egg = ModItems.eggStack(this.genes(), this.owner, this.progress);
            if (!player.getInventory().add(egg) && this.level() instanceof ServerLevel level) {
               this.spawnAtLocation(level, egg);
            }

            this.playSound(SoundEvents.ITEM_PICKUP, 0.6F, 0.8F);
            this.discard();
         }

         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.PASS;
      }
   }

   @Override
   public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
      if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         return super.hurtServer(level, source, amount);
      }

      if (source.getEntity() instanceof Player player) {
         if (player.isCreative()) {
            this.discard();
            return true;
         }

         this.wobble(level, false);
         DragonEntity.message(player, Component.translatable("message.skywyrms.egg_hint"));
      }

      return false;
   }

   @Override
   protected void tickDeath() {
      this.remove(RemovalReason.KILLED);
   }

   @Override
   public boolean isPushable() {
      return false;
   }

   @Override
   public boolean canBeCollidedWith(Entity other) {
      return this.isAlive();
   }

   @Override
   protected void doPush(Entity entity) {
   }

   @Override
   public boolean removeWhenFarAway(double distance) {
      return false;
   }

   @Override
   public boolean canBreatheUnderwater() {
      return true;
   }

   @Override
   public boolean isAffectedByPotions() {
      return false;
   }

   @Override
   public boolean shouldDropExperience() {
      return false;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource source) {
      return null;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return null;
   }

   @Override
   public void addAdditionalSaveData(ValueOutput output) {
      super.addAdditionalSaveData(output);
      output.store("Genes", CompoundTag.CODEC, this.genes().toTag());
      if (this.owner != null) {
         output.putString("EggOwner", this.owner.toString());
      }

      output.putInt("Incubation", this.progress);
   }

   @Override
   public void readAdditionalSaveData(ValueInput input) {
      super.readAdditionalSaveData(input);
      CompoundTag tag = input.read("Genes", CompoundTag.CODEC).orElse(null);
      if (tag != null) {
         this.setGenes(DragonGenes.fromTag(tag, DragonKind.EMBER));
      }

      String owner = input.getStringOr("EggOwner", "");

      try {
         this.owner = owner.isEmpty() ? null : UUID.fromString(owner);
      } catch (IllegalArgumentException e) {
         this.owner = null;
      }

      this.progress = Math.max(0, input.getIntOr("Incubation", 0));
      this.updatePercent();
   }
}
