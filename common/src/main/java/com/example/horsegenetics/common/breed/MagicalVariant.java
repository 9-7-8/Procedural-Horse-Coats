package com.example.horsegenetics.common.breed;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.GenotypeCatalog;
import com.example.horsegenetics.common.trait.HealthContribution;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * <b>A magical herd of an ordinary breed</b> (owner, 2026-09-15). Now and then - {@link BreedSpawnSettings.Magical#chance},
 * five herds in a hundred by default - a wild herd of a breed is founded as its magical version: it lives in the same
 * biomes, and <b>the whole herd shares one magical gene, every horse with the same pair of alleles</b>. It reads as
 * {@link BreedLineage.Kind#MAGICAL Magical (Breed)}. Crossed back to its own breed a foal stays Magical; crossed to
 * anything else it is the ordinary cross.
 *
 * <p>It exists because a breed carries exactly its sheet ({@link BreedFounder}): no stray magic. So nearly every horse
 * the world spawns is natural, and magical genes were only ever met on Feral Mixed. A magical herd is where they
 * appear in a named breed, without giving any breed a background rate of magic.
 *
 * <p>Every breed has magical herds unless its file says {@code "magical_variant": false}.
 *
 * <h2>What may be picked</h2>
 * Any registered magical gene, except
 * <ul>
 *   <li>the four body-stat loci, which the breed's stat scores own;</li>
 *   <li>a gene the breed's sheet or one of its strains already names, since the sheet decides those;</li>
 *   <li>a {@link Gene#feralOnly() feral-only} curiosity, which no registry keeps;</li>
 *   <li>a sex-linked gene, because one pair has to fit mares and stallions alike;</li>
 *   <li>a gene that is a health contribution, so a magical herd is never a sick one.</li>
 * </ul>
 * The pair is one that <b>shows</b> - never the wild type or a silent carrier - so a magical herd is magical to look at
 * or to live with, not only on paper. Gene and pair are both drawn evenly.
 *
 * <p>Pure: a host passes an {@link Rng} seeded from the herd's lead, so every member of the herd - founded now or
 * joining later - draws the same answer.
 */
public record MagicalVariant(Gene gene, AllelePair pair) {

    /**
     * Whether a herd of {@code breed} founded with {@code rng} is magical, and if so with what. Empty for Feral Mixed,
     * a breed that opted out, magical herds switched off, a roll above the chance, or a breed with nothing to pick.
     */
    public static Optional<MagicalVariant> roll(Breed breed, BreedSpawnSettings.Magical settings, Rng rng) {
        if (breed == null || breed == Breeds.FERAL_MIXED || !breed.magicalVariant()
                || settings == null || !settings.enabled() || settings.chance() <= 0.0) {
            return Optional.empty();
        }
        if (rng.nextFloat() >= settings.chance()) {
            return Optional.empty();
        }
        return pick(breed, rng);
    }

    /** One gene and one showing pair for a magical herd of {@code breed}, ignoring the chance. */
    public static Optional<MagicalVariant> pick(Breed breed, Rng rng) {
        List<Gene> genes = candidates(breed);
        if (genes.isEmpty()) {
            return Optional.empty();
        }
        Gene gene = genes.get(rng.nextInt(genes.size()));
        List<AllelePair> pairs = showingPairs(gene);
        return Optional.of(new MagicalVariant(gene, pairs.get(rng.nextInt(pairs.size()))));
    }

    /** Every magical gene a magical herd of {@code breed} may carry, in {@link Genes#magicalOrder()}. */
    public static List<Gene> candidates(Breed breed) {
        List<Gene> out = new ArrayList<>();
        for (Gene gene : Genes.magicalOrder()) {
            if (eligible(breed, gene)) {
                out.add(gene);
            }
        }
        return out;
    }

    /** The pairs of {@code gene} a horse of either sex can carry that are not a wild type. */
    public static List<AllelePair> showingPairs(Gene gene) {
        List<AllelePair> out = new ArrayList<>();
        for (AllelePair pair : GenotypeCatalog.allPairsOf(gene)) {
            if (!gene.expressionOf(pair).wildType()) {
                out.add(pair);
            }
        }
        return out;
    }

    private static boolean eligible(Breed breed, Gene gene) {
        if (BreedFounder.BODY_STAT_KEYS.contains(gene.key())
                || gene.feralOnly()
                || gene.inheritance().sexLinked()
                || gene instanceof HealthContribution
                || namedBySheet(breed, gene.key())) {
            return false;
        }
        return !showingPairs(gene).isEmpty();
    }

    private static boolean namedBySheet(Breed breed, String key) {
        if (breed.genePools().containsKey(key)) {
            return true;
        }
        for (Breed.Strain strain : breed.strains()) {
            if (strain.genePools().containsKey(key)) {
                return true;
            }
        }
        return false;
    }
}
