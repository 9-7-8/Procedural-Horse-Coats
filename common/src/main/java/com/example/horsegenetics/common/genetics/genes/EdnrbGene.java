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
import com.example.horsegenetics.common.genetics.EyeColor;
import com.example.horsegenetics.common.genetics.EyeColorContribution;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.trait.Condition;
import com.example.horsegenetics.common.trait.HealthContribution;
import com.example.horsegenetics.common.trait.TraitBuilder;

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
 *   <li><b>Horizontal spread.</b> The patch field is sampled with {@code x}
 *       squashed and {@code y} stretched, so white runs <i>along</i> the barrel,
 *       flank, shoulder and lower neck instead of blotting it. Tobiano runs
 *       top-down over the back and splash comes bottom-up from the feet; frame
 *       goes sideways, and that is the most diagnostic thing about it.</li>
 *   <li><b>A dark back.</b> The ceiling is a fraction of
 *       {@link com.example.horsegenetics.common.coat.pattern.WhitePattern#toplineHeight}
 *       and never reaches 1.0, so the white does not cross the dorsal midline -
 *       which is the definition of the pattern, not a tuning choice.</li>
 *   <li><b>A white belly.</b> The floor sits <i>below</i> the underline: the
 *       lower barrel is one of the two hallmark locations and the white pushes
 *       up from it into the sides in separate tongues.</li>
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
 * <p>Natural. {@code frame-overo} is <b>non-deterministic</b>;
 * {@code lethal-white} is deterministic (it is total). See
 * {@code wiki/gene-ednrb.html}.
 */
public final class EdnrbGene implements Gene, HealthContribution, EyeColorContribution {

    public static final String KEY = "horsegenetics.ednrb";
    /** Founder frequency of {@code O}: one allele copy in this many. */
    public static final int WILD_FRAME_ONE_IN = 55;

    private static final double SCALE = 0.17;

    /**
     * <b>Frame's white is horizontal</b>, and this is what makes it so. The
     * patch field is sampled with {@code x} squashed and {@code y} stretched, so
     * one patch is roughly {@value #X_SQUASH} to {@value #Y_STRETCH} - about
     * five times longer along the horse than it is tall. That is the
     * single most diagnostic thing about the pattern: frame spreads
     * <i>sideways</i> along the barrel, flank, shoulder and neck, where tobiano
     * runs top-down over the back and splash comes bottom-up from the feet.
     * Sampled isotropically it read as a paint splat, which is every white gene.
     */
    private static final double X_SQUASH = 0.45;
    private static final double Y_STRETCH = 2.25;

    /**
     * How much of the band is white before the jagged edge, rolled per horse.
     * The range is deliberately enormous - a frame carrier can be
     * <b>cryptic</b>, showing little more than a bold face and a blue eye, and
     * that is not a rare curiosity but the reason the locus is DNA-tested rather
     * than eyeballed. At {@value #COVER_MIN} almost nothing clears the
     * threshold; at the top of the range the horse is a textbook frame.
     *
     * <p>The roll is <b>skewed toward the bold end</b> ({@value #COVER_GAMMA}),
     * because cryptic is the interesting minority and not the default: about
     * one frame horse in five comes out marked so lightly you would not call it
     * frame, and the rest look like the pattern.
     */
    private static final double COVER_MIN = 0.14;
    private static final double COVER_RANGE = 0.62;
    private static final double COVER_GAMMA = 0.65;

    /** Fine-scale threshold wobble - the source of the ragged, zig-zagged edge. */
    private static final double JAG = 0.26;
    private static final double JAG_FREQ = 3.4;

    /**
     * The band the barrel's white lives in, as fractions of
     * {@link WhitePattern#toplineHeight} - on which {@code 0.52} is the
     * underline and {@code 1.0} is the spine.
     *
     * <p><b>The floor is below the underline on purpose.</b> The belly is one of
     * the two hallmark locations (the flank is the other): the lower barrel goes
     * substantially white and pushes upward into the sides in separate tongues.
     * A band that started <i>above</i> the belly - which is what this gene used
     * to do - was drawing the one part of a frame horse that is reliably dark
     * and skipping the part that is reliably white.
     *
     * <p><b>The ceiling never reaches 1.0</b>, and that is the whole definition
     * of the pattern: viewed from the side, frame white does not cross the
     * dorsal midline. It is rolled per horse between {@value #CEILING_MIN} (the
     * low barrel only) and {@code CEILING_MIN + }{@value #CEILING_RANGE} (up
     * through the flank toward the hip), leaving the withers, spine and croup
     * dark either way.
     */
    private static final double BODY_FLOOR = 0.46;
    private static final double CEILING_MIN = 0.70;
    private static final double CEILING_RANGE = 0.22;

    /**
     * How far up the <b>neck's own</b> height white may climb. The neck needs
     * its own band because it stands above the barrel: measured in body-space Y
     * against the barrel's ceiling it is entirely out of range, which is why a
     * frame horse used to have a completely dark neck. Frame white runs along
     * the lower and middle neck - sometimes joining the face white to the body
     * white - and leaves the <b>upper crest dark</b>.
     */
    private static final double NECK_CEILING_MIN = 0.50;
    private static final double NECK_CEILING_RANGE = 0.26;

    /**
     * How hard one flank is favoured over the other. A frame horse's two sides
     * do not need to match at all - one can carry a bold hip patch while the
     * other is largely dark - so a per-horse lean is added to the threshold
     * across the width. The patch field is already asymmetric (it is sampled in
     * 3D), but only mildly; this is what makes a horse that is genuinely
     * <i>whiter on one side</i>.
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
            .describe("Sharp, jagged-edged white that spreads sideways along the belly, barrel, "
                    + "flank and lower neck and never crosses the back, so the coloured coat is "
                    + "left framing it above and below - which is where the name comes from. The "
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
                    field.setRed(px, py, 0f);
                    field.setBlack(px, py, 0f);
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
     * <b>The frame shape.</b> Dark legs, a bold face, irregular horizontally
     * spread white across the belly, barrel, flank and lower neck, and a dark
     * back - which is the visual template the whole pattern is recognised by,
     * and the reason it is called <i>frame</i>: the coloured coat is left above,
     * below, fore and aft of a white field, framing it like a window.
     *
     * <p><b>Draw order</b>, off {@code ctx.epigeneticsFor(KEY)}:
     * {@code nextLong()} (the noise seed), then {@code nextFloat()}s for the
     * face strength, the body cover, the barrel ceiling, the neck ceiling and
     * the side lean, then one per leg in {@link CoatRegions#LEGS} order, then
     * whatever {@link WhitePattern#faceMarking} takes. Every draw happens
     * whether or not it is used, so retuning one of them cannot move another.
     */
    private static PigmentField paintFrame(CoatBuildContext ctx, PigmentView coat) {
        Rng epi = ctx.epigeneticsFor(KEY);
        long seed = epi.nextLong();
        double faceStrength = FACE_MIN + epi.nextFloat() * FACE_RANGE;
        double cover = COVER_MIN + COVER_RANGE * Math.pow(epi.nextFloat(), COVER_GAMMA);
        double ceiling = CEILING_MIN + epi.nextFloat() * CEILING_RANGE;
        double neckCeiling = NECK_CEILING_MIN + epi.nextFloat() * NECK_CEILING_RANGE;
        double sideLean = (epi.nextFloat() - 0.5f) * 2.0;
        double[] coronets = new double[CoatRegions.LEGS.size()];
        for (int i = 0; i < coronets.length; i++) {
            coronets[i] = epi.nextFloat();
        }
        double threshold = 1.0 - cover;

        Skin skin = ctx.skin();
        WhitePattern.FaceMarking faceMark =
                WhitePattern.faceMarking(epi, skin, faceStrength, FACE_JAG);

        HorseSkinGeometry.Bounds bb = HorseSkinGeometry.bodyBounds(skin);
        // Every vertical number here is a fraction of the TOPLINE - the top of
        // the barrel - not of the whole-horse box, which runs to the ear tips
        // and on which 0.74 sits comfortably above the spine. See
        // WhitePattern.toplineHeight.
        double topline = WhitePattern.toplineHeight(skin);
        double bodyFloor = bb.yMin() + topline * BODY_FLOOR;
        double bodyCeiling = bb.yMin() + topline * ceiling;
        double feather = topline * 0.09;

        HorseSkinGeometry.Bounds neck = HorseSkinGeometry.hasPart(skin, Part.NECK)
                ? HorseSkinGeometry.bounds(skin, Part.NECK) : null;
        double neckTop = neck == null ? 0 : neck.yMin() + neck.span(Axis.Y) * neckCeiling;
        double neckFeather = neck == null ? 1 : neck.span(Axis.Y) * 0.16;

        double halfWidth = Math.max(1e-6, bb.span(Axis.Z) * 0.5);

        PigmentField f = coat.mutableCopy();
        HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> {
            if (part == Part.HEAD || part == Part.MUZZLE) {
                if (faceMark.covers(part, face, point)) {
                    whiten(f, px, py);
                }
                return;
            }
            if (part != Part.BODY && part != Part.NECK) {
                return; // legs, crest, tail and ears stay coloured; the legs get their own coronet below
            }
            // How much of a claim this texel has on white, 0 at the ceiling.
            // The barrel is open from below the underline upward; the neck is
            // measured against its own height so the upper crest stays dark.
            double side;
            if (part == Part.BODY) {
                side = PatchNoise.smoothstep(bodyFloor - feather, bodyFloor, point.y())
                        * (1.0 - PatchNoise.smoothstep(bodyCeiling - feather, bodyCeiling, point.y()));
            } else {
                side = neck == null ? 0
                        : 1.0 - PatchNoise.smoothstep(neckTop - neckFeather, neckTop, point.y());
            }
            if (side <= 0) {
                return;
            }
            // Horizontally stretched, so a patch runs along the horse rather
            // than blotting it: x squashed, y stretched, z left alone.
            double v = PatchNoise.field(seed,
                    point.x() * X_SQUASH, point.y() * Y_STRETCH, point.z(), SCALE);
            double jag = JAG * (PatchNoise.fbm2(seed ^ 0x5AB0L,
                    point.x() * JAG_FREQ, point.y() * JAG_FREQ, point.z() * JAG_FREQ * 1.4) - 0.5);
            // One flank is favoured over the other - a frame horse's two sides
            // genuinely do not match.
            double lean = SIDE_LEAN * sideLean * (point.z() / halfWidth);
            if (v + jag <= threshold + lean + (1.0 - side) * 0.6) {
                return; // near the ceiling the bar is much higher, so the margin frays out
            }
            whiten(f, px, py);
        });

        paintCoronets(skin, f, seed, coronets);
        return f;
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

    private static void whiten(PigmentField f, int px, int py) {
        f.setRed(px, py, 0f);
        f.setBlack(px, py, 0f);
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
    public java.util.Optional<EyeColor> eyeColor(AllelePair pair, Genotype genotype,
            com.example.horsegenetics.common.genetics.Epigenome epigenome, double whiteCoverage) {
        return WhitePatternEyes.blueIf(!expressionOf(pair).wildType(), whiteCoverage);
    }

}
