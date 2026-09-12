package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.coat.pattern.ColorField;
import com.example.horsegenetics.common.coat.pattern.ColorView;
import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.PigmentView;
import com.example.horsegenetics.common.genetics.Expression;

/**
 * <b>A two-allele magical gene</b> - one that adds signed RGB over the resolved
 * coat in phase 3.
 *
 * <p>The mirror of {@link AbstractNaturalGene}: the same declaration, the same
 * seven answered methods, and {@link #tint} instead of
 * {@code restrict}. A subclass is the paint function and nothing else.
 *
 * <h2>Phase 3 sees a finished horse</h2>
 * {@code coat} is the resolved natural pigment and {@code colour} is what the
 * magical genes before this one accumulated, so a magical gene can <i>find</i>
 * the black areas, or the white ones, before painting them - which is how a
 * gene stays recognisable across every base coat instead of looking right on a
 * bay and wrong on a grey.
 *
 * <p>The accumulator is an uncapped {@code int} per channel and is only clamped
 * at conversion, so a gene may commit hard enough that nothing after it can pull
 * the horse back. That is a real choice with real consequences for every gene
 * downstream of it - {@link Setup#masking()} is the honest way to say so.
 */
public abstract class AbstractMagicalGene extends TwoAlleleGene {

    private final Expression outcome;

    protected AbstractMagicalGene(Setup setup) {
        super(setup);
        Expression.Builder b = Expression.of(setup.outcomeId, setup.outcomeName)
                .describe(setup.outcomeText);
        if (setup.varies) {
            b.varies();
        }
        if (setup.masks) {
            b.masking();
        }
        // See AbstractNaturalGene: a method reference is made here and called
        // much later, when the subclass is fully constructed.
        this.outcome = b.tint(this::tint);
    }

    @Override
    protected final Expression outcome() {
        return outcome;
    }

    @Override
    public final boolean isNatural() {
        return false;
    }

    /**
     * Add signed red / green / blue, for a horse whose combination expresses.
     * Called only then, so an implementation never tests for the wild type.
     *
     * <p>The contract is {@link Expression.Colour}'s: return a delta built with
     * {@link ColorField#deltaLike(ColorView)} and filled with
     * {@link ColorField#add}, or {@code null} for no contribution.
     *
     * <p>Per-horse variation comes from {@code ctx.epigeneticsFor(key())};
     * declare the numbers in {@link #epiSchema()} and say
     * {@link Setup#varies()}.
     */
    protected abstract ColorField tint(CoatBuildContext ctx, PigmentView coat, ColorView colour);
}
