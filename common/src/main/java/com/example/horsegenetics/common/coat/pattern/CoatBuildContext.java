package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;

/**
 * Everything a {@code Gene} needs to know about the <b>horse</b> while a coat
 * texture is built: its genotype and epigenome, the {@link Skin} (adult vs foal
 * geometry) and whether it is an <b>adult</b> (grey only greys adults).
 *
 * <p>It is <b>immutable and carries no scratch space</b>. The pigment and
 * colour fields are the composer's, handed to a gene as read-only views on the
 * call; a gene returns its contribution rather than drawing into shared state.
 * See {@code Gene.restrict} / {@code Gene.tint}.
 *
 * <p>Non-deterministic genes take all randomness from {@link #epigeneticsFor},
 * which runs on the seed of the <b>allele copy that expresses</b> at that gene,
 * so the same horse regenerates the same coat and a foal that inherited the
 * copy regenerates its parent's.
 */
public final class CoatBuildContext {

    private final Genotype genotype;
    private final Epigenome epigenome;
    private final Skin skin;
    private final boolean adult;
    private final int size;

    public CoatBuildContext(Genotype genotype, Epigenome epigenome, Skin skin, boolean adult) {
        this.genotype = genotype;
        this.epigenome = epigenome;
        this.skin = skin;
        this.adult = adult;
        this.size = HorseSkinGeometry.SHEET_SIZE;
    }

    public Genotype genotype() {
        return genotype;
    }

    public Epigenome epigenome() {
        return epigenome;
    }

    public Skin skin() {
        return skin;
    }

    /** True for grown horses; false for foals. Grey only greys adults. */
    public boolean isAdult() {
        return adult;
    }

    public int size() {
        return size;
    }

    /**
     * This horse's randomness for one gene: a {@link SeededRng} on the
     * epigenetic seed of the allele copy expressing at {@code geneKey}
     * (heterozygote - the dominant copy; homozygote - the higher-priority one).
     */
    public Rng epigeneticsFor(String geneKey) {
        return new SeededRng(epigenome.expressedSeed(Genes.byKey(geneKey), genotype), geneKey);
    }

    /**
     * This horse's randomness for <b>one particular allele copy</b> at a gene -
     * {@code slot} 0 is {@code pair.first()}, 1 is {@code pair.second()}.
     *
     * <p>{@link #epigeneticsFor(String)} answers "what does this horse show at
     * this gene", which is the right question almost everywhere: one locus, one
     * look, so one seed. It is the wrong question for a gene whose two alleles
     * each paint <i>at the same time</i> - a mane that is solid in one copy's
     * colour and striped in the other's needs both numbers, and asking for "the
     * expressed one" would silently paint the stripes in the base colour.
     *
     * <p>Derived identically to the single-copy form, so for a heterozygote
     * {@code epigeneticsForCopy(key, 0)} and {@code epigeneticsFor(key)} are the
     * same generator.
     */
    public Rng epigeneticsForCopy(String geneKey, int slot) {
        Epigenome.Copies copies = epigenome.copies(geneKey);
        long seed = (slot == 0 ? copies.first() : copies.second()).epigeneticSeed();
        return new SeededRng(seed, geneKey);
    }
}
