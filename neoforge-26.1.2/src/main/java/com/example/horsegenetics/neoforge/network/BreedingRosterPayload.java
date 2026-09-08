package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.HorseGenetics;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.UUID;

/**
 * Server -&gt; client: the horses this player could breed, for the browser's
 * Breeding preview tab.
 *
 * <h2>Why not just send {@code HorseRecord}s</h2>
 * The family tree does exactly that ({@link FamilyTreeDataPayload}) and it is
 * right to, because it sends seven of them. This sends a whole stable, and a
 * record carries its <b>epigenome</b> code - thousands of characters of
 * per-allele numbers that a Punnett square has no use for. An {@link Entry} is
 * the genotype and enough to label a row, which is roughly a tenth of the
 * bytes and exactly what the tab reads.
 *
 * <p>Capped at {@link #MAX_ENTRIES}; a player with more horses than that gets
 * the most recent generations, and the tab says so rather than pretending the
 * list is everything.
 */
public record BreedingRosterPayload(List<Entry> entries) implements CustomPacketPayload {

    /** A packet ceiling, not a design statement - see the class note. */
    public static final int MAX_ENTRIES = 256;

    /**
     * One horse, as a row of the picker: enough to identify it and its whole
     * genotype, which is the only part the preview actually computes on.
     */
    public record Entry(UUID id, String name, String breed, int generation, String geneticCode) {
    }

    public static final Type<BreedingRosterPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "breeding_roster"));

    private static final StreamCodec<ByteBuf, Entry> ENTRY_STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, Entry::id,
            ByteBufCodecs.stringUtf8(64), Entry::name,
            ByteBufCodecs.stringUtf8(64), Entry::breed,
            ByteBufCodecs.VAR_INT, Entry::generation,
            ByteBufCodecs.stringUtf8(8192), Entry::geneticCode,
            Entry::new
    );

    public static final StreamCodec<ByteBuf, BreedingRosterPayload> STREAM_CODEC = StreamCodec.composite(
            ENTRY_STREAM_CODEC.apply(ByteBufCodecs.list(MAX_ENTRIES)), BreedingRosterPayload::entries,
            BreedingRosterPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
