package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;

import java.util.List;

/**
 * A single heritable gene: its alleles, how they segregate, and how it changes
 * the coat.
 *
 * <p>The coat is built by starting every pixel at "maximal red + maximal black
 * pigment" (a black horse), then:
 * <ol>
 *   <li>every visible <b>natural</b> gene ({@link #isNatural()}, the default)
 *       gets a {@link #restrict} turn to push the shared
 *       {@link CoatBuildContext#pigment() pigment field} down - natural genes
 *       do <i>nothing else</i>;</li>
 *   <li>the pigment field is resolved to colour through the red/black gradient
 *       LUT (so champagne-on-bay looks different from champagne-on-black, and
 *       anything on white is invisible);</li>
 *   <li>every visible <b>non-natural</b> gene gets an {@link #overlayLayer}
 *       turn: it fills an ARGB layer that is then painted <i>flat on top of</i>
 *       the resolved coat (opaque layer texels win outright - the effect shows
 *       the same on a black, a chestnut or a white horse).</li>
 * </ol>
 *
 * <p>(This interface lives in {@code genetics} but references
 * {@code coat.pattern.CoatBuildContext} - the two packages form an intentional
 * cycle so "the coat function lives on the gene" stays literally true.)
 */
public interface Gene {

    /** {@code <modauthor>.<gene>}, e.g. {@code "horsegenetics.agouti"}. */
    String key();

    /** All alleles this gene defines, most-dominant first. */
    List<Allele> alleles();

    /** The "no visible effect" allele - the one a wild horse most often carries. */
    Allele wildType();

    /**
     * How this gene's variant expresses against {@link #wildType()} - metadata,
     * declared per gene rather than inferred. See {@link DominancePattern};
     * {@link GenotypeCatalog} reads it to decide which pairs are visually
     * distinct.
     */
    DominancePattern dominance();

    /**
     * A <b>natural</b> gene only restricts red / black pigment ({@link #restrict});
     * it never paints colour directly. Non-natural genes (only Test so far) are
     * painted flat on top after the pigment field is resolved.
     */
    default boolean isNatural() {
        return true;
    }

    /** Lower = more dominant. Used to canonicalize an {@link AllelePair}. */
    default int precedence(Allele allele) {
        int i = alleles().indexOf(allele);
        return i < 0 ? Integer.MAX_VALUE : i;
    }

    /** Resolve one code token to an allele of this gene. */
    default Allele fromToken(String token) {
        for (Allele a : alleles()) {
            if (a.token().equals(token)) {
                return a;
            }
        }
        throw new IllegalArgumentException("gene " + key() + " has no allele '" + token + "'");
    }

    /**
     * Draw one wild-population pair for this gene. Each gene documents how many
     * {@link Rng} values it consumes and in what order.
     */
    AllelePair randomPair(Rng rng);

    // --- coat contribution -----------------------------------------------

    /** Natural genes: mutate {@code ctx.pigment()} for a horse carrying {@code pair}. */
    default void restrict(AllelePair pair, CoatBuildContext ctx) {
    }

    /**
     * Non-natural genes: fill {@code layer} (row-major ARGB, pre-filled
     * <b>transparent</b>) with the colour to paint flat on top of the resolved
     * coat. Every texel the layer leaves transparent is left untouched; every
     * opaque texel replaces whatever the natural pass resolved there.
     */
    default void overlayLayer(AllelePair pair, CoatBuildContext ctx, int[] layer) {
    }

    /** Does {@code pair} change the coat at all, in the context of the whole {@code genotype}? */
    default boolean isVisible(AllelePair pair, Genotype genotype) {
        return pair.anyVisible();
    }

    /**
     * Is this gene's contribution byte-for-byte identical on every horse with
     * this {@code pair} / {@code genotype}? A {@code false} anywhere forces
     * per-horse texture generation.
     */
    default boolean isDeterministic(AllelePair pair, Genotype genotype) {
        return pair.allDeterministic();
    }
}
