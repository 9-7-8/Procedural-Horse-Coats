package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Axis;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.BodyPoint;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Bounds;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.spec.GeneSpec;
import com.example.horsegenetics.common.genetics.spec.GeneSpec.Layer;
import com.example.horsegenetics.common.genetics.spec.GeneSpec.Mask;
import com.example.horsegenetics.common.genetics.spec.GeneSpec.Op;
import com.example.horsegenetics.common.genetics.spec.GeneSpec.Params;
import com.example.horsegenetics.common.genetics.spec.SpecValues;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Executes a {@link GeneSpec}'s layers against the coat - the interpreter that
 * makes a JSON gene a real gene.
 *
 * <p>Every layer is <b>where times what</b>: its masks fold into one coverage
 * number per texel, and its op is applied scaled by that number. Because
 * coverage scales the effect rather than gating it, a spec gene's edges are soft
 * by construction - which is the one thing the hand-written genes each had to
 * remember separately, and the reason splash's socks still end in a hard ring
 * (see {@code wiki/verification.html}).
 *
 * <p>Pure, like every other pattern class here: same spec + same drawn
 * {@link SpecValues} + same coat in, same field out. All the randomness was
 * spent before this class was called.
 *
 * <p><b>This has a twin.</b> {@code wiki/gene-creator/js/spec-engine.js} is the
 * same interpreter in JavaScript, so the creator's preview is the game's
 * output. Changing the meaning of a mask or an op here without changing it there
 * makes the tool lie; {@code SpecEngineFixtureTest} is the tripwire.
 */
public final class SpecPainter {

    private SpecPainter() {}

    // ------------------------------------------------------------------
    // Phase 1 - natural
    // ------------------------------------------------------------------

