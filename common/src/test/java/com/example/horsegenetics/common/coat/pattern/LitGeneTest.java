package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.spec.GeneSpec;
import com.example.horsegenetics.common.genetics.spec.GeneSpecLoader;
import com.example.horsegenetics.common.genetics.spec.SpecValues;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>The two genes whose light is a level</b> - Lantern and Tron, the first
 * users of <a href="../../../../../../../../wiki/making-a-gene.html#emissive">glow
 * intensity</a>.
 *
 * <p>Both are built the same way and always were: a lit core with a halo round
 * it. Until a glow could be a fraction the halo could only be <i>painted</i>,
 * so the light stopped dead at the core's edge and the wash beside it was a
 * colour pretending to be a glow. Now the halo is lit too, dimmer, and its own
 * mask's falloff is the light's falloff.
 *
 * <p><b>None of that is visible to any other check.</b> A glow lives in a
 * second texture that only the renderer reads: the coat goldens do not hash it,
 * the creator cannot draw it, and <code>DeadLayerTest</code> measures paint
 * rather than light. So the three things these genes are actually claiming get
 * asserted here - that they light something at all, that the core stays the
 * brighter of the two on every horse, and that the dial moves the light.
 */
class LitGeneTest {

    private static final int N = HorseSkinGeometry.SHEET_SIZE;

    private static GeneSpec spec(String slug) {
        for (GeneSpec s : GeneSpecLoader.fromClasspath().specs()) {
            if (s.key().endsWith("." + slug)) {
                return s;
            }
        }
        throw new AssertionError("no gene " + slug);
    }

    /** Every texel's glow level for one horse, with its dial pinned at {@code dial}. */
    private static float[] glow(GeneSpec spec, long seed, double dial) {
        CoatBuildContext ctx = new CoatBuildContext(
                Genotype.wildType(), Epigenome.fromSeed(seed), Skin.ADULT, true);
        int i = spec.dialKnob();
        GeneSpec.Knob knob = spec.knobs().get(i);
        SpecValues values = SpecValues.read(spec,
                ctx.epigeneticsFor(spec.key())
                        .with(knob.name(), knob.min() + (knob.max() - knob.min()) * dial),
                2);
        CoatOverlay overlay = new CoatOverlay(Skin.ADULT, new int[N * N]);
        SpecPainter.emissive(spec, spec.expressions().get(0).layers(), values, Skin.ADULT, overlay);
        float[] mask = overlay.emissiveMask();
        return mask == null ? new float[N * N] : mask;
    }

    private static double brightest(float[] mask) {
        double best = 0;
        for (float v : mask) {
            best = Math.max(best, v);
        }
        return best;
    }

    private static int litTexels(float[] mask) {
        int n = 0;
        for (float v : mask) {
            if (v > 0) {
                n++;
            }
        }
        return n;
    }

    /**
     * Both genes light a real part of the horse at <b>both</b> ends of their
     * dial. The dim end matters as much as the bright one: a gene whose floor
     * is dark is a gene that sometimes forgets to be itself, which is why
     * neither dial runs to zero.
     */
    @Test
    void bothGenesLightSomethingAtEitherEndOfTheirDial() {
        for (String slug : new String[]{"lantern", "tron"}) {
            GeneSpec s = spec(slug);
            assertTrue(s.dialKnob() >= 0, slug + " has no dial knob");
            for (double dial : new double[]{0.0, 1.0}) {
                for (long seed : new long[]{3, 11, 29}) {
                    float[] mask = glow(s, seed, dial);
                    assertTrue(litTexels(mask) > 20,
                            slug + " at dial " + dial + " on horse " + seed + " lights only "
                                    + litTexels(mask) + " texels");
                    assertTrue(brightest(mask) > 0.3,
                            slug + " at dial " + dial + " peaks at " + brightest(mask)
                                    + " - too dim to read as lit");
                }
            }
        }
    }

    /**
     * <b>The dial moves the light.</b> Turning it up has to make the brightest
     * part of the horse brighter, or the knob is decorative - which is exactly
     * what a glow pointed at the wrong thing looks like, since nothing else
     * would fail.
     */
    @Test
    void theDialMovesTheLight() {
        for (String slug : new String[]{"lantern", "tron"}) {
            GeneSpec s = spec(slug);
            double dim = brightest(glow(s, 11, 0.0));
            double bright = brightest(glow(s, 11, 1.0));
            assertTrue(bright > dim + 0.2,
                    slug + " barely changes with its dial: " + dim + " -> " + bright);
        }
    }

    /**
     * <b>The core stays brighter than its halo on every horse.</b> That is the
     * whole reason the halo's level is a flat constant under the dial's floor
     * rather than a share of it: the format has no arithmetic, so "a fifth of
     * the core" cannot be written, and a halo that could out-burn a dim core
     * would read as a smear with a hole in it.
     */
    @Test
    void theHaloNeverOutburnsTheCore() {
        for (String slug : new String[]{"lantern", "tron"}) {
            GeneSpec s = spec(slug);
            double haloCeiling = 0;
            for (GeneSpec.Layer layer : s.expressions().get(0).layers()) {
                if (layer.glows() && layer.emissive() instanceof GeneSpec.Value.Const c) {
                    haloCeiling = Math.max(haloCeiling, c.v());
                }
            }
            assertTrue(haloCeiling > 0, slug + " has no flat-level layer - has the halo stopped glowing?");
            double dimmestCore = brightest(glow(s, 11, 0.0));
            assertTrue(dimmestCore > haloCeiling,
                    slug + "'s dimmest core (" + dimmestCore + ") does not out-burn its halo ("
                            + haloCeiling + ")");
        }
    }
}
