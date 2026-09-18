package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * <b>The rider has asked the horse under them to fly, or to stop.</b> Sent on F, beside
 * {@link ToggleDivePayload} - the client picks which of the two to send, because it is the side that knows whether the
 * horse can fly and whether it is standing in water.
 *
 * <p>Carries nothing. Which horse is meant is "the one the sender is riding", and the server re-checks that rather
 * than trusting it.
 */
public record ToggleFlightPayload() implements CustomPacketPayload {

    public static final Type<ToggleFlightPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "toggle_flight"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleFlightPayload> STREAM_CODEC =
            StreamCodec.unit(new ToggleFlightPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
