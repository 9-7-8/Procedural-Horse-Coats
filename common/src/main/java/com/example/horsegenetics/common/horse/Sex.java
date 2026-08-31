package com.example.horsegenetics.common.horse;

/**
 * A horse's sex, as tracked by this mod. Vanilla horses have no sex concept,
 * so this is assigned by the mod (randomly on spawn) and used for pedigree
 * bookkeeping - which parent is the dam, which the sire - and for the
 * horse-specific display labels ({@link #label}).
 */
public enum Sex {
    MALE,
    FEMALE;

    /**
     * The horse term for this sex at a given age: {@code adult} true ->
     * stallion / mare, false (a foal) -> colt / filly.
     */
    public String label(boolean adult) {
        return switch (this) {
            case MALE -> adult ? "Stallion" : "Colt";
            case FEMALE -> adult ? "Mare" : "Filly";
        };
    }
}
