package com.example.horsegenetics.common.coat;

import com.example.horsegenetics.common.genetics.CoatPhenotype;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.GeneCodeDisplay;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.Genotype;

import java.util.Objects;

/**
 * The fully-resolved, ready-to-render description of one horse's coat: its
 * {@link Genome} - the {@link Genotype} plus the {@link Epigenome} carrying a
 * priority + epigenetic seed on <b>each allele copy</b>. That's everything the
 * coat overlay pipeline ({@code coat.pattern.CoatTextureComposer}) needs.
 *
 * <p>Epigenetics are tied to the allele, not the horse: a foal inherits each
 * allele's seed from the parent copy it came from, unchanged. Deterministic
 * coats (black, chestnut, champagne, white) ignore epigenetics entirely - every
 * such horse looks identical, so their texture is generated once and shared.
 * Non-deterministic coats (bay, grey dapples, splash markings) feed the
 * <i>expressed</i> copy's seed into that gene's own RNG, so the same horse
 * regenerates the same skin every session.
 *
 * <p>Nothing below this class knows Minecraft exists.
 */
public final class CoatData {

    /** Fallback for an un-extracted render state: the all-wild-type (plain black) horse. */
    public static final CoatData DEFAULT =
            new CoatData(Genotype.wildType(), Epigenome.fromSeed(0L));

    private final Genome genome;

    public CoatData(Genome genome) {
        this.genome = Objects.requireNonNull(genome, "genome");
    }

    public CoatData(Genotype genotype, Epigenome epigenome) {
        this(new Genome(genotype, epigenome));
    }

    public Genome genome() {
        return genome;
    }

    public Genotype genotype() {
        return genome.genotype();
    }

    public Epigenome epigenome() {
        return genome.epigenome();
    }

    public CoatPhenotype phenotype() {
        return genotype().phenotype();
    }

    /** Is this coat one of the fixed, shareable set (vs. per-horse generated)? */
    public boolean isDeterministic() {
        return genotype().isDeterministic();
    }

    /**
     * Key for caching the generated texture: the genotype code, plus - only
     * when the coat is non-deterministic - a digest of the epigenetics that can
     * actually change its pixels ({@link Epigenome#visibleFingerprint}). So all
     * black horses share one texture, two bays don't, and two bays that differ
     * only in (invisible) grey epigenetics still do.
     */
    public String textureKey() {
        return isDeterministic()
                ? genotype().toCode()
                : genotype().toCode() + "@"
                        + Long.toUnsignedString(epigenome().visibleFingerprint(genotype()), 16);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CoatData c && c.genome.equals(genome);
    }

    @Override
    public int hashCode() {
        return genome.hashCode();
    }

    @Override
    public String toString() {
        return "CoatData[" + GeneCodeDisplay.shortForm(genotype()) + ", epi="
                + Long.toUnsignedString(epigenome().visibleFingerprint(genotype()), 16) + "]";
    }
}
