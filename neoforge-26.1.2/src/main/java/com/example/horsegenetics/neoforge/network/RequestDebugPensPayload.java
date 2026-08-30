package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Carries no data - just a request to teleport the sender into the debug pens dimension. */
public record RequestDebugPensPayload() implements CustomPacketPayload {

    public static final Type<RequestDebugPensPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HorseGenetics.MOD_ID, "request_debug_pens"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestDebugPensPayload> STREAM_CODEC =
            StreamCodec.unit(new RequestDebugPensPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
