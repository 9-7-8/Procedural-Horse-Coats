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
import com.example.horsegenetics.common.parts.EarFinGenerator;
import com.example.horsegenetics.common.parts.EarFinSize;

import java.util.List;
import java.util.Optional;

/**
 * <b>Ear fins</b> ({@code horsegenetics.ear_fins}) - a <b>magical, recessive</b> gene
 * that grows a fin at the outer base of each ear: thin slabs fanned out from the head
 * and swept back. One of the head parts (treatment: rare recessive), and the only one a
 * foal wears - it is the soft one.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code n/n}</td><td>wild type</td></tr>
 *   <tr><td>{@code Efn/n}</td><td>{@code ear-fins-carrier} - shows nothing</td></tr>
 *   <tr><td>{@code Efn/Efn}</td><td>{@code ear-fins} - the pair</td></tr>
 * </table>
 *
 * <p>Not sex-limited. It does nothing but show: no stat, no hearing, no hitbox. It
 * paints nothing, so every outcome is a wild type and neither coat golden reads it.
 * There is no membrane between the rays and no bone version: a fin is cartilage like an
 * ear, so a skeleton horse never grows one (owner, 2026-10-09).
 *
 * <h2>The numbers on the copy</h2>
 * {@code size} (the {@link EarFinSize} class and a grow within it) is a draw-time
 * scale; {@code form} (blade, fan, frill) and {@code spread} (how wide a fan or a frill
 * opens) are baked into the mesh; {@code rays} is how many a fan or a frill shows, the
 * longest first, out of the form's most. The pair is symmetric: both sides read the
 * same copy. Its colour is another locus, {@link EarFinColourGene}, put on by
 * {@code GrownParts.of}.
 */
public final class EarFinsGene implements Gene {

    public static final String KEY = "horsegenetics.ear_fins";

    /** Magical band, a free run after the crystal growths; only a code-order slot. */
    public static final int PRIORITY = 343;

    /**
     * Per allele - the unicorn's figure. Recessive, so about one wild horse in four
     * hundred grows a pair and one in ten carries one copy.
     */
    public static final double WILD_FIN_FREQUENCY = 0.05;

    public final Allele Efn = new Allele(KEY, 0, "Efn", "Ear fins (Efn)");
    public final Allele n = new Allele(KEY, 1, "n", "Wild-type (n)");
    private final List<Allele> alleles = List.of(Efn, n);

    private final Expression WILD = Expression.wildType("An ordinary horse.");

    private final Expression CARRIER = Expression.wildType("ear-fins-carrier", "Ear fins carrier",
            "One copy, which shows nothing. Two carriers bred together are the reliable way a "
                    + "pair appears.");

    private final Expression FINS = Expression.wildType("ear-fins", "Ear fins",
            "A fin at the outer base of each ear, fanned out from the head and swept back. "
                    + "Whether it is a single blade, a fan of three or a frill of five rays, how "
                    + "big it is, how wide it opens and how many rays show are all epigenetic and "
                    + "inherited with the allele. Their colour is a gene of its own. A foal wears "
                    + "them too, at its own size.");

    private final List<Expression> expressions = List.of(WILD, CARRIER, FINS);

    private final FounderTable founders = FounderTable.hardyWeinberg(Efn, n, WILD_FIN_FREQUENCY);

    /** Position on the {@link EarFinSize} ladder: the longest ray. */
    public static final String SIZE = "size";
    /** Which of {@link EarFinGenerator}'s forms. */
    public static final String FORM = "form";
    /** How wide a fan or a frill opens. */
    public static final String SPREAD = "spread";
    /** How many rays a fan or a frill shows, the longest first; fractional grows the next one in. */
    public static final String RAYS = "rays";

    @Override public String key() { return KEY; }
    @Override public String name() { return "Ear fins"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return false; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        return switch (pair.count(Efn)) {
            case 2 -> FINS;
            case 1 -> CARRIER;
            default -> WILD;
        };
    }

    /** Whether this combination grows the pair. */
    public boolean shows(AllelePair pair) {
        return pair.count(Efn) == 2;
    }

    /**
     * Size wild in the lower two-thirds of the ladder and bred toward grand; rays wild
     * from two up (so a wild fan is nearly always whole), and never fewer than one.
     */
    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(
                EpiValue.uniform(SIZE, 0.0, 0.7).clampedTo(0.0, 1.0),
                EpiValue.category(FORM, EarFinGenerator.FORMS),
                EpiValue.uniform(SPREAD, 0.0, 1.0).clampedTo(0.0, 1.0),
                EpiValue.uniform(RAYS, 2.0, EarFinGenerator.MAX_RAYS)
                        .clampedTo(1.0, EarFinGenerator.MAX_RAYS));
    }

    /**
     * The pair this horse grows, right then left, white and dark - or empty unless it
     * is {@code Efn/Efn}. Ask {@code GrownParts.of} for the coloured pair.
     */
    public Optional<List<AttachedPart>> finsFor(Genotype genotype, Epigenome epigenome) {
        if (!shows(genotype.pair(this))) {
            return Optional.empty();
        }
        EpiValues epi = GeneEpigenetics.forGene(this, genotype, epigenome).expressed();
        int form = epi.category(FORM);
        double spread = epi.get(SPREAD);
        double size = epi.get(SIZE);
        double rays = epi.get(RAYS);
        return Optional.of(List.of(
                AttachedPart.earFin(false, form, spread, size, rays),
                AttachedPart.earFin(true, form, spread, size, rays)));
    }

    /** What to call the fins - "grand fins". Empty for a horse that has none. */
    public Optional<EarFinSize> sizeOf(Genotype genotype, Epigenome epigenome) {
        if (!shows(genotype.pair(this))) {
            return Optional.empty();
        }
        return Optional.of(EarFinSize.of(
                GeneEpigenetics.forGene(this, genotype, epigenome).expressed().get(SIZE)));
    }
}
