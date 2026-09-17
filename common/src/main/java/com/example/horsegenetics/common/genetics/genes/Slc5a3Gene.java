package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.trait.Condition;
import com.example.horsegenetics.common.trait.TraitBuilder;

/**
 * <b>SLC5A3</b> ({@code horsegenetics.slc5a3}) - <b>foal immunodeficiency
 * syndrome</b>, and the one recessive lethal in this mod whose real carrier rate
 * is not a rarity at all.
 *
 * <p>The shape is the ordinary one - {@link RecessiveDisorderGene}, two alleles,
 * and only {@code fis/fis} does anything - but the numbers are not. A
 * foundational screen of the Fell Pony found <b>82 carriers in 214 animals</b>,
 * about 38%, and UC Davis has reported near 40%; the same mutation is in the
 * Dales Pony. Testing has existed since 2010 and the breed societies run it, so
 * a Fell that carries nothing is the one thing the real breed is <i>not</i>.
 *
 * <h2>Why the global rate is low and the breed rate is not</h2>
 * {@link #WILD_CARRIER_PERCENT} is what an <i>unaffiliated</i> wild-caught horse
 * carries, and FIS is essentially confined to two related native pony breeds. The
 * real frequency lives in those two breed files as a per-breed pool, which is the
 * mechanism built for exactly this: a disorder that is vanishing globally and
 * commonplace inside one population.
 *
 * <h2>What an affected foal looks like</h2>
 * The cruelty of it is that there is nothing to see at birth. The foal is born
 * apparently healthy, runs with its dam, and then fails over the following weeks
 * as the antibodies it borrowed from her colostrum run out and its own B cells
 * never arrive. Progressive anaemia and opportunistic infection; it does not
 * survive. The mod has no age model, so the death is the ordinary
 * born-then-dies path - see {@link com.example.horsegenetics.common.trait.Viability}.
 */
public final class Slc5a3Gene extends RecessiveDisorderGene {

    public static final String KEY = "horsegenetics.slc5a3";
    public static final int PRIORITY = 102;

    /**
     * Founders unaffiliated with a breed. The Fell and the Dales carry it at
     * dozens of times this in their own files - see the class note.
     */
    public static final double WILD_CARRIER_PERCENT = 0.8;

    public static final Condition FIS = Condition.lethalAtBirth(
            "foal-immunodeficiency-syndrome", "Foal immunodeficiency syndrome",
            "No working B cells and a failing marrow. The foal is born looking perfectly "
                    + "healthy and fades as its dam's borrowed antibodies run out - anaemia, "
                    + "then an infection it cannot answer. It does not survive.");

    public Slc5a3Gene() {
        super(KEY, "SLC5A3", PRIORITY,
                "fis", "Foal immunodeficiency (fis)", "N", "Wild-type (N)",
                WILD_CARRIER_PERCENT, FIS);
    }

    @Override
    protected void affect(TraitBuilder out) {
        out.addHealth(-12.0).addSpeed(-0.04).addJump(-0.15);
    }
}
