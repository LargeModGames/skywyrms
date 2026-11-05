package com.pokato.skywyrms.fabric;

import com.pokato.skywyrms.Skywyrms;
import com.pokato.skywyrms.net.AbilityPayload;
import com.pokato.skywyrms.platform.Platform;
import com.pokato.skywyrms.registry.ModEntities;
import com.pokato.skywyrms.registry.ModItems;
import com.pokato.skywyrms.registry.ModParticles;
import com.pokato.skywyrms.registry.ModSounds;
import com.pokato.skywyrms.server.ServerHooks;
import java.nio.file.Path;
import java.util.function.Consumer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTab.Builder;

public class SkywyrmsFabric implements ModInitializer {
   static Consumer<AbilityPayload> clientSender = payload -> {
      throw new IllegalStateException("sendToServer called without a client");
   };

   @Override
   public void onInitialize() {
      Platform.set(new SkywyrmsFabric.FabricPlatform());
      Skywyrms.init();
      ModEntities.register((id, type) -> Registry.register(BuiltInRegistries.ENTITY_TYPE, id, type));
      ModEntities.registerAttributes(FabricDefaultAttributeRegistry::register);
      ModItems.register((id, item) -> Registry.register(BuiltInRegistries.ITEM, id, item));
      ModParticles.register((id, type) -> Registry.register(BuiltInRegistries.PARTICLE_TYPE, id, type));
      ModSounds.register((id, sound) -> Registry.register(BuiltInRegistries.SOUND_EVENT, id, sound));
      ModItems.registerTabs((id, tab) -> Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id, tab));
      PayloadTypeRegistry.serverboundPlay().register(AbilityPayload.TYPE, AbilityPayload.STREAM_CODEC);
      ServerPlayNetworking.registerGlobalReceiver(AbilityPayload.TYPE, (payload, context) -> ServerHooks.onAbility(context.player(), payload));
      ServerTickEvents.END_SERVER_TICK.register(ServerHooks::onServerTick);
   }

   private static final class FabricPlatform implements Platform {
      @Override
      public String loaderName() {
         return "fabric";
      }

      @Override
      public void sendToServer(AbilityPayload payload) {
         SkywyrmsFabric.clientSender.accept(payload);
      }

      @Override
      public Path configDir() {
         return FabricLoader.getInstance().getConfigDir();
      }

      @Override
      public Builder creativeTabBuilder() {
         return FabricCreativeModeTab.builder();
      }
   }
}
