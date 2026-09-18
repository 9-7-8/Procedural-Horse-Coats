package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * <b>Get me off this horse.</b> Sent on F while flying, because sneak no longer means dismount up there -
 * {@code client.ClientFlightInput} strips it out of the input packet so a rider who wants to descend does not step
 * off instead.
 *
 * <p>It has to be a packet rather than a client-side {@code stopRiding()}: dismount is decided on the server, in
 * {@code Player.rideTick}, and a client that let go on its own would simply be put back on the horse by the next
 * position update.
 */
public record DismountPayload() implements CustomPacketPayload {

    public static final Type<DismountPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "dismount"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DismountPayload> STREAM_CODEC =
            StreamCodec.unit(new DismountPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
