package com.example.horsegenetics.common.coat.pattern;

/**
 * A <b>read-only</b> look at a {@link PigmentField} - what phase 1 has made of
 * the coat so far.
 *
 * <p>This is what a gene is handed. A gene never writes through its input: it
 * takes a {@link #mutableCopy()}, paints into that, and returns it, so two
 * genes handed the same view always produce the same output and no gene can
 * reach sideways into another's work. See {@code Gene#restrict}.
 */
public interface PigmentView {

    /** Sheet edge length in texels; the field is {@code size * size}. */
    int size();

    /** Surviving red (pheomelanin) at a texel, {@code [0, 1]}. */
    float red(int px, int py);

    /** Surviving black (eumelanin) at a texel, {@code [0, 1]}. */
    float black(int px, int py);

    /** A private, writable copy of this state - the gene's working field. */
    PigmentField mutableCopy();
}
