package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Axis;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.BodyPoint;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Bounds;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;

/**
 * A <b>zebra</b> stripe map in body space - the organised, region-by-region
 * kind, not a single field of parallel bands.
 *
 * <h2>Why this is not {@link BodyStripes}</h2>
 * {@code BodyStripes} is one function of {@code x} for the whole animal. Run a
 * real zebra through that and you get vertical bars on the legs, vertical bars
 * across the rump, and vertical bars up the face - which is the one thing every
 * field guide says a zebra does <i>not</i> have. A zebra's pattern is a
 * <b>body map</b>: the same developmental machinery lands differently on
 * differently-shaped anatomy, so the stripe direction changes with the part.
 *
 * <table>
 *   <tr><th>region</th><th>what this draws</th></tr>
 *   <tr><td>back / topline</td><td>a continuous dark <b>dorsal stripe</b>, the organiser the rest hangs off</td></tr>
 *   <tr><td>barrel / flank</td><td>vertical bands descending from the spine, warped so they fork and taper</td></tr>
 *   <tr><td>rump</td><td>the bands <b>bend into arcs</b> around the hip - the transverse / bull's-eye end of the animal</td></tr>
 *   <tr><td>belly</td><td><b>pale</b>: the bands taper out before they reach the underside</td></tr>
 *   <tr><td>neck / mane</td><td>the same bands, narrower and closer together, leaning as the neck tapers</td></tr>
 *   <tr><td>head</td><td>narrower still; the muzzle is solid dark</td></tr>
 *   <tr><td>legs</td><td><b>horizontal rings</b>, reaching as far down as {@link Pattern#legReach}</td></tr>
 *   <tr><td>ears, tail</td><td>a band or two on the ear; a dark tail</td></tr>
 * </table>
 *
 * <h2>It returns the <i>dark</i> coverage</h2>
 * <b>1 is a dark band, 0 is the pale gap between two.</b> That is the way round
 * the biology has it - the dark hair is the default state and the white stripe
 * is where melanin was locally suppressed - and it is what lets the two zebra
 * genes share this one field while doing opposite things with it:
 * <ul>
 *   <li>{@code NaturalZebraGene} whitens {@code 1 - coverage}, so the horse's
 *       own colour survives as the dark bands and the gaps go to white hair;</li>
 *   <li>{@code MagicZebraGene} paints black over {@code coverage}, so the bands
 *       land on any coat at all - including one that has no pigment left to
 *       take away.</li>
 * </ul>
 * Regions that are solid dark on a real zebra - the dorsal stripe, the muzzle,
 * the tail - return {@code 1} rather than being left out, so neither caller has
 * to special-case them and the natural gene does not whiten them by accident.
 *
 * <p>Pure, like {@link BodyNoise} and {@link BodyStripes}: same
 * {@code (seed, point, knobs)} in, same coverage out.
 */
public final class ZebraStripes {

    /**
     * The per-horse knobs. Everything else in here is a constant of the
     * <i>shape</i> of a zebra rather than of an individual one.
     *
     * @param seed             the warp / width field's seed
     * @param spacing          centre-to-centre band distance on the barrel, in
     *                         body units; the adult barrel is 22 long
     * @param duty             fraction of each period that is dark band
     * @param bend             how far, in body units, the noise may bend a band
     *                         off its plane
     * @param legReach         fraction of each leg, measured down from the top,
     *                         that carries rings - {@code 1} takes them to the
     *                         hoof (Gr&eacute;vy's), {@code 0.5} stops them at
     *                         the knee (a plains zebra)
     * @param dorsalHalfWidth  half-width of the dorsal stripe, in body units
     */
    public record Pattern(long seed, double spacing, double duty, double bend,
                          double legReach, double dorsalHalfWidth) {}

    /** Soft edge as a fraction of the half-period. Narrow: zebra bands read crisp. */
    private static final double EDGE = 0.07;
    /** How gently the warp field bends a band - small = long, lazy curves. */
    private static final double WARP_SCALE = 1.0 / 9.0;
    /** Scale of the along-the-band width modulation: bands swell, taper and fork. */
    private static final double WIDTH_SCALE = 0.24;
    private static final double WIDTH_MIN = 0.74;
    private static final double WIDTH_RANGE = 0.52;
    /**
     * How far, per body unit off the centreline, a band leans along X - the
     * shallow chevron over the back. Symmetric on {@code |z|}, so the horse is
     * not lopsided, and it is what stops every face perpendicular to X (chest,
     * rump, the front of a leg) sitting at one phase and rendering as a flat
     * band. Same reasoning as {@link BodyStripes}, and just as load-bearing.
     */
    private static final double SLANT = 0.30;

