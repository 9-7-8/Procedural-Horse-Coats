package com.example.horsegenetics.common.breed;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.common.trait.HealthContribution;
import com.example.horsegenetics.common.trait.StatAxis;
import com.example.horsegenetics.common.trait.TargetBand;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.AlleleEpigenetics;
import com.example.horsegenetics.common.genetics.genes.AbstractMagicStatGene;

import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Rolls a wild founder {@link Genome} for a {@link Breed}.
 *
 * <h2>How it is built</h2>
 * It starts from an ordinary unconstrained {@link Genotype#random} roll and then
 * overrides:
 * <ol>
 *   <li><b>every coat gene the breed does not name</b> is forced to its wild
 *       type, so a random pattern or dilution from the base roll cannot leak
 *       onto a breed that should not have it;</li>
 *   <li><b>every gene the breed does name</b> is redrawn from the breed's own
 *       {@link Breed#founderTable weighted pool};</li>
 *   <li><b>the four magical body-stat loci</b> are set from
 *       {@link Breed#statTargets()} - carrying the pushing allele on any axis
 *       the breed pins, wild on the rest - and then, once the epigenome exists,
 *       those copies are given numbers that land the horse inside the band
 *       ({@link #stampStatTargets}). Speed, jump and health are homozygous.
 *       <b>Size is decided per founder</b>: its target is drawn first, and a
 *       founder landing inside {@link BreedStatCurve#heterozygousSize 0.7x to
 *       1.3x} carries one size copy and the wild type, while one outside it
 *       carries two;</li>
 *   <li><b>every other magical gene</b> and <b>every disorder</b> the breed
 *       does not name is forced wild. A breed is exactly its breed sheet: it
 *       carries the magic it names and the disorders it names, and nothing
 *       else. There is no stray magic and no background disorder rate.</li>
 * </ol>
 * Natural genes that are neither coat nor disorder (the performance loci) keep
 * whatever the base roll gave them.
 *
 * <p>{@link Breeds#FERAL_MIXED} skips all of this and returns the base roll
 * untouched - the pre-breeds behaviour, exactly. It is the <b>only</b> source
 * of random magical genes and of unlisted disorders: the unbred population,
 * which no registry ever kept.
 *
 * <p>This is a <b>founder</b> path: the {@link Rng} is the wild spawn's, not a
 * seeded one, and consuming a few extra draws for genes that are then
 * overwritten is fine - founders are the one place randomness is free.
 *
 * <p>It is also the <b>only</b> place a breed influences a number. Everything
 * downstream - resolving a body, breeding a foal, reading a record - knows only
 * what the horse itself carries. See {@code BreedStatTargets}.
 */
public final class BreedFounder {

    /** The four magical body-stat gene keys, handled from the breed's stat bands. */
    private static final Set<String> BODY_STAT_KEYS = Set.of(
            "horsegenetics.body_size",
            "horsegenetics.magic_speed",
            "horsegenetics.magic_health",
            "horsegenetics.magic_jump");

    private BreedFounder() {
    }

    /** {@link #roll(Breed, Rng)} with the sex locus forced - the herd systems need a stallion or a mare. */
    public static Genome roll(Breed breed, Rng rng, Sex sex) {
        Genome g = roll(breed, rng);
        return g.withSex(sex);
    }

    public static Genome roll(Breed breed, Rng rng) {
        Genotype base = Genotype.random(rng);
        if (breed == Breeds.FERAL_MIXED) {
            return Genome.of(base, rng);
        }

        // Size is drawn before the genotype pass because it decides the
        // genotype: one copy of the size allele inside 0.7x-1.3x, two outside.
        TargetBand sizeBand = breed.statTargets().band(StatAxis.SCALE);
        double size = sizeBand == null ? Double.NaN : sizeBand.lerp(rng.nextFloat());

        Genotype g = base;
        for (Gene gene : Genes.codeOrder()) {
            String key = gene.key();
            if (key.equals("horsegenetics.sex")) {
                continue; // 50/50 from the base roll, not a breed trait
            }
            if (BODY_STAT_KEYS.contains(key)) {
                g = g.with(bodyStatPair(breed, gene, size));
                continue;
            }
            if (breed.constrains(key)) {
                g = g.with(breed.founderTable(key).draw(rng));
                continue;
            }
            if (gene.feralOnly()) {
                // A curiosity of the unbred population - no registry ever kept
                // it. Checked after the breed's own pool, so a breed that
                // genuinely wants one can still name it.
                g = g.with(wild(gene));
                continue;
            }
            if (gene.affectsCoat() || dependedOnByACoatGene(key)) {
                g = g.with(wild(gene)); // visually unified - no unnamed pattern, no stray modifier
                continue;
            }
            if (isMagical(gene)) {
                g = g.with(wild(gene)); // no stray magic - a breed carries what it names
                continue;
            }
            if (gene instanceof HealthContribution) {
                g = g.with(wild(gene)); // no disorder the breed sheet does not list
            }
            // otherwise: keep the base roll (the natural performance genes)
        }

        return stampBands(breed, stampStatTargets(breed, Genome.of(g, rng), rng, size), rng);
    }

    /**
     * Write the breed's stat bands onto the founder's <b>allele copies</b> - the
     * one and only place a breed touches a number.
     *
     * <p>The genotype pass above already made this horse homozygous for the
     * pushing allele on every axis the breed pins; this picks a factor inside
     * the band and splits it across the two copies, so the horse lands where the
     * breed wants it to. From here the numbers are ordinary epigenetics: they
     * inherit, they drift, and nothing ever pulls them back toward the standard.
     *
     * <p>That last point is the whole change. A Percheron bred to a Falabella
     * now produces a foal carrying one enormous copy and one tiny one - it got
     * whichever copies it got - rather than a horse whose size is recomputed
     * into the average of two breed standards every time it is looked at.
     *
     * <p>The two copies are split <b>unevenly</b> (see {@link #COPY_SKEW}) rather
     * than given half each. Two identical copies would make a founder's two
     * gametes interchangeable, and half the interest in breeding one is that its
     * foals differ depending on which copy they drew.
     */
    private static Genome stampStatTargets(Breed breed, Genome genome, Rng rng, double size) {
        Epigenome epi = genome.epigenome();
        for (Gene gene : Genes.codeOrder()) {
            if (!BODY_STAT_KEYS.contains(gene.key())) {
                continue;
            }
            TargetBand band = breed.statTargets().band(axisOf(gene));
            if (band == null) {
                continue;
            }
            AllelePair pair = genome.genotype().pair(gene);
            if (pair.count(gene.defaultAllele()) == 2) {
                continue;   // the breed pins this axis but the horse lost the allele
            }
            // The gene sums its copies' deltas, so the total distance from 1.0 is
            // what has to land in the band. The sign is the allele's job, so the
            // stored numbers are always positive. Size's target was drawn before
            // the genotype, because it chose how many copies there are.
            boolean isSize = axisOf(gene) == StatAxis.SCALE;
            double total = Math.abs((isSize ? size : band.lerp(rng.nextFloat())) - 1.0);
            Epigenome.Copies c = epi.copies(gene);
            if (pair.count(gene.defaultAllele()) == 1) {
                // One pushing copy: it carries the whole distance, and the wild
                // copy's number is inert (the baseline allele is worth nothing).
                boolean pushFirst = !pair.first().equals(gene.defaultAllele());
                epi = epi.with(gene.key(), new Epigenome.Copies(
                        pushFirst ? withDelta(c.first(), total) : c.first(),
                        pushFirst ? c.second() : withDelta(c.second(), total)));
                continue;
            }
            // Two pushing copies, each carrying a share.
            double share = 0.5 + (rng.nextFloat() - 0.5f) * COPY_SKEW;
            epi = epi.with(gene.key(), new Epigenome.Copies(
                    withDelta(c.first(), total * share),
                    withDelta(c.second(), total * (1.0 - share))));
        }
        return new Genome(genome.genotype(), epi);
    }

    /**
     * Write the breed's <b>epigenetic bands</b> onto the founder's allele
     * copies - the general form of {@link #stampStatTargets}, and the thing
     * that lets a breed say "deeply black" rather than only "black".
     *
     * <p>The two copies are drawn <b>independently</b> inside the band, for the
     * same reason the stat target is split unevenly: a founder whose two copies
     * are identical has interchangeable gametes, and half the interest in
     * breeding one is that its foals differ by which copy they drew.
     *
     * <p>Everything it cannot honour it skips in silence <i>here</i>, because
     * the complaining was already done once, at load time, where there is a
     * file name to complain about - see {@code BreedSpecParser}. A gene that is
     * simply not installed on this machine must cost the breed that locus and
     * nothing else.
     */
    private static Genome stampBands(Breed breed, Genome genome, Rng rng) {
        BreedBands bands = breed.bands();
        if (bands.isEmpty()) {
            return genome;
        }
        Epigenome epi = genome.epigenome();
        for (String key : bands.genes()) {
            if (BODY_STAT_KEYS.contains(key)) {
                continue;   // owned by the breed's stat scores; see BreedBands
            }
            Gene gene = Genes.byKeyOrNull(key);
            if (gene == null) {
                continue;
            }
            EpiSchema schema = gene.epiSchema();
            if (schema.isEmpty()) {
                continue;
            }
            Epigenome.Copies c = epi.copies(gene);
            AlleleEpigenetics first = c.first();
            AlleleEpigenetics second = c.second();
            for (Map.Entry<String, BreedBands.Band> e : bands.forGene(key).entrySet()) {
                int index = schema.indexOf(e.getKey());
                if (index < 0) {
                    continue;
                }
                EpiValue value = schema.get(index);
                if (value.kind() == EpiValue.Kind.SEED) {
                    continue;   // a seed is locked below, never banded
                }
                BreedBands.Band band = e.getValue();
                for (int leg = 0; leg < value.arity(); leg++) {
                    first = withValue(first, value, leg, draw(value, band, rng));
                    second = withValue(second, value, leg, draw(value, band, rng));
                }
            }
            // A locked seed goes on both copies, so every founder draws the same
            // field - and, from there, it inherits like any other seed.
            for (Map.Entry<String, Long> e : bands.seedsFor(key).entrySet()) {
                int index = schema.indexOf(e.getKey());
                if (index < 0 || schema.get(index).kind() != EpiValue.Kind.SEED) {
                    continue;
                }
                first = new AlleleEpigenetics(first.priority(), first.values().withSeed(e.getKey(), e.getValue()));
                second = new AlleleEpigenetics(second.priority(), second.values().withSeed(e.getKey(), e.getValue()));
            }
            epi = epi.with(key, new Epigenome.Copies(first, second));
        }
        return new Genome(genome.genotype(), epi);
    }

    /**
     * One founder's value inside a band. A category is an index, so it is drawn
     * over whole options, inclusive at both ends - {@code [1, 2]} means option
     * one or option two, evenly - where a scalar is drawn across the interval.
     */
    private static double draw(EpiValue value, BreedBands.Band band, Rng rng) {
        if (value.kind() == EpiValue.Kind.CATEGORY) {
            int lo = (int) Math.round(band.lo());
            int hi = (int) Math.round(band.hi());
            return lo + (hi > lo ? rng.nextInt(hi - lo + 1) : 0);
        }
        return band.lerp(rng.nextFloat());
    }

    private static AlleleEpigenetics withValue(AlleleEpigenetics copy, EpiValue value, int leg, double raw) {
        double v = value.clamp(raw);
        return new AlleleEpigenetics(copy.priority(),
                value.arity() == 1
                        ? copy.values().with(value.name(), v)
                        : copy.values().with(value.name(), leg, v));
    }

    /** How unevenly a founder's two copies split the breed's target. */
    private static final double COPY_SKEW = 0.30;

    private static AlleleEpigenetics withDelta(AlleleEpigenetics copy, double delta) {
        return new AlleleEpigenetics(copy.priority(),
                copy.values().with(AbstractMagicStatGene.DELTA, delta));
    }

    private static StatAxis axisOf(Gene gene) {
        return switch (gene.key()) {
            case "horsegenetics.body_size" -> StatAxis.SCALE;
            case "horsegenetics.magic_speed" -> StatAxis.SPEED;
            case "horsegenetics.magic_health" -> StatAxis.HEALTH;
            case "horsegenetics.magic_jump" -> StatAxis.JUMP;
            default -> throw new IllegalStateException(gene.key());
        };
    }

    // ------------------------------------------------------------------

    private static AllelePair bodyStatPair(Breed breed, Gene gene, double size) {
        StatAxis axis = axisOf(gene);
        TargetBand band = breed.statTargets().band(axis);
        if (band == null) {
            return wild(gene);
        }
        // alleles(): index 0 is the "up" allele, 1 is "down", 2 is the wild type
        Allele push = band.pushesUp() ? gene.alleles().get(0) : gene.alleles().get(1);
        if (axis == StatAxis.SCALE && BreedStatCurve.heterozygousSize(size)) {
            return new AllelePair(push, gene.defaultAllele());
        }
        return new AllelePair(push, push);
    }

    private static boolean isMagical(Gene gene) {
        return Genes.magicalOrder().contains(gene);
    }

    /**
     * Is {@code key} a modifier that some painting gene reads
     * ({@link Gene#coatDependsOn()})? Such a gene paints nothing itself, so it
     * would otherwise fall through to "keep the base roll" and could leak onto
     * a breed that does not name it - the leopard complex's {@code PATN1} /
     * {@code PATN2} on a non-Appaloosa. Forced wild here just like a coat gene.
     */
    private static boolean dependedOnByACoatGene(String key) {
        for (Gene g : Genes.codeOrder()) {
            if (g.affectsCoat() && g.coatDependsOn().contains(key)) {
                return true;
            }
        }
        return false;
    }

    private static AllelePair wild(Gene gene) {
        return new AllelePair(gene.defaultAllele(), gene.defaultAllele());
    }
}
