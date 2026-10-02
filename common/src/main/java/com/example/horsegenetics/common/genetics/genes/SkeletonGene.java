package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.AbilityContribution;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.GeneEpigenetics;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;
import com.example.horsegenetics.common.genetics.epi.EpiValues;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;

import java.util.List;

/**
 * <b>Skeleton</b> ({@code horsegenetics.skeleton}) - two copies and the horse is
 * its own bones: vanilla's skeleton horse sheet cuts away everything that is not
 * bone, its colour shows through the horse's own coat, and its shading is laid
 * back over the top (undead treatment D38, Appendix F). Every colour and marking
 * still comes from the horse's other genes - a bleached skeleton is white bone
 * because it is a white horse, a weathered one yellowed because it is a red dun.
 *
 * <p>A skeleton has no lungs to fill, so it never drowns: the gene grants the
 * absolute {@code underwater_breathing} flag to the horse itself (owner,
 * 2026-10-02, over the graded water-breathing locus). Its rider is not covered.
 *
 * <h2>Two numbers on each copy</h2>
 * {@link #COAT} is how much of the finished coat covers the bone colour, and
 * {@link #SHADE} how hard the bone's shading is laid back over it. Both average
 * across the two copies, so a line's bones drift as anything else does, and a
 * breed or strain may band them. The starting values are the treatment's
 * (0.6 and 0.85), set by eye and not yet seen in a game.
 */
public final class SkeletonGene extends AbstractUndeathGene implements AbilityContribution {

    public static final String KEY = "horsegenetics.skeleton";

    /** In the magical coat band, beside nothing that paints - the sheet pass runs out of band. */
    public static final int PRIORITY = 191;

    /** How much of the coat covers the bone, 0..1. */
    public static final String COAT = "coat_opacity";

    /** How hard the bone's own shading is laid back over, 0..1. */
    public static final String SHADE = "shade_strength";

    private static final List<GeneAbility> ABILITIES = List.of(
            new GeneAbility.Traversal("underwater_breathing", "self", GeneAbility.Condition.ALWAYS, 1));

    public SkeletonGene() {
        super(KEY, "Skeleton", PRIORITY, "Skel", "Skeleton", "skeleton",
                "One skeleton copy. The horse is alive in every way that shows; the copy is "
                        + "there for its foals.",
                "Skeleton",
                "Two skeleton copies. The horse is its own bones - everything but the skeleton is "
                        + "cut away, the bone shows through its coat, and whatever colour it carries "
                        + "is the colour of its bones. It never drowns. It counts as undead.");
    }

    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(
                EpiValue.uniform(COAT, 0.55, 0.65).clampedTo(0.0, 1.0),
                EpiValue.uniform(SHADE, 0.80, 0.90).clampedTo(0.0, 1.0));
    }

    @Override
    protected SheetUse use(String sheet, Genotype genotype, Epigenome epigenome) {
        EpiValues epi = GeneEpigenetics.forGene(this, genotype, epigenome).expressed();
        return new SheetUse(sheet, Pass.SHAPE, clamp01(epi.get(COAT)), clamp01(epi.get(SHADE)));
    }

    @Override
    public List<GeneAbility> abilitiesFor(AllelePair pair, Genotype genotype) {
        return expresses(pair) ? ABILITIES : List.of();
    }

    private static double clamp01(double x) {
        return Double.isNaN(x) ? 0.6 : (x < 0.0 ? 0.0 : (x > 1.0 ? 1.0 : x));
    }
}
