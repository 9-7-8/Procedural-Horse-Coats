package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Carries no data - the dev "find horses" keybind asking the server to glow every nearby horse for 10s. */
public record RequestHighlightHorsesPayload() implements CustomPacketPayload {

    public static final Type<RequestHighlightHorsesPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "request_highlight_horses"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestHighlightHorsesPayload> STREAM_CODEC =
            StreamCodec.unit(new RequestHighlightHorsesPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
