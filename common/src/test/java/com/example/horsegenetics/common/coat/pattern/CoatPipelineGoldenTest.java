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
            // the bay shade range, both ends and both dosage terms
            override("agouti=A/a", "shade=ShL/ShL"),
            override("agouti=A/A", "shade=ShL/ShL"),
            override("agouti=A/a", "shade=ShD/ShD"),
            override("agouti=A/A", "shade=ShD/ShD"),
            override("agouti=A/a", "shade=Sh/ShD", "extension=E/e"),
            override("extension=e/e", "shade=ShD/ShD"),   // silent on a chestnut
            // dhampir: the carrier is the eyes alone, the homozygote is white
            override("agouti=A/a", "dhampir=Dhmp/n"),
            override("dhampir=Dhmp/Dhmp"),
            override("agouti=A/a", "dhampir=Dhmp/Dhmp"),
            override("kit=W22/N", "dhampir=Dhmp/n"),      // red must beat the depigmented blue
            override("kit=W4/N"),                        // Camarillo white, one copy
            override("agouti=A/a", "kit=W4/W4"),         // ...and the viable homozygote
            // flaxen: a chestnut long-hair dosage, silent on anything else
            override("extension=e/e", "flaxen=Fl1/f"),
            override("extension=e/e", "flaxen=Fl1/Fl1"),
            override("extension=e/e", "flaxen=Fl2/Fl2"),
            override("extension=e/e", "flaxen=Fl2/Fl2", "matp=Cr/N"),   // a flaxen palomino
            override("agouti=A/a", "flaxen=Fl2/Fl2"),                   // carried, invisible
            // sooty: the same dosage over four bases, and a no-op on the fifth
            override("agouti=A/a", "sooty=S1/s"),
            override("agouti=A/a", "sooty=S2/S2"),
            override("agouti=A/a", "matp=Cr/N", "sooty=S2/S2"),
            override("extension=e/e", "matp=Cr/N", "sooty=S2/S2"),
            override("extension=e/e", "sooty=S2/S2"),
            override("sooty=S2/S2"),                                     // a plain black - unchanged
            override("agouti=A/a", "dun=D/d2", "sooty=S2/S2"),
            // pangare: sooty inverted, and the black points it must spare
            override("agouti=A/a", "pangare=Pa1/pa"),
            override("agouti=A/a", "pangare=Pa2/Pa2"),
            override("extension=e/e", "pangare=Pa2/Pa2"),
            override("pangare=Pa2/Pa2"),                                 // a black - unchanged
            override("agouti=A/a", "sooty=S2/S2", "pangare=Pa2/Pa2"),    // both halves at once
            override("agouti=A/A", "shade=ShD/ShD", "extension=E/E"),    // seal - the inner limb
            // rabicano: tail and flank first, and not classic roan
            override("agouti=A/a", "rabicano=Rb/rb"),
            override("agouti=A/a", "rabicano=Rb/Rb"),
            override("extension=e/e", "rabicano=Rb/Rb"),
            override("agouti=A/a", "roan=Rn/rn", "rabicano=Rb/Rb"),
            // manchado: a dorsal white field with base-colour islands in it
            override("agouti=A/a", "manchado=ma/ma"),
            override("manchado=ma/ma"),
            override("extension=e/e", "manchado=ma/ma"),
            override("agouti=A/a", "manchado=ma/Ma"),                    // a silent carrier
            // sunshine and snowdrop: two more MATP recessives and their look-alikes
            override("agouti=A/a", "matp=sun/sun"),
            override("extension=e/e", "matp=sun/sun"),
            override("agouti=A/a", "matp=sno/sno"),
            override("agouti=A/a", "matp=Cr/sun"),
            override("extension=e/e", "matp=Cr/sno"),
            override("agouti=A/a", "matp=prl/sun"),
            override("agouti=A/a", "matp=sun/N"),
            override("kit=W22/N"),
            override("champagne=Ch/c"),
            override("agouti=A/a", "champagne=Ch/c"),
            override("mitf=SW1/N"),
            override("agouti=A/a", "mitf=SW1/N"),
            override("grey=G3/N"),
            override("extension=e/e", "grey=G3/N"),
            override("agouti=A/a", "grey=G2/N"),          // dosage 1 - the slowest grey
            override("agouti=A/a", "grey=G2/G2"),         // dosage 2 the other way round
            override("agouti=A/a", "grey=G2/G3"),         // dosage 3
            override("agouti=A/a", "grey=G3/G3"),         // dosage 4 - near white
            override("extension=e/e", "grey=G3/G3"),
            override("agouti=A/a", "matp=Cr/N"),
            override("agouti=A/a", "matp=Cr/Cr"),
            override("agouti=A/a", "matp=prl/prl"),
            override("agouti=A/a", "matp=Cr/prl"),
            override("extension=e/e", "grey=G3/N", "matp=Cr/Cr"),
            override("agouti=A/a", "champagne=Ch/c", "mitf=SW1/N", "grey=G3/N", "matp=Cr/N"),
            override("magic_zebra=Mzeb/n"),
            override("magic_zebra=Mzeb/Mzeb"),
            override("natural_zebra=Zeb/n"),
            override("natural_zebra=Zeb/Zeb"),
            override("agouti=A/a", "natural_zebra=Zeb/Zeb"),
            override("extension=e/e", "natural_zebra=Zeb/Zeb"),
            override("natural_zebra=Zeb/Zeb", "magic_zebra=Mzeb/n"),
            override("agouti=A/a", "magic_zebra=Mzeb/n"),
            override("kit=W22/N", "magic_zebra=Mzeb/n"),
            override("pink_hair=Pihr/Pihr"),
            override("pink_hair=n/Pihr"),
            override("extension=e/e", "pink_hair=Pihr/Pihr"),
            override("kit=W22/N", "pink_hair=Pihr/Pihr"),
            override("agouti=A/a", "magic_zebra=Mzeb/n", "pink_hair=Pihr/Pihr"),
            override("magic_zebra=Mzeb/n", "pink_hair=Pihr/Pihr"),
            override("dun=D/d2"),
            override("agouti=A/a", "dun=D/d2"),
            override("dun=d1/d2"),
            override("agouti=A/a", "dun=d1/d1"),
            // the midtstol: a dark band down the mane and tail with pale guard
            // hair either side, on every base a dun can sit on
            override("extension=e/e", "dun=D/d2"),
            override("agouti=A/A", "shade=ShD/ShD", "dun=D/d2"),
            override("extension=e/e", "dun=d1/d2"),
            override("silver=Z/z"),
            override("agouti=A/a", "silver=Z/z"),
            override("extension=e/e", "mushroom=Mu/Mu"),
            override("roan=Rn/rn"),
            // the two roans, apart and on one horse - they are different
            // patterns and must not collapse into each other
            override("extension=e/e", "roan=Rn/rn"),
            override("agouti=A/a", "roan=Rn/rn", "leopard=LP/LP"),
            override("tobiano=To/to"),
            // EDNRB: the carrier, and the homozygous lethal white it can throw
            override("ednrb=O/N"),
            override("agouti=A/a", "ednrb=O/N"),
            override("ednrb=O/O"),
            // KIT, the whole ladder - every outcome the eight-allele locus has
            override("kit=W20/N"),
            override("kit=W20/W20"),
            override("kit=SB1/N"),
            override("agouti=A/a", "kit=SB1/N"),
            override("kit=SB1/W20"),
            override("kit=SB1/SB1"),
            override("kit=W23/SB1"),
            override("kit=W13/W10"),
            // the two splash loci, alone and stacked - the whole point of the split
            override("mitf=SW1/SW1"),
            override("mitf=SW3/N"),
            override("mitf=SW3/SW1"),
            override("pax3=SW2/N"),
            override("pax3=SW2/SW2"),
            override("agouti=A/a", "mitf=SW1/N", "pax3=SW2/N"),
            override("agouti=A/a", "kit=SB1/N", "tobiano=To/to", "ednrb=O/N"),
            // the magical utility genes that paint: hair colour, healer, light
            override("mane_color=Mnsld/n"),
            override("mane_color=Mnstrp/n"),
            override("mane_color=Mnsld/Mnstrp"),
            override("tail_color=Tlsld/n"),
            override("tail_color=Tlsld/Tlstrp"),
            override("mane_color=Mnsld/Mnstrp", "tail_color=Tlstrp/n"),
            override("mane_color=Mnsld/n", "magic_zebra=Mzeb/n"),
            override("healer=Hlr/Hlr"),
            override("extension=e/e", "healer=Hlr/Hlr"),
            override("mane_color=Mnsld/n", "healer=Hlr/Hlr"),
            override("light=Lthf/n"),
            override("light=Ltmn/n"),
            override("light=Lteye/n"),
            override("light=Lthf/Ltmn"),
            override("light=Ltmn/Lteye"),
            override("light=Lthf/Lteye"),
            override("kit=W22/N", "light=Ltmn/n"),
            override("agouti=A/a", "light=Lthf/Ltmn", "mane_color=Mnstrp/n"),
            // LUT: one copy is a wild-type carrier (natural gradient), two swap
            // phase-2 to the alternate LUT - so these two must hash differently
            override("lut=Blupnk/n"),
            override("lut=Blupnk/Blupnk"),
            override("agouti=A/a", "lut=Blupnk/Blupnk"),
            override("extension=e/e", "grey=G3/N", "lut=Blupnk/Blupnk"),
            // the leopard complex - each PATN combination is a different painter,
            // and LP zygosity flips leopard<->fewspot / blanket<->snowcap
            override("agouti=A/a", "leopard=LP/lp"),
            override("agouti=A/a", "leopard=LP/LP"),
            override("agouti=A/a", "leopard=LP/lp", "patn1=PATN1/n"),
            override("agouti=A/a", "leopard=LP/LP", "patn1=PATN1/n"),
            override("agouti=A/a", "leopard=LP/lp", "patn2=PATN2/n"),
            override("agouti=A/a", "leopard=LP/LP", "patn2=PATN2/n"),
            override("agouti=A/a", "leopard=LP/lp", "patn1=PATN1/n", "patn2=PATN2/n"),
            override("extension=e/e", "leopard=LP/lp", "patn1=PATN1/n"),
            // PATN with no LP paints nothing - must hash identical to plain
            override("agouti=A/a", "patn1=PATN1/PATN1", "patn2=PATN2/n"),

            // brindle - the X-linked locus. Both the hemizygous stallion form
            // and the homozygous mare form, and the carrier mare that must be
            // byte-identical to a plain horse.
            override("agouti=A/a", "brindle=Brn/Y", "sex=X/Y"),
            override("agouti=A/a", "brindle=Brn/Brn"),
            override("agouti=A/a", "brindle=Brn/n"),
            override("extension=e/e", "brindle=Brn/Brn"),

            // tiger eye - paints nothing but the iris, so these pin that the
            // coat is unmoved and the eye is not.
            override("agouti=A/a", "tiger_eye=TE1/TE1"),
            override("agouti=A/a", "tiger_eye=TE2/TE2"),
            override("agouti=A/a", "tiger_eye=TE1/N"),
            // blue beats amber
            override("agouti=A/a", "tiger_eye=TE1/TE1", "mitf=SW1/N"),

            // the dilutions' irises - cream blue, the compound heterozygote's
            // blue-green, pearl's pale eye, and champagne's epigenetic shade
            // (three seeds, so more than one of its four shades is pinned)
            override("agouti=A/a", "matp=Cr/Cr", "tiger_eye=TE1/TE1"), // iris-specific beats dilution
            override("extension=e/e", "champagne=Ch/Ch"),

            // heterochromia. The spread is rolled off the winning white locus's
            // seed, so the three seeds below pin three different answers to
            // "how much of each iris did the blue actually reach".
            override("agouti=A/a", "mitf=SW1/N", "champagne=Ch/c"),
            override("agouti=A/a", "kit=W22/N", "tiger_eye=TE2/TE2"),

            // magic sectoral heterochromia - the expressing heterozygote, the
            // chaos allele, and the two combinations that must paint nothing
            override("agouti=A/a", "magic_sectoral_heterochromia=green/gold"),
            override("agouti=A/a", "magic_sectoral_heterochromia=blue/chaos"),
            override("agouti=A/a", "magic_sectoral_heterochromia=chaos/chaos"),
            override("agouti=A/a", "magic_sectoral_heterochromia=green/n"),
            override("agouti=A/a", "mitf=SW1/N", "magic_sectoral_heterochromia=hazel/brown"));

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
        LutSet luts = new LutSet(lut(), java.util.Map.of("bluepink", altLut()));
        int[] adultTemplate = template(Skin.ADULT);
        int[] foalTemplate = template(Skin.BABY);
        StringBuilder sb = new StringBuilder();
        for (String code : CODES) {
            for (long seed : SEEDS) {
                Genotype gt = Genotype.parse(code);
                Epigenome epi = Epigenome.fromSeed(seed);
                sb.append(code).append(' ').append(seed).append(" adult ")
                        .append(sha256(CoatTextureComposer.compose(gt, epi, Skin.ADULT, true, adultTemplate, luts)))
                        .append('\n');
                sb.append(code).append(' ').append(seed).append(" foal  ")
                        .append(sha256(CoatTextureComposer.compose(gt, epi, Skin.BABY, false, foalTemplate, luts)))
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

    /**
     * The wild-type code with the named genes' segments replaced.
     *
     * <p><b>An unknown gene name is a hard error</b>, and that is the whole
     * point. This used to leave the segment alone and say nothing, so a case
     * naming a gene that had since been renamed or retired quietly became a
     * duplicate of its own base colour - five cases were pinning nothing after
     * cream and pearl merged into MATP, and nobody could have noticed.
     */
    private static String override(String... kv) {
        String[] segs = Genotype.wildType().toCode().split("-");
        List<Gene> order = Genes.codeOrder();
        for (String entry : kv) {
            String[] p = entry.split("=");
            boolean hit = false;
            for (int i = 0; i < order.size(); i++) {
                if (order.get(i).key().endsWith("." + p[0])) {
                    segs[i] = order.get(i).key() + "=" + p[1];
                    hit = true;
                }
            }
            if (!hit) {
                throw new IllegalArgumentException("no gene named '" + p[0]
                        + "' - a golden case naming a retired gene pins nothing");
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

    /**
     * A synthetic <i>alternate</i> LUT for the {@code LUT} locus - same layout
     * (left redder, bottom blacker) but cool blues and pinks, so a
     * {@code Blupnk/Blupnk} horse hashes differently from its natural-gradient
     * carrier.
     */
    private static GradientLut altLut() {
        int s = 16;
        int[] a = new int[s * s];
        int white = 0xFFF2ECF6, pink = 0xFFD86AA8, blueBlack = 0xFF10122A;
        for (int y = 0; y < s; y++) {
            for (int x = 0; x < s; x++) {
                float redLevel = 1f - x / (float) (s - 1);
                float blackLevel = y / (float) (s - 1);
                a[y * s + x] = lerp(lerp(white, pink, redLevel), blueBlack, blackLevel);
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
