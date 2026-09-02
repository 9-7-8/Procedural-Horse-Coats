package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.HorseGenetics;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client -&gt; server: spawn a horse with a hand-picked genome, from the custom
 * horse spawn egg's editor screen ({@code client/CustomHorseSpawnScreen}).
 *
 * <ul>
 *   <li>{@code genotypeCode} - a full genotype code string
 *       ({@code Genotype.of(pairs).toCode()}); the server re-parses and
 *       normalises it, and rejects a bad one with a chat message.</li>
 *   <li>{@code baby} - spawn as a foal.</li>
 *   <li>{@code female} - {@code Sex.FEMALE} if true, else {@code Sex.MALE}.</li>
 * </ul>
 *
 * The server applies the genome as a <b>founder record</b> before the entity
 * joins the level, so {@code HorseGeneticsEventHandler} doesn't roll a random
 * genotype over the top. The epigenome (per-horse non-deterministic expression)
 * is still rolled fresh, exactly like any wild spawn.
 */
public record SpawnCustomHorsePayload(String genotypeCode, boolean baby, boolean female)
        implements CustomPacketPayload {

    public static final Type<SpawnCustomHorsePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "spawn_custom_horse"));

    public static final StreamCodec<ByteBuf, SpawnCustomHorsePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(2048), SpawnCustomHorsePayload::genotypeCode,
            ByteBufCodecs.BOOL, SpawnCustomHorsePayload::baby,
            ByteBufCodecs.BOOL, SpawnCustomHorsePayload::female,
            SpawnCustomHorsePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
