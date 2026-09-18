package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * <b>Put me down.</b> Sent on a single press of F while flying; {@link DismountPayload} is the double press, for
 * getting off in a hurry.
 *
 * <p>The server needs to know as well as the client, because a landing horse is still a flying vehicle as far as
 * {@code isFlyingVehicle()} is concerned - and a rider whose vehicle fails that check is disconnected after eighty
 * ticks, which is less time than a descent from altitude.
 */
public record LandPayload() implements CustomPacketPayload {

    public static final Type<LandPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "land"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LandPayload> STREAM_CODEC =
            StreamCodec.unit(new LandPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
