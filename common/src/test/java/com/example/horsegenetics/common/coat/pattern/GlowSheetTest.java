package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>The glow sheet</b> - {@link CoatTextureComposer.Baked#glowSheet}, the fold
 * that turns "how brightly each texel glows" into the texture a renderer draws.
 *
 * <p>It used to live in the NeoForge texture factory. It is here because the
 * browser tools draw the same glow, and a second copy of "the level becomes the
 * alpha" would have been a copy free to drift - a horse glowing one way in game
 * and another in the designer, with nothing to catch it.
 *
 * <p>What these pin is the arithmetic, since the picture itself is in a render
 * nobody can assert on: the level lands in the <b>alpha</b> and the coat's own
 * colour lands in the RGB, a transparent texel is never lit, and "nothing
 * glows" comes back as {@code null} rather than as a sheet of zeroes - which is
 * what lets the renderer skip the whole pass.
 */
class GlowSheetTest {

    private static final int N = HorseSkinGeometry.SHEET_SIZE;

    private static final int TEAL = 0x40E0FF;

    /** An opaque coat of one colour, so a glowed texel's RGB is recognisable. */
    private static int[] flatCoat() {
        int[] argb = new int[N * N];
        java.util.Arrays.fill(argb, 0xFF000000 | TEAL);
        return argb;
    }

    private static Set<Part> parts(Part... p) {
        if (p.length == 0) {
            return Collections.emptySet();
        }
        Set<Part> out = new LinkedHashSet<>();
        Collections.addAll(out, p);
        return out;
    }

    /**
     * <b>The level becomes the alpha, and the coat's colour stays the colour.</b>
     * This is the whole contract. The emissive pass blends, so alpha 0.4 is four
     * tenths of the full-bright colour over six tenths of the texel as the world
     * lit it - a dimmer colour. Scaling the RGB instead would blend toward black,
     * and a faint glow would read as a smudge rather than a faint glow.
     */
    @Test
    void theLevelBecomesTheAlphaAndTheCoatKeepsItsColour() {
        float[] emissive = new float[N * N];
        emissive[7 * N + 7] = 0.4f;

        int[] sheet = new CoatTextureComposer.Baked(flatCoat(), emissive)
                .glowSheet(Skin.ADULT, parts());

        assertNotNull(sheet, "a horse with a lit texel should produce a sheet");
        int lit = sheet[7 * N + 7];
        assertEquals(102, lit >>> 24, "0.4 of full bright should arrive as alpha 102");
        assertEquals(TEAL, lit & 0xFFFFFF,
                "the glow has no colour of its own - it is the coat's colour at that texel");
    }

    /** Everything the genes did not light is transparent, not black. */
    @Test
    void everythingElseIsLeftTransparent() {
        float[] emissive = new float[N * N];
        emissive[7 * N + 7] = 1.0f;

        int[] sheet = new CoatTextureComposer.Baked(flatCoat(), emissive)
                .glowSheet(Skin.ADULT, parts());

        assertEquals(0, sheet[0], "an unlit texel must be fully transparent");
        assertEquals(255, sheet[7 * N + 7] >>> 24);
    }

    /**
     * A texel the coat never painted cannot glow. Otherwise a gene lighting a
     * region wider than the horse would hang light in the empty margin of the
     * sheet, which on a model reads as a floating scrap.
     */
    @Test
    void aTransparentTexelIsNeverLit() {
        int[] argb = new int[N * N];   // all zero - nothing painted anywhere
        float[] emissive = new float[N * N];
        emissive[7 * N + 7] = 1.0f;

        assertNull(new CoatTextureComposer.Baked(argb, emissive).glowSheet(Skin.ADULT, parts()),
                "lighting only transparent texels should light nothing at all");
    }

    /**
     * <b>Nothing glows -> null, not a sheet of zeroes.</b> The ordinary case, and
     * the reason it is worth distinguishing: the renderer skips the entire
     * emissive pass rather than uploading and blending 16 384 transparent texels
     * for every horse in the world.
     */
    @Test
    void anOrdinaryHorseProducesNoSheetAtAll() {
        assertNull(new CoatTextureComposer.Baked(flatCoat(), null).glowSheet(Skin.ADULT, parts()),
                "a horse with no emissive mask and no glow effect should glow nothing");
    }

    /**
     * A whole part named by a {@code glow} effect lights outright: the effect
     * carries no intensity, and full bright is what "this part glows" has always
     * meant. This is the path the browser would have missed if the lit-part set
     * had been left behind in the renderer.
     */
    @Test
    void aGlowEffectLightsAWholePartFully() {
        int[] sheet = new CoatTextureComposer.Baked(flatCoat(), null)
                .glowSheet(Skin.ADULT, parts(Part.MANE));

        assertNotNull(sheet, "a glow effect alone should still produce a sheet");
        int litTexels = 0;
        for (int px : sheet) {
            if ((px >>> 24) != 0) {
                assertEquals(255, px >>> 24, "a part glow is full bright, never a fraction");
                litTexels++;
            }
        }
        assertTrue(litTexels > 0, "the mane did not light");
    }

    /**
     * The two sources fold together rather than one replacing the other - a gene
     * may light a speckle at half and a {@code glow} effect light the mane
     * outright on the same horse.
     */
    @Test
    void bothSourcesLandOnTheOneSheet() {
        float[] emissive = new float[N * N];
        emissive[7 * N + 7] = 0.5f;

        int[] sheet = new CoatTextureComposer.Baked(flatCoat(), emissive)
                .glowSheet(Skin.ADULT, parts(Part.MANE));

        assertNotNull(sheet);
        assertEquals(128, sheet[7 * N + 7] >>> 24, "the gene's own texel kept its level");
        boolean maneLit = false;
        for (int px : sheet) {
            if ((px >>> 24) == 255) {
                maneLit = true;
                break;
            }
        }
        assertTrue(maneLit, "the effect's whole part lit alongside it");
    }

    /**
     * A part this skin has not got lights nothing - a mane glow on a foal. It
     * comes back null rather than as an empty sheet, so the foal costs no
     * texture at all.
     */
    @Test
    void aPartTheSkinHasNotGotLightsNothing() {
        assertNull(new CoatTextureComposer.Baked(flatCoat(), null).glowSheet(Skin.BABY, parts(Part.MANE)),
                "a foal has no mane, so a mane glow should produce no sheet");
    }
}
