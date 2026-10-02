package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneEpigenetics;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;
import com.example.horsegenetics.common.genetics.epi.EpiValues;
import com.example.horsegenetics.common.parts.AttachedPart;
import com.example.horsegenetics.common.parts.DragonHornGenerator;
import com.example.horsegenetics.common.parts.DragonHornSize;

import java.util.List;
import java.util.Optional;

/**
 * <b>Dragon horns</b> ({@code horsegenetics.dragon_horns}) - a <b>magical,
 * recessive</b> gene that grows a pair of horns behind the ears, swept back over the
 * neck. The first of the head parts (owner, 2026-10-01), and built on the unicorn
 * horn's rule: rare, two copies needed, and the shape on the allele copy.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code n/n}</td><td>wild type</td></tr>
 *   <tr><td>{@code Drg/n}</td><td>{@code dragon-horns-carrier} - a wild type; nothing shows</td></tr>
 *   <tr><td>{@code Drg/Drg}</td><td>{@code dragon-horns} - the pair</td></tr>
 * </table>
 *
 * <p>Not sex-limited - mares and stallions alike, unlike the antlers' {@code Antm} -
 * and not on a foal ({@code PartKind.showsOnFoal}): a hard part comes with maturity.
 * It does nothing but show: no stat, no ability, no hitbox. It paints nothing, so
 * every outcome is a wild type and neither coat golden reads it.
 *
 * <h2>The numbers on the copy</h2>
 * {@code length} (the {@link DragonHornSize} class and a stretch) and {@code girth}
 * are a draw-time scale; {@code form} (swept, straight, curled), {@code sweep} (how far
 * it bends back) and {@code splay} (how far the pair angle outward) are baked into the
 * mesh - see {@link DragonHornGenerator} on why splay is too. The pair is symmetric:
 * both sides read the same copy. Its colour is another locus,
 * {@link DragonHornColourGene}, put on by {@code GrownParts.of}.
 */
public final class DragonHornsGene implements Gene {

    public static final String KEY = "horsegenetics.dragon_horns";

    /** Magical band, the first free run after the ram horns; only a code-order slot. */
    public static final int PRIORITY = 239;

    /**
     * Per allele - the unicorn's figure. Recessive, so about one wild horse in four
     * hundred grows a pair and one in ten carries one copy.
     */
    public static final double WILD_DRAGON_FREQUENCY = 0.05;

    public final Allele Drg = new Allele(KEY, 0, "Drg", "Dragon horns (Drg)");
    public final Allele n = new Allele(KEY, 1, "n", "Wild-type (n)");
    private final List<Allele> alleles = List.of(Drg, n);

    private final Expression WILD = Expression.wildType("An ordinary horse.");

    private final Expression CARRIER = Expression.wildType("dragon-horns-carrier", "Dragon horns carrier",
            "One copy, which shows nothing. Two carriers bred together are the reliable way a "
                    + "pair appears.");

    private final Expression DRAGON = Expression.wildType("dragon-horns", "Dragon horns",
            "A pair of horns rooted behind the ears and swept back over the neck. Whether they "
                    + "curve, stand straight or curl, how long and thick they are, how far they "
                    + "sweep back and how wide they splay are all epigenetic and inherited with the "
                    + "allele. Their colour is a gene of its own. Grown at maturity - a foal has none.");

    private final List<Expression> expressions = List.of(WILD, CARRIER, DRAGON);

    private final FounderTable founders = FounderTable.hardyWeinberg(Drg, n, WILD_DRAGON_FREQUENCY);

    /** Position on the {@link DragonHornSize} ladder. */
    public static final String LENGTH = "length";
    /** Thickness on top of the length. */
    public static final String GIRTH = "girth";
    /** Which of {@link DragonHornGenerator}'s forms. */
    public static final String FORM = "form";
    /** How far a swept horn bends, a spike leans, a curl goes round. */
    public static final String SWEEP = "sweep";
    /** How far the pair angle outward. */
    public static final String SPLAY = "splay";

    @Override public String key() { return KEY; }
    @Override public String name() { return "Dragon horns"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return false; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        return switch (pair.count(Drg)) {
            case 2 -> DRAGON;
            case 1 -> CARRIER;
            default -> WILD;
        };
    }

    /** Whether this combination grows the pair. */
    public boolean shows(AllelePair pair) {
        return pair.count(Drg) == 2;
    }

    /** Length wild in the lower two-thirds of the ladder and bred toward great; the rest uniform. */
    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(
                EpiValue.uniform(LENGTH, 0.0, 0.7).clampedTo(0.0, 1.0),
                EpiValue.uniform(GIRTH, 0.80, 1.25).clampedTo(0.6, 1.5),
                EpiValue.category(FORM, DragonHornGenerator.FORMS),
                EpiValue.uniform(SWEEP, 0.0, 1.0).clampedTo(0.0, 1.0),
                EpiValue.uniform(SPLAY, 0.0, 1.0).clampedTo(0.0, 1.0));
    }

    /**
     * The pair this horse grows, right then left, white and dark - or empty unless it
     * is {@code Drg/Drg}. Ask {@code GrownParts.of} for the coloured pair.
     */
    public Optional<List<AttachedPart>> hornsFor(Genotype genotype, Epigenome epigenome) {
        if (!shows(genotype.pair(this))) {
            return Optional.empty();
        }
        EpiValues epi = GeneEpigenetics.forGene(this, genotype, epigenome).expressed();
        int form = epi.category(FORM);
        return Optional.of(List.of(
                AttachedPart.dragonHorn(false, form, epi.get(SWEEP), epi.get(SPLAY), epi.get(LENGTH),
                        epi.get(GIRTH)),
                AttachedPart.dragonHorn(true, form, epi.get(SWEEP), epi.get(SPLAY), epi.get(LENGTH),
                        epi.get(GIRTH))));
    }

    /** What to call the pair - "a long pair". Empty for a horse that has none. */
    public Optional<DragonHornSize> sizeOf(Genotype genotype, Epigenome epigenome) {
        if (!shows(genotype.pair(this))) {
            return Optional.empty();
        }
        return Optional.of(DragonHornSize.of(
                GeneEpigenetics.forGene(this, genotype, epigenome).expressed().get(LENGTH)));
    }
}
