package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneRarity;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.WhiteLockContribution;

import java.util.List;

/**
 * <b>Extreme White Dominant</b> ({@code horsegenetics.extreme_white_dominant})
 * - a <b>magical</b> gene that paints nothing whatsoever.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code n/n}</td><td>wild type</td></tr>
 *   <tr><td>{@code EWD/n}, {@code EWD/EWD}</td><td>{@code white-dominant} - white wins every argument</td></tr>
 * </table>
 *
 * <h2>What it does instead of painting</h2>
 * It flips one rule in the coat pipeline: <b>a white texel is final</b>. Every
 * magical gene still runs, in the ordinary order, and every one of them is
 * simply discarded wherever the coat is already white. The lock starts with the
 * white the <i>melanin</i> genes made - a tobiano patch, a splash, a blaze, a
 * stocking - and grows as the magical phase runs, so white a magical gene puts
 * down is locked from the moment it lands too.
 *
 * <h2>Why it is a capability and not a very high priority</h2>
 * The obvious way to write this gene is to give it priority 999 and repaint the
 * white last. That is not the same thing, and the difference is the whole gene:
 * a priority is a <i>slot</i>, and a marking sitting above the slot still paints
 * over the white. This one has no slot to get over -
 * {@link WhiteLockContribution} applies at every step of the order at once,
 * including the steps above its own.
 *
 * <p>Its own {@link #PRIORITY} is therefore only a code-order position: where
 * its segment sits in a genotype code, and nothing else.
 *
 * <h2>What it is for</h2>
 * The mod's magical markings are painted after the natural ones and mostly
 * cover them, so a horse carrying both a tobiano and something loud reads as
 * the loud thing alone. This gene inverts that for one horse: the natural white
 * markings come out on top of the magic, and the magic fills in around them.
 *
 * <p><b>The eyes are exempt</b>, and only the eyes - see
 * {@code CoatTextureComposer.whiteLock}. A blue eye on a white face is pigment
 * biology rather than a marking, and locking it would silently delete every
 * eye-colour gene on exactly the horses that have the most interesting eyes.
 *
 * <p>Founder frequency {@code 1/}{@value #WILD_EWD_ONE_IN} per allele. Dominant
 * in the flat sense - one copy is the whole effect, and two look identical,
 * because "white is final" has no second half to reach.
 */
public final class ExtremeWhiteDominantGene implements Gene, WhiteLockContribution {

    public static final String KEY = "horsegenetics.extreme_white_dominant";

    /**
     * A code-order slot, not a paint order - the lock is not painted at all.
     * High in the magical band so that a reader running down a genotype code
     * meets it late, which is where its effect reads from.
     */
    public static final int PRIORITY = 690;

    public static final int WILD_EWD_ONE_IN = 320;

    public final Allele EWD = new Allele(KEY, 0, "EWD", "Extreme white dominant (EWD)");
    public final Allele n = new Allele(KEY, 1, "n", "Wild-type (n)");
    private final List<Allele> alleles = List.of(EWD, n);

    private final Expression WILD = Expression.wildType(
            "Markings paint in the ordinary order, and the magical ones cover the white.");

    /**
     * A {@linkplain Expression.Builder#marker() marker}: it changes the horse -
     * emphatically - but it has no painter in either phase, so the composer
     * skips it in both painting loops and reads the capability instead.
     */
    private final Expression LOCKED = Expression.of("white-dominant", "Extreme white dominant")
            .describe("White wins. Every white marking the horse carries - a tobiano patch, a "
                    + "splash, a blaze, a stocking, or white a magical gene painted - is final "
                    + "from the moment it lands: nothing painted afterwards may touch it. On its "
                    + "own it changes nothing at all, and on a horse with no white it still "
                    + "changes nothing. On a horse carrying both white and something magical it "
                    + "reverses which one is on top, so the natural markings read over the magic "
                    + "instead of under it. One copy is the whole of it; two look the same.")
            .marker();

    private final List<Expression> expressions = List.of(WILD, LOCKED);

    private final FounderTable founders =
            FounderTable.hardyWeinberg(EWD, n, 1.0 / WILD_EWD_ONE_IN);

    @Override public String key() { return KEY; }
    @Override public String name() { return "Extreme white dominant"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return false; }
    @Override public GeneRarity rarity() { return GeneRarity.RARE; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        return pair.has(EWD) ? LOCKED : WILD;
    }

    @Override
    public boolean locksWhite(AllelePair pair, Genotype genotype) {
        return pair.has(EWD);
    }
}
