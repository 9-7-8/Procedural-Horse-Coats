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
import com.example.horsegenetics.common.parts.BrowRidgeGenerator;
import com.example.horsegenetics.common.parts.BrowRidgeSize;

import java.util.List;
import java.util.Optional;

/**
 * <b>Brow ridge</b> ({@code horsegenetics.brow_ridge}) - a <b>magical, dominant</b> gene
 * that grows one low wide ridge of bone across the front of the skull, above and in
 * front of the eyes. One of the head parts (treatment: dominant), and lower on the face
 * than the unicorn horn's root, so a horn still stands behind it.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code n/n}</td><td>wild type</td></tr>
 *   <tr><td>{@code Brw/n}, {@code Brw/Brw}</td><td>{@code brow-ridge} - the ridge</td></tr>
 * </table>
 *
 * <p>Not sex-limited, and not on a foal ({@code PartKind.showsOnFoal}): a hard part
 * comes with maturity. It does nothing but show: no stat, no armour value, no hitbox.
 * It paints nothing, so every outcome is a wild type and neither coat golden reads it.
 *
 * <h2>The numbers on the copy</h2>
 * {@code height} (the {@link BrowRidgeSize} class and a stretch) and {@code width}
 * (across the head) are a draw-time scale; {@code form} (plain, notched, spined) and
 * {@code bump} (how tall the bumps or spines stand) are baked into the mesh. A horse
 * with two copies reads the first-declared one, as every part gene does. Its colour is
 * another locus, {@link BrowRidgeColourGene}, put on by {@code GrownParts.of} - with a
 * bone allele, which is the part's bone version on the skeleton breeds.
 */
public final class BrowRidgeGene implements Gene {

    public static final String KEY = "horsegenetics.brow_ridge";

    /** Magical band, beside the other head parts; only a code-order slot. */
    public static final int PRIORITY = 347;

    /**
     * Founders, in percent, as {@code Brw/n} - the body plates' share. Dominant, so
     * every carrier shows its ridge: no invisible carriers in the wild.
     */
    public static final double WILD_RIDGE_PERCENT = 0.3;

    public final Allele Brw = new Allele(KEY, 0, "Brw", "Brow ridge (Brw)");
    public final Allele n = new Allele(KEY, 1, "n", "Wild-type (n)");
    private final List<Allele> alleles = List.of(Brw, n);

    private final Expression WILD = Expression.wildType("An ordinary horse.");

    private final Expression RIDGE = Expression.wildType("brow-ridge", "Brow ridge",
            "A low wide ridge of bone across the brow, above and in front of the eyes. "
                    + "Whether it is plain, notched into three bumps or set with three short "
                    + "spines, how tall and wide it is and how tall its bumps stand are all "
                    + "epigenetic and inherited with the allele. Its colour is a gene of its "
                    + "own. Grown at maturity - a foal has none.");

    private final List<Expression> expressions = List.of(WILD, RIDGE);

    private final FounderTable founders = FounderTable.builder()
            .weight(Brw, n, WILD_RIDGE_PERCENT)
            .weight(n, 100.0 - WILD_RIDGE_PERCENT)
            .build();

    /** Position on the {@link BrowRidgeSize} ladder: how tall the ridge stands. */
    public static final String HEIGHT = "height";
    /** Width across the head; {@code 1} is {@link BrowRidgeGenerator#WIDTH}. */
    public static final String WIDTH = "width";
    /** Which of {@link BrowRidgeGenerator}'s forms. */
    public static final String FORM = "form";
    /** How tall a notched ridge's bumps or a spined one's spines stand. */
    public static final String BUMP = "bump";

    @Override public String key() { return KEY; }
    @Override public String name() { return "Brow ridge"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return false; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        return shows(pair) ? RIDGE : WILD;
    }

    /** Whether this combination grows the ridge - one copy is enough. */
    public boolean shows(AllelePair pair) {
        return pair.count(Brw) > 0;
    }

    /**
     * Height wild in the lower two-thirds of the ladder and bred toward towering; width
     * wild inside the skull's own six units and clamped at its edges.
     */
    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(
                EpiValue.uniform(HEIGHT, 0.0, 0.7).clampedTo(0.0, 1.0),
                EpiValue.uniform(WIDTH, 0.80, 1.10).clampedTo(0.6, 1.2),
                EpiValue.category(FORM, BrowRidgeGenerator.FORMS),
                EpiValue.uniform(BUMP, 0.0, 1.0).clampedTo(0.0, 1.0));
    }

    /**
     * The ridge this horse grows, white and dark - or empty unless it carries
     * {@code Brw}. Ask {@code GrownParts.of} for the coloured one.
     */
    public Optional<AttachedPart> ridgeFor(Genotype genotype, Epigenome epigenome) {
        if (!shows(genotype.pair(this))) {
            return Optional.empty();
        }
        EpiValues epi = GeneEpigenetics.forGene(this, genotype, epigenome).expressed();
        return Optional.of(AttachedPart.browRidge(epi.category(FORM), epi.get(BUMP),
                epi.get(HEIGHT), epi.get(WIDTH)));
    }

    /** What to call the ridge - "a heavy ridge". Empty for a horse that has none. */
    public Optional<BrowRidgeSize> sizeOf(Genotype genotype, Epigenome epigenome) {
        if (!shows(genotype.pair(this))) {
            return Optional.empty();
        }
        return Optional.of(BrowRidgeSize.of(
                GeneEpigenetics.forGene(this, genotype, epigenome).expressed().get(HEIGHT)));
    }
}
