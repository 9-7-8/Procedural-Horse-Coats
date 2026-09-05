package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Axis;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.BodyPoint;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Bounds;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;

/**
 * The shared painter for the <b>hair</b> genes - the magical mane and tail
 * colours, and the healer's stripe. Body-space, pure, and reusable the way
 * {@link WhitePattern} is for the white-marking loci: one shape per family,
 * with the difference between two genes being its parameters rather than a
 * second copy of the code.
 *
 * <h2>Painting toward a colour, not with one</h2>
 * {@link #paint} walks each texel a fraction of the way from what it already
 * looks like to the target colour, exactly the way pink hair does, and for the
 * same reason: flat paint throws away the strand shading the natural phase and
 * the template gave the mane, and leaves a dead rectangle. Short of the whole
 * way, the mane is unmistakably the new colour and still reads as hair.
 *
 * <h2>Where the bands run</h2>
 * A mane is a long thin box and a tail is a short fat one, and both are rotated
 * out of the axes by their rest pose, so neither "stripes along X" nor "stripes
 * along Y" is right for both. Instead {@link #bands} and {@link #centreStripe}
 * find the part's own axes - longest span first - and work in those. Bands run
 * <i>across</i> the long axis (so they cross the hair the way a barcode does);
 * the centre stripe runs <i>along</i> it, centred in the second axis, which on
 * the big side faces of the mane reads as a line straight down the middle.
 */
public final class HairPattern {

    private HairPattern() {}

    /** A colour to walk a texel toward, and how far of the way to go. */
    public record Tone(int rgb, double strength) {}

    /** What colour, if any, one texel should end up. {@code null} leaves it alone. */
    @FunctionalInterface
    public interface Target {
        Tone at(int px, int py, BodyPoint point);
    }

    // ------------------------------------------------------------------
    // Colour
    // ------------------------------------------------------------------

    /**
     * A bright, saturated colour drawn from an epigenetic seed - three
     * {@code nextFloat()}s, hue then saturation then value.
     *
     * <p>Hue is uniform over the whole circle (so any mane colour is possible),
     * but saturation and value are held well up: a magical mane that rolled a
     * muddy olive would read as a bug, and the whole point of the gene is that
     * you can see what a horse is carrying.
     */
    public static int randomBrightColour(Rng rng) {
        float hue = rng.nextFloat();
        float saturation = 0.60f + rng.nextFloat() * 0.40f;
        float value = 0.62f + rng.nextFloat() * 0.38f;
        return hsvToRgb(hue, saturation, value);
    }

    /** {@code h}, {@code s}, {@code v} in {@code [0,1]} to {@code 0xRRGGBB}. */
    public static int hsvToRgb(double h, double s, double v) {
        double hh = (h - Math.floor(h)) * 6.0;
        int sector = (int) hh;
        double f = hh - sector;
        double p = v * (1 - s);
        double q = v * (1 - s * f);
        double t = v * (1 - s * (1 - f));
        double r;
        double g;
        double b;
        switch (sector % 6) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return (channel(r) << 16) | (channel(g) << 8) | channel(b);
    }

    private static int channel(double v) {
        int i = (int) Math.round(v * 255.0);
        return i < 0 ? 0 : (i > 255 ? 255 : i);
    }

    // ------------------------------------------------------------------
    // Shapes
    // ------------------------------------------------------------------

    /**
     * Bands running across {@code part}, wobbled off straight by
     * {@link BodyNoise} so they read as dyed hair rather than a printed
     * barcode. Returns coverage in {@code [0,1]} for one texel.
     *
     * @param spacing band period, in body units (1 = 1/16 block)
     * @param duty    fraction of each period the band actually covers
     */
    public static double bands(Skin skin, Part part, BodyPoint point, long seed,
                               double spacing, double duty) {
        Axis along = axesBySpan(skin, part)[0];
        double t = point.along(along);
        double wobble = (BodyNoise.value(seed, point.x() * 0.30, point.y() * 0.30, point.z() * 0.30) - 0.5)
                * spacing * 0.55;
        double phase = (t + wobble) / spacing;
        double f = phase - Math.floor(phase);
        double half = duty * 0.5;
        // Distance from the centre of the nearest band, as a fraction of a period.
        double d = Math.abs(f - 0.5);
        double edge = Math.max(0.02, half * 0.35);
        return 1.0 - BodyStripes.smoothstep(half - edge, half + edge, d);
    }

