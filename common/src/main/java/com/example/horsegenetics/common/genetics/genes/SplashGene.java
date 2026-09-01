package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.CoatRegions;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.DominancePattern;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;

import java.util.List;

/**
 * <b>Splash white</b> ({@code horsegenetics.splash}) - random white markings, as
 * if the horse were dipped in white from below. {@code Spl} dominant, {@code spl}
 * wild-type. Natural (removes both pigments -&gt; transparent -&gt; the white
 * template shows). Non-deterministic. White climbs each leg an independent
 * random amount + a random centreline face blaze.
 */
public final class SplashGene implements Gene {

    public static final String KEY = "horsegenetics.splash";
    public static final int WILD_SPLASH_ALLELE_ODDS = 20;

    public final Allele Spl = new Allele(KEY, "Spl", "Splash white (Spl)", true, false);
    public final Allele spl = new Allele(KEY, "spl", "Wild-type (spl)", false, true);
    private final List<Allele> alleles = List.of(Spl, spl);

    @Override public String key() { return KEY; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele wildType() { return spl; }

    /**
     * IncompleteDominant - <b>aspirational</b>: {@code Spl/spl} and
     * {@code Spl/Spl} currently render identically, because this gene doesn't
     * read its own dose yet. Homozygous splash should give much larger white
     * markings; until it does, the catalogue gives the heterozygote its own pen
     * and the two look the same. See {@code Docs/to be verified.md}.
     */
    @Override public DominancePattern dominance() { return DominancePattern.INCOMPLETE_DOMINANT; }

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
        Skin skin = ctx.skin();
        PigmentField f = ctx.pigment();

        for (var leg : CoatRegions.LEGS) {
            double h = 0.15 + epi.nextFloat() * epi.nextFloat() * 0.75; // socks .. stockings
            CoatRegions.whitenLowerLeg(skin, f, leg, h);
        }

        double blazeHalfWidth = 0.4 + epi.nextFloat() * 1.4;  // body units either side of centre
        double blazeLength = 0.2 + epi.nextFloat() * 0.75;     // fraction of the head length
        CoatRegions.whitenBlaze(skin, f, blazeHalfWidth, blazeLength);
    }
}
