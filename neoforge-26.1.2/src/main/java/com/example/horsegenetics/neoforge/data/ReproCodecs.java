package com.example.horsegenetics.neoforge.data;

import com.example.horsegenetics.common.genetics.GenomeSample;
import com.example.horsegenetics.common.repro.Embryo;
import com.example.horsegenetics.common.repro.Pregnancy;
import com.example.horsegenetics.common.repro.Reproduction;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.List;

/**
 * The save format of {@code HORSE_REPRO} - {@link Reproduction} and what it
 * holds. Kept here because {@code common/} imports no DFU. Server-side only:
 * the client is told what it needs in words, through the social summary, and
 * never sees an embryo's genome.
 *
 * <p>Dev only: no legacy handling.
 */
public final class ReproCodecs {

    private ReproCodecs() {
    }

    private static final Codec<GenomeSample> SAMPLE = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("genotype").forGetter(GenomeSample::genotypeCode),
            Codec.STRING.fieldOf("epigenome").forGetter(GenomeSample::epigenomeCode)
    ).apply(i, GenomeSample::new));

    private static final Codec<Embryo> EMBRYO = RecordCodecBuilder.create(i -> i.group(
            SAMPLE.fieldOf("genome").forGetter(Embryo::genome),
            Codec.STRING.optionalFieldOf("breed", "").forGetter(Embryo::breedToken),
            Codec.BOOL.optionalFieldOf("lost_early", false).forGetter(Embryo::lostEarly),
            UUIDUtil.CODEC.fieldOf("sire_id").forGetter(Embryo::sireId),
            Codec.STRING.optionalFieldOf("sire_first", "").forGetter(Embryo::sireFirstName),
            Codec.STRING.optionalFieldOf("sire_last", "").forGetter(Embryo::sireLastName),
            Codec.INT.optionalFieldOf("sire_generation", 0).forGetter(Embryo::sireGeneration),
            SAMPLE.fieldOf("sire").forGetter(Embryo::sire),
            Codec.STRING.optionalFieldOf("bred_by", "").forGetter(Embryo::bredBy)
    ).apply(i, Embryo::new));

    private static final Codec<Pregnancy> PREGNANCY = RecordCodecBuilder.create(i -> i.group(
            EMBRYO.listOf().fieldOf("embryos").forGetter(Pregnancy::embryos),
            Codec.LONG.fieldOf("conceived").forGetter(Pregnancy::conceivedTick),
            Codec.LONG.fieldOf("due").forGetter(Pregnancy::dueTick),
            Codec.LONG.optionalFieldOf("loss", Pregnancy.NO_LOSS).forGetter(Pregnancy::lossTick)
    ).apply(i, Pregnancy::new));

    public static final MapCodec<Reproduction> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.DOUBLE.optionalFieldOf("cycle_phase", 0.0).forGetter(Reproduction::cyclePhase),
            PREGNANCY.optionalFieldOf("pregnancy").forGetter(Reproduction::pregnancy),
            Codec.LONG.optionalFieldOf("foaled", Reproduction.NEVER).forGetter(Reproduction::foaledTick),
            UUIDUtil.CODEC.listOf().optionalFieldOf("nursing", List.of()).forGetter(Reproduction::nursing),
            Codec.LONG.optionalFieldOf("apart_since", Reproduction.NEVER).forGetter(Reproduction::apartSince),
            Codec.LONG.optionalFieldOf("cover_day", Reproduction.NEVER).forGetter(Reproduction::coverDay),
            Codec.INT.optionalFieldOf("covers", 0).forGetter(Reproduction::covers)
    ).apply(i, Reproduction::new));
}
