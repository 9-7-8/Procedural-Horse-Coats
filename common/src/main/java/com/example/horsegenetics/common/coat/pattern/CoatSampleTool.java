package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.genetics.Genotype;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Build/dev tooling: renders a strip of sample coats through the real
 * {@link CoatTextureComposer} + {@code redblackgradient.png} + {@code
 * horse_white.png}, and writes each as a PNG so the pipeline can be eyeballed
 * without launching the game.
 *
 * <p>{@code ./gradlew :common:bakeCoatSamples} - output dir is arg 0
 * (default {@code build/coat-samples}). Not on any game-runtime path;
 * {@code javax.imageio} is confined to this class and {@link CoatSheetRasterizer}.
 */
public final class CoatSampleTool {

    /** code, epigenetic seed, filename.  segments: extension-agouti-white-test-champagne-seal-splash */
    private static final String[][] SAMPLES = {
            {"E/E-a/a-w/w-t/t-c/c-sl/sl-spl/spl", "0", "black"},
            {"e/e-a/a-w/w-t/t-c/c-sl/sl-spl/spl", "0", "chestnut"},
            {"E/e-A/a-w/w-t/t-c/c-sl/sl-spl/spl", "12345", "bay"},
            {"E/e-A/a-w/w-t/t-c/c-sl/sl-spl/spl", "999", "bay2"},
            {"E/E-a/a-w/w-t/t-c/c-Sl/sl-spl/spl", "4242", "seal"},
            {"E/E-a/a-w/w-t/t-Ch/c-sl/sl-spl/spl", "0", "champagne_black"},
            {"E/e-A/a-w/w-t/t-Ch/c-sl/sl-spl/spl", "7", "champagne_bay"},
            {"e/e-a/a-w/w-t/t-Ch/c-sl/sl-spl/spl", "0", "champagne_chestnut"},
            {"e/e-a/a-W/w-t/t-c/c-sl/sl-spl/spl", "0", "white"},
            {"E/e-A/a-w/w-t/t-c/c-sl/sl-Spl/spl", "31", "bay_splash"},
            {"E/E-a/a-w/w-t/t-c/c-sl/sl-Spl/spl", "88", "black_splash"},
            {"e/e-a/a-w/w-T/t-c/c-sl/sl-spl/spl", "0", "chestnut_test"},
    };

    private CoatSampleTool() {}

    public static void main(String[] args) throws IOException {
        Path outDir = Path.of(args.length > 0 ? args[0] : "build/coat-samples");
        Files.createDirectories(outDir);

        int n = HorseSkinGeometry.SHEET_SIZE;
        int[] template = readArgb("/assets/horsegenetics/textures/entity/horse/horse_white.png");
        int[] g = readArgb("/assets/horsegenetics/textures/coat/redblackgradient.png");
        int gw = lastReadWidth;
        int gh = lastReadHeight;
        GradientLut lut = new GradientLut(g, gw, gh);

        for (String[] s : SAMPLES) {
            int[] argb = CoatTextureComposer.compose(Genotype.parse(s[0]), Long.parseLong(s[1]), template, lut);
            writePng(argb, n, n, outDir.resolve(s[2] + ".png"));
            System.out.println("wrote " + s[2] + ".png  (" + s[0] + ")");
        }
        System.out.println("-> " + outDir.toAbsolutePath());
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
