package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.HorseGenetics;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * The name typed into the Tack Dyeing Bench, on its way to the server.
 *
 * <p>Vanilla has a rename packet already, but it is hard-wired to the anvil -
 * its handler checks {@code containerMenu instanceof AnvilMenu} - so a bench
 * needs its own. A colour fits in {@code clickMenuButton}; a string does not.
 *
 * <p>Length-capped in the codec rather than trusted: this arrives from a client
 * and ends up on an item that other players can see.
 */
public record BenchNamePayload(String name) implements CustomPacketPayload {

    public static final int MAX_LENGTH = 48;

    public static final Type<BenchNamePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "bench_name"));

    public static final StreamCodec<ByteBuf, BenchNamePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(MAX_LENGTH), BenchNamePayload::name,
            BenchNamePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
