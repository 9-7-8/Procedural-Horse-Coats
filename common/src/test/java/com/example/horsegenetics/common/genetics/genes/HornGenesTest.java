package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.genetics.EditorRules;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneFamily;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.GrownParts;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;
import com.example.horsegenetics.common.genetics.spec.HorseAbilities;
import com.example.horsegenetics.common.parts.AttachedPart;
import com.example.horsegenetics.common.testutil.Codes;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The three loci that finish a unicorn's horn - colour, glow and dust - and the
 * rule they share: each does nothing at all to a horse without a horn.
 */
class HornGenesTest {

    private static final List<Gene> FINISHERS =
            List.of(Genes.HORN_COLOUR, Genes.HORN_GLOW, Genes.HORN_DUST);

    /** A unicorn, plus whatever else is asked for. */
    private static Genotype unicorn(String... geneThenPair) {
        String[] all = new String[geneThenPair.length + 2];
        all[0] = "unicorn_horn";
        all[1] = "Horn/Horn";
        System.arraycopy(geneThenPair, 0, all, 2, geneThenPair.length);
        return Genotype.parse(Codes.of(all));
    }

    private static AttachedPart horn(Genotype genotype, long seed) {
        List<AttachedPart> parts = GrownParts.of(genotype, Epigenome.fromSeed(seed));
        assertEquals(1, parts.size());
        return parts.get(0);
    }

    private static int r(int argb) { return argb >> 16 & 0xFF; }
    private static int g(int argb) { return argb >> 8 & 0xFF; }
    private static int b(int argb) { return argb & 0xFF; }

    @Test
    void theyAreMagicalPaintNothingAndLiveBesideTheHorn() {
        for (Gene gene : FINISHERS) {
            assertFalse(gene.isNatural(), gene.key());
            assertFalse(gene.affectsCoat(), gene.key() + " must not reach the coat texture");
            assertSame(GeneFamily.MAGIC_PARTS, GeneFamily.of(gene), gene.key());
            assertFalse(gene.description().isBlank(), gene.key());
        }
    }

    /** Nothing about a hornless horse changes, whatever it carries. */
    @Test
    void withoutAHornTheyDoNothing() {
        Genotype hornless = Genotype.parse(Codes.of(
                "horn_colour", "Red/Blu", "horn_glow", "Glw/Glw", "horn_dust", "Dst/Dst"));
        assertTrue(GrownParts.of(hornless, Epigenome.fromSeed(3)).isEmpty());
        assertTrue(HorseAbilities.activeFor(hornless, Epigenome.fromSeed(3)).stream()
                .noneMatch(a -> a.geneKey().equals(HornDustGene.KEY)));
    }

    @Test
    void aWildTypeUnicornHasAPlainWhiteHorn() {
        AttachedPart horn = horn(unicorn(), 1);
        assertEquals(0xFFFFFFFF, horn.baseTint());
        assertFalse(horn.twoTone());
        assertFalse(horn.emissive());
    }

    @Test
    void eachColourLooksLikeItsName() {
        for (long seed = 0; seed < 200; seed++) {
            int red = horn(unicorn("horn_colour", "Red/Red"), seed).baseTint();
            assertTrue(r(red) > g(red) + 60 && r(red) > b(red) + 60, "not red: " + Integer.toHexString(red));
            int blue = horn(unicorn("horn_colour", "Blu/Blu"), seed).baseTint();
            assertTrue(b(blue) > r(blue) + 60, "not blue: " + Integer.toHexString(blue));
            int green = horn(unicorn("horn_colour", "Grn/Grn"), seed).baseTint();
            assertTrue(g(green) > r(green) + 40 && g(green) > b(green) + 40, "not green: " + Integer.toHexString(green));
            int black = horn(unicorn("horn_colour", "Blk/Blk"), seed).baseTint();
            assertTrue(r(black) < 60 && r(black) == g(black) && g(black) == b(black), "not black: " + Integer.toHexString(black));
            int white = horn(unicorn("horn_colour", "Wht/Wht"), seed).baseTint();
            assertTrue(r(white) > 235 && r(white) == b(white), "not white: " + Integer.toHexString(white));
        }
    }

    /**
     * Two copies of the same colour are still two copies: each drifts on its own,
     * so red horns are all red and not all the same red.
     */
    @Test
    void everyRedIsItsOwnRed() {
        Set<Integer> reds = new HashSet<>();
        for (long seed = 0; seed < 300; seed++) {
            reds.add(horn(unicorn("horn_colour", "Red/Red"), seed).baseTint());
        }
        assertTrue(reds.size() > 100, "shade drift is not being read: " + reds.size());
    }

    @Test
    void chaosHornsDoNotMatch() {
        Set<Integer> colours = new HashSet<>();
        Set<Integer> hueSextants = new HashSet<>();
        for (long seed = 0; seed < 300; seed++) {
            int c = horn(unicorn("horn_colour", "Cha/Cha"), seed).baseTint();
            colours.add(c);
            float[] hsb = java.awt.Color.RGBtoHSB(r(c), g(c), b(c), null);
            hueSextants.add((int) (hsb[0] * 6));
        }
        assertTrue(colours.size() > 250, "chaos is repeating itself: " + colours.size());
        assertEquals(6, hueSextants.size(), "chaos does not reach the whole hue circle");
    }

