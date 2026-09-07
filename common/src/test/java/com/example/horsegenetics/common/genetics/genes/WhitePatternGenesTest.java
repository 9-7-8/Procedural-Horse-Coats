package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.coat.pattern.CoatRegions;
import com.example.horsegenetics.common.coat.pattern.CoatTextureComposer;
import com.example.horsegenetics.common.coat.pattern.GradientLut;
import com.example.horsegenetics.common.coat.pattern.WhitePattern;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.GenotypeCatalog;
import com.example.horsegenetics.common.testutil.Codes;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The four <b>white-pattern loci</b> - {@code KIT}, {@code MITF}, {@code PAX3}
 * and {@code EDNRB} - as a set, because the point of splitting them the way
 * real genetics does is a set of claims about how they interact, and each one
 * is only checkable across two genes.
 *
 * <p>What is pinned here:
 * <ul>
 *   <li>each locus's combination table is <b>total</b> and every declared
 *       outcome is actually reachable;</li>
 *   <li>the homozygous combinations the source calls nonviable cannot occur,
 *       and the ones it calls viable can;</li>
 *   <li>the ladder is <b>monotone</b> - more white per step, every step;</li>
 *   <li>the two splash loci <b>stack</b>, which is the whole reason splash is
 *       two genes here;</li>
 *   <li>{@code W20} <b>boosts</b> rather than acting alone;</li>
 *   <li>{@code EDNRB}'s {@code O/O} is a real, occurring, all-white
 *       combination that is simply absent from the founder population.</li>
 * </ul>
 */
class WhitePatternGenesTest {

    private static final List<Gene> LOCI = List.of(Genes.KIT, Genes.MITF, Genes.PAX3, Genes.EDNRB);

    // ------------------------------------------------------------------
    // The tables
    // ------------------------------------------------------------------

    /**
     * Every combination of every locus lands on a declared outcome. Nothing may
     * throw and nothing may return an expression the gene does not list - not
     * even the combinations {@link Gene#canOccur} rules out, because parsing is
     * tolerant and a hand-written code can still name one.
     */
    @Test
    void everyCombinationOfEveryLocusLandsOnADeclaredOutcome() {
        for (Gene gene : LOCI) {
            Set<Expression> declared = new HashSet<>(gene.expressions());
            int seen = 0;
            for (Allele a : gene.alleles()) {
                for (Allele b : gene.alleles()) {
                    if (a.order() > b.order()) {
                        continue;
                    }
                    Expression e = gene.expressionOf(new AllelePair(a, b));
                    assertTrue(declared.contains(e),
                            gene.key() + " " + a.token() + "/" + b.token() + " -> undeclared " + e);
                    seen++;
                }
            }
            int n = gene.alleles().size();
            assertEquals(n * (n + 1) / 2, seen, gene.key() + " should answer for every combination");
        }
    }

    /** No locus declares an outcome no combination can reach - dead rows lie to the wiki. */
    @Test
    void everyDeclaredOutcomeIsReachableFromACombinationAHorseCanCarry() {
        for (Gene gene : LOCI) {
            Set<Expression> reached = new HashSet<>();
            for (AllelePair pair : GenotypeCatalog.allPairsOf(gene)) {
                reached.add(gene.expressionOf(pair));
            }
            assertEquals(new HashSet<>(gene.expressions()), reached,
                    gene.key() + " declares an outcome no carryable combination reaches");
        }
    }

    /**
     * KIT has eight alleles, so thirty-six combinations - four of them the
     * homozygotes UC Davis lists as thought nonviable, leaving thirty-two a
     * horse can carry and eight distinct looks.
     */
    @Test
    void kitIsThirtySixCombinationsThirtyTwoCarryableAndEightOutcomes() {
        assertEquals(8, Genes.KIT.alleles().size());
        assertEquals(32, GenotypeCatalog.allPairsOf(Genes.KIT).size());
        assertEquals(8, Genes.KIT.expressions().size());
        assertEquals(8, GenotypeCatalog.distinctPairsOf(Genes.KIT).size());
    }

    // ------------------------------------------------------------------
    // Viability
    // ------------------------------------------------------------------

