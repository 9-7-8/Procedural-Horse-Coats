package com.example.horsegenetics.common.coat.skin;

import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Axis;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Bounds;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.BodyPoint;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>{@link HorseSkinGeometry#local} - the part's own frame</b>, and the
 * calibration that nothing used to check.
 *
 * <h2>What this is guarding</h2>
 * Eleven gene files used to carry two hand-written numbers - a sawtooth
 * {@code wavelength} of 90 at an {@code amplitude} of 77.94 - which between
 * them tilted a band to {@code tan(60&deg;)} so it would lie along the neck
 * rather than across it. Sixty degrees because that is the neck's own long
 * axis: the box is 12 by 7 pitched 30&deg;, and its bounding box comes out
 * 12.07 wide by 13.89 tall. Those numbers were the geometry of the neck,
 * written down in eleven places, in files {@code HorseSkinGeometry} has never
 * heard of. Change the neck's pitch or its proportions and all eleven slid off
 * the crest with nothing anywhere going red - and the first calibration was
 * wrong (thirty degrees, the complement), which put goth's hood on the
 * underside of the neck and had to be reported twice.
 *
 * <p>{@code local} replaces the trick by asking the part what shape it is. So
 * the tests here are the properties the trick was hand-tuned to have, asserted
 * against the geometry tables instead of against two constants.
 */
class PartLocalFrameTest {

    /** Parts with no rest-pose pitch: their box is their bounding box. */
    private static final List<Part> UNPITCHED = List.of(
            Part.BODY, Part.LEFT_FRONT_LEG, Part.RIGHT_FRONT_LEG,
            Part.LEFT_HIND_LEG, Part.RIGHT_HIND_LEG);

    /**
     * On a part that is not tilted, the local frame and the bounding box are
     * the same box - so {@code local} must agree with plain part-space
     * normalisation exactly. This is what lets a gene author stop worrying
     * about which space to use on a leg.
     */
    @Test
    void localIsPartSpaceExactlyOnAnUnpitchedPart() {
        for (Skin skin : Skin.values()) {
            for (Part part : UNPITCHED) {
                if (!HorseSkinGeometry.hasPart(skin, part)) {
                    continue;
                }
                Bounds b = HorseSkinGeometry.bounds(skin, part);
                double[] worst = {0};
                HorseSkinGeometry.forEachTexel(skin, part, (px, py, p2, face, point) -> {
                    BodyPoint local = HorseSkinGeometry.local(skin, part, point);
                    for (Axis axis : Axis.values()) {
                        double partSpace = (point.along(axis) - b.min(axis)) / b.span(axis);
                        worst[0] = Math.max(worst[0], Math.abs(partSpace - local.along(axis)));
                    }
                });
                assertTrue(worst[0] < 1e-9,
                        skin + " " + part + " is unpitched, so local space must equal part space; "
                                + "worst disagreement was " + worst[0]);
            }
        }
    }

    /**
     * ...and on a pitched part they must <b>not</b> agree, or the new space is
     * doing nothing and every crest band is back to being a collar. The neck is
     * pitched 30&deg;, which is a long way from a rounding error.
     */
    @Test
    void localIsNotPartSpaceOnThePitchedNeck() {
        Bounds b = HorseSkinGeometry.bounds(Skin.ADULT, Part.NECK);
        double[] worst = {0};
        HorseSkinGeometry.forEachTexel(Skin.ADULT, Part.NECK, (px, py, p2, face, point) -> {
            BodyPoint local = HorseSkinGeometry.local(Skin.ADULT, Part.NECK, point);
            for (Axis axis : Axis.values()) {
                double partSpace = (point.along(axis) - b.min(axis)) / b.span(axis);
                worst[0] = Math.max(worst[0], Math.abs(partSpace - local.along(axis)));
            }
        });
        assertTrue(worst[0] > 0.5,
                "local space on the 30-degree neck should differ sharply from its bounding box; "
                        + "worst disagreement was only " + worst[0]);
    }

    /**
     * <b>The calibration itself.</b> The old trick's band coordinate was
     * {@code y - 77.94 * saw(x / 90)}, which over the length of a horse is a
     * straight ramp of slope {@code 2A/lambda = tan(60&deg;)}. If {@code local}
     * has the neck's axis right, that coordinate must be an <b>affine
     * function</b> of local X - the same lines, differently scaled - and the fit
     * has to be exact rather than approximate.
     *
     * <p>This is the assertion the eleven gene files never had. It fails if the
     * neck's pitch or its box changes, which is the exact failure that used to
     * be silent.
     */
    @Test
    void theOldSixtyDegreeTrickIsExactlyTheLocalDepthAxis() {
        List<double[]> rows = new ArrayList<>();
        HorseSkinGeometry.forEachTexel(Skin.ADULT, Part.NECK, (px, py, p2, face, point) -> {
            double trick = point.y() - 77.94 * saw(point.x() / 90.0);
            double localX = HorseSkinGeometry.local(Skin.ADULT, Part.NECK, point).x();
            rows.add(new double[] {localX, trick});
        });
        assertTrue(rows.size() > 500, "expected the whole neck, got " + rows.size() + " texels");

        double sx = 0, sy = 0, sxx = 0, sxy = 0;
        for (double[] r : rows) {
            sx += r[0];
            sy += r[1];
            sxx += r[0] * r[0];
            sxy += r[0] * r[1];
        }
        int n = rows.size();
        double slope = (n * sxy - sx * sy) / (n * sxx - sx * sx);
        double intercept = (sy - slope * sx) / n;
        double worst = 0;
        for (double[] r : rows) {
            worst = Math.max(worst, Math.abs(r[1] - (slope * r[0] + intercept)));
        }
        assertTrue(worst < 0.01,
                "the 60-degree trick is no longer the neck's own depth axis - worst residual "
                        + worst + " over " + n + " texels. Either the neck's geometry moved or "
                        + "local() is wrong; the eleven genes that used to hard-code this are "
                        + "listed on wiki/making-a-gene.html#local-space.");

        // The scale is the depth of the box, doubled - the trick measured in
        // body units and this measures in fractions of a 7-unit edge.
        assertEquals(-14.0, slope, 0.01,
                "the trick's slope against local X should be twice the neck's 7-unit depth");
    }

    /**
     * The crest is the face the <b>mane</b> sits on, so "low local X" has to
     * mean the crest rather than the throat - otherwise every converted gene
     * paints the underside of the neck, which is the original bug wearing a new
     * coordinate. Asserted by asking where the mane actually is rather than by
     * repeating a number.
     */
    @Test
    void lowLocalXIsTheCrestTheManeSitsOn() {
        double[] neck = {Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        HorseSkinGeometry.forEachTexel(Skin.ADULT, Part.NECK, (px, py, p2, face, point) -> {
            double lx = HorseSkinGeometry.local(Skin.ADULT, Part.NECK, point).x();
            neck[0] = Math.min(neck[0], lx);
            neck[1] = Math.max(neck[1], lx);
        });

        // Every mane texel, expressed in the NECK's frame: the mane is a
        // separate box sitting just off the neck's crest face.
        double sum = 0;
        int count = 0;
        List<double[]> mane = new ArrayList<>();
        HorseSkinGeometry.forEachTexel(Skin.ADULT, Part.MANE, (px, py, p2, face, point) ->
                mane.add(new double[] {HorseSkinGeometry.local(Skin.ADULT, Part.NECK, point).x()}));
        for (double[] m : mane) {
            sum += m[0];
            count++;
        }
        double maneMean = sum / count;
        double neckMid = (neck[0] + neck[1]) / 2;

        assertTrue(maneMean < neckMid,
                "the mane should sit at the LOW end of the neck's local X - that is what makes "
                        + "'from -1.0' the crest. Mane mean " + maneMean + " vs neck midpoint " + neckMid);
    }

    private static double saw(double turns) {
        double t = turns - Math.floor(turns);
        return 2 * t - 1;
    }
}
