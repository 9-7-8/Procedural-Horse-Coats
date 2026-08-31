package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.pattern.BayCoat;
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
 * <b>Agouti</b> ({@code horsegenetics.agouti}) - where black pigment is allowed
 * on a horse that can make it.
 * <ul>
 *   <li>{@code A} (most dominant): restrict black to the <b>points</b> - a bay
 *       horse: red-brown body, black mane/tail/ears/hooves, and black that
 *       climbs a random amount up the legs and face.</li>
 *   <li>{@code S} ({@code A^t}, seal brown): mostly black, but tan lifts a
 *       random amount up the lower legs (and the muzzle).</li>
 *   <li>{@code a} (recessive, wild-type): no restriction - black everywhere.</li>
 * </ul>
 * {@code A_} and {@code S_} are <b>non-deterministic</b> (the random heights are
 * the epigenetic value); {@code aa} is deterministic.
 */
public final class AgoutiGene implements Gene {

    public static final String KEY = "horsegenetics.agouti";

    public final Allele A = new Allele(KEY, "A", 'A', "Agouti / bay (A)", true, false);
    public final Allele S = new Allele(KEY, "At", 'S', "Seal brown (A^t)", true, false);
    public final Allele a = new Allele(KEY, "a", 'a', "Non-agouti (a)", false, true);
    private final List<Allele> alleles = List.of(A, S, a);

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
        return a;
    }

    @Override
    public AllelePair randomPair(Rng rng) {
        return new AllelePair(rollAllele(rng), rollAllele(rng));
    }

    private Allele rollAllele(Rng rng) {
        int r = rng.nextInt(20);       // 45% A, 45% a, 10% seal
        return r < 9 ? A : (r < 18 ? a : S);
    }

    public boolean isBay(AllelePair pair) {
        return pair.has(A);
    }

    public boolean isSeal(AllelePair pair) {
        return !pair.has(A) && pair.has(S);
    }

    @Override
    public boolean isVisible(AllelePair pair, Genotype genotype) {
        // agouti only shows if the horse can make black at all
        return (isBay(pair) || isSeal(pair)) && genotype.hasBlackPigment();
    }

    @Override
    public boolean isDeterministic(AllelePair pair, Genotype genotype) {
        return !isVisible(pair, genotype); // bay / seal carry random point heights
    }

    @Override
    public void restrict(AllelePair pair, CoatBuildContext ctx) {
        if (isBay(pair)) {
            BayCoat.apply(ctx, ctx.epigeneticsFor(KEY));
        } else if (isSeal(pair)) {
            applySeal(ctx);
        }
    }

    /** Seal: black base, tan creeps a random amount up the lower legs + a tan muzzle. */
    private void applySeal(CoatBuildContext ctx) {
        Rng epi = ctx.epigeneticsFor(KEY);
        PigmentField f = ctx.pigment();
        for (Part leg : List.of(Part.LEFT_FRONT_LEG, Part.RIGHT_FRONT_LEG, Part.LEFT_HIND_LEG, Part.RIGHT_HIND_LEG)) {
            double h = 0.10 + epi.nextFloat() * epi.nextFloat() * 0.55;
            Bounds b = HorseSkinGeometry.bounds(leg);
            double cutoff = b.yMin() + b.span(Axis.Y) * h;
            HorseSkinGeometry.forEachTexel(leg, (px, py, part, face, point) -> {
                if (point.y() <= cutoff) {
                    f.setBlack(px, py, 0.30f); // lift black -> tan shows through
                }
            });
        }
        HorseSkinGeometry.forEachTexel(Part.MUZZLE, (px, py, part, face, point) -> f.restrictBlack(px, py, 0.45f));
    }
}
