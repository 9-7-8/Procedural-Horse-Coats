package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.spec.GeneAbility;

import java.util.List;

/**
 * <b>Magic swim speed</b> ({@code horsegenetics.magic_swim_speed}) - how fast
 * the horse moves <b>in water</b>, and nothing else.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th><th>in water</th></tr>
 *   <tr><td>{@code n/n}</td><td>wild type</td><td>unchanged</td></tr>
 *   <tr><td>{@code Otr/n}</td><td>{@code more}</td><td>&times; about 1.25</td></tr>
 *   <tr><td>{@code Otr/Otr}</td><td>{@code double-more}</td><td><b>both</b> copies' percentages, added</td></tr>
 *   <tr><td>{@code Stne/n}</td><td>{@code less}</td><td>&times; about 0.75</td></tr>
 *   <tr><td>{@code Stne/Stne}</td><td>{@code double-less}</td><td>both copies' percentages off</td></tr>
 *   <tr><td>{@code Otr/Stne}</td><td>{@code balanced}</td><td>the two nearly cancel</td></tr>
 * </table>
 *
 * <h2>It has nothing to do with {@link MagicSpeedGene}</h2>
 * Not "related but separate" - <b>unrelated</b>, and that is the point of
 * having it. A horse can be the slowest thing in the paddock and cross a river
 * faster than a boat, or win every race on land and sink like a brick. The two
 * loci are independent draws, inherited independently, and a breeder who wants
 * both has to select for both.
 *
 * <p>That independence is why this is not a mode of the speed gene. Land speed
 * is a <i>body</i> fact - it multiplies whatever the three natural speed loci
 * settled on, so a magically fast pony is still slower than a magically fast
 * racehorse. Swimming has no natural loci under it at all: nothing in the
 * mod makes one horse a better swimmer than another, so this locus is not
 * scaling anything and its percentages are correspondingly larger.
 *
 * <p>It reaches the game as an {@code attribute} effect on
 * {@code water_movement_efficiency} rather than as a number on
 * {@link com.example.horsegenetics.common.trait.Traits} - see
 * {@link AbstractMagicFactorGene} for why that line is where it is.
 */
public final class MagicSwimSpeedGene extends AbstractMagicFactorGene {

    public static final String KEY = "horsegenetics.magic_swim_speed";
    public static final int PRIORITY = 144;

    /**
     * The vanilla attribute this multiplies - the share of its land speed a mob
     * keeps in water. There is no {@code swim_speed} attribute in vanilla, much
     * as the name suggests itself; this is the one that means it.
     */
    public static final String ATTRIBUTE = "water_movement_efficiency";

    public MagicSwimSpeedGene() {
        super(KEY, PRIORITY, "Magic swim speed",
                "Otr", "Otterlike (Otr)",
                "Stne", "Stonelike (Stne)",
                new AbstractMagicStatGene.Vocabulary(
                        "The horse swims the way any horse swims - badly, and at whatever pace "
                                + "the game gives it.",
                        "Stronger swimmer",
                        "One otterlike copy. In water the horse moves faster than it has any "
                                + "business doing, by the percentage written on that copy - "
                                + "usually about a quarter. On land it is completely unchanged, "
                                + "which is what makes the locus worth having as its own.",
                        "Much stronger swimmer",
                        "Two otterlike copies, and the percentages add. Half again on average, "
                                + "and a great deal more when both copies rolled well. No wild "
                                + "horse is born with two, so a genuinely fast swimmer is always "
                                + "something a breeder made.",
                        "Weaker swimmer",
                        "One stonelike copy. Slower in water by the percentage on that copy, and "
                                + "no slower at all on land.",
                        "Much weaker swimmer",
                        "Two stonelike copies, and the percentages subtract together. The horse "
                                + "crosses water at a crawl and is otherwise entirely ordinary.",
                        "Balanced",
                        "One of each. Their percentages nearly cancel, so the horse swims at "
                                + "about the usual pace while carrying, and passing on, both "
                                + "extremes."));
    }

    @Override
    protected List<GeneAbility> abilitiesFor(double factor) {
        // multiply_total, so it scales whatever the game (or another mod) has
        // already decided the horse's swim speed is, rather than replacing it.
        return List.of(new GeneAbility.AttributeMod(ATTRIBUTE, "multiply_total", factor - 1.0,
                GeneAbility.Condition.ALWAYS, 1));
    }
}
