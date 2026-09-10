package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.spec.GeneSpec;
import com.example.horsegenetics.common.genetics.spec.GeneSpecLoader;
import com.example.horsegenetics.common.genetics.spec.SpecValues;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * <b>Take a layer out. If the horse does not change, the layer was not doing
 * anything.</b>
 *
 * <p>That one measurement catches three unrelated mistakes, and the reason it
 * is worth a test of its own is that <b>none of them fails anything else</b>.
 * The file is valid, the layers are valid, the gene paints plenty of texels,
 * and one of the things its author wrote is simply not on the horse:
 *
 * <ul>
 *   <li><b>A mask fold that comes out empty.</b> An {@code AXIS} inverted the
 *       wrong way selects the part of the horse the layer's own
 *       {@code parts} list excludes, so the fold is zero everywhere.</li>
 *   <li><b>A layer overruled because a gene's layers SUM rather than stack</b>
 *       - see <a href="../../../../../../../../wiki/known-gaps.html">gap 169</a>.
 *       Every layer measures its delta against the colour the <i>gene</i>
 *       started from and the deltas are added, so an earlier {@code TINT} of
 *       -200 drives the texel to the clamp and everything painted inside it is
 *       thrown away by the byte. Dorsal wing shipped from intake that way: a
 *       teal band, rust squiggles and pink cells, and a solid black horse.</li>
 *   <li><b>A feature smaller than a texel.</b> Ringwork's two contour rings are
 *       annuli 0.1 body units wide, and a texel is 0.5.</li>
 * </ul>
 *
 * <p><b>Why removal rather than inspection.</b> Whether two masks overlap is
 * not decidable by reading them - they are arbitrary functions of position,
 * folded in an order that includes inversion and subtraction. It <i>is</i>
 * decidable by evaluating them, which is what the painter already does, so this
 * evaluates them and diffs the result. A layer that changes nothing is dead
 * whatever the reason, and the reason is the author's to find.
 *
 * <p><b>Several base coats, because a layer may have nothing to do on one.</b>
 * A layer is only called dead if it is dead on <i>all</i> of them.
 *
 * <p><b>What it deliberately does not check.</b> Layers carrying a
 * {@code PIGMENT} or {@code LUMA} mask read the coat underneath, and the bases
 * here are synthetic - so a layer that wants a leopard's varnish, or somebody
 * else's white, would read as dead when it is only unhoused. Those are counted
 * and skipped rather than guessed at; {@link #SKIPPED_ARE_REPORTED} keeps the
 * number honest. Extending the check to them means composing real horses
 * through phase 1 and 2 first, which the composer does not currently expose.
 *
 * <p><b>The baseline is a ratchet.</b> {@code dead-layers.txt} is what was dead
 * when the check was written. The test fails if anything new appears <i>and</i>
 * if anything in the file has quietly come alive - the second half is what
 * stops the file rotting, and it means fixing a gene is not done until its line
 * is deleted. The file is meant to reach zero.
 */
class DeadLayerTest {

    /** Channel difference that counts as a visible change. */
    private static final int VISIBLE = 20;

    /** Texels a layer has to move on some base before it counts as alive. */
    private static final int FLOOR = 12;

    private static final String BASELINE = "/dead-layers.txt";

    /**
     * The coats a layer gets a chance to show on. Three rather than a dozen
     * because the cost is one render per layer per base and this has to stay
     * inside the seconds-not-minutes working loop; light, dark and a broken-up
     * one is what separates "nothing to do here" from "nothing to do at all".
     */
    private record Base(String name, int argb, float red, float black, boolean broken) {}

    private static final Base[] BASES = {
            new Base("bay", 0xFF6B4A2F, 1f, 0.55f, false),
            new Base("black", 0xFF2A2420, 0.15f, 1f, false),
            new Base("part-white", 0xFF6B4A2F, 1f, 0.55f, true),
    };

    @Test
    void everyLayerPutsSomethingOnTheHorse() throws IOException {
        Set<String> dead = new TreeSet<>(findDead());
        Set<String> baseline = new TreeSet<>(readBaseline());

        Set<String> appeared = new LinkedHashSet<>(dead);
        appeared.removeAll(baseline);
        Set<String> fixed = new LinkedHashSet<>(baseline);
        fixed.removeAll(dead);

        if (!appeared.isEmpty() || !fixed.isEmpty()) {
            Path out = Path.of("build", "dead-layers.txt");
            Files.createDirectories(out.toAbsolutePath().getParent());
            Files.writeString(out, String.join("\n", dead) + "\n", StandardCharsets.UTF_8);
            StringBuilder say = new StringBuilder();
            if (!appeared.isEmpty()) {
                say.append("\nThese layers put NOTHING on the horse on any base coat.\n")
                        .append("Either the mask fold is empty, or an overlapping layer's delta\n")
                        .append("clamped theirs away (gap 169), or the feature is under a texel:\n");
                appeared.forEach(s -> say.append("    ").append(s).append('\n'));
            }
            if (!fixed.isEmpty()) {
                say.append("\nThese are in the baseline and are alive again - delete their lines\n")
                        .append("from common/src/test/resources/dead-layers.txt:\n");
                fixed.forEach(s -> say.append("    ").append(s).append('\n'));
            }
            say.append("\nThe actual list is at ").append(out.toAbsolutePath()).append('\n');
            fail(say.toString());
        }
    }

    /** Being explicit about the blind spot, so its size cannot drift unnoticed. */
    @Test
    void SKIPPED_ARE_REPORTED() {
        int skipped = 0;
        for (GeneSpec spec : GeneSpecLoader.fromClasspath().specs()) {
            for (GeneSpec.ExpressionSpec e : spec.expressions()) {
                for (GeneSpec.Layer layer : e.layers()) {
                    if (readsCoat(layer)) {
                        skipped++;
                    }
                }
            }
        }
        assertTrue(skipped < 120,
                "layers reading PIGMENT or LUMA, which this check cannot see: " + skipped
                        + ". If that has grown a lot, the blind spot is worth closing - it means"
                        + " composing real horses through phase 1 and 2 rather than synthesising"
                        + " a base.");
    }

    // ------------------------------------------------------------------

    private static List<String> findDead() {
        int n = HorseSkinGeometry.SHEET_SIZE;
        List<int[]> mapped = new ArrayList<>();
        HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, part, face, point) ->
                mapped.add(new int[] {px, py}));

        List<String> dead = new ArrayList<>();
        for (GeneSpec spec : GeneSpecLoader.fromClasspath().specs()) {
            String slug = spec.key().substring(spec.key().indexOf('.') + 1);
            for (GeneSpec.ExpressionSpec e : spec.expressions()) {
                if (e.wildType() || e.layers().isEmpty()) {
                    continue;
                }
                CoatBuildContext ctx = new CoatBuildContext(
                        Genotype.wildType(), Epigenome.fromSeed(11), Skin.ADULT, true);
                SpecValues values = SpecValues.read(spec, ctx.epigeneticsFor(spec.key()), 2);

                // Alive on the first base it shows on, and then left alone: the
                // extra coats exist for the layers that looked dead on the bay,
                // and re-proving the other nine hundred costs the whole budget.
                int[] best = new int[e.layers().size()];
                for (int i = 0; i < best.length; i++) {
                    best[i] = readsCoat(e.layers().get(i)) ? Integer.MAX_VALUE : 0;
                }
                for (Base b : BASES) {
                    boolean anyLeft = false;
                    for (int v : best) {
                        anyLeft |= v < FLOOR;
                    }
                    if (!anyLeft) {
                        break;
                    }
                    PigmentField coat = new PigmentField(n);
                    ColorField seed = new ColorField(n);
                    paint(mapped, coat, seed, b);

                    ColorField all = new ColorField(n);
                    paint(mapped, new PigmentField(n), all, b);
                    all.apply(SpecPainter.tint(spec, e.layers(), values, ctx, coat, seed));

                    for (int i = 0; i < e.layers().size(); i++) {
                        if (best[i] >= FLOOR) {
                            continue;
                        }
                        List<GeneSpec.Layer> without = new ArrayList<>(e.layers());
                        without.remove(i);
                        ColorField cut = new ColorField(n);
                        paint(mapped, new PigmentField(n), cut, b);
                        if (!without.isEmpty()) {
                            cut.apply(SpecPainter.tint(spec, without, values, ctx, coat, seed));
                        }
                        best[i] = Math.max(best[i], moved(mapped, all, cut));
                    }
                }
                for (int i = 0; i < best.length; i++) {
                    if (best[i] < FLOOR) {
                        dead.add(slug + " / " + e.id() + " / " + i + " / " + e.layers().get(i).name());
                    }
                }
            }
        }
        return dead;
    }

    private static void paint(List<int[]> mapped, PigmentField coat, ColorField colour, Base b) {
        for (int[] t : mapped) {
            boolean white = b.broken() && ((t[0] / 16 + t[1] / 16) % 2 == 0);
            coat.setRed(t[0], t[1], white ? 0f : b.red());
            coat.setBlack(t[0], t[1], white ? 0f : b.black());
            colour.setArgb(t[0], t[1], white ? 0xFFF7F4EC : b.argb());
        }
    }

    private static int moved(List<int[]> mapped, ColorView a, ColorView b) {
        int moved = 0;
        for (int[] t : mapped) {
            int x = a.argb(t[0], t[1]), y = b.argb(t[0], t[1]);
            int d = Math.max(Math.abs(((x >> 16) & 0xFF) - ((y >> 16) & 0xFF)),
                    Math.max(Math.abs(((x >> 8) & 0xFF) - ((y >> 8) & 0xFF)),
                            Math.abs((x & 0xFF) - (y & 0xFF))));
            if (d > VISIBLE) {
                moved++;
            }
        }
        return moved;
    }

    /** These read the coat underneath, and a synthetic base cannot stand in for one. */
    private static boolean readsCoat(GeneSpec.Layer layer) {
        for (GeneSpec.Mask m : layer.masks()) {
            if (m.type() == GeneSpec.MaskType.PIGMENT || m.type() == GeneSpec.MaskType.LUMA) {
                return true;
            }
        }
        return false;
    }

    private static List<String> readBaseline() throws IOException {
        try (InputStream in = DeadLayerTest.class.getResourceAsStream(BASELINE)) {
            if (in == null) {
                return List.of();
            }
            List<String> out = new ArrayList<>();
            for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                String s = line.strip();
                if (!s.isEmpty() && !s.startsWith("#")) {
                    out.add(s);
                }
            }
            return out;
        }
    }
}