    /**
     * A single stripe running the length of {@code part} and centred across it -
     * the healer's red line down the middle of the mane. Coverage in
     * {@code [0,1]}.
     *
     * @param halfWidth half the stripe's width, as a fraction of the part's
     *                  second-longest span
     */
    public static double centreStripe(Skin skin, Part part, BodyPoint point, double halfWidth) {
        Axis[] axes = axesBySpan(skin, part);
        Axis across = axes[1];
        Bounds b = HorseSkinGeometry.bounds(skin, part);
        double span = b.span(across);
        if (span <= 0) {
            return 1.0;
        }
        double centre = (b.min(across) + b.max(across)) * 0.5;
        double d = Math.abs(point.along(across) - centre) / span;
        double edge = Math.max(0.03, halfWidth * 0.5);
        return 1.0 - BodyStripes.smoothstep(halfWidth - edge, halfWidth + edge, d);
    }

    /** {@code part}'s three axes, longest span first - its own frame rather than the world's. */
    public static Axis[] axesBySpan(Skin skin, Part part) {
        Bounds b = HorseSkinGeometry.bounds(skin, part);
        Axis[] axes = {Axis.X, Axis.Y, Axis.Z};
        // Three elements: an insertion sort is clearer than anything cleverer.
        for (int i = 1; i < axes.length; i++) {
            Axis key = axes[i];
            int j = i - 1;
            while (j >= 0 && b.span(axes[j]) < b.span(key)) {
                axes[j + 1] = axes[j];
                j--;
            }
            axes[j + 1] = key;
        }
        return axes;
    }

    // ------------------------------------------------------------------
    // The paint pass
    // ------------------------------------------------------------------

    /**
     * Walk every texel of {@code part} toward whatever colour {@code target}
     * names for it, and return the phase-3 delta that does it.
     *
     * <p>Opacity is raised to full on any texel it touches, so a coloured mane
     * still shows on a dominant-white horse - the same choice pink hair and
     * magic zebra make.
     *
     * @param delta the field to add into, so a gene painting several parts (or
     *              a solid colour and stripes over it) produces one contribution
     */
    public static void paint(CoatBuildContext ctx, ColorView accumulated, ColorField delta,
                             Part part, Target target) {
        Skin skin = ctx.skin();
        if (!HorseSkinGeometry.hasPart(skin, part)) {
            return; // a foal has no mane
        }
        HorseSkinGeometry.forEachTexel(skin, part, (px, py, p, face, point) -> {
            Tone tone = target.at(px, py, point);
            if (tone == null || tone.strength() <= 0) {
                return;
            }
            delta.add(px, py,
                    toward(accumulated, px, py, 0, (tone.rgb() >> 16) & 0xFF, tone.strength()),
                    toward(accumulated, px, py, 1, (tone.rgb() >> 8) & 0xFF, tone.strength()),
                    toward(accumulated, px, py, 2, tone.rgb() & 0xFF, tone.strength()));
            int wanted = (int) Math.round(255.0 * Math.min(1.0, tone.strength() * 2.0));
            int gap = wanted - accumulated.opacity(px, py);
            if (gap > 0) {
                delta.addOpacity(px, py, gap);
            }
        });
    }

    /**
     * The signed step from this texel's accumulated channel to {@code strength}
     * of the way to {@code target} - the same manoeuvre pink hair makes, and the
     * reason phase 3 hands a gene the colour so far at all.
     */
    private static int toward(ColorView colour, int px, int py, int channel, int target, double strength) {
        int seen = colour.visible(px, py, channel);
        int wanted = (int) Math.round(seen + (target - seen) * strength);
        return wanted - switch (channel) {
            case 0 -> colour.red(px, py);
            case 1 -> colour.green(px, py);
            default -> colour.blue(px, py);
        };
    }
}
