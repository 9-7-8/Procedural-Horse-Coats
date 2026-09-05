package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Gene;

/**
 * <b>A capability a {@link Gene} may additionally implement</b>: "after the coat
 * is finished, I want these exact pixels, and/or these texels glowing".
 *
 * <p>It is the coat's counterpart to
 * {@link com.example.horsegenetics.common.trait.TraitContribution} - a separate
 * interface rather than another method on {@code Gene}, because almost no gene
 * has anything to say in this phase and the ones that do are magical genes
 * doing something the pigment model cannot describe. See {@link CoatOverlay}
 * for what the phase is for and why it exists at all.
 *
 * <p>The gene is only asked when its combination is not a
 * {@link com.example.horsegenetics.common.genetics.Expression#wildType() wild
 * type}, exactly as in the other two phases, so a carrier draws nothing.
 *
 * <p>Purity: the same contract as the phase-1 and phase-3 hooks. The overlay
 * hands out the finished coat and takes writes; it never exposes another gene's.
 */
@FunctionalInterface
public interface CoatOverlayContribution {

    /** Write this gene's final pixels and emissive texels for {@code pair} into {@code out}. */
    void overlay(AllelePair pair, CoatBuildContext ctx, CoatOverlay out);
}
