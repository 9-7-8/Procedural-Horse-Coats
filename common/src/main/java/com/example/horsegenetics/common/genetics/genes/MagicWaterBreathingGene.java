package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.spec.GeneAbility;

import java.util.List;

/**
 * <b>Magic water breathing</b> ({@code horsegenetics.magic_water_breathing}) -
 * how long the horse lasts under water before it starts to drown.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th><th>time under</th></tr>
 *   <tr><td>{@code n/n}</td><td>wild type</td><td>the usual fifteen seconds</td></tr>
 *   <tr><td>{@code Gil/n}</td><td>{@code more}</td><td>&times; about 1.25</td></tr>
 *   <tr><td>{@code Gil/Gil}</td><td>{@code double-more}</td><td><b>both</b> copies' percentages, added</td></tr>
 *   <tr><td>{@code Shal/n}</td><td>{@code less}</td><td>&times; about 0.75</td></tr>
 *   <tr><td>{@code Shal/Shal}</td><td>{@code double-less}</td><td>both copies' percentages off</td></tr>
 *   <tr><td>{@code Gil/Shal}</td><td>{@code balanced}</td><td>the two nearly cancel</td></tr>
 * </table>
 *
 * <h2>Graded, not absolute</h2>
 * There is already an absolute version of this in the format - the
 * {@code underwater_breathing} traversal flag, which simply refills the horse's
 * air every tick and can never be beaten. This locus is the <i>dial</i>, and
 * the two are meant to coexist: a horse that cannot drown does not care how
 * slowly it would have, and a horse that can gets a number worth breeding for.
 *
 * <p>It is deliberately independent of {@link MagicSwimSpeedGene}. Crossing a
 * river quickly and surviving the bottom of a lake are different problems, and
 * a breeder who wants a horse that can do both has to select for two loci
 * rather than one.
 */
public final class MagicWaterBreathingGene extends AbstractMagicFactorGene {

    public static final String KEY = "horsegenetics.magic_water_breathing";
    public static final int PRIORITY = 145;

    public MagicWaterBreathingGene() {
        super(KEY, PRIORITY, "Magic water breathing",
                "Gil", "Gilled (Gil)",
                "Shal", "Shallow-lunged (Shal)",
                new AbstractMagicStatGene.Vocabulary(
                        "The horse holds its breath as long as anything else does, and drowns on "
                                + "the usual schedule.",
                        "Longer breath",
                        "One gilled copy. The horse lasts longer under water before the first "
                                + "drowning tick, by the percentage written on that copy - "
                                + "usually about a quarter. It swims no faster; this is lung "
                                + "capacity, not propulsion.",
                        "Much longer breath",
                        "Two gilled copies, and the percentages add. Half again on average and "
                                + "far more at the extreme, which is long enough to make a horse "
                                + "a genuine way of getting somewhere underwater.",
                        "Shorter breath",
                        "One shallow-lunged copy. The horse drowns sooner by the percentage on "
                                + "that copy - a real hazard on a river crossing and invisible "
                                + "anywhere else.",
                        "Much shorter breath",
                        "Two shallow-lunged copies, subtracting together. The horse has very "
                                + "little time under water and its owner finds out the first time "
                                + "it matters.",
                        "Balanced",
                        "One of each. They nearly cancel, so the horse breathes for about the "
                                + "usual time while carrying both extremes."));
    }

    @Override
    protected List<GeneAbility> abilitiesFor(double factor) {
        return List.of(new GeneAbility.Breath(factor, GeneAbility.Condition.ALWAYS, 1));
    }
}
