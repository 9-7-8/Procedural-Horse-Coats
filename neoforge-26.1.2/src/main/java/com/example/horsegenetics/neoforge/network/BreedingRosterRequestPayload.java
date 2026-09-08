package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.HorseGenetics;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client -&gt; server: "which of my horses could I breed?". Carries nothing -
 * the answer is entirely about who asked. The server replies with a
 * {@link BreedingRosterPayload}.
 *
 * <p>Sent when the horse browser's Breeding preview tab is opened, and again on
 * its Refresh button, rather than pushed on a timer: the roster is only looked
 * at deliberately, and a horse tamed thirty seconds ago is worth one click.
 */
public record BreedingRosterRequestPayload() implements CustomPacketPayload {

    public static final BreedingRosterRequestPayload INSTANCE = new BreedingRosterRequestPayload();

    public static final Type<BreedingRosterRequestPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "breeding_roster_request"));

    public static final StreamCodec<ByteBuf, BreedingRosterRequestPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