    /** Two different colours: the base is the one earlier in the list, whichever parent gave it. */
    @Test
    void twoDifferentColoursMakeATwoToneHornTheSameWayRound() {
        AttachedPart whiteRed = horn(unicorn("horn_colour", "Wht/Red"), 4);
        assertTrue(whiteRed.twoTone());
        assertTrue(r(whiteRed.baseTint()) > 235 && g(whiteRed.baseTint()) > 235, "the base should be white");
        assertTrue(r(whiteRed.tipTint()) > g(whiteRed.tipTint()) + 60, "the tip should be red");

        AttachedPart redBlue = horn(unicorn("horn_colour", "Blu/Red"), 4);
        assertTrue(r(redBlue.baseTint()) > b(redBlue.baseTint()), "red comes before blue, so it is the base");
        assertTrue(b(redBlue.tipTint()) > r(redBlue.tipTint()));
        assertEquals(redBlue, horn(unicorn("horn_colour", "Red/Blu"), 4),
                "written either way round it is the same horse");

        assertEquals(redBlue.baseTint(), redBlue.tintAt(0f));
        assertEquals(redBlue.tipTint(), redBlue.tintAt(1f));
        assertNotEquals(redBlue.baseTint(), redBlue.tintAt(0.5f), "it fades rather than cutting");
        assertEquals(0xFF, redBlue.tintAt(0.5f) >>> 24, "a blend must stay opaque");
    }

    @Test
    void glowNeedsTwoCopiesAndAnyColourGlows() {
        assertFalse(horn(unicorn("horn_glow", "Glw/n"), 2).emissive());
        assertTrue(horn(unicorn("horn_glow", "Glw/Glw"), 2).emissive());
        AttachedPart blackGlow = horn(unicorn("horn_glow", "Glw/Glw", "horn_colour", "Blk/Blk"), 2);
        assertTrue(blackGlow.emissive(), "glow is regardless of colour");
        assertTrue(r(blackGlow.baseTint()) < 60);
    }

    /** The dust asks the horn for its colours, so it can never disagree with what is drawn. */
    @Test
    void dustFallsFromTheHornInTheHornsColours() {
        Epigenome epi = Epigenome.fromSeed(8);
        Genotype gt = unicorn("horn_dust", "Dst/Dst", "horn_colour", "Pnk/Cha");
        AttachedPart horn = GrownParts.of(gt, epi).get(0);
        List<GeneAbility.Emitter> emitters = HorseAbilities.activeFor(gt, epi).stream()
                .filter(a -> a.geneKey().equals(HornDustGene.KEY))
                .map(a -> (GeneAbility.Emitter) a.ability())
                .toList();
        assertEquals(1, emitters.size());
        GeneAbility.Emitter dust = emitters.get(0);
        assertEquals(HornDustGene.PARTICLE, dust.particle());
        assertEquals(HornDustGene.ANCHOR, dust.anchor());
        assertEquals(horn.baseTint() & 0xFFFFFF, dust.color());
        assertEquals(horn.tipTint() & 0xFFFFFF, dust.color2());

        Genotype carrier = unicorn("horn_dust", "Dst/n");
        assertTrue(HorseAbilities.activeFor(carrier, epi).stream()
                .noneMatch(a -> a.geneKey().equals(HornDustGene.KEY)));
    }

    /**
     * The editors' randomize moves what you can see. Colour and glow change the
     * horn, so they are on the visible side; dust is an ability, like rainbow dust,
     * and stays behind "Rnd health" with the others.
     */
    @Test
    void theEditorsSeeColourAndGlow() {
        assertTrue(EditorRules.changesLooks(Genes.HORN_COLOUR));
        assertTrue(EditorRules.changesLooks(Genes.HORN_GLOW));
        assertFalse(GrownParts.shapes(Genes.HORN_DUST));
    }

    /** A recessive whose carrier is invisible puts its expressing combination in the wild, never a carrier. */
    @Test
    void wildHorsesCarryGlowAndDustWholeOrNotAtAll() {
        SeededRng rng = new SeededRng(4242L);
        int glowing = 0;
        int n = 20_000;
        for (int i = 0; i < n; i++) {
            Genotype gt = Genotype.random(rng);
            int glow = gt.pair(Genes.HORN_GLOW).count(Genes.HORN_GLOW.Glw);
            int dust = gt.pair(Genes.HORN_DUST).count(Genes.HORN_DUST.Dst);
            assertTrue(glow != 1 && dust != 1, "a wild carrier: " + gt.toCode());
            if (glow == 2) {
                glowing++;
            }
        }
        double rate = 100.0 * glowing / n;
        assertTrue(Math.abs(rate - HornGlowGene.WILD_HOMOZYGOUS_PERCENT) < 2.0, "glow founders at " + rate + "%");
    }
}
