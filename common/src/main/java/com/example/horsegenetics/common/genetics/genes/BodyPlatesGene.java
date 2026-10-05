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
import com.example.horsegenetics.common.parts.PlateGenerator;
import com.example.horsegenetics.common.parts.PlateSize;

import java.util.List;
import java.util.Optional;

/**
 * <b>Body plates</b> ({@code horsegenetics.body_plates}) - a <b>magical, dominant</b>
 * gene that grows overlapping bone plates over the shoulders and the hips, stepped
 * down the flank like armour. The third of the body parts (treatment: dominant, one
 * copy shows), and the first part with a pair of anchors off the head: one mesh per
 * side on {@code BODY_RIGHT} and {@code BODY_LEFT}, both clusters in it.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code n/n}</td><td>wild type</td></tr>
 *   <tr><td>{@code Plt/n}, {@code Plt/Plt}</td><td>{@code body-plates} - the plates</td></tr>
 * </table>
 *
 * <p>Not sex-limited, and not on a foal ({@code PartKind.showsOnFoal}): a hard part
 * comes with maturity. No saddle zone: the clusters sit in front of and behind the
 * saddle, and how a saddle flap or barding meets them is the treatment's open look,
 * to be decided once seen. It does nothing but show: no stat, no armour value, no
 * hitbox. It paints nothing, so every outcome is a wild type and neither coat golden
 * reads it.
 *
 * <h2>The numbers on the copy</h2>
 * {@code size} (the {@link PlateSize} class and a stretch) is each slab's own scale;
 * {@code count} is how many slabs each cluster shows, top down, out of
 * {@link PlateGenerator#MAX_PLATES}; {@code form} (smooth, ridged, spiked),
 * {@code overlap} and {@code spikiness} are baked into the mesh. A horse with two
 * copies reads the first-declared one, as every part gene does. Its colour is another
 * locus, {@link BodyPlateColourGene}, put on by {@code GrownParts.of} - with a bone
 * allele, which is the part's bone version on the skeleton breeds.
 */
public final class BodyPlatesGene implements Gene {

    public static final String KEY = "horsegenetics.body_plates";

    /** Magical band, the first free run after the back sail; only a code-order slot. */
    public static final int PRIORITY = 245;

    /**
     * Founders, in percent, as {@code Plt/n} - the dorsal spines' share. Dominant, so
     * every carrier shows its plates: no invisible carriers in the wild.
     */
    public static final double WILD_PLATES_PERCENT = 0.3;

    public final Allele Plt = new Allele(KEY, 0, "Plt", "Body plates (Plt)");
    public final Allele n = new Allele(KEY, 1, "n", "Wild-type (n)");
    private final List<Allele> alleles = List.of(Plt, n);

    private final Expression WILD = Expression.wildType("An ordinary horse.");

    private final Expression PLATES = Expression.wildType("body-plates", "Body plates",
            "Overlapping plates of bone over the shoulders and the hips, stepped down the "
                    + "flank like armour. Whether they are smooth, ridged or spiked, how big "
                    + "they are, how many each cluster has, how far they overlap and how far "
                    + "they flare are all epigenetic and inherited with the allele. Their "
                    + "colour is a gene of its own. Grown at maturity - a foal has none.");

    private final List<Expression> expressions = List.of(WILD, PLATES);

    private final FounderTable founders = FounderTable.builder()
            .weight(Plt, n, WILD_PLATES_PERCENT)
            .weight(n, 100.0 - WILD_PLATES_PERCENT)
            .build();

    /** Position on the {@link PlateSize} ladder: one slab's height down the flank. */
    public static final String SIZE = "size";
    /** How many slabs each cluster shows, from the top down; fractional grows the next one in. */
    public static final String COUNT = "count";
    /** Which of {@link PlateGenerator}'s forms. */
    public static final String FORM = "form";
    /** How far each slab laps the one below it. */
    public static final String OVERLAP = "overlap";
    /** How far the slabs flare from the body, and how big a rib or spike stands. */
    public static final String SPIKINESS = "spikiness";

    @Override public String key() { return KEY; }
    @Override public String name() { return "Body plates"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return false; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        return shows(pair) ? PLATES : WILD;
    }

    /** Whether this combination grows the plates - one copy is enough. */
    public boolean shows(AllelePair pair) {
        return pair.count(Plt) > 0;
    }

    /**
     * Size wild in the lower two-thirds of the ladder and bred toward massive; the
     * count wild from three to five a cluster (the treatment's range), and never fewer
     * than one.
     */
    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(
                EpiValue.uniform(SIZE, 0.0, 0.7).clampedTo(0.0, 1.0),
                EpiValue.uniform(COUNT, 3.0, PlateGenerator.MAX_PLATES)
                        .clampedTo(1.0, PlateGenerator.MAX_PLATES),
                EpiValue.category(FORM, PlateGenerator.FORMS),
                EpiValue.uniform(OVERLAP, 0.0, 1.0).clampedTo(0.0, 1.0),
                EpiValue.uniform(SPIKINESS, 0.0, 1.0).clampedTo(0.0, 1.0));
    }

    /**
     * Both sides' plates, right then left, white and dark - or empty unless the horse
     * carries {@code Plt}. The sides always match. Ask {@code GrownParts.of} for the
     * coloured pair.
     */
    public Optional<List<AttachedPart>> platesFor(Genotype genotype, Epigenome epigenome) {
        if (!shows(genotype.pair(this))) {
            return Optional.empty();
        }
        EpiValues epi = GeneEpigenetics.forGene(this, genotype, epigenome).expressed();
        int form = epi.category(FORM);
        double overlap = epi.get(OVERLAP);
        double spikiness = epi.get(SPIKINESS);
        double size = epi.get(SIZE);
        double count = epi.get(COUNT);
        return Optional.of(List.of(
                AttachedPart.plates(false, form, overlap, spikiness, size, count),
                AttachedPart.plates(true, form, overlap, spikiness, size, count)));
    }

    /** What to call the plates - "broad plates". Empty for a horse that has none. */
    public Optional<PlateSize> sizeOf(Genotype genotype, Epigenome epigenome) {
        if (!shows(genotype.pair(this))) {
            return Optional.empty();
        }
        return Optional.of(PlateSize.of(
                GeneEpigenetics.forGene(this, genotype, epigenome).expressed().get(SIZE)));
    }
}
