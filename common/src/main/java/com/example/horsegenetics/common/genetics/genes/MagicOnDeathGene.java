package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.AbilityContribution;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;

import java.util.List;

/**
 * <b>Magic on death</b> ({@code horsegenetics.magic_on_death}) - what happens
 * to the <i>world</i> where the horse died.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code Lav/Lav}</td><td>{@code lava} - a lava source at its feet</td></tr>
 *   <tr><td>{@code Wat/Wat}</td><td>{@code water} - a water source at its feet</td></tr>
 *   <tr><td>{@code Xpl/Xpl}</td><td>{@code explode} - it goes off like a creeper</td></tr>
 *   <tr><td>anything with an {@code n}, or two different variants</td><td>wild type</td></tr>
 * </table>
 *
 * <h2>Not a drop table</h2>
 * This locus deliberately says <b>nothing</b> about items. What a horse leaves
 * behind is {@link MagicItemDropGene}'s business and {@link MagicMeatGene}'s,
 * and the two questions are kept apart so that "drops diamonds and leaves a
 * crater" is what it looks like - two independent loci - rather than a single
 * table with every combination spelled out in it.
 *
 * <h2>Every expression takes two copies, and no wild horse has two</h2>
 * All three variants are recessive to the wild type <b>and to each other</b>:
 * one copy shows nothing, and a horse carrying two <i>different</i> variants
 * shows nothing either, because the two answers to "what happens here" cannot
 * both be right.
 *
 * <p>The founder table therefore lists the three <b>carriers</b> and the plain
 * horse, and no doubled form at all - so a feral horse is never an exploding
 * one. That is the opposite of the rule the other magical loci follow, where
 * the wild population is put on the <i>expressing</i> combinations precisely so
 * that a player can see what they are catching, and it is deliberate: the other
 * loci are things you want, and this one is a hazard. A horse that leaves a
 * crater has to be somebody's fault. (Owner's call: "homozygous versions never
 * appear in Feral Mixed horses".)
 */
public final class MagicOnDeathGene implements Gene, AbilityContribution {

    public static final String KEY = "horsegenetics.magic_on_death";
    public static final int PRIORITY = 134;

    /** Share of wild horses carrying one copy of each variant. Carriers only - see the class note. */
    public static final double WILD_LAVA_PERCENT = 3.0;
    public static final double WILD_WATER_PERCENT = 5.0;
    public static final double WILD_EXPLODE_PERCENT = 2.0;

    public final Allele Lav = new Allele(KEY, 0, "Lav", "Lava spring (Lav)");
    public final Allele Wat = new Allele(KEY, 1, "Wat", "Water spring (Wat)");
    public final Allele Xpl = new Allele(KEY, 2, "Xpl", "Volatile (Xpl)");
    public final Allele n = new Allele(KEY, 3, "n", "Wild-type (n)");
    private final List<Allele> alleles = List.of(Lav, Wat, Xpl, n);

    private final Expression WILD = Expression.wildType(
            "Nothing happens where the horse died. This covers every combination with a wild-type "
                    + "copy in it and also every pair of two DIFFERENT variants - the three "
                    + "answers are mutually exclusive, so a horse carrying two of them gives "
                    + "none.");

    private final Expression LAVA = Expression.wildType("lava", "Lava spring",
            "Two lava copies. When the horse dies a lava source appears where it was standing. "
                    + "It is a real source block and it flows, so where the horse dies matters "
                    + "rather a lot.");

    private final Expression WATER = Expression.wildType("water", "Water spring",
            "Two water copies. A water source appears where the horse died - the useful half of "
                    + "the same idea, and by some distance the safest thing on this locus.");

    private final Expression EXPLODE = Expression.wildType("explode", "Volatile",
            "Two volatile copies. The horse goes off like a creeper when it dies, with everything "
                    + "that implies for whatever killed it, whatever was standing nearby, and the "
                    + "ground underneath. No wild horse carries two, so every one of these was "
                    + "bred on purpose by somebody.");

    private final List<Expression> expressions = List.of(WILD, LAVA, WATER, EXPLODE);

    /** Carriers and the plain horse. No doubled form is ever born wild - see the class note. */
    private final FounderTable founders = FounderTable.builder()
            .weight(Lav, n, WILD_LAVA_PERCENT)
            .weight(Wat, n, WILD_WATER_PERCENT)
            .weight(Xpl, n, WILD_EXPLODE_PERCENT)
            .weight(n, n, 100.0 - WILD_LAVA_PERCENT - WILD_WATER_PERCENT - WILD_EXPLODE_PERCENT)
            .build();

    private final List<GeneAbility> lava = one("lava");
    private final List<GeneAbility> water = one("water");
    private final List<GeneAbility> explode = one("explode");

    private static List<GeneAbility> one(String effect) {
        return List.of(new GeneAbility.OnDeath(effect, GeneAbility.Condition.ALWAYS, 1));
    }

    @Override public String key() { return KEY; }
    @Override public String name() { return "Magic on death"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return false; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        if (pair.homozygousFor(Lav)) {
            return LAVA;
        }
        if (pair.homozygousFor(Wat)) {
            return WATER;
        }
        return pair.homozygousFor(Xpl) ? EXPLODE : WILD;
    }

    @Override
    public List<GeneAbility> abilitiesFor(AllelePair pair, Genotype genotype) {
        if (pair.homozygousFor(Lav)) {
            return lava;
        }
        if (pair.homozygousFor(Wat)) {
            return water;
        }
        return pair.homozygousFor(Xpl) ? explode : List.of();
    }
}
