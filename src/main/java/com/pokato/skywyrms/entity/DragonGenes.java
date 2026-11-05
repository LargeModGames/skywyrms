package com.pokato.skywyrms.entity;

import com.pokato.skywyrms.compat.Nbt;
import java.util.function.ToDoubleFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public record DragonGenes(DragonKind body, DragonKind blood, float health, float speed, float power, int generation, boolean vigor) {
   public static final float MIN_TRAIT = 0.8F;
   public static final float MAX_TRAIT = 1.3F;
   public static final float VIGOR_BONUS = 1.05F;
   private static final double DRIFT = 0.035;

   public boolean purebred() {
      return this.body == this.blood;
   }

   public boolean has(DragonKind kind) {
      return this.body == kind || this.blood == kind;
   }

   public DragonGenes withBody(DragonKind kind) {
      return new DragonGenes(kind, this.blood, this.health, this.speed, this.power, this.generation, this.vigor);
   }

   private float expressed(float trait) {
      return this.vigor ? trait * 1.05F : trait;
   }

   public float healthFactor() {
      return this.expressed(this.health);
   }

   public float speedFactor() {
      return this.expressed(this.speed);
   }

   public float powerFactor() {
      return this.expressed(this.power);
   }

   public double mix(ToDoubleFunction<DragonKind> stat) {
      return (stat.applyAsDouble(this.body) + stat.applyAsDouble(this.blood)) * 0.5;
   }

   public static DragonGenes wild(DragonKind body, DragonKind blood, RandomSource random) {
      return new DragonGenes(body, blood, wildTrait(random), wildTrait(random), wildTrait(random), 0, false);
   }

   public static DragonGenes pure(DragonKind kind) {
      return new DragonGenes(kind, kind, 1.0F, 1.0F, 1.0F, 0, false);
   }

   private static float wildTrait(RandomSource random) {
      return 0.9F + (random.nextFloat() + random.nextFloat()) * 0.1F;
   }

   public static DragonGenes cross(DragonGenes a, DragonGenes b, RandomSource random) {
      DragonKind fromA = random.nextBoolean() ? a.body : a.blood;
      DragonKind fromB = random.nextBoolean() ? b.body : b.blood;
      boolean bFirst = random.nextBoolean();
      DragonKind body = bFirst ? fromB : fromA;
      DragonKind blood = bFirst ? fromA : fromB;
      boolean vigor = a.purebred() && b.purebred() && a.body != b.body;
      return new DragonGenes(
         body,
         blood,
         inherit(a.health, b.health, random),
         inherit(a.speed, b.speed, random),
         inherit(a.power, b.power, random),
         Math.max(a.generation, b.generation) + 1,
         vigor
      );
   }

   private static float inherit(float a, float b, RandomSource random) {
      return clampTrait((a + b) * 0.5F + (float)(random.nextGaussian() * 0.035));
   }

   private static float clampTrait(float value) {
      return Mth.clamp(value, 0.8F, 1.3F);
   }

   public CompoundTag toTag() {
      CompoundTag tag = new CompoundTag();
      tag.putString("Body", this.body.element());
      tag.putString("Blood", this.blood.element());
      tag.putFloat("Health", this.health);
      tag.putFloat("Speed", this.speed);
      tag.putFloat("Power", this.power);
      tag.putInt("Generation", this.generation);
      tag.putInt("Vigor", this.vigor ? 1 : 0);
      return tag;
   }

   public static DragonGenes fromTag(CompoundTag tag, DragonKind fallbackBody) {
      DragonKind body = DragonKind.byName(Nbt.getString(tag, "Body", ""));
      if (body == null) {
         body = fallbackBody;
      }

      DragonKind blood = DragonKind.byName(Nbt.getString(tag, "Blood", ""));
      if (blood == null) {
         blood = body;
      }

      return new DragonGenes(
         body,
         blood,
         clampTrait(Nbt.getFloat(tag, "Health", 1.0F)),
         clampTrait(Nbt.getFloat(tag, "Speed", 1.0F)),
         clampTrait(Nbt.getFloat(tag, "Power", 1.0F)),
         Math.max(0, Nbt.getInt(tag, "Generation", 0)),
         Nbt.getInt(tag, "Vigor", 0) != 0
      );
   }

   public static DragonGenes loadTag(CompoundTag entityTag, DragonKind body) {
      CompoundTag tag = Nbt.getCompound(entityTag, "Genes");
      return tag == null ? null : fromTag(tag, body).withBody(body);
   }

   public void save(ValueOutput output) {
      ValueOutput out = output.child("Genes");
      out.putString("Body", this.body.element());
      out.putString("Blood", this.blood.element());
      out.putFloat("Health", this.health);
      out.putFloat("Speed", this.speed);
      out.putFloat("Power", this.power);
      out.putInt("Generation", this.generation);
      out.putInt("Vigor", this.vigor ? 1 : 0);
   }

   public static DragonGenes load(ValueInput input, DragonKind body) {
      ValueInput in = input.child("Genes").orElse(null);
      if (in == null) {
         return null;
      }

      DragonKind blood = DragonKind.byName(in.getStringOr("Blood", ""));
      if (blood == null) {
         blood = body;
      }

      return new DragonGenes(
         body,
         blood,
         clampTrait(in.getFloatOr("Health", 1.0F)),
         clampTrait(in.getFloatOr("Speed", 1.0F)),
         clampTrait(in.getFloatOr("Power", 1.0F)),
         Math.max(0, in.getIntOr("Generation", 0)),
         in.getIntOr("Vigor", 0) != 0
      );
   }
}
