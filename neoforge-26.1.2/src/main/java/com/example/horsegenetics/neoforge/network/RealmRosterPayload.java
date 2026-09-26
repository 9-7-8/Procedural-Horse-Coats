package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.HorseGenetics;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Server &rarr; client: <b>every horse standing in the horse realm</b>, for the
 * browser's <i>Horse realm</i> tab. Built by {@code server/RealmRoster}.
 *
 * <p>It carries {@link HorseRosterPayload.Entry} rather than a shape of its own,
 * and reuses that payload's hand-written entry codec, because it is the same
 * table with a different question behind it: one renderer, one row type, and a
 * column added to the stable is a column here without a second codec to keep in
 * step. The two are separate payloads only so that arriving in the realm cannot
 * overwrite the client's idea of what is in the player's stable.
 *
 * <h2>Why this one arrives in pieces and the stable does not</h2>
 * <b>A full genotype code is 5,612 characters</b> ({@code GenomeCodeCodecs}), and
 * a row carries one because the table sorts, filters and colours on it. A
 * stable is capped at {@link HorseRosterPayload#MAX_ENTRIES} and a player who
 * has more is told the list was cut, which is a fair trade for a list that is
 * nearly always short.
 *
 * <p>The realm is the opposite case in every respect. The field holds <b>three
 * thousand horses before it even starts growing</b>, the tab's whole purpose is
 * to be the catalogue of all of them, and a cut list is exactly the failure -
 * the horse you are looking for is the one over the cap. Three thousand rows
 * will not fit in a packet at any cap, so the roster is sent as a run of these,
 * {@value #BATCH} rows at a time: {@link #first} clears whatever the client had,
 * {@link #last} says the run is complete. Anything in between is a partial list
 * the tab draws as it fills, which is the honest thing to show while it is
 * filling. (Owner, 2026-09-26: "It's okay if it's slow to load in all the
 * horses.")
 *
 * <p>The batch size is bounded by the <i>worst</i> row rather than the average:
 * {@value #BATCH} maximal genotypes is around 570&nbsp;KB, inside the
 * megabyte a custom payload may carry, and a realistic field of ordinary horses
 * is a small fraction of that.
 */
public record RealmRosterPayload(List<HorseRosterPayload.Entry> entries,
                                 boolean first, boolean last) implements CustomPacketPayload {

    /**
     * Rows per packet. See the class note - this is a byte budget wearing a row
     * count, and the budget is spent by a horse carrying every gene in the
     * registry rather than by a normal one.
     */
    public static final int BATCH = 100;

    public static final Type<RealmRosterPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "realm_roster"));

    public static final StreamCodec<ByteBuf, RealmRosterPayload> STREAM_CODEC = StreamCodec.composite(
            HorseRosterPayload.ENTRY_STREAM_CODEC.apply(ByteBufCodecs.list(BATCH)),
            RealmRosterPayload::entries,
            ByteBufCodecs.BOOL, RealmRosterPayload::first,
            ByteBufCodecs.BOOL, RealmRosterPayload::last,
            RealmRosterPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
