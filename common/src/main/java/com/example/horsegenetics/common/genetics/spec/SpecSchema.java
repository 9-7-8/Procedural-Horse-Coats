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
        COLOR
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
    }

    /** Which coordinate an {@code AXIS} mask measures against. */
    public static final List<String> AXIS_SPACES = List.of("part", "body", "units");

    /** Which pigment reading a {@code PIGMENT} mask thresholds. */
    public static final List<String> PIGMENT_CHANNELS = List.of("darkness", "red", "black", "total");

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
                Param.value("to", 1.0, "reading where coverage reaches 1")));

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

        OPS.put(OpType.TINT, List.of(
                Param.value("red", 0.0, "signed percent of full scale added to red"),
                Param.value("green", 0.0, "signed percent added to green"),
                Param.value("blue", 0.0, "signed percent added to blue"),
                Param.value("opacity", 100.0, "percent of opacity added, so the paint shows on a white horse")));

        OPS.put(OpType.TOWARD, List.of(
                Param.color("color", "the colour this layer walks the texel toward"),
                Param.value("strength", 100.0, "percent of the way there"),
                Param.value("opacity", 100.0, "percent opacity the texel ends at")));

        OPS.put(OpType.FLAT, List.of(
                Param.color("color", "flat paint, replacing whatever was accumulated"),
                Param.value("opacity", 100.0, "percent opacity")));
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
