package com.example.horsegenetics.common.trait;

import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneEpigenetics;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>Which gene is worth which part of this horse's body.</b>
 * {@link HorseTraits#resolve} walks every gene into one shared
 * {@link TraitBuilder} and hands back a total; this walks them into
 * <i>separate</i> builders and hands back the terms.
 *
 * <p>It exists for the horse-information screen's Health tab, which has to
 * answer "why is this horse fast" with the alleles rather than with a number.
 * Nothing here re-implements a gene: each one is handed exactly the pair,
 * genotype and epigenetics {@code HorseTraits} would hand it, so a term is the
 * gene's own arithmetic and the terms sum back to the total by construction -
 * additions add and factors multiply, which is the order-independence
 * {@link TraitBuilder} is built on.
 *
 * <p><b>The sum is not re-derived here.</b> A caller that wants the horse asks
 * {@link HorseTraits#resolve}; this only says where it came from. Deriving the
 * total by adding these up would be a second implementation of {@code build()}
 * and would drift from it the first time a clamp moved.
 */
public final class TraitBreakdown {

    /**
     * One gene's contribution to the body, as the gene itself pushed it: four
     * additive terms in attribute units and four multipliers.
     *
     * <p>A multiplier of {@code 1.0} means the gene did not multiply that axis;
     * an additive term of {@code 0.0} means it added nothing. A gene that did
     * neither, and reported no condition, is {@link #silent()} and is not in the
     * list this class returns.
     */
    public record Term(Gene gene, AllelePair pair,
                       double speed, double health, double jump, double scale,
                       double speedFactor, double healthFactor, double jumpFactor, double scaleFactor,
                       List<Condition> conditions) {

        public Term {
            conditions = List.copyOf(conditions);
        }

        public boolean silent() {
            return speed == 0.0 && health == 0.0 && jump == 0.0 && scale == 0.0
                    && speedFactor == 1.0 && healthFactor == 1.0 && jumpFactor == 1.0 && scaleFactor == 1.0
                    && conditions.isEmpty();
        }

        /** Does this gene say anything about the given axis? */
        public boolean touches(StatAxis axis) {
            return switch (axis) {
                case SPEED -> speed != 0.0 || speedFactor != 1.0;
                case HEALTH -> health != 0.0 || healthFactor != 1.0;
                case JUMP -> jump != 0.0 || jumpFactor != 1.0;
                case SCALE -> scale != 0.0 || scaleFactor != 1.0;
            };
        }
    }

    private TraitBreakdown() {
    }

    /**
     * Every gene that moves this horse's body, in {@link Genes#codeOrder()},
     * skipping the ones whose alleles happen to say nothing. {@code epigenome}
     * may be {@code null}, in which case an epigenetic contribution reports its
     * schema's midpoint - exactly as in {@link HorseTraits#resolve}.
     *
     * @param healthGenetics {@code false} suppresses every
     *        {@link HealthContribution}, so the breakdown matches what the
     *        server actually applied rather than what the alleles would do.
     */
    public static List<Term> of(Genotype genotype, Epigenome epigenome, boolean healthGenetics) {
        List<Term> out = new ArrayList<>();
        for (Gene gene : Genes.codeOrder()) {
            boolean plain = gene instanceof TraitContribution;
            boolean epigenetic = gene instanceof EpigeneticTraitContribution;
            if (!plain && !epigenetic) {
                continue;
            }
            if (!healthGenetics && gene instanceof HealthContribution) {
                continue;
            }
            AllelePair pair = genotype.pair(gene);
            TraitBuilder solo = new TraitBuilder();
            if (plain) {
                ((TraitContribution) gene).contribute(pair, genotype, solo);
            }
            if (epigenetic) {
                ((EpigeneticTraitContribution) gene).contribute(pair, genotype,
                        GeneEpigenetics.forGene(gene, genotype, epigenome), solo);
            }
            Term term = new Term(gene, pair,
                    solo.rawSpeed() - HorseTraits.BASE_SPEED,
                    solo.rawHealth() - HorseTraits.BASE_HEALTH,
                    solo.rawJump() - HorseTraits.BASE_JUMP,
                    solo.rawScale() - HorseTraits.BASE_SCALE,
                    solo.rawSpeedFactor(), solo.rawHealthFactor(), solo.rawJumpFactor(),
                    solo.rawScaleFactor() * solo.rawMagicalScaleFactor(),
                    solo.rawConditions());
            if (!term.silent()) {
                out.add(term);
            }
        }
        return List.copyOf(out);
    }

    /** Only the genes with something to say about {@code axis}. */
    public static List<Term> on(List<Term> terms, StatAxis axis) {
        List<Term> out = new ArrayList<>();
        for (Term t : terms) {
            if (t.touches(axis)) {
                out.add(t);
            }
        }
        return List.copyOf(out);
    }
}
