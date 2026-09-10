package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;
import com.example.horsegenetics.common.genetics.epi.EpiValues;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;

import java.util.List;

/**
 * <b>Egg layer</b> ({@code horsegenetics.egg_layer}) - the horse lays. An egg,
 * at first, but the locus takes an allele for any item at all.
 *
 * <h2>The third way a horse gives you something</h2>
 * {@link MagicItemDropGene} is what it leaves when it dies, {@link MilkGene} is
 * what it gives when you ask, and this is what it produces while nobody is
 * watching. That symmetry is the argument the {@code produce} verb is real
 * rather than a special case - it is the missing corner of a table, not a new
 * table.
 *
 * <h2>What may be laid, and what may be found</h2>
 * The verb accepts any item id. The balance therefore lives entirely in the
 * <b>founder table</b>, and this locus keeps to things a chicken could plausibly
 * be jealous of. Nothing valuable is rollable in the wild; a horse that lays
 * something worth having is one somebody engineered through the splice layer,
 * which is where an open item vocabulary belongs.
 *
 * <h2>On the ground, not into a chest</h2>
 * Laying into the horse's inventory is tidier and makes the accumulation hazard
 * disappear. It was rejected because it silently does nothing on a horse with no
 * chest, and a gene you cannot see working is a failure mode this mod already
 * tracks. The {@code nearby_cap} on the verb is what keeps the ground version
 * honest.
 */
public final class EggLayerGene extends AbstractMatchedPairGene {

    public static final String KEY = "horsegenetics.egg_layer";
    public static final int PRIORITY = 179;

    /** The one value a copy carries: ticks between layings. */
    public static final String INTERVAL = "interval";

    /** A Minecraft day is 24000 ticks. Bounded so breeding can hurry it, not transform it. */
    public static final double MIN_INTERVAL = 4000;
    public static final double MAX_INTERVAL = 14000;

    /** Skip the drop when this many are already lying nearby - the accumulation guard. */
    public static final int NEARBY_CAP = 8;

    public static final double WILD_CARRIER_PERCENT = 14.0;

    public EggLayerGene() {
        super(KEY, PRIORITY, "Egg layer",
                List.of(
                        new Variant0("Egg", "minecraft:egg", "Egg"),
                        new Variant0("Fthr", "minecraft:feather", "Feather"),
                        new Variant0("Wl", "minecraft:white_wool", "Wool"),
                        new Variant0("Slm", "minecraft:slime_ball", "Slime ball"),
                        new Variant0("Ink", "minecraft:ink_sac", "Ink sac"),
                        new Variant0("Str", "minecraft:string", "String"),
                        new Variant0("Bne", "minecraft:bone", "Bone"),
                        new Variant0("Lthr", "minecraft:leather", "Leather")),
                WILD_CARRIER_PERCENT,
                "The horse produces nothing on its own.",
                "One copy, and nothing is laid. The horse carries a laying allele and shows no "
                        + "sign of it.",
                "Two different laying alleles. The horse settles on neither and lays nothing.",
                new MatchedText() {
                    @Override public String name(Variant v) {
                        return "Lays " + v.label().toLowerCase();
                    }

                    @Override public String description(Variant v) {
                        return "Two matching copies. Every few hours the horse drops "
                                + v.label().toLowerCase() + " on the ground where it is standing, "
                                + "with no prompting and nothing required from you. How often is "
                                + "written on the allele copy. It stops bothering if enough is "
                                + "already lying about.";
                    }
                });
    }

    @Override
    protected String idPrefix() {
        return "lays";
    }

    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(EpiValue.uniform(INTERVAL, MIN_INTERVAL, MAX_INTERVAL));
    }

    @Override
    protected List<GeneAbility> abilitiesFor(Variant v, EpiValues epi) {
        int interval = (int) Math.round(epi.get(INTERVAL));
        return List.of(new GeneAbility.Produce(v.subject(), interval, 1, 1, NEARBY_CAP,
                GeneAbility.Condition.ALWAYS, 1));
    }
}
