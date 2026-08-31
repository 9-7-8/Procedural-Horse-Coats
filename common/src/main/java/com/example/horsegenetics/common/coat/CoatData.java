package com.example.horsegenetics.common.coat;

import com.example.horsegenetics.common.genetics.CoatPhenotype;
import com.example.horsegenetics.common.genetics.Genotype;

import java.util.Objects;

/**
 * The fully-resolved, ready-to-render description of one horse's coat: its
 * {@link Genotype} plus its <b>epigenetic seed</b>. That's everything the coat
 * overlay pipeline ({@code coat.pattern.CoatTextureComposer}) needs.
 *
 * <p>The epigenetic seed is rolled once at birth and stored. Deterministic
 * coats (black, chestnut, champagne, white) ignore it - every such horse looks
 * identical, so their texture is generated once and shared. Non-deterministic
 * coats (bay, seal, white markings) feed the seed into each gene's own RNG so
 * the same horse regenerates the same skin every session.
 *
 * <p>Nothing below this class knows Minecraft exists.
 */
public final class CoatData {

    /** Fallback for an un-extracted render state: a plain black horse. */
    public static final CoatData DEFAULT = new CoatData(Genotype.parse("Eeaawwttcc"), 0L);

    private final Genotype genotype;
    private final long epigeneticSeed;

    public CoatData(Genotype genotype, long epigeneticSeed) {
        this.genotype = Objects.requireNonNull(genotype, "genotype");
        this.epigeneticSeed = epigeneticSeed;
    }

    public Genotype genotype() {
        return genotype;
    }

    public long epigeneticSeed() {
        return epigeneticSeed;
    }

    public CoatPhenotype phenotype() {
        return genotype.phenotype();
    }

    /** Is this coat one of the fixed, shareable set (vs. per-horse generated)? */
    public boolean isDeterministic() {
        return genotype.isDeterministic();
    }

    /**
     * Key for caching the generated texture: the genotype code, plus the
     * epigenetic seed only when the coat is non-deterministic (so all black
     * horses share one texture, but two bays don't).
     */
    public String textureKey() {
        return isDeterministic()
                ? genotype.toCode()
                : genotype.toCode() + "@" + Long.toUnsignedString(epigeneticSeed);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CoatData c
                && c.epigeneticSeed == epigeneticSeed
                && c.genotype.equals(genotype);
    }

    @Override
    public int hashCode() {
        return Objects.hash(genotype, epigeneticSeed);
    }

    @Override
    public String toString() {
        return "CoatData[" + genotype.toCode() + ", epi=" + Long.toUnsignedString(epigeneticSeed) + "]";
    }
}
