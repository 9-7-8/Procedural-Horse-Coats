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
import com.example.horsegenetics.common.parts.CheekSpikeGenerator;
import com.example.horsegenetics.common.parts.CheekSpikeSize;

import java.util.List;
import java.util.Optional;

/**
 * <b>Cheek spikes</b> ({@code horsegenetics.cheek_spikes}) - a <b>magical, dominant</b>
 * gene that grows a short row of bone spikes along each cheek, pointing out from the
 * head and a little back. One of the head parts (treatment: dominant, one copy shows).
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code n/n}</td><td>wild type</td></tr>
 *   <tr><td>{@code Chk/n}, {@code Chk/Chk}</td><td>{@code cheek-spikes} - the spikes</td></tr>
 * </table>
 *
 * <p>Not sex-limited, and not on a foal ({@code PartKind.showsOnFoal}): a hard part
 * comes with maturity. It does nothing but show: no stat, no damage, no hitbox. It
 * paints nothing, so every outcome is a wild type and neither coat golden reads it.
 *
 * <h2>The numbers on the copy</h2>
 * {@code length} (the {@link CheekSpikeSize} class and a stretch) and {@code girth} are
 * each spike's own scale; {@code count} is how many each cheek shows, from the back
 * forward, out of {@link CheekSpikeGenerator#MAX_SPIKES}. There is one style. A horse
 * with two copies reads the first-declared one, as every part gene does. Its colour is
 * another locus, {@link CheekSpikeColourGene}, put on by {@code GrownParts.of} - with a
 * bone allele, which is the part's bone version on the skeleton breeds.
 */
public final class CheekSpikesGene implements Gene {

    public static final String KEY = "horsegenetics.cheek_spikes";

    /** Magical band, beside the other head parts; only a code-order slot. */
    public static final int PRIORITY = 345;

    /**
     * Founders, in percent, as {@code Chk/n} - the body plates' share. Dominant, so
     * every carrier shows its spikes: no invisible carriers in the wild.
     */
    public static final double WILD_SPIKES_PERCENT = 0.3;

    public final Allele Chk = new Allele(KEY, 0, "Chk", "Cheek spikes (Chk)");
    public final Allele n = new Allele(KEY, 1, "n", "Wild-type (n)");
    private final List<Allele> alleles = List.of(Chk, n);

    private final Expression WILD = Expression.wildType("An ordinary horse.");

    private final Expression SPIKES = Expression.wildType("cheek-spikes", "Cheek spikes",
            "A short row of bone spikes along each cheek, pointing out from the head and a "
                    + "little back, the longest at the rear. How long and thick they are and how "
                    + "many each cheek has are epigenetic and inherited with the allele. Their "
                    + "colour is a gene of its own. Grown at maturity - a foal has none.");

    private final List<Expression> expressions = List.of(WILD, SPIKES);

    private final FounderTable founders = FounderTable.builder()
            .weight(Chk, n, WILD_SPIKES_PERCENT)
            .weight(n, 100.0 - WILD_SPIKES_PERCENT)
            .build();

    /** Position on the {@link CheekSpikeSize} ladder: the longest spike. */
    public static final String LENGTH = "length";
    /** Thickness on top of the length. */
    public static final String GIRTH = "girth";
    /** How many spikes each cheek shows, from the back forward; fractional grows the next one in. */
    public static final String COUNT = "count";

    @Override public String key() { return KEY; }
    @Override public String name() { return "Cheek spikes"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return false; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        return shows(pair) ? SPIKES : WILD;
    }

    /** Whether this combination grows the spikes - one copy is enough. */
    public boolean shows(AllelePair pair) {
        return pair.count(Chk) > 0;
    }

    /**
     * Length wild in the lower two-thirds of the ladder and bred toward long; the count
     * wild from two to four a cheek (the treatment's range), and never fewer than one.
     */
    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(
                EpiValue.uniform(LENGTH, 0.0, 0.7).clampedTo(0.0, 1.0),
                EpiValue.uniform(GIRTH, 0.80, 1.25).clampedTo(0.6, 1.5),
                EpiValue.uniform(COUNT, 2.0, CheekSpikeGenerator.MAX_SPIKES)
                        .clampedTo(1.0, CheekSpikeGenerator.MAX_SPIKES));
    }

    /**
     * Both cheeks' spikes, right then left, white and dark - or empty unless the horse
     * carries {@code Chk}. The sides always match. Ask {@code GrownParts.of} for the
     * coloured pair.
     */
    public Optional<List<AttachedPart>> spikesFor(Genotype genotype, Epigenome epigenome) {
        if (!shows(genotype.pair(this))) {
            return Optional.empty();
        }
        EpiValues epi = GeneEpigenetics.forGene(this, genotype, epigenome).expressed();
        double length = epi.get(LENGTH);
        double girth = epi.get(GIRTH);
        double count = epi.get(COUNT);
        return Optional.of(List.of(
                AttachedPart.cheekSpikes(false, length, girth, count),
                AttachedPart.cheekSpikes(true, length, girth, count)));
    }

    /** What to call the spikes - "long spikes". Empty for a horse that has none. */
    public Optional<CheekSpikeSize> sizeOf(Genotype genotype, Epigenome epigenome) {
        if (!shows(genotype.pair(this))) {
            return Optional.empty();
        }
        return Optional.of(CheekSpikeSize.of(
                GeneEpigenetics.forGene(this, genotype, epigenome).expressed().get(LENGTH)));
    }
}
