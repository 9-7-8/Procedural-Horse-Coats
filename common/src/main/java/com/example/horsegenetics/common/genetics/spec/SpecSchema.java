package com.example.horsegenetics.common.genetics.spec;

import com.example.horsegenetics.common.genetics.spec.GeneSpec.MaskType;
import com.example.horsegenetics.common.genetics.spec.GeneSpec.OpType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What parameters each {@link MaskType} and {@link OpType} accepts, and what
 * each one defaults to.
 *
 * <p>It is a <b>table, not prose</b>, on purpose. {@link GeneSpecParser} checks
 * every file against it - so a mistyped {@code "spacng"} is a load error naming
 * the key and listing the legal ones, rather than a knob that silently does
 * nothing and an author who spends an evening wondering why. The gene creator
 * builds its parameter forms from the same table (mirrored in
 * {@code wiki/gene-creator/js/schema.js}), which is what keeps the tool from
 * offering settings the game does not read.
 *
 * <p>When you add a mask or an op: add it here, in {@code SpecPainter}, and in
 * the creator's mirror. All three, or the tool and the game drift.
 */
public final class SpecSchema {

    /** How a parameter's JSON is read. */
    public enum Kind {
        /** A number, a knob reference, or a per-dose triple - a {@link GeneSpec.Value}. */
        VALUE,
        /** A list of {@code Part} names and {@link PartGroups} aliases. */
        PARTS,
        /** One of a fixed set of words. */
        CHOICE,
        /** A boolean. */
        FLAG,
        /** {@code "#rrggbb"}. */
        COLOR,
        /** A list of {@code "#rrggbb"} - a palette, or a ramp's stops. */
        COLORS,
        /**
         * A flat array of numbers read as {@code [u0, v0, u1, v1, ...]} - the
         * control points of a {@code PATH}, in the plane that mask names.
         *
         * <p>The first parameter kind in the format that is neither a number
         * nor a word, and it exists because a <b>drawn</b> shape has no
         * parametric description. Every other mask here says what a shape is
         * made of and lets the horse decide where it lands; this one carries
         * the shape itself.
         */
        POINTS,
        /**
         * <b>SVG path data</b> - a {@code d} string, flattened by
         * {@link com.example.horsegenetics.common.coat.pattern.SvgPath} at load
         * into the polylines the painter walks.
         *
         * <p>The one kind whose parsed form is not the shape the file wrote,
         * and the reason is cost: walking the path grammar and sampling every
         * cubic and arc in it is a hundred times what measuring one texel
         * against the result costs, so it happens once. The source string is
         * kept on the flattened shape, so writing the file back out is exact.
         */
        SVG,
        /**
         * A free string - an SVG {@code transform} list, and nothing else so
         * far. Unlike {@link #CHOICE} there is no fixed set to check it
         * against, so whatever validation there is happens in the thing that
         * reads it.
         */
        TEXT,
        /**
         * Four numbers read as {@code [minU, minV, width, height]} - an SVG
         * {@code viewBox}, in the order and the meaning that file writes them.
         */
        BOX
    }

    /**
     * One parameter. {@code fallback} is what the painter uses when the file
     * leaves it out - the values here are the documented defaults, so a minimal
     * spec is a short one.
     */
    public record Param(String name, Kind kind, double fallback, List<String> choices, String doc) {

        static Param value(String name, double fallback, String doc) {
            return new Param(name, Kind.VALUE, fallback, List.of(), doc);
        }

        static Param parts(String name, String doc) {
            return new Param(name, Kind.PARTS, 0, List.of(), doc);
        }

        static Param choice(String name, List<String> choices, String doc) {
            return new Param(name, Kind.CHOICE, 0, choices, doc);
        }

        static Param flag(String name, String doc) {
            return new Param(name, Kind.FLAG, 0, List.of(), doc);
        }

        /**
         * A flag the painter reads as <b>true</b> when the file leaves it out.
         *
         * <p>Every flag in the format was false-by-default until the SVG mask,
         * and there the two that matter are both true: a {@code <path>} with no
         * stroke is filled, and a pasted drawing whose y is not flipped lands
         * upside down. Making the author write those out on every mask would be
         * the tool disagreeing with the format it is importing from.
         *
         * <p>It costs the creator a real change rather than a cast, because its
         * exporter drops any setting equal to its default: a flag whose default
         * is true has to be <i>written</i> when it is unticked. That is why the
         * fallback is compared by the parity check now and was not before.
         */
        static Param flagOn(String name, String doc) {
            return new Param(name, Kind.FLAG, 1, List.of(), doc);
        }

        static Param color(String name, String doc) {
            return new Param(name, Kind.COLOR, 0, List.of(), doc);
        }

        static Param colors(String name, String doc) {
            return new Param(name, Kind.COLORS, 0, List.of(), doc);
        }

        static Param points(String name, String doc) {
            return new Param(name, Kind.POINTS, 0, List.of(), doc);
        }

        static Param svg(String name, String doc) {
            return new Param(name, Kind.SVG, 0, List.of(), doc);
        }

        static Param text(String name, String doc) {
            return new Param(name, Kind.TEXT, 0, List.of(), doc);
        }

        static Param box(String name, String doc) {
            return new Param(name, Kind.BOX, 0, List.of(), doc);
        }
    }

    /**
     * Which coordinate an {@code AXIS} mask measures against.
     *
     * <p>{@code part} and {@code local} differ only on a <b>pitched</b> part,
     * and there they differ completely. {@code part} normalises against the
     * rest-pose axis-aligned bounding box, so on the 30&deg;-pitched neck a band
     * on Y is a horizontal slice: it crosses the crest and the throat alike,
     * which is a collar. {@code local} takes the pitch back out first, so the
     * same band runs <b>along</b> the part's own edge - a stripe up the crest,
     * the full length of the neck. Anything that wants to follow a part rather
     * than cut across it wants {@code local}.
     */
    public static final List<String> AXIS_SPACES = List.of("part", "body", "units", "local");

