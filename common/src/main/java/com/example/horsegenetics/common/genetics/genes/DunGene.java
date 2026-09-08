package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.pattern.HairPattern;
import com.example.horsegenetics.common.coat.pattern.CoatRegions;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;
import com.example.horsegenetics.common.genetics.epi.EpiValues;

import java.util.List;

/**
 * <b>Dun</b> ({@code horsegenetics.dun}) - real-horse {@code TBX3}, and the
 * mod's second <b>three-allele</b> locus after {@link MatpGene}.
 *
 * <p>The real gene carries three alleles whose relationship needs a table, not
 * a dominance label: {@code D} dilutes <i>and</i> marks, {@code d1} does not
 * dilute but still <i>marks</i>, and {@code d2} does neither. So dilution reads
 * {@code D > d1 = d2} while marking reads {@code D = d1 > d2} - two different
 * dominance orders over the same three alleles, which is exactly the shape a
 * single dominance label could not say. Six combinations, three outcomes:
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code d2/d2}</td><td>wild type - no dilution, no markings</td></tr>
 *   <tr><td>{@code d1/d2}, {@code d1/d1}</td><td>{@code primitive-marks} - undiluted, but a dorsal stripe</td></tr>
 *   <tr><td>{@code D/d2}, {@code D/d1}, {@code D/D}</td><td>{@code dun} - diluted body, and the full marking set</td></tr>
 * </table>
 *
 * <h2>One idea: the dilution is what everything else is made of</h2>
 * Phase 1 is <b>downward-only</b> - a natural gene may never add pigment back -
 * so nothing here can literally paint a dark line onto a coat. It does not need
 * to. A primitive marking is <i>countershading</i>: "a stripe darker than the
 * body" and "a body lighter than the stripe" are the same picture. So the whole
 * gene is <b>one dilution and one mask</b>. The mask says, per texel, how much
 * of the dilution to lerp back off, and <i>every</i> dark thing a dun has is a
 * region where that number is high:
 *
 * <ul>
 *   <li>the <b>points</b> - mane, tail, ears, muzzle and the lower half of each
 *       leg - which a dun keeps at full base colour, and which are why a bay
 *       dun has black points over a tan body and a grullo black ones over a
 *       blue-grey one;</li>
 *   <li>the <b>dorsal stripe</b>, poll to dock and into the tail, on both
 *       marked outcomes;</li>
 *   <li>and, on {@code D} only, the accessory markings - <b>leg bars</b>, a
 *       <b>shoulder bar</b>, and a <b>face mask</b> or <b>cobwebbing</b>.</li>
 * </ul>
 *
 * <p>Both marked outcomes therefore run the <b>same painter</b> and differ only
 * in their constants and in how much of that list they draw. That keeps the
 * whole locus inside the restrict-only contract with no special case anywhere.
 *
 * <h2>Grullo, and the order the two pigments come off in</h2>
 * The dilution is {@link PigmentField#diluteNeutral}, not the warm
 * {@code dilute} that cream and champagne use: <b>red down to nothing first,
 * then black</b>. A black horse carries {@code red = 1} that its eumelanin
 * hides, so a dilution that takes the black off first unmasks that red and
 * walks the sample into the browns - and grullo is a <i>blue-grey</i>, on the
 * gradient's neutral column, not a mouse-brown. Scaling the <i>visible</i> red
 * instead is what puts it there, and it fixes the leg bars for free: a bar on a
 * grullo leg is now just "less diluted", where the old painter had to keep
 * black alone or the bar came back as a warm patch.
 *
 * <h2>The markings are incomplete on purpose</h2>
 * The dorsal stripe is the diagnostic one - a true dun essentially always has
 * it, and it reaches the tail. Everything else "sometimes": leg barring,
 * shoulder bars and forehead rings each occur on some duns and not others, one
 * foreleg can carry three bars while the other shows a smudge, and a strong
 * face mask tends to replace the finer cobwebbing rather than join it. So the
 * accessories are rolled per horse off the expressing copy's epigenetic seed
 * (see {@link #roll}), and the leg bars are rolled and seeded <b>per leg</b>.
 *
 * <p>Founder allele frequencies {@code 1/}{@value #WILD_DUN_ONE_IN} for
 * {@code D} and {@code 1/}{@value #WILD_MARKED_ONE_IN} for {@code d1}. See
 * {@code wiki/gene-dun.html}.
 */
public final class DunGene implements Gene {

