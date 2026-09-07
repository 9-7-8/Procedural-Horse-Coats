package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.pattern.BlaschkoStripes;
import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.coat.pattern.PigmentView;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Inheritance;
import com.example.horsegenetics.common.horse.Sex;

import java.util.List;

/**
 * <b>Brindle</b> ({@code horsegenetics.brindle}, {@code MBTPS2}) - the model's
 * first and so far only <b>{@code X}-linked</b> gene, and the case the whole
 * sex-linked scaffolding was built for.
 *
 * <table>
 *   <tr><th>combination</th><th>who</th><th>outcome</th></tr>
 *   <tr><td>{@code n/n}</td><td>mare</td><td>wild type</td></tr>
 *   <tr><td>{@code Brn/n}</td><td>mare</td><td>{@code brindle-carrier} - a wild type; nothing shows</td></tr>
 *   <tr><td>{@code Brn/Brn}</td><td>mare</td><td>{@code brindle}</td></tr>
 *   <tr><td>{@code n/Y}</td><td>stallion</td><td>wild type</td></tr>
 *   <tr><td>{@code Brn/Y}</td><td>stallion</td><td>{@code brindle} - shown as {@code X-Brn}</td></tr>
 * </table>
 *
 * <h2>Why this gene is worth the scaffolding</h2>
 * A stallion has one {@code X}, so he has one copy of this locus and <b>can
 * never be a carrier</b>: if he has the allele, he wears it. A mare has two, so
 * she needs both to show it and is a carrier the rest of the time. That
 * asymmetry has consequences a player can watch happen and cannot get from any
 * autosomal gene:
 * <ul>
 *   <li>a brindle stallion throws <b>no brindle sons at all</b> - he gives every
 *       son his {@code Y} - and <b>every daughter a carrier copy</b>;</li>
 *   <li>so brindle skips a generation on the male line and reappears through the
 *       mares, which is the classic pedigree read;</li>
 *   <li>a brindle <b>mare</b> is the breeding project: she needs a brindle sire
 *       and a dam carrying it, and once you have her every son of hers is
 *       brindle;</li>
 *   <li>and at the wild frequency ({@value #WILD_BRN_FREQUENCY} per {@code X})
 *       that is {@code 2%} of wild stallions against {@code 0.04%} of wild mares
 *       - a fifty-fold difference that is visible in a herd.</li>
 * </ul>
 *
 * <h2>What it paints</h2>
 * Irregular <b>white streaks</b> - not a dilution of the horse's own colour, and
 * not stripes of a second one. A brindle streak goes to white hair and stops
 * there, whatever the horse underneath it is, so the pattern reads the same on a
 * black, a bay, a palomino or a grey. That keeps it inside phase 1's
 * restrict-only contract with room to spare: white is what is left when
 * <i>all</i> the pigment is taken away, which is the one thing phase 1 is always
 * allowed to do.
 *
 * <p>The streaks are {@link BlaschkoStripes}, which is brindle's own field and
 * deliberately <b>not</b> the zebra one. The two patterns share nothing but the
 * word "stripe": a zebra's bands are crisp, matched side to side and organised
 * round a body map, and brindle's are soft-edged, broken, unequal in width, and
 * <b>different on the two sides of the same horse</b> - because they are a
 * record of which X chromosome each patch of skin happened to silence. Sharing
 * one field between them is what made both of them wrong.
 *
 * <p>Natural, <b>non-deterministic</b>. See {@code wiki/gene-brindle.html}.
 */
public final class BrindleGene implements Gene {

    public static final String KEY = "horsegenetics.brindle";
    public static final int PRIORITY = 36; // with the other dilutions: silver 30, mushroom 32, dun 34

    /**
     * Per {@code X}. Because a stallion has one {@code X} this <i>is</i> the
     * share of wild stallions that are brindle; a wild mare needs two and is
     * therefore {@code p}&sup2; = 0.04%.
     */
    public static final double WILD_BRN_FREQUENCY = 0.02;