    /**
     * Which plane a {@code PATH} is drawn in, and therefore which axis it is
     * extruded along. Each names its two axes in {@code (u, v)} order.
     *
     * <p>{@code side} is {@code (x, y)} seen from the horse's flank, extruded
     * along {@code z} - so a shape drawn once appears on <b>both</b> flanks,
     * mirrored, which is what a marking drawn on a side view of a horse should
     * do. {@code top} is {@code (x, z)} seen from above, extruded down: the
     * plane for anything that straddles the spine. {@code front} is
     * {@code (z, y)} seen from the nose, extruded along the length - a girth, a
     * collar, a band round the barrel.
     *
     * <p>The first entry is the default, so it has to be the one
     * {@code SpecPainter} falls back to.
     */
    public static final List<String> PATH_PLANES = List.of("side", "top", "front");

    /**
     * How a {@code PATH}'s {@code points} are measured.
     *
     * <p>{@code body} normalises each axis over the whole-horse bounds, so
     * {@code 0.5, 0.5} is the middle of the horse whichever skin is being
     * baked - which is the only way a path drawn on the adult lands in the same
     * anatomical place on the foal, whose boxes are a different size.
     * {@code units} takes them as raw body units, for a path positioned against
     * something measured.
     *
     * <p><b>Only the points are affected.</b> {@code width} and
     * {@code softness} are in body units either way. Mixing the two is
     * deliberate: a stroke a quarter of a horse wide is never what anybody
     * meant, and {@code WAVES} is already a standing lesson in what happens
     * when a length changes meaning with a space setting.
     */
    public static final List<String> PATH_SPACES = List.of("body", "units");

    /**
     * How an {@code SVG} mask's {@code viewBox} is fitted into the viewport the
     * mask places it in - SVG's {@code preserveAspectRatio}, under its own
     * name.
     *
     * <p>It is a parameter and not a convenience because the three answers are
     * genuinely different pictures and the file cannot tell you which one was
     * meant. {@code meet} fits the whole drawing inside the viewport and leaves
     * slack on one axis - nothing is lost, and a square drawing in a long
     * viewport ends up small. {@code slice} fills the viewport and lets the
     * drawing run off the other axis - nothing is small, and the corners are
     * gone. {@code none} stretches to fill, which is the only one that changes
     * the shape, and is exactly what a marking meant to wrap a barrel usually
     * wants.
     */
    public static final List<String> SVG_FITS = List.of("meet", "slice", "none");

    /**
     * Where a drawing sits inside its viewport when {@code fit} left slack -
     * SVG's nine {@code preserveAspectRatio} alignments, spelled the way that
     * attribute spells them so a value can be copied straight out of the file.
     */
    public static final List<String> SVG_ALIGNMENTS =
            com.example.horsegenetics.common.coat.pattern.SvgPath.ALIGNMENTS;

    /**
     * Which area an {@code SVG} fill counts as inside.
     *
     * <p>The hole in a letter O is a second subpath, and whether it is a hole
     * at all depends entirely on this: two subpaths wound the same way are one
     * solid blob under {@code nonzero} and a ring under {@code evenodd}. Which
     * the artist meant is written in their file and is not recoverable from the
     * geometry, so it is asked for rather than guessed - and the default is
     * {@code nonzero} because that is SVG's own.
     */
    public static final List<String> SVG_FILL_RULES = List.of("nonzero", "evenodd");

    /** How an open {@code SVG} subpath's two free ends are finished. */
    public static final List<String> SVG_CAPS = List.of("butt", "round", "square");

    /** How an {@code SVG} stroke turns a corner. */
    public static final List<String> SVG_JOINS = List.of("miter", "round", "bevel");

    /**
     * How many straight sub-segments each span of a smoothed {@code PATH} is
     * walked in.
     *
     * <p>It is a constant rather than a knob because it is not a look, it is a
     * <b>fidelity</b>: too few and a curve reads as the polyline it is made of,
     * more and the extra segments are shorter than the texel that samples them.
     * Eight puts the error of a right-angle span well under half a texel at
     * horse scale. It also has to match the creator's port exactly, and a
     * number both sides hard-code is one fewer thing that can drift.
     */
    public static final int PATH_CURVE_SAMPLES = 8;

    /**
     * The most control points a {@code PATH} may carry. Every texel of every
     * skin walks all of them, so this is the cost ceiling; it is generous
     * because a hand-drawn outline really can want thirty or forty, and it is
     * finite because a gene file is not a place to put a traced photograph.
     */
    public static final int MAX_PATH_POINTS = 64;

    /** Which pigment reading a {@code PIGMENT} mask thresholds. */
    public static final List<String> PIGMENT_CHANNELS = List.of("darkness", "red", "black", "total");

    /**
     * Which reading of the <b>resolved colour</b> a {@code LUMA} mask
     * thresholds. Every one is taken from
     * {@link com.example.horsegenetics.common.coat.pattern.ColorView#visible} -
     * the coat as a viewer sees it, after the gradient chart and after every
     * magical gene that painted before this one.
     *
     * <p>{@code dark} and {@code light} are the two halves of one number and
     * both are here on purpose: a gene that wants the black of the horse should
     * say {@code "channel": "dark"} rather than {@code "light"} with an
     * {@code invert}, because an inverted mask also inverts what a {@code spread}
     * grows and reads backwards at the call site.
     *
     * <p>{@code white} is <b>not</b> {@code light}. It is the achromatic floor -
     * the smallest of the three channels, the HWB whiteness - so a bald white
     * texel reads 1 and a saturated yellow one reads near 0 however bright it
     * is. That distinction is the whole reason this mask exists: "the white
     * markings" and "the pale parts" are different sets of texels on a palomino.
     */
    public static final List<String> LUMA_CHANNELS =
            List.of("dark", "light", "white", "saturation", "red", "green", "blue");

    /**
     * Why a mask needs asking for symmetry rather than getting it free: every
     * field here is sampled in body space, and body space has a signed
     * {@code z}. A lattice point at {@code z = +2} and one at {@code z = -2} are
     * different cells, so a spot on the near flank has no counterpart on the
     * far one - which is right for a paint marking and wrong for anything that
     * reads as a <i>design</i>. Angler's string of lights is the case: a lure
     * runs down both sides of a fish the same way.
     */
    private static final String MIRROR_DOC =
            "draw the field on |z| rather than z, so the two sides of the horse get the same "
                    + "marks in the same places - the only way to ask for a symmetrical scatter";