    public static final String KEY = "horsegenetics.dun";
    /** Founder frequency of {@code D}: one allele copy in this many. */
    public static final int WILD_DUN_ONE_IN = 24;
    /** Founder frequency of {@code d1}: one allele copy in this many. */
    public static final int WILD_MARKED_ONE_IN = 10;

    /**
     * Body dilution under {@code D}, through
     * {@link PigmentField#diluteNeutral}.
     *
     * <p><b>Dun takes black off. It barely touches red.</b> That one asymmetry
     * is what makes the three bases come out as different as they do from a
     * single pair of numbers:
     * <ul>
     *   <li>a <b>black</b> horse has no <i>visible</i> red at all, so only
     *       {@code KEEP_BLACK} applies and the sample slides straight down the
     *       gradient's neutral column into grullo;</li>
     *   <li>a <b>bay</b> body is mostly red under a third of a load of black,
     *       so taking that black off unmasks the red it was damping and the
     *       body swings from red-brown to tan - the big, obvious change,
     *       and none of it is the red moving;</li>
     *   <li>a <b>chestnut</b> has no black to take, so red dun is
     *       <b>almost the same colour as chestnut</b>. That is the point of
     *       {@code KEEP_RED} being this close to 1: it is not a dilution knob,
     *       it is just enough countershade to let the markings read at all.
     *       A red dun is a chestnut with primitive markings, not a pale
     *       horse.</li>
     * </ul>
     */
    private static final float KEEP_RED = 0.93f;
    private static final float KEEP_BLACK = 0.42f;

    /**
     * The same two numbers for {@code d1} - the <i>undiluted</i> outcome, so
     * they are chosen to move less than {@code D}'s do, and {@code D}'s barely
     * move at all.
     *
     * <p><b>Black is never touched</b> ({@code MARKED_KEEP_BLACK = 1}), which
     * is the whole of what "non-dun" means here: taking black off is the half
     * of the dilution that actually changes a horse's colour, so an allele that
     * does not dilute must not do it. What is left is a whisper off the visible
     * red - enough for a spine line on a chestnut or a bay, and nothing at all
     * on a true black or a bay's points. That last one is not a gap but the
     * answer: a real non-dun black shows no primitive markings either.
     */
    private static final float MARKED_KEEP_RED = 0.95f;
    private static final float MARKED_KEEP_BLACK = 1.0f;

    /** Mean half-width of the dorsal stripe, in body units; jittered per horse. */
    private static final double DORSAL_HALF_WIDTH = 1.5;

    /**
     * Fraction of each leg that is a <b>point</b> - undiluted, so a dun's lower
     * legs stay dark - fading out over the {@link #POINT_LEG_FADE} above it.
     * The bars then sit on the pale leg above the fade, which is where a real
     * dun's read best and why the two numbers belong together.
     */
    private static final double POINT_LEG_SOLID = 0.20;
    private static final double POINT_LEG_FADE = 0.22;

    /** Leg bars: strongest at the knee / hock, reaching this far either side. */
    private static final double BAR_JOINT = 0.56;
    private static final double BAR_SPREAD = 0.48;
    private static final double BAR_SPACING = 3.2;      // body units, centre to centre
    private static final double BAR_DUTY = 0.42;
    /** How much of the dilution a bar at full coverage takes back off. */
    private static final double BAR_DEPTH = 1.0;

    /** Shoulder bar: one stroke, well forward on the barrel, leaning back as it drops. */
    private static final double SHOULDER_CENTRE = 0.80;
    private static final double SHOULDER_HALF_WIDTH = 0.085;
    private static final double SHOULDER_LEAN = 0.22;
    private static final double SHOULDER_DEPTH = 0.90;

    /** Face: a broad forehead mask, and the finer rings drawn inside it. */
    private static final double FACE_REACH = 3.4;       // body units from the forehead
    private static final double COBWEB_RING_SPACING = 1.5;
    private static final double MASK_DEPTH = 0.45;
    private static final double COBWEB_DEPTH = 0.70;

    /**
     * How often each accessory marking shows at all. The dorsal stripe has no
     * entry here because a true dun essentially always has one; these are the
     * "sometimes" list, and a horse that rolls none of them is a perfectly
     * ordinary dun with a stripe and nothing else.
     */
    private static final double BAR_CHANCE = 0.82;      // per leg, rolled four times
    private static final double SHOULDER_CHANCE = 0.55;
    private static final double COBWEB_CHANCE = 0.40;

