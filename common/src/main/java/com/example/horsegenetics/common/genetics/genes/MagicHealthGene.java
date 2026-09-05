package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.trait.TraitBuilder;

/**
 * <b>Magic health</b> ({@code horsegenetics.magic_health}) - the magical
 * counterpart of the hearts the natural loci trade around ({@link MstnGene}
 * spends them for speed, {@link Hmga2Gene} gives them for being small), and one
 * of the four {@link AbstractMagicStatGene magical body-stat genes} most wild
 * horses carry a copy of.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th><th>max health</th></tr>
 *   <tr><td>{@code n/n}</td><td>wild type</td><td>unchanged</td></tr>
 *   <tr><td>{@code Hardy/n}</td><td>{@code more}</td><td>&times; about 1.10</td></tr>
 *   <tr><td>{@code Hardy/Hardy}</td><td>{@code double-more}</td><td><b>both</b> copies' percentages, added</td></tr>
 *   <tr><td>{@code Frail/n}</td><td>{@code less}</td><td>&times; about 0.90</td></tr>
 *   <tr><td>{@code Frail/Frail}</td><td>{@code double-less}</td><td>both copies' percentages off</td></tr>
 *   <tr><td>{@code Hardy/Frail}</td><td>{@code balanced}</td><td>the two nearly cancel</td></tr>
 * </table>
 *
 * <p>It <b>multiplies</b> the health the additive loci resolved - the natural
 * baseline, plus MSTN's cost, plus HMGA2's gift, plus every disorder penalty -
 * through {@link TraitBuilder#multiplyHealthUnclamped}, and the
 * {@link com.example.horsegenetics.common.trait.HorseTraits#MIN_HEALTH} floor
 * still applies last, so it cannot resolve a horse to zero hearts.
 *
 * <p>It is <b>not a {@link com.example.horsegenetics.common.trait.HealthContribution}</b>:
 * that marker is for disorders, and the server config's "off" position must not
 * quietly delete a horse's magical vigour along with its lethal foals. A frail
 * horse here is just a horse with fewer hearts, not a sick one.
 */
public final class MagicHealthGene extends AbstractMagicStatGene {

    public static final String KEY = "horsegenetics.magic_health";
    public static final int PRIORITY = 142;

    public MagicHealthGene() {
        super(KEY, PRIORITY, "Magic health",
                "Hardy", "Hardiness (Hardy)",
                "Frail", "Frailty (Frail)",
                new Vocabulary(
                        "Ordinary constitution for whatever the horse's own genes say - which, since "
                                + "most horses carry a copy of this one, is itself slightly unusual.",
                        "Hardier",
                        "One hardy copy. More hearts than the horse's own genes would give it, by the "
                                + "percentage written on that copy - usually around a tenth. It passes "
                                + "that exact percentage on with the allele.",
                        "Much hardier",
                        "Two hardy copies, and the percentages add. Around a fifth over on average, and "
                                + "far more when both copies rolled well - the toughest horses in the "
                                + "world are bred here, because no wild horse is born with two.",
                        "Frailer",
                        "One frail copy. The same thing in reverse: fewer hearts by the percentage on "
                                + "that copy, usually around a tenth, and inherited with the allele.",
                        "Much frailer",
                        "Two frail copies, and the percentages subtract together. Around a fifth under "
                                + "on average and a great deal less at the extreme - though never all "
                                + "the way to nothing.",
                        "Balanced",
                        "One hardy copy and one frail copy. Their percentages very nearly cancel, so "
                                + "the horse keeps close to its ordinary number of hearts while carrying, "
                                + "and passing on, both extremes."));
    }

    @Override
    protected void applyMagic(TraitBuilder out, double factor) {
        out.multiplyHealthUnclamped(factor);
    }
}
