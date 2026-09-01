package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;

/**
 * Scratch space a {@code Gene} draws into while a coat texture is built. Passed
 * to {@code Gene.restrict} (mutate {@link #pigment()}) and
 * {@code Gene.overlayLayer}.
 *
 * <p>Carries the {@link Skin} (adult vs foal geometry) and whether the horse is
 * an <b>adult</b> (grey only greys adults). Non-deterministic genes take all
 * randomness from {@link #epigeneticsFor}, which runs on the seed of the
 * <b>allele copy that expresses</b> at that gene, so the same horse regenerates
 * the same coat and a foal that inherited the copy regenerates its parent's.
 */
public final class CoatBuildContext {

    private final Genotype genotype;
    private final Epigenome epigenome;
    private final Skin skin;
    private final boolean adult;
    private final PigmentField pigment;
    private final int[] overlay;
    private final int size;

    public CoatBuildContext(Genotype genotype, Epigenome epigenome, Skin skin, boolean adult) {
        this.genotype = genotype;
        this.epigenome = epigenome;
        this.skin = skin;
        this.adult = adult;
        this.size = HorseSkinGeometry.SHEET_SIZE;
        this.pigment = new PigmentField(size);
        this.overlay = new int[size * size];
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

    public PigmentField pigment() {
        return pigment;
    }

    public int[] overlay() {
        return overlay;
    }

    /**
     * This horse's randomness for one gene: a {@link SeededRng} on the
     * epigenetic seed of the allele copy expressing at {@code geneKey}
     * (heterozygote - the dominant copy; homozygote - the higher-priority one).
     */
    public Rng epigeneticsFor(String geneKey) {
        return new SeededRng(epigenome.expressedSeed(Genes.byKey(geneKey), genotype), geneKey);
    }
}
