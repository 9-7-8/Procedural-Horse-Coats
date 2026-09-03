package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.coat.pattern.PigmentView;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.DominancePattern;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;

import java.util.List;

/**
 * <b>Silver dapple</b> ({@code horsegenetics.silver}) - real-horse
 * {@code PMEL17}. {@code Z} dominant, {@code z} wild-type. Natural,
 * deterministic.
 *
 * <p>Silver dilutes <b>eumelanin only</b> - it walks black pigment toward
 * chocolate and, on the mane and tail, most of the way to flaxen, while leaving
 * pheomelanin (red) untouched. So a <b>black</b> becomes a chocolate-bodied
 * horse with a pale mane, a <b>bay</b> becomes "silver bay" (red body,
 * chocolate points, flaxen mane / tail), and a <b>chestnut carrying it looks
 * unchanged</b> - it has no black for silver to act on. That last point is why
 * silver has to run <i>after</i> agouti in {@code Genes.naturalOrder()}: the
 * black points have to be placed before silver can lighten them.
 *
 * <p>The dappling that gives the gene its name is a follow-up - v1 is the
 * dilution only. See {@code wiki/gene-silver.html}.
 */
public final class SilverGene implements Gene {

    public static final String KEY = "horsegenetics.silver";
    public static final int WILD_SILVER_ALLELE_ODDS = 60;

    /** Body: black cut to a chocolate; red barely touched. */
    private static final float BODY_KEEP_RED = 0.90f;
    private static final float BODY_KEEP_BLACK = 0.46f;
    private static final float BODY_BLACK_TINT = 0.30f;
    /**
     * Mane / tail: silver's <b>flaxen</b> signature. The red is pulled well
     * down too (not just the black) so the sample leaves the dark-red corner
     * and lands light and only faintly warm - a flaxen mane, not a chestnut one.
     */
    private static final float HAIR_KEEP_RED = 0.40f;
    private static final float HAIR_KEEP_BLACK = 0.10f;
    private static final float HAIR_BLACK_TINT = 0.28f;

    public final Allele Z = new Allele(KEY, "Z", "Silver dapple (Z)", true, true);
    public final Allele z = new Allele(KEY, "z", "Wild-type (z)", false, true);
    private final List<Allele> alleles = List.of(Z, z);

    @Override public String key() { return KEY; }
    @Override public int priority() { return 30; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele wildType() { return z; }

    /** Dominant: one {@code Z} gives the full dilution. */
    @Override public DominancePattern dominance() { return DominancePattern.DOMINANT; }

    @Override
    public AllelePair randomPair(Rng rng) {
        return new AllelePair(
                rng.nextInt(WILD_SILVER_ALLELE_ODDS) == 0 ? Z : z,
                rng.nextInt(WILD_SILVER_ALLELE_ODDS) == 0 ? Z : z);
    }

    public boolean isSilver(AllelePair pair) {
        return pair.has(Z);
    }

    @Override
    public boolean isVisible(AllelePair pair, Genotype genotype) {
        return isSilver(pair);
    }

    @Override
    public PigmentField restrict(AllelePair pair, CoatBuildContext ctx, PigmentView coat) {
        if (!isSilver(pair)) {
            return null;
        }
        Skin skin = ctx.skin();
        PigmentField f = coat.mutableCopy();
        HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> {
            boolean hair = part == Part.MANE || part == Part.TAIL
                    || part == Part.LEFT_EAR || part == Part.RIGHT_EAR;
            f.dilute(px, py,
                    hair ? HAIR_KEEP_RED : BODY_KEEP_RED,
                    hair ? HAIR_KEEP_BLACK : BODY_KEEP_BLACK,
                    hair ? HAIR_BLACK_TINT : BODY_BLACK_TINT);
        });
        return f;
    }
}
