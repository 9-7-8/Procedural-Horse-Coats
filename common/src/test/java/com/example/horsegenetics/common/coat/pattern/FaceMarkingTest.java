package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.coat.pattern.WhitePattern.FaceMarking;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Face;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.testutil.FakeRng;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link WhitePattern#faceMarking} - the <b>star / stripe / snip</b> vocabulary
 * every white locus now draws its face marking from.
 *
 * <p>What is pinned here is mostly the two claims that were not true of the old
 * painter, which drew a single centreline band running back from the nose:
 * <ul>
 *   <li>a <b>star</b> and a <b>snip</b> are <b>detached</b> - white with
 *       coloured face on every side of it, which a band from the nose cannot
 *       be;</li>
 *   <li>the <b>named markings really are spanned</b> by three booleans and a
 *       width, so nothing has to enumerate them.</li>
 * </ul>
 * plus the determinism contract (a fixed, unconditional draw), the ladder from
 * star to bald face, and the underside rule.
 */
class FaceMarkingTest {

    /** A strength in the middle of the range, where all three components are live. */
    private static final double MID = 0.30;

    // ------------------------------------------------------------------
    // The determinism contract
    // ------------------------------------------------------------------

    /**
     * <b>The draw is fixed and unconditional</b>: one {@code long} and eight
     * {@code float}s, whatever marking falls out. This is the property the
     * particle locus paid for the hard way - a draw made only when a flag is
     * set silently repaints every horse in every save the first time that
     * flag's odds move. So the empty marking and the bald face must consume
     * exactly the same sequence.
     */
    @Test
    void everyMarkingConsumesTheSameNineDrawsWhateverItTurnsOutToBe() {
        FakeRng bare = new FakeRng().longs(1L).floats(0.99f, 0.99f, 0.99f, 0.0f, 0.5f, 0.5f, 0.0f, 0.0f);
        FaceMarking nothing = WhitePattern.faceMarking(bare, Skin.ADULT, 0.05, 0.4);
        bare.assertExhausted();
        assertFalse(nothing.marksAnything(), "0.99 on all three presence rolls is an unmarked face");

        FakeRng loud = new FakeRng().longs(1L).floats(0.01f, 0.01f, 0.01f, 0.99f, 0.5f, 0.5f, 0.99f, 0.99f);
        FaceMarking bald = WhitePattern.faceMarking(loud, Skin.ADULT, 0.95, 0.4);
        loud.assertExhausted();
        assertTrue(bald.isBald(), "0.01 on all three at full strength is a bald face");
    }

    /** Same seed, same marking - the coat is rebuilt every session and must not drift. */
    @Test
    void theSameSeedDrawsTheSameMarking() {
        FaceMarking a = WhitePattern.faceMarking(new SeededRng(4242L), Skin.ADULT, MID, 0.4);
        FaceMarking b = WhitePattern.faceMarking(new SeededRng(4242L), Skin.ADULT, MID, 0.4);
        assertEquals(a.describe(), b.describe());
        assertEquals(coverage(a, Skin.ADULT), coverage(b, Skin.ADULT));
    }

    // ------------------------------------------------------------------
    // The two shapes the old painter could not draw
    // ------------------------------------------------------------------

    /**
     * <b>A star is detached.</b> It sits on the forehead with coloured face
     * all round it - in particular it never reaches the muzzle, which is what
     * separates it from the short stripe the old painter drew instead.
     */
    @Test
    void aStarIsAPatchOnTheForeheadAndNeverReachesTheMuzzle() {
        FaceMarking star = starOnly();
        assertEquals("star", star.describe());
        assertTrue(star.hasStar());
        assertFalse(star.hasStripe());
        assertFalse(star.hasSnip());

        assertTrue(countCovered(star, Skin.ADULT, Part.HEAD) > 0, "a star has to mark the head");
        assertEquals(0, countCovered(star, Skin.ADULT, Part.MUZZLE),
                "a star that reaches the muzzle is a stripe");
    }

    /**
     * <b>A snip is detached too</b>, at the other end: it is a patch at the
     * nostrils with coloured face above it, so it must not touch the head box
     * at all.
     */
    @Test
    void aSnipIsAPatchAtTheNostrilsAndNeverReachesTheForehead() {
        FaceMarking snip = snipOnly();
        assertEquals("snip", snip.describe());

        assertTrue(countCovered(snip, Skin.ADULT, Part.MUZZLE) > 0, "a snip has to mark the muzzle");
        assertEquals(0, countCovered(snip, Skin.ADULT, Part.HEAD),
                "a snip that reaches the head is a stripe");
    }

