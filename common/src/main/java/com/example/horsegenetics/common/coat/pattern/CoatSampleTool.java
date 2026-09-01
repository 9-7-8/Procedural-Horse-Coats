package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.GeneCodeDisplay;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Build/dev tooling: renders sample coats through the real
 * {@link CoatTextureComposer} + {@code redblackgradient.png} + the white
 * templates, so the pipeline can be eyeballed without launching the game.
 * {@code ./gradlew :common:bakeCoatSamples} - output dir is arg 0.
 */
public final class CoatSampleTool {

    /** label -> gene tokens (only the non-wild ones need naming). */
    private static final String[][] SAMPLES = {
            {"black", ""},
            {"chestnut", "extension=e/e"},
            {"bay", "agouti=A/a"},
            {"bay_high", "agouti=A/a"},   // different seed -> seal-ish
            {"champagne_black", "champagne=Ch/c"},
            {"champagne_bay", "agouti=A/a champagne=Ch/c"},
            {"buckskin", "agouti=A/a cream=Cr/N"},
            {"palomino", "extension=e/e cream=Cr/N"},
            {"perlino", "agouti=A/a cream=Cr/Cr"},
            {"pearl_bay", "agouti=A/a pearl=prl/prl"},
            {"cream_pearl_bay", "agouti=A/a cream=Cr/N pearl=prl/N"},
            {"grey_black", "grey=G/g"},
            {"grey_bay", "agouti=A/a grey=G/g"},
            {"white", "white=W/w"},
            {"bay_splash", "agouti=A/a splash=Spl/spl"},
            {"chestnut_test", "extension=e/e test=T/t"},
    };

    private static final long[] SEEDS = {0, 0, 12345, 88, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 31, 0};

    private CoatSampleTool() {}

    public static void main(String[] args) throws IOException {
        Path outDir = Path.of(args.length > 0 ? args[0] : "build/coat-samples");
        Files.createDirectories(outDir);

        int n = HorseSkinGeometry.SHEET_SIZE;
        int[] adultTemplate = readArgb("/assets/horsegenetics/textures/entity/horse/horse_white.png");
        int[] babyTemplate = readArgb("/assets/horsegenetics/textures/entity/horse/horse_white_baby.png");
        int gw = lastReadWidth, gh = lastReadHeight;
        int[] g = readArgb("/assets/horsegenetics/textures/coat/redblackgradient.png");
        GradientLut lut = new GradientLut(g, lastReadWidth, lastReadHeight);
        // gw/gh above were overwritten; not needed further

        for (int i = 0; i < SAMPLES.length; i++) {
            Genotype gt = build(SAMPLES[i][1]);
            long seed = SEEDS[i];
            int[] adult = CoatTextureComposer.compose(gt, seed, Skin.ADULT, true, adultTemplate, lut);
            int[] foal = CoatTextureComposer.compose(gt, seed, Skin.BABY, false, babyTemplate, lut);
            writePng(adult, n, n, outDir.resolve(SAMPLES[i][0] + ".png"));
            writePng(foal, n, n, outDir.resolve(SAMPLES[i][0] + "_foal.png"));
            System.out.println("wrote " + SAMPLES[i][0] + "(.png/_foal.png)  " + GeneCodeDisplay.shortForm(gt));
        }
        System.out.println("-> " + outDir.toAbsolutePath());
    }

    private static Genotype build(String spec) {
        Genotype gt = Genotype.wildType();
        if (spec.isBlank()) {
            return gt;
        }
        // rebuild the code by overriding named segments
        String[] segs = gt.toCode().split("-");
        var order = Genes.codeOrder();
        for (String kv : spec.trim().split("\\s+")) {
            String[] p = kv.split("=");
            int idx = -1;
            for (int i = 0; i < order.size(); i++) {
                if (order.get(i).key().endsWith("." + p[0])) {
                    idx = i;
                    break;
                }
            }
            segs[idx] = p[1];
        }
        return Genotype.parse(String.join("-", segs));
    }

    private static int lastReadWidth;
    private static int lastReadHeight;

    private static int[] readArgb(String resource) throws IOException {
        try (InputStream in = CoatSampleTool.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException("resource not found: " + resource);
            }
            BufferedImage img = ImageIO.read(in);
            lastReadWidth = img.getWidth();
            lastReadHeight = img.getHeight();
            int[] px = new int[lastReadWidth * lastReadHeight];
            img.getRGB(0, 0, lastReadWidth, lastReadHeight, px, 0, lastReadWidth);
            return px;
        }
    }

    private static void writePng(int[] argb, int w, int h, Path out) throws IOException {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        img.setRGB(0, 0, w, h, argb, 0, w);
        ImageIO.write(img, "PNG", out.toFile());
    }
}
