package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.ColorField;
import com.example.horsegenetics.common.coat.pattern.ColorView;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.coat.pattern.PigmentView;

import java.util.List;

/**
 * A single heritable gene: its alleles, how they segregate, and how it changes
 * the coat.
 *
 * <p>The coat is built in <b>three phases</b>. Every pixel starts at "maximal
 * red + maximal black pigment, no magical colour" - a plain black horse - and
 * then:
 * <ol>
 *   <li><b>natural (melanin) phase</b> - every visible <b>natural</b> gene
 *       ({@link #isNatural()}, the default) gets a {@link #restrict} turn to
 *       push the {@link PigmentView pigment field} <i>down</i>. Downward only:
 *       a natural gene can take red or black away, never add colour.</li>
 *   <li><b>resolve</b> - the surviving {@code (red, black)} pair is looked up
 *       in the red/black gradient and becomes an RGB colour (so
 *       champagne-on-bay differs from champagne-on-black, and anything under a
 *       fully-restricted white is invisible).</li>
 *   <li><b>magical (RGB) phase</b> - every visible <b>magical</b> gene
 *       ({@code isNatural() == false}) gets a {@link #tint} turn to add or
 *       remove signed red / green / blue on top of that resolved colour. The
 *       accumulator is an uncapped {@code int} per channel and is only capped
 *       to 0-255 at conversion, so a gene can commit hard enough that nothing
 *       else can pull the horse back.</li>
 * </ol>
 *
 * <p><b>A gene is natural or magical, never both</b> - declared, not inferred.
 * A gene that wants to do both registers as two genes. Natural is reserved for
 * genes that exist in real life (see {@code Docs/Philosophy.md}).
 *
 * <p><b>Both coat hooks are pure.</b> A gene is handed read-only views of the
 * state so far and <i>returns</i> its contribution; it never mutates shared
 * scratch space. Two genes handed the same inputs always return the same
 * outputs, and each is testable on its own against a synthetic coat.
 *
 * <p>(This interface lives in {@code genetics} but references
 * {@code coat.pattern} - the two packages form an intentional cycle so "the
 * coat function lives on the gene" stays literally true.)
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
     * A <b>natural</b> gene only restricts red / black pigment
     * ({@link #restrict}); it never paints colour. A <b>magical</b> gene
     * ({@code false}, only Test so far) only adds signed RGB ({@link #tint})
     * after the pigment field has been resolved. Never both.
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

    /**
     * <b>Phase 1.</b> A natural gene's turn to push pigment down, given the
     * coat as the genes before it have left it.
     *
     * <p>Take {@code coat}{@link PigmentView#mutableCopy() .mutableCopy()},
     * paint into that, and return it. Return {@code null} (the default) to
     * change nothing - which is also the cheap answer for a gene whose pair
     * happens not to express. <b>Never write through {@code coat}</b>: it is
     * the previous gene's output and the next gene's input.
     *
     * <p>The magical field does not exist yet at this point in the bake, which
     * is why - unlike {@link #tint} - there is nothing to read here but the
     * pigment.
     */
    default PigmentField restrict(AllelePair pair, CoatBuildContext ctx, PigmentView coat) {
        return null;
    }

    /**
     * <b>Phase 3.</b> A magical gene's turn to add colour, given the resolved
     * natural {@code coat} (so a gene can <i>find</i> the black areas, or the
     * white ones, before painting them) and the {@code colour} accumulated by
     * the magical genes before it.
     *
     * <p>Return a delta - {@link ColorField#deltaLike(ColorView)} filled with
     * {@link ColorField#add} - or {@code null} for no contribution. Deltas are
     * folded in by signed integer addition, so ordinary magical genes are
     * order-independent. A gene that must show identically on any base uses
     * {@link ColorField#set} instead, which replaces rather than adds.
     *
     * <p>A texel the natural phase left fully transparent (dominant white, a
     * splash marking) has zero opacity: colour alone will not show there, so a
     * gene that means to paint a white horse must raise
     * {@link ColorField#addOpacity} or {@code set} the texel.
     */
    default ColorField tint(AllelePair pair, CoatBuildContext ctx, PigmentView coat, ColorView colour) {
        return null;
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
