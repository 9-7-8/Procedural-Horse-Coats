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
import com.example.horsegenetics.common.genetics.epi.EpiValues;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.common.parts.AttachedPart;
import com.example.horsegenetics.common.parts.RamHornSize;

import java.util.List;
import java.util.Optional;

/**
 * <b>Ram horns</b> ({@code horsegenetics.ram_horns}) - a <b>magical, dominant</b>
 * gene that grows a pair of ram's horns, built on the antler pattern (owner,
 * 2026-10-01): a presence locus with a sex-limited allele, a shape locus
 * ({@link RamHornFormGene}) and a modifier ({@link RamHornTipGene}).
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code n/n}</td><td>wild type - polled</td></tr>
 *   <tr><td>any {@code Rh}</td><td>{@code ram-horns} - horns on mares and stallions alike</td></tr>
 *   <tr><td>{@code Rhm} without {@code Rh}</td><td>{@code ram-horns-stallion} - horns on a stallion;
 *       a mare is polled and carries it, as the ewes of most horned sheep breeds are</td></tr>
 * </table>
 *
 * <p>The sex gate is applied where the part is resolved ({@link #grows}), never in the
 * expression - {@link AntlersGene} explains why that is safe for a part. A foal grows
 * none ({@code PartKind.showsOnFoal}).
 *
 * <h2>The numbers on the copy</h2>
 * {@code size} (the {@link RamHornSize} class and a stretch), {@code curl} (how far
 * round a curl goes, how tight a corkscrew twists - a mesh bucket), {@code girth},
 * and {@code shade}, pale horn to black: the colour of the whole horn, or of its base
 * when {@link RamHornTipGene} colours the point.
 */
public final class RamHornsGene implements Gene {

    public static final String KEY = "horsegenetics.ram_horns";

    /** Magical band, after the antler run. Only a code-order slot. */
    public static final int PRIORITY = 235;

    /** Founders, in percent, written as pairs - the antler figures: rare, and mostly rams-only. */
    public static final double WILD_RH_PERCENT = 0.14;
    /** @see #WILD_RH_PERCENT */
    public static final double WILD_RHM_PERCENT = 0.26;

    public final Allele Rh = new Allele(KEY, 0, "Rh", "Ram horns (Rh)");
    public final Allele Rhm = new Allele(KEY, 1, "Rhm", "Stallion ram horns (Rhm)");
    public final Allele n = new Allele(KEY, 2, "n", "Polled (n)");
    private final List<Allele> alleles = List.of(Rh, Rhm, n);

    private final Expression WILD = Expression.wildType("An ordinary, polled horse.");

    private final Expression HORNED = Expression.wildType("ram-horns", "Ram horns",
            "A pair of ram's horns, on a mare or a stallion alike. Their size, how far they "
                    + "curl, their thickness and their shade are epigenetic and inherited with "
                    + "the allele; their shape is another gene. Grown at maturity - a foal has none.");

    private final Expression STALLION = Expression.wildType("ram-horns-stallion", "Stallion ram horns",
            "Ram's horns on a stallion only. A mare with this is polled and passes it on - the "
                    + "genotype alone will not tell you, so look at her colts.");

    private final List<Expression> expressions = List.of(WILD, HORNED, STALLION);

    private final FounderTable founders = FounderTable.builder()
            .weight(Rh, n, WILD_RH_PERCENT)
            .weight(Rhm, n, WILD_RHM_PERCENT)
            .weight(n, 100.0 - WILD_RH_PERCENT - WILD_RHM_PERCENT)
            .build();

    /** Position on the {@link RamHornSize} ladder. */
    public static final String SIZE = "size";
    /** How far round a curl goes; how tight a corkscrew twists. */
    public static final String CURL = "curl";
    /** Thickness on top of the size. */
    public static final String GIRTH = "girth";
    /**
     * Pale horn at 0 to black at 1 - and, below 0, from pale horn to bare <b>bone</b>
     * at -1. No wild horse rolls below 0 (the founder roll is 0..1, as it always was), so
     * the bone end is reached only by a breed that bands it: the skeleton horse, whose
     * ram's horns are bone like everything else on it (owner, 2026-10-02 - every part that
     * has bone has a bone version; wiki/model-parts.html#bone-versions).
     */
    public static final String SHADE = "shade";