    /**
     * The homozygote of a strong {@code W} is an <b>embryonic</b> lethal - no
     * such horse - while {@code W20} and {@code SB1} double up perfectly well,
     * and a <b>compound</b> heterozygote of two strong alleles is a real horse.
     * That last distinction is the one the source is explicit about: the risk
     * is the same variant twice, not two strong variants.
     */
    @Test
    void onlyTheSameStrongKitAlleleTwiceIsRuledOut() {
        KitGene kit = Genes.KIT;
        for (Allele lethal : List.of(kit.W22, kit.W13, kit.W10, kit.W5)) {
            assertFalse(kit.canOccur(new AllelePair(lethal, lethal)), lethal.token() + "/" + lethal.token());
        }
        for (Allele viable : List.of(kit.W23, kit.SB1, kit.W20, kit.N)) {
            assertTrue(kit.canOccur(new AllelePair(viable, viable)), viable.token() + "/" + viable.token());
        }
        assertTrue(kit.canOccur(new AllelePair(kit.W22, kit.W5)));
        assertTrue(kit.canOccur(new AllelePair(kit.W13, kit.W10)));
    }

    /** The same rule, one locus over: {@code SW3/SW3} and {@code SW4/SW4} have never been seen. */
    @Test
    void theUnseenSplashHomozygotesCannotOccur() {
        assertFalse(Genes.MITF.canOccur(new AllelePair(Genes.MITF.SW3, Genes.MITF.SW3)));
        assertTrue(Genes.MITF.canOccur(new AllelePair(Genes.MITF.SW1, Genes.MITF.SW1)));
        assertFalse(Genes.PAX3.canOccur(new AllelePair(Genes.PAX3.SW4, Genes.PAX3.SW4)));
        assertTrue(Genes.PAX3.canOccur(new AllelePair(Genes.PAX3.SW2, Genes.PAX3.SW2)));
    }

    /**
     * Lethal white is the other kind of lethal, and the model keeps them apart.
     * An {@code O/O} foal is <b>born</b>, so it occurs, it has a look of its
     * own, and it gets a gallery pen - it is simply never a founder, because a
     * founder is an adult horse and no adult horse is {@code O/O}.
     */
    @Test
    void lethalWhiteOccursAndIsAllWhiteButIsNeverAFounder() {
        AllelePair oo = new AllelePair(Genes.EDNRB.O, Genes.EDNRB.O);
        assertTrue(Genes.EDNRB.canOccur(oo), "an O/O foal is born - it is not an embryonic lethal");
        assertTrue(Genes.EDNRB.isLethalWhite(oo));
        assertTrue(Genes.EDNRB.expressionOf(oo).masks(), "lethal white hides every other gene");
        assertEquals(0.0, Genes.EDNRB.founderTable(null).share(oo),
                "no wild-caught horse is homozygous frame");
        assertTrue(Genes.EDNRB.founderTable(null).share(new AllelePair(Genes.EDNRB.O, Genes.EDNRB.N)) > 0,
                "carriers are what the wild population has");
        // ...and it really is white, not just labelled so
        assertEquals(1.0, whiteFraction(Codes.of("ednrb", "O/O"), 7L), 0.01);
    }

    // ------------------------------------------------------------------
    // The ladder
    // ------------------------------------------------------------------

    /**
     * KIT's outcomes get steadily whiter, in the order the class documents
     * them. A ladder that is not monotone is a ladder whose steps a player
     * cannot read, and it would mean an allele's "stronger" description was
     * decoration.
     */
    @Test
    void theKitLadderGetsWhiterAtEveryStep() {
        String[][] rungs = {
                {"N/N", "wild"},
                {"W20/N", "minimal-white"},
                {"W20/W20", "modest-white"},
                {"SB1/N", "sabino"},
                {"SB1/W20", "broad-white"},
                {"W13/N", "extensive-white"},
                {"SB1/SB1", "near-white"},
                {"W22/N", "dominant-white"},
        };
        double previous = -1;
        for (String[] rung : rungs) {
            assertEquals(rung[1], Genes.KIT.expressionOf(pairOf(Genes.KIT, rung[0])).id(), rung[0]);
            double white = averageWhite(Codes.of("kit", rung[0]));
            assertTrue(white > previous + 0.02,
                    "kit " + rung[0] + " (" + rung[1] + ") is " + white + ", not whiter than " + previous);
            previous = white;
        }
        assertTrue(previous > 0.99, "dominant white should be the whole horse");
    }