    // Declaration order is slot order (AllelePair canonicalises on it), so the
    // reserved Y goes LAST - a stallion reads Brn/Y with his real allele first.
    // Genes.register refuses this gene if it is anywhere else.
    public final Allele Brn = new Allele(KEY, 0, "Brn", "Brindle (Brn)");
    public final Allele n = new Allele(KEY, 1, "n", "Wild-type (n)");
    public final Allele Y = new Allele(KEY, 2, "Y", "No copy (Y chromosome)");

    private final List<Allele> alleles = List.of(Brn, n, Y);

    private final Expression WILD = Expression.wildType("No brindle striping.");

    private final Expression CARRIER = Expression.wildType("brindle-carrier", "Brindle carrier",
            "A mare with one brindle copy on one of her two X chromosomes. Nothing shows, but half "
                    + "her sons are brindle - which is how the pattern reappears a generation after "
                    + "a brindle stallion.");

    private final Expression BRINDLE = Expression.of("brindle", "Brindle")
            .describe("Irregular white streaks, soft-edged and broken, running down from the topline "
                    + "over the barrel, quarters and neck and turning crosswise on the upper legs. "
                    + "They are white on any base colour, and they do not match from one side of the "
                    + "horse to the other. Every stallion carrying the allele shows it; a mare needs "
                    + "two copies.")
            .varies()
            .restrict(this::paint);

    private final List<Expression> expressions = List.of(WILD, CARRIER, BRINDLE);

    private final FounderTable mareFounders = FounderTable.builder()
            .weight(Brn, Brn, 100.0 * WILD_BRN_FREQUENCY * WILD_BRN_FREQUENCY)
            .weight(Brn, n, 100.0 * 2 * WILD_BRN_FREQUENCY * (1 - WILD_BRN_FREQUENCY))
            .weight(n, n, 100.0 * (1 - WILD_BRN_FREQUENCY) * (1 - WILD_BRN_FREQUENCY))
            .build();

    /**
     * A stallion draws <b>one</b> allele, not two - the whole of X-linkage in
     * one table. Written out rather than derived from Hardy-Weinberg, because
     * Hardy-Weinberg is a statement about diploid loci and this one is not.
     */
    private final FounderTable stallionFounders = FounderTable.builder()
            .weight(Brn, Y, 100.0 * WILD_BRN_FREQUENCY)
            .weight(n, Y, 100.0 * (1 - WILD_BRN_FREQUENCY))
            .build();

    @Override public String key() { return KEY; }
    @Override public String name() { return "Brindle (MBTPS2)"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return true; }
    @Override public Inheritance inheritance() { return Inheritance.X_LINKED; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }

    /**
     * Sex-aware, and it has to be: the sex locus is priority 1 so it is always
     * already rolled, and a founder's number of copies here depends on it. A
     * context that somehow has not rolled sex degrades to the mare table rather
     * than throwing - the diploid case is the safe guess.
     */
    @Override
    public FounderTable founderTable(FounderContext context) {
        if (!context.isRolled(Genes.SEX)) {
            return mareFounders;
        }
        return Genes.SEX.sexOf(context.pair(Genes.SEX)) == Sex.FEMALE ? mareFounders : stallionFounders;
    }

    /**
     * A horse shows brindle when <b>every</b> real copy it has is {@code Brn} -
     * which is two for a mare and one for a stallion, so the same sentence
     * covers "recessive in mares" and "always shows in stallions" with no
     * special case. A pair with no real copy at all cannot occur
     * ({@link #sexConsistent}) but is answered anyway, because parsing is
     * tolerant and a hand-written code can name one.
     */
    @Override
    public Expression expressionOf(AllelePair pair) {
        List<Allele> real = realAlleles(pair);
        if (real.isEmpty()) {
            return WILD;
        }
        int brindle = 0;
        for (Allele a : real) {
            if (a.equals(Brn)) {
                brindle++;
            }
        }
        if (brindle == real.size()) {
            return BRINDLE;
        }
        return brindle > 0 ? CARRIER : WILD;
    }

    // ------------------------------------------------------------------
    // The painter
    // ------------------------------------------------------------------

