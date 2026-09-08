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
 * Server -&gt; client: <b>the player's stable</b>. One roster, read by two tabs
 * of the horse browser - the <i>My horses</i> table and the <i>Breeding
 * preview</i> pickers - because they are the same question asked twice and a
 * second round trip for the second answer would be a second thing to keep in
 * step.
 *
 * <h2>Why not just send {@code HorseRecord}s</h2>
 * The family tree does exactly that ({@link FamilyTreeDataPayload}) and it is
 * right to, because it sends seven of them. This sends a whole stable, and a
 * record carries its <b>epigenome</b> code - thousands of characters of
 * per-allele numbers that neither a Punnett square nor a table column has any
 * use for. An {@link Entry} is the genotype plus the handful of denormalised
 * facts a column can show, which is roughly a tenth of the bytes.
 *
 * <h2>The live fields, and what they mean when they are absent</h2>
 * {@link Entry#bond}, {@link Entry#adult}, {@link Entry#inHerd} and
 * {@link Entry#where} can only be answered by a <b>loaded</b> entity. When
 * {@link Entry#loaded} is false they carry defined "not known" values - bond
 * {@code -1}, an empty {@code where} - rather than a plausible zero, so the
 * table can print <i>unknown</i> instead of quietly asserting <i>none</i>.
 *
 * <p>Capped at {@link #MAX_ENTRIES}; a player with more horses than that gets
 * the most recent generations, and the tabs say so rather than pretending the
 * list is everything. The roadmap's real answer here is a server-side index
 * that sorts, filters and paginates - see {@code wiki/roadmap.html#browser};
 * this is the honest interim, and the cap is where it shows.
 */
public record HorseRosterPayload(List<Entry> entries) implements CustomPacketPayload {

    /** A packet ceiling, not a design statement - see the class note. */
    public static final int MAX_ENTRIES = 256;

    /** Bond is unknown until the entity is loaded; this is that, not "no bond". */
    public static final int BOND_UNKNOWN = -1;

    /**
     * One horse, as a row of a table: enough to identify it, its whole genotype
     * (which is what the preview computes on and what a gene filter searches),
     * and the live facts no genotype can answer.
     */
    public record Entry(UUID id,
                        String firstName,
                        String lastName,
                        String barnName,
                        String breed,
                        int generation,
                        String geneticCode,
                        boolean tamed,
                        boolean adult,
                        boolean loaded,
                        int bond,
                        boolean inHerd,
                        String where,
                        String tamedBy,
                        String bredBy,
                        boolean hasParents) {
    }

    public static final Type<HorseRosterPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "horse_roster"));

    /**
     * Sixteen fields is past {@code StreamCodec.composite}'s arity, so the entry
     * is written by hand. Straight-line reads and writes in the same order -
     * the one rule is that they stay in the same order.
     */
    private static final StreamCodec<ByteBuf, Entry> ENTRY_STREAM_CODEC =
            new StreamCodec<ByteBuf, Entry>() {
                @Override
                public Entry decode(ByteBuf buf) {
                    UUID id = UUIDUtil.STREAM_CODEC.decode(buf);
                    String firstName = ByteBufCodecs.stringUtf8(64).decode(buf);
                    String lastName = ByteBufCodecs.stringUtf8(64).decode(buf);
                    String barnName = ByteBufCodecs.stringUtf8(64).decode(buf);
                    String breed = ByteBufCodecs.stringUtf8(64).decode(buf);
                    int generation = ByteBufCodecs.VAR_INT.decode(buf);
                    String geneticCode = ByteBufCodecs.stringUtf8(8192).decode(buf);
                    boolean tamed = ByteBufCodecs.BOOL.decode(buf);
                    boolean adult = ByteBufCodecs.BOOL.decode(buf);
                    boolean loaded = ByteBufCodecs.BOOL.decode(buf);
                    int bond = ByteBufCodecs.VAR_INT.decode(buf);
                    boolean inHerd = ByteBufCodecs.BOOL.decode(buf);
                    String where = ByteBufCodecs.stringUtf8(64).decode(buf);
                    String tamedBy = ByteBufCodecs.stringUtf8(64).decode(buf);
                    String bredBy = ByteBufCodecs.stringUtf8(64).decode(buf);
                    boolean hasParents = ByteBufCodecs.BOOL.decode(buf);
                    return new Entry(id, firstName, lastName, barnName, breed, generation,
                            geneticCode, tamed, adult, loaded, bond - 1, inHerd, where,
                            tamedBy, bredBy, hasParents);
                }

                @Override
                public void encode(ByteBuf buf, Entry entry) {
                    UUIDUtil.STREAM_CODEC.encode(buf, entry.id());
                    ByteBufCodecs.stringUtf8(64).encode(buf, entry.firstName());
                    ByteBufCodecs.stringUtf8(64).encode(buf, entry.lastName());
                    ByteBufCodecs.stringUtf8(64).encode(buf, entry.barnName());
                    ByteBufCodecs.stringUtf8(64).encode(buf, entry.breed());
                    ByteBufCodecs.VAR_INT.encode(buf, entry.generation());
                    ByteBufCodecs.stringUtf8(8192).encode(buf, entry.geneticCode());
                    ByteBufCodecs.BOOL.encode(buf, entry.tamed());
                    ByteBufCodecs.BOOL.encode(buf, entry.adult());
                    ByteBufCodecs.BOOL.encode(buf, entry.loaded());
                    // +1 so BOND_UNKNOWN (-1) survives VAR_INT, which is unsigned.
                    ByteBufCodecs.VAR_INT.encode(buf, entry.bond() + 1);
                    ByteBufCodecs.BOOL.encode(buf, entry.inHerd());
                    ByteBufCodecs.stringUtf8(64).encode(buf, entry.where());
                    ByteBufCodecs.stringUtf8(64).encode(buf, entry.tamedBy());
                    ByteBufCodecs.stringUtf8(64).encode(buf, entry.bredBy());
                    ByteBufCodecs.BOOL.encode(buf, entry.hasParents());
                }
            };

    public static final StreamCodec<ByteBuf, HorseRosterPayload> STREAM_CODEC = StreamCodec.composite(
            ENTRY_STREAM_CODEC.apply(ByteBufCodecs.list(MAX_ENTRIES)), HorseRosterPayload::entries,
            HorseRosterPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
