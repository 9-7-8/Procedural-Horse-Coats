package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Carries no data - the dev "show stalls" keybind asking the server to flash the nearby stall outlines. */
public record RequestStallHighlightPayload() implements CustomPacketPayload {

    public static final Type<RequestStallHighlightPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "request_stall_highlight"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestStallHighlightPayload> STREAM_CODEC =
            StreamCodec.unit(new RequestStallHighlightPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