    /** Where along the barrel the bands have finished turning into hip arcs. */
    private static final double RUMP_END = 0.34;
    /** How far up the hip the arcs are centred, as a fraction of the barrel box. */
    private static final double HIP_X = 0.14;
    private static final double HIP_Y = 0.58;

    /**
     * The pale belly: fully pale at {@code BELLY_BOTTOM} of the barrel box and
     * fully striped by {@code BELLY_TOP}. Kept <b>low</b> deliberately - a
     * zebra's pale region is the underside, and pushing it up the flank turns
     * the side view into a horse with a white line ruled along it.
     */
    private static final double BELLY_BOTTOM = 0.00;
    private static final double BELLY_TOP = 0.22;

    /**
     * Band spacing multipliers - a zebra's stripes tighten as the body narrows.
     * <b>The floor on these is the sheet, not the animal.</b> A real zebra's
     * facial stripes are finer than anything here, but the coat is two texels
     * to the body unit: below about half a unit per band the pattern stops
     * being stripes and starts being noise, which is what the first pass at the
     * head did.
     */
    private static final double NECK_TIGHT = 0.72;
    private static final double HEAD_TIGHT = 0.60;
    private static final double LEG_TIGHT = 0.62;
    private static final double EAR_TIGHT = 0.55;

    /** How far back the foot of a neck band trails behind its top, in body units. */
    private static final double NECK_LEAN = 1.6;

    /** The dark muzzle, as a fraction of the head box measured back from the nose. */
    private static final double MUZZLE_DARK = 0.34;
    private static final double MUZZLE_FADE = 0.22;

    /** How far a leg's rings fade out below {@link Pattern#legReach}. */
    private static final double LEG_FADE = 0.18;
    /** How much of a ring survives on the inside of the limb, which is barely striped. */
    private static final double LEG_INNER_KEEP = 0.45;

    private ZebraStripes() {}

    /**
     * Dark-band coverage at a texel: <b>1</b> deep inside a dark band,
     * <b>0</b> in the pale gap, with a crisp soft edge in between.
     */
    public static double coverage(Skin skin, Part part, BodyPoint point, Pattern pat) {
        if (pat.spacing() <= 0 || !HorseSkinGeometry.hasPart(skin, part)) {
            return 0;
        }
        double c = region(skin, part, point, pat);
        // The dorsal stripe is the organiser, so it wins wherever it lands: on
        // a real zebra the side bands meet it, they do not cross it.
        return Math.max(c, CoatRegions.dorsalStripe(skin, part, point, pat.dorsalHalfWidth()));
    }

    private static double region(Skin skin, Part part, BodyPoint point, Pattern pat) {
        switch (part) {
            case BODY:
                return torso(skin, point, pat);
            case NECK:
            case MANE:
                return column(skin, part, point, pat);
            case HEAD:
                return face(skin, point, pat);
            case MUZZLE:
                return 1.0;                       // a zebra's nose is solid dark
            case TAIL:
                return 1.0;                       // and so is the tuft
            case LEFT_EAR:
            case RIGHT_EAR:
                return ear(skin, part, point, pat);
            case LEFT_FRONT_LEG:
            case RIGHT_FRONT_LEG:
            case LEFT_HIND_LEG:
            case RIGHT_HIND_LEG:
                return rings(skin, part, point, pat);
            default:
                return 0;
        }
    }

    /**
     * The barrel: vertical bands descending from the spine, <b>turning into
     * arcs around the hip</b> as they run back, and tapering out over the
     * belly.
     *
     * <p>The turn is one line and no special case. A vertical band is a plane
     * of constant {@code x}; an arc round the hip is a circle of constant
     * radius from a point on it. Both are a phase, so the two are simply
     * <b>lerped</b> - and every intermediate value between them is one of the
     * angled, curving bands a real zebra wears over the flank, which is the
     * part nothing built out of parallel planes can draw.
     */
    private static double torso(Skin skin, BodyPoint point, Pattern pat) {
        Bounds b = HorseSkinGeometry.bounds(skin, Part.BODY);
        double fx = (point.x() - b.xMin()) / b.span(Axis.X);   // 0 rump .. 1 shoulder
        double fy = (point.y() - b.yMin()) / b.span(Axis.Y);   // 0 belly .. 1 topline

        double hipX = b.xMin() + b.span(Axis.X) * HIP_X;
        double hipY = b.yMin() + b.span(Axis.Y) * HIP_Y;
        double dx = point.x() - hipX;
        double dy = point.y() - hipY;
        double radial = Math.sqrt(dx * dx + dy * dy);

        double toArcs = 1.0 - smooth01(fx / RUMP_END);
        double along = point.x() + (radial - point.x()) * toArcs;

        double u = (along + Math.abs(point.z()) * SLANT + warp(point, pat)) / pat.spacing();
        double pale = smooth01((fy - BELLY_BOTTOM) / (BELLY_TOP - BELLY_BOTTOM));
        return band(u, pat, point) * pale;
    }

