package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.CoatRegions;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;

/**
 * <b>Splash white</b> ({@code horsegenetics.splash}) - random white markings, as
 * if the horse were dipped in white paint from below. {@code Spl} dominant,
 * {@code spl} recessive/wild-type. Natural (it removes both pigments -&gt;
 * transparent -&gt; the white template shows). Non-deterministic - the marking
 * extents are part of the epigenetic value.
 *
 * <p>Effect: white climbs a random amount up <b>each leg</b> independently, plus
 * a random <b>face blaze</b> (width + length) down the centre of the muzzle /
 * head.
 */
public final class SplashGene implements Gene {

    public static final String KEY = "horsegenetics.splash";
    public static final int WILD_SPLASH_ALLELE_ODDS = 20;

    public final Allele Spl = new Allele(KEY, "Spl", "Splash white (Spl)", true, false);
    public final Allele spl = new Allele(KEY, "spl", "Wild-type (spl)", false, true);
    private final java.util.List<Allele> alleles = java.util.List.of(Spl, spl);

    @Override public String key() { return KEY; }
    @Override public java.util.List<Allele> alleles() { return alleles; }
    @Override public Allele wildType() { return spl; }

    @Override
    public AllelePair randomPair(Rng rng) {
        return new AllelePair(
                rng.nextInt(WILD_SPLASH_ALLELE_ODDS) == 0 ? Spl : spl,
                rng.nextInt(WILD_SPLASH_ALLELE_ODDS) == 0 ? Spl : spl);
    }

    public boolean isSplash(AllelePair pair) {
        return pair.has(Spl);
    }

    @Override
    public boolean isVisible(AllelePair pair, Genotype genotype) {
        return isSplash(pair);
    }

    @Override
    public boolean isDeterministic(AllelePair pair, Genotype genotype) {
        return !isSplash(pair);
    }

    @Override
    public void restrict(AllelePair pair, CoatBuildContext ctx) {
        if (!isSplash(pair)) {
            return;
        }
        Rng epi = ctx.epigeneticsFor(KEY);
        PigmentField f = ctx.pigment();

        for (var leg : CoatRegions.LEGS) {
            double h = 0.15 + epi.nextFloat() * epi.nextFloat() * 0.75; // usually socks, sometimes stockings
            CoatRegions.whitenLowerLeg(f, leg, h);
        }

        double blazeHalfWidth = 0.4 + epi.nextFloat() * 1.4;  // body units either side of centre
        double blazeLength = 0.2 + epi.nextFloat() * 0.75;     // fraction of the head length
        CoatRegions.whitenBlaze(f, blazeHalfWidth, blazeLength);
    }
}
