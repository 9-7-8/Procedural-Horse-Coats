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
        COLORS
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

        static Param color(String name, String doc) {
            return new Param(name, Kind.COLOR, 0, List.of(), doc);
        }

        static Param colors(String name, String doc) {
            return new Param(name, Kind.COLORS, 0, List.of(), doc);
        }
    }

    /** Which coordinate an {@code AXIS} mask measures against. */
    public static final List<String> AXIS_SPACES = List.of("part", "body", "units");

    /** Which pigment reading a {@code PIGMENT} mask thresholds. */
    public static final List<String> PIGMENT_CHANNELS = List.of("darkness", "red", "black", "total");

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

    /** Which outline a {@code SPOTS} element is drawn with. */
    public static final List<String> SPOT_SHAPES = List.of("round", "heart");

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
                                + "'units' takes from/to as raw body units"),
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
                                + "(integration's spots spread out of the black points)")));

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
                Param.choice("axis", List.of("X", "Y", "Z"), "the axis the wave runs along"),
                Param.choice("across", List.of("Y", "X", "Z"), "the axis the wave displaces the band on"),
                Param.choice("shape", WAVEFORMS,
                        "'sine' scallops, 'triangle' zigzags into straight-sided teeth, 'saw' ramps "
                                + "and cuts back square into raked spears"),
                Param.choice("space", AXIS_SPACES,
                        "how 'across' is measured, exactly as on an AXIS mask - and so the units "
                                + "'from', 'to', 'amplitude', 'spacing' and 'softness' are in. "
                                + "'wavelength' is always in body units"),
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
                                + "is the one hard edge in the vocabulary")));

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
                Param.choice("axis", List.of("X", "Y", "Z"), "the body axis the ramp runs along"),
                Param.choice("space", AXIS_SPACES,
                        "as on an AXIS mask - 'part' runs the ramp inside each part, which is what a mane wants"),
                Param.value("from", 0.0, "axis position the first stop sits at"),
                Param.value("to", 1.0, "axis position the last stop sits at"),
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
