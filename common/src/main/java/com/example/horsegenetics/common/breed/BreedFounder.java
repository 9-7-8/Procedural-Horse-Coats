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

import java.util.ArrayList;
import java.util.List;
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
 *       {@link Breed#statTargets()} - homozygous for the pushing allele on any
 *       axis the breed pins, wild on the rest;</li>
 *   <li><b>every other magical gene</b> is forced wild, then the geometric
 *       {@link #rollMagic magic-gene draw} switches a few back on.</li>
 * </ol>
 * Disorder genes the breed does not name keep whatever the base roll gave them
 * (so a bred line can still surface a carrier); a {@link Breed#hardy() hardy}
 * breed clears them too.
 *
 * <p>{@link Breeds#UNKNOWN} skips all of this and returns the base roll
 * untouched - the pre-breeds behaviour, exactly.
 *
 * <p>This is a <b>founder</b> path: the {@link Rng} is the wild spawn's, not a
 * seeded one, and consuming a few extra draws for genes that are then
 * overwritten is fine - founders are the one place randomness is free.
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
        if (breed == Breeds.UNKNOWN) {
            return Genome.of(base, rng);
        }

        Genotype g = base;
        for (Gene gene : Genes.codeOrder()) {
            String key = gene.key();
            if (key.equals("horsegenetics.sex")) {
                continue; // 50/50 from the base roll, not a breed trait
            }
            if (BODY_STAT_KEYS.contains(key)) {
                g = g.with(bodyStatPair(breed, gene));
                continue;
            }
            if (breed.constrains(key)) {
                g = g.with(breed.founderTable(key).draw(rng));
                continue;
            }
            if (gene.affectsCoat()) {
                g = g.with(wild(gene)); // visually unified - no unnamed pattern
                continue;
            }
            if (isMagical(gene)) {
                g = g.with(wild(gene)); // cleared; the geometric draw adds some back
                continue;
            }
            if (breed.hardy() && gene instanceof HealthContribution) {
                g = g.with(wild(gene));
            }
            // otherwise: keep the base roll (disorder carriers, natural performance genes)
        }

        g = rollMagic(breed, g, rng);
        return Genome.of(g, rng);
    }

    // ------------------------------------------------------------------

    private static Genotype rollMagic(Breed breed, Genotype g, Rng rng) {
        List<Gene> pool = new ArrayList<>();
        for (Gene gene : Genes.magicalOrder()) {
            String key = gene.key();
            if (BODY_STAT_KEYS.contains(key)) {
                continue;
            }
            if (breed.magicBlacklist().contains(key)) {
                continue;
            }
            if (!breed.magicWhitelist().isEmpty() && !breed.magicWhitelist().contains(key)) {
                continue;
            }
            pool.add(gene);
        }

        double p = breed.magicChance();
        int picks = 0;
        while (picks < 10 && !pool.isEmpty() && rng.nextFloat() < p) {
            Gene gene = pool.remove(rng.nextInt(pool.size()));
            g = g.with(oneVariant(gene, rng));
            picks++;
            p *= 0.5;
        }
        return g;
    }

    /** A single random variant copy against the wild type. */
    private static AllelePair oneVariant(Gene gene, Rng rng) {
        List<Allele> variants = new ArrayList<>();
        for (Allele a : gene.alleles()) {
            if (!a.equals(gene.defaultAllele())) {
                variants.add(a);
            }
        }
        Allele v = variants.get(rng.nextInt(variants.size()));
        return new AllelePair(v, gene.defaultAllele());
    }

    private static AllelePair bodyStatPair(Breed breed, Gene gene) {
        StatAxis axis = switch (gene.key()) {
            case "horsegenetics.body_size" -> StatAxis.SCALE;
            case "horsegenetics.magic_speed" -> StatAxis.SPEED;
            case "horsegenetics.magic_health" -> StatAxis.HEALTH;
            case "horsegenetics.magic_jump" -> StatAxis.JUMP;
            default -> throw new IllegalStateException(gene.key());
        };
        TargetBand band = breed.statTargets().band(axis);
        if (band == null) {
            return wild(gene);
        }
        // alleles(): index 0 is the "up" allele, 1 is "down", 2 is the wild type
        Allele push = band.pushesUp() ? gene.alleles().get(0) : gene.alleles().get(1);
        return new AllelePair(push, push);
    }

    private static boolean isMagical(Gene gene) {
        return Genes.magicalOrder().contains(gene);
    }

    private static AllelePair wild(Gene gene) {
        return new AllelePair(gene.defaultAllele(), gene.defaultAllele());
    }
}
