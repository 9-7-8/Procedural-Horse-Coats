package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.coat.pattern.BayCoat;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.BayShade;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;

import java.util.List;

/**
 * <b>Agouti</b> ({@code horsegenetics.agouti}) - where black pigment is allowed
 * to sit.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code a/a}</td><td>wild type - black is left everywhere, so the horse is whatever extension made it</td></tr>
 *   <tr><td>{@code A/a}, {@code A/A}</td><td>a <b>bay</b>, of one of four shades - black restricted toward the points</td></tr>
 * </table>
 *
 * <h2>Two alleles, four bays</h2>
 * Agouti says <i>whether</i> black is restricted; it does not say <i>how far</i>.
 * That is the {@link ShadeGene} locus plus <i>MC1R</i> and <i>ASIP</i> dosage,
 * summed by {@link BayShade} into one score, and it is what separates a bright
 * <b>blood bay</b> from an ordinary <b>bay</b>, a <b>liver bay</b> and a
 * <b>seal brown</b>. So this gene declares four bay expressions and
 * {@link #expressionIn} picks between them by reading the rest of the genotype
 * - the same cross-locus mechanism the leopard complex uses, and the reason
 * {@link #coatDependsOn()} names the shade locus so a bay's texture key folds
 * those alleles in.
 *
 * <p><b>None of the four is an allele</b>, here or in a real horse. Blood, bay,
 * liver and seal all test as the same {@code E_A_} foundation; they are bands
 * on a continuum, and the horse's own epigenetic roll moves it within its band.
 * The {@code A+}/{@code A}/{@code At}/{@code a} series often quoted for seal
 * brown is not validated in horses and is deliberately absent - see
 * {@code wiki/gene-shade.html}.
 *
 * <p>Agouti only moves black pigment, so on a chestnut horse it does nothing at
 * all - {@link #expressionIn} reports the wild type there rather than painting
 * black points onto a horse that has no black.
 *
 * <p>Natural. Founders: 25 / 50 / 25.
 */
public final class AgoutiGene implements Gene {

    public static final String KEY = "horsegenetics.agouti";

    public final Allele A = new Allele(KEY, 0, "A", "Agouti / bay (A)");
    public final Allele a = new Allele(KEY, 1, "a", "Non-agouti (a)");
    private final List<Allele> alleles = List.of(A, a);

    private final Expression WILD = Expression.wildType(
            "Black pigment is not restricted, so it covers the whole horse - a plain black, "
                    + "or a chestnut if extension already removed it.");

    private final Expression BLOOD_BAY = bay(BayShade.Shade.BLOOD,
            "The red end of the bay range: a saturated copper-red body with crisp black mane, "
                    + "tail, ears and hooves, and black barely climbing the legs.");
    private final Expression BAY = bay(BayShade.Shade.BAY,
            "The ordinary bay: a red-brown body with black mane, tail, ears and hooves, and "
                    + "black climbing the legs and a little of the face.");
    private final Expression LIVER_BAY = bay(BayShade.Shade.LIVER,
            "A dark mahogany or chocolate body - still visibly red rather than black - with the "
                    + "black points running well up the legs and over the muzzle.");
    private final Expression SEAL_BROWN = bay(BayShade.Shade.SEAL,
            "Near-black at a distance, and given away by the soft tan points a seal brown keeps: "
                    + "a mealy muzzle, a paler ring over the eye, and light patches at the elbow "
                    + "and the flank.");

    private final List<Expression> expressions =
            List.of(WILD, BLOOD_BAY, BAY, LIVER_BAY, SEAL_BROWN);

    /**
     * One bay outcome. All four paint through the same {@link BayCoat} call and
     * differ only in the shade score it reads back out of the genotype, which is
     * the point: they are bands on one continuum, not four painters.
     */
    private static Expression bay(BayShade.Shade shade, String description) {
        return Expression.of(shade.id(), shade.label())
                .describe(description)
                .varies()
                .restrict((ctx, coat) -> {
                    PigmentField f = coat.mutableCopy();
                    BayCoat.apply(ctx, f, ctx.epigeneticsFor(KEY));
                    return f;
                });
    }

    private final FounderTable founders = FounderTable.builder()
            .weight(A, A, 25.0)
            .weight(A, a, 50.0)
            .weight(a, a, 25.0)
            .build();

    @Override public String key() { return KEY; }
    @Override public String name() { return "Agouti"; }
    @Override public int priority() { return 20; }
    @Override public boolean hasGeneCarrot() { return false; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return a; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    /** The shade locus decides which bay this is, so a bay's texture key needs its alleles. */
    @Override
    public List<String> coatDependsOn() {
        return List.of(ShadeGene.KEY);
    }

    /**
     * <b>Coarse by its own admission.</b> A pair on its own cannot say which bay
     * this is - that needs the shade locus and both dosage terms - so this
     * answers with the middle of the range and leaves the real one of four to
     * {@link #expressionIn}. Exactly the position {@code LeopardGene} is in.
     */
    @Override
    public Expression expressionOf(AllelePair pair) {
        return pair.has(A) ? BAY : WILD;
    }

    /** Nothing to restrict on a horse that makes no black pigment. */
    @Override
    public Expression expressionIn(AllelePair pair, Genotype genotype) {
        if (!pair.has(A) || !genotype.hasBlackPigment()) {
            return WILD;
        }
        return switch (BayShade.shadeOf(genotype)) {
            case BLOOD -> BLOOD_BAY;
            case BAY -> BAY;
            case LIVER -> LIVER_BAY;
            case SEAL -> SEAL_BROWN;
        };
    }

    public boolean isBay(AllelePair pair) {
        return pair.has(A);
    }
}
