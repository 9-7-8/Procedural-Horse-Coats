package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.coat.pattern.PigmentView;
import com.example.horsegenetics.common.coat.pattern.GreyCoat;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.DominancePattern;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;

import java.util.List;

/**
 * <b>Grey</b> ({@code horsegenetics.grey}). {@code G} dominant, {@code g}
 * wild-type. Natural: {@code G_} <b>restricts black and red pigment equally</b>
 * (so it lightens without shifting hue) - but only on <b>adults</b>. A foal is
 * born whatever colour it would be without grey; once grown it renders as a
 * <b>dapple grey</b>.
 *
 * <p><b>Non-deterministic.</b> The restriction is not one flat number: the
 * {@code G} copy's epigenetics decide how far along this horse's greying is
 * (dark steel grey through mid dapple grey to nearly white), how big and how
 * pronounced its dapples are, and how much longer its mane / tail / legs hold
 * their colour. See {@link GreyCoat}.
 *
 * <p>(Real greying advances with age. The pipeline has no age input past
 * adult/foal, so a horse's stage is drawn once and fixed for life.)
 */
public final class GreyGene implements Gene {

    public static final String KEY = "horsegenetics.grey";
    public static final int WILD_GREY_ALLELE_ODDS = 16;

    public final Allele G = new Allele(KEY, "G", "Grey (G)", true, false);
    public final Allele g = new Allele(KEY, "g", "Wild-type (g)", false, true);
    private final List<Allele> alleles = List.of(G, g);

    @Override public String key() { return KEY; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele wildType() { return g; }

    /** Dominant: one {@code G} greys the adult out. */
    @Override public DominancePattern dominance() { return DominancePattern.DOMINANT; }

    @Override
    public AllelePair randomPair(Rng rng) {
        return new AllelePair(
                rng.nextInt(WILD_GREY_ALLELE_ODDS) == 0 ? G : g,
                rng.nextInt(WILD_GREY_ALLELE_ODDS) == 0 ? G : g);
    }

    public boolean isGrey(AllelePair pair) {
        return pair.has(G);
    }

    @Override
    public boolean isVisible(AllelePair pair, Genotype genotype) {
        return isGrey(pair); // gates the pass; restrict() no-ops for foals
    }

    /**
     * Every grey adult is its own horse. This can't see the foal flag (only
     * {@code restrict} gets the {@link CoatBuildContext}), so a grey <i>foal</i>
     * is also treated as per-horse - it bakes its own texture and that texture
     * is identical to a non-grey foal's. Harmless, just a few extra cache
     * entries.
     */
    @Override
    public boolean isDeterministic(AllelePair pair, Genotype genotype) {
        return !isGrey(pair);
    }

    @Override
    public PigmentField restrict(AllelePair pair, CoatBuildContext ctx, PigmentView coat) {
        if (!isGrey(pair) || !ctx.isAdult()) {
            return null;
        }
        PigmentField f = coat.mutableCopy();
        GreyCoat.apply(ctx, f, ctx.epigeneticsFor(KEY));
        return f;
    }
}
