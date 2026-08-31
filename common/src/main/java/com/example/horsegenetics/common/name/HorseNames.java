package com.example.horsegenetics.common.name;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.name.HorseNameGenerator.NameParts;

/**
 * Name combination for breeding: a foal takes the first name of one parent and
 * the last name of the other, each parent equally likely to supply either
 * half. Pure Layer-1 logic.
 */
public final class HorseNames {

    /**
     * @return {@code {first, last}} where one half comes from {@code dam} and
     *         the other from {@code sire} (never both halves from the same parent)
     */
    public static NameParts breed(NameParts dam, NameParts sire, Rng rng) {
        boolean firstFromDam = rng.nextBoolean();
        return new NameParts(
                firstFromDam ? dam.first() : sire.first(),
                firstFromDam ? sire.last() : dam.last());
    }

    private HorseNames() {
    }
}