    /**
     * A star and a snip on the same horse are <b>two</b> patches, not one long
     * one: the coloured face between them is the whole point of naming them
     * separately.
     */
    @Test
    void aStarAndASnipLeaveColouredFaceBetweenThem() {
        FakeRng rng = new FakeRng().longs(9L).floats(0.01f, 0.99f, 0.01f, 0.0f, 0.5f, 0.5f, 0.5f, 0.5f);
        FaceMarking m = WhitePattern.faceMarking(rng, Skin.ADULT, MID, 0.4);
        assertEquals("star and snip", m.describe());

        boolean[] seen = new boolean[3];   // white forehead, coloured middle, white nostrils
        HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, part, face, point) -> {
            if (part != Part.HEAD && part != Part.MUZZLE) {
                return;
            }
            double t = faceT(Skin.ADULT, point);
            boolean white = m.covers(part, face, point);
            if (white && t < 0.45) seen[0] = true;
            if (!white && t > 0.50 && t < 0.75 && Math.abs(point.z()) < 1.0) seen[1] = true;
            if (white && t > 0.80) seen[2] = true;
        });
        assertTrue(seen[0], "no white on the forehead");
        assertTrue(seen[1], "no coloured face left between the star and the snip");
        assertTrue(seen[2], "no white at the nostrils");
    }

    // ------------------------------------------------------------------
    // The vocabulary
    // ------------------------------------------------------------------

    /**
     * Nothing in the code enumerates the named markings - they are read back
     * off three booleans and a width. So the test that the components really do
     * span the vocabulary is that every name a horseman would use actually
     * turns up somewhere on the strength range.
     */
    @Test
    void everyNamedMarkingIsReachable() {
        Set<String> seen = new HashSet<>();
        for (double s = 0.05; s <= 0.96; s += 0.03) {
            for (long seed = 0; seed < 400; seed++) {
                seen.add(WhitePattern.faceMarking(new SeededRng(seed * 7919L), Skin.ADULT, s, 0.4)
                        .describe());
            }
        }
        for (String name : new String[]{"none", "star", "snip", "star and snip", "stripe",
                "star and stripe", "stripe and snip", "star and stripe, with a snip",
                "blaze", "blaze to the nostrils", "bald face"}) {
            assertTrue(seen.contains(name), "no draw anywhere produces a " + name + "; got " + seen);
        }
    }

    /**
     * Strength picks the <b>distribution</b>, and the distribution has to move
     * the right way: a barely-marked locus is mostly stars and bare faces, a
     * strong one is blazes and bald faces, and the white on the face rises
     * monotonically between them.
     */
    @Test
    void moreWhiteOnTheGeneMeansMoreWhiteOnTheFace() {
        double[] strengths = {0.12, 0.24, 0.42, 0.58, 0.74, 0.93};
        double previous = -1;
        for (double s : strengths) {
            double mean = 0;
            int n = 300;
            for (long seed = 0; seed < n; seed++) {
                mean += coverage(WhitePattern.faceMarking(new SeededRng(seed * 7919L), Skin.ADULT, s, 0.4),
                        Skin.ADULT) / n;
            }
            assertTrue(mean > previous,
                    "face white did not increase at strength " + s + " (" + mean + " <= " + previous + ")");
            previous = mean;
        }
    }

    /** A minimally-marked locus leaves a good share of horses with a plain face. */
    @Test
    void aBarelyMarkedLocusOftenLeavesTheFaceAlone() {
        int bare = 0;
        int n = 1000;
        for (long seed = 0; seed < n; seed++) {
            if (!WhitePattern.faceMarking(new SeededRng(seed * 7919L), Skin.ADULT, 0.12, 0.4).marksAnything()) {
                bare++;
            }
        }
        assertTrue(bare > n / 10 && bare < n / 2,
                "expected a sizeable minority of unmarked faces at strength 0.12, got " + bare + "/" + n);
    }

    // ------------------------------------------------------------------
    // Where a marking is allowed to go
    // ------------------------------------------------------------------

    /**
     * A blaze runs down the <b>front</b> of the face and stops. Only a true
     * bald face is allowed onto the underside of the jaw and the chin - the old
     * painter tested the centreline on every plane of the box, so every blaze
     * wrapped under the jaw.
     */
    @Test
    void onlyABaldFaceReachesUnderTheJaw() {
        FaceMarking blaze = WhitePattern.faceMarking(
                new FakeRng().longs(3L).floats(0.01f, 0.01f, 0.99f, 0.0f, 0.5f, 0.99f, 0.5f, 0.5f),
                Skin.ADULT, 0.62, 0.11);
        assertEquals("blaze", blaze.describe());
        assertEquals(0, countCovered(blaze, Skin.ADULT, Face.BOTTOM), "a blaze wrapped under the jaw");

        FaceMarking bald = WhitePattern.faceMarking(
                new FakeRng().longs(3L).floats(0.01f, 0.01f, 0.01f, 0.99f, 0.5f, 0.99f, 0.99f, 0.99f),
                Skin.ADULT, 0.95, 0.11);
        assertTrue(bald.isBald());
        assertTrue(countCovered(bald, Skin.ADULT, Face.BOTTOM) > 0, "a bald face should take the jaw");
    }

    /** Only a bald face is wide enough to reach the sides of the head, where the eyes are. */
    @Test
    void onlyABaldFaceReachesTheSidesOfTheHead() {
        assertEquals(0, countCovered(starOnly(), Skin.ADULT, Face.RIGHT));
        FaceMarking bald = WhitePattern.faceMarking(
                new FakeRng().longs(3L).floats(0.01f, 0.01f, 0.01f, 0.99f, 0.5f, 0.99f, 0.99f, 0.99f),
                Skin.ADULT, 0.95, 0.11);
        assertTrue(countCovered(bald, Skin.ADULT, Face.RIGHT) > 0);
    }

    /** A face marking is a face marking - it never answers for any other part. */
    @Test
    void aFaceMarkingNeverCoversAnythingButTheHeadAndMuzzle() {
        FaceMarking bald = WhitePattern.faceMarking(
                new FakeRng().longs(3L).floats(0.01f, 0.01f, 0.01f, 0.99f, 0.5f, 0.99f, 0.99f, 0.99f),
                Skin.ADULT, 0.95, 0.4);
        HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, part, face, point) -> {
            if (part == Part.HEAD || part == Part.MUZZLE) {
                return;
            }
            assertFalse(bald.covers(part, face, point), part + " should be out of a face marking's reach");
        });
    }

    // ------------------------------------------------------------------
    // The foal
    // ------------------------------------------------------------------

    /**
     * The foal mesh has <b>no muzzle box</b> - the whole face is one part - so
     * face space has to be measured over whatever parts the mesh actually has.
     * A snip still has to land at the nose end of it.
     */
    @Test
    void theFoalHasNoMuzzleBoxAndStillGetsItsMarkingsInTheRightPlaces() {
        assertFalse(HorseSkinGeometry.hasPart(Skin.BABY, Part.MUZZLE));

        FaceMarking snip = WhitePattern.faceMarking(
                new FakeRng().longs(9L).floats(0.99f, 0.99f, 0.01f, 0.0f, 0.5f, 0.5f, 0.5f, 0.5f),
                Skin.BABY, MID, 0.4);
        assertEquals("snip", snip.describe());

        double[] range = {1.0, 0.0};
        HorseSkinGeometry.forEachTexel(Skin.BABY, (px, py, part, face, point) -> {
            if (part != Part.HEAD || !snip.covers(part, face, point)) {
                return;
            }
            double t = faceT(Skin.BABY, point);
            range[0] = Math.min(range[0], t);
            range[1] = Math.max(range[1], t);
        });
        assertTrue(range[1] > range[0], "the foal's snip drew nothing at all");
        assertTrue(range[0] > 0.6, "the foal's snip reached back to t=" + range[0] + ", which is its forehead");
    }

    // ------------------------------------------------------------------

    private static FaceMarking starOnly() {
        return WhitePattern.faceMarking(
                new FakeRng().longs(9L).floats(0.01f, 0.99f, 0.99f, 0.0f, 0.5f, 0.5f, 0.5f, 0.5f),
                Skin.ADULT, MID, 0.4);
    }

    private static FaceMarking snipOnly() {
        return WhitePattern.faceMarking(
                new FakeRng().longs(9L).floats(0.99f, 0.99f, 0.01f, 0.0f, 0.5f, 0.5f, 0.5f, 0.5f),
                Skin.ADULT, MID, 0.4);
    }

    /** Where along the face a body point sits: 0 at the poll, 1 at the nose tip. */
    private static double faceT(Skin skin, HorseSkinGeometry.BodyPoint point) {
        double min = HorseSkinGeometry.bounds(skin, Part.HEAD).xMin();
        double max = HorseSkinGeometry.hasPart(skin, Part.MUZZLE)
                ? HorseSkinGeometry.bounds(skin, Part.MUZZLE).xMax()
                : HorseSkinGeometry.bounds(skin, Part.HEAD).xMax();
        return (point.x() - min) / (max - min);
    }

    private static int countCovered(FaceMarking m, Skin skin, Part part) {
        int[] n = new int[1];
        HorseSkinGeometry.forEachTexel(skin, (px, py, p, face, point) -> {
            if (p == part && m.covers(p, face, point)) {
                n[0]++;
            }
        });
        return n[0];
    }

    private static int countCovered(FaceMarking m, Skin skin, Face wanted) {
        int[] n = new int[1];
        HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> {
            if (face == wanted && m.covers(part, face, point)) {
                n[0]++;
            }
        });
        return n[0];
    }

    private static double coverage(FaceMarking m, Skin skin) {
        int[] t = new int[2];
        HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> {
            if (part != Part.HEAD && part != Part.MUZZLE) {
                return;
            }
            t[1]++;
            if (m.covers(part, face, point)) {
                t[0]++;
            }
        });
        return t[1] == 0 ? 0 : t[0] / (double) t[1];
    }
}
