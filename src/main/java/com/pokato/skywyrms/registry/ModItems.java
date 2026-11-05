package com.pokato.skywyrms.registry;

import com.pokato.skywyrms.Skywyrms;
import com.pokato.skywyrms.entity.DragonGenes;
import com.pokato.skywyrms.entity.DragonKind;
import com.pokato.skywyrms.item.DragonEggItem;
import com.pokato.skywyrms.platform.Platform;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.Item.Properties;

public final class ModItems {
   public static final Identifier MAIN_TAB_ID = Skywyrms.id("main");
   private static final Map<DragonKind, Item> SPAWN_EGGS = new EnumMap<>(DragonKind.class);
   private static final Map<DragonKind, DragonEggItem> DRAGON_EGGS = new EnumMap<>(DragonKind.class);
   private static CreativeModeTab mainTab;

   private ModItems() {
   }

   public static Identifier spawnEggId(DragonKind kind) {
      return Skywyrms.id(kind.id() + "_spawn_egg");
   }

   public static synchronized Item spawnEgg(DragonKind kind) {
      Item egg = SPAWN_EGGS.get(kind);
      if (egg == null) {
         egg = spawnEgg(spawnEggId(kind), ModEntities.type(kind));
         SPAWN_EGGS.put(kind, egg);
      }

      return egg;
   }

   public static Identifier dragonEggId(DragonKind kind) {
      return Skywyrms.id(kind.id() + "_egg");
   }

   public static synchronized DragonEggItem dragonEgg(DragonKind kind) {
      DragonEggItem egg = DRAGON_EGGS.get(kind);
      if (egg == null) {
         egg = new DragonEggItem(kind, properties(dragonEggId(kind)).stacksTo(1).fireResistant().rarity(Rarity.RARE));
         DRAGON_EGGS.put(kind, egg);
      }

      return egg;
   }

   public static ItemStack eggStack(DragonGenes genes, UUID owner, int progress) {
      ItemStack stack = new ItemStack(dragonEgg(genes.body()));
      DragonEggItem.write(stack, genes, owner, progress);
      return stack;
   }

   public static CreativeModeTab mainTab() {
      if (mainTab == null) {
         mainTab = Platform.get()
            .creativeTabBuilder()
            .title(Component.translatable("itemGroup.skywyrms.main"))
            .icon(() -> new ItemStack(spawnEgg(DragonKind.EMBER)))
            .displayItems((parameters, output) -> {
               for (DragonKind kind : DragonKind.values()) {
                  output.accept(spawnEgg(kind));
               }

               for (DragonKind kind : DragonKind.values()) {
                  output.accept(dragonEgg(kind));
               }
            })
            .build();
      }

      return mainTab;
   }

   public static Properties properties(Identifier id) {
      return new Properties().setId(ResourceKey.create(Registries.ITEM, id));
   }

   private static Item spawnEgg(Identifier id, EntityType<? extends Mob> type) {
      return new SpawnEggItem(properties(id).spawnEgg(type));
   }

   public static void register(BiConsumer<Identifier, Item> registrar) {
      for (DragonKind kind : DragonKind.values()) {
         registrar.accept(spawnEggId(kind), spawnEgg(kind));
      }

      for (DragonKind kind : DragonKind.values()) {
         registrar.accept(dragonEggId(kind), dragonEgg(kind));
      }
   }

   public static void registerTabs(BiConsumer<Identifier, CreativeModeTab> registrar) {
      registrar.accept(MAIN_TAB_ID, mainTab());
   }
}
