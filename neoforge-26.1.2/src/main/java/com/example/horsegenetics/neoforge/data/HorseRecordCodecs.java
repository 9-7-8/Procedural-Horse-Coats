package com.example.horsegenetics.neoforge.data;

import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.horse.ParentStats;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Serialization for the Layer-1 {@link HorseRecord}. It lives in the
 * integration layer on purpose so the domain type stays free of any
 * DataFixerUpper / Minecraft dependency. Used by both the entity attachment
 * ({@link ModAttachments#HORSE_RECORD}) and the SavedData
 * ({@link HorseAncestryData}).
 */
public final class HorseRecordCodecs {

    // No "sex" field: sex is a gene, so it already rides in "genetic_code"
    // (see HorseRecord.sex()). Serialising it separately would let a saved
    // record disagree with the genome it carries.
    //
    // No "speed" or "health" either, for the same reason: they are resolved
    // from the genotype (HorseRecord.traits()), so a saved number could only
    // ever go stale against the alleles sitting next to it.

    public static final Codec<ParentStats> PARENT_STATS = RecordCodecBuilder.create(i -> i.group(
            Codec.DOUBLE.fieldOf("speed_min").forGetter(ParentStats::speedMin),
            Codec.DOUBLE.fieldOf("speed_max").forGetter(ParentStats::speedMax),
            Codec.DOUBLE.fieldOf("health_min").forGetter(ParentStats::healthMin),
            Codec.DOUBLE.fieldOf("health_max").forGetter(ParentStats::healthMax)
    ).apply(i, ParentStats::new));

    public static final MapCodec<HorseRecord> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            UUIDUtil.STRING_CODEC.fieldOf("id").forGetter(HorseRecord::id),
            Codec.STRING.optionalFieldOf("first_name", "").forGetter(HorseRecord::firstName),
            Codec.STRING.optionalFieldOf("last_name", "").forGetter(HorseRecord::lastName),
            Codec.STRING.optionalFieldOf("barn_name").forGetter(HorseRecord::barnName),
            GenomeCodeCodecs.STORED_GENOTYPE.fieldOf("genetic_code").forGetter(HorseRecord::geneticCode),
            GenomeCodeCodecs.STORED_EPIGENOME.optionalFieldOf("epigenome_code", "").forGetter(HorseRecord::epigenomeCode),
            Codec.STRING.optionalFieldOf("breed").forGetter(HorseRecord::breed),
            UUIDUtil.STRING_CODEC.optionalFieldOf("mother_id").forGetter(HorseRecord::motherId),
            UUIDUtil.STRING_CODEC.optionalFieldOf("father_id").forGetter(HorseRecord::fatherId),
            Codec.STRING.optionalFieldOf("tamed_by").forGetter(HorseRecord::tamedBy),
            Codec.STRING.optionalFieldOf("bred_by").forGetter(HorseRecord::bredBy),
            Codec.INT.optionalFieldOf("generation", 0).forGetter(HorseRecord::generation),
            PARENT_STATS.optionalFieldOf("parent_stats").forGetter(HorseRecord::parentStats),
            Codec.BOOL.optionalFieldOf("gelded", false).forGetter(HorseRecord::gelded),
            // Vanilla keeps the owner server-side only, so it rides here to reach
            // the client at all (HorseRecord.ownedBy). Reconciled against
            // vanilla's owner by HorseOwnerTrackingHandler, which is the authority.
            UUIDUtil.STRING_CODEC.optionalFieldOf("owner_id").forGetter(HorseRecord::ownerId)
    ).apply(instance, HorseRecord::new));

    public static final Codec<HorseRecord> CODEC = MAP_CODEC.codec();

    /**
     * For custom packets. Encodes the record as NBT over the wire - heavier
     * than a hand-built {@code StreamCodec.composite}. They travel on
     * horse-tracking start, on any record change, and in the family-tree and
     * offspring answers.
     */
    public static final StreamCodec<ByteBuf, HorseRecord> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    /**
     * <b>One NBT tag per record, not one for the list</b> (#202). {@code fromCodec} reads against the 2 MiB NBT
     * quota ({@code NbtAccounter.defaultQuota}, checked in the 26.1.2 sources), and a string counts about twice
     * its length. As one tag, a generation of 24 descendants with long epigenomes came to roughly that limit -
     * and a decode over it disconnects the client. Per record, each has the whole quota to itself.
     */
    public static final StreamCodec<ByteBuf, java.util.List<HorseRecord>> LIST_STREAM_CODEC =
            STREAM_CODEC.apply(ByteBufCodecs.list());

    private HorseRecordCodecs() {
    }
}
