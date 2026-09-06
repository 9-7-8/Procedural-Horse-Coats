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
 * <h2>Rank</h2>
 * A horse has two eyes and one iris colour, so several genes claiming it need an
 * order rather than a blend - the same argument that makes the LUT and the cutie
 * mark single-owner channels. Higher rank wins; ties go to the earlier gene in
 * {@link Genes#codeOrder()}.
 *
 * <p>The one ordering that matters is settled here: <b>blue beats amber</b>.
 * A blue eye is an iris with no pigment in it at all, and an absence cannot be
 * overpainted by a pigment gene - so a splashed white horse that also carries
 * tiger eye has blue eyes, not amber ones.
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
     * A pigment gene changing what colour the iris is - tiger eye's amber. The
     * ordinary rank.
     */
    public static final int RANK_PIGMENT = 10;

    /**
     * A white-spotting gene taking the pigment out of the iris altogether. Above
     * {@link #RANK_PIGMENT}, because there is nothing left for a pigment gene to
     * recolour.
     */
    public static final int RANK_DEPIGMENTED = 20;

    /**
     * The blue of a splashed white or dominant white horse's eye. Pale and cool,
     * and deliberately not saturated: at two texels a bright cyan stops reading
     * as an eye.
     */
    public static final EyeColor BLUE = new EyeColor("blue", "Blue",
            0x6FA8D8, RANK_DEPIGMENTED, 1.0);

    public EyeColor {
        if (strength < 0.0 || strength > 1.0) {
            throw new IllegalArgumentException("eye-colour strength must be in [0,1], got " + strength);
        }
    }

    /** A pigment claim at {@link #RANK_PIGMENT}, fully replacing the iris. */
    public static EyeColor pigment(String id, String name, int rgb) {
        return new EyeColor(id, name, rgb, RANK_PIGMENT, 1.0);
    }

    /** Does {@code other} out-rank this claim? */
    public boolean losesTo(EyeColor other) {
        return other != null && other.rank > this.rank;
    }
}
