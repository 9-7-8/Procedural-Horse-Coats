package com.example.horsegenetics.common.name;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.name.HorseNameGenerator.NameParts;

/**
 * Name combination for breeding. Pure Layer-1 logic.
 *
 * <p>{@link #breedNth} varies the name by how many foals a pairing has already
 * produced, so repeated matings don't churn out the same two names forever:
 * <ol>
 *   <li><b>Foal 1</b> - dam's first + sire's last.</li>
 *   <li><b>Foal 2</b> - sire's first + dam's last (the other combo).</li>
 *   <li><b>Foals 3-6</b> - one half from a parent (either parent, either
 *       half), the other half random.</li>
 *   <li><b>Foal 7 onward</b> - a fully random name.</li>
 * </ol>
 */
public final class HorseNames {

    /**
     * The original 50/50 combination (one half from each parent). Kept for
     * callers / tests that don't track a pairing's foal count.
     *
     * @return {@code {first, last}} where one half comes from {@code dam} and
     *         the other from {@code sire} (never both halves from the same parent)
     */
    public static NameParts breed(NameParts dam, NameParts sire, Rng rng) {
        boolean firstFromDam = rng.nextBoolean();
        return new NameParts(
                firstFromDam ? dam.first() : sire.first(),
                firstFromDam ? sire.last() : dam.last());
    }

    /**
     * @param existingFoals how many foals this exact pairing has already had
     *                      (so the new foal is number {@code existingFoals + 1})
     */
    public static NameParts breedNth(NameParts dam, NameParts sire, int existingFoals,
                                     HorseNameGenerator generator, Rng rng) {
        if (existingFoals <= 0) {
            return new NameParts(dam.first(), sire.last());
        }
        if (existingFoals == 1) {
            return new NameParts(sire.first(), dam.last());
        }
        if (existingFoals <= 5) {
            NameParts random = generator.generateParts(rng);
            NameParts parent = rng.nextBoolean() ? dam : sire;
            return rng.nextBoolean()
                    ? new NameParts(parent.first(), random.last())
                    : new NameParts(random.first(), parent.last());
        }
        return generator.generateParts(rng);
    }

    private HorseNames() {
    }
}
