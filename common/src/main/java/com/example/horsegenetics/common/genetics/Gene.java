package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;

import java.util.List;

/**
 * A single heritable gene: its alleles, how they segregate, and - the point of
 * the whole rework - <b>how it restricts / paints coat pigment</b>.
 *
 * <p>The coat is built by starting every pixel at "maximal red + maximal black
 * pigment" (a black horse) and letting each visible gene push pigment down.
 * {@link #restrict} is a gene's turn to mutate the shared
 * {@link CoatBuildContext#pigment() pigment field}; genes run in
 * {@link Genes#restrictionOrder()}. After that the field is resolved to colour
 * through the red/black gradient LUT, and {@link #paint} genes (e.g. the Test
 * gradient) get a pass to draw ARGB directly. Both default to no-op.
 *
 * <p>(This interface lives in {@code genetics} but references
 * {@code coat.pattern.CoatBuildContext} - the two packages form an intentional
 * cycle so "the overlay function lives on the gene" stays literally true.)
 */
public interface Gene {

    /** {@code <modauthor>.<gene>}, e.g. {@code "horsegenetics.agouti"}. */
    String key();

    /** All alleles this gene defines, most-dominant first. */
    List<Allele> alleles();

    /** The "no visible effect" allele - what a missing locus in a legacy code becomes. */
    Allele wildType();

    /** Lower = more dominant. Used to canonicalize an {@link AllelePair}. */
    default int precedence(Allele allele) {
        int i = alleles().indexOf(allele);
        return i < 0 ? Integer.MAX_VALUE : i;
    }

    /** Resolve one character of a genotype code to an allele of this gene. */
    default Allele fromSymbol(char c) {
        for (Allele a : alleles()) {
            if (a.symbol() == c) {
                return a;
            }
        }
        throw new IllegalArgumentException(
                "gene " + key() + " has no allele with symbol '" + c + "'");
    }

    /**
     * Draw one wild-population pair for this gene. Each gene documents how many
     * {@link Rng} values it consumes and in what order (so {@code FakeRng}
     * scripting stays predictable).
     */
    AllelePair randomPair(Rng rng);

    // --- coat contribution -------------------------------------------------

    /** Mutate {@code ctx.pigment()} for a horse carrying {@code pair} here. */
    default void restrict(AllelePair pair, CoatBuildContext ctx) {
    }

    /** Draw ARGB onto {@code ctx.overlay()} after the pigment field is resolved. */
    default void paint(AllelePair pair, CoatBuildContext ctx) {
    }

    /**
     * Does {@code pair} change the coat at all, in the context of the whole
     * {@code genotype}? (A gene can be masked - e.g. agouti does nothing on a
     * chestnut that makes no black.) Default: any visible allele.
     */
    default boolean isVisible(AllelePair pair, Genotype genotype) {
        return pair.anyVisible();
    }

    /**
     * Is this gene's contribution byte-for-byte identical on every horse with
     * this {@code pair} / {@code genotype}? Default: every allele in the pair is
     * deterministic-tagged. A {@code false} anywhere in the genotype forces
     * per-horse texture generation.
     */
    default boolean isDeterministic(AllelePair pair, Genotype genotype) {
        return pair.allDeterministic();
    }
}
