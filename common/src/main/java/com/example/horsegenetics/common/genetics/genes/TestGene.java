package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.ColorField;
import com.example.horsegenetics.common.coat.pattern.ColorView;
import com.example.horsegenetics.common.coat.pattern.PigmentView;
import com.example.horsegenetics.common.coat.pattern.TestCoatPattern;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.DominancePattern;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;

import java.util.List;

/**
 * <b>Test</b> ({@code horsegenetics.test}) - the diagnostic gene, the one
 * <b>magical</b> gene: a dominant {@code T} paints a {@link TestCoatPattern}
 * gradient (pink-&gt;blue along body X, red-&gt;yellow along body Y) <b>flat on
 * top</b> of the resolved coat in phase 3, so the full colourful field is
 * visible on any base - black, chestnut, or white.
 * {@code 1 in} {@value #WILD_TEST_ODDS} carriers (deliberately common while the
 * skin engine is being built). Deterministic. Expect it removed once the engine
 * is trusted.
 */
public final class TestGene implements Gene {

    public static final String KEY = "horsegenetics.test";
    public static final int WILD_TEST_ODDS = 4;

    public final Allele T = new Allele(KEY, "T", "Test (T)", true, true);
    public final Allele t = new Allele(KEY, "t", "Wild-type (t)", false, true);
    private final List<Allele> alleles = List.of(T, t);


    @Override public String key() { return KEY; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele wildType() { return t; }

    /** CompleteDominant: the paint is flat and opaque, so one {@code T} hides whatever is underneath. */
    @Override public DominancePattern dominance() { return DominancePattern.COMPLETE_DOMINANT; }

    @Override
    public boolean isNatural() {
        return false;
    }

    @Override
    public AllelePair randomPair(Rng rng) {
        return rng.nextInt(WILD_TEST_ODDS) == 0 ? new AllelePair(T, t) : new AllelePair(t, t);
    }

    public boolean isTest(AllelePair pair) {
        return pair.has(T);
    }

    @Override
    public boolean isVisible(AllelePair pair, Genotype genotype) {
        return isTest(pair);
    }

    /**
     * Flat, opaque paint - the one magical effect that {@link ColorField#set}s
     * instead of adding, because a diagnostic gradient is only useful if it
     * reads the same over black, chestnut and dominant white alike.
     */
    @Override
    public ColorField tint(AllelePair pair, CoatBuildContext ctx, PigmentView coat, ColorView colour) {
        if (!isTest(pair)) {
            return null;
        }
        ColorField delta = ColorField.deltaLike(colour);
        TestCoatPattern pattern = new TestCoatPattern(HorseSkinGeometry.bodyBounds(ctx.skin()));
        HorseSkinGeometry.forEachTexel(ctx.skin(), (px, py, part, face, point) -> {
            int argb = pattern.argb(point.x(), point.y(), point.z());
            delta.set(px, py, argb >>> 24, (argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF);
        });
        return delta;
    }
}
