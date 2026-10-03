package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;

import java.util.Map;
import java.util.Optional;

/**
 * <b>A capability a {@link Gene} may additionally implement</b>: "this horse's
 * coat is laid onto a whole sheet of art" - the two undeath loci, whose look is
 * vanilla's own zombie and skeleton horses (undead treatment D27, D38, Appendix F).
 *
 * <p>Not a painting gene: a sheet is not a delta and not a pigment restriction,
 * and both passes have to run where no painting gene can.
 * <ul>
 *   <li>{@link Pass#SEED} stamps the sheet into the colour field <b>before the
 *       magical phase</b>, replacing what the natural genes resolved - the zombie,
 *       whose green-grey is no pigment a horse has. Magical genes then paint over
 *       it as over any coat.</li>
 *   <li>{@link Pass#SHAPE} runs <b>after the overlay phase</b> - the skeleton:
 *       the sheet's alpha cuts the horse away to its bones, its colour sits under
 *       the finished coat, and its shading is laid back over the top so the result
 *       reads as bone. After the overlay because the white lock refuses every
 *       overlay write on a locked texel, and a bleached skeleton is locked white
 *       all over: its cut-outs are not a marking, so they are not the lock's to
 *       refuse.</li>
 * </ul>
 * Either way the sheet's alpha is applied at the very end; it is strictly binary
 * in vanilla's art, so the coat needs a cutout render, never a translucent one.
 *
 * <p>Purity, as {@link LutContribution}: keys out, and the pixels are loaded by
 * whoever builds the {@link com.example.horsegenetics.common.coat.pattern.LutSet}
 * - the game, the designer, the tools - from {@link #sheetResources}.
 */
public interface CoatSheetContribution {

    /** Where in the bake the sheet goes - see the class note. */
    enum Pass { SEED, SHAPE }

    /**
     * One horse's use of a sheet.
     *
     * @param sheet         the key into the {@code LutSet}'s sheets
     * @param coatOpacity   SHAPE only: how much of the finished coat covers the
     *                      sheet's own colour, 0..1
     * @param shadeStrength SHAPE only: how hard the sheet's shading is laid back
     *                      over the top, 0..1
     */
    record SheetUse(String sheet, Pass pass, double coatOpacity, double shadeStrength) {
    }

    /** The sheet this horse wears for {@code skin}, or empty - a carrier wears none. */
    Optional<SheetUse> sheetUse(AllelePair pair, Genotype genotype, Epigenome epigenome, Skin skin);

    /**
     * Every sheet this gene can ask for, as {@code key -> resource path} relative to
     * {@code assets/<namespace>/}, e.g. {@code "textures/entity/horse/horse_zombie.png"}.
     */
    Map<String, String> sheetResources();
}
