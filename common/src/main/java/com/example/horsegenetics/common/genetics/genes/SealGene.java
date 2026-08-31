package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Axis;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Bounds;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;

import java.util.List;

/**
 * <b>Seal</b> ({@code horsegenetics.seal}) - a dominant black-shading modifier
 * (real-horse {@code A^t}, split out into its own gene here). Only visible on a
 * horse that makes black pigment.
 *
 * <p>Effect (natural, so it only pushes black around): the body is barely
 * lightened; the <b>lower legs and lower face fade to full black</b> - densest
 * at the hoof / muzzle, easing back to the body level by a <b>random</b> point
 * up the leg / face (a smooth gradient, not a hard edge). Non-deterministic
 * (those fade points are part of the epigenetic value).
 */
public final class SealGene implements Gene {

    public static final String KEY = "horsegenetics.seal";
    public static final int WILD_SEAL_ALLELE_ODDS = 16;

    /** How dark the seal body sits (black kept). 1 = pure black, lower = a touch browner. TUNE. */
    private static final float BODY_BLACK = 0.82f;
    /**
     * The darkest the leg / face fade goes - kept just <b>under</b> 1.0 on
     * purpose so it never lands on the composer's pure-black-lift and the
     * gradient stays monotonic all the way to the hoof.
     */
    private static final float DEEPEST_BLACK = 0.985f;

    public final Allele Sl = new Allele(KEY, "Sl", "Seal (Sl)", true, false);
    public final Allele sl = new Allele(KEY, "sl", "Wild-type (sl)", false, true);
    private final List<Allele> alleles = List.of(Sl, sl);

    @Override public String key() { return KEY; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele wildType() { return sl; }

    @Override
    public AllelePair randomPair(Rng rng) {
        return new AllelePair(
                rng.nextInt(WILD_SEAL_ALLELE_ODDS) == 0 ? Sl : sl,
                rng.nextInt(WILD_SEAL_ALLELE_ODDS) == 0 ? Sl : sl);
    }

    public boolean isSeal(AllelePair pair) {
        return pair.has(Sl);
    }

    @Override
    public boolean isVisible(AllelePair pair, Genotype genotype) {
        return isSeal(pair) && genotype.hasBlackPigment();
    }

    @Override
    public boolean isDeterministic(AllelePair pair, Genotype genotype) {
        return !isVisible(pair, genotype);
    }

    @Override
    public void restrict(AllelePair pair, CoatBuildContext ctx) {
        if (!isSeal(pair)) {
            return;
        }
        Rng epi = ctx.epigeneticsFor(KEY);
        PigmentField f = ctx.pigment();

        // barely lighten the body
        HorseSkinGeometry.forEachTexel((px, py, part, face, point) ->
                f.setBlack(px, py, Math.min(f.black(px, py), BODY_BLACK)));

        // legs: full black at the hoof, easing to the body level by a random height
        for (Part leg : List.of(Part.LEFT_FRONT_LEG, Part.RIGHT_FRONT_LEG, Part.LEFT_HIND_LEG, Part.RIGHT_HIND_LEG)) {
            double fade = 0.25 + epi.nextFloat() * 0.55; // fraction of leg height the fade completes by
            Bounds b = HorseSkinGeometry.bounds(leg);
            HorseSkinGeometry.forEachTexel(leg, (px, py, part, face, point) -> {
                double frac = (point.y() - b.yMin()) / b.span(Axis.Y);
                float extra = ramp(frac, fade);            // 1 at hoof -> 0 by `fade`
                f.setBlack(px, py, lerp(f.black(px, py), DEEPEST_BLACK, extra));
            });
        }

        // face: deepest black at the muzzle, easing up over a random length of the head
        double faceFade = 0.20 + epi.nextFloat() * 0.55;
        Bounds head = HorseSkinGeometry.bounds(Part.HEAD);
        HorseSkinGeometry.forEachTexel(Part.MUZZLE, (px, py, part, face, point) -> f.setBlack(px, py, DEEPEST_BLACK));
        HorseSkinGeometry.forEachTexel(Part.HEAD, (px, py, part, face, point) -> {
            double fromNose = (head.xMax() - point.x()) / head.span(Axis.X);
            float extra = ramp(fromNose, faceFade);
            f.setBlack(px, py, lerp(f.black(px, py), DEEPEST_BLACK, extra));
        });
    }

    /** 1 at {@code t == 0}, linearly to 0 at {@code t == end}, then 0. */
    private static float ramp(double t, double end) {
        if (t >= end) {
            return 0f;
        }
        return (float) (1.0 - t / end);
    }

    private static float lerp(float from, float to, float k) {
        return from + (to - from) * k;
    }
}
