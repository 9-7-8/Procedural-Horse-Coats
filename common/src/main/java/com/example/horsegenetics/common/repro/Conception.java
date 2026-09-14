package com.example.horsegenetics.common.repro;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.breed.BreedLineage;
import com.example.horsegenetics.common.genetics.GameteBias;
import com.example.horsegenetics.common.genetics.GenomeSample;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.GeneticCodeCombiner;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.SpliceOutcome;
import com.example.horsegenetics.common.trait.HorseTraits;
import com.example.horsegenetics.common.trait.Viability;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * <b>One breeding on this mod's own paths</b> - seed jar, breeding carrot, a
 * stallion left with a mare in heat - and whether it makes a pregnancy.
 *
 * <p>All of those paths call {@link #attempt}; none of them draws a genome for
 * itself. The draw is the ordinary Mendelian one ({@link GeneticCodeCombiner})
 * and happens <b>here, at conception</b>, because that is the only moment both
 * genomes and any carrot bias are in hand.
 *
 * <p>Plain golden-carrot breeding does not come through here. It stays instant,
 * and the only thing fertility does to it is {@link #vanillaFoalChance}.
 */
public final class Conception {

    private Conception() {
    }

    /** Everything about the pair that the attempt needs. */
    public record Mating(Genome dam, BreedLineage damLineage,
                         Genome sire, BreedLineage sireLineage, UUID sireId,
                         String sireFirstName, String sireLastName, int sireGeneration,
                         GameteBias damBias, GameteBias sireBias, String bredBy) {

        public Mating {
            Objects.requireNonNull(dam, "dam");
            Objects.requireNonNull(sire, "sire");
            Objects.requireNonNull(sireId, "sireId");
            damLineage = damLineage == null ? BreedLineage.FERAL : damLineage;
            sireLineage = sireLineage == null ? BreedLineage.FERAL : sireLineage;
            damBias = damBias == null ? GameteBias.NONE : damBias;
            sireBias = sireBias == null ? GameteBias.NONE : sireBias;
        }
    }

    public enum Outcome {
        /** Not in heat (or pregnant, or just foaled). Nothing was drawn and nothing should be spent. */
        NOT_RECEPTIVE,
        /** Receptive, and the roll failed. */
        DID_NOT_TAKE,
        /** She is pregnant. */
        CONCEIVED
    }

    /**
     * @param chance    the odds that were rolled against (0 if not receptive)
     * @param pregnancy present exactly when {@code outcome} is {@link Outcome#CONCEIVED}
     */
    public record Result(Outcome outcome, double chance, Optional<Pregnancy> pregnancy) {
    }

    /**
     * Try once.
     *
     * @param mare          the dam's reproductive record, for her state
     * @param sireCoversToday covers the sire had already made today, before this one
     * @param healthActive  {@code ServerConfig.healthGeneticsActive()}
     * @param lethalsActive {@code ServerConfig.lethalsActive()} - off, and a lethal
     *                      embryo is carried like any other
     */
    public static Result attempt(Mating m, Reproduction mare, long now, ReproTiming t,
                                 int sireCoversToday, boolean healthActive, boolean lethalsActive, Rng rng) {
        double base = ReproRules.baseChance(mare, now, t);
        if (base <= 0.0) {
            return new Result(Outcome.NOT_RECEPTIVE, 0.0, Optional.empty());
        }
        double mareFactor = Genes.FERTILITY.mareFactor(m.dam());
        double sireFactor = Genes.FERTILITY.alleleFactor(m.sire().genotype().pair(Genes.FERTILITY))
                * ReproRules.stallionFactor(sireCoversToday);
        double chance = ReproRules.conceptionChance(base, mareFactor, sireFactor);
        if (rng.nextFloat() >= chance) {
            return new Result(Outcome.DID_NOT_TAKE, chance, Optional.empty());
        }

        double twinChance = Genes.FERTILITY.twinChance(m.dam().genotype().pair(Genes.FERTILITY));
        int count = rng.nextFloat() < twinChance ? 2 : 1;
        GenomeSample sireSample = GenomeSample.of(m.sire());
        List<Embryo> embryos = new ArrayList<>(count);
        boolean anyLost = false;
        for (int i = 0; i < count; i++) {
            Embryo e = draw(m, sireSample, healthActive, lethalsActive, rng);
            anyLost |= e.lostEarly();
            embryos.add(e);
        }

        long due = now + t.gestationTicks();
        long loss = anyLost ? ReproRules.earlyLossTick(now, due, rng) : Pregnancy.NO_LOSS;
        return new Result(Outcome.CONCEIVED, chance, Optional.of(new Pregnancy(embryos, now, due, loss)));
    }

    private static Embryo draw(Mating m, GenomeSample sireSample, boolean healthActive, boolean lethalsActive,
                               Rng rng) {
        Genome foal = GeneticCodeCombiner.combine(m.dam(), m.sire(), rng, m.damBias(), m.sireBias());
        BreedLineage lineage = BreedLineage.combine(m.damLineage(), m.sireLineage());
        if (SpliceOutcome.spliceReached(foal.genotype(), m.dam().genotype(), m.sire().genotype(),
                m.damBias(), m.sireBias())) {
            lineage = lineage.spliced();
        }
        // Read, never conditioned on: the draw above is the ordinary one, so two
        // carriers still lose one pregnancy in four.
        boolean lethal = lethalsActive && HorseTraits.resolve(foal.genotype(), foal.epigenome(), healthActive)
                .viability() == Viability.LETHAL_AT_CONCEPTION;
        return new Embryo(GenomeSample.of(foal), lineage.toToken(), lethal,
                m.sireId(), m.sireFirstName(), m.sireLastName(), m.sireGeneration(), sireSample, m.bredBy());
    }

    /**
     * <b>The chance plain golden-carrot breeding produces a foal at all.</b> 1
     * unless a parent is {@code sf/sf}. Only the allele counts here - the
     * epigenetic number never touches vanilla breeding - so every horse nobody
     * bred for subfertility breeds exactly as vanilla does.
     */
    public static double vanillaFoalChance(Genotype dam, Genotype sire) {
        return Genes.FERTILITY.alleleFactor(dam.pair(Genes.FERTILITY))
                * Genes.FERTILITY.alleleFactor(sire.pair(Genes.FERTILITY));
    }
}