    /**
     * Which side a {@code spread} may take its growth <b>from</b>.
     *
     * <p>{@code any} is the isotropic disc - grow in every direction, which is
     * what a halo round a marking wants. The other four restrict the search to
     * candidates on one side along one body axis, so the selection grows the
     * <i>other</i> way: {@code above} means "a selected texel higher up counts",
     * and therefore grows the selection <b>downward</b>. That is the only way to
     * ask for the bottom edge of a marking rather than its whole rim, and
     * markings that pool, run or drip all want exactly one side of themselves.
     */
    public static final List<String> SPREAD_SIDES = List.of("any", "above", "below", "ahead", "behind");

    private static final String SPREAD_FROM_DOC =
            "which side the growth comes from - 'above' grows the selection downward, 'below' "
                    + "upward, 'ahead' toward the tail and 'behind' toward the nose. 'any' is the "
                    + "isotropic disc. Read only when 'spread' is above 0";

    /** Which outline a {@code SPOTS} element is drawn with. */
    public static final List<String> SPOT_SHAPES = List.of("round", "heart");

    /**
     * Which distance a {@code CRACKLE} mask reports.
     *
     * <p>Both readings come off the <b>same tessellation</b>, and that is the
     * whole value of the second one: a layer measuring {@code wall} and a layer
     * measuring {@code centroid} on the same seed and scale are talking about
     * the same polygons, so a colour banded by the second lands concentric
     * inside the cells outlined by the first. Nothing else in the vocabulary
     * can promise that - {@code SPOTS} and {@code DAPPLES} lay down their own
     * centres, and those have never had anything to do with where a crackle
     * wall fell.
     *
     * <p>The first entry is the default, so it has to be the one
     * {@code SpecPainter} falls back to.
     */
    public static final List<String> CRACKLE_MEASURES = List.of("wall", "centroid");

    /**
     * Where a {@code RAMP} reads the position it looks its colour up at.
     *
     * <p>The three axes are a straight line through the horse, and a straight
     * line is monotonic by construction: whatever the stops are, the colour
     * sweeps once from one end to the other and never comes back. The other
     * three are the answer to markings that are not like that.
     *
     * <p>{@code noise} is a smooth field, so the colour <b>wanders</b> - it
     * doubles back, pools and thins the way an oil slick does, with no cell
     * walls anywhere. {@code cell} is the distance from a texel to the middle of
     * its own cell, so every cell gets its own independent radial fade at once.
     * {@code cellId} is one number drawn per cell and constant across it, so
     * neighbours take unrelated colours - the difference between "iridescent"
     * and "a gradient", and the only one of the three that has hard edges.
     *
     * <p>All three read {@code seed} and {@code scale} and ignore
     * {@code space}, {@code from} and {@code to}, which measure a spatial axis
     * and have nothing to say about a field.
     */
    public static final List<String> RAMP_AXES = List.of("X", "Y", "Z", "noise", "cell", "cellId");

    /**
     * What a {@code FRACTAL} mask does with the field once the octaves are
     * summed. All three are the <i>same</i> field read three ways, so switching
     * between them keeps the shape and changes only what counts as inside it.
     *
     * <p>{@code fbm} is the field itself - blobs with detail on their edges, the
     * multi-scale {@code PATCHES}. {@code ridged} is 1 along the surfaces where
     * the field crosses its midpoint and 0 either side, which turns those blob
     * outlines into <b>lines</b>: they curve, fork, pinch out and taper, and at
     * a high {@code threshold} they are thin enough to read as lace. {@code
     * billow} is its complement - the field folded at the midpoint, so both
     * extremes come out bright and the pattern is puffed rather than laced.
     *
     * <p>The first entry is the default, so it has to be the one
     * {@code SpecPainter} falls back to.
     */
    public static final List<String> FRACTAL_SHAPES = List.of("fbm", "ridged", "billow");

    /**
     * Which waveform a {@code WAVES} mask is displaced by.
     *
     * <p>{@code sine} scallops, {@code triangle} zigzags into teeth with
     * straight sides, and {@code saw} ramps and then cuts back square - a row
     * of spears all raked the same way. The last two are the only way in this
     * vocabulary to ask for an edge made of <b>straight lines</b>: everything
     * noise-derived rounds off, however hard the softness is wound down.
     */
    public static final List<String> WAVEFORMS = List.of("sine", "triangle", "saw");

    /**
     * Why every colour op takes an {@code hue} as well as a {@code color}: a
     * {@code color} is a constant, and a constant cannot be the thing a horse
     * drew for itself. {@code hue} is a {@link Kind#VALUE}, so it can be pointed
     * at a knob - and one knob is the difference between a gene that paints
     * teal spots and a gene that paints spots of whatever colour this line of
     * horses runs to.
     */
    private static final String HUE_DOC =
            "hue in degrees - 0 red, 120 green, 240 blue. Below 0 means \"not set\", and the "
                    + "layer paints 'color' instead - which is what lets one number be the "
                    + "difference between a fixed palette and an epigenetic one.";
    private static final String SATURATION_DOC = "saturation 0 to 1; read only when 'hue' is set";
    private static final String LIGHTNESS_DOC = "lightness 0 to 1; read only when 'hue' is set";

    /**
     * The most octaves a {@code FRACTAL} mask will take. Every octave is a full
     * lattice sample - eight hashes - at every mapped texel of every skin, so
     * this is a cost ceiling rather than a taste one. Six octaves at the default
     * {@code lacunarity} already put the finest one below a texel, so the next
     * one would be sampling detail the sheet cannot hold.
     */
    public static final int MAX_OCTAVES = 6;

    private static final Map<MaskType, List<Param>> MASKS = new LinkedHashMap<>();
    private static final Map<OpType, List<Param>> OPS = new LinkedHashMap<>();

