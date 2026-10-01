package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneEpigenetics;
import com.example.horsegenetics.common.genetics.GeneRarity;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;

import java.util.List;

/**
 * <b>Crystal antlers</b> ({@code horsegenetics.antler_crystal}) - a <b>magical
 * recessive</b> that turns an antlered horse's rack to crystal: the beams and tines
 * see-through in a gem colour, the points solid. On a horse with no antlers it does
 * nothing.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code n/n}</td><td>wild type</td></tr>
 *   <tr><td>{@code Cry/n}</td><td>a wild type - nothing shows</td></tr>
 *   <tr><td>{@code Cry/Cry}</td><td>{@code antler-crystal} - crystal antlers</td></tr>
 * </table>
 *
 * <h2>Shafts see-through, points solid</h2>
 * The treatment's split (Attached model parts 12.5): the translucent pass is the one
 * that costs - it blends and sorts - so it draws only the shafts, and the points stay
 * in the ordinary opaque pass. It also reads better: a crystal with an opaque cap has
 * an edge.
 *
 * <p>It is a modifier and not its own part (D6, owner): it can only change antlers
 * the antlers locus already grew. Crystal growths on the body would be a separate
 * gene.
 *
 * <h2>The gem is on the copy</h2>
 * One epigenetic number, {@code gem}, picks the colour from {@link #GEMS}; it is
 * inherited with the allele and re-rolls only rarely, so a line of amethyst antlers
 * stays amethyst. A crystal rack takes the gem's colour instead of the bone's.
 */
public final class AntlerCrystalGene implements Gene {

    public static final String KEY = "horsegenetics.antler_crystal";

    /** Magical band, in the antler run. */
    public static final int PRIORITY = 233;

    /** Founders born {@code Cry/Cry}, in percent. */
    public static final double WILD_HOMOZYGOUS_PERCENT = 5.0;

    /** Which gem. An index into {@link #GEMS}. */
    public static final String GEM = "gem";

    /** The gems, as opaque ARGB. Append only - the epigenome stores the index. */
    public static final List<Integer> GEMS = List.of(
            0xFFE8F4FF,   // clear quartz
            0xFFB28CE6,   // amethyst
            0xFFF2A7C3,   // rose quartz
            0xFFF2C94C,   // citrine
            0xFF5CCB8A,   // emerald
            0xFF5B8DEF,   // sapphire
            0xFFE0525C);  // ruby

    public final Allele Cry = new Allele(KEY, 0, "Cry", "Crystal antlers (Cry)");
    public final Allele n = new Allele(KEY, 1, "n", "Wild-type (n)");
    private final List<Allele> alleles = List.of(Cry, n);

    private final Expression WILD = Expression.wildType("Antlers, if the horse has any, of bone.");

    private final Expression CARRIER = Expression.wildType("antler-crystal-carrier", "Crystal antler carrier",
            "One copy, which shows nothing. Two carriers bred together are how crystal antlers "
                    + "appear in a line that lacks them.");

    private final Expression CRYSTAL = Expression.wildType("antler-crystal", "Crystal antlers",
            "The antlers are crystal: beams and tines see-through in a gem colour - clear, "
                    + "amethyst, rose, citrine, emerald, sapphire or ruby - with solid points. The "
                    + "gem is inherited. Shows only on a horse with antlers.");

    private final List<Expression> expressions = List.of(WILD, CARRIER, CRYSTAL);

    private final FounderTable founders = FounderTable.builder()
            .weight(Cry, WILD_HOMOZYGOUS_PERCENT)
            .weight(n, 100.0 - WILD_HOMOZYGOUS_PERCENT)
            .build();

    @Override public String key() { return KEY; }
    @Override public String name() { return "Crystal antlers"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return false; }
    @Override public GeneRarity rarity() { return GeneRarity.RARE; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        return switch (pair.count(Cry)) {
            case 2 -> CRYSTAL;
            case 1 -> CARRIER;
            default -> WILD;
        };
    }

    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(EpiValue.category(GEM, GEMS.size()));
    }

    /** Would this horse's antlers be crystal, if it had any? Two copies, or nothing. */
    public boolean crystal(AllelePair pair) {
        return pair != null && pair.count(Cry) == 2;
    }

    /** The gem colour this horse's crystal antlers would be, read off the expressing copy. */
    public int gemOf(Genotype genotype, Epigenome epigenome) {
        int index = GeneEpigenetics.forGene(this, genotype, epigenome).expressed().category(GEM);
        return GEMS.get(Math.max(0, Math.min(GEMS.size() - 1, index)));
    }
}
