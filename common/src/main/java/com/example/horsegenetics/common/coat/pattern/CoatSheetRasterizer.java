package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Sample;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import javax.imageio.ImageIO;

/**
 * Bakes a {@link CoatPattern} onto the coat sheet by walking every texel,
 * asking {@link HorseSkinGeometry#sample} which body-space point it covers,
 * and writing the pattern's colour there. Texels that map to no body part are
 * left fully transparent.
 *
 * <p>{@link #bake} is pure ({@code int[]} ARGB, row-major). {@link #writePng}
 * is a thin {@code javax.imageio} wrapper for dev tooling; nothing on the game
 * runtime path calls it. (The live coat pipeline is
 * {@link CoatTextureComposer}, driven by the gene model.)
 */
public final class CoatSheetRasterizer {

    private CoatSheetRasterizer() {}

    /** Row-major {@code 0xAARRGGBB}, {@code SHEET_SIZE * SHEET_SIZE} entries; 0 where unmapped. */
    public static int[] bake(CoatPattern pattern) {
        int n = HorseSkinGeometry.SHEET_SIZE;
        int[] argb = new int[n * n];
        for (int py = 0; py < n; py++) {
            for (int px = 0; px < n; px++) {
                Optional<Sample> s = HorseSkinGeometry.sample(px, py);
                if (s.isPresent()) {
                    var p = s.get().point();
                    argb[py * n + px] = pattern.argb(p.x(), p.y(), p.z());
                }
            }
        }
        return argb;
    }

    /** Write a {@link #bake} result to {@code out} as a PNG (creates parent dirs). */
    public static void writePng(int[] argb, Path out) throws IOException {
        int n = HorseSkinGeometry.SHEET_SIZE;
        if (argb.length != n * n) {
            throw new IllegalArgumentException("expected " + (n * n) + " pixels, got " + argb.length);
        }
        BufferedImage img = new BufferedImage(n, n, BufferedImage.TYPE_INT_ARGB);
        img.setRGB(0, 0, n, n, argb, 0, n);
        Path parent = out.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        ImageIO.write(img, "PNG", out.toFile());
    }
}
