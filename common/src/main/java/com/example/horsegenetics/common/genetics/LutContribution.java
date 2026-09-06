package com.example.horsegenetics.common.genetics;

import java.util.Map;
import java.util.Optional;

/**
 * <b>A capability a {@link Gene} may additionally implement</b>: "this
 * combination of my alleles swaps the colour lookup the natural genes resolve
 * through". The {@code LUT} locus is the only implementor - a <b>magical</b>
 * gene that leaves phase 1 alone and instead replaces the red/black gradient in
 * phase 2 with an unnatural one, so the same melanin genotype lands on a
 * different palette.
 *
 * <p>It is a fourth thing a gene can be, alongside the coat
 * ({@link Expression}), the body
 * ({@link com.example.horsegenetics.common.trait.TraitContribution}) and game
 * behaviour ({@link AbilityContribution}), and like those it is a separate
 * interface because it is the rare case.
 *
 * <p><b>Exactly one gene implements this, and that is deliberate and
 * permanent.</b> There is no "this gene resolves against gradient X" knob on an
 * arbitrary gene. The LUT a coat resolves against is a property of a single
 * locus, so a horse can shift its palette at most one way at a time; anything
 * new that should change the LUT is added as another allele on that locus, not
 * as a second implementor of this interface. The composer still loops over all
 * implementors purely as defensiveness.
 *
 * <h2>Homozygous, and the same allele</h2>
 * A LUT allele only does anything when the horse carries <b>two identical
 * copies</b> of it. One copy, or two <i>different</i> LUT variant alleles, is a
 * wild type - the horse keeps the natural gradient. That is what
 * {@link #alternateLut} encodes: it returns a key only for a true homozygote of
 * a variant.
 *
 * <p>Purity: the pair and the genotype in, a key out. The returned key is inert
 * until {@link com.example.horsegenetics.common.coat.pattern.CoatTextureComposer}
 * looks it up in the {@link com.example.horsegenetics.common.coat.pattern.LutSet}
 * the game module built.
 */
public interface LutContribution {

    /**
     * The alternate-LUT key phase 2 should resolve this horse's pigment
     * through, or {@link Optional#empty()} for the natural red/black gradient.
     * Non-empty only for a horse homozygous for one of this gene's variant
     * alleles.
     */
    Optional<String> alternateLut(AllelePair pair, Genotype genotype);

    /**
     * Every alternate LUT this gene can select, as {@code key -> resource path}
     * (relative to {@code assets/<namespace>/}, e.g.
     * {@code "textures/coat/lutbluepink.png"}). The game module loads each into
     * a {@link com.example.horsegenetics.common.coat.pattern.GradientLut} and
     * assembles the {@link com.example.horsegenetics.common.coat.pattern.LutSet};
     * {@code common} never names an {@code Identifier}.
     */
    Map<String, String> lutResources();
}
