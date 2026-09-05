package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.trait.StatAxis;
import com.example.horsegenetics.common.trait.TraitBuilder;

/**
 * <b>Magic jump</b> ({@code horsegenetics.magic_jump}) - the magical version of
 * the natural jump locus ({@link Ryr2Gene}), and one of the four
 * {@link AbstractMagicStatGene magical body-stat genes} most wild horses carry
 * a copy of.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th><th>jump strength</th></tr>
 *   <tr><td>{@code n/n}</td><td>wild type</td><td>unchanged</td></tr>
 *   <tr><td>{@code Springy/n}</td><td>{@code more}</td><td>&times; about 1.10</td></tr>
 *   <tr><td>{@code Springy/Springy}</td><td>{@code double-more}</td><td><b>both</b> copies' percentages, added</td></tr>
 *   <tr><td>{@code Leaden/n}</td><td>{@code less}</td><td>&times; about 0.90</td></tr>
 *   <tr><td>{@code Leaden/Leaden}</td><td>{@code double-less}</td><td>both copies' percentages off</td></tr>
 *   <tr><td>{@code Springy/Leaden}</td><td>{@code balanced}</td><td>the two nearly cancel</td></tr>
 * </table>
 *
 * <p>It <b>multiplies</b> whatever the natural jump contributions settled on,
 * through {@link TraitBuilder#multiplyJumpUnclamped} - so, like its three
 * siblings, it scales the horse the natural loci built rather than replacing
 * their arithmetic.
 */
public final class MagicJumpGene extends AbstractMagicStatGene {

    public static final String KEY = "horsegenetics.magic_jump";
    public static final int PRIORITY = 143;

    public MagicJumpGene() {
        super(KEY, PRIORITY, "Magic jump",
                "Springy", "Springiness (Springy)",
                "Leaden", "Leadenness (Leaden)",
                new Vocabulary(
                        "Ordinary jump for whatever the horse's own jump genes say - which, since most "
                                + "horses carry a copy of this one, is itself slightly unusual.",
                        "Higher jumper",
                        "One springy copy. The horse clears more than its own genes would let it, by "
                                + "the percentage written on that copy - usually around a tenth. It "
                                + "passes that exact percentage on with the allele.",
                        "Much higher jumper",
                        "Two springy copies, and the percentages add. Around a fifth over on average, "
                                + "and far more when both copies rolled well - the highest jumpers in "
                                + "the world are bred here, because no wild horse is born with two.",
                        "Lower jumper",
                        "One leaden copy. The same thing in reverse: less height by the percentage on "
                                + "that copy, usually around a tenth, and inherited with the allele.",
                        "Much lower jumper",
                        "Two leaden copies, and the percentages subtract together. Around a fifth under "
                                + "on average and a great deal less at the extreme.",
                        "Balanced",
                        "One springy copy and one leaden copy. Their percentages very nearly cancel, so "
                                + "the horse jumps close to its ordinary height while carrying, and "
                                + "passing on, both extremes."));
    }

    @Override
    protected StatAxis axis() {
        return StatAxis.JUMP;
    }

    @Override
    protected void applyMagic(TraitBuilder out, double factor) {
        out.multiplyJumpUnclamped(factor);
    }
}
