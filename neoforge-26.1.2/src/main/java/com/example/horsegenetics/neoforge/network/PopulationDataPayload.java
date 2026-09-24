package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.HorseGenetics;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Server -&gt; client: <b>every horse in the world, as a graph</b> - one light
 * entry each, carrying only what the family overview draws. A name, a sex, the
 * generation it sits on, and the two parents that put it there.
 *
 * <p><b>Deliberately not {@code HorseRecord}s.</b> The family tree and the
 * Offspring tab send whole records because they draw each horse in its real
 * coat, and a record carries a genetic code and an epigenome - thousands of
 * characters of per-allele numbers. That is affordable for a dozen horses and
 * absurd for every horse a world has ever bred. The overview draws named boxes,
 * so it is sent named boxes; a horse you want to know more about is one click
 * from its own screen, which fetches the record then.
 *
 * <p>{@code truncated} says the table outgrew {@link #MAX_ENTRIES}. The cut is
 * made <b>oldest first</b>: the entries are sorted by generation before the cap
 * is applied, so what survives is a complete map from the founders down to
 * wherever it stops, rather than a scatter of horses whose parents are missing.
 * The screen says so instead of presenting part of a world as the whole of it.
 */
public record PopulationDataPayload(List<Entry> entries, boolean truncated)
        implements CustomPacketPayload {

    /**
     * Enough for a world nobody has ever seen the end of. A light entry is
     * around seventy bytes, so the whole cap is a few hundred kilobytes - well
     * inside what a custom payload may carry, and asked for only on a button.
     */
    public static final int MAX_ENTRIES = 2000;

    /**
     * One horse on the map. {@code female} rather than a {@code Sex} because
     * that is the whole of what the box does with it - the colour it is drawn
     * in - and a gelding is a stallion to a pedigree chart either way.
     */
    public record Entry(UUID id, String name, boolean female, int generation,
                        Optional<UUID> mother, Optional<UUID> father) {

        public static final StreamCodec<ByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, Entry::id,
                ByteBufCodecs.stringUtf8(64), Entry::name,
                ByteBufCodecs.BOOL, Entry::female,
                ByteBufCodecs.VAR_INT, Entry::generation,
                ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), Entry::mother,
                ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), Entry::father,
                Entry::new);
    }

    public static final Type<PopulationDataPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "population_data"));

    public static final StreamCodec<ByteBuf, PopulationDataPayload> STREAM_CODEC = StreamCodec.composite(
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_ENTRIES)), PopulationDataPayload::entries,
            ByteBufCodecs.BOOL, PopulationDataPayload::truncated,
            PopulationDataPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
