package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.EpigeneticAbilityContribution;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneEpigenetics;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;

import java.util.List;

/**
 * <b>Magic meat</b> ({@code horsegenetics.magic_meat}) - how much meat the
 * horse leaves.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code Mty/Mty}</td><td>{@code meaty} - the number written on the copies, in meat</td></tr>
 *   <tr><td>{@code Mty/n}, {@code n/n}</td><td>wild type - vanilla, which is no meat at all</td></tr>
 * </table>
 *
 * <h2>Not the same locus as {@link MagicItemDropGene}</h2>
 * That one decides what a horse leaves <i>instead of</i> its leather - diamonds,
 * a spawn egg, a sword. This one is additive and is about the animal rather
 * than about treasure: a meaty horse still drops whatever the item-drop locus
 * says, and the meat is on top of it. Keeping them apart means a diamond horse
 * can also be a meat horse, which is a cross worth making and would be a table
 * row nobody wrote if the two were one gene.
 *
 * <h2>How much is written on the copy, not on the allele</h2>
 * The {@code Mty} allele does not mean "six meat". It means "meat, and the
 * amount is this number" - a value carried by each allele copy, inherited with
 * it, and drifting slightly at every breeding. So two meaty horses are not
 * interchangeable, a foal's yield is its parents' rather than a fresh roll, and
 * a line selected for it climbs. The two copies' numbers are <b>averaged</b>
 * rather than added, because the quantity is a property of the animal and not a
 * dose: a homozygote is one horse, not two half-horses.
 */
public final class MagicMeatGene implements Gene, EpigeneticAbilityContribution {

    public static final String KEY = "horsegenetics.magic_meat";
    public static final int PRIORITY = 132;

    /** The name of the number a copy carries: how much meat it is worth. */
    public static final String YIELD = "yield";

    /** The range a founder's copy is drawn from. Breeding drift takes a line past it. */
    public static final double WILD_YIELD_MIN = 2.0;
    public static final double WILD_YIELD_MAX = 7.0;

    /** Share of wild horses that are the doubled form. Expressing combinations only. */
    public static final double WILD_MEATY_PERCENT = 1.2;

    public final Allele Mty = new Allele(KEY, 0, "Mty", "Meaty (Mty)");
    public final Allele n = new Allele(KEY, 1, "n", "Wild-type (n)");
    private final List<Allele> alleles = List.of(Mty, n);

    private final Expression WILD = Expression.wildType(
            "Whatever a horse normally drops, which does not include meat. A single meaty copy "
                    + "shows nothing, so a carrier is indistinguishable from a horse that has "
                    + "never had the allele.");

    private final Expression MEATY = Expression.wildType("meaty", "Meaty",
            "Two meaty copies. The horse drops meat as well as everything else it would have "
                    + "dropped. How much is a number written on the copies rather than on the "
                    + "allele - averaged between the two, inherited with them, and drifting a "
                    + "little every generation - so a line bred for it yields more than the "
                    + "horses it started from.");

    private final List<Expression> expressions = List.of(WILD, MEATY);

    /** The expressing homozygote and the plain horse - no invisible carriers in the wild. */
    private final FounderTable founders = FounderTable.builder()
            .weight(Mty, Mty, WILD_MEATY_PERCENT)
            .weight(n, n, 100.0 - WILD_MEATY_PERCENT)
            .build();

    @Override public String key() { return KEY; }
    @Override public String name() { return "Magic meat"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return false; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        return pair.homozygousFor(Mty) ? MEATY : WILD;
    }

    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(EpiValue.uniform(YIELD, WILD_YIELD_MIN, WILD_YIELD_MAX));
    }

    @Override
    public List<GeneAbility> abilitiesFor(AllelePair pair, Genotype genotype,
                                          GeneEpigenetics epigenetics) {
        if (!pair.homozygousFor(Mty)) {
            return List.of();
        }
        // Averaged, not summed - see the class note. Rounded to at least one,
        // because "meaty, and drops nothing" is not an outcome anyone means.
        double average = (epigenetics.copy(0).get(YIELD) + epigenetics.copy(1).get(YIELD)) / 2.0;
        int count = Math.max(1, (int) Math.round(average));
        return List.of(new GeneAbility.ItemDrop("meat", count, count,
                GeneAbility.Condition.ALWAYS, 1));
    }
}
