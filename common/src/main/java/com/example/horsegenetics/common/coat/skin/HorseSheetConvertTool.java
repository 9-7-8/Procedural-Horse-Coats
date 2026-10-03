package com.example.horsegenetics.common.coat.skin;

import com.example.horsegenetics.common.coat.skin.HorseSheetConverter.Mirror;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * <b>{@code :common:convertHorseSheets}</b> - regenerate every converted sheet
 * from {@code common/sheet-sources/manifest.txt}. Build-time only (ImageIO), so
 * nothing the browser compiles reaches it.
 *
 * <p>An output is rewritten only when its <i>pixels</i> would change. That is
 * what keeps a re-run from dirtying git: ImageIO does not write the bytes the
 * white template was first saved with, and a file whose every pixel already
 * matches is left alone.
 */
public final class HorseSheetConvertTool {

    private HorseSheetConvertTool() {
    }

    /** One manifest line. */
    public record Entry(Path source, Path output, Skin skin, Mirror mirror, String origin) {
    }

    public static void main(String[] args) throws IOException {
        Path root = Path.of(args.length > 0 ? args[0] : ".");
        int written = 0;
        for (Entry e : manifest(root)) {
            int[] src = read(e.source());
            int size = (int) Math.round(Math.sqrt(src.length));
            HorseSheetConverter.Result r = HorseSheetConverter.convert(src, size, e.skin(), e.mirror());
            for (String w : r.warnings()) {
                System.out.println("  " + root.relativize(e.source()) + ": " + w);
            }
            System.out.println(root.relativize(e.output()) + "  (" + r.drawnPixels() + " drawn, "
                    + r.strayPixels() + " outside the mesh)");
            if (Files.exists(e.output()) && Arrays.equals(read(e.output()), r.argb())) {
                continue;
            }
            BufferedImage img = new BufferedImage(r.size(), r.size(), BufferedImage.TYPE_INT_ARGB);
            img.setRGB(0, 0, r.size(), r.size(), r.argb(), 0, r.size());
            ImageIO.write(img, "PNG", e.output().toFile());
            written++;
        }
        System.out.println(written + " sheet(s) rewritten");
    }

    /** The manifest, parsed. */
    public static List<Entry> manifest(Path root) throws IOException {
        Path file = root.resolve("common/sheet-sources/manifest.txt");
        List<Entry> out = new java.util.ArrayList<>();
        for (String raw : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] f = line.split("\\|");
            if (f.length != 5) {
                throw new IOException("manifest line needs five fields: " + line);
            }
            out.add(new Entry(root.resolve(f[0].trim()), root.resolve(f[1].trim()),
                    Skin.valueOf(f[2].trim().toUpperCase(java.util.Locale.ROOT)),
                    Mirror.valueOf(f[3].trim().toUpperCase(java.util.Locale.ROOT)), f[4].trim()));
        }
        return out;
    }

    /** A PNG's pixels, row-major ARGB. */
    public static int[] read(Path png) throws IOException {
        BufferedImage img = ImageIO.read(png.toFile());
        if (img == null) {
            throw new IOException("not an image: " + png);
        }
        int w = img.getWidth(), h = img.getHeight();
        int[] px = new int[w * h];
        img.getRGB(0, 0, w, h, px, 0, w);
        return px;
    }
}
