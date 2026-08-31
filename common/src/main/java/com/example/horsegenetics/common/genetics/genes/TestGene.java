package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.TestCoatPattern;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;

import java.util.List;

/**
 * <b>Test</b> ({@code horsegenetics.test}) - the diagnostic gene from the last
 * pass, now folded into the gene model. A dominant {@code T} <b>paints</b> the
 * {@link TestCoatPattern} gradient over the resolved coat (pink-&gt;blue along
 * body X, red-&gt;yellow along body Y), so it exercises {@code HorseSkinGeometry}
 * end to end. Deliberately common in the wild ({@code 1 in}
 * {@value #WILD_TEST_ODDS}). Deterministic. Expect it removed once the skin
 * engine is trusted.
 */
public final class TestGene implements Gene {

    public static final String KEY = "horsegenetics.test";
    public static final int WILD_TEST_ODDS = 4;

    public final Allele T = new Allele(KEY, "T", 'T', "Test (T)", true, true);
    public final Allele t = new Allele(KEY, "t", 't', "Wild-type (t)", false, true);
    private final List<Allele> alleles = List.of(T, t);

    private final TestCoatPattern pattern = new TestCoatPattern();

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public List<Allele> alleles() {
        return alleles;
    }

    @Override
    public Allele wildType() {
        return t;
    }

    @Override
    public AllelePair randomPair(Rng rng) {
        // single 1-in-N roll for a carrier (Tt), not per-allele
        return rng.nextInt(WILD_TEST_ODDS) == 0 ? new AllelePair(T, t) : new AllelePair(t, t);
    }

    public boolean isTest(AllelePair pair) {
        return pair.has(T);
    }

    @Override
    public boolean isVisible(AllelePair pair, Genotype genotype) {
        return isTest(pair);
    }

    @Override
    public void paint(AllelePair pair, CoatBuildContext ctx) {
        if (!isTest(pair)) {
            return;
        }
        int[] overlay = ctx.overlay();
        int n = ctx.size();
        HorseSkinGeometry.forEachTexel((px, py, part, face, point) ->
                overlay[py * n + px] = pattern.argb(point.x(), point.y(), point.z()));
    }
}