    /**
     * Neck and mane: the same bands, tighter, and leaning back as they drop -
     * the neck is a tapering column, so its stripes curve round it toward the
     * throat instead of hanging straight down.
     */
    private static double column(Skin skin, Part part, BodyPoint point, Pattern pat) {
        Bounds b = HorseSkinGeometry.bounds(skin, part);
        double fy = (point.y() - b.yMin()) / Math.max(1e-4, b.span(Axis.Y));
        double u = (point.x() + NECK_LEAN * (1.0 - fy) + Math.abs(point.z()) * SLANT + warp(point, pat))
                / (pat.spacing() * NECK_TIGHT);
        return band(u, pat, point);
    }

    /**
     * The head: narrower bands again, and a solid dark muzzle in front of them.
     *
     * <p>Real facial stripes curve round the eye and the nostril rather than
     * crossing the face as bars. <b>Unverified how much of that survives</b> at
     * two texels to the body unit and through a rest-pose AABB for a part that
     * is pitched 30&deg; - the narrow spacing and the dark nose are what carry
     * the read, and anything finer is probably below the resolution of the
     * sheet.
     */
    private static double face(Skin skin, BodyPoint point, Pattern pat) {
        Bounds b = HorseSkinGeometry.bounds(skin, Part.HEAD);
        double fromNose = (b.xMax() - point.x()) / Math.max(1e-4, b.span(Axis.X));
        double dark = 1.0 - smooth01((fromNose - MUZZLE_DARK) / MUZZLE_FADE);
        double u = (point.x() + Math.abs(point.z()) * SLANT + warp(point, pat))
                / (pat.spacing() * HEAD_TIGHT);
        return Math.max(dark, band(u, pat, point));
    }

    /**
     * The legs: <b>rings</b>, not bars. A torso band that reaches a leg
     * reorganises into a band round the limb, which is a phase in {@code y}
     * rather than in {@code x} - the single most recognisable thing about the
     * pattern and the thing a one-axis stripe field cannot say.
     *
     * <p>They fade out below {@link Pattern#legReach} (a plains zebra's lower
     * legs are much plainer than a Gr&eacute;vy's, which are ringed to the
     * hoof) and are damped on the <b>inside</b> of the limb, which is barely
     * striped on any of them.
     */
    private static double rings(Skin skin, Part leg, BodyPoint point, Pattern pat) {
        Bounds b = HorseSkinGeometry.bounds(skin, leg);
        double span = b.span(Axis.Y);
        if (span <= 0) {
            return 0;
        }
        double up = (point.y() - b.yMin()) / span;
        double window = smooth01((up - (1.0 - pat.legReach())) / LEG_FADE);
        if (window <= 0) {
            return 0;
        }
        double u = (point.y() + warp(point, pat)) / (pat.spacing() * LEG_TIGHT);

        // Outward = away from the horse's centre plane. The leg's own box sits
        // off-centre, so its far face is the outside of the limb.
        double centre = Math.abs((b.zMin() + b.zMax()) * 0.5);
        double outward = smooth01((Math.abs(point.z()) - centre + 1.0) / 2.0);
        double side = LEG_INNER_KEEP + (1.0 - LEG_INNER_KEEP) * outward;

        return band(u, pat, point) * window * side;
    }

    /** An ear carries a band or two of its own, across rather than down. */
    private static double ear(Skin skin, Part part, BodyPoint point, Pattern pat) {
        Bounds b = HorseSkinGeometry.bounds(skin, part);
        double span = b.span(Axis.Y);
        if (span <= 0) {
            return 0;
        }
        double u = point.y() / (pat.spacing() * EAR_TIGHT);
        return band(u, pat, point);
    }

    /** The one band evaluator every region phases into. */
    private static double band(double u, Pattern pat, BodyPoint point) {
        double offset = u - Math.floor(u);
        double d = Math.abs(offset - 0.5) * 2.0;
        double width = pat.duty() * (WIDTH_MIN + WIDTH_RANGE * BodyNoise.value(
                pat.seed() ^ 0x9E3779B97F4A7C15L,
                point.x() * WIDTH_SCALE, point.y() * WIDTH_SCALE, point.z() * WIDTH_SCALE));
        return 1.0 - BodyStripes.smoothstep(width - EDGE, width + EDGE, d);
    }

    private static double warp(BodyPoint point, Pattern pat) {
        return (BodyNoise.value(pat.seed(), point.x() * WARP_SCALE, point.y() * WARP_SCALE,
                point.z() * WARP_SCALE) - 0.5) * 2.0 * pat.bend();
    }

    private static double smooth01(double t) {
        t = t < 0 ? 0 : (t > 1 ? 1 : t);
        return t * t * (3 - 2 * t);
    }
}
