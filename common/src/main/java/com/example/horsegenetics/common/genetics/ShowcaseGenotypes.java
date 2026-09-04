package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.Rng;

import java.util.ArrayList;
import java.util.List;

/**
 * A founder draw for a horse that is <b>worth looking at</b> - the horse
 * dimension's stock.
 *
 * <p>An ordinary {@link Genotype#random} founder is the honest wild
 * distribution, and the honest wild distribution is mostly plain horses: every
 * founder table is weighted hard toward its baseline, so a corridor of wild
 * draws is a corridor of bays and chestnuts with the occasional grey. That is
 * correct for a wild spawn and useless for a gallery whose whole job is to show
 * what the genes do.
 *
 * <p>So a showcase horse is a wild draw with a floor under it:
 * <ul>
 *   <li>extension and agouti are whatever they rolled - they are the base coat
 *       colour and every horse has them;</li>
 *   <li><b>at least one other natural coat gene is expressing</b> - if the wild
 *       draw already produced one, it is left exactly as it fell, and only an
 *       all-baseline draw gets a gene forced;</li>
 *   <li>and with probability {@value #MAGICAL_CHANCE}, <b>at least one magical
 *       gene is expressing</b> too.</li>
 * </ul>
 *
 * <p>Forcing picks a random combination from a random candidate gene and keeps
 * only one that actually expresses <i>in this genotype</i>
 * ({@link Genotype#shows}) - so it cannot hand a chestnut horse an agouti it
 * will not paint. Combinations that {@link Expression#masks} are never forced:
 * a gene that hides every other gene is the one thing more boring than a plain
 * horse.
 *
 * <p>Pure, and a founder path: randomness here is legitimate because these
 * horses have no parents (see {@code wiki/philosophy.html}). Nothing else in
 * the mod should call it - a wild spawn is a wild spawn.
 */
public final class ShowcaseGenotypes {

    /** How often a showcase horse is guaranteed a magical gene as well. */
    public static final float MAGICAL_CHANCE = 0.5f;

    private ShowcaseGenotypes() {
    }

    /**
     * One showcase founder. Draws: the ordinary per-gene founder roll, then one
     * {@link Rng#nextFloat()} for the magical coin, plus a small handful of
     * {@link Rng#nextInt} picks per locus actually forced.
     */
    public static Genotype random(Rng rng) {
        Genotype genotype = force(Genotype.random(rng), naturalShowcaseGenes(), rng);
        if (rng.nextFloat() < MAGICAL_CHANCE) {
            genotype = force(genotype, magicalShowcaseGenes(), rng);
        }
        return genotype;
    }

    /**
     * The natural genes a showcase horse may be given: every registered natural
     * gene that paints, minus extension and agouti, which every horse already
     * carries and which are the base colour rather than a marking. Derived from
     * the registry, so a drop-in natural gene joins the pool on its own.
     */
    public static List<Gene> naturalShowcaseGenes() {
        List<Gene> out = new ArrayList<>();
        for (Gene gene : Genes.naturalOrder()) {
            if (gene == Genes.EXTENSION || gene == Genes.AGOUTI || !gene.affectsCoat()) {
                continue;
            }
            out.add(gene);
        }
        return List.copyOf(out);
    }

    /** Every registered magical gene that paints. */
    public static List<Gene> magicalShowcaseGenes() {
        List<Gene> out = new ArrayList<>();
        for (Gene gene : Genes.magicalOrder()) {
            if (gene.affectsCoat()) {
                out.add(gene);
            }
        }
        return List.copyOf(out);
    }

    /**
     * Every combination of {@code gene} worth putting on a showcase horse: it
     * has to be carryable ({@link Gene#canOccur}), it has to do something
     * ({@link Expression#wildType()} is false), and it must not
     * {@link Expression#masks() mask} everything else.
     */
    public static List<AllelePair> showcasePairs(Gene gene) {
        List<AllelePair> out = new ArrayList<>();
        for (AllelePair pair : GenotypeCatalog.allPairsOf(gene)) {
            Expression e = gene.expressionOf(pair);
            if (!e.wildType() && !e.masks()) {
                out.add(pair);
            }
        }
        return List.copyOf(out);
    }

    /**
     * Leave {@code genotype} alone if any of {@code candidates} already
     * expresses on it; otherwise give it one that does. Genes and combinations
     * are both tried in a random rotation and the first that actually
     * {@link Genotype#shows} is kept, so a context-dependent gene (agouti on a
     * chestnut) is passed over rather than silently added and not painted. If
     * nothing at all lands - no candidate genes registered - the genotype comes
     * back untouched rather than throwing.
     *
     * <p>A gene that already expresses but {@link Expression#masks() masks}
     * does <b>not</b> satisfy the floor: the diagnostic test gene is carried by
     * a quarter of all founders and paints flat over everything, so counting it
     * would quietly exempt a quarter of the corridor from the guarantee. The
     * rule is the same one that keeps masking combinations out of the forced
     * pool, applied to the draw that came in.
     */
    private static Genotype force(Genotype genotype, List<Gene> candidates, Rng rng) {
        for (Gene gene : candidates) {
            Expression e = genotype.expressionOf(gene);
            if (!e.wildType() && !e.masks()) {
                return genotype;
            }
        }
        List<Gene> pool = new ArrayList<>();
        for (Gene gene : candidates) {
            if (!showcasePairs(gene).isEmpty()) {
                pool.add(gene);
            }
        }
        if (pool.isEmpty()) {
            return genotype;
        }
        int geneStart = rng.nextInt(pool.size());
        for (int i = 0; i < pool.size(); i++) {
            Gene gene = pool.get((geneStart + i) % pool.size());
            List<AllelePair> pairs = showcasePairs(gene);
            int pairStart = rng.nextInt(pairs.size());
            for (int k = 0; k < pairs.size(); k++) {
                Genotype candidate = genotype.with(pairs.get((pairStart + k) % pairs.size()));
                if (candidate.shows(gene)) {
                    return candidate;
                }
            }
        }
        return genotype;
    }
}