    /**
     * Apply one expression's natural layers to a copy of {@code coat}.
     *
     * <p>{@code layers} comes from the expression the horse's allele
     * combination landed on, not from the gene as a whole - two combinations of
     * the same gene can paint entirely different things.
     */
    public static PigmentField restrict(GeneSpec spec, List<Layer> layers, SpecValues values,
                                        CoatBuildContext ctx, PigmentView coat) {
        Skin skin = ctx.skin();
        Map<Part, Bounds> bounds = boundsOf(skin);
        PigmentField field = coat.mutableCopy();
        for (int i = 0; i < layers.size(); i++) {
            Layer layer = layers.get(i);
            long fallbackSeed = layerSeed(spec, i);
            // Masks read the coat as the *previous layer* left it, which is what
            // lets a PIGMENT mask chain ("darken what the layer above blacked").
            PigmentView asRead = field.mutableCopy();
            PigmentField target = field;
            HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> {
                int leg = legIndex(part);
                double k = coverage(layer, values, skin, bounds, part, point, asRead, px, py, leg, fallbackSeed);
                if (k > 0) {
                    applyPigment(layer.op(), values, target, px, py, leg, k);
                }
            });
        }
        return field;
    }

    private static void applyPigment(Op op, SpecValues v, PigmentField f, int px, int py, int leg, double k) {
        Params p = op.params();
        switch (op.type()) {
            case DILUTE -> {
                double keepRed = lerp(1.0, v.get(p.value("keepRed", 1.0), leg), k);
                double keepBlack = lerp(1.0, v.get(p.value("keepBlack", 1.0), leg), k);
                double tint = v.get(p.value("blackTint", 0.0), leg) * k;
                f.dilute(px, py, (float) keepRed, (float) keepBlack, (float) tint);
            }
            case RESTRICT -> {
                f.restrictRed(px, py, (float) (v.get(p.value("red", 0.0), leg) * k));
                f.restrictBlack(px, py, (float) (v.get(p.value("black", 0.0), leg) * k));
            }
            case SET_PIGMENT -> {
                if (p.has("red")) {
                    f.setRed(px, py, (float) lerp(f.red(px, py), v.get(p.value("red", 0.0), leg), k));
                }
                if (p.has("black")) {
                    f.setBlack(px, py, (float) lerp(f.black(px, py), v.get(p.value("black", 0.0), leg), k));
                }
            }
            default -> throw new IllegalStateException("not a pigment op: " + op.type());
        }
    }

    // ------------------------------------------------------------------
    // Phase 3 - magical
    // ------------------------------------------------------------------

    /** Build one expression's signed colour delta. See {@link #restrict} on {@code layers}. */
    public static ColorField tint(GeneSpec spec, List<Layer> layers, SpecValues values,
                                  CoatBuildContext ctx, PigmentView coat, ColorView colour) {
        Skin skin = ctx.skin();
        Map<Part, Bounds> bounds = boundsOf(skin);
        ColorField delta = ColorField.deltaLike(colour);
        for (int i = 0; i < layers.size(); i++) {
            Layer layer = layers.get(i);
            long fallbackSeed = layerSeed(spec, i);
            HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> {
                int leg = legIndex(part);
                double k = coverage(layer, values, skin, bounds, part, point, coat, px, py, leg, fallbackSeed);
                if (k > 0) {
                    applyColour(layer.op(), values, delta, colour, px, py, leg, k);
                }
            });
        }
        return delta;
    }

    private static void applyColour(Op op, SpecValues v, ColorField delta, ColorView colour,
                                    int px, int py, int leg, double k) {
        Params p = op.params();
        switch (op.type()) {
            case TINT -> {
                delta.add(px, py,
                        percentToChannel(v.get(p.value("red", 0.0), leg) * k),
                        percentToChannel(v.get(p.value("green", 0.0), leg) * k),
                        percentToChannel(v.get(p.value("blue", 0.0), leg) * k));
                delta.addOpacity(px, py, percentToChannel(v.get(p.value("opacity", 100.0), leg) * k));
            }
            case TOWARD -> {
                int rgb = p.color("color", 0xFFFFFF);
                double strength = v.get(p.value("strength", 100.0), leg) / 100.0 * k;
                delta.add(px, py,
                        toward(colour, px, py, 0, (rgb >> 16) & 0xFF, strength),
                        toward(colour, px, py, 1, (rgb >> 8) & 0xFF, strength),
                        toward(colour, px, py, 2, rgb & 0xFF, strength));
                int wantOpacity = percentToChannel(v.get(p.value("opacity", 100.0), leg));
                delta.addOpacity(px, py, (int) Math.round((wantOpacity - colour.opacity(px, py)) * k));
            }
            case FLAT -> {
                int rgb = p.color("color", 0xFFFFFF);
                int wantOpacity = percentToChannel(v.get(p.value("opacity", 100.0), leg));
                delta.set(px, py,
                        (int) Math.round(lerp(colour.opacity(px, py), wantOpacity, k)),
                        (int) Math.round(lerp(colour.red(px, py), (rgb >> 16) & 0xFF, k)),
                        (int) Math.round(lerp(colour.green(px, py), (rgb >> 8) & 0xFF, k)),
                        (int) Math.round(lerp(colour.blue(px, py), rgb & 0xFF, k)));
            }
            default -> throw new IllegalStateException("not a colour op: " + op.type());
        }
    }

    /**
     * The signed step from a texel's accumulated channel to {@code target},
     * measured against what the texel <b>looks like</b> rather than what it
     * stores - the {@code PinkHairGene} move, generalised. Reading
     * {@link ColorView#visible} is what lets one number ("82% of the way to
     * pink") land on a black mane and a cremello one alike.
     */
    private static int toward(ColorView colour, int px, int py, int channel, int target, double strength) {
        int seen = colour.visible(px, py, channel);
        double wanted = seen + (target - seen) * strength;
        int stored = switch (channel) {
            case 0 -> colour.red(px, py);
            case 1 -> colour.green(px, py);
            default -> colour.blue(px, py);
        };
        return (int) Math.round(wanted - stored);
    }

    // ------------------------------------------------------------------
    // Masks
    // ------------------------------------------------------------------

    private static double coverage(Layer layer, SpecValues v, Skin skin, Map<Part, Bounds> bounds,
                                   Part part, BodyPoint point, PigmentView coat,
                                   int px, int py, int leg, long fallbackSeed) {
        double acc = 1.0;
        List<Mask> masks = layer.masks();
        for (int i = 0; i < masks.size(); i++) {
            Mask mask = masks.get(i);
            double c = maskCoverage(mask, v, skin, bounds, part, point, coat, px, py, leg,
                    fallbackSeed ^ ((long) i * 0x9E3779B97F4A7C15L));
            if (mask.invert()) {
                c = 1.0 - c;
            }
            acc = switch (mask.combine()) {
                case MULTIPLY -> acc * c;
                case MAX -> Math.max(acc, c);
                case MIN -> Math.min(acc, c);
                case ADD -> clamp01(acc + c);
                case SUBTRACT -> clamp01(acc - c);
            };
            if (acc <= 0 && mask.combine() == GeneSpec.Combine.MULTIPLY) {
                return 0;
            }
        }
        return clamp01(acc);
    }

    private static double maskCoverage(Mask mask, SpecValues v, Skin skin, Map<Part, Bounds> bounds,
                                       Part part, BodyPoint point, PigmentView coat,
                                       int px, int py, int leg, long seedBase) {
        Params p = mask.params();
        List<Part> parts = p.parts("parts");
        if (!parts.isEmpty() && !parts.contains(part) && mask.type() != GeneSpec.MaskType.PARTS) {
            return 0;
        }
        switch (mask.type()) {
            case ALL:
                return 1;
            case PARTS:
                return parts.contains(part) ? 1 : 0;
            case AXIS: {
                Axis axis = Axis.valueOf(p.text("axis", "Y").toUpperCase(java.util.Locale.ROOT));
                double coord = point.along(axis);
                double t = switch (p.text("space", "part")) {
                    case "body" -> normalise(coord, HorseSkinGeometry.bodyBounds(skin), axis);
                    case "units" -> coord;
                    default -> normalise(coord, bounds.get(part), axis);
                };
                return band(t, v.get(p.value("from", 0.0), leg), v.get(p.value("to", 1.0), leg),
                        v.get(p.value("softness", 0.15), leg));
            }
            case CENTERLINE: {
                double d = Math.abs(point.z() - v.get(p.value("offset", 0.0), leg));
                double half = v.get(p.value("halfWidth", 1.0), leg);
                double soft = v.get(p.value("softness", 0.35), leg);
                return 1.0 - BodyStripes.smoothstep(half, half + Math.max(1e-6, soft), d);
            }
            case STRIPES: {
                long seed = v.seed(p.value("seed", 0), seedBase);
                return BodyStripes.coverage(seed, point.x(), point.y(), point.z(),
                        Math.max(0.01, v.get(p.value("spacing", 3.0), leg)),
                        clamp01(v.get(p.value("duty", 0.45), leg)),
                        v.get(p.value("warp", 1.0), leg));
            }
            case DAPPLES: {
                long seed = v.seed(p.value("seed", 0), seedBase);
                double spacing = Math.max(0.05, v.get(p.value("spacing", 3.5), leg));
                double warp = v.get(p.value("warp", 0.45), leg) * spacing;
                double warpScale = 1.0 / (spacing * 3.0);
                double n = BodyNoise.value(seed ^ 0x51L,
                        point.x() * warpScale, point.y() * warpScale, point.z() * warpScale);
                double m = BodyNoise.value(seed ^ 0x52L,
                        point.z() * warpScale, point.x() * warpScale, point.y() * warpScale);
                double d = BodyNoise.cellDistance(seed,
                        (point.x() + (n - 0.5) * warp) / spacing,
                        (point.y() + (m - 0.5) * warp) / spacing,
                        (point.z() + (n - m) * warp) / spacing);
                return 1.0 - BodyStripes.smoothstep(v.get(p.value("edge0", 0.35), leg),
                        v.get(p.value("edge1", 0.78), leg), d);
            }
            case PATCHES: {
                long seed = v.seed(p.value("seed", 0), seedBase);
                double scale = Math.max(0.05, v.get(p.value("scale", 6.0), leg));
                double n = BodyNoise.value(seed, point.x() / scale, point.y() / scale, point.z() / scale);
                double threshold = v.get(p.value("threshold", 0.5), leg);
                double soft = Math.max(1e-6, v.get(p.value("softness", 0.12), leg));
                return BodyStripes.smoothstep(threshold - soft, threshold + soft, n);
            }
            case NOISE: {
                long seed = v.seed(p.value("seed", 0), seedBase);
                double scale = Math.max(0.05, v.get(p.value("scale", 8.0), leg));
                double n = BodyNoise.value(seed, point.x() / scale, point.y() / scale, point.z() / scale);
                double low = v.get(p.value("low", 0.0), leg);
                return clamp01(low + (v.get(p.value("high", 1.0), leg) - low) * n);
            }
            case PIGMENT: {
                float red = coat.red(px, py);
                float black = coat.black(px, py);
                double reading = switch (p.text("channel", "darkness")) {
                    case "red" -> red;
                    case "black" -> black;
                    case "total" -> (red + black) / 2.0;
                    default -> clamp01(0.55 * red + 0.95 * black);
                };
                return BodyStripes.smoothstep(v.get(p.value("from", 0.5), leg),
                        v.get(p.value("to", 1.0), leg), reading);
            }
            default:
                throw new IllegalStateException("unhandled mask " + mask.type());
        }
    }

    /**
     * 1 inside {@code [from, to]}, fading to 0 across {@code softness} either
     * side. A zero {@code softness} gives a hard edge - available, but the
     * default is soft because a marking that stops on a line reads as a bug.
     */
    private static double band(double t, double from, double to, double softness) {
        double soft = Math.max(1e-6, softness);
        return BodyStripes.smoothstep(from - soft, from, t) * (1.0 - BodyStripes.smoothstep(to, to + soft, t));
    }

    // ------------------------------------------------------------------

    private static Map<Part, Bounds> boundsOf(Skin skin) {
        Map<Part, Bounds> out = new EnumMap<>(Part.class);
        for (Part part : Part.values()) {
            if (HorseSkinGeometry.hasPart(skin, part)) {
                out.put(part, HorseSkinGeometry.bounds(skin, part));
            }
        }
        return out;
    }

    private static double normalise(double coord, Bounds b, Axis axis) {
        double span = b.span(axis);
        return span == 0 ? 0 : (coord - b.min(axis)) / span;
    }

    private static int legIndex(Part part) {
        return CoatRegions.LEGS.indexOf(part);
    }

    /**
     * The seed a noise mask uses when the author declared none: stable across
     * sessions, different per gene and per layer, so two layers of the same gene
     * don't come out on top of each other.
     */
    private static long layerSeed(GeneSpec spec, int layerIndex) {
        return (spec.key().hashCode() * 0x9E3779B97F4A7C15L) ^ ((layerIndex + 1) * 0xC2B2AE3D27D4EB4FL);
    }

    private static int percentToChannel(double percent) {
        double v = Math.round(255.0 * percent / 100.0);
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, v));
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }
}
