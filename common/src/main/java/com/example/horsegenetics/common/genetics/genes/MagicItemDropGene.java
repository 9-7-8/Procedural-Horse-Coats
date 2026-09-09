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
 * <b>Magic item drop</b> ({@code horsegenetics.magic_item_drop}) - what the
 * horse leaves behind.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code Dia/Dia}</td><td>{@code diamonds} - {@value #DIAMOND_MIN} to {@value #DIAMOND_MAX} diamonds instead of the leather</td></tr>
 *   <tr><td>{@code Egg/Egg}</td><td>{@code spawn-egg} - an egg that puts <b>this horse</b> back</td></tr>
 *   <tr><td>{@code Swd/Swd}</td><td>{@code sword} - an enchanted sword, the enchantment rolled at the time</td></tr>
 *   <tr><td>anything with an {@code n}, or two different variants</td><td>wild type - vanilla leather</td></tr>
 * </table>
 *
 * <h2>Not the same question as {@link MagicOnDeathGene}</h2>
 * That locus decides what happens to the <i>ground</i>; this one decides what
 * is on it afterwards. Keeping them apart is what makes "explodes and drops
 * diamonds" a cross of two loci rather than a ninth row of one table.
 *
 * <h2>The egg is the interesting one</h2>
 * {@code Egg/Egg} drops a spawn egg carrying this horse's own genotype and
 * epigenome, so what comes back out is a <b>clone</b> and not a horse with the
 * same alleles: the same coat, the same particle colours, the same percentages
 * on every copy. It is the only thing in the mod that undoes a death, which is
 * why it takes two copies of a recessive that no amount of looking at a horse
 * will reveal.
 *
 * <h2>Every expression takes two copies</h2>
 * All three variants are recessive to the wild type and to each other - two
 * different variants give the vanilla drop, because a horse cannot leave
 * behind two contradictory things. Unlike {@link MagicOnDeathGene}, the
 * doubled forms <b>are</b> in the founder table: these are rewards rather than
 * hazards, and the standing rule for a magical locus whose carrier is invisible
 * is that wild horses sit on the combinations that actually show. What you
 * catch is what you watched it do; the carriers turn up a generation later.
 */
public final class MagicItemDropGene implements Gene, AbilityContribution {

    public static final String KEY = "horsegenetics.magic_item_drop";
    public static final int PRIORITY = 133;

    /** How many diamonds a doubled diamond horse leaves. */
    public static final int DIAMOND_MIN = 2;
    public static final int DIAMOND_MAX = 5;

    /** Share of wild horses that are the doubled form. Expressing combinations only - see the class note. */
    public static final double WILD_DIAMOND_PERCENT = 0.06;
    public static final double WILD_EGG_PERCENT = 0.04;
    public static final double WILD_SWORD_PERCENT = 0.10;

    public final Allele Dia = new Allele(KEY, 0, "Dia", "Diamond-bearing (Dia)");
    public final Allele Egg = new Allele(KEY, 1, "Egg", "Self-seeding (Egg)");
    public final Allele Swd = new Allele(KEY, 2, "Swd", "Sword-bearing (Swd)");
    public final Allele n = new Allele(KEY, 3, "n", "Wild-type (n)");
    private final List<Allele> alleles = List.of(Dia, Egg, Swd, n);

    private final Expression WILD = Expression.wildType(
            "Whatever a horse normally drops, which is leather. This covers every combination "
                    + "with a wild-type copy and every pair of two DIFFERENT variants - the horse "
                    + "cannot leave behind two contradictory things, so it leaves the ordinary "
                    + "one.");

    private final Expression DIAMONDS = Expression.wildType("diamonds", "Diamond-bearing",
            "Two diamond copies. The horse drops " + DIAMOND_MIN + " to " + DIAMOND_MAX
                    + " diamonds instead of its leather. There is nothing about the living animal "
                    + "that shows this, which is the point: it is worth breeding a line for and "
                    + "impossible to spot in a field.");

    private final Expression SPAWN_EGG = Expression.wildType("spawn-egg", "Self-seeding",
            "Two self-seeding copies. The horse drops a spawn egg carrying its own genotype AND "
                    + "its own epigenome, so what comes back out is a clone - the same coat, the "
                    + "same colours, the same percentages written on every allele copy - and not "
                    + "merely a horse with the same alleles. It is the only thing in the mod that "
                    + "undoes a death.");

    private final Expression SWORD = Expression.wildType("sword", "Sword-bearing",
            "Two sword copies. The horse drops an enchanted sword, with one enchantment rolled "
                    + "when it dies rather than fixed by the gene - so two horses of identical "
                    + "genotype do not leave identical swords, and this is the one drop here that "
                    + "is a gamble rather than a certainty.");

    private final List<Expression> expressions = List.of(WILD, DIAMONDS, SPAWN_EGG, SWORD);

    /** The expressing combinations and the plain horse - no invisible carriers in the wild. */
    private final FounderTable founders = FounderTable.builder()
            .weight(Dia, Dia, WILD_DIAMOND_PERCENT)
            .weight(Egg, Egg, WILD_EGG_PERCENT)
            .weight(Swd, Swd, WILD_SWORD_PERCENT)
            .weight(n, n, 100.0 - WILD_DIAMOND_PERCENT - WILD_EGG_PERCENT - WILD_SWORD_PERCENT)
            .build();

    private final List<GeneAbility> diamonds =
            one("diamonds", DIAMOND_MIN, DIAMOND_MAX);
    private final List<GeneAbility> egg = one("spawn_egg", 1, 1);
    private final List<GeneAbility> sword = one("enchanted_sword", 1, 1);

    private static List<GeneAbility> one(String drop, int min, int max) {
        return List.of(new GeneAbility.ItemDrop(drop, min, max, GeneAbility.Condition.ALWAYS, 1));
    }

    @Override public String key() { return KEY; }
    @Override public String name() { return "Magic item drop"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return false; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        if (pair.homozygousFor(Dia)) {
            return DIAMONDS;
        }
        if (pair.homozygousFor(Egg)) {
            return SPAWN_EGG;
        }
        return pair.homozygousFor(Swd) ? SWORD : WILD;
    }

    @Override
    public List<GeneAbility> abilitiesFor(AllelePair pair, Genotype genotype) {
        if (pair.homozygousFor(Dia)) {
            return diamonds;
        }
        if (pair.homozygousFor(Egg)) {
            return egg;
        }
        return pair.homozygousFor(Swd) ? sword : List.of();
    }
}
