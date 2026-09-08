package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Bounds;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Face;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.spec.SpecGene;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Renders one <b>side-on snapshot per data-driven gene</b>, on a standard bay,
 * for the wiki's gene index to use as an icon.
 *
 * <p>Two jobs, and the second is the one that earns it. As an <i>icon</i> baker
 * it turns a list of gene names into a page a reader can scan. As a
 * <i>contact sheet</i> it is the only way to look at eighty genes without
 * launching the game eighty times - and the coat pipeline has a long history of
 * measurements that agreed with a horse nobody had actually looked at (see
 * {@code wiki/verification.html}). Bake, open the folder, and the gene that
 * paints nothing is obvious in a second.
 *
 * <p>The projection is deliberately flat: every texel whose face points at the
 * viewer, plotted by its body {@code (x, y)}, painted back to front so a near
 * leg covers a far one. It is not the game's renderer and is not trying to be;
 * it is a silhouette that shows where a gene put its paint.
 *
 * <p>{@code ./gradlew :common:bakeGeneIcons} - output dir is arg 0.
 */
public final class GeneIconTool {

    /** Pixels per body unit. The adult barrel is 22 long, so this is a ~150px horse. */
    private static final int SCALE = 6;

    /** Blank margin round the silhouette, in pixels. */
    private static final int MARGIN = 4;

    /**
     * The camera, as a direction from the horse toward the viewer: off the
     * horse's left ({@code -Z}), ahead of the shoulder ({@code +X}), and well
     * above ({@code +Y}). The elevation is the part that was tuned: a level
     * camera cannot see the topline at all, and a shallow one foreshortens it
     * to a two-pixel strip, which for a dorsal-stripe gene is the same as
     * showing nothing. High enough to read the back, low enough to keep the
     * near flank.
     */
    private static final double VX = 0.38;
    private static final double VY = 0.72;
    private static final double VZ = -0.58;

    /** Pixels a single texel is painted as. See {@link #fill}. */
    private static final int BLOCK = SCALE / HorseSkinGeometry.TEXELS_PER_UNIT + 2;

    /**
     * The horse every icon is taken on: an ordinary bay, which is what the
     * owner asked for and is also the right call - a black horse hides a dark
     * marking and a grey one hides a pale one, and bay is the only common base
     * that shows both.
     */
    private static final String BASE = "agouti=A/a";

    /**
     * The fallback horse, for the genes that paint <b>nothing</b> on a plain
     * bay.
     *
     * <p>Voided and Opalized do not draw a shape of their own: they change what
     * the horse's <i>white markings</i> come out as, so on a horse with no
     * white they correctly do nothing at all - and an icon of nothing is a
     * useless icon, however honest. A gene that comes out identical to the
     * plain bay is re-baked on a bay with a tobiano, which gives it something
     * to work on. Detected rather than declared: a gene that stops painting is
     * then a gene whose icon quietly changes backdrop, which is a hint worth
     * having.
     */
    private static final String MARKED = "agouti=A/a tobiano=To/to";

    private static int lastReadWidth;
    private static int lastReadHeight;

    private GeneIconTool() {}

    public static void main(String[] args) throws IOException {
        Path outDir = Path.of(args.length > 0 ? args[0] : "wiki/assets/gene-icons");
        Files.createDirectories(outDir);

        int[] template = readArgb("/assets/horsegenetics/textures/entity/horse/horse_white.png");
        int[] g = readArgb("/assets/horsegenetics/textures/coat/redblackgradient.png");
        GradientLut base = new GradientLut(g, lastReadWidth, lastReadHeight);
        int[] bp = readArgb("/assets/horsegenetics/textures/coat/lutbluepink.png");
        GradientLut bluepink = new GradientLut(bp, lastReadWidth, lastReadHeight);
        LutSet luts = new LutSet(base, java.util.Map.of("bluepink", bluepink));

        List<String> written = new ArrayList<>();
        List<String> onMarked = new ArrayList<>();
        // The plain bay, so a reader has something to compare every icon against.
        Genotype plain = bayWith(null, BASE);
        int[] plainSheet = CoatTextureComposer.compose(plain, Epigenome.fromSeed(11),
                Skin.ADULT, true, template, luts);
        ImageIO.write(sideView(plainSheet), "PNG", outDir.resolve("_bay.png").toFile());
        written.add("_bay");

        for (SpecGene gene : Genes.loaded()) {
            String slug = gene.key().substring(gene.key().indexOf('.') + 1);
            // Seeded off the key, so re-baking gives the same horse back and the
            // icons do not all churn every time one gene is added.
            Epigenome epi = Epigenome.fromSeed(gene.key().hashCode() * 2654435761L);
            int[] sheet = CoatTextureComposer.compose(bayWith(gene, BASE), epi,
                    Skin.ADULT, true, template, luts);
            if (Arrays.equals(sheet, CoatTextureComposer.compose(plain, epi,
                    Skin.ADULT, true, template, luts))) {
                sheet = CoatTextureComposer.compose(bayWith(gene, MARKED), epi,
                        Skin.ADULT, true, template, luts);
                onMarked.add(slug);
            }
            ImageIO.write(sideView(sheet), "PNG", outDir.resolve(slug + ".png").toFile());
            written.add(slug);
        }
        if (!onMarked.isEmpty()) {
            System.out.println("on a marked bay (they paint nothing on a plain one): " + onMarked);
        }

        Files.writeString(outDir.resolve("index.txt"),
                String.join("\n", written) + "\n", StandardCharsets.UTF_8);
        System.out.println("wrote " + written.size() + " icons to " + outDir.toAbsolutePath());
    }

