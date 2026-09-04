package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The refactor safety net: a fixed set of genotype / epigenome / age
 * combinations, composed through the real pipeline against a synthetic gradient
 * and template, hashed per case.
 *
 * <p>Its job is to prove that a change to the <b>machinery</b> - the three-phase
 * pipeline, the field types, the gene hooks - leaves every horse rendering
 * <b>byte-identically</b>. It is not a description of what a coat should look
 * like (the other tests in this package do that), so when a <i>gene</i>
 * deliberately changes, regenerate the golden file: delete
 * {@code common/src/test/resources/coat-golden.txt}, run the test, copy the file
 * it writes to {@code common/build/coat-golden.txt} back into place, and say so
 * in the commit.
 */
class CoatPipelineGoldenTest {

    private static final int N = HorseSkinGeometry.SHEET_SIZE;
    private static final String RESOURCE = "/coat-golden.txt";

    /**
     * Codes chosen to hit every natural gene, both dilutions stacked, both
     * magical genes alone and together, and Test's flat paint over both.
     */
    private static final List<String> CODES = List.of(
            Genotype.wildType().toCode(),
            override("extension=e/e"),
            override("agouti=A/a"),
            override("agouti=A/A"),
            override("white=W/w"),
            override("test=T/t"),
            override("extension=e/e", "test=T/t"),
            override("white=W/w", "test=T/t"),
            override("champagne=Ch/c"),
            override("agouti=A/a", "champagne=Ch/c"),
            override("splash=Spl/spl"),
            override("agouti=A/a", "splash=Spl/spl"),
            override("grey=G/g"),
            override("extension=e/e", "grey=G/g"),
            override("agouti=A/a", "cream=Cr/N"),
            override("agouti=A/a", "cream=Cr/Cr"),
            override("agouti=A/a", "pearl=prl/prl"),
            override("agouti=A/a", "cream=Cr/N", "pearl=prl/N"),
            override("extension=e/e", "grey=G/g", "cream=Cr/Cr"),
            override("agouti=A/a", "champagne=Ch/c", "splash=Spl/spl", "grey=G/g", "cream=Cr/N"),
            override("magic_zebra=Mzeb/n"),
            override("magic_zebra=Mzeb/Mzeb"),
            override("agouti=A/a", "magic_zebra=Mzeb/n"),
            override("white=W/w", "magic_zebra=Mzeb/n"),
            override("pink_hair=Pihr/Pihr"),
            override("pink_hair=n/Pihr"),
            override("extension=e/e", "pink_hair=Pihr/Pihr"),
            override("white=W/w", "pink_hair=Pihr/Pihr"),
            override("agouti=A/a", "magic_zebra=Mzeb/n", "pink_hair=Pihr/Pihr"),
            override("test=T/t", "magic_zebra=Mzeb/n", "pink_hair=Pihr/Pihr"),
            override("dun=D/d2"),
            override("agouti=A/a", "dun=D/d2"),
            override("dun=d1/d2"),
            override("agouti=A/a", "dun=d1/d1"),
            override("silver=Z/z"),
            override("agouti=A/a", "silver=Z/z"),
            override("extension=e/e", "mushroom=Mu/Mu"),
            override("roan=Rn/rn"),
            override("tobiano=To/to"),
            override("frame=Ov/ov"),
            override("sabino=SB1/sb1"),
            override("sabino=SB1/SB1"));

    private static final long[] SEEDS = {0L, 3L, 4242L};

    @Test
    void thePipelineComposesTheSameBytesItAlwaysHas() throws IOException {
        String actual = render();
        String expected = readGolden();
        if (expected == null) {
            Path out = Path.of("build", "coat-golden.txt");
            Files.createDirectories(out.toAbsolutePath().getParent());
            Files.writeString(out, actual, StandardCharsets.UTF_8);
            fail("no golden file on the test classpath - wrote a fresh one to " + out.toAbsolutePath()
                    + "; copy it to common/src/test/resources/coat-golden.txt");
        }
        assertEquals(expected, actual, "the composed coat bytes moved - see this test's javadoc");
    }

