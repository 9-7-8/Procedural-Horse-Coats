package com.example.horsegenetics.common.genetics;

/**
 * <b>An iris colour a gene is claiming</b> for a horse, and the rank it claims
 * it at.
 *
 * <h2>The iris is the dark texels, not the sclera</h2>
 * On the coat sheet a horse's eye is a small block of <b>pure black</b> - the
 * iris and pupil together, there being no room for both at 128px - beside a
 * block of near-white sclera. Eye colour therefore means <b>colouring the black
 * texels</b> and leaving the white ones white, which is what
 * {@link com.example.horsegenetics.common.coat.pattern.CoatOverlay#tintIris}
 * does.
 *
 * <p>That is worth stating because the obvious tool is the wrong one.
 * {@code shadeToward} scales its target by the texel's own brightness so that a
 * gold hoof keeps the template's shading - which on an eye means the <i>sclera</i>
 * takes the colour and the iris, being black, comes out black. It is the right
 * blend for {@code light}'s glowing gold eye and the leopard complex's white
 * sclera rim, and exactly backwards for an iris.
 *
 * <h2>Rank is the biological route, not a priority number</h2>
 * A horse has two eyes and one iris colour, so several genes claiming it need an
 * order rather than a blend - the same argument that makes the LUT and the cutie
 * mark single-owner channels. Higher rank wins; ties go to the earlier gene in
 * {@link Genes#codeOrder()}.
 *
 * <p>The three ranks are the <b>three ways a real horse's eye changes colour</b>,
 * and they nest for a reason:
 * <ol>
 *   <li>{@link #RANK_DILUTION} - a whole-body pigment dilution that happens to
 *       include the iris ({@code SLC45A2} cream / pearl, {@code SLC36A1}
 *       champagne). The pigment cells are all there; they just handle less
 *       melanin.</li>
 *   <li>{@link #RANK_PIGMENT} - an <b>iris-specific</b> pigment change
 *       ({@code SLC24A5} tiger eye), which alters the iris and nothing else.
 *       It beats a whole-body dilution because it is aimed at the eye.</li>
 *   <li>{@link #RANK_DEPIGMENTED} - the iris never got enough melanocytes at
 *       all ({@code MITF} / {@code PAX3} splash, {@code EDNRB} frame,
 *       {@code KIT} dominant white). It beats both, because an absence cannot
 *       be overpainted: a splashed white horse that also carries tiger eye has
 *       blue eyes, not amber ones.</li>
 * </ol>
 *
 * <p>Only the depigmented rank needs to reach the whole of both irises, and it
 * frequently does not - see {@link EyeSpread}, which is where one blue eye and
 * the blue wedge in a brown one come from.
 *
 * @param id       stable slug, unique across genes - {@code "amber"}, {@code "blue"}
 * @param name     display name for the info panel and the gene pages
 * @param rgb      the colour the iris texels are walked toward, {@code 0xRRGGBB}
 * @param rank     see above; use the constants
 * @param strength how far toward {@code rgb} to walk, {@code [0,1]}; 1 replaces
 *                 the iris outright
 */
public record EyeColor(String id, String name, int rgb, int rank, double strength) {

    /**
     * A dilution of the horse's <i>whole</i> pigment that the iris is caught up
     * in - cream, pearl, champagne. The lowest rank: it is the least specific
     * claim anything can make on an eye.
     */
    public static final int RANK_DILUTION = 10;

    /**
     * A gene changing what colour the iris <i>is</i>, and only the iris - tiger
     * eye. Above {@link #RANK_DILUTION}: a gene aimed at the eye beats one that
     * reached it on the way past.
     */
    public static final int RANK_PIGMENT = 20;

    /**
     * A white-spotting gene taking the pigment out of the iris altogether. Above
     * {@link #RANK_PIGMENT}, because there is nothing left for a pigment gene to
     * recolour.
     */
    public static final int RANK_DEPIGMENTED = 30;

    /**
     * The blue of a splashed white or dominant white horse's eye. Pale and cool,
     * and deliberately not saturated: at two texels a bright cyan stops reading
     * as an eye.
     *
     * <p>The one {@code EyeColor} that is a shared constant rather than a
     * gene's own, because four loci claim the identical colour for the identical
     * reason. Every other iris colour is declared by the gene that owns it.
     */
    public static final EyeColor BLUE = new EyeColor("blue", "Blue",
            0x6FA8D8, RANK_DEPIGMENTED, 1.0);

    public EyeColor {
        if (strength < 0.0 || strength > 1.0) {
            throw new IllegalArgumentException("eye-colour strength must be in [0,1], got " + strength);
        }
    }

    /** A whole-body-dilution claim at {@link #RANK_DILUTION}, fully replacing the iris. */
    public static EyeColor dilution(String id, String name, int rgb) {
        return new EyeColor(id, name, rgb, RANK_DILUTION, 1.0);
    }

    /** An iris-specific claim at {@link #RANK_PIGMENT}, fully replacing the iris. */
    public static EyeColor pigment(String id, String name, int rgb) {
        return new EyeColor(id, name, rgb, RANK_PIGMENT, 1.0);
    }

    /** Is this claim an absence of pigment rather than a colour of it? */
    public boolean depigmented() {
        return rank >= RANK_DEPIGMENTED;
    }

    /** Does {@code other} out-rank this claim? */
    public boolean losesTo(EyeColor other) {
        return other != null && other.rank > this.rank;
    }
}
