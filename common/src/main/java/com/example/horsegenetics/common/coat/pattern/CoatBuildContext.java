package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.genetics.Genotype;

/**
 * Scratch space a {@code Gene} draws into while a coat texture is built.
 * Passed to {@code Gene.restrict} (mutate {@link #pigment()}) and
 * {@code Gene.paint} (draw ARGB into {@link #overlay()}).
 *
 * <p>Non-deterministic genes must take all of their randomness from
 * {@link #epigeneticsFor} so the same horse regenerates the same coat.
 */
public final class CoatBuildContext {

    private final Genotype genotype;
    private final long epigeneticSeed;
    private final PigmentField pigment;
    private final int[] overlay;
    private final int size;

    public CoatBuildContext(Genotype genotype, long epigeneticSeed) {
        this.genotype = genotype;
        this.epigeneticSeed = epigeneticSeed;
        this.size = HorseSkinGeometry.SHEET_SIZE;
        this.pigment = new PigmentField(size);
        this.overlay = new int[size * size];
    }

    public Genotype genotype() {
        return genotype;
    }

    public long epigeneticSeed() {
        return epigeneticSeed;
    }

    public int size() {
        return size;
    }

    /** The red/black restriction map genes mutate in the {@code restrict} pass. */
    public PigmentField pigment() {
        return pigment;
    }

    /**
     * The row-major ARGB buffer. {@link CoatTextureComposer} fills it from the
     * pigment field + gradient LUT before the {@code paint} pass; paint genes
     * overwrite texels here.
     */
    public int[] overlay() {
        return overlay;
    }

    public void setOverlay(int px, int py, int argb) {
        overlay[py * size + px] = argb;
    }

    /**
     * A deterministic {@link Rng} for one gene's per-horse randomness, seeded
     * from this horse's epigenetic seed and the gene key. Call once per gene
     * and pull every value from it in a fixed order.
     */
    public Rng epigeneticsFor(String geneKey) {
        return new SeededRng(epigeneticSeed, geneKey);
    }
}
