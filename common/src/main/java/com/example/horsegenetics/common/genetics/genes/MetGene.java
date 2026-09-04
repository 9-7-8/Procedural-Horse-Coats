package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.trait.Condition;
import com.example.horsegenetics.common.trait.TraitBuilder;

/**
 * <b>MET</b> ({@code horsegenetics.met}) - the mod's <b>embryonic lethal</b>.
 * Two non-functional copies and the embryo never develops, so the pairing simply
 * produces no foal at all.
 *
 * <p>This is a different code path from every other lethal, and the difference
 * is worth being precise about. The Mendelian draw happens exactly as it always
 * does - {@code Genotype.breedWith} knows nothing about viability and is not
 * touched - and then the breeding handler <i>reads</i> the genotype it just drew
 * and cancels the birth. So the odds are the ordinary one-in-four for two
 * carriers, the draw stays a pure function of the two parents and the RNG, and
 * the check is one branch in one place.
 *
 * <p>Two consequences follow from "no horse is ever born with it":
 * <ul>
 *   <li>{@link #canOccur} is <b>false</b> for {@code met/met}, so the genotype
 *       gallery gives it no pen and does not count it. That is the same rule
 *       that rules out a {@code Y/Y} horse, and the opposite of overo lethal
 *       white, which <i>is</i> born and therefore does get one.</li>
 *   <li>{@link #expressionOf} still answers for it, because parsing is tolerant
 *       and a hand-written code can name any combination.</li>
 * </ul>
 *
 * <p>From the player side it reads as a pairing that keeps refusing: two horses
 * that will breed with anything else and never with each other. The chat line
 * naming the cause is what turns that from a bug report into a clue.
 */
public final class MetGene extends RecessiveDisorderGene {

    public static final String KEY = "horsegenetics.met";
    public static final int PRIORITY = 92;

    public static final double WILD_CARRIER_PERCENT = 3.0;

    public static final Condition EMBRYONIC_LETHAL = Condition.lethalAtConception(
            "met-embryonic-lethal", "Embryonic lethal (MET)",
            "Two non-functional copies of MET. The embryo never develops, so the pairing "
                    + "produces no foal at all.");

    public MetGene() {
        super(KEY, "MET", PRIORITY,
                "met", "Embryonic lethal (met)", "N", "Wild-type (N)",
                WILD_CARRIER_PERCENT, EMBRYONIC_LETHAL);
    }

    /**
     * No horse ever carries two copies - the embryo does not implant. Nothing
     * can produce one, so the catalogue leaves it out entirely.
     */
    @Override
    public boolean canOccur(AllelePair pair) {
        return !isAffected(pair);
    }

    /**
     * Never reached in play: a horse with this combination is never born, and
     * the numbers exist only so a hand-written code string resolves to
     * something rather than throwing.
     */
    @Override
    protected void affect(TraitBuilder out) {
        out.addHealth(-19.0);
    }
}
