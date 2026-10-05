package com.example.horsegenetics.common.coat;

import com.example.horsegenetics.common.coat.pattern.CoatTextureComposer;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.genetics.CoatPhenotype;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.GeneCodeDisplay;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.GrownParts;
import com.example.horsegenetics.common.parts.AttachedPart;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

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
     * Key for caching the generated texture: the genotype's
     * {@link Genotype#coatCode() coat code} - the genes that can paint
     * something, so a purely heritable locus like sex doesn't fork the cache -
     * plus, only when the coat is non-deterministic, a digest of the
     * epigenetics that can actually change its pixels
     * ({@link Epigenome#visibleFingerprint}). So all black horses share one
     * texture, two bays don't, and two bays that differ only in (invisible)
     * grey epigenetics still do.
     */
    public String textureKey() {
        String key = textureKey;
        if (key == null) {
            key = isDeterministic()
                    ? genotype().coatCode()
                    : genotype().coatCode() + "@"
                            + Long.toUnsignedString(epigenome().visibleFingerprint(genotype()), 16);
            textureKey = key;
        }
        return key;
    }

    // ------------------------------------------------------------------
    // Memoised (#205). The renderer asks every one of these once per horse per
    // frame, and each walks the whole genome to answer. A CoatData cannot change
    // after it is built (Genotype and Epigenome hold final unmodifiable maps), so
    // there is nothing to invalidate, and the client keeps one CoatData per horse
    // across frames (ClientCoatCache) - which is what makes the memo pay.
    // Unsynchronised on purpose, the way String.hashCode is: two threads racing
    // compute equal values, and each value is immutable, so either write is fine.
    // ------------------------------------------------------------------

    private String textureKey;
    private String adultKey;
    private String foalKey;
    private String adultGlowKey;
    private String foalGlowKey;
    private Set<Part> glowParts;
    private List<AttachedPart> grownParts;

    /**
     * The coat texture's cache key for one mesh: {@link #textureKey()} plus which
     * of the two skins it is painted on, because a foal's sheet is a different
     * layout from an adult's.
     */
    public String meshKey(boolean baby) {
        String key = baby ? foalKey : adultKey;
        if (key == null) {
            key = textureKey() + (baby ? ":foal" : ":adult");
            if (baby) {
                foalKey = key;
            } else {
                adultKey = key;
            }
        }
        return key;
    }

    /**
     * The glow mask's cache key for one mesh. The gene-painted half of a mask is a
     * function of the texture key already, so only the {@link #glowParts() lit parts}
     * join it, in a fixed order.
     */
    public String glowKey(boolean baby) {
        String key = baby ? foalGlowKey : adultGlowKey;
        if (key == null) {
            StringBuilder tag = new StringBuilder();
            for (Part part : new TreeSet<>(glowParts())) {
                tag.append(tag.length() == 0 ? "" : "+").append(part.name());
            }
            key = meshKey(baby) + ":glow:" + tag;
            if (baby) {
                foalGlowKey = key;
            } else {
                adultGlowKey = key;
            }
        }
        return key;
    }

    /** The body parts a {@code glow} effect lights outright - {@link CoatTextureComposer#glowParts}. */
    public Set<Part> glowParts() {
        Set<Part> parts = glowParts;
        if (parts == null) {
            parts = Collections.unmodifiableSet(CoatTextureComposer.glowParts(genotype()));
            glowParts = parts;
        }
        return parts;
    }

    /** The parts this horse grows - {@link GrownParts#of}, already an immutable list. */
    public List<AttachedPart> grownParts() {
        List<AttachedPart> parts = grownParts;
        if (parts == null) {
            parts = GrownParts.of(genotype(), epigenome());
            grownParts = parts;
        }
        return parts;
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
