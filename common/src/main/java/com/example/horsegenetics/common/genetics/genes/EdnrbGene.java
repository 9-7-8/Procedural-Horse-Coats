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
 * <p>Natural. {@code frame} is <b>non-deterministic</b>; {@code lethal-white}
 * is deterministic (it is total). See {@code wiki/gene-ednrb.html}.
 */
public final class EdnrbGene implements Gene, HealthContribution {

    public static final String KEY = "horsegenetics.ednrb";
    /** Founder frequency of {@code O}: one allele copy in this many. */
    public static final int WILD_FRAME_ONE_IN = 55;

    private static final double SCALE = 0.19;
    /** How much of the flank band is white, before the jagged edge - deliberately high, frame is bold. */
    private static final double COVER_MIN = 0.52;
    private static final double COVER_RANGE = 0.22;
    /** Fine-scale threshold wobble - the source of the ragged edge. */
    private static final double JAG = 0.15;
    private static final double JAG_FREQ = 3.1;
    /** The flank band, as fractions of the body's height: above the belly, below the topline. */
    private static final double BAND_LO = 0.28;
    private static final double BAND_HI = 0.74;
    /**
     * How much white frame puts on the face, on {@link WhitePattern}'s shared
     * scale. Frame is the classically <b>bald-faced</b> pattern - a broad blaze
     * at the low end of the roll and an apron face at the high end - so it sits
     * near the top of the ramp, and the marking itself is drawn from the same
     * star / stripe / snip vocabulary every other white locus uses.
     */
    private static final double FACE_STRENGTH = 0.80;
    /** Frame's margins are torn, on the face as much as on the flank. */
    private static final double FACE_JAG = 0.34;

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

    private final Expression FRAME = Expression.of("frame", "Frame overo")
            .describe("Bold, jagged-edged white patches on the sides of the neck and barrel that "
                    + "never reach the topline, framed by colour above and below, plus a broad "
                    + "white face. The legs stay coloured. Some carriers are marked so little you "
                    + "would never guess, which is why frame is tested for and not eyeballed.")
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

    private final List<Expression> expressions = List.of(WILD, FRAME, LETHAL_WHITE);

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
        return pair.has(O) ? FRAME : WILD;
    }

    /** Does this combination carry frame at all - i.e. is it a carrier or worse? */
    public boolean isFrame(AllelePair pair) {
        return pair.has(O);
    }

    /** Is this the homozygous lethal-white combination? */
    public boolean isLethalWhite(AllelePair pair) {
        return pair.homozygousFor(O);
    }

    private static PigmentField paintFrame(CoatBuildContext ctx, PigmentView coat) {
        Rng epi = ctx.epigeneticsFor(KEY);
        long seed = epi.nextLong();
        double cover = COVER_MIN + epi.nextFloat() * COVER_RANGE;
        double threshold = 1.0 - cover;

        Skin skin = ctx.skin();
        WhitePattern.FaceMarking faceMark =
                WhitePattern.faceMarking(epi, skin, FACE_STRENGTH, FACE_JAG);

        HorseSkinGeometry.Bounds bb = HorseSkinGeometry.bodyBounds(skin);
        double span = bb.span(Axis.Y);
        double bandLo = bb.yMin() + span * BAND_LO;
        double bandHi = bb.yMin() + span * BAND_HI;
        double feather = span * 0.10;

        PigmentField f = coat.mutableCopy();
        HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> {
            if (part == Part.HEAD || part == Part.MUZZLE) {
                if (faceMark.covers(part, face, point)) {
                    f.setRed(px, py, 0f);
                    f.setBlack(px, py, 0f);
                }
                return;
            }
            if (part != Part.BODY && part != Part.NECK) {
                return; // legs, crest, tail, ears stay coloured - white never reaches the topline
            }
            // The flank band, in absolute body-space Y so it does not depend on
            // which face a texel is on: zero at the belly and at the topline.
            double side = PatchNoise.smoothstep(bandLo - feather, bandLo, point.y())
                    * (1.0 - PatchNoise.smoothstep(bandHi, bandHi + feather, point.y()));
            if (side <= 0) {
                return;
            }
            double v = PatchNoise.field(seed, point.x(), point.y(), point.z(), SCALE);
            double jag = JAG * (PatchNoise.fbm2(seed ^ 0x5AB0L,
                    point.x() * JAG_FREQ, point.y() * JAG_FREQ, point.z() * JAG_FREQ * 1.4) - 0.5);
            if (v + jag <= threshold + (1.0 - side) * 0.6) {
                return; // outside the band the bar has to clear a much higher threshold
            }
            f.setRed(px, py, 0f);
            f.setBlack(px, py, 0f);
        });
        return f;
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
}