    public final Allele D = new Allele(KEY, 0, "D", "Dun (D)");
    public final Allele d1 = new Allele(KEY, 1, "d1", "Non-dun, marked (d1)");
    public final Allele d2 = new Allele(KEY, 2, "d2", "Non-dun, unmarked (d2)");
    private final List<Allele> alleles = List.of(D, d1, d2);

    private final Expression WILD = Expression.wildType("No dilution and no primitive markings.");

    private final Expression MARKED = Expression.of("primitive-marks", "Primitive markings")
            .describe("No real dilution - the horse is its base colour - but a dorsal stripe still runs "
                    + "from poll to tail, a shade darker than the body around it. One d1 copy is "
                    + "enough; it is how a plain bay or black ends up with a spine line and no dun.")
            .varies()
            .restrict(primitive(MARKED_KEEP_RED, MARKED_KEEP_BLACK, false));

    private final Expression DUN = Expression.of("dun", "Dun")
            .describe("Black pigment thins while red is left alone, so a bay turns tan, a black "
                    + "turns blue-grey grullo, and a chestnut - which has no black to lose - stays "
                    + "very nearly chestnut. The points, a dorsal stripe from poll to tail, and "
                    + "whichever primitive markings this horse drew skip the dilution and stay "
                    + "dark.")
            .varies()
            .restrict(primitive(KEEP_RED, KEEP_BLACK, true));

    private final List<Expression> expressions = List.of(WILD, MARKED, DUN);

    /**
     * The six combinations at their Hardy-Weinberg shares given
     * {@code p(D) = 1/24} and {@code p(d1) = 1/10}. Written out rather than
     * computed so an author can retune one row without disturbing the others.
     * The three {@code D} rows still sum to the share the old two-allele table
     * gave, so adding {@code d1} split the non-dun population without making
     * duns any rarer.
     */
    private final FounderTable founders = FounderTable.builder()
            .weight(D, D, 0.173611)
            .weight(D, d1, 0.833333)
            .weight(D, d2, 7.152778)
            .weight(d1, d1, 1.000000)
            .weight(d1, d2, 17.166667)
            .weight(d2, d2, 73.673611)
            .build();

    /**
     * Half the width of the dark band down the mane and the tail, as a fraction
     * of the part's second-longest span - the same axis
     * {@link HairPattern#centreStripe} puts the healer's line down, which on the
     * big side faces of both parts reads as a stripe straight down the middle
     * with paler hair either side of it.
     *
     * <p>The value is set by the mesh, not by taste. A mane is four texels
     * across, so the only two distances that exist are an eighth of the span
     * and three eighths of it; {@link HairPattern#centreStripe} feathers over
     * {@code halfWidth +/- halfWidth/2}, and {@code 0.25} puts those two edges
     * exactly on the two texel rows. Anything narrower feathers the dark centre
     * as well, which is how a bay dun ended up with a mane 3% off black instead
     * of black.
     *
     * <p>Fixed rather than rolled: the dorsal-width jitter already varies the
     * stripe on the <i>body</i>, and another epigenetic draw here would move
     * every dun in the golden file for a difference nobody could see.
     */
    private static final double MIDSTOL_HALF_WIDTH = 0.25;

    /** How much further than the body the pale guard hairs are taken down. */
    private static final float GUARD_HAIR = 0.28f;

    @Override public String key() { return KEY; }
    @Override public String name() { return "Dun"; }
    @Override public int priority() { return 34; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return d2; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        if (pair.has(D)) {
            return DUN;
        }
        return pair.has(d1) ? MARKED : WILD;
    }

    /** Does this combination dilute the body - i.e. carry {@code D}? */
    public boolean isDun(AllelePair pair) {
        return pair.has(D);
    }

    /** Does this combination draw a dorsal stripe - {@code D} or {@code d1}? */
    public boolean isMarked(AllelePair pair) {
        return pair.has(D) || pair.has(d1);
    }

