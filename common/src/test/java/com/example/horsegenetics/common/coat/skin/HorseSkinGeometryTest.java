package com.example.horsegenetics.common.coat.skin;

import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Bounds;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Face;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Sample;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Texel;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HorseSkinGeometryTest {

    private static final double EPS = 1e-6;

    @Test
    void hoovesSitAtYZero() {
        for (Part leg : new Part[]{
                Part.LEFT_FRONT_LEG, Part.RIGHT_FRONT_LEG, Part.LEFT_HIND_LEG, Part.RIGHT_HIND_LEG}) {
            assertEquals(0.0, HorseSkinGeometry.bounds(leg).yMin(), EPS, leg + " bottom");
        }
    }

    @Test
    void tailBackEdgeSitsAtXZero() {
        assertEquals(0.0, HorseSkinGeometry.bounds(Part.TAIL).xMin(), EPS);
        assertEquals(0.0, HorseSkinGeometry.bodyBounds().xMin(), EPS);
    }

    @Test
    void bodyIsCentredOnZ() {
        Bounds b = HorseSkinGeometry.bounds(Part.BODY);
        assertEquals(-b.zMax(), b.zMin(), EPS);
        assertEquals(5.0, b.zMax(), EPS);
    }

    @Test
    void noseIsTheGlobalMaxX() {
        Bounds whole = HorseSkinGeometry.bodyBounds();
        assertEquals(HorseSkinGeometry.bounds(Part.MUZZLE).xMax(), whole.xMax(), EPS);
        assertTrue(whole.xMax() > 40 && whole.xMax() < 42, "nose ~41 units from the tail, got " + whole.xMax());
    }

    @Test
    void positiveZIsTheHorsesRight() {
        // "left" parts sit on -Z, "right" parts on +Z
        assertTrue(HorseSkinGeometry.bounds(Part.LEFT_HIND_LEG).zMax() < 0);
        assertTrue(HorseSkinGeometry.bounds(Part.RIGHT_HIND_LEG).zMin() > 0);
    }

    @Test
    void mirrorPartsHaveMirroredZAndIdenticalXY() {
        for (Part p : new Part[]{Part.LEFT_FRONT_LEG, Part.LEFT_HIND_LEG, Part.LEFT_EAR}) {
            Bounds l = HorseSkinGeometry.bounds(p);
            Bounds r = HorseSkinGeometry.bounds(p.mirror());
            assertEquals(l.xMin(), r.xMin(), EPS);
            assertEquals(l.xMax(), r.xMax(), EPS);
            assertEquals(l.yMin(), r.yMin(), EPS);
            assertEquals(l.yMax(), r.yMax(), EPS);
            assertEquals(-l.zMax(), r.zMin(), EPS);
            assertEquals(-l.zMin(), r.zMax(), EPS);
        }
    }

    @Test
    void allFourLegsShareTheSameXBandFrontVsHind() {
        Bounds lf = HorseSkinGeometry.bounds(Part.LEFT_FRONT_LEG);
        Bounds rf = HorseSkinGeometry.bounds(Part.RIGHT_FRONT_LEG);
        Bounds lh = HorseSkinGeometry.bounds(Part.LEFT_HIND_LEG);
        Bounds rh = HorseSkinGeometry.bounds(Part.RIGHT_HIND_LEG);
        assertEquals(lf.xMin(), rf.xMin(), EPS);
        assertEquals(lf.xMax(), rf.xMax(), EPS);
        assertEquals(lh.xMin(), rh.xMin(), EPS);
        assertEquals(lh.xMax(), rh.xMax(), EPS);
        // hind legs are behind the front legs on the nose axis
        assertTrue(lh.xMax() < lf.xMin(), "hind X band should be strictly behind front");
        // and both bands fall inside the body's X extent
        Bounds body = HorseSkinGeometry.bounds(Part.BODY);
        assertTrue(lh.xMin() >= body.xMin() - EPS);
        assertTrue(lf.xMax() <= body.xMax() + EPS);
    }

    @Test
    void everyFaceRectStaysOnTheSheet() {
        for (Part part : Part.values()) {
            for (Face face : Face.values()) {
                Texel a = HorseSkinGeometry.project(part, face,
                        HorseSkinGeometry.bounds(part).min(face.spanA()),
                        HorseSkinGeometry.bounds(part).min(face.spanB()));
                Texel b = HorseSkinGeometry.project(part, face,
                        HorseSkinGeometry.bounds(part).max(face.spanA()),
                        HorseSkinGeometry.bounds(part).max(face.spanB()));
                for (Texel t : new Texel[]{a, b}) {
                    assertFalse(t.clamped(), part + "/" + face + " corner flagged clamped");
                    assertTrue(t.u() >= -EPS && t.u() <= HorseSkinGeometry.SHEET_SIZE + EPS,
                            part + "/" + face + " u=" + t.u());
                    assertTrue(t.v() >= -EPS && t.v() <= HorseSkinGeometry.SHEET_SIZE + EPS,
                            part + "/" + face + " v=" + t.v());
                }
            }
        }
    }

    @Test
    void outOfBoundsQueryClampsAndFlags() {
        Bounds b = HorseSkinGeometry.bounds(Part.BODY);
        Texel t = HorseSkinGeometry.project(Part.BODY, Face.RIGHT,
                b.max(Face.RIGHT.spanA()) + 100.0, b.min(Face.RIGHT.spanB()) - 100.0);
        assertTrue(t.clamped());
        assertTrue(t.x() >= 0 && t.x() < HorseSkinGeometry.SHEET_SIZE);
    }

    @Test
    void projectRoundTripsThroughSampleForEveryMappedTexel() {
        int checked = 0;
        for (int py = 0; py < HorseSkinGeometry.SHEET_SIZE; py++) {
            for (int px = 0; px < HorseSkinGeometry.SHEET_SIZE; px++) {
                Optional<Sample> s = HorseSkinGeometry.sample(px, py);
                if (s.isEmpty()) {
                    continue;
                }
                Sample sample = s.get();
                Texel back = HorseSkinGeometry.project(sample.part(), sample.face(), sample.point());
                assertFalse(back.clamped(),
                        "texel (" + px + "," + py + ") -> " + sample.part() + "/" + sample.face()
                                + " re-projected as clamped");
                assertTrue(Math.abs(back.u() - (px + 0.5)) <= 0.5 + EPS,
                        "u round-trip at (" + px + "," + py + "): " + back.u());
                assertTrue(Math.abs(back.v() - (py + 0.5)) <= 0.5 + EPS,
                        "v round-trip at (" + px + "," + py + "): " + back.v());
                checked++;
            }
        }
        assertTrue(checked > 2000, "expected most of the sheet to be mapped, got " + checked);
    }

    @Test
    void faceOrientationIsUprightAndNotMirrored() {
        Bounds b = HorseSkinGeometry.bounds(Part.BODY);
        double midX = (b.xMin() + b.xMax()) / 2;
        double midY = (b.yMin() + b.yMax()) / 2;
        double midZ = (b.zMin() + b.zMax()) / 2;

        // up (larger Y) -> higher on the sheet (smaller v), on both flanks
        assertTrue(HorseSkinGeometry.project(Part.BODY, Face.RIGHT, midX, b.yMin()).v()
                > HorseSkinGeometry.project(Part.BODY, Face.RIGHT, midX, b.yMax()).v());
        assertTrue(HorseSkinGeometry.project(Part.BODY, Face.LEFT, midX, b.yMin()).v()
                > HorseSkinGeometry.project(Part.BODY, Face.LEFT, midX, b.yMax()).v());

        // toward the nose (larger X): u rises on the right flank, falls on the
        // left flank - the box-unwrap handedness the non-mirrored sheet exists
        // to capture. The two flanks occupy disjoint u ranges.
        double rTail = HorseSkinGeometry.project(Part.BODY, Face.RIGHT, b.xMin(), midY).u();
        double rNose = HorseSkinGeometry.project(Part.BODY, Face.RIGHT, b.xMax(), midY).u();
        double lTail = HorseSkinGeometry.project(Part.BODY, Face.LEFT, b.xMin(), midY).u();
        double lNose = HorseSkinGeometry.project(Part.BODY, Face.LEFT, b.xMax(), midY).u();
        assertTrue(rNose > rTail, "right flank: nose end should be at larger u");
        assertTrue(lNose < lTail, "left flank: nose end should be at smaller u");
        assertTrue(Math.max(rTail, rNose) <= Math.min(lTail, lNose) + EPS, "flanks share u range");

        // topline: toward the nose -> larger v; toward the horse's right -> smaller u
        assertTrue(HorseSkinGeometry.project(Part.BODY, Face.TOP, b.xMax(), midZ).v()
                > HorseSkinGeometry.project(Part.BODY, Face.TOP, b.xMin(), midZ).v());
        assertTrue(HorseSkinGeometry.project(Part.BODY, Face.TOP, midX, b.zMax()).u()
                < HorseSkinGeometry.project(Part.BODY, Face.TOP, midX, b.zMin()).u());
    }

    @Test
    void xGradientIsContinuousFromBodyOntoAHindLeg() {
        // A pattern shade = bx, sampled on the topline of the body and of a
        // hind leg, must agree where the two boxes meet on the X axis.
        Bounds body = HorseSkinGeometry.bounds(Part.BODY);
        Bounds leg = HorseSkinGeometry.bounds(Part.LEFT_HIND_LEG);
        double sharedX = leg.xMin(); // hind-leg back == body back region
        assertTrue(sharedX >= body.xMin() - EPS && sharedX <= body.xMax() + EPS);

        // same body X on both parts -> sample() recovers the same X (the value
        // a gradient function would receive), so no seam.
        Texel onBody = HorseSkinGeometry.project(Part.BODY, Face.TOP, sharedX, 0.0);
        Texel onLeg = HorseSkinGeometry.project(Part.LEFT_HIND_LEG, Face.TOP, sharedX, leg.zMin());
        assertEquals(sharedX, HorseSkinGeometry.sample(onBody.x(), onBody.y()).orElseThrow().point().x(),
                1.0);
        assertEquals(sharedX, HorseSkinGeometry.sample(onLeg.x(), onLeg.y()).orElseThrow().point().x(),
                1.0);
    }
}
