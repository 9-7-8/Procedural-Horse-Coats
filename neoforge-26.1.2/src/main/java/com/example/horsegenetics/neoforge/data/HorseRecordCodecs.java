package com.example.horsegenetics.neoforge.data;

import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.horse.ParentStats;
import com.example.horsegenetics.common.horse.Sex;
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

    // Sex is a plain Layer-1 enum (no StringRepresentable), so map it by name.
    public static final Codec<Sex> SEX = Codec.STRING.xmap(Sex::valueOf, Sex::name);

    public static final Codec<ParentStats> PARENT_STATS = RecordCodecBuilder.create(i -> i.group(
            Codec.DOUBLE.fieldOf("speed_min").forGetter(ParentStats::speedMin),
            Codec.DOUBLE.fieldOf("speed_max").forGetter(ParentStats::speedMax),
            Codec.DOUBLE.fieldOf("health_min").forGetter(ParentStats::healthMin),
            Codec.DOUBLE.fieldOf("health_max").forGetter(ParentStats::healthMax)
    ).apply(i, ParentStats::new));

    public static final MapCodec<HorseRecord> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            UUIDUtil.STRING_CODEC.fieldOf("id").forGetter(HorseRecord::id),
            SEX.fieldOf("sex").forGetter(HorseRecord::sex),
            Codec.STRING.optionalFieldOf("first_name", "").forGetter(HorseRecord::firstName),
            Codec.STRING.optionalFieldOf("last_name", "").forGetter(HorseRecord::lastName),
            Codec.STRING.optionalFieldOf("barn_name").forGetter(HorseRecord::barnName),
            Codec.STRING.fieldOf("genetic_code").forGetter(HorseRecord::geneticCode),
            UUIDUtil.STRING_CODEC.optionalFieldOf("mother_id").forGetter(HorseRecord::motherId),
            UUIDUtil.STRING_CODEC.optionalFieldOf("father_id").forGetter(HorseRecord::fatherId),
            Codec.STRING.optionalFieldOf("tamed_by").forGetter(HorseRecord::tamedBy),
            Codec.STRING.optionalFieldOf("bred_by").forGetter(HorseRecord::bredBy),
            Codec.INT.optionalFieldOf("generation", 0).forGetter(HorseRecord::generation),
            Codec.DOUBLE.optionalFieldOf("speed", 0.0).forGetter(HorseRecord::speed),
            Codec.DOUBLE.optionalFieldOf("health", 0.0).forGetter(HorseRecord::health),
            PARENT_STATS.optionalFieldOf("parent_stats").forGetter(HorseRecord::parentStats)
    ).apply(instance, HorseRecord::new));

    public static final Codec<HorseRecord> CODEC = MAP_CODEC.codec();

    /**
     * For custom packets. Encodes the record as NBT over the wire - heavier
     * than a hand-built {@code StreamCodec.composite} but these only travel on
     * horse-tracking start and when the family-tree screen is open, so it's
     * not worth the fuss.
     */
    public static final StreamCodec<ByteBuf, HorseRecord> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public static final StreamCodec<ByteBuf, java.util.List<HorseRecord>> LIST_STREAM_CODEC =
            ByteBufCodecs.fromCodec(CODEC.listOf());

    private HorseRecordCodecs() {
    }
}
