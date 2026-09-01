package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.Genotype;

/**
 * Scratch space a {@code Gene} draws into while a coat texture is built. Passed
 * to {@code Gene.restrict} (mutate {@link #pigment()}) and
 * {@code Gene.overlayLayer}.
 *
 * <p>Carries the {@link Skin} (adult vs foal geometry) and whether the horse is
 * an <b>adult</b> (grey only greys adults). Non-deterministic genes take all
 * randomness from {@link #epigeneticsFor} so the same horse regenerates the
 * same coat.
 */
public final class CoatBuildContext {

    private final Genotype genotype;
    private final long epigeneticSeed;
    private final Skin skin;
    private final boolean adult;
    private final PigmentField pigment;
    private final int[] overlay;
    private final int size;

    public CoatBuildContext(Genotype genotype, long epigeneticSeed, Skin skin, boolean adult) {
        this.genotype = genotype;
        this.epigeneticSeed = epigeneticSeed;
        this.skin = skin;
        this.adult = adult;
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

    public Rng epigeneticsFor(String geneKey) {
        return new SeededRng(epigeneticSeed, geneKey);
    }
}
