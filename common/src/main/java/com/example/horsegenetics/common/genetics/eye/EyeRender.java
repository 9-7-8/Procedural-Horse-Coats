package com.example.horsegenetics.common.genetics.eye;

/**
 * <b>One finished eye</b> - everything the painter needs about it, with no
 * genes left in it.
 *
 * <p>Six loci feed one of these (twelve for the pair, and the third eye copies
 * or invents a seventh set), and the point of the record is that resolving them
 * and painting them are separate jobs: {@link Eyes#resolve} answers what the
 * eye <i>is</i>, entirely from the genotype and the epigenome, and the coat
 * composer then draws it. Anything that wants to <i>ask</i> what colour a
 * horse's eyes are - the info panel, the designer, a breeding preview - reads
 * this and never touches the coat pipeline.
 *
 * <h2>The paint order is the field order</h2>
 * <ol>
 *   <li>{@link #sclera} over the light texels, unless it is
 *       {@link EyeHue#INVISIBLE};</li>
 *   <li>{@link #iris} over the dark texels, unless it is
 *       {@link EyeHue#INVISIBLE} - and an invisible iris takes the
 *       {@link #sector} with it, because a sector of an iris that is not there
 *       is not there either;</li>
 *   <li>{@link #sectorInk} over {@link #sector}'s quadrants of the iris;</li>
 *   <li>the two glows, which colour nothing and only mark texels full-bright.</li>
 * </ol>
 *
 * @param iris       the whole iris
 * @param sector     which part of it the second colour covers; {@link EyeSector#WILD}
 *                   for the ordinary one-colour eye
 * @param sectorInk  that second colour - meaningless, and never read, when
 *                   {@code sector} is wild
 * @param glowIris   the iris renders full-bright, in whatever colour it ended up
 * @param sclera     the white of the eye
 * @param glowSclera the sclera renders full-bright
 */
public record EyeRender(EyeInk iris, EyeSector sector, EyeInk sectorInk, boolean glowIris,
                        EyeInk sclera, boolean glowSclera) {

    /** The eye an ordinary horse has: brown iris, white sclera, nothing else. */
    public static final EyeRender WILD = new EyeRender(
            EyeInk.of(EyeHue.BROWN), EyeSector.WILD, EyeInk.of(EyeHue.MID_BLUE), false,
            EyeInk.of(EyeHue.WHITE), false);

    /** Is there a second colour in this iris at all? */
    public boolean sectoral() {
        return !sector.empty() && iris.paints() && sectorInk.paints();
    }

    /**
     * Does this eye differ from horse to horse - i.e. must a horse showing it
     * get a texture of its own rather than sharing a cached one? True as soon
     * as any part of it is {@link EyeHue#CHAOS}.
     */
    public boolean varies() {
        return iris.varies() || sclera.varies() || (sectoral() && sectorInk.varies());
    }

    // There is deliberately no "is this the wild type, so skip painting" helper.
    // The wild type is a BROWN iris on a template whose eye is pure black, so
    // the ordinary horse is precisely the case that must still be painted.
}
