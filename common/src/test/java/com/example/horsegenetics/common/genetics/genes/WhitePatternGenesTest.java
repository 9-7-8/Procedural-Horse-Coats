package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.coat.pattern.CoatTextureComposer;
import com.example.horsegenetics.common.coat.pattern.GradientLut;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
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

    // ------------------------------------------------------------------

    private static AllelePair pairOf(Gene gene, String tokens) {
        String[] p = tokens.split("/");
        return new AllelePair(gene.fromToken(p[0]), gene.fromToken(p[1]));
    }

    /** Mean white coverage over three epigenetic seeds, so one unlucky roll cannot decide a rung. */
    private static double averageWhite(String code) {
        return (whiteFraction(code, 0L) + whiteFraction(code, 3L) + whiteFraction(code, 4242L)) / 3.0;
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