    /**
     * Centre-to-centre streak distance in body units. The adult barrel is 22
     * long, so this puts six to ten streaks along it before the break field and
     * the per-side roll take some of them away - which is what brindle looks
     * like, and is a long way from the first attempt.
     */
    private static final double SPACING_MIN = 2.2;
    private static final double SPACING_RANGE = 1.4;
    /** Fraction of each period that is streak. Brindle streaks are thinner than the gaps. */
    private static final double DUTY_MIN = 0.30;
    private static final double DUTY_RANGE = 0.16;
    /**
     * How far, in body units, the noise may bend a streak off its plane.
     *
     * <p><b>It has to stay well under half the spacing.</b> Brindle is a smeared
     * pattern and the temptation is to warp it hard, but once the warp exceeds
     * the gap between two streaks they bend into each other and the whole thing
     * stops being stripes: the first attempt at 1.6-3.0 against a spacing of
     * 1.5-2.8 baked out as wood grain.
     */
    private static final double WARP_MIN = 0.5;
    private static final double WARP_RANGE = 0.7;
    /**
     * Gain on the streak coverage before it is whitened. <b>A brindle streak is
     * white</b>, so the core of one has to reach {@code 1} - and the raw
     * coverage rarely does, because the field multiplies the soft edge profile
     * by a per-side presence roll and a break field. Saturating above 1 takes
     * the core all the way while leaving the edge, and the streaks the two
     * rolls have genuinely damped, soft. Without it every streak came out as a
     * tan smear - a diluted stripe, which is the thing this is not.
     */
    private static final double WHITE_GAIN = 1.7;

    /**
     * White streaks, strongest over the barrel and quarters, still strong on the
     * neck, and reduced to a few crosswise strokes on the upper legs - which is
     * where BR1 sits on a real horse. Nothing at all on the head, which is not a
     * defining location for it and is where the rest-pose projection is only
     * approximate.
     *
     * <p>The streak goes to <b>white</b> at its core, through
     * {@link PigmentField#whiten}, so it is white hair over any base colour
     * rather than a paler version of one. {@code whiten} is the shared move
     * every white marking in the mod uses, and using it here is what keeps a
     * streak through a black horse on the gradient's neutral column - fading
     * through greys rather than walking sideways into the browns, which is what
     * scaling the two pigments together would do.
     *
     * <p><b>Draw order</b>, off {@code ctx.epigeneticsFor(geneKey)}:
     * {@code nextLong()} (the streak seed), then three {@code nextFloat()}s -
     * spacing, duty, warp.
     */
    private PigmentField paint(CoatBuildContext ctx, PigmentView coat) {
        Rng epi = ctx.epigeneticsFor(KEY);
        BlaschkoStripes.Pattern pat = new BlaschkoStripes.Pattern(
                epi.nextLong(),
                SPACING_MIN + SPACING_RANGE * epi.nextFloat(),
                DUTY_MIN + DUTY_RANGE * epi.nextFloat(),
                WARP_MIN + WARP_RANGE * epi.nextFloat());

        Skin skin = ctx.skin();
        PigmentField f = coat.mutableCopy();
        HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> {
            double weight = partWeight(part);
            if (weight <= 0.0) {
                return;
            }
            double c = BlaschkoStripes.coverage(skin, part, point, pat) * weight;
            if (c <= 0.0) {
                return;
            }
            // Lerp toward white by coverage, so a streak's own soft edge is a
            // soft edge in the coat rather than a hard margin.
            f.whiten(px, py, (float) Math.min(1.0, c * WHITE_GAIN));
        });
        return f;
    }

    /**
     * How hard brindle paints on each part. The <i>shape</i> per part is
     * {@link BlaschkoStripes}' business - it is what turns a body streak into a
     * crosswise stroke on a limb, and what leaves the head, muzzle, ears, mane
     * and tail alone - so this is only the strength.
     */
    private static double partWeight(Part part) {
        return switch (part) {
            case BODY -> 1.0;
            case NECK -> 0.90;
            case LEFT_FRONT_LEG, RIGHT_FRONT_LEG, LEFT_HIND_LEG, RIGHT_HIND_LEG -> 0.75;
            default -> 0.0;
        };
    }
}
