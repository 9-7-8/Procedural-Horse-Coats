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
 * <b>Blooming antlers</b> ({@code horsegenetics.antler_bloom}) - a <b>magical
 * recessive</b> that grows leaves, moss or blossom on an antlered horse's rack: a
 * clump on every tine and a few along each beam. On a horse with no antlers it does
 * nothing.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code n/n}</td><td>wild type</td></tr>
 *   <tr><td>{@code Blm/n}</td><td>a wild type - nothing shows</td></tr>
 *   <tr><td>{@code Blm/Blm}</td><td>{@code antler-bloom} - the antlers bloom</td></tr>
 * </table>
 *
 * <h2>It costs boxes, not a system</h2>
 * Every antler mesh already carries its clumps, in the sheet's bloom region; a
 * horse without this gene simply never draws that region. So blooming is one more
 * submit of a mesh that is already baked, in the growth's colour, and a clump on a
 * tine the horse does not show hides with the tine.
 *
 * <h2>What grows is on the copy</h2>
 * One number, {@code growth}, picks from {@link #GROWTHS}: leaves, moss or blossom.
 * It is inherited with the allele and re-rolls only rarely.
 */
public final class AntlerBloomGene implements Gene {

    public static final String KEY = "horsegenetics.antler_bloom";

    /** Magical band, last of the antler run. */
    public static final int PRIORITY = 234;

    /** Founders born {@code Blm/Blm}, in percent. */
    public static final double WILD_HOMOZYGOUS_PERCENT = 8.0;

    /** What grows. An index into {@link #GROWTHS}. */
    public static final String GROWTH = "growth";

    /** One kind of growth: its name and its colour as opaque ARGB. */
    public record Growth(String name, int tint) {}

    /** Append only - the epigenome stores the index. */
    public static final List<Growth> GROWTHS = List.of(
            new Growth("leaves", 0xFF4F9A3A),
            new Growth("moss", 0xFF7A8F3A),
            new Growth("blossom", 0xFFF3B6CF));

    public final Allele Blm = new Allele(KEY, 0, "Blm", "Blooming antlers (Blm)");
    public final Allele n = new Allele(KEY, 1, "n", "Wild-type (n)");
    private final List<Allele> alleles = List.of(Blm, n);

    private final Expression WILD = Expression.wildType("Antlers, if the horse has any, that are bare.");

    private final Expression CARRIER = Expression.wildType("antler-bloom-carrier", "Antler bloom carrier",
            "One copy, which shows nothing. Two carriers bred together are how blooming antlers "
                    + "appear in a line that lacks them.");

    private final Expression BLOOM = Expression.wildType("antler-bloom", "Blooming antlers",
            "Leaves, moss or blossom grow on the antlers - a clump on every tine and a few along "
                    + "each beam. Which of the three is inherited. Shows only on a horse with "
                    + "antlers.");

    private final List<Expression> expressions = List.of(WILD, CARRIER, BLOOM);

    private final FounderTable founders = FounderTable.builder()
            .weight(Blm, WILD_HOMOZYGOUS_PERCENT)
            .weight(n, 100.0 - WILD_HOMOZYGOUS_PERCENT)
            .build();

    @Override public String key() { return KEY; }
    @Override public String name() { return "Blooming antlers"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return false; }
    @Override public GeneRarity rarity() { return GeneRarity.RARE; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        return switch (pair.count(Blm)) {
            case 2 -> BLOOM;
            case 1 -> CARRIER;
            default -> WILD;
        };
    }

    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(EpiValue.category(GROWTH, GROWTHS.size()));
    }

    /** Would this horse's antlers bloom, if it had any? Two copies, or nothing. */
    public boolean blooms(AllelePair pair) {
        return pair != null && pair.count(Blm) == 2;
    }

    /** What would grow on this horse's antlers, read off the expressing copy. */
    public Growth growthOf(Genotype genotype, Epigenome epigenome) {
        int index = GeneEpigenetics.forGene(this, genotype, epigenome).expressed().category(GROWTH);
        return GROWTHS.get(Math.max(0, Math.min(GROWTHS.size() - 1, index)));
    }
}
