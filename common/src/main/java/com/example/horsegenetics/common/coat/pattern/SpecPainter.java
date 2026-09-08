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
            case WHITEN -> f.whiten(px, py, (float) (v.get(p.value("amount", 1.0), leg) * k));
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
                    applyColour(layer.op(), values, delta, colour, skin, bounds, part, point,
                            px, py, leg, k, fallbackSeed);
                }
            });
        }
        return delta;
    }

    // ------------------------------------------------------------------
    // Phase 4 - the overlay, where glow is decided
    // ------------------------------------------------------------------

    /**
     * Mark every texel an {@code emissive} layer covers as full-bright.
     *
     * <p>It runs in the <b>overlay</b> pass rather than in {@link #tint},
     * because that is the pass {@code CoatOverlay} - and so
     * {@code markEmissive} - exists in. The layers are re-walked rather than
     * remembered from phase 3: masks are pure functions of the geometry and the
     * horse's drawn values, so walking them twice costs a little time and buys
     * not having to carry a boolean sheet through two phases that have no use
     * for it.
     *
     * <p>The one thing lost by running here is the coat: a {@code PIGMENT} mask
     * has no pigment field to read once the texture is baked. {@code
     * GeneSpecParser} rejects that combination outright rather than letting it
     * quietly read as zero.
     */
    public static void emissive(GeneSpec spec, List<Layer> layers, SpecValues values,
                                Skin skin, CoatOverlay out) {
        Map<Part, Bounds> bounds = boundsOf(skin);
        for (int i = 0; i < layers.size(); i++) {
            Layer layer = layers.get(i);
            if (!layer.emissive()) {
                continue;
            }
            long fallbackSeed = layerSeed(spec, i);
            HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> {
                double k = coverage(layer, values, skin, bounds, part, point, null, px, py,
                        legIndex(part), fallbackSeed);
                if (k >= GeneSpec.EMISSIVE_THRESHOLD) {
                    out.markEmissive(px, py);
                }
            });
        }
    }

    private static void applyColour(Op op, SpecValues v, ColorField delta, ColorView colour,
                                    Skin skin, Map<Part, Bounds> bounds, Part part, BodyPoint point,
                                    int px, int py, int leg, double k, long seedBase) {
        Params p = op.params();
        switch (op.type()) {
            case TINT -> {
                delta.add(px, py,
                        percentToChannel(v.get(p.value("red", 0.0), leg) * k),
                        percentToChannel(v.get(p.value("green", 0.0), leg) * k),
                        percentToChannel(v.get(p.value("blue", 0.0), leg) * k));
                delta.addOpacity(px, py, percentToChannel(v.get(p.value("opacity", 100.0), leg) * k));
            }
            case TOWARD -> towardColour(delta, colour, p, v, leg, px, py, k, solidColour(op, v, leg));
            case RAMP -> {
                int rgb = rampColour(op, v, leg, axisPosition(op, v, leg, skin, bounds, part, point));
                towardColour(delta, colour, p, v, leg, px, py, k, rgb);
            }
            case PALETTE -> {
                int rgb = paletteColour(op, v, leg, point, seedBase);
                towardColour(delta, colour, p, v, leg, px, py, k, rgb);
            }
            case INVERT -> {
                // Against what the texel LOOKS like, not what it stores - the
                // same reading `toward` uses. Inverting the accumulator instead
                // would give a different negative on every base coat, which is
                // the one thing an inversion must not do.
                double amount = v.get(p.value("amount", 100.0), leg) / 100.0 * k;
                delta.add(px, py,
                        toward(colour, px, py, 0, 255 - colour.visible(px, py, 0), amount),
                        toward(colour, px, py, 1, 255 - colour.visible(px, py, 1), amount),
                        toward(colour, px, py, 2, 255 - colour.visible(px, py, 2), amount));
                int wantOpacity = percentToChannel(v.get(p.value("opacity", 100.0), leg));
                delta.addOpacity(px, py, (int) Math.round((wantOpacity - colour.opacity(px, py)) * k));
            }
            case FLAT -> {
                int rgb = solidColour(op, v, leg);
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
     * The {@code TOWARD} move, shared by the three ops that make it: walk the
     * texel's <i>visible</i> colour a share of the way to {@code rgb}, then take
     * its opacity a matching share of the way to what the layer asked for. All
     * that separates {@code TOWARD}, {@code RAMP} and {@code PALETTE} is where
     * {@code rgb} came from.
     */
    private static void towardColour(ColorField delta, ColorView colour, Params p, SpecValues v,
                                     int leg, int px, int py, double k, int rgb) {
        double strength = v.get(p.value("strength", 100.0), leg) / 100.0 * k;
        delta.add(px, py,
                toward(colour, px, py, 0, (rgb >> 16) & 0xFF, strength),
                toward(colour, px, py, 1, (rgb >> 8) & 0xFF, strength),
                toward(colour, px, py, 2, rgb & 0xFF, strength));
        int wantOpacity = percentToChannel(v.get(p.value("opacity", 100.0), leg));
        delta.addOpacity(px, py, (int) Math.round((wantOpacity - colour.opacity(px, py)) * k));
    }

    /**
     * The colour a {@code TOWARD} or {@code FLAT} layer aims at: the literal
     * {@code color}, unless {@code hue} is 0 or above - in which case it is
     * built from HSL, and {@code hue} may be a knob. That one indirection is
     * what lets a single gene file paint a different colour on every line of
     * horses rather than the one colour its author happened to type.
     *
     * <p>The switch is a <b>negative sentinel</b> rather than "is the key
     * present", because the gene creator carries every parameter at its default
     * whether or not the author touched it. A presence test would have the
     * preview take the HSL path and the exported file take the colour one.
     */
    private static int solidColour(Op op, SpecValues v, int leg) {
        Params p = op.params();
        double hue = v.get(p.value("hue", -1), leg);
        if (hue < 0) {
            return p.color("color", 0xFFFFFF);
        }
        return hsl(hue, v.get(p.value("saturation", 0.8), leg),
                v.get(p.value("lightness", 0.55), leg));
    }

    /** Where along a {@code RAMP}'s axis this texel sits, as a 0..1 position between its stops. */
    private static double axisPosition(Op op, SpecValues v, int leg, Skin skin,
                                       Map<Part, Bounds> bounds, Part part, BodyPoint point) {
        Params p = op.params();
        Axis axis = Axis.valueOf(p.text("axis", "X").toUpperCase(java.util.Locale.ROOT));
        double coord = point.along(axis);
        Bounds partBounds = bounds.get(part);
        double t = switch (p.text("space", "part")) {
            case "body" -> normalise(coord, HorseSkinGeometry.bodyBounds(skin), axis);
            case "units" -> coord;
            default -> partBounds == null ? 0 : normalise(coord, partBounds, axis);
        };
        double from = v.get(p.value("from", 0.0), leg);
        double to = v.get(p.value("to", 1.0), leg);
        return to == from ? 0 : clamp01((t - from) / (to - from));
    }

    /** The stop, or the swept hue, a {@code RAMP} reaches at position {@code t}. */
    private static int rampColour(Op op, SpecValues v, int leg, double t) {
        Params p = op.params();
        List<Integer> stops = p.colors("colors");
        if (stops.isEmpty()) {
            double span = v.get(p.value("hueSpan", 60.0), leg);
            return hsl(v.get(p.value("hue", 0), leg) + span * t,
                    v.get(p.value("saturation", 0.8), leg),
                    v.get(p.value("lightness", 0.55), leg));
        }
        // n stops make n-1 segments; t lands in one and interpolates across it,
        // so the ramp comes out continuous rather than banded.
        double scaled = t * (stops.size() - 1);
        int i = (int) Math.floor(scaled);
        if (i >= stops.size() - 1) {
            return stops.get(stops.size() - 1);
        }
        return mixRgb(stops.get(i), stops.get(i + 1), scaled - i);
    }

    /**
     * The colour a {@code PALETTE} layer gives this texel: one entry of the
     * palette, chosen by the cell the texel falls in. Neighbouring cells take
     * unrelated entries and meet at a wall, which is the whole difference
     * between an opal and a gradient.
     */
    private static int paletteColour(Op op, SpecValues v, int leg, BodyPoint point, long seedBase) {
        Params p = op.params();
        long seed = v.seed(p.value("seed", 0), seedBase);
        double scale = Math.max(0.05, v.get(p.value("scale", 5.0), leg));
        BodyNoise.Cell cell = BodyNoise.cell(seed,
                point.x() / scale, point.y() / scale, point.z() / scale);
        List<Integer> palette = p.colors("colors");
        if (!palette.isEmpty()) {
            int i = (int) (cell.pick() * palette.size());
            return palette.get(Math.min(i, palette.size() - 1));
        }
        double spread = v.get(p.value("hueSpread", 40.0), leg);
        return hsl(v.get(p.value("hue", 0), leg) + (cell.pick() * 2 - 1) * spread,
                v.get(p.value("saturation", 0.8), leg),
                v.get(p.value("lightness", 0.55), leg));
    }

    private static int mixRgb(int a, int b, double t) {
        int r = (int) Math.round(lerp((a >> 16) & 0xFF, (b >> 16) & 0xFF, t));
        int g = (int) Math.round(lerp((a >> 8) & 0xFF, (b >> 8) & 0xFF, t));
        int bl = (int) Math.round(lerp(a & 0xFF, b & 0xFF, t));
        return (r << 16) | (g << 8) | bl;
    }

    /** HSL to {@code 0xRRGGBB}. {@code h} is in degrees and wraps; {@code s} and {@code l} clamp. */
    static int hsl(double h, double s, double l) {
        double hue = (((h % 360) + 360) % 360) / 60.0;
        double sat = clamp01(s);
        double light = clamp01(l);
        double c = (1 - Math.abs(2 * light - 1)) * sat;
        double x = c * (1 - Math.abs(hue % 2 - 1));
        double m = light - c / 2;
        double r;
        double g;
        double b;
        if (hue < 1) {
            r = c;
            g = x;
            b = 0;
        } else if (hue < 2) {
            r = x;
            g = c;
            b = 0;
        } else if (hue < 3) {
            r = 0;
            g = c;
            b = x;
        } else if (hue < 4) {
            r = 0;
            g = x;
            b = c;
        } else if (hue < 5) {
            r = x;
            g = 0;
            b = c;
        } else {
            r = c;
            g = 0;
            b = x;
        }
        return (channel(r + m) << 16) | (channel(g + m) << 8) | channel(b + m);
    }

    private static int channel(double v) {
        int i = (int) Math.round(v * 255);
        return i < 0 ? 0 : (i > 255 ? 255 : i);
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
            c = spread(mask, v, skin, coat, part, point, px, py, leg, c);
            acc = switch (mask.combine()) {
                case MULTIPLY -> acc * c;
                case MAX -> Math.max(acc, c);
                case MIN -> Math.min(acc, c);
                case ADD -> clamp01(acc + c);
                case SUBTRACT -> clamp01(acc - c);
            };
            // Bail out early only when nothing left can put coverage BACK.
            // The test used to be "this mask multiplied and the result is 0",
            // which quietly threw away every union: a layer masked "the
            // shoulder, MAX the hindquarter" returned 0 for the whole horse
            // behind the shoulder, because the first mask was 0 there and the
            // second was never reached. Panda lost half its pattern to it and
            // nothing went red - both engines agreed, because the JavaScript
            // twin had the same line.
            if (acc <= 0 && cannotRise(masks, i + 1)) {
                return 0;
            }
        }
        return clamp01(acc);
    }

    /** Can any mask from {@code from} on raise the accumulator above zero? */
    private static boolean cannotRise(List<Mask> masks, int from) {
        for (int i = from; i < masks.size(); i++) {
            GeneSpec.Combine c = masks.get(i).combine();
            if (c == GeneSpec.Combine.MAX || c == GeneSpec.Combine.ADD) {
                return false;
            }
        }
        return true;
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
            case PIGMENT:
                return pigmentCoverage(mask, v, coat, px, py, leg);
            case SPOTS: {
                long seed = v.seed(p.value("seed", 0), seedBase);
                double spacing = Math.max(0.05, v.get(p.value("spacing", 4.0), leg));
                double stretch = Math.max(0.05, v.get(p.value("stretch", 1.0), leg));
                Axis longAxis = Axis.valueOf(p.text("axis", "X").toUpperCase(java.util.Locale.ROOT));
                // Squashing the sample along one axis before the lattice walk is
                // what makes an oval: the cells stay round in the squashed frame
                // and come out stretched on the horse.
                // |z| when mirrored, so the far flank draws the near one's spots.
                double sz = p.flag("mirror", false) ? Math.abs(point.z()) : point.z();
                BodyNoise.Cell cell = BodyNoise.cell(seed,
                        point.x() / (spacing * (longAxis == Axis.X ? stretch : 1)),
                        point.y() / (spacing * (longAxis == Axis.Y ? stretch : 1)),
                        sz / (spacing * (longAxis == Axis.Z ? stretch : 1)));
                if (cell.pick() >= v.get(p.value("chance", 1.0), leg)) {
                    return 0;
                }
                double vary = clamp01(v.get(p.value("vary", 0.5), leg));
                double radius = v.get(p.value("radius", 0.9), leg) * (1 - vary + 2 * vary * cell.size());
                if ("heart".equals(p.text("shape", "round"))) {
                    radius *= heartRadius(Math.atan2(cell.dy(), cell.dx()));
                }
                double soft = Math.max(1e-6, v.get(p.value("softness", 0.25), leg));
                return 1.0 - BodyStripes.smoothstep(radius, radius + soft, cell.distance() * spacing);
            }
            case RINGS: {
                long seed = v.seed(p.value("seed", 0), seedBase);
                double spacing = Math.max(0.05, v.get(p.value("spacing", 6.0), leg));
                BodyNoise.Cell cell = BodyNoise.cell(seed,
                        point.x() / spacing, point.y() / spacing, point.z() / spacing);
                if (cell.pick() >= v.get(p.value("chance", 1.0), leg)) {
                    return 0;
                }
                double vary = clamp01(v.get(p.value("vary", 0.4), leg));
                double radius = v.get(p.value("radius", 2.0), leg) * (1 - vary + 2 * vary * cell.size());
                double half = Math.max(1e-6, v.get(p.value("thickness", 0.6), leg)) / 2;
                double soft = Math.max(1e-6, v.get(p.value("softness", 0.2), leg));
                double ring = 1.0 - BodyStripes.smoothstep(half, half + soft,
                        Math.abs(cell.distance() * spacing - radius));
                double arc = clamp01(v.get(p.value("arc", 1.0), leg));
                if (arc >= 1) {
                    return ring;
                }
                // A crescent is a ring with a wedge missing. The wedge opens at
                // the ring's own angle, so every crescent on the horse faces a
                // different way instead of all of them pointing at one corner.
                double theta = Math.atan2(cell.dy(), cell.dz()) / (2 * Math.PI) + 0.5;
                return (((theta - cell.angle()) % 1) + 1) % 1 <= arc ? ring : 0;
            }
            case SPECKLE: {
                long seed = v.seed(p.value("seed", 0), seedBase);
                double spacing = Math.max(0.02, v.get(p.value("spacing", 0.7), leg));
                BodyNoise.Cell cell = BodyNoise.cell(seed,
                        point.x() / spacing, point.y() / spacing, point.z() / spacing);
                double density = clamp01(v.get(p.value("density", 0.5), leg));
                double clumping = clamp01(v.get(p.value("clumping", 0.0), leg));
                if (clumping > 0) {
                    double clumpScale = Math.max(0.05, v.get(p.value("clumpScale", 7.0), leg));
                    double n = BodyNoise.value(seed ^ 0x5EC1EL,
                            point.x() / clumpScale, point.y() / clumpScale, point.z() / clumpScale);
                    density = clamp01(density * (1 - clumping + 2 * clumping * n));
                }
                if (cell.pick() >= density) {
                    return 0;
                }
                double radius = clamp01(v.get(p.value("size", 0.45), leg));
                double soft = Math.max(1e-6, v.get(p.value("softness", 0.3), leg)) * radius;
                return 1.0 - BodyStripes.smoothstep(radius, radius + soft, cell.distance());
            }
            case STROKES: {
                long seed = v.seed(p.value("seed", 0), seedBase);
                double spacing = Math.max(0.05, v.get(p.value("spacing", 3.0), leg));
                double length = Math.max(0.05, v.get(p.value("length", 12.0), leg));
                Axis longAxis = Axis.valueOf(p.text("axis", "X").toUpperCase(java.util.Locale.ROOT));
                double curl = v.get(p.value("curl", 0.35), leg) * spacing;
                // Warping the sample before the field is what stops the strokes
                // reading as a comb: it is the difference between parallel lines
                // and lines that wander, fork and pinch out the way drawn ones do.
                double w = BodyNoise.value(seed ^ 0x71L, point.x() / (spacing * 4),
                        point.y() / (spacing * 4), point.z() / (spacing * 4));
                double sx = (point.x() + (longAxis == Axis.X ? 0 : (w - 0.5) * curl))
                        / (longAxis == Axis.X ? length : spacing);
                double sy = (point.y() + (longAxis == Axis.Y ? 0 : (w - 0.5) * curl))
                        / (longAxis == Axis.Y ? length : spacing);
                double sz = (point.z() + (longAxis == Axis.Z ? 0 : (w - 0.5) * curl))
                        / (longAxis == Axis.Z ? length : spacing);
                double half = Math.max(1e-4, v.get(p.value("width", 0.8), leg)) / 2;
                double soft = Math.max(1e-6, v.get(p.value("softness", 0.25), leg));
                return 1.0 - BodyStripes.smoothstep(half, half + soft,
                        levelSetDistance(seed, sx, sy, sz) * spacing);
            }
            case SPIRAL: {
                long seed = v.seed(p.value("seed", 0), seedBase);
                Bounds b = bounds.get(part);
                if (b == null) {
                    return 0;
                }
                Axis view = Axis.valueOf(p.text("axis", "Z").toUpperCase(java.util.Locale.ROOT));
                Axis u = view == Axis.X ? Axis.Y : Axis.X;
                Axis w2 = view == Axis.Z ? Axis.Y : Axis.Z;
                double cu = (b.min(u) + b.max(u)) / 2 + v.get(p.value("offset", 0.0), leg) * b.span(u);
                double cw = (b.min(w2) + b.max(w2)) / 2;
                double du = point.along(u) - cu;
                double dw = point.along(w2) - cw;
                double r = Math.sqrt(du * du + dw * dw);
                double outer = Math.max(0.05, v.get(p.value("radius", 4.0), leg));
                if (r > outer) {
                    return 0;
                }
                // Archimedean: the arm sits wherever the radius matches how far
                // the angle has turned, and the rounding picks whichever
                // revolution of the arm is nearest to this texel.
                double turns = Math.max(0.25, v.get(p.value("turns", 2.0), leg));
                double pitch = outer / turns;
                double theta = Math.atan2(dw, du) / (2 * Math.PI) + (seed & 0xFF) / 255.0;
                double offsetR = r - ((theta % 1) + 1) % 1 * pitch;
                double nearest = Math.abs(offsetR - Math.round(offsetR / pitch) * pitch);
                double half = Math.max(1e-6, v.get(p.value("width", 0.5), leg)) / 2;
                double soft = Math.max(1e-6, v.get(p.value("softness", 0.2), leg));
                return 1.0 - BodyStripes.smoothstep(half, half + soft, nearest);
            }
            case WAVES: {
                long seed = v.seed(p.value("seed", 0), seedBase);
                Axis along = Axis.valueOf(p.text("axis", "X").toUpperCase(java.util.Locale.ROOT));
                Axis across = Axis.valueOf(p.text("across", "Y").toUpperCase(java.util.Locale.ROOT));
                // The sine's own coordinate stays in body units whatever space
                // the band is measured in, so 'wavelength' means one thing.
                double travel = point.along(along);
                double coord = point.along(across);
                double t = switch (p.text("space", "part")) {
                    case "body" -> normalise(coord, HorseSkinGeometry.bodyBounds(skin), across);
                    case "units" -> coord;
                    default -> normalise(coord, bounds.get(part), across);
                };
                String shape = p.text("shape", "sine");
                double lambda = Math.max(0.05, v.get(p.value("wavelength", 8.0), leg));
                double amplitude = v.get(p.value("amplitude", 0.5), leg);
                double from = v.get(p.value("from", 0.0), leg);
                double to = v.get(p.value("to", 1.0), leg);
                double soft = v.get(p.value("softness", 0.15), leg);
                double spacing = v.get(p.value("spacing", 0.0), leg);
                if (spacing <= 0) {
                    return band(t - amplitude * waveform(shape, travel / lambda), from, to, soft);
                }
                // Repeating: which ribbon a texel belongs to is decided by its
                // UNDISPLACED position, or the phase would depend on the answer
                // it is being used to compute. The neighbours either side are
                // tried too, because a displaced ribbon reaches into the next
                // lane and would otherwise be clipped off at the boundary.
                double phase = v.get(p.value("phase", 0.0), leg);
                double mid = (from + to) / 2;
                int lane = (int) Math.round((t - mid) / spacing);
                double best = 0;
                for (int n = lane - 1; n <= lane + 1; n++) {
                    double jitter = phase == 0 ? 0
                            : phase * BodyNoise.value(seed ^ 0x77L, n, 0.25, 0.25);
                    double centre = n * spacing
                            + amplitude * waveform(shape, travel / lambda + jitter);
                    best = Math.max(best, band(t - centre, from, to, soft));
                }
                return best;
            }
            case CRACKLE: {
                long seed = v.seed(p.value("seed", 0), seedBase);
                double scale = Math.max(0.05, v.get(p.value("scale", 5.0), leg));
                double warp = v.get(p.value("warp", 0.35), leg);
                double wx = point.x() / scale;
                double wy = point.y() / scale;
                double wz = point.z() / scale;
                if (warp != 0) {
                    // Warping the SAMPLE rather than the lattice keeps the walls
                    // straight and the corners three-way; it is the polygons
                    // that come out irregular, which is the giraffe.
                    double n = BodyNoise.value(seed ^ 0x2CL, wx / 3, wy / 3, wz / 3);
                    double m = BodyNoise.value(seed ^ 0x2DL, wz / 3, wx / 3, wy / 3);
                    wx += (n - 0.5) * warp;
                    wy += (m - 0.5) * warp;
                    wz += (n - m) * warp;
                }
                double chance = v.get(p.value("chance", 1.0), leg);
                if (chance < 1 && BodyNoise.cell(seed, wx, wy, wz).pick() >= chance) {
                    return 0;
                }
                double half = Math.max(1e-4, v.get(p.value("gap", 0.5), leg)) / 2;
                double soft = Math.max(1e-6, v.get(p.value("softness", 0.08), leg));
                return BodyStripes.smoothstep(half, half + soft,
                        BodyNoise.cellEdge(seed, wx, wy, wz) * scale);
            }
            default:
                throw new IllegalStateException("unhandled mask " + mask.type());
        }
    }

    /**
     * One cycle of a {@code WAVES} displacement, in {@code [-1, 1]}, at
     * {@code turns} full turns from the origin.
     *
     * <p>All three start at 0 and rise, so swapping one for another keeps the
     * marking in the same place and changes only whether its edge is curved,
     * folded or cut.
     */
    private static double waveform(String shape, double turns) {
        double t = turns - Math.floor(turns);
        return switch (shape) {
            case "triangle" -> t < 0.25 ? 4 * t : (t < 0.75 ? 2 - 4 * t : 4 * t - 4);
            case "saw" -> 2 * t - 1;
            default -> Math.sin(2 * Math.PI * turns);
        };
    }

    /**
     * A {@code PIGMENT} mask's {@code spread}: <b>grow whatever this mask
     * selected</b> outward by a radius, by taking the largest coverage found in
     * a disc around the texel.
     *
     * <p>It runs here rather than inside {@link #maskCoverage} because it has
     * to happen <b>after {@code invert}</b>, and that is the whole of the
     * design. Dilating the <i>reading</i> can only ever grow one side of it:
     * the first version took the lowest reading in the disc, which grows the
     * pale, and fielded wanted exactly that. Integration wants the opposite -
     * it spreads out of the black points, so it needs the dark grown. Dilating
     * the mask's <i>output</i> serves both without a second parameter, because
     * an inverted mask has already turned "dark" into "what I selected". For
     * fielded the two are the same operation: the maximum of {@code 1 - s(r)}
     * is {@code 1 - } the minimum of {@code s(r)}.
     *
     * <p><b>The radius is in body units, and the test is done in body space.</b>
     * The sheet window is only a bound on the search: a texture atlas packs
     * unrelated parts next to each other, so a disc measured in <i>texels</i>
     * leaks across a UV seam and grows the marking somewhere it has no business
     * being. Integration is what proved it - dilating three units off a bay's
     * black socks covered the entire horse, because the leg patches sit beside
     * the barrel on the sheet. Asking the geometry where each candidate texel
     * actually <i>is</i> costs one cached grid lookup, and {@code sample} is a
     * table.
     */
    private static double spread(Mask mask, SpecValues v, Skin skin, PigmentView coat,
                                 Part part, BodyPoint point, int px, int py, int leg,
                                 double c) {
        if (mask.type() != GeneSpec.MaskType.PIGMENT || c >= 1) {
            return c;
        }
        Params p = mask.params();
        List<Part> parts = p.parts("parts");
        if (!parts.isEmpty() && !parts.contains(part)) {
            return c;
        }
        double radius = v.get(p.value("spread", 0.0), leg);
        if (radius <= 0) {
            return c;
        }
        // Generous by a texel, because the sheet is not exactly two texels to
        // the unit on a face seen at an angle.
        int r = (int) Math.ceil(radius * HorseSkinGeometry.TEXELS_PER_UNIT) + 1;
        double rr = radius * radius;
        double best = c;
        for (int dy = -r; dy <= r; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                int qx = px + dx;
                int qy = py + dy;
                if (qx < 0 || qy < 0
                        || qx >= HorseSkinGeometry.SHEET_SIZE
                        || qy >= HorseSkinGeometry.SHEET_SIZE) {
                    continue;
                }
                java.util.Optional<HorseSkinGeometry.Sample> at =
                        HorseSkinGeometry.sample(skin, qx, qy);
                if (at.isEmpty()) {
                    continue;
                }
                BodyPoint q = at.get().point();
                double bx = q.x() - point.x();
                double by = q.y() - point.y();
                double bz = q.z() - point.z();
                if (bx * bx + by * by + bz * bz > rr) {
                    continue;
                }
                double n = pigmentCoverage(mask, v, coat, qx, qy, leg);
                if (mask.invert()) {
                    n = 1.0 - n;
                }
                if (n > best) {
                    best = n;
                    if (best >= 1) {
                        return 1;
                    }
                }
            }
        }
        return best;
    }

    /** A {@code PIGMENT} mask at one texel, before {@code invert} and before {@code spread}. */
    private static double pigmentCoverage(Mask mask, SpecValues v, PigmentView coat,
                                          int px, int py, int leg) {
        Params p = mask.params();
        return BodyStripes.smoothstep(v.get(p.value("from", 0.5), leg),
                v.get(p.value("to", 1.0), leg),
                pigmentReading(coat, p.text("channel", "darkness"), px, py));
    }

    /** One channel of the coat, as a {@code PIGMENT} mask reads it. */
    private static double pigmentReading(PigmentView coat, String channel, int px, int py) {
        float red = coat.red(px, py);
        float black = coat.black(px, py);
        return switch (channel) {
            case "red" -> red;
            case "black" -> black;
            case "total" -> (red + black) / 2.0;
            default -> clamp01(0.55 * red + 0.95 * black);
        };
    }

    /**
     * The radius of a <b>heart</b> at angle {@code theta}, as a multiple of the
     * radius the same element would have round - the whole of what
     * {@code "shape": "heart"} changes.
     *
     * <p>The classic polar heart, {@code r = 2 - 2 sin t + sin t sqrt|cos t| /
     * (sin t + 1.4)}, divided through by 2.4 so a heart and a disc of the same
     * {@code radius} come out about the same size. It is measured in the
     * {@code (x, y)} plane - nose-to-tail across, hoof-to-withers up - so the
     * heart stands upright with its point down on the flank, which is the face
     * a spot is read on. Over the topline it is seen edge-on and reads as a
     * lozenge, which is the same compromise {@code stretch} already makes.
     */
    private static double heartRadius(double theta) {
        double sin = Math.sin(theta);
        double cos = Math.cos(theta);
        return (2 - 2 * sin + sin * Math.sqrt(Math.abs(cos)) / (sin + 1.4)) / 2.4;
    }

    /**
     * How far this sample is from the surface where the noise field crosses its
     * midpoint, in lattice units - the shape a {@code STROKES} mask draws.
     *
     * <p>The naive version of this is {@code |n - 0.5|}, and it does not work.
     * Value noise is flat near its extrema and steep between them, so the band
     * where {@code |n - 0.5|} is small is <b>thin where the field is steep and
     * enormous where it is flat</b>: the same parameters give a hairline over
     * half the horse and a blot over the other half. Dividing by the gradient
     * turns the reading into an approximate distance, and the stroke keeps its
     * width all the way along. It costs three extra noise samples, which is the
     * whole reason to write down why they are there.
     */
    private static double levelSetDistance(long seed, double x, double y, double z) {
        double n = BodyNoise.value(seed, x, y, z);
        double e = 0.25;
        double gx = (BodyNoise.value(seed, x + e, y, z) - n) / e;
        double gy = (BodyNoise.value(seed, x, y + e, z) - n) / e;
        double gz = (BodyNoise.value(seed, x, y, z + e) - n) / e;
        double grad = Math.sqrt(gx * gx + gy * gy + gz * gz);
        return Math.abs(n - 0.5) / Math.max(grad, 1e-4);
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
