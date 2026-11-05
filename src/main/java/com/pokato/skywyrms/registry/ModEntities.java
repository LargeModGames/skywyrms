package com.pokato.skywyrms.registry;

import com.pokato.skywyrms.Skywyrms;
import com.pokato.skywyrms.entity.DragonEggEntity;
import com.pokato.skywyrms.entity.DragonEntity;
import com.pokato.skywyrms.entity.DragonKind;
import com.pokato.skywyrms.entity.GeoData;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType.Builder;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.phys.Vec3;

public final class ModEntities {
   private static final Map<DragonKind, EntityType<DragonEntity>> TYPES = new EnumMap<>(DragonKind.class);
   public static final String EGG_ID = "dragon_egg";
   private static EntityType<DragonEggEntity> eggType;

   private ModEntities() {
   }

   public static Identifier id(DragonKind kind) {
      return Skywyrms.id(kind.id());
   }

   public static synchronized EntityType<DragonEntity> type(DragonKind kind) {
      EntityType<DragonEntity> type = TYPES.get(kind);
      if (type == null) {
         type = create(kind);
         TYPES.put(kind, type);
      }

      return type;
   }

   public static synchronized EntityType<DragonEggEntity> egg() {
      if (eggType == null) {
         eggType = Builder.of(DragonEggEntity::new, MobCategory.MISC)
            .sized(0.62F, 0.9F)
            .eyeHeight(0.6F)
            .fireImmune()
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, Skywyrms.id("dragon_egg")));
      }

      return eggType;
   }

   public static synchronized DragonKind kindOf(EntityType<?> type) {
      for (Entry<DragonKind, EntityType<DragonEntity>> entry : TYPES.entrySet()) {
         if (entry.getValue() == type) {
            return entry.getKey();
         }
      }

      throw new IllegalArgumentException("not a Skywyrms dragon type: " + type);
   }

   private static EntityType<DragonEntity> create(DragonKind kind) {
      GeoData geo = kind.geo();
      Builder<DragonEntity> builder = Builder.of(DragonEntity::new, MobCategory.CREATURE)
         .sized(geo.hitboxWidth(), geo.hitboxHeight())
         .eyeHeight(geo.hitboxHeight() * 0.85F)
         .passengerAttachments(new Vec3(0.0, geo.seatHeight(), -geo.seatZ / 16.0F * geo.scale))
         .clientTrackingRange(12);
      if (kind.fireImmune()) {
         builder.fireImmune();
      }

      return builder.build(ResourceKey.create(Registries.ENTITY_TYPE, id(kind)));
   }

   public static void register(BiConsumer<Identifier, EntityType<?>> registrar) {
      for (DragonKind kind : DragonKind.values()) {
         registrar.accept(id(kind), type(kind));
      }

      registrar.accept(Skywyrms.id("dragon_egg"), egg());
   }

   public static void registerAttributes(BiConsumer<EntityType<? extends LivingEntity>, AttributeSupplier> registrar) {
      for (DragonKind kind : DragonKind.values()) {
         registrar.accept(type(kind), DragonEntity.createAttributes(kind).build());
      }

      registrar.accept(egg(), DragonEggEntity.createAttributes().build());
   }
}