    /** The two ends of {@link #SHADE}'s wild range. */
    static final int PALE_HORN = 0xFFDCCDA8;
    /** @see #PALE_HORN */
    static final int BLACK_HORN = 0xFF141110;
    /** {@link #SHADE} at -1: the antler's pale bone ({@code AntlersGene.PALE_BONE}), so every bone part matches. */
    static final int BONE = AntlersGene.PALE_BONE;

    @Override public String key() { return KEY; }
    @Override public String name() { return "Ram horns"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return false; }
    @Override public GeneRarity rarity() { return GeneRarity.RARE; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        if (pair.count(Rh) > 0) {
            return HORNED;
        }
        return pair.count(Rhm) > 0 ? STALLION : WILD;
    }

    /** Would this horse grow horns - the genotype and the sex gate together. */
    public boolean grows(Genotype genotype) {
        AllelePair pair = genotype.pair(this);
        if (pair.count(Rh) > 0) {
            return true;
        }
        return pair.count(Rhm) > 0 && genotype.sex() == Sex.MALE;
    }

    /** Size wild in the lower part of the ladder and bred toward massive; shade pale to black. */
    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(
                EpiValue.uniform(SIZE, 0.0, 0.6).clampedTo(0.0, 1.0),
                EpiValue.uniform(CURL, 0.0, 1.0).clampedTo(0.0, 1.0),
                EpiValue.uniform(GIRTH, 0.85, 1.15).clampedTo(0.6, 1.5),
                EpiValue.uniform(SHADE, 0.0, 1.0).clampedTo(-1.0, 1.0));
    }

    /**
     * The pair this horse grows, right then left, one colour root to tip - or empty
     * for a polled horse. The shape is {@link RamHornFormGene}'s; ask
     * {@code GrownParts.of} for the finished pair.
     */
    public Optional<List<AttachedPart>> hornsFor(Genotype genotype, Epigenome epigenome, int form) {
        if (!grows(genotype)) {
            return Optional.empty();
        }
        EpiValues epi = GeneEpigenetics.forGene(this, genotype, epigenome).expressed();
        int tint = shadeOf(epi.get(SHADE));
        return Optional.of(List.of(
                AttachedPart.ramHorn(false, form, epi.get(CURL), epi.get(SIZE), epi.get(GIRTH), tint),
                AttachedPart.ramHorn(true, form, epi.get(CURL), epi.get(SIZE), epi.get(GIRTH), tint)));
    }

    /** The keratin colour this horse's horns are, or would be. */
    public int shadeOf(Genotype genotype, Epigenome epigenome) {
        return shadeOf(GeneEpigenetics.forGene(this, genotype, epigenome).expressed().get(SHADE));
    }

    /** What to call the horns - "a heavy pair". Empty for a polled horse. */
    public Optional<RamHornSize> sizeOf(Genotype genotype, Epigenome epigenome) {
        if (!grows(genotype)) {
            return Optional.empty();
        }
        return Optional.of(RamHornSize.of(
                GeneEpigenetics.forGene(this, genotype, epigenome).expressed().get(SIZE)));
    }

    static int shadeOf(double t) {
        if (t < 0.0) {
            double b = t < -1.0 ? 1.0 : -t;
            return 0xFF000000 | lerp(PALE_HORN >> 16, BONE >> 16, b) << 16
                    | lerp(PALE_HORN >> 8, BONE >> 8, b) << 8 | lerp(PALE_HORN, BONE, b);
        }
        double c = t > 1.0 ? 1.0 : t;
        int r = lerp(PALE_HORN >> 16, BLACK_HORN >> 16, c);
        int g = lerp(PALE_HORN >> 8, BLACK_HORN >> 8, c);
        int b = lerp(PALE_HORN, BLACK_HORN, c);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    private static int lerp(int a, int b, double t) {
        a &= 0xFF;
        b &= 0xFF;
        return (int) Math.round(a + (b - a) * t);
    }
}
