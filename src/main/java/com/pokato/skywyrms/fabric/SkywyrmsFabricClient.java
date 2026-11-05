package com.pokato.skywyrms.fabric;

import com.pokato.skywyrms.client.ClientHooks;
import com.pokato.skywyrms.client.ModKeys;
import com.pokato.skywyrms.client.SkywyrmsClient;
import com.pokato.skywyrms.client.particle.ClientParticles;
import com.pokato.skywyrms.client.render.ModModelLayers;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

public class SkywyrmsFabricClient implements ClientModInitializer {
   @Override
   public void onInitializeClient() {
      SkywyrmsClient.init();
      SkywyrmsFabric.clientSender = ClientPlayNetworking::send;
      SkywyrmsClient.registerRenderers(EntityRendererRegistry::register);
      ClientParticles.register((type, factory) -> ParticleProviderRegistry.getInstance().register(type, factory::apply));
      ModModelLayers.register((location, definition) -> ModelLayerRegistry.registerModelLayer(location, definition::get));
      ModKeys.register(KeyMappingHelper::registerKeyMapping);
      ClientTickEvents.END_CLIENT_TICK.register(ClientHooks::onClientTick);
   }
}
