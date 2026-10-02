package com.example.horsegenetics.common.genetics.genes;

/**
 * <b>Dragon horn colour</b> ({@code horsegenetics.dragon_horn_colour}) - a
 * <b>magical, codominant</b> locus that colours a horse's dragon horns and does
 * nothing at all to a horse without them. The horn colour gene's alleles and rules,
 * through {@link AbstractPartColourGene}, on a locus of its own (owner, 2026-10-01:
 * colour is its own gene per part) - so a unicorn with dragon horns can wear a white
 * horn and black dragon horns, and each is bred for separately.
 *
 * <p>Two different copies make a two-tone pair, the earlier allele at the root and
 * the other at the tips, fading between; each copy's {@code hue} and {@code tone}
 * nudge its colour, and a chaos copy wears a colour of its own. It paints nothing on
 * the coat, so every outcome is a wild type and neither coat golden reads it.
 */
public final class DragonHornColourGene extends AbstractPartColourGene {

    public static final String KEY = "horsegenetics.dragon_horn_colour";

    /** Magical band, beside {@link DragonHornsGene}, as the horn's colour sits beside the horn. */
    public static final int PRIORITY = 240;

    public DragonHornColourGene() {
        super(KEY, "Dragon horn colour", PRIORITY, new Words("dragon-horns", "dragon horns", true,
                "dragon horns", "what nearly every dragon-horned horse has", "a horse with dragon horns"));
    }
}
