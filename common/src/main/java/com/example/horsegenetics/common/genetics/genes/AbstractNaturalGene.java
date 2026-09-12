package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.coat.pattern.PigmentView;
import com.example.horsegenetics.common.genetics.Expression;

/**
 * <b>A two-allele natural gene</b> - one that takes red or black pigment away
 * in phase 1, and nothing else.
 *
 * <p>A subclass declares the locus with {@link TwoAlleleGene#gene} and writes
 * {@link #restrict}: the whole gene, minus the alleles, the founder table, the
 * expression list and the combination table, which the base has from the
 * declaration.
 *
 * <pre>{@code
 * public final class MushroomGene extends AbstractNaturalGene {
 *     public MushroomGene() {
 *         super(gene("horsegenetics.mushroom", 32, "Mushroom")
 *                 .variant("Mu", "Mushroom (Mu)")
 *                 .recessive()
 *                 .hardyWeinberg(1.0 / 34)
 *                 .wild("Red pigment is left alone.")
 *                 .carrier("mushroom-carrier", "Mushroom carrier", "One copy shows nothing...")
 *                 .outcome("mushroom", "Mushroom", "Red pigment cut hard..."));
 *     }
 *
 *     protected PigmentField restrict(CoatBuildContext ctx, PigmentView coat) { ... }
 * }
 * }</pre>
 *
 * <p><b>Natural means real.</b> The phase is what decides which of the two coat
 * passes a gene runs in, and natural is reserved for genes that exist in real
 * life - see {@code wiki/philosophy.html}. A gene that invents a colour is
 * magical however plausible it looks; use {@link AbstractMagicalGene}.
 */
public abstract class AbstractNaturalGene extends TwoAlleleGene {

    private final Expression outcome;

    protected AbstractNaturalGene(Setup setup) {
        super(setup);
        Expression.Builder b = Expression.of(setup.outcomeId, setup.outcomeName)
                .describe(setup.outcomeText);
        if (setup.varies) {
            b.varies();
        }
        if (setup.masks) {
            b.masking();
        }
        // A method reference, not a call: the painter runs long after this
        // constructor, by which time the subclass is fully built. Constructing
        // it here is what lets the outcome be a final field on the base.
        this.outcome = b.restrict(this::restrict);
    }

    @Override
    protected final Expression outcome() {
        return outcome;
    }

    @Override
    public final boolean isNatural() {
        return true;
    }

    /**
     * Push the pigment field down, for a horse whose combination expresses.
     * Called only then, so an implementation never tests for the wild type.
     *
     * <p>The contract is {@link Expression.Pigment}'s: take
     * {@code coat.mutableCopy()}, paint into that, and return it. Never write
     * through {@code coat} - it is the previous gene's output and the next
     * gene's input. Returning {@code null} means no contribution after all.
     *
     * <p>Per-horse variation comes from {@code ctx.epigeneticsFor(key())},
     * which reads the expressing copy's stored numbers; declare them in
     * {@link #epiSchema()} and say {@link Setup#varies()} so the coat cache
     * knows the outcome is not one texture.
     */
    protected abstract PigmentField restrict(CoatBuildContext ctx, PigmentView coat);
}