    /**
     * A bay carrying two copies of {@code gene}'s first-declared allele - the
     * gene at full expression, which is what an icon should show. A gene whose
     * homozygote is a different outcome from its heterozygote therefore
     * advertises the homozygote; that is the honest choice for a one-image
     * summary, and the gene's own page carries both.
     */
    private static Genotype bayWith(SpecGene gene, String base) {
        Genotype gt = override(base);
        if (gene == null) {
            return gt;
        }
        Allele variant = gene.alleles().get(0);
        return gt.with(new AllelePair(variant, variant));
    }

    // ------------------------------------------------------------------

    /**
     * Project the sheet to a three-quarter view from above the horse's left
     * shoulder.
     *
     * <p>It started as a flat side view, which was simpler and wrong: a
     * <b>dorsal</b> marking - a dorsal stripe, a topline blanket, half the
     * genes in this import - lives on the {@code TOP} faces, and a camera level
     * with the horse cannot see one. An icon that shows nothing for a gene that
     * paints plenty is worse than no icon, so the camera went up and round.
     *
     * <p>Faces pointing away are dropped and the rest are painted far to near,
     * which is the whole of the hidden-surface handling. It is not the game's
     * renderer; it is a silhouette that shows where a gene put its paint.
     */
    private static BufferedImage sideView(int[] sheet) {
        // From the horse's left (negative Z), forward of the shoulder, looking
        // down. Normalised so the pixel scale means what SCALE says.
        double len = Math.sqrt(VX * VX + VY * VY + VZ * VZ);
        double vx = VX / len;
        double vy = VY / len;
        double vz = VZ / len;
        // right = normalize(up x v), up' = v x right. World up is +Y.
        double rlen = Math.hypot(vz, vx);
        final double rx = vz / rlen;
        final double rz = -vx / rlen;
        final double ux = -vy * rz;
        final double uy = vz * rx - vx * rz;
        final double uz = vy * rx;

        final double cx = vx;
        final double cy = vy;
        final double cz = vz;
        List<double[]> queue = new ArrayList<>();
        double minSx = Double.MAX_VALUE;
        double maxSx = -Double.MAX_VALUE;
        double minSy = Double.MAX_VALUE;
        double maxSy = -Double.MAX_VALUE;
        HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, part, face, point) -> {
            if (facing(face, cx, cy, cz) <= 0) {
                return;
            }
            int argb = sheet[py * HorseSkinGeometry.SHEET_SIZE + px];
            if ((argb >>> 24) == 0) {
                return;
            }
            double sx = point.x() * rx + point.z() * rz;
            double sy = -(point.x() * ux + point.y() * uy + point.z() * uz);
            double depth = point.x() * cx + point.y() * cy + point.z() * cz;
            queue.add(new double[]{depth, sx, sy, argb});
        });
        for (double[] t : queue) {
            minSx = Math.min(minSx, t[1]);
            maxSx = Math.max(maxSx, t[1]);
            minSy = Math.min(minSy, t[2]);
            maxSy = Math.max(maxSy, t[2]);
        }
        if (queue.isEmpty()) {
            return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        }

        int w = (int) Math.ceil((maxSx - minSx) * SCALE) + MARGIN * 2 + BLOCK;
        int h = (int) Math.ceil((maxSy - minSy) * SCALE) + MARGIN * 2 + BLOCK;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        // Far first, so a near leg lands on top of the far one.
        queue.sort((a, b) -> Double.compare(a[0], b[0]));
        for (double[] t : queue) {
            int x = MARGIN + (int) Math.round((t[1] - minSx) * SCALE);
            int y = MARGIN + (int) Math.round((t[2] - minSy) * SCALE);
            fill(img, x, y, (int) t[3]);
        }
        return img;
    }

    /** How square-on a face is to the camera; at or below zero it is facing away. */
    private static double facing(Face face, double vx, double vy, double vz) {
        return switch (face) {
            case NOSE -> vx;
            case TAIL -> -vx;
            case TOP -> vy;
            case BOTTOM -> -vy;
            case RIGHT -> vz;
            case LEFT -> -vz;
        };
    }

    /**
     * One texel drawn as a block. Deliberately a pixel or two wider than the
     * texel pitch: at an angle the projected texel centres do not tile, and an
     * exactly-sized block leaves the horse full of pinholes.
     */
    private static void fill(BufferedImage img, int x, int y, int argb) {
        for (int dy = 0; dy < BLOCK; dy++) {
            for (int dx = 0; dx < BLOCK; dx++) {
                int px = x + dx;
                int py = y + dy;
                if (px >= 0 && py >= 0 && px < img.getWidth() && py < img.getHeight()) {
                    img.setRGB(px, py, argb);
                }
            }
        }
    }

    // ------------------------------------------------------------------

    /** {@code "gene=a/b"} pairs applied over the wild type - the CoatSampleTool spelling. */
    private static Genotype override(String spec) {
        Genotype gt = Genotype.wildType();
        for (String kv : spec.trim().split("\\s+")) {
            String[] p = kv.split("=");
            Gene gene = Genes.byKeyOrNull(Genes.NS + "." + p[0]);
            if (gene == null) {
                throw new IllegalArgumentException("no gene " + p[0]);
            }
            String[] tokens = p[1].split("/");
            gt = gt.with(new AllelePair(allele(gene, tokens[0]), allele(gene, tokens[1])));
        }
        return gt;
    }

    private static Allele allele(Gene gene, String token) {
        for (Allele a : gene.alleles()) {
            if (a.token().equals(token)) {
                return a;
            }
        }
        throw new IllegalArgumentException(gene.key() + " has no allele " + token);
    }

    private static int[] readArgb(String resource) throws IOException {
        try (InputStream in = GeneIconTool.class.getResourceAsStream(resource)) {
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
}