    static {
        MASKS.put(MaskType.ALL, List.of());

        MASKS.put(MaskType.PARTS, List.of(
                Param.parts("parts", "the body parts this layer touches")));

        MASKS.put(MaskType.AXIS, List.of(
                Param.parts("parts", "restrict to these parts (and, in 'part' space, measure within each)"),
                // The FIRST choice is the default, so it has to be the one
                // SpecPainter falls back to when the key is absent - Y here.
                Param.choice("axis", List.of("Y", "X", "Z"),
                        "X runs tail to nose, Y hoof to withers, Z centre to the horse's right"),
                Param.choice("space", AXIS_SPACES,
                        "'part' normalises inside each part (a sock per leg), 'body' across the whole horse, "
                                + "'units' takes from/to as raw body units, 'local' normalises inside the "
                                + "part's own box with its pitch taken out (a stripe along the crest, not a "
                                + "collar round the throat)"),
                Param.value("from", 0.0, "start of the solid band"),
                Param.value("to", 1.0, "end of the solid band"),
                Param.value("softness", 0.15, "fade width outside the band, same units as from/to")));

        MASKS.put(MaskType.CENTERLINE, List.of(
                Param.parts("parts", "restrict to these parts - a blaze is FACE"),
                Param.value("halfWidth", 1.0, "body units either side of the centreline"),
                Param.value("softness", 0.35, "fade width at the edge, in body units"),
                Param.value("offset", 0.0, "shift the stripe off centre, in body units")));

        MASKS.put(MaskType.STRIPES, List.of(
                Param.parts("parts", "restrict to these parts"),
                Param.value("seed", 0, "a seed knob; omit for a stable per-gene default"),
                Param.value("spacing", 3.0, "centre-to-centre, body units (the adult barrel is 22 long)"),
                Param.value("duty", 0.45, "share of each period that is stripe"),
                Param.value("warp", 1.0, "how far the noise may bend a stripe, body units")));

        MASKS.put(MaskType.DAPPLES, List.of(
                Param.parts("parts", "restrict to these parts"),
                Param.value("seed", 0, "a seed knob; omit for a stable per-gene default"),
                Param.value("spacing", 3.5, "body units between dapple centres"),
                Param.value("warp", 0.45, "how far the lattice flows off the grid, as a share of spacing"),
                Param.value("edge0", 0.35, "distance where the dapple centre ends"),
                Param.value("edge1", 0.78, "distance where the web between dapples begins")));

        MASKS.put(MaskType.PATCHES, List.of(
                Param.parts("parts", "restrict to these parts"),
                Param.value("seed", 0, "a seed knob; omit for a stable per-gene default"),
                Param.value("scale", 6.0, "body units across a typical patch"),
                Param.value("threshold", 0.5, "how much of the horse a patch covers - lower is more"),
                Param.value("softness", 0.12, "edge softness of a patch")));

        MASKS.put(MaskType.NOISE, List.of(
                Param.parts("parts", "restrict to these parts"),
                Param.value("seed", 0, "a seed knob; omit for a stable per-gene default"),
                Param.value("scale", 8.0, "body units per noise feature"),
                Param.value("low", 0.0, "coverage the darkest noise maps to"),
                Param.value("high", 1.0, "coverage the brightest noise maps to")));

        MASKS.put(MaskType.FRACTAL, List.of(
                Param.parts("parts", "restrict to these parts"),
                Param.value("seed", 0, "a seed knob; omit for a stable per-gene default"),
                Param.value("scale", 6.0, "body units across one feature of the COARSEST octave"),
                Param.value("octaves", 3.0,
                        "how many times the field is re-sampled at higher frequency, 1 to "
                                + MAX_OCTAVES + ". 1 is exactly a PATCHES mask; each one after "
                                + "adds finer detail WITHOUT changing how much of the horse "
                                + "clears 'threshold'"),
                Param.value("lacunarity", 2.13,
                        "frequency multiplier per octave. Deliberately not 2: whole multiples "
                                + "line the octaves up on the same lattice and the sum grids up "
                                + "visibly, which is why PatchNoise uses 2.13 too"),
                Param.value("gain", 0.5, "amplitude multiplier per octave - below 0.5 is smoother, "
                        + "above is rougher and grainier"),
                Param.value("warp", 0.0,
                        "body units the sample point is pushed around by a second, coarser field "
                                + "before the octaves are taken. 0 is off; it is what stops the "
                                + "detail from lying in rows along the coarse features, and it is "
                                + "what makes an edge wander rather than merely wobble"),
                Param.choice("shape", FRACTAL_SHAPES,
                        "'fbm' is the field (blobs with detailed edges), 'ridged' its midpoint "
                                + "crossings (lines that fork and taper - the lace), 'billow' its "
                                + "fold (both extremes bright)"),
                Param.value("threshold", 0.5, "how much of the horse the field covers - lower is more"),
                Param.value("softness", 0.12, "edge fade, in field units either side of the threshold")));

        MASKS.put(MaskType.PATH, List.of(
                Param.parts("parts", "restrict to these parts"),
                Param.choice("plane", PATH_PLANES,
                        "which two axes the shape is drawn in, and so which one it is extruded "
                                + "along - 'side' (x,y) appears on both flanks, 'top' (x,z) "
                                + "straddles the spine, 'front' (z,y) runs round the barrel"),
                Param.choice("space", PATH_SPACES,
                        "how 'points' are measured - 'body' normalises each axis over the whole "
                                + "horse so the shape lands in the same anatomical place on the "
                                + "foal, 'units' takes raw body units. 'width' and 'softness' "
                                + "are in body units either way"),
                Param.points("points",
                        "the control points, flat: [u0, v0, u1, v1, ...]. At least two points, "
                                + "at most " + MAX_PATH_POINTS),
                Param.flag("curve",
                        "smooth the points into a Catmull-Rom spline that passes through every "
                                + "one of them, instead of joining them with straight lines"),
                Param.flag("closed", "join the last point back to the first"),
                Param.flag("fill",
                        "fill the enclosed area rather than stroking the line. Implies 'closed'; "
                                + "'width' is then unread"),
                Param.value("width", 1.0, "stroke width, body units - a texel is about 0.5"),
                Param.value("softness", 0.25, "edge fade, body units")));

        MASKS.put(MaskType.CHOICE, List.of(
                Param.parts("parts", "restrict to these parts"),
                Param.value("seed", 0, "a seed knob; omit for a stable per-gene default"),
                Param.value("options", 2.0, "how many outcomes the choice has"),
                Param.value("is", 0.0, "which outcome this layer draws on")));

        MASKS.put(MaskType.PIGMENT, List.of(
                Param.parts("parts", "restrict to these parts"),
                Param.choice("channel", PIGMENT_CHANNELS,
                        "'darkness' is 0.55*red + 0.95*black, the reading GreyCoat uses"),
                Param.value("from", 0.5, "reading where coverage starts climbing"),
                Param.value("to", 1.0, "reading where coverage reaches 1"),
                Param.value("spread", 0.0,
                        "body units to grow WHAT THIS MASK SELECTED by - the largest coverage "
                                + "found within the radius wins, applied after 'invert'. So an "
                                + "inverted mask over white grows the white (fielded's wisps run "
                                + "out of it) and a plain one over darkness grows the dark "
                                + "(integration's spots spread out of the black points)"),
                Param.choice("spreadFrom", SPREAD_SIDES, SPREAD_FROM_DOC)));

        MASKS.put(MaskType.LUMA, List.of(
                Param.parts("parts", "restrict to these parts"),
                Param.choice("channel", LUMA_CHANNELS,
                        "'dark' is 1 - relative luminance, 'light' its complement, 'white' the "
                                + "achromatic floor (bald white 1, saturated colour ~0)"),
                Param.value("from", 0.5, "reading where coverage starts climbing"),
                Param.value("to", 1.0, "reading where coverage reaches 1"),
                Param.value("spread", 0.0,
                        "body units to grow WHAT THIS MASK SELECTED by, exactly as on PIGMENT - "
                                + "the largest coverage found within the radius wins, applied "
                                + "after 'invert'"),
                Param.choice("spreadFrom", SPREAD_SIDES, SPREAD_FROM_DOC)));

        MASKS.put(MaskType.EDGE, List.of(
                Param.parts("parts", "restrict to these parts - one rectangle per face of each"),
                Param.value("width", 0.6, "how far in from the boundary the rim runs, body units"),
                Param.value("softness", 0.25, "fade width inside the rim, body units")));

        MASKS.put(MaskType.SPOTS, List.of(
                Param.parts("parts", "restrict to these parts"),
                Param.value("seed", 0, "a seed knob; omit for a stable per-gene default"),
                Param.value("spacing", 4.0, "body units between element centres - the lattice pitch"),
                Param.value("radius", 0.9, "element radius, body units (the adult barrel is 22 long)"),
                Param.value("vary", 0.5, "how much the radius varies element to element, 0 to 1"),
                Param.value("chance", 1.0, "share of lattice cells that carry an element at all"),
                Param.value("stretch", 1.0, "long-axis multiplier - 1 is round, 2 is a 2:1 oval"),
                Param.choice("axis", List.of("X", "Y", "Z"), "the body axis the oval is stretched along"),
                Param.choice("shape", SPOT_SHAPES,
                        "'round' is the spot field; 'heart' swaps the disc for a heart, point down, "
                                + "upright on the flank"),
                Param.flag("mirror", MIRROR_DOC),
                Param.value("arc", 1.0,
                        "share of each element's own circumference that is drawn, measured around "
                                + "that element's centre. Below 1 clips every mark in the field to "
                                + "the same one-sided crescent, which is what makes a tiling read as "
                                + "SHINGLED - overlapping scales, feathers, roof tiles - instead of "
                                + "as a closed net. RINGS has the same parameter and can only ever "
                                + "apply it to one placed instance"),
                Param.value("angle", 0.0,
                        "degrees the drawn arc is centred on, in the plane of the two axes the long "
                                + "axis is not. 0 points toward the nose. Read only when 'arc' is "
                                + "below 1"),
                Param.value("offsetX", 0.0,
                        "shift THIS mask's reading of the cell centre, body units, without moving the "
                                + "cell. Several masks on one seed and spacing stay concentric - that "
                                + "is rule 14 - and this is how one of them deliberately does not: "
                                + "the off-centre glint inside an eyespot, the highlight on a bead"),
                Param.value("offsetY", 0.0, "the same, on Y"),
                Param.value("offsetZ", 0.0, "the same, on Z"),
                Param.value("softness", 0.25, "edge fade, body units")));

        MASKS.put(MaskType.RINGS, List.of(
                Param.parts("parts", "restrict to these parts"),
                Param.value("seed", 0, "a seed knob; omit for a stable per-gene default"),
                Param.value("spacing", 6.0, "body units between ring centres"),
                Param.value("radius", 2.0, "ring radius, body units"),
                Param.value("thickness", 0.6, "wall thickness, body units"),
                Param.value("vary", 0.4, "how much the radius varies ring to ring, 0 to 1"),
                Param.value("chance", 1.0, "share of lattice cells that carry a ring at all"),
                Param.value("arc", 1.0, "share of the circumference drawn - below 1 gives a crescent"),
                Param.value("offsetX", 0.0,
                        "shift THIS mask's reading of the cell centre, body units, without moving the "
                                + "cell - exactly as on SPOTS, and for the same reason"),
                Param.value("offsetY", 0.0, "the same, on Y"),
                Param.value("offsetZ", 0.0, "the same, on Z"),
                Param.value("softness", 0.2, "edge fade, body units")));

        MASKS.put(MaskType.SPECKLE, List.of(
                Param.parts("parts", "restrict to these parts"),
                Param.value("seed", 0, "a seed knob; omit for a stable per-gene default"),
                Param.value("spacing", 0.7, "body units between particle centres - a texel is about 0.5"),
                Param.value("size", 0.45, "particle radius as a share of the spacing"),
                Param.value("density", 0.5, "share of lattice cells carrying a particle"),
                Param.value("clumping", 0.0, "how far a low-frequency field pushes density around, 0 to 1"),
                Param.value("clumpScale", 7.0, "body units per clump"),
                Param.value("softness", 0.3, "edge fade as a share of the particle radius")));

        MASKS.put(MaskType.STROKES, List.of(
                Param.parts("parts", "restrict to these parts"),
                Param.value("seed", 0, "a seed knob; omit for a stable per-gene default"),
                Param.value("spacing", 3.0, "body units between neighbouring strokes"),
                Param.value("length", 12.0, "body units a stroke runs before it curves away or pinches out"),
                Param.choice("axis", List.of("X", "Y", "Z"), "the axis strokes run along"),
                Param.value("width", 0.8, "stroke width, body units - a texel is 0.5"),
                Param.value("curl", 0.35, "how far strokes wander off the axis, as a share of the spacing"),
                Param.value("softness", 0.25, "edge fade, body units")));

        MASKS.put(MaskType.SPIRAL, List.of(
                Param.parts("parts", "one spiral is drawn per named part"),
                Param.value("seed", 0, "a seed knob; omit for a stable per-gene default"),
                Param.value("radius", 4.0, "outer radius, body units"),
                Param.value("turns", 2.0, "revolutions from the centre out to the radius"),
                Param.value("width", 0.5, "stroke width, body units"),
                Param.choice("axis", List.of("Z", "X", "Y"), "the axis the spiral is viewed down"),
                Param.value("offset", 0.0, "shift the centre along the part's long axis, as a share of its span"),
                Param.value("softness", 0.2, "edge fade, body units")));

        MASKS.put(MaskType.WAVES, List.of(
                Param.parts("parts", "restrict to these parts (and, in 'part' space, measure within each)"),
                Param.value("seed", 0, "a seed knob; omit for a stable per-gene default"),
                Param.choice("axis", List.of("X", "Y", "Z"),
                        "the axis the wave TRAVELS along - NOT the band's axis, which is the opposite "
                                + "of what 'axis' means on AXIS. A topline band with lobes hanging down "
                                + "is axis X, across Y"),
                Param.choice("across", List.of("Y", "X", "Z"),
                        "the axis the BAND sits on - from and to are measured along it, and the wave "
                                + "pushes the band's edges back and forth on it"),
                Param.choice("shape", WAVEFORMS,
                        "'sine' scallops, 'triangle' zigzags into straight-sided teeth, 'saw' ramps "
                                + "and cuts back square into raked spears"),
                Param.choice("space", AXIS_SPACES,
                        "how 'across' is measured, exactly as on an AXIS mask - and so the units "
                                + "'from', 'to', 'amplitude', 'spacing' and 'softness' are in. "
                                + "'wavelength' is always in body units, which is why an amplitude "
                                + "tuned in 'units' means something else entirely in 'part' or 'local'"),
                Param.value("from", 0.0, "start of the band, before the sine displaces it"),
                Param.value("to", 1.0, "end of the band"),
                Param.value("wavelength", 8.0, "body units per full oscillation along 'axis'"),
                Param.value("amplitude", 0.5, "how far the sine displaces the band"),
                Param.value("spacing", 0.0,
                        "0 draws one band; above 0 repeats it every this far along 'across', "
                                + "which is what turns an edge into a set of parallel ribbons"),
                Param.value("phase", 0.0,
                        "0 keeps every repeat in step, 1 gives each its own phase - read only "
                                + "when 'spacing' is above 0"),
                Param.value("softness", 0.15, "fade width outside the band")));

        MASKS.put(MaskType.GOO, List.of(
                Param.parts("parts", "restrict to these parts (and, in 'part' space, measure within each)"),
                Param.value("seed", 0, "a seed knob; omit for a stable per-gene default"),
                Param.choice("axis", List.of("X", "Y", "Z"),
                        "the axis the band RUNS along, and so the one the drips are spaced out "
                                + "along - as on WAVES, not as on AXIS"),
                Param.choice("across", List.of("Y", "X", "Z"),
                        "the axis the band sits on. The drips hang from the 'from' edge, away "
                                + "from 'to' - so a topline band that runs down the flanks is "
                                + "axis X, across Y, from high, to higher"),
                Param.choice("space", AXIS_SPACES,
                        "how 'across' is measured, exactly as on AXIS - and so the units 'from' "
                                + "and 'to' are in. Every other length here is in BODY units, "
                                + "because a drip is a shape and has to stay round"),
                Param.value("from", 0.75, "the edge the drips hang from"),
                Param.value("to", 1.6,
                        "the far edge of the band. Put it well past the end of the part - a band "
                                + "that stops at 1.0 leaves the top of the back bare wherever the "
                                + "edge wanders above it"),
                Param.value("spacing", 4.0, "body units from one drip to the next, before 'vary' moves them"),
                Param.value("drop", 3.0, "how far the longest drip runs below the edge, body units"),
                Param.value("width", 1.6, "the stem's width, body units"),
                Param.value("bulb", 1.5,
                        "the tip's radius as a multiple of the stem's half-width. 1 is a plain "
                                + "round end; above 1 is a bead of liquid about to fall"),
                Param.value("vary", 0.6,
                        "0 makes every drip the same length, in the same place, and the mask is "
                                + "a comb; 1 runs them from nothing to 'drop' and shifts each a "
                                + "third of a cell either way"),
                Param.value("chance", 0.75, "share of cells that carry a drip at all"),
                Param.value("wobble", 0.5,
                        "how far the band's own edge wanders, body units - the difference "
                                + "between a poured line and a ruled one"),
                Param.value("sag", 0.6,
                        "how deeply the edge arcs UP between two drips, body units. 0 leaves it "
                                + "ruled, which reads as beads hung on a wire"),
                Param.value("softness", 0.1, "edge fade, body units")));

        MASKS.put(MaskType.CRACKLE, List.of(
                Param.parts("parts", "restrict to these parts"),
                Param.value("seed", 0, "a seed knob; omit for a stable per-gene default"),
                Param.value("scale", 5.0, "body units across one polygon"),
                Param.value("gap", 0.5, "width of the channel between two polygons, body units"),
                Param.value("warp", 0.35, "how far a low-frequency field pushes the polygons out of "
                        + "true, as a share of the scale - 0 is a regular tiling"),
                Param.value("chance", 1.0, "share of polygons that are filled at all"),
                Param.value("softness", 0.08,
                        "edge fade, body units - small on purpose, because the point of this mask "
                                + "is the one hard edge in the vocabulary"),
                Param.choice("measure", CRACKLE_MEASURES,
                        "'wall' is distance to the boundary between two polygons - the crack. "
                                + "'centroid' is distance from the texel to ITS OWN polygon's centre, "
                                + "which is the only way to shade or band each irregular cell "
                                + "independently. Under 'centroid' the mask covers everything FARTHER "
                                + "than gap/2 BODY UNITS from the centre - not a fraction of the cell: "
                                + "the field runs from 0 to roughly 'scale', with walls near 0.55 of "
                                + "that. A disc at the centre is therefore the mask INVERTED. And the "
                                + "lattice is 3D, so most centres lie off the skin: a small centre mark "
                                + "rarely shows. For rings concentric with the polygon outline, bands "
                                + "of 'wall' distance are the robust choice"),
                Param.value("vertexWeight", 0.0,
                        "0 to 1, how far the wall distance is blended toward distance to the nearest "
                                + "three-way CORNER of the tiling. 0 is the even channel; at 1 the "
                                + "field is only near zero at the junctions. In between, a threshold "
                                + "on it gives a network that POOLS where cracks meet and thins to a "
                                + "hairline between them, which even walls cannot do. Read under "
                                + "'wall' only")));

        MASKS.put(MaskType.SVG, List.of(
                Param.parts("parts", "restrict to these parts"),
                Param.svg("d", "the SVG path data, verbatim: M m L l H h V v C c S s Q q T t A a Z z, "
                        + "absolute and relative, as many subpaths as you like. Flattened at load, so "
                        + "the cost per texel is the number of points it came to and not the number of "
                        + "commands you wrote"),
                Param.text("transform", "an SVG transform list applied to the path before anything "
                        + "else - matrix, translate, scale, rotate, skewX, skewY, composed left to "
                        + "right. Paste the one off the <path> or its <g>"),
                Param.box("viewBox", "the drawing's own coordinate box, [minU, minV, width, height], "
                        + "exactly as the <svg> writes it. Omit and the path's own bounding box is "
                        + "used, which is what you want for a single mark and wrong for one mark out "
                        + "of a set that has to keep its position among the others"),
                Param.choice("plane", SpecSchema.PATH_PLANES,
                        "which two axes the drawing lies in, and so which one it is extruded along - "
                                + "as on PATH. 'side' appears on BOTH flanks"),
                Param.choice("space", SpecSchema.PATH_SPACES,
                        "how the viewport below is measured - 'body' normalises each axis over the "
                                + "whole horse, 'units' is raw body units. Stroke 'width', 'softness' "
                                + "and the dash lengths are body units either way"),
                Param.value("originU", 0.0, "the viewport's near corner along the plane's first axis"),
                Param.value("originV", 0.0, "the viewport's near corner along its second axis"),
                Param.value("sizeU", 1.0, "the viewport's extent along the first axis"),
                Param.value("sizeV", 1.0, "the viewport's extent along the second axis"),
                Param.choice("fit", SVG_FITS,
                        "preserveAspectRatio: 'meet' fits the whole drawing in and leaves slack, "
                                + "'slice' fills the viewport and overflows, 'none' stretches to fill"),
                Param.choice("align", SVG_ALIGNMENTS, "where the drawing sits in the slack 'fit' left"),
                Param.flagOn("flipY",
                        "SVG's y runs DOWN the page and the horse's runs up, so this is true by "
                                + "default and a pasted drawing lands the right way up. Set it false "
                                + "for one authored in body space"),
                Param.flagOn("fill",
                        "fill the enclosed area rather than stroking the outline. True by default, "
                                + "because that is what a <path> with no stroke does and it is what "
                                + "nearly every drawing means. 'width', 'cap', 'join' and the dashes "
                                + "are then unread"),
                Param.choice("fillRule", SVG_FILL_RULES,
                        "'nonzero' or 'evenodd' - which is a hole and which is a blob. Copy it from "
                                + "the file's fill-rule; the geometry cannot tell you"),
                Param.value("width", 1.0, "stroke width, body units - a texel is about 0.5"),
                Param.choice("cap", SVG_CAPS, "how an open subpath's free ends finish"),
                Param.choice("join", SVG_JOINS, "how the stroke turns a corner"),
                Param.value("miterLimit", 4.0,
                        "how many stroke widths a miter may reach before it falls back to a bevel, "
                                + "as in SVG. A shallow corner spikes without it"),
                Param.value("dash", 0.0,
                        "length of one dash, body units. 0 is a solid stroke; above 0 turns the "
                                + "outline into a broken one, measured along the path the way a "
                                + "stroke-dasharray is"),
                Param.value("gap", 0.0, "length of the space between dashes; 0 means the same as 'dash'"),
                Param.value("dashOffset", 0.0, "shift the pattern along the path, body units"),
                Param.value("softness", 0.25, "edge fade, body units")));

        MASKS.put(MaskType.FAN, List.of(
                Param.parts("parts", "restrict to these parts"),
                Param.choice("plane", SpecSchema.PATH_PLANES,
                        "which two axes the angle is measured in - as on PATH. 'side' fans on both flanks"),
                Param.choice("space", SpecSchema.PATH_SPACES,
                        "how the origin is measured; the radii and 'spacing' are body units either way"),
                Param.value("originU", 0.5, "the pivot every bar radiates from, first axis"),
                Param.value("originV", 0.5, "the pivot, second axis"),
                Param.value("spacing", 0.4,
                        "the angular period, given as the arc length one cycle covers at ONE BODY "
                                + "UNIT from the pivot - so it is an ANGLE, in radians, and a full "
                                + "turn holds 2*pi/spacing bars. The default is about sixteen of "
                                + "them. Bars widen with distance from the pivot, which is the whole "
                                + "difference from WAVES, and it is also why a number tuned at one "
                                + "radius still reads at another"),
                Param.value("duty", 0.5, "share of each cycle that is bar rather than gap"),
                Param.value("twist", 0.0,
                        "degrees the fan's zero rotates per body unit of distance from the pivot. "
                                + "0 is a sunburst; anything else is a pinwheel"),
                Param.value("inner", 0.0, "body units from the pivot where the fan starts"),
                Param.value("outer", 0.0, "body units where it stops; 0 means it never does"),
                Param.value("softness", 0.15, "edge fade, as a share of the bar's own width")));

        MASKS.put(MaskType.NORMAL, List.of(
                Param.parts("parts", "restrict to these parts"),
                Param.choice("axis", List.of("Y", "X", "Z"),
                        "the direction the surface is compared against - Y up, X toward the nose, "
                                + "Z toward the horse's right"),
                Param.choice("space", List.of("body", "local"),
                        "'body' takes the box's own facing, so a pitched neck's top face is not level; "
                                + "'local' takes the pitch out first, so 'up' means up the part"),
                Param.value("round", 0.0,
                        "0 reads the FLAT face normal, which on boxes takes exactly three values and "
                                + "gives hard, faceted zones. Above 0 it blends toward the normal the "
                                + "part would have if its box were an ellipsoid, which is continuous "
                                + "and is what a shell's sheen or a rim light needs. 1 is fully rounded"),
                Param.value("from", -1.0, "the dot product that reads as coverage 0"),
                Param.value("to", 1.0, "the dot product that reads as coverage 1")));

        OPS.put(OpType.DILUTE, List.of(
                Param.value("keepRed", 1.0, "share of red pigment kept"),
                Param.value("keepBlack", 1.0, "share of black pigment kept"),
                Param.value("blackTint", 0.0,
                        "share of the removed black fed back as red - without it a diluted point "
                                + "stays on the gradient's jet-black column")));

        OPS.put(OpType.RESTRICT, List.of(
                Param.value("red", 0.0, "share of red pigment removed"),
                Param.value("black", 0.0, "share of black pigment removed")));

        OPS.put(OpType.SET_PIGMENT, List.of(
                Param.value("red", 0.0, "red pigment level to move toward"),
                Param.value("black", 0.0, "black pigment level to move toward")));

        OPS.put(OpType.WHITEN, List.of(
                Param.value("amount", 1.0,
                        "share of white hair mixed in - 1 is bald white, a fraction is a roan "
                                + "fleck. Multiplied by the mask coverage, so a soft-edged mask "
                                + "greys out rather than browning on its way to white")));

        OPS.put(OpType.TINT, List.of(
                Param.value("red", 0.0, "signed percent of full scale added to red"),
                Param.value("green", 0.0, "signed percent added to green"),
                Param.value("blue", 0.0, "signed percent added to blue"),
                Param.value("opacity", 100.0, "percent of opacity added, so the paint shows on a white horse")));

        OPS.put(OpType.TOWARD, List.of(
                Param.color("color", "the colour this layer walks the texel toward"),
                Param.value("hue", -1, HUE_DOC),
                Param.value("saturation", 0.8, SATURATION_DOC),
                Param.value("lightness", 0.55, LIGHTNESS_DOC),
                Param.value("strength", 100.0, "percent of the way there"),
                Param.value("opacity", 100.0, "percent opacity the texel ends at")));

        OPS.put(OpType.FLAT, List.of(
                Param.color("color", "flat paint, replacing whatever was accumulated"),
                Param.value("hue", -1, HUE_DOC),
                Param.value("saturation", 0.8, SATURATION_DOC),
                Param.value("lightness", 0.55, LIGHTNESS_DOC),
                Param.value("opacity", 100.0, "percent opacity")));

        OPS.put(OpType.RAMP, List.of(
                Param.colors("colors", "two or more stops, walked through in order along the axis"),
                Param.value("hue", 0, HUE_DOC + " On a ramp it names the first stop, and the ramp "
                        + "then travels 'hueSpan' degrees round the wheel from it."),
                Param.value("hueSpan", 60.0, "degrees of hue the ramp travels; negative runs the other way"),
                Param.value("saturation", 0.8, SATURATION_DOC),
                Param.value("lightness", 0.55, LIGHTNESS_DOC),
                Param.choice("axis", RAMP_AXES,
                        "X, Y or Z is a straight line through the horse and so sweeps ONCE. 'noise' "
                                + "reads a smooth field instead, so the colour wanders and doubles "
                                + "back with no hard edge anywhere; 'cell' reads distance to the "
                                + "middle of the texel's own cell, so every cell fades independently; "
                                + "'cellId' reads one number per cell, constant across it, so "
                                + "neighbours take unrelated colours"),
                Param.choice("space", AXIS_SPACES,
                        "as on an AXIS mask - 'part' runs the ramp inside each part, which is what a mane "
                                + "wants, and 'local' runs it along a pitched part's own length. Read "
                                + "only by the three spatial axes"),
                Param.value("from", 0.0, "axis position the first stop sits at"),
                Param.value("to", 1.0, "axis position the last stop sits at"),
                Param.value("seed", 0, "a seed knob; omit for a stable per-gene default. Read only by "
                        + "the 'noise', 'cell' and 'cellId' axes"),
                Param.value("scale", 5.0,
                        "body units per feature of the field - one wavelength of the noise, or one "
                                + "cell across. Read only by the 'noise', 'cell' and 'cellId' axes. "
                                + "Point it at the same knob as the mask's own scale and the colour "
                                + "lines up with the shape"),
                Param.value("strength", 100.0, "percent of the way to the ramp colour"),
                Param.value("opacity", 100.0, "percent opacity the texel ends at")));

        OPS.put(OpType.PALETTE, List.of(
                Param.colors("colors", "the palette; each cell takes one entry whole"),
                Param.value("hue", 0, HUE_DOC + " On a palette it names the centre hue, and each "
                        + "cell lands within 'hueSpread' degrees of it."),
                Param.value("hueSpread", 40.0, "degrees either side of 'hue' a cell may land"),
                Param.value("saturation", 0.8, SATURATION_DOC),
                Param.value("lightness", 0.55, LIGHTNESS_DOC),
                Param.value("seed", 0, "a seed knob; omit for a stable per-gene default"),
                Param.value("scale", 5.0, "body units across one colour cell"),
                Param.value("strength", 100.0, "percent of the way to the cell's colour"),
                Param.value("opacity", 100.0, "percent opacity the texel ends at")));

        OPS.put(OpType.INVERT, List.of(
                Param.value("amount", 100.0,
                        "percent of the way to the negative - 100 is a full inversion, 50 is flat grey"),
                Param.value("opacity", 100.0, "percent opacity the texel ends at")));
    }

    private SpecSchema() {}

    public static List<Param> maskParams(MaskType type) {
        return MASKS.get(type);
    }

    public static List<Param> opParams(OpType type) {
        return OPS.get(type);
    }

    /** {@code null} when this type has no such parameter - the parser's typo check. */
    public static Param maskParam(MaskType type, String name) {
        return find(MASKS.get(type), name);
    }

    public static Param opParam(OpType type, String name) {
        return find(OPS.get(type), name);
    }

    public static List<String> maskParamNames(MaskType type) {
        return MASKS.get(type).stream().map(Param::name).toList();
    }

    public static List<String> opParamNames(OpType type) {
        return OPS.get(type).stream().map(Param::name).toList();
    }

    private static Param find(List<Param> params, String name) {
        for (Param p : params) {
            if (p.name().equals(name)) {
                return p;
            }
        }
        return null;
    }
}
