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
import com.example.horsegenetics.common.parts.DorsalSpineGenerator;
import com.example.horsegenetics.common.parts.SpineSize;

import java.util.List;
import java.util.Optional;

/**
 * <b>Dorsal spines</b> ({@code horsegenetics.dorsal_spines}) - a <b>magical,
 * dominant</b> gene that grows a row of tapering spines along the top of the back,
 * from the withers to the croup. The first of the body parts (owner, 2026-10-02:
 * dominant, one copy shows), and the simplest: one centred kind, opaque, on the
 * {@code SPINE} anchor.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code n/n}</td><td>wild type</td></tr>
 *   <tr><td>{@code Dsp/n}, {@code Dsp/Dsp}</td><td>{@code dorsal-spines} - the row</td></tr>
 * </table>
 *
 * <p>Not sex-limited, and not on a foal ({@code PartKind.showsOnFoal}): a hard part
 * comes with maturity. A saddled or ridden horse hides the spines under the saddle
 * and keeps the rest ({@code SaddleZone}). It does nothing but show: no stat, no
 * ability, no hitbox. It paints nothing, so every outcome is a wild type and neither
 * coat golden reads it.
 *
 * <h2>The numbers on the copy</h2>
 * {@code length} (the {@link SpineSize} class and a stretch) and {@code girth} are
 * each spine's own scale; {@code count} is how many show from the withers back, out
 * of {@link DorsalSpineGenerator#MAX_SPINES}; {@code form} (uniform, graduated,
 * alternating) and {@code taper} are baked into the mesh. A horse with two copies
 * reads the first-declared one, as every part gene does. Its colour is another locus,
 * {@link DorsalSpineColourGene}, put on by {@code GrownParts.of} - with a bone
 * allele, which is the part's bone version on the skeleton breeds.
 */
public final class DorsalSpinesGene implements Gene {

    public static final String KEY = "horsegenetics.dorsal_spines";

    /** Magical band, the first free run after the dragon horns; only a code-order slot. */
    public static final int PRIORITY = 241;

    /**
     * Founders, in percent, as {@code Dsp/n} - the antlers' small share (about four
     * horses in a thousand carry an antler allele, under three show one). Dominant, so
     * every carrier shows its row: no invisible carriers in the wild.
     */
    public static final double WILD_SPINES_PERCENT = 0.3;

    public final Allele Dsp = new Allele(KEY, 0, "Dsp", "Dorsal spines (Dsp)");
    public final Allele n = new Allele(KEY, 1, "n", "Wild-type (n)");
    private final List<Allele> alleles = List.of(Dsp, n);

    private final Expression WILD = Expression.wildType("An ordinary horse.");

    private final Expression SPINES = Expression.wildType("dorsal-spines", "Dorsal spines",
            "A row of spines along the top of the back, from the withers to the croup. "
                    + "Whether they are all one height, fall away toward the tail or alternate "
                    + "tall and short, how tall and thick they are, how sharply they taper and "
                    + "how many there are are all epigenetic and inherited with the allele. "
                    + "Their colour is a gene of its own. A saddle or a rider hides the ones "
                    + "under the saddle. Grown at maturity - a foal has none.");

    private final List<Expression> expressions = List.of(WILD, SPINES);

    private final FounderTable founders = FounderTable.builder()
            .weight(Dsp, n, WILD_SPINES_PERCENT)
            .weight(n, 100.0 - WILD_SPINES_PERCENT)
            .build();

    /** Position on the {@link SpineSize} ladder: the tallest spine's height. */
    public static final String LENGTH = "length";
    /** Each spine's thickness on top of its height. */
    public static final String GIRTH = "girth";
    /** How many spines show, from the withers back; fractional grows the last one in. */
    public static final String COUNT = "count";
    /** Which of {@link DorsalSpineGenerator}'s forms. */
    public static final String FORM = "form";
    /** How sharply each spine narrows to its point. */
    public static final String TAPER = "taper";

    @Override public String key() { return KEY; }
    @Override public String name() { return "Dorsal spines"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return false; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        return shows(pair) ? SPINES : WILD;
    }

    /** Whether this combination grows the row - one copy is enough. */
    public boolean shows(AllelePair pair) {
        return pair.count(Dsp) > 0;
    }

    /**
     * Height wild in the lower two-thirds of the ladder and bred toward towering; the
     * count wild from about half a row to a full one, and never fewer than one.
     */
    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(
                EpiValue.uniform(LENGTH, 0.0, 0.7).clampedTo(0.0, 1.0),
                EpiValue.uniform(GIRTH, 0.80, 1.25).clampedTo(0.6, 1.5),
                EpiValue.uniform(COUNT, 5.0, DorsalSpineGenerator.MAX_SPINES)
                        .clampedTo(1.0, DorsalSpineGenerator.MAX_SPINES),
                EpiValue.category(FORM, DorsalSpineGenerator.FORMS),
                EpiValue.uniform(TAPER, 0.0, 1.0).clampedTo(0.0, 1.0));
    }

    /**
     * The row this horse grows, white and dark - or empty unless it carries
     * {@code Dsp}. Ask {@code GrownParts.of} for the coloured row.
     */
    public Optional<AttachedPart> spinesFor(Genotype genotype, Epigenome epigenome) {
        if (!shows(genotype.pair(this))) {
            return Optional.empty();
        }
        EpiValues epi = GeneEpigenetics.forGene(this, genotype, epigenome).expressed();
        return Optional.of(AttachedPart.dorsalSpines(epi.category(FORM), epi.get(TAPER), epi.get(LENGTH),
                epi.get(COUNT), epi.get(GIRTH)));
    }

    /** What to call the row - "a tall row". Empty for a horse that has none. */
    public Optional<SpineSize> sizeOf(Genotype genotype, Epigenome epigenome) {
        if (!shows(genotype.pair(this))) {
            return Optional.empty();
        }
        return Optional.of(SpineSize.of(
                GeneEpigenetics.forGene(this, genotype, epigenome).expressed().get(LENGTH)));
    }
}
