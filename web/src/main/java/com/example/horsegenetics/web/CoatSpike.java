package com.example.horsegenetics.web;

import com.example.horsegenetics.common.coat.pattern.CoatTextureComposer;
import com.example.horsegenetics.common.coat.pattern.GradientLut;
import com.example.horsegenetics.common.coat.pattern.LutSet;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;

/**
 * SPIKE: does TeaVM swallow {@code common/}?
 *
 * <p>The horse designer is served from GitHub Pages, which is static - there is
 * no server to run the real pipeline on, and hand-porting genes to JavaScript is
 * the practice we are trying to end. TeaVM compiles JVM <i>bytecode</i>, so this
 * is not a rewrite; it is {@code common/} itself, running in a browser.
 *
 * <p>This entry point deliberately exercises the parts most likely to break
 * rather than the parts most likely to be wanted:
 * <ul>
 *   <li>the {@link Genes} registry's static initialiser, which builds all 48
 *       genes and holds a {@code java.lang.System.Logger} - the single most
 *       suspect API in the reachable graph;</li>
 *   <li>{@link Genotype#parse} and {@link Epigenome#fromSeed}, so the code
 *       strings really are the interface;</li>
 *   <li>the whole three-phase {@link CoatTextureComposer}, adult and foal,
 *       across genotypes that reach every kind of painter - absolute points,
 *       dilution, dapple noise, stripes and white patterning.</li>
 * </ul>
 *
 * <p><b>No resources are read.</b> The gradient and the template are synthesised
 * here, because resource loading is TeaVM's known weak spot <i>and</i> because
 * it is the wrong design anyway: the browser already has the real PNGs inlined
 * and can decode them, so the boundary should be {@code int[]} in, {@code int[]}
 * out. Nothing crosses it but numbers.
 */
public final class CoatSpike {

    private CoatSpike() {
    }

    public static void main(String[] args) {
        int n = HorseSkinGeometry.SHEET_SIZE;
        say("sheet " + n + "x" + n);

        // 1. The registry. If the static initialiser cannot run, nothing else can.
        int genes = 0;
        int painting = 0;
        for (Gene g : Genes.codeOrder()) {
            genes++;
            if (g.affectsCoat()) {
                painting++;
            }
        }
        say("registry: " + genes + " genes, " + painting + " that paint");

        // 2. The two colour inputs, synthesised - see the class comment.
        LutSet lut = new LutSet(syntheticGradient(), java.util.Map.of());
        int[] template = syntheticTemplate(n);

        // 3. The pipeline, over genotypes that between them reach every painter.
        String[][] cases = {
                {"black", ""},
                {"chestnut", "horsegenetics.extension=e/e"},
                {"bay", "horsegenetics.agouti=A/a"},
                {"buckskin", "horsegenetics.agouti=A/a-horsegenetics.matp=Cr/N"},
                {"dapple grey", "horsegenetics.grey=G/g"},
                {"sabino", "horsegenetics.kit=SB1/N"},
                {"splash x2", "horsegenetics.mitf=SW1/N-horsegenetics.pax3=SW2/N"},
                {"tobiano", "horsegenetics.tobiano=To/to"},
                {"leopard", "horsegenetics.leopard=LP/lp-horsegenetics.patn1=PATN1/n"},
                {"magic zebra", "horsegenetics.magic_zebra=Mzeb/n"},
        };

        for (String[] c : cases) {
            Genotype gt = Genotype.parse(c[1]);
            Epigenome epi = Epigenome.fromSeed(0x5EEDL);
            int[] adult = CoatTextureComposer.compose(gt, epi, Skin.ADULT, true, template, lut);
            int[] foal = CoatTextureComposer.compose(gt, epi, Skin.BABY, false, template, lut);
            say(pad(c[0]) + " adult " + digest(adult) + "   foal " + digest(foal));
        }

        // 4. Is it fast enough to rebake on a slider drag? Two genotypes,
        //    because they cost wildly different amounts: a plain bay is a few
        //    arithmetic ops per texel, while dapple grey samples BodyNoise
        //    three times per texel and that hash is 64-bit integer maths -
        //    which a JavaScript number cannot do natively.
        bench("bay", "horsegenetics.agouti=A/a", template, lut);
        bench("dapple grey", "horsegenetics.grey=G/g", template, lut);
        bench("grey bay + sabino",
                "horsegenetics.agouti=A/a-horsegenetics.grey=G/g-horsegenetics.kit=SB1/N", template, lut);

        say("SPIKE OK - common/ ran in JavaScript");
    }

    private static void bench(String label, String code, int[] template, LutSet lut) {
        Genotype gt = Genotype.parse(code);
        Epigenome epi = Epigenome.fromSeed(0x5EEDL);
        for (int i = 0; i < 3; i++) {
            CoatTextureComposer.compose(gt, epi, Skin.ADULT, true, template, lut);
        }
        int runs = 10;
        long start = System.currentTimeMillis();
        for (int i = 0; i < runs; i++) {
            CoatTextureComposer.compose(gt, epi, Skin.ADULT, true, template, lut);
        }
        say("  " + pad(label) + (System.currentTimeMillis() - start) / runs + " ms per adult compose");
    }

    /**
     * A stand-in for redblackgradient.png: red left to right, black top to
     * bottom. Not the real chart - the point is that the arithmetic runs, and
     * the real one arrives as an int[] from the page.
     */
    private static GradientLut syntheticGradient() {
        int w = 64;
        int h = 64;
        int[] argb = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int r = 255 - (x * 255 / (w - 1));
                int k = 255 - (y * 255 / (h - 1));
                argb[y * w + x] = 0xFF000000 | (r << 16) | ((k / 2) << 8) | (k / 3);
            }
        }
        return new GradientLut(argb, w, h);
    }

    /** An opaque white sheet, so every mapped texel survives the composite. */
    private static int[] syntheticTemplate(int n) {
        int[] t = new int[n * n];
        java.util.Arrays.fill(t, 0xFFFFFFFF);
        return t;
    }

    /** Cheap order-sensitive checksum - enough to tell two coats apart. */
    private static String digest(int[] argb) {
        long h = 1469598103934665603L;
        for (int p : argb) {
            h = (h ^ p) * 1099511628211L;
        }
        String hex = Long.toHexString(h);
        return hex.length() > 8 ? hex.substring(0, 8) : hex;
    }

    private static String pad(String s) {
        StringBuilder b = new StringBuilder(s);
        while (b.length() < 14) {
            b.append(' ');
        }
        return b.toString();
    }

    private static void say(String s) {
        System.out.println(s);
    }
}