    /** Ninety per cent white or more is what "sabino-white" means, and it has to be true. */
    @Test
    void sabinoWhiteIsAtLeastNinetyPerCentWhite() {
        assertTrue(averageWhite(Codes.of("kit", "SB1/SB1")) >= 0.90);
    }

    // ------------------------------------------------------------------
    // Interaction - the reason for the split
    // ------------------------------------------------------------------

    /**
     * <b>The headline claim.</b> {@code MITF} and {@code PAX3} are different
     * genes, so a horse carries one copy at each and comes out markedly whiter
     * than either alone. A single splash gene could not express this genotype
     * at all; a two-gene model that painted blindly would express it and get it
     * wrong, because two waterlines at the same height are one waterline.
     */
    @Test
    void aHorseSplashAtBothLociIsWhiterThanEitherAlone() {
        double mitf = averageWhite(Codes.of("mitf", "SW1/N"));
        double pax3 = averageWhite(Codes.of("pax3", "SW2/N"));
        double both = averageWhite(Codes.of("mitf", "SW1/N", "pax3", "SW2/N"));
        assertTrue(both > Math.max(mitf, pax3) + 0.15,
                "SW1 + SW2 came out " + both + ", barely more than " + mitf + " / " + pax3);
    }

    /**
     * {@code W20} is a booster, not a pattern: subtle on its own, and visibly
     * more white beside {@code SB1} than {@code SB1} manages alone.
     */
    @Test
    void w20IsSubtleAloneAndBoostsSabinoBesideIt() {
        double w20 = averageWhite(Codes.of("kit", "W20/N"));
        double sabino = averageWhite(Codes.of("kit", "SB1/N"));
        double boosted = averageWhite(Codes.of("kit", "SB1/W20"));
        assertTrue(w20 < 0.10, "one W20 copy should be ordinary markings, got " + w20);
        assertTrue(boosted > sabino + 0.05, "SB1/W20 (" + boosted + ") should beat SB1/N (" + sabino + ")");
    }

    /**
     * A splash horse that is also frame is louder than either - white finds
     * white, across loci, with no interaction table anywhere in the model.
     */
    @Test
    void splashOverFrameIsWhiterThanEither() {
        double frame = averageWhite(Codes.of("ednrb", "O/N"));
        double splash = averageWhite(Codes.of("mitf", "SW1/N"));
        double both = averageWhite(Codes.of("ednrb", "O/N", "mitf", "SW1/N"));
        assertTrue(both > Math.max(frame, splash) + 0.10,
                "frame + splash came out " + both + " against " + frame + " / " + splash);
    }

    /**
     * Tobiano and roan are <b>not</b> {@code KIT} alleles - they are their own
     * genes - so a horse can carry them alongside anything at this locus. If
     * they were ever folded in, this genotype would stop parsing.
     */
    @Test
    void tobianoAndRoanStillComposeFreelyWithKit() {
        Genotype g = Genotype.parse(Codes.of("kit", "SB1/N", "tobiano", "To/to", "roan", "Rn/rn"));
        assertTrue(g.shows(Genes.KIT));
        assertTrue(g.shows(Genes.TOBIANO));
        assertTrue(g.shows(Genes.ROAN));
    }

    /**
     * A splash horse always wears a bold face marking. The shared face
     * vocabulary at plain splash strength lands on stars and snips; splash
     * boosts it (see {@code WhitePattern.SPLASH_FACE_BOOST}) so even a
     * single-copy splash reads as a blaze, and a homozygote as a bald face -
     * the splash phenotype. Regression guard for "splash puts nothing on the
     * face".
     */
    @Test
    void everySplashHorseWearsAFaceMarking() {
        for (String code : new String[]{Codes.of("mitf", "SW1/N"), Codes.of("pax3", "SW2/N")}) {
            for (long seed : new long[]{0L, 1L, 2L, 3L, 7L, 42L, 99L, 4242L}) {
                double f = faceWhite(code, seed);
                assertTrue(f > 0.08,
                        "single-copy splash left the face nearly bare (" + f + ") for " + code + " seed " + seed);
            }
        }
        // homozygous / two-locus splash goes bald-faced - much more than one copy
        double one = faceWhite(Codes.of("mitf", "SW1/N"), 3L);
        double hom = faceWhite(Codes.of("mitf", "SW1/SW1"), 3L);
        double both = faceWhite(Codes.of("mitf", "SW1/N", "pax3", "SW2/N"), 3L);
        assertTrue(hom > one + 0.15, "SW1/SW1 face (" + hom + ") not much bolder than SW1/N (" + one + ")");
        assertTrue(both > one + 0.10, "two-locus splash face (" + both + ") not bolder than one copy (" + one + ")");
    }

