package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.HorseGenetics;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client -&gt; server: "every horse this world has a record of, please". The
 * server answers with a {@link PopulationDataPayload}.
 *
 * <p>No fields - the question has no parameters, and the answer is the whole
 * ancestry table either way. Sent once when the family overview opens, and
 * again on its Refresh, which is the same bargain the Offspring tab strikes: a
 * sweep of the table is not something to do on a tick.
 */
public record PopulationRequestPayload() implements CustomPacketPayload {

    public static final PopulationRequestPayload INSTANCE = new PopulationRequestPayload();

    public static final Type<PopulationRequestPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "population_request"));

    public static final StreamCodec<ByteBuf, PopulationRequestPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
