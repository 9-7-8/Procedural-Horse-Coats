package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * <b>Every registered gene, baked and hashed.</b> One line per gene per variant
 * allele: the horse it makes, through the real gradient and the real templates,
 * reduced to a SHA-256 of the composed sheet.
 *
 * <h2>Why this exists beside the pipeline golden</h2>
 * {@link CoatPipelineGoldenTest} hashes a <b>hand-picked list of genotypes</b>,
 * and that list exercises 39 of the registry's genes - 3 of the hundred-odd
 * data-driven ones. It is a very good regression test for melanin, the
 * dilutions, KIT and the white patterns, which is what it was built for.
 *
 * <p>It is not a regression test for the corpus, and on 2026-09-08 that stopped
 * being a theoretical complaint: a change to what a mask's {@code parts} means
 * altered what <b>twelve</b> genes paint, four of them over the whole horse, and
 * the pipeline golden came out <b>byte-identical</b>. A defect of that size was
 * invisible to the one test whose job is noticing that a coat moved. This test
 * is the answer to that (known-gaps gap 124): it has no list to fall behind,
 * because it walks {@link Genes#codeOrder()}.
 *
 * <h2>What it deliberately does not do</h2>
 * It hashes; it does not look. A hash tells you a gene moved and cannot tell you
 * whether it moved somewhere better - four of those twelve genes were <i>fixed</i>
 * by the change that broke the hashes. So a diff here is a <b>prompt to go and
 * look at the icons</b>, never a verdict. That is the same contract the pipeline
 * golden has and the reason both of them print how to regenerate rather than
 * just failing.
 *
 * <h2>It churns, and that is not a bug</h2>
 * Adding a gene or an epigenetic knob shifts every later gene's position in the
 * epigenetic draw (see known-gaps gap 117), so a registry change moves most of
 * these hashes at once. That is expected and is exactly why the file is
 * regenerated rather than edited: a wholesale diff means "the registry moved", a
 * handful of changed lines means "these genes moved", and the second is the one
 * worth reading.
 *
 * <h2>Regenerating</h2>
 * Delete {@code common/src/test/resources/coat-bake-golden.txt}, run this test,
 * copy {@code common/build/coat-bake-golden.txt} over it, and run again. Unlike
 * the pipeline golden this is a <b>single test</b> rather than a full suite, so
 * it costs seconds:
 * {@code ./gradlew :common:test --tests '*CoatBakeGoldenTest'}.
 */
class CoatBakeGoldenTest {

    private static final String RESOURCE = "/coat-bake-golden.txt";

    /**
     * One epigenome for every horse here. A single seed is enough because this
     * test is asking "did this gene's output change", not "does it vary" - and a
     * second seed would double the runtime to tell us the same thing twice.
     */
    private static final long SEED = 20260908L;

    private static int lastWidth;
    private static int lastHeight;

    @Test
    void everyGeneBakesTheSameSheetItAlwaysHas() throws IOException {
        String actual = render();
        String expected = readGolden();
        if (expected == null) {
            Path out = Path.of("build", "coat-bake-golden.txt");
            Files.createDirectories(out.toAbsolutePath().getParent());
            Files.writeString(out, actual, StandardCharsets.UTF_8);
            fail("no coat-bake golden on the test classpath - wrote a fresh one to "
                    + out.toAbsolutePath()
                    + "; copy it to common/src/test/resources/coat-bake-golden.txt");
        }
        if (!expected.equals(actual)) {
            Path out = Path.of("build", "coat-bake-golden.txt");
            Files.createDirectories(out.toAbsolutePath().getParent());
            Files.writeString(out, actual, StandardCharsets.UTF_8);
        }
        assertEquals(expected, actual,
                "a gene's baked sheet moved. That is not automatically wrong - go and look at "
                        + "the icons (./gradlew :common:bakeGeneIcons, then git status) before "
                        + "deciding. If it is intended, copy build/coat-bake-golden.txt over "
                        + "common/src/test/resources/coat-bake-golden.txt.");
    }

    /**
     * Every gene is represented, including the ones that paint nothing.
     *
     * <p>A non-painting gene contributes a line whose hash is the plain horse's,
     * and that is worth having rather than skipping: if one of them ever starts
     * painting, this test says so. Skipping them would make the file a list of
     * genes somebody once believed painted.
     */
    @Test
    void theGoldenCoversEveryRegisteredGene() throws IOException {
        String golden = readGolden();
        if (golden == null) {
            return; // the other test is already failing with instructions
        }
        List<String> missing = new ArrayList<>();
        for (Gene gene : Genes.codeOrder()) {
            if (!golden.contains(gene.key() + " ")) {
                missing.add(gene.key());
            }
        }
        assertEquals(List.of(), missing,
                "genes with no line in the coat-bake golden - regenerate it");
    }

    // ------------------------------------------------------------------

    private static String render() throws IOException {
        int[] adult = readArgb("/assets/horsegenetics/textures/entity/horse/horse_white.png");
        int[] gradient = readArgb("/assets/horsegenetics/textures/coat/redblackgradient.png");
        GradientLut base = new GradientLut(gradient, lastWidth, lastHeight);
        LutSet luts = LutSet.fromRegistry(base, path -> {
            try {
                int[] px = readArgb("/assets/horsegenetics/" + path);
                return new GradientLut(px, lastWidth, lastHeight);
            } catch (IOException missing) {
                return null;    // LutSet.resolve falls back to the base gradient
            }
        });
        Epigenome epi = Epigenome.fromSeed(SEED);

        StringBuilder sb = new StringBuilder();
        for (Gene gene : Genes.codeOrder()) {
            for (Allele variant : gene.alleles()) {
                AllelePair pair = new AllelePair(variant, variant);
                if (!gene.canOccur(pair)) {
                    continue;   // an embryonic lethal is never a horse
                }
                Genotype gt = Genotype.wildType().with(pair);
                int[] sheet = CoatTextureComposer.compose(gt, epi, Skin.ADULT, true, adult, luts);
                sb.append(gene.key()).append(' ').append(variant.token()).append(' ')
                        .append(sha256(sheet)).append('\n');
            }
        }
        return sb.toString();
    }

    private static String readGolden() throws IOException {
        try (InputStream in = CoatBakeGoldenTest.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                return null;
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        }
    }

    private static String sha256(int[] argb) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = new byte[argb.length * 4];
            for (int i = 0; i < argb.length; i++) {
                bytes[i * 4] = (byte) (argb[i] >>> 24);
                bytes[i * 4 + 1] = (byte) (argb[i] >>> 16);
                bytes[i * 4 + 2] = (byte) (argb[i] >>> 8);
                bytes[i * 4 + 3] = (byte) argb[i];
            }
            StringBuilder hex = new StringBuilder();
            for (byte b : digest.digest(bytes)) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static int[] readArgb(String resource) throws IOException {
        try (InputStream in = CoatBakeGoldenTest.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException("resource not found: " + resource);
            }
            BufferedImage img = ImageIO.read(in);
            lastWidth = img.getWidth();
            lastHeight = img.getHeight();
            int[] px = new int[lastWidth * lastHeight];
            img.getRGB(0, 0, lastWidth, lastHeight, px, 0, lastWidth);
            return px;
        }
    }

    @Test
    void theSheetIsTheSizeTheGeometryDeclares() throws IOException {
        int[] adult = readArgb("/assets/horsegenetics/textures/entity/horse/horse_white.png");
        assertTrue(adult.length == HorseSkinGeometry.SHEET_SIZE * HorseSkinGeometry.SHEET_SIZE,
                "the white template is not the sheet size the geometry declares");
    }
}
