package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.BodyPoint;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Face;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders one <b>three-quarter snapshot per registered gene</b>, on a standard
 * bay, for the wiki's gene index and gene cards to use as an icon.
 *
 * <p>Two jobs, and the second is the one that earns it. As an <i>icon</i> baker
 * it turns a list of gene names into a page a reader can scan. As a
 * <i>contact sheet</i> it is the only way to look at eighty genes without
 * launching the game eighty times - and the coat pipeline has a long history of
 * measurements that agreed with a horse nobody had actually looked at (see
 * {@code wiki/verification.html}). Bake, open the folder, and the gene that
 * paints nothing is obvious in a second.
 *
 * <p>The projection is deliberately simple: every texel whose face points at
 * the viewer, placed on the <b>posed</b> mesh - the cuboids the game actually
 * draws, by way of {@code HorseSkinGeometry.posed} - and painted back to front
 * so a near leg covers a far one. It is not the game's renderer and is not
 * trying to be; it is a silhouette that shows where a gene put its paint.
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
     * The fallback horses, in order, for the genes that paint <b>nothing</b> on
     * a plain bay - each one giving a different kind of modifier gene something
     * to modify.
     *
     * <ul>
     *   <li><b>a tobiano bay</b> for the genes that recolour <i>white
     *       markings</i> rather than drawing a shape - Voided and Opalized do
     *       correctly nothing at all on a horse with no white;</li>
     *   <li><b>a chestnut</b> for the genes that need red pigment - Flaxen
     *       lightens a chestnut's mane and tail and is invisible on a bay;</li>
     *   <li><b>a leopard bay</b> for the genes that shape somebody else's
     *       pattern - PATN1 and PATN2 only exist to widen a blanket.</li>
     * </ul>
     *
     * <p>An icon of nothing is a useless icon, however honest. Which backdrop a
     * gene lands on is <b>detected rather than declared</b>: a gene that starts
     * or stops painting quietly changes backdrop on the next bake, which is a
     * hint worth having, and no list here goes stale.
     */
    private static final String[] BACKDROPS = {
        "agouti=A/a tobiano=To/to",
        "extension=e/e",
        "agouti=A/a leopard=LP/lp"
    };

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
        Map<String, List<String>> onBackdrop = new LinkedHashMap<>();
        List<String> invisible = new ArrayList<>();
        // The plain bay, so a reader has something to compare every icon against.
        int[] plainSheet = CoatTextureComposer.compose(override(BASE), Epigenome.fromSeed(11),
                Skin.ADULT, true, template, luts);
        ImageIO.write(sideView(plainSheet), "PNG", outDir.resolve("_bay.png").toFile());
        written.add("_bay");

        for (Gene gene : Genes.all()) {
            String slug = gene.key().substring(gene.key().indexOf('.') + 1);
            // Seeded off the key, so re-baking gives the same horse back and the
            // icons do not all churn every time one gene is added.
            Epigenome epi = Epigenome.fromSeed(gene.key().hashCode() * 2654435761L);
            int[] sheet = showing(gene, BASE, epi, template, luts);
            for (String backdrop : BACKDROPS) {
                if (sheet != null) {
                    break;
                }
                sheet = showing(gene, backdrop, epi, template, luts);
                if (sheet != null) {
                    onBackdrop.computeIfAbsent(backdrop, k -> new ArrayList<>()).add(slug);
                }
            }
            if (sheet == null) {
                sheet = CoatTextureComposer.compose(override(BASE), epi,
                        Skin.ADULT, true, template, luts);
                invisible.add(slug);
            }
            ImageIO.write(sideView(sheet), "PNG", outDir.resolve(slug + ".png").toFile());
            written.add(slug);
        }
        onBackdrop.forEach((backdrop, slugs) ->
                System.out.println("on " + backdrop + " (nothing shows on a plain bay): " + slugs));
        if (!invisible.isEmpty()) {
            System.out.println("no coat of their own - baked as the plain bay: " + invisible);
        }

        Files.writeString(outDir.resolve("index.txt"),
                String.join("\n", written) + "\n", StandardCharsets.UTF_8);
        System.out.println("wrote " + written.size() + " icons to " + outDir.toAbsolutePath());
    }

    /** A channel has to move this far before a texel counts as repainted. */
    private static final int CHANNEL_STEP = 16;

    /** And this many texels have to move before the gene counts as showing. */
    private static final int MIN_TEXELS = 24;

    /**
     * <b>The gene showing itself</b>, over {@code base}: whichever of its
     * alleles repaints the most of the horse when homozygous - or {@code null}
     * if none of them repaints enough of it to be worth a picture.
     *
     * <p>Homozygous, because an icon has one image to spend and the full
     * expression is the honest thing to spend it on; the gene's own page
     * carries the heterozygote beside it. <b>The loudest allele</b> rather than
     * the first declared, because a great many genes declare the wild type
     * first - Extension leads with {@code E}, and an icon of {@code E/E} on a
     * bay is a picture of a bay.
     *
     * <p>Loudest is measured, not assumed equal-or-not: swapping one allele
     * copy for another swaps the <i>epigenome slot</i> the coat reads with it,
     * so {@code A/A} is never byte-identical to the {@code A/a} underneath it
     * even though both are the same bay. A plain equality test therefore called
     * the wild type a picture of the gene. Counting the texels that moved by
     * {@value #CHANNEL_STEP} or more separates that jitter, which is a handful
     * of texels, from a gene that actually paints, which is hundreds.
     */
    private static int[] showing(Gene gene, String base, Epigenome epi,
            int[] template, LutSet luts) {
        Genotype plain = override(base);
        int[] plainSheet = CoatTextureComposer.compose(plain, epi,
                Skin.ADULT, true, template, luts);
        int[] best = null;
        int loudest = 0;
        for (Allele variant : gene.alleles()) {
            int[] sheet = CoatTextureComposer.compose(
                    plain.with(new AllelePair(variant, variant)), epi,
                    Skin.ADULT, true, template, luts);
            int moved = repainted(sheet, plainSheet);
            if (moved > loudest) {
                loudest = moved;
                best = sheet;
            }
        }
        return loudest >= MIN_TEXELS ? best : null;
    }

    /** How many texels {@code sheet} moved off {@code from}, ignoring jitter. */
    private static int repainted(int[] sheet, int[] from) {
        int moved = 0;
        for (int i = 0; i < sheet.length; i++) {
            if (sheet[i] == from[i]) {
                continue;
            }
            int d = Math.max(Math.max(
                    Math.abs(((sheet[i] >> 16) & 0xFF) - ((from[i] >> 16) & 0xFF)),
                    Math.abs(((sheet[i] >> 8) & 0xFF) - ((from[i] >> 8) & 0xFF))),
                    Math.abs((sheet[i] & 0xFF) - (from[i] & 0xFF)));
            if (d >= CHANNEL_STEP) {
                moved++;
            }
        }
        return moved;
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
     *
     * <p><b>It draws the posed mesh, not the bounding boxes.</b> Every texel is
     * placed by {@link HorseSkinGeometry#posed}, which is the same cuboid the
     * gene pages' preview window builds - see {@code model3d.js emitPart}, and
     * its note on why an AABB horse is a pile of blocks: the adult neck's box is
     * nearly twice the neck, so a head baked from bounds floats in front of a
     * neck that is far too deep, and the topline the camera was raised to show
     * is a flat lid rather than the tilted crest it is in game. Back-face
     * culling asks the <i>posed</i> normal for the same reason - a pitched
     * part's "top" is not +Y.
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
        // One normal per (part, face) rather than per texel - it is a property
        // of the box, and there are 128x128 texels to get through.
        Map<Part, Map<Face, BodyPoint>> normals = new EnumMap<>(Part.class);
        HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, part, face, point) -> {
            BodyPoint n = normals
                    .computeIfAbsent(part, k -> new EnumMap<>(Face.class))
                    .computeIfAbsent(face, f -> HorseSkinGeometry.posedNormal(Skin.ADULT, part, f));
            if (n.x() * cx + n.y() * cy + n.z() * cz <= 0) {
                return;
            }
            int argb = sheet[py * HorseSkinGeometry.SHEET_SIZE + px];
            if ((argb >>> 24) == 0) {
                return;
            }
            BodyPoint at = HorseSkinGeometry.posed(Skin.ADULT, part, face, point);
            double sx = at.x() * rx + at.z() * rz;
            double sy = -(at.x() * ux + at.y() * uy + at.z() * uz);
            double depth = at.x() * cx + at.y() * cy + at.z() * cz;
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
