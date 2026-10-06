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
import com.example.horsegenetics.common.parts.CrystalGenerator;
import com.example.horsegenetics.common.parts.CrystalSize;

import java.util.List;
import java.util.Optional;

/**
 * <b>Crystal growths</b> ({@code horsegenetics.back_crystals}) - a <b>magical,
 * dominant</b> gene that grows clusters of crystals out of the back along the spine:
 * see-through shafts with solid points, fanning from the midline. The last of the four
 * body parts (treatment: dominant, one copy shows), on the dorsal spines' row.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code n/n}</td><td>wild type</td></tr>
 *   <tr><td>{@code Crg/n}, {@code Crg/Crg}</td><td>{@code back-crystals} - the crystals</td></tr>
 * </table>
 *
 * <p>Not sex-limited, and not on a foal ({@code PartKind.showsOnFoal}): a hard part comes
 * with maturity. A saddled or ridden horse hides the clusters under the saddle and keeps
 * the rest ({@code SaddleZone}). It does nothing but show: no stat, no ability, no
 * hitbox, and no glow - the treatment leaves glow out, and the emissive path
 * ({@code AttachedPart.emissive}, {@code PartKind.glowRegions}) is where one would go.
 * It paints nothing, so every outcome is a wild type and neither coat golden reads it.
 * It is not the crystal antlers locus ({@link AntlerCrystalGene}), which only changes
 * antlers a horse already has.
 *
 * <h2>The numbers on the copy</h2>
 * {@code seed} picks the arrangement - where every crystal of every cluster stands, and
 * how many each cluster has - and is never nudged by drift, only rarely re-rolled whole,
 * so a horse's growths are its own and its line's. {@code size} (the {@link CrystalSize}
 * class and a stretch) grows each cluster; {@code count} is how many clusters show from
 * the withers back, out of {@link CrystalGenerator#MAX_CLUSTERS}; {@code spread} is how
 * far the outer crystals fan from upright, baked into the mesh. A horse with two copies
 * reads the first-declared one, as every part gene does. Its colour is another locus,
 * {@link BackCrystalColourGene}, put on by {@code GrownParts.of}: the base colour is the
 * see-through shafts and the tip colour the points.
 */
public final class BackCrystalsGene implements Gene {

    public static final String KEY = "horsegenetics.back_crystals";

    /**
     * Magical band. The part loci fill 227-247 and the tusks took 334-335, so this and
     * its colour take the next free run of two; a {@code MAGIC_PARTS} override files them
     * with the parts whatever the band. Only a code-order slot.
     */
    public static final int PRIORITY = 336;

    /**
     * Founders, in percent, as {@code Crg/n} - the other dominant body parts' share, near
     * the antlers' small one (body-parts treatment). Dominant, so every carrier shows its
     * crystals: no invisible carriers in the wild.
     */
    public static final double WILD_CRYSTALS_PERCENT = 0.3;

    public final Allele Crg = new Allele(KEY, 0, "Crg", "Crystal growths (Crg)");
    public final Allele n = new Allele(KEY, 1, "n", "Wild-type (n)");
    private final List<Allele> alleles = List.of(Crg, n);

    private final Expression WILD = Expression.wildType("An ordinary horse.");

    private final Expression CRYSTALS = Expression.wildType("back-crystals", "Crystal growths",
            "Clusters of crystals breaking out of the back along the spine, each of three to "
                    + "seven see-through crystals with solid points, fanning out from the midline. "
                    + "Where each crystal stands is the horse's own and is inherited whole; how "
                    + "big they are, how many clusters there are and how far they fan are "
                    + "epigenetic and inherited with the allele. Their colour is a gene of its "
                    + "own. A saddle or a rider hides the clusters under the saddle. Grown at "
                    + "maturity - a foal has none.");

    private final List<Expression> expressions = List.of(WILD, CRYSTALS);

    private final FounderTable founders = FounderTable.builder()
            .weight(Crg, n, WILD_CRYSTALS_PERCENT)
            .weight(n, 100.0 - WILD_CRYSTALS_PERCENT)
            .build();

    /** Which arrangement - the seed the variant is cut from. */
    public static final String SEED = "seed";
    /** Position on the {@link CrystalSize} ladder: the tallest crystal of a cluster. */
    public static final String SIZE = "size";
    /** How many clusters show, from the withers back; fractional grows the last one in. */
    public static final String COUNT = "count";
    /** How far the outer crystals of a cluster fan from upright - bucketed into the mesh. */
    public static final String SPREAD = "spread";

    @Override public String key() { return KEY; }
    @Override public String name() { return "Crystal growths"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return false; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        return shows(pair) ? CRYSTALS : WILD;
    }

    /** Whether this combination grows the crystals - one copy is enough. */
    public boolean shows(AllelePair pair) {
        return pair.count(Crg) > 0;
    }

    /**
     * Size wild in the lower two-thirds of the ladder and bred toward great; the count
     * wild from two clusters to a full row, never fewer than one; the spread across the
     * whole of its range.
     */
    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(
                EpiValue.seed(SEED),
                EpiValue.uniform(SIZE, 0.0, 0.7).clampedTo(0.0, 1.0),
                EpiValue.uniform(COUNT, 2.0, CrystalGenerator.MAX_CLUSTERS)
                        .clampedTo(1.0, CrystalGenerator.MAX_CLUSTERS),
                EpiValue.uniform(SPREAD, 0.0, 1.0).clampedTo(0.0, 1.0));
    }

    /**
     * The crystals this horse grows, white and dark - or empty unless it carries
     * {@code Crg}. Ask {@code GrownParts.of} for the coloured ones.
     */
    public Optional<AttachedPart> crystalsFor(Genotype genotype, Epigenome epigenome) {
        if (!shows(genotype.pair(this))) {
            return Optional.empty();
        }
        EpiValues epi = GeneEpigenetics.forGene(this, genotype, epigenome).expressed();
        return Optional.of(AttachedPart.crystals(variantOf(epi.seed(SEED)), epi.get(SPREAD),
                epi.get(SIZE), epi.get(COUNT)));
    }

    /** What to call the crystals - "great crystals". Empty for a horse that has none. */
    public Optional<CrystalSize> sizeOf(Genotype genotype, Epigenome epigenome) {
        if (!shows(genotype.pair(this))) {
            return Optional.empty();
        }
        return Optional.of(CrystalSize.of(
                GeneEpigenetics.forGene(this, genotype, epigenome).expressed().get(SIZE)));
    }

    /** The seed's arrangement. A {@code long} genome, {@link CrystalGenerator#VARIANTS} cached shapes. */
    static int variantOf(long seed) {
        return (int) Math.floorMod(mix(seed), (long) CrystalGenerator.VARIANTS);
    }

    /** A 64-bit finaliser, so neighbouring seeds land on unrelated arrangements. */
    private static long mix(long z) {
        z = (z ^ (z >>> 33)) * 0xFF51AFD7ED558CCDL;
        z = (z ^ (z >>> 33)) * 0xC4CEB9FE1A85EC53L;
        return z ^ (z >>> 33);
    }
}
