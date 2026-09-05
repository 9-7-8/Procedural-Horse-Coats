package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.trait.TraitBuilder;

/**
 * <b>Magic speed</b> ({@code horsegenetics.magic_speed}) - the magical version
 * of the three natural speed loci ({@link MstnGene}, {@link Pdk4Gene},
 * {@link CkmGene}), and one of the four {@link AbstractMagicStatGene magical
 * body-stat genes} most wild horses carry a copy of.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th><th>speed</th></tr>
 *   <tr><td>{@code n/n}</td><td>wild type</td><td>unchanged</td></tr>
 *   <tr><td>{@code Swift/n}</td><td>{@code more}</td><td>&times; about 1.10</td></tr>
 *   <tr><td>{@code Swift/Swift}</td><td>{@code double-more}</td><td><b>both</b> copies' percentages, added</td></tr>
 *   <tr><td>{@code Sluggish/n}</td><td>{@code less}</td><td>&times; about 0.90</td></tr>
 *   <tr><td>{@code Sluggish/Sluggish}</td><td>{@code double-less}</td><td>both copies' percentages off</td></tr>
 *   <tr><td>{@code Swift/Sluggish}</td><td>{@code balanced}</td><td>the two nearly cancel</td></tr>
 * </table>
 *
 * <p>It <b>multiplies</b> whatever the natural speed loci settled on, through
 * {@link TraitBuilder#multiplySpeedUnclamped}, so a magically fast pony is still
 * slower than a magically fast racehorse. The bounded Gaussian keeps the real
 * reach near {@code 2x} either way; the {@code MAGICAL_*_FACTOR} clamp (ten
 * times) is a guard it does not touch.
 */
public final class MagicSpeedGene extends AbstractMagicStatGene {

    public static final String KEY = "horsegenetics.magic_speed";
    public static final int PRIORITY = 141;

    public MagicSpeedGene() {
        super(KEY, PRIORITY, "Magic speed",
                "Swift", "Swiftness (Swift)",
                "Sluggish", "Sluggishness (Sluggish)",
                new Vocabulary(
                        "Ordinary speed for whatever the horse's own speed genes say - which, "
                                + "since most horses carry a copy of this one, is itself slightly unusual.",
                        "Faster",
                        "One swift copy. The horse is faster than its own genes would make it, by the "
                                + "percentage written on that copy - usually around a tenth. It passes that "
                                + "exact percentage on with the allele.",
                        "Much faster",
                        "Two swift copies, and the percentages add. Around a fifth over on average, and "
                                + "far more when both copies rolled well - the fastest horses in the world "
                                + "are bred here, because no wild horse is born with two.",
                        "Slower",
                        "One sluggish copy. The same thing in reverse: slower by the percentage on that "
                                + "copy, usually around a tenth, and inherited with the allele.",
                        "Much slower",
                        "Two sluggish copies, and the percentages subtract together. Around a fifth "
                                + "under on average and a great deal less at the extreme.",
                        "Balanced",
                        "One swift copy and one sluggish copy. Their percentages very nearly cancel, so "
                                + "the horse runs close to its ordinary speed while carrying, and passing "
                                + "on, both extremes."));
    }

    @Override
    protected void applyMagic(TraitBuilder out, double factor) {
        out.multiplySpeedUnclamped(factor);
    }
}