    /**
     * The marking field, the dorsal stripe's width, and a propensity for each
     * accessory - the shoulder bar, the facial cobweb, and one per leg. The
     * accessories are stored as propensities rather than as strengths so
     * {@link #accessory} can keep its ramp: most duns that have a shoulder bar
     * have a hint of one, not a painted stripe, and that shape lives in the
     * painter rather than in the stored number.
     */
    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(
                EpiValue.seed("seed"),
                EpiValue.uniform("dorsal_width", 0.80, 1.30),
                EpiValue.uniform("shoulder", 0, 1),
                EpiValue.uniform("face", 0, 1),
                EpiValue.perLeg("bar", 0, 1));
    }

    /**
     * Which primitive markings <b>this</b> horse drew, and how strongly. Rolled
     * off the expressing copy's epigenetic seed, so a horse regenerates the
     * same set every session and a foal that inherits the copy inherits its
     * dam's markings.
     *
     * <p>Both marked outcomes read the same stored values even though
     * {@code d1} only uses the seed and the dorsal width, so a {@code d1} horse
     * that later gains a {@code D} copy keeps the stripe it had and gains the
     * accessories it was always carrying.
     */
    private record Markings(long seed, double dorsalHalfWidth, double shoulder, double face,
                            boolean cobweb, double[] bars) {}

    private static Markings roll(EpiValues epi) {
        long seed = epi.seed("seed");
        double dorsal = DORSAL_HALF_WIDTH * epi.get("dorsal_width");
        double shoulder = accessory(epi.get("shoulder"), SHOULDER_CHANCE);
        double face = accessory(epi.get("face"), COBWEB_CHANCE);
        double[] bars = new double[CoatRegions.LEGS.size()];
        for (int i = 0; i < bars.length; i++) {
            bars[i] = accessory(epi.get("bar", i), BAR_CHANCE);
        }
        // "A full dark mask can replace the more delicate web effect visually" -
        // so the same roll picks which of the two this horse shows, the stronger
        // half of the range going to the plain mask.
        return new Markings(seed, dorsal, shoulder, face, face < 0.6, bars);
    }

    /**
     * One accessory roll: absent on {@code 1 - chance} of horses, and on the
     * rest ramping from a faint smudge up to full strength. Deliberately not a
     * plain uniform - most of the duns that have a shoulder bar have a hint of
     * one, not a painted stripe.
     */
    private static double accessory(double roll, double chance) {
        if (roll >= chance) {
            return 0;
        }
        double t = (chance - roll) / (chance * 0.55);
        return t > 1 ? 1 : t;
    }

    /**
     * The one painter both marked outcomes use: dilute everything, then lerp
     * the dilution back off wherever the marking mask says so.
     *
     * @param accessories whether to draw the {@code D}-only markings - leg bars,
     *                    the shoulder bar and the face - as well as the points
     *                    and the dorsal stripe
     */
    private static Expression.Pigment primitive(float keepRedBody, float keepBlackBody, boolean accessories) {
        return (ctx, coat) -> {
            Skin skin = ctx.skin();
            Markings m = roll(ctx.epigeneticsFor(KEY));
            PigmentField f = coat.mutableCopy();
            HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> {
                double mark = CoatRegions.dorsalStripe(skin, part, point, m.dorsalHalfWidth());
                mark = Math.max(mark, pointRegion(skin, part, point));
                mark = Math.max(mark, alreadyAPoint(f.red(px, py), f.black(px, py)));
                if (accessories) {
                    mark = Math.max(mark, m.shoulder() * SHOULDER_DEPTH * CoatRegions.shoulderBar(
                            skin, part, point, m.seed(), SHOULDER_CENTRE, SHOULDER_HALF_WIDTH, SHOULDER_LEAN));
                    mark = Math.max(mark, m.face() * faceMarking(skin, part, point, m));
                    int leg = CoatRegions.LEGS.indexOf(part);
                    if (leg >= 0 && m.bars()[leg] > 0) {
                        mark = Math.max(mark, m.bars()[leg] * BAR_DEPTH * CoatRegions.legBar(
                                skin, part, point, m.seed() + leg * 0x9E3779B97F4A7C15L,
                                BAR_JOINT, BAR_SPREAD, BAR_SPACING, BAR_DUTY));
                    }
                }
                if (part == Part.MANE || part == Part.TAIL) {
                    // On the long hair the midtstol is the AUTHORITY, so cap
                    // rather than take a maximum. A bay's mane arrives here as
                    // absolute black, which makes alreadyAPoint return 1 and
                    // would protect the whole mane from the marking - which is
                    // how a bay dun ended up with a mane indistinguishable from
                    // a plain bay's while a black dun got its stripe. Every
                    // other part still wants the maximum: a leg is a point
                    // because it is already black, not because a mask said so.
                    mark = Math.min(mark, midtstol(skin, part, point));
                }
                if (mark >= 1.0) {
                    return;
                }
                // The guard hairs either side of the midtstol go paler than the
                // body, not merely as pale: the reference calls them cream,
                // silver-white or yellowish, and a mane diluted only as far as
                // the barrel reads as one colour at riding distance.
                //
                // The overshoot is scaled by how far this channel is being
                // diluted at all, which is what keeps it a DUN feature. A
                // non-dun d1 horse takes no black off anything, so its guard
                // factor is exactly 1 and the painter stays the byte-for-byte
                // no-op that DunGeneTest pins - guard hair is pale because the
                // dilution is stronger there, not for its own reasons.
                boolean hair = part == Part.MANE || part == Part.TAIL;
                float over = hair ? (float) (GUARD_HAIR * (1.0 - mark)) : 0f;
                f.diluteNeutral(px, py,
                        lerp(keepRedBody, 1f, (float) mark) * (1f - over * (1f - keepRedBody)),
                        lerp(keepBlackBody, 1f, (float) mark) * (1f - over * (1f - keepBlackBody)));
            });
            return f;
        };
    }

    /**
     * <b>Is this texel already a point?</b> - {@code black * (1 - red)}, and the
     * one line that lets a bay dun keep the black its agouti copy climbed up
     * its legs and face.
     *
     * <p>The pigment model says it exactly. A gene that paints a point paints it
     * <i>absolutely</i>: {@code BayCoat} sets {@code red = 0, black = 1}, and
     * that pair - black with no red under it - occurs nowhere else. A black
     * <i>horse</i> is {@code (1, 1)}: black with a full load of masked
     * pheomelanin, which is what makes it a body colour and not a point, and
     * which is what dun is supposed to dilute into grullo. Multiply and the two
     * separate cleanly - 1 for a point, 0 for a black body, 0 for a chestnut,
     * and the partial values through a bay's leg ramp turn out to be exactly
     * the fade the marking mask wants.
     *
     * <p>Without it, the top of a seal bay's black leg - mostly black hair, a
     * little red - diluted to a grey cuff between the tan body and the black
     * point, and no real horse has one of those. The square root is there for
     * the same reason: it pushes a half-and-half texel most of the way to
     * "point", because the grey a half dilution makes out of one is not a
     * colour a leg has either, and the fade wants to be short.
     */
    private static double alreadyAPoint(float red, float black) {
        return Math.sqrt(black * (1.0 - red));
    }

    /**
     * The <b>midtstol</b>: the dark band down the centre of the mane and the
     * tail, with paler guard hair either side of it. A dun's primitive
     * markings are one continuous system - forelock, crest, spine, tail dock -
     * and this is the stretch of it the long hair carries. It is what makes a
     * Fjord mane read as a pale-dark-pale ridge, and it is on every dun here,
     * not only the Fjord: the breed is famous for it because the mane is
     * clipped upright to show it off, not because the breed alone has it.
     */
    private static double midtstol(Skin skin, Part part, HorseSkinGeometry.BodyPoint point) {
        return HairPattern.centreStripe(skin, part, point, MIDSTOL_HALF_WIDTH);
    }

    /**
     * The <b>points</b>: 1 where a dun keeps its base colour outright. Mane,
     * tail, ears and muzzle are points in full; a leg is a point up to
     * {@link #POINT_LEG_SOLID} and then fades out, so the dark lower leg
     * dissolves into the diluted upper leg instead of ending in a ring.
     */
    private static double pointRegion(Skin skin, Part part, HorseSkinGeometry.BodyPoint point) {
        switch (part) {
            case MANE, TAIL -> {
                return midtstol(skin, part, point);
            }
            case LEFT_EAR, RIGHT_EAR, MUZZLE -> {
                return 1.0;
            }
            case LEFT_FRONT_LEG, RIGHT_FRONT_LEG, LEFT_HIND_LEG, RIGHT_HIND_LEG -> {
                HorseSkinGeometry.Bounds b = HorseSkinGeometry.bounds(skin, part);
                double frac = (point.y() - b.yMin()) / b.span(HorseSkinGeometry.Axis.Y);
                double t = (frac - POINT_LEG_SOLID) / POINT_LEG_FADE;
                return t <= 0 ? 1.0 : (t >= 1 ? 0.0 : 1.0 - t * t * (3 - 2 * t));
            }
            default -> {
                return 0.0;
            }
        }
    }

    /** Either a broad forehead mask or the finer cobwebbing inside it, never both. */
    private static double faceMarking(Skin skin, Part part, HorseSkinGeometry.BodyPoint point, Markings m) {
        return m.cobweb()
                ? COBWEB_DEPTH * CoatRegions.faceCobweb(skin, part, point, m.seed(),
                        COBWEB_RING_SPACING, FACE_REACH)
                : MASK_DEPTH * CoatRegions.faceMask(skin, part, point, FACE_REACH);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
