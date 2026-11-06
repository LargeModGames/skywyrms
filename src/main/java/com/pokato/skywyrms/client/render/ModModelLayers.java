package com.pokato.skywyrms.client.render;

import com.pokato.skywyrms.Skywyrms;
import com.pokato.skywyrms.entity.DragonKind;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;

public final class ModModelLayers {
   private static final Map<DragonKind, ModelLayerLocation> LAYERS = new EnumMap<>(DragonKind.class);
   public static final ModelLayerLocation EGG = new ModelLayerLocation(Skywyrms.id("dragon_egg"), "main");

   private ModModelLayers() {
   }

   public static ModelLayerLocation dragon(DragonKind kind) {
      return LAYERS.get(kind);
   }

   public static void register(BiConsumer<ModelLayerLocation, Supplier<LayerDefinition>> registrar) {
      for (DragonKind kind : DragonKind.values()) {
         registrar.accept(dragon(kind), () -> DragonModel.createLayer(kind, cube -> true));
      }

      registrar.accept(EGG, EggModel::createLayer);
   }

   static {
      for (DragonKind kind : DragonKind.values()) {
         LAYERS.put(kind, new ModelLayerLocation(Skywyrms.id(kind.id()), "main"));
      }
   }
}