    private static String render() {
        GradientLut lut = lut();
        int[] adultTemplate = template(Skin.ADULT);
        int[] foalTemplate = template(Skin.BABY);
        StringBuilder sb = new StringBuilder();
        for (String code : CODES) {
            for (long seed : SEEDS) {
                Genotype gt = Genotype.parse(code);
                Epigenome epi = Epigenome.fromSeed(seed);
                sb.append(code).append(' ').append(seed).append(" adult ")
                        .append(sha256(CoatTextureComposer.compose(gt, epi, Skin.ADULT, true, adultTemplate, lut)))
                        .append('\n');
                sb.append(code).append(' ').append(seed).append(" foal  ")
                        .append(sha256(CoatTextureComposer.compose(gt, epi, Skin.BABY, false, foalTemplate, lut)))
                        .append('\n');
            }
        }
        return sb.toString();
    }

    private static String readGolden() throws IOException {
        try (InputStream in = CoatPipelineGoldenTest.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                return null;
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        }
    }

    private static String sha256(int[] argb) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        byte[] bytes = new byte[argb.length * 4];
        for (int i = 0; i < argb.length; i++) {
            bytes[i * 4] = (byte) (argb[i] >>> 24);
            bytes[i * 4 + 1] = (byte) (argb[i] >>> 16);
            bytes[i * 4 + 2] = (byte) (argb[i] >>> 8);
            bytes[i * 4 + 3] = (byte) argb[i];
        }
        StringBuilder hex = new StringBuilder();
        for (byte b : md.digest(bytes)) {
            hex.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
        }
        return hex.toString();
    }

    /** The wild-type code with the named genes' segments replaced. */
    private static String override(String... kv) {
        String[] segs = Genotype.wildType().toCode().split("-");
        List<Gene> order = Genes.codeOrder();
        for (String entry : kv) {
            String[] p = entry.split("=");
            for (int i = 0; i < order.size(); i++) {
                if (order.get(i).key().endsWith("." + p[0])) {
                    segs[i] = order.get(i).key() + "=" + p[1];
                }
            }
        }
        return String.join("-", segs);
    }

    /** Synthetic 16x16 LUT; bottom row pure black, left edge red, top-left white. */
    private static GradientLut lut() {
        int s = 16;
        int[] a = new int[s * s];
        int white = 0xFFF0EDEA, red = 0xFF9B4A28, black = 0xFF000000;
        for (int y = 0; y < s; y++) {
            for (int x = 0; x < s; x++) {
                float redLevel = 1f - x / (float) (s - 1);
                float blackLevel = y / (float) (s - 1);
                a[y * s + x] = lerp(lerp(white, red, redLevel), black, blackLevel);
            }
        }
        return new GradientLut(a, s, s);
    }

    private static int lerp(int c0, int c1, float t) {
        int r = Math.round(((c0 >> 16) & 0xFF) + (((c1 >> 16) & 0xFF) - ((c0 >> 16) & 0xFF)) * t);
        int g = Math.round(((c0 >> 8) & 0xFF) + (((c1 >> 8) & 0xFF) - ((c0 >> 8) & 0xFF)) * t);
        int b = Math.round((c0 & 0xFF) + ((c1 & 0xFF) - (c0 & 0xFF)) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /**
     * A template with real per-texel variation, so the multiply-onto-template
     * and eye-redraw steps are actually exercised by the hash.
     */
    private static int[] template(Skin skin) {
        int[] t = new int[N * N];
        HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> {
            int shade = 190 + ((px * 7 + py * 13) % 66);
            t[py * N + px] = 0xFF000000 | (shade << 16) | (shade << 8) | shade;
        });
        return t;
    }
}
