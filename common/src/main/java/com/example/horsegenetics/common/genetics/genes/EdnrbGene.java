package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.CoatRegions;
import com.example.horsegenetics.common.coat.pattern.PatchNoise;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.coat.pattern.PigmentView;
import com.example.horsegenetics.common.coat.pattern.WhitePattern;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Axis;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.eye.EyeRequest;
import com.example.horsegenetics.common.genetics.eye.EyeRequestContribution;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.trait.Condition;
import com.example.horsegenetics.common.trait.HealthContribution;
import com.example.horsegenetics.common.trait.TraitBuilder;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;
import com.example.horsegenetics.common.genetics.epi.EpiValues;
import com.example.horsegenetics.common.genetics.EyeSpread;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>{@code EDNRB}</b> ({@code horsegenetics.ednrb}) - <b>frame overo</b>, and
 * the model's first genuine lethal. Alleles {@code O} and {@code N}; this is
 * what used to be {@code horsegenetics.frame}, renamed to its locus alongside
 * {@link KitGene}, {@link MitfGene} and {@link Pax3Gene} so a reader can see at
 * a glance which patterns share a chromosome slot and which do not. Frame has
 * this locus to itself, which is precisely why it composes freely with
 * everything else - a horse really can be frame <i>and</i> tobiano <i>and</i>
 * splash.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code N/N}</td><td>wild type</td></tr>
 *   <tr><td>{@code O/N}</td><td>{@code frame} - irregular flank white that never crosses the topline</td></tr>
 *   <tr><td>{@code O/O}</td><td>{@code lethal-white} - an all-white foal; <b>masks</b></td></tr>
 * </table>
 *
 * <h2>The lethal, and why it is a pen and not a hole</h2>
 * {@code O/O} is Overo Lethal White Syndrome: an all-white foal born without
 * the nerve supply its gut needs, which dies within a day or two. That makes it
 * a <b>different kind of impossible</b> from {@link KitGene}'s nonviable
 * {@code W} homozygotes, and the model distinguishes them:
 * <ul>
 *   <li>An <b>embryonic</b> lethal is a horse that never existed, so it gets
 *       {@link #canOccur} {@code false} - no pen, not counted.</li>
 *   <li>{@code O/O} is <b>born</b>. It has a real phenotype, a player who
 *       crosses two carriers will see it, and hiding it would hide the single
 *       most important fact about breeding frame horses. So it occurs, it has
 *       its own outcome, and it gets a gallery pen.</li>
 * </ul>
 * What is <b>not</b> modelled yet is the death: the mod has no health system,
 * so an {@code O/O} foal is born all-white and then simply lives. That is a
 * deliberate first slice - the coat is honest, the consequence waits for the
 * health work ({@code wiki/roadmap.html} §6.4). It is the reason the outcome is
 * named {@code lethal-white} rather than something softer.
 *
 * <p>{@code O/O} is left <b>out of the founder table</b> regardless: a wild
 * spawn is an adult horse, and no adult horse is homozygous frame. It can only
 * arise from breeding two carriers, at the usual one in four - which is the
 * whole point.
 *
 * <h2>The shape, and what makes it recognisable</h2>
 * Frame is identified by the <b>distribution and outline</b> of its white
 * rather than by any single marking, and every part of {@link #paintFrame} is
 * one of those properties:
 * <ul>
 *   <li><b>Splotches in the middle of the side.</b> Big, solid, irregular
 *       patches gathered around the centre of the barrel, the flank, the
 *       shoulder and the lower neck, with coloured coat left <i>above and
 *       below</i> them. That framing is the pattern and the name. Tobiano runs
 *       top-down over the back and splash comes bottom-up from the feet; frame
 *       sits in the middle and touches neither end.</li>
 *   <li><b>A dark back.</b> {@link #TOPLINE_CAP} is a hard exclusion, not a
 *       ramp: frame white does not cross the dorsal midline, which is the
 *       definition of the pattern rather than a tuning choice.</li>
 *   <li><b>Dark legs and a bold face.</b> The legs are excluded outright but
 *       for an occasional coronet; the face is drawn near the top of the shared
 *       face-marking ramp, and rolled independently, because a frame horse can
 *       be dramatically bald-faced with a modest body.</li>
 *   <li><b>Asymmetry.</b> A per-horse lean across the width, so one flank can
 *       carry a bold patch while the other is largely dark.</li>
 *   <li><b>Cryptic expression.</b> The cover roll reaches low enough that some
 *       carriers show little more than the face and a blue eye - which is not a
 *       curiosity, it is why the locus is DNA-tested.</li>
 * </ul>
 *
 * <p><b>What this used to draw</b>, and why it is worth naming: a horse dipped
 * in white to a frayed waterline, belly-deep or deeper, with the coloured coat
 * reduced to scraps. Three independent mistakes made it - a threshold that was
 * not calibrated against the noise field it thresholded, a band that was open
 * downward instead of centred, and a jag running finer than one texel. Each is
 * documented on the constant that carries it ({@link #COVER_MIN},
 * {@link #BAND_CENTER_MIN}, {@link #JAG}), because all three were individually
 * plausible and only the picture showed they were wrong.
 *
 * <p>Natural. {@code frame-overo} is <b>non-deterministic</b>;
 * {@code lethal-white} is deterministic (it is total). See
 * {@code wiki/gene-ednrb.html}.
 */
public final class EdnrbGene implements Gene, HealthContribution, EyeRequestContribution,
        WhitePatternEyes.WhiteExtent {

    public static final String KEY = "horsegenetics.ednrb";
    /** Founder frequency of {@code O}: one allele copy in this many. */
    public static final int WILD_FRAME_ONE_IN = 55;

    /**
     * <b>Frame patches are big.</b> Both numbers are counts of patch periods
     * across the <b>barrel's own</b> bounding box rather than absolute model
     * units, so a foal is marked at the same relative scale as the adult it
     * grows into instead of inheriting a coarser version of the same field.
     *
     * <p>{@value #PATCHES_ALONG} periods along a barrel and
     * {@value #PATCHES_TALL} up its side puts two or three splotches on a
     * flank, each of them a good half the depth of the barrel - which is what
     * frame looks like. Sampled five times longer than tall, which is what this
     * used to do, the field produced horizontal <i>streaks</i>: the anisotropy
     * was doing the job the vertical band is now responsible for, and doing it
     * by smearing every patch out into a waterline.
     */
    private static final double PATCHES_ALONG = 2.2;
    private static final double PATCHES_TALL = 1.3;

    /**
     * How much of the eligible side goes white, rolled per horse. This is a
     * genuine <b>area fraction</b> - see {@link #paintFrame}, which sorts the
     * field and cuts it at the matching quantile - and not a raw threshold on
     * the noise.
     *
     * <p>That distinction is the whole reason this gene used to paint a dipped
     * horse. {@link PatchNoise#field} is an average of three octaves, so it
     * concentrates around {@code 0.5} and rarely leaves {@code [0.25, 0.70]};
     * thresholding it at {@code 1 - cover} over a range that ran up to
     * {@code 0.86} and down to {@code 0.24} meant most rolls landed outside the
     * field's actual spread entirely. A frame horse came out either blank or
     * flooded, with nothing in between, and because the band was open downward
     * the flood pooled in the belly. Calibrating against the field's own
     * distribution makes the roll mean what its name says at every value.
     *
     * <p>The roll is <b>skewed toward the bold end</b> ({@value #COVER_GAMMA}),
     * because <b>cryptic</b> expression is the interesting minority and not the
     * default: some frame carriers show little more than a bold face and a blue
     * eye, which is not a rare curiosity but the reason the locus is DNA-tested
     * rather than eyeballed.
     */
    private static final double COVER_MIN = 0.02;
    private static final double COVER_RANGE = 0.40;
    private static final double COVER_GAMMA = 0.80;

    /**
     * Fine-scale threshold wobble - the source of the ragged, zig-zagged edge
     * frame is known for. Also measured in periods across the barrel, and both
     * numbers are far smaller than they were.
     *
     * <p>A texel on the barrel is half a model unit. The old jag ran at
     * {@code 3.4} <i>per unit</i> - a period of a third of a texel - so it was
     * not an edge treatment at all but per-pixel white noise, which is what
     * dissolved the coloured coat into scraps. {@value #JAG_PERIODS} periods
     * along the barrel is a wobble about five texels across, and the amplitude
     * is sized to displace an edge by two or three texels rather than to
     * overwhelm the patch field it is meant to be perturbing.
     */
    private static final double JAG = 0.06;
    private static final double JAG_PERIODS = 6.5;

    /**
     * <b>The band frame white lives in</b>, as fractions of the <b>barrel's own
     * height</b> - {@code 0} the underline, {@code 1} the topline. Measuring
     * against {@link Part#BODY}'s bounds rather than
     * {@link WhitePattern#toplineHeight} is what lets "the centre of the side"
     * be said literally.
     *
     * <p>This is the shape of the pattern. Frame is <b>splotches in the middle
     * of the side</b>, with coloured coat left above <i>and</i> below them -
     * that is what frames them, and it is where the name comes from. So the
     * band is a soft <b>centre</b> and not a floor: the bar rises quadratically
     * with distance from it, so the strongest peaks of the field can push a
     * patch down over the belly or up toward the hip while everything else
     * stays gathered around the middle of the barrel.
     *
     * <p>The previous version had a floor <i>below</i> the underline and no
     * penalty above it, which is a bottom-anchored gate: every texel from the
     * belly to the ceiling was equally eligible, so white arrived as a rising
     * tide with a frayed top edge. That is the "dipped" horse, and retuning the
     * noise would not have fixed it, because the band and not the noise was
     * drawing the shape.
     *
     * <p>{@value #BAND_UP} is smaller than {@value #BAND_DOWN} on purpose:
     * frame white reaches the belly far more readily than it climbs, and
     * {@value #TOPLINE_CAP} caps it outright below the spine. White never
     * crossing the dorsal midline is the definition of the pattern rather than
     * a tuning choice, so it is enforced and not merely discouraged.
     */
    private static final double BAND_CENTER_MIN = 0.40;
    private static final double BAND_CENTER_RANGE = 0.22;
    private static final double BAND_UP = 0.30;
    private static final double BAND_DOWN = 0.42;
    private static final double TOPLINE_CAP = 0.86;

    /**
     * A per-horse scale on both halves of the band, so one frame horse carries
     * compact splotches high on the flank and the next has them spilling down
     * the barrel.
     */
    private static final double BAND_REACH_MIN = 0.80;
    private static final double BAND_REACH_RANGE = 0.50;

    /**
     * The same band on the <b>neck's own</b> height. The neck needs its own
     * because it stands above the barrel: measured in barrel-space Y it is
     * almost entirely out of range, which is why a frame horse used to have a
     * completely dark neck. Frame white runs along the lower and middle neck -
     * sometimes joining the face white to the body white - and leaves the
     * <b>upper crest dark</b>, so the centre sits low and the cap is hard.
     */
    private static final double NECK_CENTER_MIN = 0.26;
    private static final double NECK_CENTER_RANGE = 0.14;
    private static final double NECK_UP = 0.26;
    private static final double NECK_DOWN = 0.34;
    private static final double NECK_CAP = 0.62;

    /**
     * How steeply the bar rises away from the middle of the band, in units of
     * the patch field. That field's usable spread is about {@code 0.4} wide, so
     * a full {@value #EDGE_BIAS} at the band edge is several standard
     * deviations of penalty - firm confinement, with the tail of the
     * distribution still able to break out of it here and there.
     *
     * <p>Because the threshold is a <b>quantile</b>, this moves white around
     * without changing how much of it there is: {@link #COVER_MIN} and friends
     * decide the area, this decides where it pools. Those were the same knob
     * before, which is why neither could be set correctly.
     */
    private static final double EDGE_BIAS = 0.24;

    /**
     * How hard one flank is favoured over the other. A frame horse's two sides
     * do not need to match at all - one can carry a bold hip patch while the
     * other is largely dark - so a per-horse lean is added across the width.
     * The patch field is already asymmetric (it is sampled in 3D), but only
     * mildly; this is what makes a horse that is genuinely <i>whiter on one
     * side</i>.
     */
    private static final double SIDE_LEAN = 0.13;

    /**
     * How much white frame puts on the face, on {@link WhitePattern}'s shared
     * scale. Frame is the classically <b>bald-faced</b> pattern - a broad blaze
     * at the low end of the roll and an apron face at the high end - so it sits
     * near the top of the ramp, and the marking itself is drawn from the same
     * star / stripe / snip vocabulary every other white locus uses.
     *
     * <p>Rolled <b>independently of the body cover</b>, because face-body
     * contrast is part of the phenotype: the head can be dramatically white on a
     * horse whose body spotting is modest, and on a cryptic carrier the face is
     * often the only thing there is to see.
     */
    private static final double FACE_MIN = 0.62;
    private static final double FACE_RANGE = 0.36;
    /** Frame's margins are torn, on the face as much as on the flank. */
    private static final double FACE_JAG = 0.34;

    /**
     * Frame legs are dark - that is half of how the pattern is recognised - but
     * a coronet band or a white hoof does turn up. Per leg, and never more than
     * {@value #CORONET_MAX} of the leg's height, because a tall stocking on a
     * frame horse means it is carrying something else as well.
     */
    private static final double CORONET_CHANCE = 0.17;
    private static final double CORONET_MIN = 0.03;
    private static final double CORONET_MAX = 0.09;
    private static final double CORONET_WOBBLE = 0.35;

    /**
     * <b>Overo lethal white syndrome.</b> The all-white foal an {@code O/O}
     * pairing throws has no working enteric nervous system and cannot pass
     * anything through its gut.
     *
     * <p>The coat half of this has shipped since the white-pattern rewrite -
     * {@code O/O} has its own masking all-white outcome, it occurs, and it gets
     * a gallery pen. <b>This is the death</b>, which was the half that was
     * missing: the foal is born, named and filed in the pedigree, and then does
     * not survive. That is the whole difference between this gene and
     * {@link MetGene}, whose embryo never implants and which therefore produces
     * no foal to name.
     */
    public static final Condition LETHAL_WHITE_SYNDROME = Condition.lethalAtBirth(
            "overo-lethal-white", "Overo lethal white syndrome",
            "Two frame copies. The foal is born pure white with an unformed gut and cannot "
                    + "survive its first day.");

    public final Allele O = new Allele(KEY, 0, "O", "Frame overo (O)");
    public final Allele N = new Allele(KEY, 1, "N", "Wild-type (N)");
    private final List<Allele> alleles = List.of(O, N);

    private final Expression WILD = Expression.wildType("No white patches.");

    private final Expression FRAME_OVERO = Expression.of("frame-overo", "Frame overo")
            .describe("Big, sharp-edged splotches of white across the middle of the side - "
                    + "barrel, flank, shoulder and lower neck - that never cross the back and "
                    + "rarely reach the belly, so the coloured coat is left framing them above "
                    + "and below, which is where the name comes from. The "
                    + "face is broadly white, often bald, and the legs stay dark but for the "
                    + "occasional coronet. The two sides need not match. Some carriers are marked "
                    + "so little you would never guess, which is why frame is tested for and not "
                    + "eyeballed.")
            .varies()
            .restrict(EdnrbGene::paintFrame);

    private final Expression LETHAL_WHITE = Expression.of("lethal-white", "Lethal white")
            .describe("An all-white foal with pink skin - Overo Lethal White Syndrome, which two "
                    + "frame carriers produce one time in four. In a real horse the gut has no "
                    + "nerve supply and the foal dies within a day or two; this mod paints the coat "
                    + "and does not yet model the death.")
            .masking()
            .restrict((ctx, coat) -> {
                PigmentField f = coat.mutableCopy();
                CoatRegions.restrictAll(ctx.skin(), f, (field, px, py, p) -> {
                    field.whiten(px, py, 1f);
                });
                return f;
            });

    private final List<Expression> expressions = List.of(WILD, FRAME_OVERO, LETHAL_WHITE);

    /**
     * {@code O/O} is excluded here and <b>only</b> here: it is a real
     * combination ({@link #canOccur} is true) that simply never turns up in an
     * adult founder population, because those foals do not become adults.
     */
    private final FounderTable founders =
            FounderTable.hardyWeinberg(frequencies(), pair -> !pair.homozygousFor(O));

    private Map<Allele, Double> frequencies() {
        Map<Allele, Double> p = new LinkedHashMap<>();
        p.put(O, 1.0 / WILD_FRAME_ONE_IN);
        p.put(N, 1.0 - 1.0 / WILD_FRAME_ONE_IN);
        return p;
    }

    @Override public String key() { return KEY; }
    @Override public String name() { return "EDNRB (frame overo)"; }
    @Override public int priority() { return 74; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return N; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        if (pair.homozygousFor(O)) {
            return LETHAL_WHITE;
        }
        return pair.has(O) ? FRAME_OVERO : WILD;
    }

    /** Does this combination carry frame at all - i.e. is it a carrier or worse? */
    public boolean isFrame(AllelePair pair) {
        return pair.has(O);
    }

    /** Is this the homozygous lethal-white combination? */
    public boolean isLethalWhite(AllelePair pair) {
        return pair.homozygousFor(O);
    }

    /**
     * <b>The frame shape.</b> Dark legs, a bold face, a dark back, and big
     * irregular splotches of white gathered around the <b>middle of the
     * side</b> - across the barrel, the flank, the shoulder and the lower neck,
     * with coloured coat left above and below them. That is the visual template
     * the pattern is recognised by and the reason it is called <i>frame</i>:
     * the colour is left above, below, fore and aft of a white field, framing
     * it like a window.
     *
     * <p><b>Two passes, and why.</b> The first pass scores every eligible texel
     * and the second whitens the top {@code cover} fraction of them. Scoring
     * twice costs two noise evaluations per texel, and buys the one thing a
     * single pass cannot give: a threshold that is a <b>quantile of this
     * horse's own field</b> rather than a constant hoped to sit somewhere
     * inside it. {@link #COVER_MIN} explains what went wrong when it was a
     * constant. It also cleanly separates <i>how much</i> white there is
     * ({@code cover}) from <i>where it pools</i> ({@link #EDGE_BIAS}, the band,
     * the side lean), because a bias that only re-ranks texels cannot change
     * how many of them clear a quantile.
     *
     * <p>Every number is read by name off the expressing copy - the noise seed,
     * the face strength, the body cover, the barrel band centre and reach, the
     * neck band centre, the side lean, and one coronet height per leg in
     * {@link CoatRegions#LEGS} order. See {@link #epiSchema()}.
     */
    // ------------------------------------------------------------------
    // Epigenetics
    // ------------------------------------------------------------------

    private static final String SEED = "seed";
    private static final String FACE = "face";
    private static final String COVER = "cover";
    private static final String BAND_CENTER = "band_center";
    private static final String BAND_REACH = "band_reach";
    private static final String NECK_CENTER = "neck_center";
    private static final String LEAN = "side_lean";
    private static final String CORONET = "coronet";

    /**
     * Frame's shape, per horse: the noise field, how strong the face marking is,
     * <b>how much white there is</b> ({@link #COVER}), where the barrel and neck
     * bands sit and how far they reach, which side the white leans to, and one
     * coronet height per leg.
     *
     * <p>{@link #COVER} keeps the power curve it was drawn with, so most frames
     * carry a modest amount of white and the near-white ones stay rare. It is
     * the single most legible number on the gene, and the one a breeder aiming
     * for a loud frame is actually selecting on.
     *
     * <p>Composed with {@link WhitePattern#faceSchema()} - frame draws the
     * shared face marking - and with {@link EyeSpread#schema()}, because a frame
     * can claim the eye colour and the spread is read off whichever gene wins.
     */
    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(
                        EpiValue.seed(SEED),
                        EpiValue.uniform(FACE, FACE_MIN, FACE_MIN + FACE_RANGE),
                        EpiValue.power(COVER, COVER_MIN, COVER_MIN + COVER_RANGE, COVER_GAMMA),
                        EpiValue.uniform(BAND_CENTER, BAND_CENTER_MIN, BAND_CENTER_MIN + BAND_CENTER_RANGE),
                        EpiValue.uniform(BAND_REACH, BAND_REACH_MIN, BAND_REACH_MIN + BAND_REACH_RANGE),
                        EpiValue.uniform(NECK_CENTER, NECK_CENTER_MIN, NECK_CENTER_MIN + NECK_CENTER_RANGE),
                        EpiValue.uniform(LEAN, -1, 1),
                        EpiValue.perLeg(CORONET, 0, 1))
                .and(WhitePattern.faceSchema().values().toArray(new EpiValue[0]))
                .and(EyeSpread.schema().values().toArray(new EpiValue[0]));
    }

    private static PigmentField paintFrame(CoatBuildContext ctx, PigmentView coat) {
        EpiValues epi = ctx.epigeneticsFor(KEY);
        long seed = epi.seed(SEED);
        double faceStrength = epi.get(FACE);
        double cover = epi.get(COVER);
        double bandCenter = epi.get(BAND_CENTER);
        double reach = epi.get(BAND_REACH);
        double neckCenter = epi.get(NECK_CENTER);
        double sideLean = epi.get(LEAN);
        double[] coronets = new double[CoatRegions.LEGS.size()];
        for (int i = 0; i < coronets.length; i++) {
            coronets[i] = epi.get(CORONET, i);
        }

        Skin skin = ctx.skin();
        WhitePattern.FaceMarking faceMark =
                WhitePattern.faceMarking(epi, skin, faceStrength, FACE_JAG);
        Shape shape = new Shape(skin, seed, bandCenter, reach, neckCenter, sideLean);

        // Pass one: what does this horse's field actually look like? Collected
        // over the eligible side only, so `cover` is a fraction of the surface
        // frame can mark and not of the whole hide.
        final double[][] scores = { new double[2048] };
        final int[] count = { 0 };
        HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> {
            double s = shape.score(part, point);
            if (s == EXCLUDED) {
                return;
            }
            if (count[0] == scores[0].length) {
                double[] bigger = new double[scores[0].length * 2];
                System.arraycopy(scores[0], 0, bigger, 0, count[0]);
                scores[0] = bigger;
            }
            scores[0][count[0]++] = s;
        });
        double[] sorted = Arrays.copyOf(scores[0], count[0]);
        Arrays.sort(sorted);
        int keep = (int) Math.round(cover * count[0]);
        // A cryptic carrier can legitimately keep nothing; +INFINITY is the
        // threshold no texel clears, rather than a special case in pass two.
        double threshold = keep <= 0 ? Double.POSITIVE_INFINITY : sorted[count[0] - keep];

        // Pass two: paint it, plus the face, which is rolled and drawn
        // independently and so takes no part in the calibration above.
        PigmentField f = coat.mutableCopy();
        HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> {
            if (part == Part.HEAD || part == Part.MUZZLE) {
                if (faceMark.covers(part, face, point)) {
                    whiten(f, px, py);
                }
                return;
            }
            if (shape.score(part, point) >= threshold) {
                whiten(f, px, py);
            }
        });

        paintCoronets(skin, f, seed, coronets);
        return f;
    }

    /** The score of a texel that frame may not mark at all - below every threshold. */
    private static final double EXCLUDED = Double.NEGATIVE_INFINITY;

    /**
     * One horse's frame field: the rolled band, the rolled lean, and the noise
     * frequencies derived from this skin's barrel. {@link #score} is a pure
     * function of {@code (part, point)} so the calibration pass and the paint
     * pass cannot disagree about what a texel is worth.
     */
    private static final class Shape {

        private final long seed;
        private final double scale;
        private final double yStretch;
        private final double jagFreq;
        private final double sideLean;
        private final double halfWidth;

        private final double bodyCenter;
        private final double bodyUp;
        private final double bodyDown;
        private final double bodyCap;

        private final boolean hasNeck;
        private final double neckCenter;
        private final double neckUp;
        private final double neckDown;
        private final double neckCap;

        Shape(Skin skin, long seed, double bandCenter, double reach, double neckCenterFrac,
                double sideLean) {
            this.seed = seed;
            this.sideLean = sideLean;

            HorseSkinGeometry.Bounds barrel = HorseSkinGeometry.bounds(skin, Part.BODY);
            double length = Math.max(1e-6, barrel.span(Axis.X));
            double height = Math.max(1e-6, barrel.span(Axis.Y));
            // Frequencies off the barrel, not off absolute model units, so the
            // foal and the adult are marked at the same relative scale.
            this.scale = PATCHES_ALONG / length;
            this.yStretch = (PATCHES_TALL / height) / this.scale;
            this.jagFreq = JAG_PERIODS / length;
            this.halfWidth = Math.max(1e-6, barrel.span(Axis.Z) * 0.5);

            this.bodyCenter = barrel.yMin() + height * bandCenter;
            this.bodyUp = height * BAND_UP * reach;
            this.bodyDown = height * BAND_DOWN * reach;
            this.bodyCap = barrel.yMin() + height * TOPLINE_CAP;

            this.hasNeck = HorseSkinGeometry.hasPart(skin, Part.NECK);
            if (hasNeck) {
                HorseSkinGeometry.Bounds neck = HorseSkinGeometry.bounds(skin, Part.NECK);
                double neckHeight = Math.max(1e-6, neck.span(Axis.Y));
                this.neckCenter = neck.yMin() + neckHeight * neckCenterFrac;
                this.neckUp = neckHeight * NECK_UP * reach;
                this.neckDown = neckHeight * NECK_DOWN * reach;
                this.neckCap = neck.yMin() + neckHeight * NECK_CAP;
            } else {
                this.neckCenter = 0;
                this.neckUp = 1;
                this.neckDown = 1;
                this.neckCap = Double.NEGATIVE_INFINITY;
            }
        }

        /**
         * How strong a claim this texel has on white. Legs, mane, tail and ears
         * stay coloured outright; so does everything above the cap, which is
         * what keeps frame off the topline.
         */
        double score(Part part, HorseSkinGeometry.BodyPoint p) {
            double center;
            double up;
            double down;
            double cap;
            if (part == Part.BODY) {
                center = bodyCenter;
                up = bodyUp;
                down = bodyDown;
                cap = bodyCap;
            } else if (part == Part.NECK && hasNeck) {
                center = neckCenter;
                up = neckUp;
                down = neckDown;
                cap = neckCap;
            } else {
                return EXCLUDED;
            }
            if (p.y() >= cap) {
                return EXCLUDED;
            }
            // Distance from the middle of the side, in band widths - asymmetric,
            // because frame white drops toward the belly more readily than it
            // climbs toward the spine.
            double d = p.y() >= center ? (p.y() - center) / up : (center - p.y()) / down;
            double v = PatchNoise.field(seed, p.x(), p.y() * yStretch, p.z(), scale);
            double jag = JAG * (PatchNoise.fbm2(seed ^ 0x5AB0L,
                    p.x() * jagFreq, p.y() * jagFreq, p.z() * jagFreq * 1.4) - 0.5);
            // One flank is favoured over the other - a frame horse's two sides
            // genuinely do not match.
            double lean = SIDE_LEAN * sideLean * (p.z() / halfWidth);
            return v + jag + lean - EDGE_BIAS * d * d;
        }
    }

    /**
     * The occasional coronet band or white hoof. Drawn here rather than with
     * {@link CoatRegions#whitenLowerLeg} because that cut is a hard
     * {@code y <= cutoff} and ends in a perfect ring; frame's margins are torn
     * everywhere else on the horse and have no business being clean here.
     */
    private static void paintCoronets(Skin skin, PigmentField f, long seed, double[] rolls) {
        for (int i = 0; i < CoatRegions.LEGS.size(); i++) {
            Part leg = CoatRegions.LEGS.get(i);
            if (rolls[i] >= CORONET_CHANCE || !HorseSkinGeometry.hasPart(skin, leg)) {
                continue;
            }
            // Re-use the same roll for the height, rescaled off its own range so
            // a coronet that appears at all is not always the same size.
            double t = rolls[i] / CORONET_CHANCE;
            HorseSkinGeometry.Bounds b = HorseSkinGeometry.bounds(skin, leg);
            double span = b.span(Axis.Y);
            double cutoff = b.yMin() + span * (CORONET_MIN + t * (CORONET_MAX - CORONET_MIN));
            double wobble = span * CORONET_WOBBLE * (CORONET_MAX - CORONET_MIN);
            HorseSkinGeometry.forEachTexel(skin, leg, (px, py, part, face, point) -> {
                double edge = cutoff + wobble * (PatchNoise.fbm2(seed ^ (0xC0A5L + leg.ordinal()),
                        point.x() * 2.6, point.y() * 2.6, point.z() * 2.6) - 0.5);
                if (point.y() <= edge) {
                    whiten(f, px, py);
                }
            });
        }
    }

    /** Bald white - the transparent path. The shared move, at full strength. */
    private static void whiten(PigmentField f, int px, int py) {
        f.whiten(px, py, 1f);
    }

    /**
     * {@code O/O} is the lethal. A single frame copy is a white pattern and
     * nothing more - the horse is entirely healthy, which is exactly what makes
     * the locus dangerous to breed blind.
     */
    @Override
    public void contribute(AllelePair pair, Genotype genotype, TraitBuilder out) {
        if (pair.homozygousFor(O)) {
            out.condition(LETHAL_WHITE_SYNDROME).addHealth(-14.0).addSpeed(-0.05).addJump(-0.2);
        }
    }

    /**
     * Frame overos are commonly blue-eyed, and a lethal white foal always is -
     * it has no pigment anywhere at all. See {@link WhitePatternEyes}.
     */
    @Override
    public EyeRequest requestEyes(AllelePair pair, Genotype genotype,
            com.example.horsegenetics.common.genetics.Epigenome epigenome) {
        return WhitePatternEyes.blueIf(!expressionOf(pair).wildType(), this, pair, genotype, epigenome);
    }

    /**
     * Frame's own coverage range, at its midpoint - the horse's real roll is on
     * the coat and this runs before there is one. Lethal white is 1 and will
     * never be asked: that foal does not live to have eyes looked at.
     */
    @Override
    public double whiteness(AllelePair pair) {
        Expression e = expressionOf(pair);
        if (e == FRAME_OVERO) {
            return COVER_MIN + COVER_RANGE / 2.0;
        }
        if (e == LETHAL_WHITE) {
            return 1.0;
        }
        return 0.0;
    }

}
