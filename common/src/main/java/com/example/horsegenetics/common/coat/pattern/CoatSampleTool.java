package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.GeneCodeDisplay;
import com.example.horsegenetics.common.genetics.Epigenome;
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
            {"bay", "agouti=A/a"},                          // Sh/Sh - the ordinary bay
            {"bay_seal", "agouti=A/A shade=ShD/ShD"},       // the dark haplotype - seal brown
            {"champagne_black", "champagne=Ch/c"},
            {"champagne_bay", "agouti=A/a champagne=Ch/c"},
            {"buckskin", "agouti=A/a matp=Cr/N"},
            {"palomino", "extension=e/e matp=Cr/N"},
            {"perlino", "agouti=A/a matp=Cr/Cr"},
            {"pearl_bay", "agouti=A/a matp=prl/prl"},
            {"cream_pearl_bay", "agouti=A/a matp=Cr/prl"},
            {"grey_steel", "grey=G/g"},         // barely greyed
            {"grey_dapple", "grey=G/g"},        // mid - strongest dapples
            {"grey_old", "grey=G/g"},           // nearly white
            {"grey_bay", "agouti=A/a grey=G/g"},
            {"grey_chestnut", "extension=e/e grey=G/g"},
            {"bay_blood", "agouti=A/A shade=ShL/ShL"},      // the light haplotype - blood bay
            {"kit_dominant_white", "kit=W22/N"},
            {"bay_splash", "agouti=A/a mitf=SW1/N"},
            {"zebra_bay", "agouti=A/a magic_zebra=Mzeb/n"},          // stripes over a bay
            {"zebra_bay_long", "agouti=A/a magic_zebra=Mzeb/n"},     // same gene, rings further down the legs
            {"zebra_palomino", "extension=e/e matp=Cr/N magic_zebra=Mzeb/n"},
            {"zebra_white", "kit=W22/N magic_zebra=Mzeb/n"},        // magical paints over dominant white
            {"pink_hair_black", "pink_hair=Pihr/Pihr"},
            {"pink_hair_chestnut", "extension=e/e pink_hair=Pihr/Pihr"},
            {"pink_hair_perlino", "agouti=A/a matp=Cr/Cr pink_hair=Pihr/Pihr"},
            {"zebra_pink_bay", "agouti=A/a magic_zebra=Mzeb/n pink_hair=Pihr/Pihr"}, // two magical genes at once
            {"dun_bay", "agouti=A/a dun=D/d2"},          // dorsal stripe + leg bars over tan
            {"dun_black", "dun=D/d2"},                    // grullo
            {"dun_chestnut", "extension=e/e dun=D/d2"},   // red dun
            {"dun_marked_bay", "agouti=A/a dun=d1/d2"},  // undiluted, dorsal stripe only
            {"dun_marked_black", "dun=d1/d1"},           // undiluted black + spine line
            {"silver_black", "silver=Z/z"},              // chocolate body, flaxen mane
            {"silver_bay", "agouti=A/a silver=Z/z"},     // silver bay
            {"mushroom_chestnut", "extension=e/e mushroom=Mu/Mu"},
            {"roan_black", "roan=Rn/rn"},                // blue roan
            {"roan_bay", "agouti=A/a roan=Rn/rn"},
            {"tobiano_black", "tobiano=To/to"},
            {"frame_bay", "agouti=A/a ednrb=O/N"},
            {"lethal_white_bay", "agouti=A/a ednrb=O/O"},     // all white, and in a real horse fatal
            // the KIT ladder, all on a bay so the white reads against colour
            {"kit_w20_bay", "agouti=A/a kit=W20/N"},          // a star and a sock
            {"kit_w20_hom_bay", "agouti=A/a kit=W20/W20"},
            {"kit_sabino_bay", "agouti=A/a kit=SB1/N"},
            {"kit_sabino_boosted_bay", "agouti=A/a kit=SB1/W20"},
            {"kit_sabino_white_bay", "agouti=A/a kit=SB1/SB1"},
            {"kit_broad_black", "kit=W5/N"},
            {"kit_extensive_bay", "agouti=A/a kit=W13/N"},
            // the two splash loci, alone and together
            {"splash_mitf_bay", "agouti=A/a mitf=SW1/SW1"},
            {"splash_pax3_bay", "agouti=A/a pax3=SW2/N"},
            {"splash_both_loci_bay", "agouti=A/a mitf=SW1/N pax3=SW2/N"},
            // the magical utility genes that paint - hair colour, healer, light
            {"mane_solid_bay", "agouti=A/a mane_color=Mnsld/n"},
            {"mane_solid_bay_alt", "agouti=A/a mane_color=Mnsld/n"},   // same gene, another colour
            {"mane_striped_black", "mane_color=Mnstrp/n"},
            {"mane_solid_striped_bay", "agouti=A/a mane_color=Mnsld/Mnstrp"},  // two colours at once
            {"tail_solid_chestnut", "extension=e/e tail_color=Tlsld/n"},
            {"tail_solid_striped_bay", "agouti=A/a tail_color=Tlsld/Tlstrp"},
            {"mane_and_tail_bay", "agouti=A/a mane_color=Mnsld/n tail_color=Tlstrp/n"},
            {"healer_bay", "agouti=A/a healer=Hlr/Hlr"},
            {"healer_faint_black", "healer=Hlr/Hlr"},                  // same gene, low opacity
            {"healer_over_mane_colour", "agouti=A/a mane_color=Mnsld/n healer=Hlr/Hlr"},
            {"light_hooves_bay", "agouti=A/a light=Lthf/n"},
            {"light_mane_black", "light=Ltmn/n"},
            {"light_eyes_chestnut", "extension=e/e light=Lteye/n"},
            {"light_hooves_mane_bay", "agouti=A/a light=Lthf/Ltmn"},
            {"light_mane_eyes_white", "kit=W22/N light=Ltmn/Lteye"},
            // LUT: same melanin genotypes, resolved against the blue/pink gradient
            {"lut_bluepink_black", "lut=Blupnk/Blupnk"},
            {"lut_bluepink_bay", "agouti=A/a lut=Blupnk/Blupnk"},
            {"lut_bluepink_chestnut", "extension=e/e lut=Blupnk/Blupnk"},
            // the leopard complex - LP zygosity x PATN1 x PATN2, all on a bay
            {"lp_mottled_bay", "agouti=A/a leopard=LP/lp"},
            {"lp_varnish_roan_bay", "agouti=A/a leopard=LP/LP"},
            {"lp_leopard_bay", "agouti=A/a leopard=LP/lp patn1=PATN1/n"},
            {"lp_fewspot_bay", "agouti=A/a leopard=LP/LP patn1=PATN1/n"},
            {"lp_blanket_bay", "agouti=A/a leopard=LP/lp patn2=PATN2/n"},
            {"lp_snowcap_bay", "agouti=A/a leopard=LP/LP patn2=PATN2/n"},
            {"lp_semi_leopard_bay", "agouti=A/a leopard=LP/lp patn1=PATN1/n patn2=PATN2/n"},
            {"lp_leopard_black", "leopard=LP/lp patn1=PATN1/n"},

            // brindle - the X-linked locus, both the stallion and the mare form
            {"brindle_stallion_bay", "sex=X/Y agouti=A/a brindle=Brn/Y"},
            {"brindle_mare_bay", "agouti=A/a brindle=Brn/Brn"},
            {"brindle_chestnut", "extension=e/e brindle=Brn/Brn"},
            {"brindle_black", "brindle=Brn/Brn"},

            // natural zebra - the same body map, made by taking pigment out
            {"natural_zebra_black", "natural_zebra=Zeb/Zeb"},            // the classic: black and white
            {"natural_zebra_bay", "agouti=A/a natural_zebra=Zeb/Zeb"},
            {"natural_zebra_chestnut", "extension=e/e natural_zebra=Zeb/Zeb"},  // red and white
            {"natural_zebra_palomino", "extension=e/e matp=Cr/N natural_zebra=Zeb/Zeb"},
            {"natural_zebra_shadow_bay", "agouti=A/a natural_zebra=Zeb/n"},     // one copy - shadow stripes

            // tiger eye - the coat is untouched; only the two iris texels move
            {"tiger_eye_amber_bay", "agouti=A/a tiger_eye=TE1/TE1"},
            {"tiger_eye_yellow_bay", "agouti=A/a tiger_eye=TE2/TE2"},

            // the middle of the bay shade range, and the shade locus on a horse
            // with no black for it to move
            {"bay_liver", "agouti=A/A shade=Sh/ShD"},           // one dark copy - mahogany
            {"bay_seal_het_agouti", "agouti=A/a shade=ShD/ShD"},// the darkest a bay gets
            {"chestnut_dark_shade", "extension=e/e shade=ShD/ShD"},
    };

    /**
     * One epigenetic seed per sample, chosen to show the spread rather than a
     * single draw: the three greys are the same {@code G/g} at three stages of
     * greying, and the two zebras the same {@code Mzeb/n} at two different
     * stripe reaches.
     *
     * <p>The bays <b>used to be</b> one genotype at three seeds, back when the
     * point extent was a bare epigenetic roll. It is a shade score now, so they
     * are three genotypes instead - which is the point of the change, and also
     * the reason a sample list is a poor place to keep a claim about how a gene
     * works.
     *
     * <p>Positional, and {@link #main} refuses to run if the two arrays have
     * drifted apart: they had, by two entries, which silently handed every
     * sample after the gap somebody else's seed.
     */
    private static final long[] SEEDS = {0, 0, 7, 3, 0, 0, 0, 0, 0, 0, 0, 1, 3, 21, 3, 3, 0, 0, 31, 0,
            5, 11, 5, 5, 0, 0, 0, 11,
            0, 0, 0, 7, 0, 0, 0, 0, 4, 9, 2, 6,
            3, 0,
            2, 5, 8, 8, 8, 4, 6,
            3, 3, 3,
            4, 19, 6, 6, 2, 8, 12,
            9, 2, 9,
            0, 0, 0, 0, 0,
            0, 0, 0,
            1, 7, 3, 5, 2, 9, 4, 6,
            // brindle x4, natural zebra x5, tiger eye x2, the three extra shades
            5, 2, 7, 3,
            1, 4, 6, 2, 8,
            0, 0,
            2};

    private CoatSampleTool() {}

    public static void main(String[] args) throws IOException {
        if (SEEDS.length != SAMPLES.length) {
            throw new IllegalStateException("SEEDS has " + SEEDS.length + " entries for "
                    + SAMPLES.length + " samples - the two arrays are positional, so a mismatch "
                    + "hands every sample after the gap the wrong horse's seed");
        }
        Path outDir = Path.of(args.length > 0 ? args[0] : "build/coat-samples");
        Files.createDirectories(outDir);

        int n = HorseSkinGeometry.SHEET_SIZE;
        int[] adultTemplate = readArgb("/assets/horsegenetics/textures/entity/horse/horse_white.png");
        int[] babyTemplate = readArgb("/assets/horsegenetics/textures/entity/horse/horse_white_baby.png");
        int[] g = readArgb("/assets/horsegenetics/textures/coat/redblackgradient.png");
        GradientLut base = new GradientLut(g, lastReadWidth, lastReadHeight);
        int[] bp = readArgb("/assets/horsegenetics/textures/coat/lutbluepink.png");
        GradientLut bluepink = new GradientLut(bp, lastReadWidth, lastReadHeight);
        LutSet lut = new LutSet(base, java.util.Map.of("bluepink", bluepink));

        for (int i = 0; i < SAMPLES.length; i++) {
            Genotype gt = build(SAMPLES[i][1]);
            Epigenome epi = Epigenome.fromSeed(SEEDS[i]);
            int[] adult = CoatTextureComposer.compose(gt, epi, Skin.ADULT, true, adultTemplate, lut);
            int[] foal = CoatTextureComposer.compose(gt, epi, Skin.BABY, false, babyTemplate, lut);
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
            if (idx < 0) {
                throw new IllegalArgumentException("no registered gene named '" + p[0] + "' in sample spec: " + spec);
            }
            // the code is gene-keyed, so a replaced segment carries its key too
            segs[idx] = order.get(idx).key() + "=" + p[1];
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
