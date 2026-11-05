package com.pokato.skywyrms.net;

import com.pokato.skywyrms.Skywyrms;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.Identifier;

public record AbilityPayload(byte ability) implements CustomPacketPayload {
   public static final Identifier ID = Skywyrms.id("ability");
   public static final Type<AbilityPayload> TYPE = new Type<>(ID);
   public static final StreamCodec<RegistryFriendlyByteBuf, AbilityPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.BYTE, AbilityPayload::ability, AbilityPayload::new
   );

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static AbilityPayload read(FriendlyByteBuf buf) {
      return new AbilityPayload(buf.readByte());
   }
}