    /** White fraction over the head + muzzle only, one epigenetic seed. */
    private static double faceWhite(String code, long seed) {
        int n = HorseSkinGeometry.SHEET_SIZE;
        int[] template = new int[n * n];
        HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, part, face, point) ->
                template[py * n + px] = 0xFFFFFFFF);
        int[] lut = new int[16 * 16];
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int shade = 255 - Math.round(y / 15f * 255);
                lut[y * 16 + x] = 0xFF000000 | (shade << 16) | (shade << 8) | shade;
            }
        }
        int[] img = CoatTextureComposer.compose(Genotype.parse(code), Epigenome.fromSeed(seed),
                Skin.ADULT, true, template, new GradientLut(lut, 16, 16));
        int[] tally = new int[2];
        HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, part, face, point) -> {
            if (part != HorseSkinGeometry.Part.HEAD && part != HorseSkinGeometry.Part.MUZZLE) {
                return;
            }
            tally[1]++;
            if ((img[py * n + px] & 0xFFFFFF) > 0xE0E0E0) {
                tally[0]++;
            }
        });
        return tally[1] == 0 ? 0 : tally[0] / (double) tally[1];
    }

    // ------------------------------------------------------------------
    // Frame overo's shape
    // ------------------------------------------------------------------

    /** The seeds the frame-shape tests are measured over. */
    private static final long[] FRAME_SEEDS = {0L, 1L, 2L, 3L, 5L, 8L, 13L, 21L, 34L, 55L, 4242L};

    private static final String FRAME = Codes.of("agouti", "A/a", "ednrb", "O/N");

    /**
     * <b>Frame's white does not cross the back.</b> This is the definition of
     * the pattern rather than a tuning preference - "doesn't cross the back"
     * is how frame is told from tobiano at a glance - so the painter enforces
     * it with a hard exclusion above {@code EdnrbGene.TOPLINE_CAP} rather than
     * with a ramp that merely discourages it. It has been broken twice: once
     * by measuring the ceiling against the whole-horse box, which runs to the
     * <i>ear tips</i>, and once by a threshold so far outside the noise field's
     * range that the white flooded up to whatever ceiling it was given.
     */
    @Test
    void frameWhiteNeverReachesTheTopline() {
        for (long seed : FRAME_SEEDS) {
            double spine = bodyBandWhite(FRAME, seed, 0.93, 1.01);
            assertEquals(0.0, spine, 0.001,
                    "frame put white on the spine at seed " + seed + " (" + spine + ")");
        }
    }

    /**
     * <b>Frame white is framed above and below</b>, which is the shape of the
     * pattern and the reason for its name: big splotches in the middle of the
     * side, with coloured coat left over the back <i>and</i> under the belly.
     *
     * <p>This is the test that was missing, and its absence is why the gene
     * shipped drawing a horse dipped in white to a frayed waterline. The
     * assertion it replaces demanded the opposite - a belly whiter than the
     * flank - which is a description of a dipped horse rather than of a frame
     * one. Frame's white does reach the belly here and there, at the bottom of
     * its boldest patches; what it does not do is <i>start</i> there.
     */
    @Test
    void frameWhiteIsFramedAboveAndBelow() {
        double belly = 0;
        double middle = 0;
        double upper = 0;
        for (long seed : FRAME_SEEDS) {
            double b = bodyBandWhite(FRAME, seed, 0.40, 0.62);
            double m = bodyBandWhite(FRAME, seed, 0.62, 0.85);
            assertTrue(m > b, "frame pooled in the belly rather than the side at seed " + seed
                    + ": belly " + b + " against middle " + m);
            belly += b;
            middle += m;
            upper += bodyBandWhite(FRAME, seed, 0.85, 0.93);
        }
        belly /= FRAME_SEEDS.length;
        middle /= FRAME_SEEDS.length;
        upper /= FRAME_SEEDS.length;
        assertTrue(middle > 0.30,
                "the middle of the side is where frame lives, got " + middle);
        assertTrue(middle > belly + 0.25,
                "frame is not a dipped horse: belly " + belly + " against middle " + middle);
        assertTrue(middle > upper + 0.25,
                "frame is framed above too: upper barrel " + upper + " against middle " + middle);
    }

    /**
     * <b>The legs stay dark and the crest stays dark.</b> Four dark legs plus a
     * bold face is the frame template; extensive leg white on a frame horse
     * means it is carrying splash, sabino or tobiano as well. A coronet band or
     * a white hoof is allowed, which is what the tolerance is for.
     */
    @Test
    void frameLeavesTheLegsAndTheUpperNeckDark() {
        for (long seed : FRAME_SEEDS) {
            double legs = partWhite(FRAME, seed, CoatRegions.LEGS.toArray(new Part[0]));
            assertTrue(legs < 0.10, "frame put " + legs + " white on the legs at seed " + seed);
            double crest = neckBandWhite(FRAME, seed, 0.80, 1.01);
            assertTrue(crest < 0.15, "frame put " + crest + " white on the crest at seed " + seed);
        }
    }

    /**
     * <b>The white runs along the horse, not up it.</b> Tobiano runs top-down
     * over the back and splash comes bottom-up from the feet; frame's splotches
     * are wider than they are tall, which is what the patch field's two period
     * counts ({@code EdnrbGene.PATCHES_ALONG} against
     * {@code PATCHES_TALL}) buy. This is a claim about the <i>patches</i>, not
     * about the pattern as a whole - a frame horse that is elongated because it
     * has been dipped to a waterline passes this and fails
     * {@link #frameWhiteIsFramedAboveAndBelow}, which is why both exist.
     *
     * <p>Measured as edge anisotropy on the finished coat, over the barrel's
     * two <b>side</b> faces only - the ones whose sheet axes are the horse's
     * length and height, so no unprojection is needed. (The belly face's
     * vertical axis is the horse's <i>width</i>, which would dilute the
     * measurement with a direction the test is not about.) Scanning a
     * horizontally elongated patch across its long axis crosses fewer margins
     * than scanning it up and down, so a body whose white is stretched
     * lengthways has <b>fewer horizontal transitions than vertical ones</b>.
     */
    @Test
    void frameWhiteIsWiderThanItIsTall() {
        int horizontal = 0;
        int vertical = 0;
        for (long seed : FRAME_SEEDS) {
            int[] counts = bodyEdgeCounts(FRAME, seed);
            horizontal += counts[0];
            vertical += counts[1];
        }
        assertTrue(vertical > horizontal * 1.25,
                "frame's patches are not horizontally elongated: " + horizontal
                        + " horizontal edges against " + vertical + " vertical");
    }

    /**
     * <b>Cryptic at one end, textbook at the other.</b> "No obvious frame
     * pattern" does not rule out a carrier, and that is not a curiosity - it is
     * why the locus is DNA-tested rather than eyeballed, and the mod would be
     * lying about the gene if every {@code O/N} horse were obviously frame.
     * The face is rolled separately, so even a nearly-unmarked body comes with
     * a bold face and a blue eye, which is exactly how a cryptic frame is
     * spotted.
     *
     * <p>Measured over the <b>barrel and neck</b> rather than the whole hide,
     * because that is the surface frame can mark: the legs, mane and tail are
     * excluded by the painter, so including them only dilutes every reading by
     * the same constant and makes the thresholds harder to reason about.
     */
    @Test
    void frameRunsFromCrypticToTextbook() {
        double least = 1;
        double most = 0;
        for (long seed : FRAME_SEEDS) {
            double w = sideWhite(FRAME, seed);
            least = Math.min(least, w);
            most = Math.max(most, w);
        }
        assertTrue(least < 0.12, "no seed produced a cryptic frame (least was " + least + ")");
        assertTrue(most > 0.22, "no seed produced a bold frame (most was " + most + ")");
        for (long seed : FRAME_SEEDS) {
            assertTrue(faceWhite(FRAME, seed) > 0.15,
                    "every frame horse wears a bold face, seed " + seed);
        }
    }

    /** White fraction of the surface frame can mark - the barrel and the neck. */
    private static double sideWhite(String code, long seed) {
        int[] img = composeAdult(code, seed);
        int[] tally = new int[2];
        int n = HorseSkinGeometry.SHEET_SIZE;
        HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, part, face, point) -> {
            if (part != Part.BODY && part != Part.NECK) {
                return;
            }
            tally[1]++;
            if ((img[py * n + px] & 0xFFFFFF) > 0xE0E0E0) {
                tally[0]++;
            }
        });
        return tally[1] == 0 ? 0 : tally[0] / (double) tally[1];
    }

    // ------------------------------------------------------------------
    // Tobiano's shape
    // ------------------------------------------------------------------

    /**
     * Tobiano is measured over a wider seed set than frame because the claims
     * below are about the <b>population</b> - an individual tobiano can carry a
     * little more white on its side than over its back without being wrong.
     */
    private static final long[] TOBIANO_SEEDS = new long[24];

    static {
        for (int i = 0; i < TOBIANO_SEEDS.length; i++) {
            TOBIANO_SEEDS[i] = i;
        }
    }

    private static final String TOBIANO = Codes.of("agouti", "A/a", "tobiano", "To/to");

    /**
     * <b><code>cover</code> is an area fraction, and this is the test that says
     * so.</b> The knob rolls between {@code TobianoGene.COVER_MIN} and
     * {@code COVER_MIN + COVER_RANGE}; the coat that comes out has to land in
     * that band and has to reach both ends of it.
     *
     * <p>It did not, and nothing here noticed. The roll used to feed a bare
     * {@code 1 - cover} threshold on {@link PatchNoise#field}, which concentrates
     * near {@code 0.5}, so a range written as {@code 0.40}-{@code 0.56} - which
     * reads like a 1.4x swing - delivered <b>31% to 86%</b> coverage. The
     * threshold is a quantile of the horse's own field now, which makes the
     * relationship an identity rather than a hope; this test is what keeps it
     * one when the field or the geometry moves underneath.
     *
     * <p>The same defect in {@link EdnrbGene} produced a horse dipped in white,
     * and the same absence of a test let it ship twice.
     */
    @Test
    void tobianoCoverIsAnAreaFractionAndNotAThresholdGuess() {
        double lo = TobianoGene.COVER_MIN;
        double hi = TobianoGene.COVER_MIN + TobianoGene.COVER_RANGE;
        double slack = 0.03;   // one texel's worth of rounding, not a swing
        double least = 1;
        double most = 0;
        for (long seed : TOBIANO_SEEDS) {
            double w = eligibleWhite(TOBIANO, seed);
            assertTrue(w > lo - slack && w < hi + slack,
                    "tobiano coverage escaped the band its knob declares (" + lo + ".." + hi
                            + ") at seed " + seed + ": " + w);
            least = Math.min(least, w);
            most = Math.max(most, w);
        }
        // and the range is actually exercised, not merely respected
        assertTrue(least < lo + 0.20, "no modestly marked tobiano (least was " + least + ")");
        assertTrue(most > hi - 0.15, "no loudly marked tobiano (most was " + most + ")");
    }

    /**
     * <b>Tobiano crosses the topline.</b> This is the shape that tells it from
     * frame overo at a glance, and the two tests are deliberate mirrors:
     * frame's white is <em>framed</em> by colour above and below
     * ({@link #frameWhiteIsFramedAboveAndBelow}), tobiano's runs up and over the
     * back.
     *
     * <p>The comparison is over means because it is a claim about the pattern
     * and not about every horse: a given tobiano can be a little whiter on the
     * barrel than over the spine. What no tobiano does is have a
     * <em>dark</em> back, so that part is asserted per seed.
     */
    @Test
    void tobianoCrossesTheTopline() {
        double top = 0;
        double middle = 0;
        for (long seed : TOBIANO_SEEDS) {
            double t = bodyBandWhite(TOBIANO, seed, 0.90, 1.01);
            assertTrue(t > 0.15, "tobiano left the back dark at seed " + seed + ": " + t);
            top += t;
            middle += bodyBandWhite(TOBIANO, seed, 0.62, 0.85);
        }
        top /= TOBIANO_SEEDS.length;
        middle /= TOBIANO_SEEDS.length;
        assertTrue(top > 0.50, "tobiano's back should be substantially white, got " + top);
        assertTrue(top > middle + 0.06,
                "tobiano is topline-weighted: back " + top + " against middle " + middle);
    }

    /**
     * <b>White legs, coloured head.</b> The other half of the tobiano template.
     * The head is excluded from the painter outright, so the tolerance is for
     * the white of the eye in the template rather than for any patch.
     */
    @Test
    void tobianoWhitensTheLegsAndLeavesTheHeadColoured() {
        double legs = 0;
        for (long seed : TOBIANO_SEEDS) {
            double l = partWhite(TOBIANO, seed, CoatRegions.LEGS.toArray(new Part[0]));
            assertTrue(l > 0.30, "tobiano's legs tend to white, got " + l + " at seed " + seed);
            legs += l;
            double head = partWhite(TOBIANO, seed, Part.HEAD, Part.MUZZLE,
                    Part.LEFT_EAR, Part.RIGHT_EAR);
            assertTrue(head < 0.05,
                    "tobiano keeps the head coloured, got " + head + " at seed " + seed);
        }
        legs /= TOBIANO_SEEDS.length;
        assertTrue(legs > 0.55, "tobiano's legs should be mostly white, got " + legs);
    }

    /** White fraction of everything tobiano is allowed to mark - all but the head. */
    private static double eligibleWhite(String code, long seed) {
        int[] img = composeAdult(code, seed);
        int[] tally = new int[2];
        int n = HorseSkinGeometry.SHEET_SIZE;
        HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, part, face, point) -> {
            if (part == Part.HEAD || part == Part.MUZZLE
                    || part == Part.LEFT_EAR || part == Part.RIGHT_EAR) {
                return;
            }
            tally[1]++;
            if ((img[py * n + px] & 0xFFFFFF) > 0xE0E0E0) {
                tally[0]++;
            }
        });
        return tally[1] == 0 ? 0 : tally[0] / (double) tally[1];
    }

    /** White fraction of the BODY part between two fractions of the topline. */
    private static double bodyBandWhite(String code, long seed, double lo, double hi) {
        int[] img = composeAdult(code, seed);
        double topline = WhitePattern.toplineHeight(Skin.ADULT);
        double y0 = HorseSkinGeometry.bodyBounds(Skin.ADULT).yMin();
        int[] tally = new int[2];
        int n = HorseSkinGeometry.SHEET_SIZE;
        HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, part, face, point) -> {
            if (part != Part.BODY) {
                return;
            }
            double h = (point.y() - y0) / topline;
            if (h < lo || h >= hi) {
                return;
            }
            tally[1]++;
            if ((img[py * n + px] & 0xFFFFFF) > 0xE0E0E0) {
                tally[0]++;
            }
        });
        return tally[1] == 0 ? 0 : tally[0] / (double) tally[1];
    }

    /** White fraction of the NECK part between two fractions of the neck's own height. */
    private static double neckBandWhite(String code, long seed, double lo, double hi) {
        int[] img = composeAdult(code, seed);
        HorseSkinGeometry.Bounds nb = HorseSkinGeometry.bounds(Skin.ADULT, Part.NECK);
        double span = nb.span(HorseSkinGeometry.Axis.Y);
        int[] tally = new int[2];
        int n = HorseSkinGeometry.SHEET_SIZE;
        HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, part, face, point) -> {
            if (part != Part.NECK) {
                return;
            }
            double h = (point.y() - nb.yMin()) / span;
            if (h < lo || h >= hi) {
                return;
            }
            tally[1]++;
            if ((img[py * n + px] & 0xFFFFFF) > 0xE0E0E0) {
                tally[0]++;
            }
        });
        return tally[1] == 0 ? 0 : tally[0] / (double) tally[1];
    }

    /** White fraction over a set of parts. */
    private static double partWhite(String code, long seed, Part... parts) {
        int[] img = composeAdult(code, seed);
        Set<Part> want = new HashSet<>(List.of(parts));
        int[] tally = new int[2];
        int n = HorseSkinGeometry.SHEET_SIZE;
        HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, part, face, point) -> {
            if (!want.contains(part)) {
                return;
            }
            tally[1]++;
            if ((img[py * n + px] & 0xFFFFFF) > 0xE0E0E0) {
                tally[0]++;
            }
        });
        return tally[1] == 0 ? 0 : tally[0] / (double) tally[1];
    }

    /**
     * {@code {horizontal, vertical}} white/coloured transitions across the BODY
     * texels - the anisotropy measure {@link #frameWhiteIsWiderThanItIsTall}
     * reads.
     */
    private static int[] bodyEdgeCounts(String code, long seed) {
        int n = HorseSkinGeometry.SHEET_SIZE;
        int[] img = composeAdult(code, seed);
        boolean[] body = new boolean[n * n];
        boolean[] white = new boolean[n * n];
        HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, part, face, point) -> {
            if (part != Part.BODY) {
                return;
            }
            if (face != HorseSkinGeometry.Face.LEFT && face != HorseSkinGeometry.Face.RIGHT) {
                return; // only the side faces map (length, height) to (u, v)
            }
            body[py * n + px] = true;
            white[py * n + px] = (img[py * n + px] & 0xFFFFFF) > 0xE0E0E0;
        });
        int[] counts = new int[2];
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                int i = y * n + x;
                if (!body[i]) {
                    continue;
                }
                if (x + 1 < n && body[i + 1] && white[i] != white[i + 1]) {
                    counts[0]++;
                }
                if (y + 1 < n && body[i + n] && white[i] != white[i + n]) {
                    counts[1]++;
                }
            }
        }
        return counts;
    }

    private static int[] composeAdult(String code, long seed) {
        int n = HorseSkinGeometry.SHEET_SIZE;
        int[] template = new int[n * n];
        HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, part, face, point) ->
                template[py * n + px] = 0xFFFFFFFF);
        int[] lut = new int[16 * 16];
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int shade = 255 - Math.round(y / 15f * 255);
                lut[y * 16 + x] = 0xFF000000 | (shade << 16) | (shade << 8) | shade;
            }
        }
        return CoatTextureComposer.compose(Genotype.parse(code), Epigenome.fromSeed(seed),
                Skin.ADULT, true, template, new GradientLut(lut, 16, 16));
    }

    // ------------------------------------------------------------------

    private static AllelePair pairOf(Gene gene, String tokens) {
        String[] p = tokens.split("/");
        return new AllelePair(gene.fromToken(p[0]), gene.fromToken(p[1]));
    }

    /** Mean white coverage over three epigenetic seeds, so one unlucky roll cannot decide a rung. */
    /**
     * Mean white fraction over a spread of epigenetic seeds. Eight rather than
     * a handful because the non-deterministic white painters vary a few per
     * cent seed to seed, and a three-seed mean can sit a knife-edge off a real
     * threshold purely on which seeds were picked (it did, once the gene set
     * shifted the seed stream).
     */
    private static double averageWhite(String code) {
        long[] seeds = {0L, 1L, 2L, 3L, 5L, 8L, 13L, 4242L};
        double sum = 0;
        for (long s : seeds) {
            sum += whiteFraction(code, s);
        }
        return sum / seeds.length;
    }

    /**
     * The fraction of mapped texels the composed coat leaves as bare template -
     * i.e. actually white. Measured through the real pipeline against a white
     * template and a grey LUT, so it is what the horse looks like and not what
     * the pigment field says.
     */
    private static double whiteFraction(String code, long seed) {
        int n = HorseSkinGeometry.SHEET_SIZE;
        int[] template = new int[n * n];
        HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, part, face, point) ->
                template[py * n + px] = 0xFFFFFFFF);

        int[] lut = new int[16 * 16];
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int shade = 255 - Math.round(y / 15f * 255);
                lut[y * 16 + x] = 0xFF000000 | (shade << 16) | (shade << 8) | shade;
            }
        }

        int[] img = CoatTextureComposer.compose(Genotype.parse(code), Epigenome.fromSeed(seed),
                Skin.ADULT, true, template, new GradientLut(lut, 16, 16));
        int[] tally = new int[2];
        HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, part, face, point) -> {
            tally[1]++;
            if ((img[py * n + px] & 0xFFFFFF) > 0xE0E0E0) {
                tally[0]++;
            }
        });
        return tally[1] == 0 ? 0 : tally[0] / (double) tally[1];
    }
}
