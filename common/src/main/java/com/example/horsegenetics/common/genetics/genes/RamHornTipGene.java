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
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;

import java.util.List;

/**
 * <b>Ram horn tip</b> ({@code horsegenetics.ram_horn_tip}) - a <b>magical
 * recessive</b> that gives a horned horse's ram horns a coloured point: the horn
 * keeps its own shade at the base and fades into this colour toward the tip. On a
 * polled horse it does nothing.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code n/n}</td><td>wild type</td></tr>
 *   <tr><td>{@code Tip/n}</td><td>a wild type - nothing shows</td></tr>
 *   <tr><td>{@code Tip/Tip}</td><td>{@code ram-horn-tip} - coloured points</td></tr>
 * </table>
 *
 * <p>The colour is on the copy, rolled bright, and drifts as a lineage does - any
 * colour at all. It is also what the molten hooves locus's {@code MltH} allele prints
 * in, through {@link #printColourOf}, so a horse's hoofprints match its horn tips
 * (owner, 2026-10-01) - asked of this gene, not worked out twice.
 */
public final class RamHornTipGene implements Gene {

    public static final String KEY = "horsegenetics.ram_horn_tip";

    /** Magical band, after {@link RamHornFormGene}. */
    public static final int PRIORITY = 238;

    /** Founders born {@code Tip/Tip}, in percent - no wild carriers. */
    public static final double WILD_HOMOZYGOUS_PERCENT = 5.0;

    /** Prefix of the tip colour's three channels. */
    public static final String TIP = "tip";

    public final Allele Tip = new Allele(KEY, 0, "Tip", "Coloured horn tip (Tip)");
    public final Allele n = new Allele(KEY, 1, "n", "Wild-type (n)");
    private final List<Allele> alleles = List.of(Tip, n);

    private final Expression WILD = Expression.wildType("Horns, if the horse has any, one colour root to tip.");

    private final Expression CARRIER = Expression.wildType("ram-horn-tip-carrier", "Horn tip carrier",
            "One copy, which shows nothing. Two carriers bred together are how coloured horn tips "
                    + "appear in a line that lacks them.");

    private final Expression TIPPED = Expression.wildType("ram-horn-tip", "Coloured horn tips",
            "The ram horns keep their own shade at the base and fade into a colour of their own "
                    + "toward the points - any colour, inherited with the allele. Shows only on a "
                    + "horned horse.");

    private final List<Expression> expressions = List.of(WILD, CARRIER, TIPPED);

    private final FounderTable founders = FounderTable.builder()
            .weight(Tip, WILD_HOMOZYGOUS_PERCENT)
            .weight(n, 100.0 - WILD_HOMOZYGOUS_PERCENT)
            .build();

    @Override public String key() { return KEY; }
    @Override public String name() { return "Ram horn tip"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return false; }
    @Override public GeneRarity rarity() { return GeneRarity.RARE; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        return switch (pair.count(Tip)) {
            case 2 -> TIPPED;
            case 1 -> CARRIER;
            default -> WILD;
        };
    }

    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of().and(EpiValue.colour(TIP, 0.55, 1.00, 0.70, 1.00));
    }

    /** Two copies, or nothing. */
    public boolean tipped(AllelePair pair) {
        return pair != null && pair.count(Tip) == 2;
    }

    /** The tip colour on the expressing copy, opaque ARGB - whether or not it shows. */
    public int tipColourOf(Genotype genotype, Epigenome epigenome) {
        return 0xFF000000 | GeneEpigenetics.forGene(this, genotype, epigenome).expressed().rgb(TIP);
    }

    /**
     * The colour a {@code MltH} horse's hoofprints are: its horn tips' colour when it
     * has coloured tips, and otherwise its horns' own shade - so the prints always
     * match the end of the horn, horned or not.
     */
    public int printColourOf(Genotype genotype, Epigenome epigenome) {
        if (tipped(genotype.pair(this))) {
            return tipColourOf(genotype, epigenome);
        }
        return Genes.RAM_HORNS.shadeOf(genotype, epigenome);
    }
}
